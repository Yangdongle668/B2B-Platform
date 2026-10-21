package com.b2bweb.infrastructure.redis;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisBufferStore implements BufferStore {
  private final StringRedisTemplate redis;
  private final SiteContextProvider sites;
  private static final DefaultRedisScript<List> DRAIN =
      new DefaultRedisScript<>(
          "local fields=redis.call('HKEYS',KEYS[1]); local result={}; for"
              + " i=1,math.min(#fields,tonumber(ARGV[1])) do local"
              + " k=fields[i];table.insert(result,k);table.insert(result,redis.call('HGET',KEYS[1],k));redis.call('HDEL',KEYS[1],k);end;return"
              + " result",
          List.class);
  private static final DefaultRedisScript<Long> SCHEDULE =
      new DefaultRedisScript<>(
          "local t=redis.call('TIME');local"
              + " due=t[1]*1000+math.floor(t[2]/1000)+tonumber(ARGV[2]);local"
              + " old=redis.call('ZSCORE',KEYS[1],ARGV[1]);if not old or due<tonumber(old) then"
              + " redis.call('ZADD',KEYS[1],due,ARGV[1]);end;return 1",
          Long.class);
  private static final DefaultRedisScript<List> POLL =
      new DefaultRedisScript<>(
          "local t=redis.call('TIME');local now=t[1]*1000+math.floor(t[2]/1000);local"
              + " result=redis.call('ZRANGEBYSCORE',KEYS[1],'-inf',now,'LIMIT',0,ARGV[1]);if"
              + " #result>0 then redis.call('ZREM',KEYS[1],unpack(result));end;return result",
          List.class);

  public RedisBufferStore(StringRedisTemplate redis, SiteContextProvider sites) {
    this.redis = redis;
    this.sites = sites;
  }

  private String key(String bucket, String kind) {
    return RedisKeys.scope(sites) + "buf:" + kind + ":" + RedisKeys.bucket(bucket);
  }

  private <T> T call(java.util.function.Supplier<T> action) {
    try {
      return action.get();
    } catch (org.springframework.dao.DataAccessException e) {
      throw new ExternalServiceException(InfraErrors.REDIS_UNAVAILABLE, "缓冲服务暂时不可用", true, null);
    }
  }

  private static void limit(int limit) {
    if (limit < 1 || limit > 1000) throw new IllegalArgumentException("批量上限必须在1–1000之间");
  }

  public void increment(String bucket, String member, long delta) {
    String key = key(bucket, "counter");
    RedisKeys.member(member);
    call(() -> redis.opsForHash().increment(key, member, delta));
  }

  public Map<String, Long> drainCounters(String bucket, int max) {
    limit(max);
    String key = key(bucket, "counter");
    List<?> list = call(() -> redis.execute(DRAIN, List.of(key), Integer.toString(max)));
    var result = new LinkedHashMap<String, Long>();
    for (int i = 0; i < list.size(); i += 2)
      result.put(list.get(i).toString(), Long.parseLong(list.get(i + 1).toString()));
    return Map.copyOf(result);
  }

  public void schedule(String queue, String member, Duration delay) {
    if (delay == null || delay.isNegative()) throw new IllegalArgumentException("延迟不能为负");
    String key = key(queue, "queue");
    RedisKeys.member(member);
    call(() -> redis.execute(SCHEDULE, List.of(key), member, Long.toString(delay.toMillis())));
  }

  public List<String> pollDue(String queue, int max) {
    limit(max);
    String key = key(queue, "queue");
    List<?> list = call(() -> redis.execute(POLL, List.of(key), Integer.toString(max)));
    return list.stream().map(Object::toString).toList();
  }

  public long size(String queue) {
    String key = key(queue, "queue");
    return call(() -> redis.opsForZSet().zCard(key));
  }
}
