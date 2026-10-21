package com.b2bweb.core.support.cache;
import java.time.Duration;
public record LocalCacheSpec(long maximumSize, Duration ttl) {
  public LocalCacheSpec { if(maximumSize<1 || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(10))>0) throw new IllegalArgumentException("本地缓存容量或期限无效"); }
}
