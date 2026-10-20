package com.b2bweb.testkit.port;

import com.b2bweb.api.port.BufferStore;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;

public final class InMemoryBufferStore implements BufferStore {
  final Map<String, LinkedHashMap<String, Long>> counters = new HashMap<>();
  final Map<String, Map<String, Instant>> queues = new HashMap<>();
  final SiteContextProvider sites;
  final Clock clock;

  public InMemoryBufferStore(SiteContextProvider sites, Clock clock) {
    this.sites = sites;
    this.clock = clock;
  }

  String key(String name) {
    if (!name.matches("[a-z][a-z0-9]*\\.[a-z][a-z0-9_.]*"))
      throw new IllegalArgumentException("缓冲键非法");
    return sites.requireSiteId() + ":" + name;
  }

  public synchronized void increment(String bucket, String member, long delta) {
    if (member.length() > 2048) throw new IllegalArgumentException("成员过长");
    counters
        .computeIfAbsent(key(bucket), k -> new LinkedHashMap<>())
        .merge(member, delta, Math::addExact);
  }

  public synchronized Map<String, Long> drainCounters(String bucket, int max) {
    if (max <= 0) throw new IllegalArgumentException("数量非法");
    var source = counters.computeIfAbsent(key(bucket), k -> new LinkedHashMap<>());
    var out = new LinkedHashMap<String, Long>();
    var it = source.entrySet().iterator();
    while (it.hasNext() && out.size() < max) {
      var item = it.next();
      out.put(item.getKey(), item.getValue());
      it.remove();
    }
    return Map.copyOf(out);
  }

  public synchronized void schedule(String queue, String member, Duration delay) {
    if (delay.isNegative()) throw new IllegalArgumentException("延迟非法");
    queues
        .computeIfAbsent(key(queue), k -> new HashMap<>())
        .merge(member, clock.instant().plus(delay), (a, b) -> a.isBefore(b) ? a : b);
  }

  public synchronized List<String> pollDue(String queue, int limit) {
    if (limit <= 0) throw new IllegalArgumentException("数量非法");
    var source = queues.computeIfAbsent(key(queue), k -> new HashMap<>());
    var due =
        source.entrySet().stream()
            .filter(e -> !e.getValue().isAfter(clock.instant()))
            .sorted(Map.Entry.<String, Instant>comparingByValue().thenComparing(Map.Entry::getKey))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    due.forEach(source::remove);
    return due;
  }

  public synchronized long size(String queue) {
    return queues.getOrDefault(key(queue), Map.of()).size();
  }
}
