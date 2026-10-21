package com.b2bweb.testkit.port;

import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class InMemoryTaggedCache implements TaggedCache {
  record Key(long site, String namespace, String key) {}

  record Entry(Object value, Set<CacheTag> tags, Instant until, CacheWriteFence fence) {}

  final Map<Key, Entry> entries = new HashMap<>();
  final Map<Long, CacheWriteFence> fences = new HashMap<>();
  final ConcurrentMap<Key, CompletableFuture<Void>> loads = new ConcurrentHashMap<>();
  final SiteContextProvider sites;
  final Clock clock;

  public InMemoryTaggedCache(SiteContextProvider sites, Clock clock) {
    this.sites = sites;
    this.clock = clock;
  }

  Key key(String ns, String key) {
    if (!ns.matches("[a-z][a-z0-9]*\\.[a-z][a-z0-9_.]*"))
      throw new IllegalArgumentException("缓存命名空间非法");
    return new Key(sites.requireSiteId(), ns, key);
  }

  void validate(Collection<CacheTag> tags) {
    String prefix = "s" + sites.requireSiteId();
    if (tags.size() > CacheTag.MAX_TAGS_PER_RESPONSE
        || tags.stream()
            .anyMatch(t -> !t.value().equals(prefix) && !t.value().startsWith(prefix + ":")))
      throw new IllegalArgumentException("缓存标签越界");
  }

  synchronized CacheWriteFence fence(long site) {
    return fences.computeIfAbsent(site, id -> new CacheWriteFence(UUID.randomUUID().toString(), 0));
  }

  public synchronized Optional<CacheWriteFence> beginLoad() {
    return Optional.of(fence(sites.requireSiteId()));
  }

  public synchronized <T> Optional<CachedValue<T>> getWithFreshness(
      String ns, String key, Class<T> type) {
    Key k = key(ns, key);
    Entry e = entries.get(k);
    if (e == null || !e.until().isAfter(clock.instant()) || !e.fence().equals(fence(k.site()))) {
      entries.remove(k);
      return Optional.empty();
    }
    return Optional.of(new CachedValue<>(type.cast(e.value()), e.until()));
  }

  public <T> Optional<T> get(String ns, String key, Class<T> type) {
    return getWithFreshness(ns, key, type).map(CachedValue::value);
  }

  public void put(String ns, String key, Object value, Set<CacheTag> tags, Duration ttl) {
    putIfUnchanged(ns, key, value, tags, ttl, beginLoad().orElseThrow());
  }

  public synchronized boolean putIfUnchanged(
      String ns,
      String key,
      Object value,
      Set<CacheTag> tags,
      Duration ttl,
      CacheWriteFence token) {
    validate(tags);
    Key k = key(ns, key);
    if (ttl.isNegative() || ttl.isZero() || !token.equals(fence(k.site()))) return false;
    entries.put(
        k,
        new Entry(
            Objects.requireNonNull(value), Set.copyOf(tags), clock.instant().plus(ttl), token));
    return true;
  }

  public <T> Optional<T> getOrLoad(
      String ns,
      String key,
      Class<T> type,
      Duration ttl,
      Duration wait,
      Supplier<Optional<CacheEntry<T>>> loader) {
    return getOrLoadWithFreshness(ns, key, type, ttl, wait, loader).map(CachedValue::value);
  }

  public <T> Optional<CachedValue<T>> getOrLoadWithFreshness(
      String ns,
      String key,
      Class<T> type,
      Duration ttl,
      Duration wait,
      Supplier<Optional<CacheEntry<T>>> loader) {
    var hit = getWithFreshness(ns, key, type);
    if (hit.isPresent()) return hit;
    Key k = key(ns, key);
    var signal = new CompletableFuture<Void>();
    var owner = loads.putIfAbsent(k, signal);
    if (owner != null) {
      try {
        owner.get(Math.max(0, wait.toMillis()), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException | TimeoutException ignored) {
      }
      hit = getWithFreshness(ns, key, type);
      if (hit.isPresent()) return hit;
      return load(ns, key, ttl, loader);
    }
    try {
      return load(ns, key, ttl, loader);
    } finally {
      loads.remove(k, signal);
      signal.complete(null);
    }
  }

  private <T> Optional<CachedValue<T>> load(
      String ns, String key, Duration ttl, Supplier<Optional<CacheEntry<T>>> loader) {
    var token = beginLoad();
    return loader
        .get()
        .map(
            e -> {
              Instant cap = clock.instant().plus(ttl);
              Instant until = e.freshUntil().isBefore(cap) ? e.freshUntil() : cap;
              boolean stored = token.isPresent() && putIfUnchanged(ns, key, e.value(), e.tags(), Duration.between(clock.instant(), until), token.get());
              return new CachedValue<>(e.value(), stored ? until : clock.instant());
            });
  }

  synchronized void advance() {
    long site = sites.requireSiteId();
    var f = fence(site);
    fences.put(
        site,
        f.counter() == Long.MAX_VALUE
            ? new CacheWriteFence(UUID.randomUUID().toString(), 0)
            : new CacheWriteFence(f.incarnation(), f.counter() + 1));
  }

  public synchronized long invalidate(Collection<CacheTag> tags) {
    validate(tags);
    advance();
    long before = entries.size();
    entries
        .entrySet()
        .removeIf(
            e ->
                e.getKey().site() == sites.requireSiteId()
                    && !Collections.disjoint(e.getValue().tags(), tags));
    return before - entries.size();
  }

  public synchronized void evict(String ns, String key) {
    var k = key(ns, key);
    advance();
    entries.remove(k);
  }

  public synchronized long evictNamespace(String ns) {
    key(ns, "");
    advance();
    long before = entries.size();
    entries.keySet().removeIf(k -> k.site() == sites.requireSiteId() && k.namespace().equals(ns));
    return before - entries.size();
  }

  public synchronized void restart() {
    entries.clear();
    fences.clear();
  }
}
