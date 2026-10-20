package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.testkit.fixture.*;
import com.b2bweb.testkit.port.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocalPortDoublesTest {
  @Test
  void storageIsolatedByVisibilityAndDoesNotCloseCallerStream() throws Exception {
    var storage = new InMemoryObjectStorage(new ManualClock());
    var key = new StorageKey("1/doc.txt");
    var input =
        new ByteArrayInputStream(new byte[] {1, 2}) {
          public void close() {
            throw new AssertionError("caller owns stream");
          }
        };
    storage.put(key, input, 2, "text/plain", Visibility.PRIVATE);
    assertThat(storage.stat(key, Visibility.PUBLIC)).isEmpty();
    assertThat(storage.get(key, Visibility.PRIVATE).readAllBytes()).containsExactly(1, 2);
    assertThatThrownBy(
            () ->
                storage.put(
                    key,
                    new ByteArrayInputStream(new byte[3]),
                    2,
                    "text/plain",
                    Visibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(storage.get(key, Visibility.PRIVATE).readAllBytes()).containsExactly(1, 2);
    storage.delete(key, Visibility.PRIVATE);
    storage.delete(key, Visibility.PRIVATE);
    assertThatThrownBy(() -> storage.get(key, Visibility.PRIVATE))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void captchaIsSingleUseAndUnknownTokensFailClosed() {
    var captcha = new FakeCaptchaVerifier();
    captcha.allow(
        "valid", new CaptchaResult(true, List.of(), "site.example.test", "inquiry", Instant.EPOCH));
    var input = new CaptchaVerification("valid", Secret.of("test"), null, null);
    assertThat(captcha.verify(input).success()).isTrue();
    assertThat(captcha.verify(input).success()).isFalse();
    assertThat(
            captcha
                .verify(new CaptchaVerification("unknown", Secret.of("test"), null, null))
                .success())
        .isFalse();
  }

  @Test
  void expiredLockOwnerCannotReleaseOrExtendANewLease() {
    var clock = new ManualClock();
    var lock = new InMemoryDistributedLock(clock);
    var old = lock.tryAcquire("sample", Duration.ofSeconds(1)).orElseThrow();
    assertThat(lock.tryAcquire("sample", Duration.ofSeconds(1))).isEmpty();
    clock.advance(Duration.ofSeconds(2));
    var current = lock.tryAcquire("sample", Duration.ofSeconds(2)).orElseThrow();
    assertThat(old.extend(Duration.ofSeconds(5))).isFalse();
    old.close();
    assertThat(lock.tryAcquire("sample", Duration.ofSeconds(1))).isEmpty();
    current.close();
    current.close();
    assertThat(lock.tryAcquire("sample", Duration.ofSeconds(1))).isPresent();
  }

  @Test
  void rateLimitRefillsByManualTimeAndSeparatesSites() {
    var p = TestPlatform.standard();
    var limiter = new InMemoryRateLimiter(p.siteContextProvider(), p.clock());
    var rule = new RateLimitRule("form.submit", 2, Duration.ofSeconds(10), true, true);
    p.useSite(1);
    assertThat(limiter.tryConsume(rule, "subject", 2).allowed()).isTrue();
    assertThat(limiter.tryConsume(rule, "subject").allowed()).isFalse();
    p.useSite(2);
    assertThat(limiter.tryConsume(rule, "subject").allowed()).isTrue();
    p.useSite(1);
    p.clock().advance(Duration.ofSeconds(5));
    assertThat(limiter.tryConsume(rule, "subject").allowed()).isTrue();
    assertThat(limiter.tryConsume(rule, "subject").allowed()).isFalse();
  }

  @Test
  void limiterOutageHonoursPerRuleFailurePolicy() {
    var p = TestPlatform.standard().useSite(1);
    var limiter = new InMemoryRateLimiter(p.siteContextProvider(), p.clock());
    limiter.setAvailable(false);
    assertThat(
            limiter
                .tryConsume(
                    new RateLimitRule("core.login", 5, Duration.ofMinutes(15), false, true), "hash")
                .allowed())
        .isFalse();
    assertThat(
            limiter
                .tryConsume(
                    new RateLimitRule("tracking.view", 5, Duration.ofMinutes(1), true, false),
                    "hash")
                .allowed())
        .isTrue();
  }

  @Test
  void scannerFindsEicarAcrossReadBoundariesAndReportsOutage() {
    var scanner = new FakeVirusScanner(new ManualClock());
    byte[] value =
        ("a".repeat(8180) + "EICAR-STANDARD-ANTIVIRUS-TEST-FILE")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(scanner.scan(new ByteArrayInputStream(value), value.length, "test.txt").verdict())
        .isEqualTo(ScanVerdict.INFECTED);
    scanner.setAvailable(false);
    assertThatThrownBy(
            () -> scanner.scan(new ByteArrayInputStream(value), value.length, "test.txt"))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  void fakeEnginePreservesNumbersAndInlineTagsAndSupportsBadOutputFixtures() {
    var engine = new FakeTranslationEngine();
    var config =
        new EngineConfig(
            1,
            "测试",
            URI.create("https://unused.example.test"),
            Secret.of("fake"),
            "fake",
            0,
            null,
            Duration.ofSeconds(1),
            StructuredOutputMode.JSON_OBJECT,
            Map.of());
    var request =
        new TranslationRequest(
            config,
            "instruction",
            "zh-CN",
            "en",
            List.of(new EngineItem("u1", "功率 <1>100</1> W", null, null)),
            "BATCH");
    assertThat(engine.translate(request).items().getFirst().text())
        .isEqualTo("[en] 功率 <1>100</1> W");
    engine.output("u1", "deliberately invalid output");
    assertThat(engine.translate(request).items().getFirst().text())
        .isEqualTo("deliberately invalid output");
    assertThat(engine.requests()).hasSize(2);
  }
}
