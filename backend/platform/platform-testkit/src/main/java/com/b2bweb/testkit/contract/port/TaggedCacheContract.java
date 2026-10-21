package com.b2bweb.testkit.contract.port;

import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** Same behavioral suite for in-memory and real Redis implementations. */
public abstract class TaggedCacheContract {
  protected abstract TaggedCache api();
  protected abstract void site(long id);
  protected abstract Instant now();
  @BeforeEach void prepare() { site(1); api().evictNamespace("delivery.contract"); }
  @Test public void siteIsolationAndTagOwnership() {
    api().put("delivery.contract","a","one",Set.of(CacheTag.site(1)),Duration.ofSeconds(20));
    site(2); assertThat(api().get("delivery.contract","a",String.class)).isEmpty();
    assertThatThrownBy(() -> api().put("delivery.contract","a","bad",Set.of(CacheTag.site(1)),Duration.ofSeconds(20))).isInstanceOf(IllegalArgumentException.class);
    site(1); assertThat(api().get("delivery.contract","a",String.class)).contains("one");
  }
  @Test public void invalidationAndExplicitEvictionFenceInFlightRefills() {
    var fence=api().beginLoad().orElseThrow();
    api().invalidate(Set.of(CacheTag.content(1,9)));
    assertThat(api().putIfUnchanged("delivery.contract","a","stale",Set.of(CacheTag.content(1,9)),Duration.ofSeconds(20),fence)).isFalse();
    var next=api().beginLoad().orElseThrow(); api().evictNamespace("delivery.contract");
    assertThat(api().putIfUnchanged("delivery.contract","a","stale",Set.of(),Duration.ofSeconds(20),next)).isFalse();
  }
  @Test public void dependencyDeadlineNeverRestartsOnCacheHit() {
    Instant deadline=now().plusSeconds(10);
    var loaded=api().getOrLoadWithFreshness("delivery.contract","deadline",String.class,Duration.ofSeconds(30),Duration.ZERO,() -> Optional.of(new CacheEntry<>("value",Set.of(CacheTag.site(1)),deadline))).orElseThrow();
    var hit=api().getWithFreshness("delivery.contract","deadline",String.class).orElseThrow();
    assertThat(hit.value()).isEqualTo("value");
    assertThat(hit.freshUntil().toEpochMilli()).isEqualTo(loaded.freshUntil().toEpochMilli());
    assertThat(hit.freshUntil()).isBeforeOrEqualTo(deadline);
  }
  @Test public void invalidationDuringOriginLoadForbidsDownstreamCaching() {
    var response=api().getOrLoadWithFreshness("delivery.contract","race",String.class,Duration.ofSeconds(30),Duration.ZERO,() -> {
      api().invalidate(Set.of(CacheTag.site(1))); return Optional.of(new CacheEntry<>("old-origin",Set.of(CacheTag.site(1)),now().plusSeconds(30)));
    }).orElseThrow();
    assertThat(response.value()).isEqualTo("old-origin");
    assertThat(response.freshUntil()).isBeforeOrEqualTo(now());
    assertThat(api().get("delivery.contract","race",String.class)).isEmpty();
  }
  @Test public void emptyLoaderAndExpiredWritesAreNotCached() {
    assertThat(api().getOrLoad("delivery.contract","missing",String.class,Duration.ofSeconds(20),Duration.ZERO,Optional::empty)).isEmpty();
    assertThat(api().putIfUnchanged("delivery.contract","expired","x",Set.of(),Duration.ZERO,api().beginLoad().orElseThrow())).isFalse();
    assertThat(api().get("delivery.contract","expired",String.class)).isEmpty();
  }
}
