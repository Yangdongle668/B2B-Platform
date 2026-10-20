package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.util.*;
import java.util.concurrent.*;

public final class FakeCaptchaVerifier implements CaptchaVerifier {
  private final Map<String, CaptchaResult> tokens = new ConcurrentHashMap<>();
  private final Set<String> used = ConcurrentHashMap.newKeySet();
  private RuntimeException failure;

  public void allow(String token, CaptchaResult result) {
    tokens.put(token, result);
  }

  public void failWith(RuntimeException error) {
    failure = error;
  }

  public CaptchaResult verify(CaptchaVerification input) {
    if (failure != null) throw failure;
    if (!used.add(input.token()))
      return new CaptchaResult(false, List.of("timeout-or-duplicate"), null, null, null);
    return tokens.getOrDefault(
        input.token(),
        new CaptchaResult(false, List.of("invalid-input-response"), null, null, null));
  }

  public void reset() {
    tokens.clear();
    used.clear();
    failure = null;
  }
}
