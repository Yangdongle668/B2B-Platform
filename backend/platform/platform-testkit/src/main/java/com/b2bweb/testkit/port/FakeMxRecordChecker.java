package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;

public final class FakeMxRecordChecker implements MxRecordChecker {
  private final Clock clock;
  private final Map<String, MxCheckResult> fixtures = new HashMap<>();

  public FakeMxRecordChecker(Clock clock) {
    this.clock = clock;
  }

  public void seed(String domain, MxStatus status, List<String> exchanges) {
    fixtures.put(domain, new MxCheckResult(domain, status, exchanges, clock.instant()));
  }

  public MxCheckResult check(String domain) {
    return fixtures.getOrDefault(
        domain, new MxCheckResult(domain, MxStatus.UNKNOWN, List.of(), clock.instant()));
  }
}
