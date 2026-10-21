package com.b2bweb.core.cache;

import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import tools.jackson.databind.json.JsonMapper;

/** Conservative site-wide fencing plus bounded tag cleanup. No stale serving on outages. */
public final class RedisTaggedCache implements TaggedCache {
  static final String CONTROL = "sys:core:cache:control";
  final StringRedisTemplate redis;
  final SiteContextProvider sites;
  final JsonMapper json;
  final Clock clock;
  final ConcurrentMap<String, CompletableFuture<Void>> loads = new ConcurrentHashMap<>();
  @SuppressWarnings("rawtypes")
  static final DefaultRedisScript<List> SCRIPT = script();
  @SuppressWarnings("rawtypes")
  private static DefaultRedisScript<List> script() {
    var script = new DefaultRedisScript<List>(); script.setLocation(new ClassPathResource("cache/tagged.lua")); script.setResultType(List.class); return script;
  }
  public RedisTaggedCache(StringRedisTemplate redis, SiteContextProvider sites, JsonMapper json, Clock clock) {
    this.redis = redis; this.sites = sites; this.json = json; this.clock = clock;
  }
  private String prefix() { return "s" + sites.requireSiteId() + ":"; }
  private String key(String ns, String key) {
    if (ns == null || !ns.matches("[a-z][a-z0-9]*\\.[a-z][a-z0-9_.]*") || key == null || key.length() > 4096) throw new IllegalArgumentException("缓存命名空间或键无效");
    return prefix() + ns + ":" + key;
  }
  private void tags(Collection<CacheTag> tags) {
    String site = "s" + sites.requireSiteId();
    if (tags.size() > CacheTag.MAX_TAGS_PER_RESPONSE || tags.stream().anyMatch(t -> !t.value().equals(site) && !t.value().startsWith(site + ":"))) throw new IllegalArgumentException("缓存标签越界");
  }
  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<String> execute(String operation, String entry, String... arguments) {
    List<String> args = new ArrayList<>(List.of(operation)); args.addAll(Arrays.asList(arguments));
    List result = redis.execute(SCRIPT, List.of(CONTROL, sites.current().isPresent() ? prefix() + "core.cache:fence" : "sys:core:cache:unused", entry), args.toArray());
    return result == null ? List.of() : (List<String>) result;
  }
  public List<String> recoveryState() { return execute("state", CONTROL, ""); }
  public List<String> beginRecovery(String epoch) { return execute("begin", CONTROL, epoch); }
  public boolean finishRecovery(String boot, String epoch) { return execute("finish", CONTROL, epoch, boot).equals(List.of("1")); }
  public Optional<CacheWriteFence> beginLoad() {
    sites.requireSiteId();
    try { var values = execute("capture", CONTROL, UUID.randomUUID().toString()); return values.size() == 2 ? Optional.of(new CacheWriteFence(values.get(0), Long.parseLong(values.get(1)))) : Optional.empty(); }
    catch (org.springframework.dao.DataAccessException e) { return Optional.empty(); }
  }
  public <T> Optional<CachedValue<T>> getWithFreshness(String ns, String key, Class<T> type) {
    String entry = key(ns, key);
    try { var values = execute("get", entry, ""); return values.size() == 2 ? Optional.of(new CachedValue<>(json.readValue(values.get(0), type), Instant.ofEpochMilli(Long.parseLong(values.get(1))))) : Optional.empty(); }
    catch (org.springframework.dao.DataAccessException e) { return Optional.empty(); }
  }
  public <T> Optional<T> get(String ns, String key, Class<T> type) { return getWithFreshness(ns, key, type).map(CachedValue::value); }
  public void put(String ns, String key, Object value, Set<CacheTag> tags, Duration ttl) { key(ns,key); tags(tags); beginLoad().ifPresent(f -> putIfUnchanged(ns, key, value, tags, ttl, f)); }
  public boolean putIfUnchanged(String ns, String key, Object value, Set<CacheTag> tags, Duration ttl, CacheWriteFence fence) {
    return writeUntil(ns,key,value,tags,clock.instant().plus(ttl),fence);
  }
  private boolean writeUntil(String ns, String key, Object value, Set<CacheTag> tags, Instant until, CacheWriteFence fence) {
    String entry = key(ns, key); tags(tags);
    if (!until.isAfter(clock.instant())) return false;
    List<String> args = new ArrayList<>(List.of(UUID.randomUUID().toString(), fence.incarnation(), Long.toString(fence.counter()), Long.toString(until.toEpochMilli()), json.writeValueAsString(Objects.requireNonNull(value)), prefix() + "core.cache:namespace:" + ns));
    tags.forEach(tag -> args.add(prefix() + "tag:" + tag.value()));
    try { return execute("put", entry, args.toArray(String[]::new)).equals(List.of("1")); }
    catch (org.springframework.dao.DataAccessException e) { return false; }
  }
  public <T> Optional<T> getOrLoad(String ns, String key, Class<T> type, Duration ttl, Duration wait, Supplier<Optional<CacheEntry<T>>> loader) { return getOrLoadWithFreshness(ns,key,type,ttl,wait,loader).map(CachedValue::value); }
  public <T> Optional<CachedValue<T>> getOrLoadWithFreshness(String ns, String key, Class<T> type, Duration ttl, Duration wait, Supplier<Optional<CacheEntry<T>>> loader) {
    var cached = getWithFreshness(ns, key, type); if (cached.isPresent()) return cached;
    String identity = key(ns, key); var signal = new CompletableFuture<Void>(); var owner = loads.putIfAbsent(identity, signal);
    if (owner != null) {
      try { owner.get(Math.max(0, wait.toMillis()), TimeUnit.MILLISECONDS); }
      catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      catch (ExecutionException | TimeoutException ignored) {}
      cached = getWithFreshness(ns,key,type); if (cached.isPresent()) return cached;
      return load(ns,key,ttl,loader);
    }
    try { return load(ns,key,ttl,loader); }
    finally { loads.remove(identity,signal); signal.complete(null); }
  }
  private <T> Optional<CachedValue<T>> load(String ns, String key, Duration ttl, Supplier<Optional<CacheEntry<T>>> loader) {
    Instant start = clock.instant(); var fence = beginLoad();
    return loader.get().map(entry -> {
      Instant until = (entry.freshUntil().isBefore(start.plus(ttl)) ? entry.freshUntil() : start.plus(ttl)).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      boolean stored = fence.isPresent() && writeUntil(ns,key,entry.value(),entry.tags(),until,fence.get());
      // A failed fence cannot lend an old origin result a new downstream cache lifetime.
      return new CachedValue<>(entry.value(), stored ? until : clock.instant());
    });
  }
  private void advance() { execute("advance", CONTROL, UUID.randomUUID().toString()); }
  private long cleanIndex(String index) {
    long deleted = 0;
    try (var cursor = redis.opsForSet().scan(index, ScanOptions.scanOptions().count(200).build())) {
      List<String> batch = new ArrayList<>(200);
      while (cursor.hasNext()) { batch.add(cursor.next()); if (batch.size() == 200) { deleted += redis.unlink(batch); batch.clear(); } }
      if (!batch.isEmpty()) deleted += redis.unlink(batch);
    }
    redis.unlink(index); return deleted;
  }
  private <T> T required(Supplier<T> action) {
    try { return action.get(); }
    catch (org.springframework.dao.DataAccessException e) { throw new com.b2bweb.api.common.ExternalServiceException(InfraErrors.REDIS_UNAVAILABLE,"缓存失效暂时不可用",true,e); }
  }
  public long invalidate(Collection<CacheTag> tags) { tags(tags); return required(() -> { advance(); long removed = 0; for (var tag : tags) removed += cleanIndex(prefix()+"tag:"+tag.value()); return removed; }); }
  public void evict(String ns, String key) { String entry = key(ns,key); required(() -> { advance(); return redis.unlink(entry); }); }
  public long evictNamespace(String ns) { key(ns,""); return required(() -> { advance(); return cleanIndex(prefix()+"core.cache:namespace:"+ns); }); }
}
