package com.b2bweb.core;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.security.*;
import com.b2bweb.core.support.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class CorePrimitivesTest {
  static SiteContext site(long id) {
    return new SiteContext(
        id, "s" + id, "zh-CN", "zh-CN", ZoneOffset.UTC, null, ContextOrigin.SYSTEM);
  }

  @Test
  void nestedSitesRestoreAfterExceptionsAndNeverInheritAcrossThreads() throws Exception {
    var contexts = new SiteContexts(CorePrimitivesTest::site);
    assertThatThrownBy(contexts::require).isInstanceOf(NoSiteContextException.class);
    contexts.runAs(
        1,
        () -> {
          assertThat(contexts.requireSiteId()).isEqualTo(1);
          assertThatThrownBy(
                  () ->
                      contexts.runAs(
                          2,
                          () -> {
                            throw new IllegalStateException();
                          }))
              .isInstanceOf(IllegalStateException.class);
          assertThat(contexts.requireSiteId()).isEqualTo(1);
          try (var pool = Executors.newSingleThreadExecutor()) {
            assertThat(pool.submit(() -> contexts.current().isEmpty()).get()).isTrue();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    assertThat(contexts.current()).isEmpty();
  }

  @Test
  void tsidsStayUniqueAndOrderedWhenClockDoesNotMoveAndAcrossNodes() {
    var clock = Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
    var a = new TsidIds(clock, 1);
    var b = new TsidIds(clock, 2);
    Set<Long> ids = new HashSet<>();
    long previous = 0;
    for (int i = 0; i < 10000; i++) {
      long next = a.nextId();
      assertThat(next).isGreaterThan(previous);
      assertThat(ids.add(next)).isTrue();
      assertThat(ids.add(b.nextId())).isTrue();
      previous = next;
    }
    assertThat(a.timestampOf(previous)).isAfterOrEqualTo(clock.instant());
  }

  @Test
  void cipherAuthenticatesPayloadAndKeyIdAndSupportsRotation() {
    byte[] key = new byte[32];
    new java.security.SecureRandom().nextBytes(key);
    var old = new AesGcmSecrets("old", Map.of("old", key));
    String encrypted = old.encrypt(Secret.of("a-sensitive-value"));
    assertThat(encrypted).doesNotContain("sensitive");
    assertThat(old.decrypt(encrypted).value()).isEqualTo("a-sensitive-value");
    var rotated = new AesGcmSecrets("new", Map.of("new", new byte[32], "old", key));
    assertThat(rotated.needsReEncryption(encrypted)).isTrue();
    assertThatThrownBy(() -> rotated.decrypt(encrypted.replace("v1:old:", "v1:new:")))
        .isInstanceOf(BusinessException.class);
    assertThat(old.encrypt(Secret.of("a-sensitive-value"))).isNotEqualTo(encrypted);
    assertThat(old.mask(Secret.of("short"))).isEqualTo("****");
  }
}
