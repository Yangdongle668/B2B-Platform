package com.b2bweb.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.dns.DnsMxRecordChecker;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DnsMxTest {
  @Test
  void nullMxHasNoImplicitFallbackAndFailuresRemainUnknown() {
    var count = new AtomicInteger();
    var clock = Clock.systemUTC();
    var checker =
        new DnsMxRecordChecker(
            clock,
            Duration.ofHours(24),
            domain -> {
              count.incrementAndGet();
              return Map.of("MX", List.of("0 ."), "A", List.of("1.2.3.4"));
            });
    assertThat(checker.check("example.test").status()).isEqualTo(MxStatus.NONE);
    checker.check("example.test");
    assertThat(count.get()).isEqualTo(1);
    var failed =
        new DnsMxRecordChecker(
            clock,
            Duration.ofHours(24),
            domain -> {
              throw new javax.naming.CommunicationException();
            });
    assertThat(failed.check("example.test").status()).isEqualTo(MxStatus.UNKNOWN);
    assertThatThrownBy(() -> checker.check("EXAMPLE.test"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
