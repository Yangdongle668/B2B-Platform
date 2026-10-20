package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.testkit.fixture.*;
import com.b2bweb.testkit.port.*;
import com.b2bweb.testkit.site.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class CacheDoublesTest {
  final ManualClock clock = new ManualClock();
  final FakeSiteContextProvider sites = new FakeSiteContextProvider();
  final InMemoryTaggedCache cache = new InMemoryTaggedCache(sites, clock);

  @BeforeEach
  void setup() {
    sites.use(1);
  }

  @Test
  void fenceInvalidationRejectsOldFillAndIsolatesSites() {
    var old = cache.beginLoad().orElseThrow();
    cache.invalidate(Set.of(CacheTag.content(1, 42)));
    assertThat(
            cache.putIfUnchanged(
                "delivery.resolve",
                "x",
                "old",
                Set.of(CacheTag.content(1, 42)),
                Duration.ofSeconds(50),
                old))
        .isFalse();
    sites.use(2);
    cache.put(
        "delivery.resolve", "x", "B", Set.of(CacheTag.content(2, 42)), Duration.ofSeconds(50));
    sites.use(1);
    cache.evictNamespace("delivery.resolve");
    sites.use(2);
    assertThat(cache.get("delivery.resolve", "x", String.class)).contains("B");
  }

  @Test
  void inheritedDeadlineIsNotExtendedByLoaderTtl() {
    var until = clock.instant().plusSeconds(10);
    cache.getOrLoadWithFreshness(
        "delivery.resolve",
        "x",
        String.class,
        Duration.ofSeconds(55),
        Duration.ZERO,
        () -> Optional.of(new CacheEntry<>("old", Set.of(), until)));
    clock.advance(Duration.ofSeconds(11));
    assertThat(cache.get("delivery.resolve", "x", String.class)).isEmpty();
  }

  @Test
  void restartedCacheRejectsOldTokensAndForeignTags() {
    var old = cache.beginLoad().orElseThrow();
    cache.restart();
    assertThat(
            cache.putIfUnchanged(
                "delivery.resolve", "x", "old", Set.of(), Duration.ofSeconds(1), old))
        .isFalse();
    assertThatThrownBy(() -> cache.invalidate(Set.of(CacheTag.site(2))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bufferKeepsEarlierDeadlineAndDrainsOnlyRequestedMembers() {
    var buffer = new InMemoryBufferStore(sites, clock);
    buffer.schedule("graph.recompute", "42", Duration.ofSeconds(10));
    buffer.schedule("graph.recompute", "42", Duration.ofSeconds(30));
    clock.advance(Duration.ofSeconds(10));
    assertThat(buffer.pollDue("graph.recompute", 1)).containsExactly("42");
    assertThat(buffer.size("graph.recompute")).isZero();
    buffer.increment("url.not_found", "a", 2);
    buffer.increment("url.not_found", "b", 3);
    assertThat(buffer.drainCounters("url.not_found", 1)).hasSize(1);
    assertThat(buffer.drainCounters("url.not_found", 5)).hasSize(1);
  }
}
