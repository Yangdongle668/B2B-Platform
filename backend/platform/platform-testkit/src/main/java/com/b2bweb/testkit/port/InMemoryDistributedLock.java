package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;

public final class InMemoryDistributedLock implements DistributedLock {
  private final Clock clock;
  private final com.b2bweb.api.site.SiteContextProvider sites;
  private final Map<String, Handle> locks = new HashMap<>();

  public InMemoryDistributedLock(Clock clock) {
    this(clock, null);
  }

  public InMemoryDistributedLock(Clock clock, com.b2bweb.api.site.SiteContextProvider sites) {
    this.clock = clock;
    this.sites = sites;
  }

  private static void lease(Duration value) {
    if (value.isNegative() || value.isZero()) throw new IllegalArgumentException("锁租约必须为正");
  }

  public synchronized Optional<LockHandle> tryAcquire(String name, Duration lease) {
    lease(lease);
    if (name == null || !name.matches("[a-z]+\\.[a-z0-9_]+:.+") || name.length() > 2048)
      throw new IllegalArgumentException("锁名称格式无效");
    String key =
        (sites == null
                ? "sys:"
                : sites.current().map(site -> "s" + site.siteId() + ":").orElse("sys:"))
            + name;
    var old = locks.get(key);
    if (old != null && old.expires.isAfter(clock.instant())) return Optional.empty();
    var handle = new Handle(name, key, clock.instant().plus(lease));
    locks.put(key, handle);
    return Optional.of(handle);
  }

  public Optional<LockHandle> tryAcquire(String name, Duration wait, Duration lease) {
    if (wait.isNegative()) throw new IllegalArgumentException("等待时间不能为负");
    long end = System.nanoTime() + wait.toNanos();
    do {
      var value = tryAcquire(name, lease);
      if (value.isPresent() || System.nanoTime() >= end) return value;
      try {
        Thread.sleep(
            Math.min(50, Math.max(1, Duration.ofNanos(end - System.nanoTime()).toMillis())));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
    } while (true);
  }

  private final class Handle implements LockHandle {
    final String name, key;
    Instant expires;

    Handle(String name, String key, Instant expires) {
      this.name = name;
      this.key = key;
      this.expires = expires;
    }

    public String name() {
      return name;
    }

    public boolean extend(Duration lease) {
      lease(lease);
      synchronized (InMemoryDistributedLock.this) {
        if (locks.get(key) != this || !expires.isAfter(clock.instant())) return false;
        expires = clock.instant().plus(lease);
        return true;
      }
    }

    public void close() {
      synchronized (InMemoryDistributedLock.this) {
        locks.remove(key, this);
      }
    }
  }

  public synchronized void reset() {
    locks.clear();
  }
}
