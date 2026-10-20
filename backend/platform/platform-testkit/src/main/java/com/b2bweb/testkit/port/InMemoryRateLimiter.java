package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import com.b2bweb.api.site.*;
import java.math.*;
import java.time.*;
import java.util.*;

public final class InMemoryRateLimiter implements RateLimiter {
  private record Bucket(BigDecimal balance, Instant at, RateLimitRule rule) {}

  private final Map<String, Bucket> buckets = new HashMap<>();
  private final SiteContextProvider sites;
  private final Clock clock;
  private boolean available = true;

  public InMemoryRateLimiter(SiteContextProvider sites, Clock clock) {
    this.sites = sites;
    this.clock = clock;
  }

  public void setAvailable(boolean value) {
    available = value;
  }

  public synchronized RateLimitDecision tryConsume(RateLimitRule rule, String subject, int tokens) {
    if (tokens < 1 || rule.period().isNegative() || rule.period().isZero())
      throw new IllegalArgumentException("令牌和周期必须为正");
    String key =
        rule.name() + ":" + (rule.siteScoped() ? sites.requireSiteId() : "global") + ":" + subject;
    if (!available)
      return new RateLimitDecision(
          !rule.failClosed(), 0, rule.failClosed() ? rule.period() : Duration.ZERO);
    var now = clock.instant();
    var old = buckets.get(key);
    BigDecimal capacity = BigDecimal.valueOf(rule.capacity());
    BigDecimal balance =
        old == null || !old.rule().equals(rule)
            ? capacity
            : old.balance()
                .add(
                    BigDecimal.valueOf(Math.max(0, Duration.between(old.at(), now).toNanos()))
                        .multiply(capacity)
                        .divide(BigDecimal.valueOf(rule.period().toNanos()), 18, RoundingMode.DOWN))
                .min(capacity);
    boolean allowed = balance.compareTo(BigDecimal.valueOf(tokens)) >= 0;
    if (allowed) balance = balance.subtract(BigDecimal.valueOf(tokens));
    buckets.put(key, new Bucket(balance, now, rule));
    long missing =
        Math.max(
            0,
            BigDecimal.valueOf(tokens)
                .subtract(balance)
                .multiply(BigDecimal.valueOf(rule.period().toNanos()))
                .divide(capacity, 0, RoundingMode.CEILING)
                .longValue());
    return new RateLimitDecision(
        allowed, balance.longValue(), allowed ? Duration.ZERO : Duration.ofNanos(missing));
  }

  public synchronized void reset() {
    buckets.clear();
    available = true;
  }
}
