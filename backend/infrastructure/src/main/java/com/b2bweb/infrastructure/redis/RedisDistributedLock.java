package com.b2bweb.infrastructure.redis;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisDistributedLock implements DistributedLock {
  private final StringRedisTemplate redis;
  private final SiteContextProvider sites;
  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end return"
              + " 0",
          Long.class);
  private static final DefaultRedisScript<Long> EXTEND =
      new DefaultRedisScript<>(
          "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('PEXPIRE',KEYS[1],ARGV[2])"
              + " end return 0",
          Long.class);

  public RedisDistributedLock(StringRedisTemplate redis, SiteContextProvider sites) {
    this.redis = redis;
    this.sites = sites;
  }

  public Optional<LockHandle> tryAcquire(String name, Duration lease) {
    RedisKeys.member(name);
    if (!name.matches("[a-z]+\\.[a-z0-9_]+:.+")) throw new IllegalArgumentException("锁名称格式无效");
    long millis = RedisKeys.positive(lease);
    String key = RedisKeys.scope(sites) + "lock:" + name, token = UUID.randomUUID().toString();
    try {
      if (!Boolean.TRUE.equals(
          redis.opsForValue().setIfAbsent(key, token, Duration.ofMillis(millis))))
        return Optional.empty();
    } catch (org.springframework.dao.DataAccessException e) {
      throw unavailable();
    }
    return Optional.of(
        new LockHandle() {
          private boolean closed;

          public String name() {
            return name;
          }

          public synchronized boolean extend(Duration duration) {
            long ms = RedisKeys.positive(duration);
            if (closed) return false;
            try {
              return Long.valueOf(1)
                  .equals(redis.execute(EXTEND, List.of(key), token, Long.toString(ms)));
            } catch (org.springframework.dao.DataAccessException e) {
              throw unavailable();
            }
          }

          public synchronized void close() {
            if (closed) return;
            try {
              redis.execute(RELEASE, List.of(key), token);
              closed = true;
            } catch (org.springframework.dao.DataAccessException e) {
              throw unavailable();
            }
          }
        });
  }

  public Optional<LockHandle> tryAcquire(String name, Duration wait, Duration lease) {
    if (wait == null || wait.isNegative()) throw new IllegalArgumentException("等待时间不能为负");
    long started = System.nanoTime(), budget = wait.toNanos();
    do {
      var handle = tryAcquire(name, lease);
      if (handle.isPresent()) return handle;
      long left = budget - (System.nanoTime() - started);
      if (left <= 0) return Optional.empty();
      try {
        Thread.sleep(
            Duration.ofNanos(
                Math.min(
                    left,
                    Duration.ofMillis(ThreadLocalRandom.current().nextInt(50, 201)).toNanos())));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
    } while (true);
  }

  private static ExternalServiceException unavailable() {
    return new ExternalServiceException(InfraErrors.REDIS_UNAVAILABLE, "分布式锁暂时不可用", true, null);
  }
}
