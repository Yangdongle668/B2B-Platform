package com.b2bweb.infrastructure.redis;

import com.b2bweb.api.site.SiteContextProvider;

final class RedisKeys {
  private RedisKeys() {}

  static String scope(SiteContextProvider sites) {
    return sites.current().map(s -> "s" + s.siteId() + ":").orElse("sys:");
  }

  static String member(String value) {
    if (value == null || value.isEmpty() || value.length() > 2048)
      throw new IllegalArgumentException("成员长度必须在1–2048之间");
    return value;
  }

  static String bucket(String value) {
    if (value == null || !value.matches("[a-z]+\\.[a-z0-9_]+"))
      throw new IllegalArgumentException("桶名称格式无效");
    return value;
  }

  static long positive(java.time.Duration duration) {
    if (duration == null || duration.isNegative() || duration.isZero() || duration.toMillis() < 1)
      throw new IllegalArgumentException("租约或周期必须至少1毫秒");
    return duration.toMillis();
  }
}
