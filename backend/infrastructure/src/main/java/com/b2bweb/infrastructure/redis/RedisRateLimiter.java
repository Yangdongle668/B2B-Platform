package com.b2bweb.infrastructure.redis;

import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Atomic continuous token bucket; Redis owns the clock and expiry. ADR-033 candidate. */
public final class RedisRateLimiter implements RateLimiter {
  private final StringRedisTemplate redis;
  private final SiteContextProvider sites;
  private static final DefaultRedisScript<List> CONSUME =
      new DefaultRedisScript<>(
          "local t=redis.call('TIME');local now=t[1]*1000+math.floor(t[2]/1000);local"
              + " cap=tonumber(ARGV[1]);local period=tonumber(ARGV[2]);local"
              + " n=tonumber(ARGV[3]);local state=redis.call('HMGET',KEYS[1],'tokens','time');local"
              + " tokens=cap;if state[1] then"
              + " tokens=math.min(cap,tonumber(state[1])+math.max(0,now-tonumber(state[2]))*cap/period)"
              + " end;local allowed=0;local retry=0;if tokens>=n then"
              + " tokens=tokens-n;allowed=1;else retry=math.ceil((n-tokens)*period/cap)"
              + " end;redis.call('HSET',KEYS[1],'tokens',tokens,'time',now);redis.call('PEXPIRE',KEYS[1],math.ceil(period*2));return"
              + " {allowed,math.floor(tokens),retry}",
          List.class);

  public RedisRateLimiter(StringRedisTemplate redis, SiteContextProvider sites) {
    this.redis = redis;
    this.sites = sites;
  }

  public RateLimitDecision tryConsume(RateLimitRule rule, String subject, int tokens) {
    long period = RedisKeys.positive(rule.period());
    RedisKeys.member(subject);
    if (tokens < 1 || tokens > rule.capacity() || rule.capacity() > 1_000_000_000L)
      throw new IllegalArgumentException("令牌数超出规则容量");
    String scope = rule.siteScoped() ? "s" + sites.requireSiteId() + ":" : "sys:";
    String key = scope + "rl:" + rule.name() + ":" + subject;
    try {
      List<?> r =
          redis.execute(
              CONSUME,
              List.of(key),
              Long.toString(rule.capacity()),
              Long.toString(period),
              Integer.toString(tokens));
      return new RateLimitDecision(
          ((Number) r.get(0)).longValue() == 1,
          ((Number) r.get(1)).longValue(),
          Duration.ofMillis(((Number) r.get(2)).longValue()));
    } catch (org.springframework.dao.DataAccessException e) {
      return new RateLimitDecision(
          !rule.failClosed(), 0, rule.failClosed() ? rule.period() : Duration.ZERO);
    }
  }
}
