package com.b2bweb.core;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.core.support.log.*;
import com.b2bweb.core.support.persistence.*;
import com.b2bweb.core.support.web.ETags;
import org.junit.jupiter.api.Test;

class PersistenceSupportTest {
  @Test
  void strictEtagsAndOptimisticOutcomes() {
    assertThat(ETags.of(12)).isEqualTo("\"v12\"");
    assertThat(ETags.parse("\"v12\"")).isEqualTo(12);
    for (String invalid : new String[] {"*", "W/\"v1\"", "\"v01\"", "1", "\"v1\",\"v2\""})
      assertThatThrownBy(() -> ETags.parse(invalid)).isInstanceOf(IllegalArgumentException.class);
    var conflict = ErrorCode.of("CORE_TEST_CONFLICT", 409, "冲突");
    var absent = ErrorCode.of("CORE_TEST_MISSING", 404, "不存在");
    assertThatCode(
            () ->
                OptimisticUpdates.check(
                    1,
                    () -> {
                      throw new AssertionError();
                    },
                    conflict,
                    absent))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> OptimisticUpdates.check(0, () -> 4, conflict, absent))
        .isInstanceOfSatisfying(
            VersionConflictException.class,
            e -> assertThat(e.properties()).containsEntry("currentVersion", 4));
    assertThatThrownBy(() -> OptimisticUpdates.check(0, () -> null, conflict, absent))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void logHelpersNeverResolveHostnamesAndMaskCommonCredentials() {
    assertThat(IpAnonymizer.anonymize("198.51.100.42")).isEqualTo("198.51.100.0");
    assertThat(IpAnonymizer.anonymize("2001:db8:1234:5678:abcd:1234:5678:abcd"))
        .isEqualTo("2001:db8:1234:0:0:0:0:0");
    assertThat(IpAnonymizer.anonymize("localhost")).isEqualTo("unknown");
    assertThat(LogSafe.mask("email=user@example.test password=private-value Bearer access-value"))
        .doesNotContain("user@example.test", "private-value", "access-value");
    assertThat(LogSafe.hash("fixture")).hasSize(16).isEqualTo(LogSafe.hash("fixture"));
  }
}
