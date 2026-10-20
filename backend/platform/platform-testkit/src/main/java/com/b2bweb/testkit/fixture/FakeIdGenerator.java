package com.b2bweb.testkit.fixture;

import com.b2bweb.api.common.IdGenerator;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class FakeIdGenerator implements IdGenerator {
  final AtomicLong next = new AtomicLong(7201893453900001L);
  final ConcurrentMap<Long, Instant> times = new ConcurrentHashMap<>();
  final Clock clock;

  public FakeIdGenerator(Clock clock) {
    this.clock = clock;
  }

  public long nextId() {
    long id = next.getAndIncrement();
    times.put(id, clock.instant());
    return id;
  }

  public Instant timestampOf(long id) {
    var time = times.get(id);
    if (time == null) throw new IllegalArgumentException("未知ID");
    return time;
  }

  public void reset(long seed) {
    next.set(seed);
    times.clear();
  }
}
