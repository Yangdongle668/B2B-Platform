package com.b2bweb.testkit.fixture;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public final class ManualClock extends Clock {
  private final AtomicReference<Instant> instant;
  private final AtomicReference<Clock> follower;
  private final ZoneId zone;

  public ManualClock() {
    this(Instant.parse("2026-01-01T00:00:00Z"));
  }

  public ManualClock(Instant instant) {
    this(new AtomicReference<>(instant), ZoneOffset.UTC);
  }

  private ManualClock(AtomicReference<Instant> instant, ZoneId zone) {
    this(instant, zone, new AtomicReference<>());
  }

  private ManualClock(
      AtomicReference<Instant> instant, ZoneId zone, AtomicReference<Clock> follower) {
    this.instant = instant;
    this.zone = zone;
    this.follower = follower;
  }

  public void follow(Clock source) {
    follower.set(source);
  }

  public ZoneId getZone() {
    return zone;
  }

  public Clock withZone(ZoneId zone) {
    return new ManualClock(instant, zone, follower);
  }

  public Instant instant() {
    return follower.get() == null ? instant.get() : follower.get().instant();
  }

  public void advance(Duration duration) {
    if (duration.isNegative()) throw new IllegalArgumentException("时钟不能倒退");
    if (follower.get() != null) {
      instant.set(follower.getAndSet(null).instant());
    }
    instant.updateAndGet(i -> i.plus(duration));
  }

  public void reset(Instant value) {
    follower.set(null);
    instant.set(value);
  }
}
