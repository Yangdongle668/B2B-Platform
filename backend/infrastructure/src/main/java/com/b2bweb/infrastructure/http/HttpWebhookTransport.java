package com.b2bweb.infrastructure.http;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HttpWebhookTransport implements WebhookTransport {
  private final Clock clock;
  private final SsrfGuard guard = new SsrfGuard();
  private final PinnedHttp http = new PinnedHttp();

  public HttpWebhookTransport(Clock clock) {
    this.clock = clock;
  }

  public WebhookResponse post(WebhookDelivery delivery) {
    RemoteCalls.outsideTransaction();
    var timeout = delivery.timeout() == null ? Duration.ofSeconds(10) : delivery.timeout();
    if (timeout.isNegative()
        || timeout.isZero()
        || delivery.eventType().contains("\r")
        || delivery.eventType().contains("\n")
        || delivery.deliveryId().contains("\r")
        || delivery.deliveryId().contains("\n")) throw new IllegalArgumentException("Webhook参数无效");
    long started = System.nanoTime();
    try {
      var target = guard.validate(delivery.url(), null, true, timeout);
      String timestamp = Long.toString(clock.instant().getEpochSecond());
      String signature = signature(delivery.signingSecret(), timestamp, delivery.body());
      return http.exchange(
          target,
          "POST",
          Map.of(
              "Content-Type",
              "application/json",
              "X-B2B-Event",
              delivery.eventType(),
              "X-B2B-Delivery",
              delivery.deliveryId(),
              "X-B2B-Timestamp",
              timestamp,
              "X-B2B-Signature",
              signature),
          delivery.body().getBytes(StandardCharsets.UTF_8),
          Duration.ofNanos(Math.max(1, timeout.toNanos() - (System.nanoTime() - started))),
          (status, headers, in) ->
              new WebhookResponse(
                  status,
                  Duration.ofNanos(System.nanoTime() - started),
                  new String(in.readNBytes(1024), StandardCharsets.UTF_8)));
    } catch (IllegalArgumentException e) {
      throw new BusinessException(InfraErrors.WEBHOOK_URL_FORBIDDEN, "Webhook地址不允许");
    } catch (java.io.IOException e) {
      throw new ExternalServiceException(
          InfraErrors.WEBHOOK_UNREACHABLE, "Webhook投递失败", true, null);
    }
  }

  static String signature(Secret key, String timestamp, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.value().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return "v1="
          + HexFormat.of()
              .formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException("无法生成Webhook签名");
    }
  }
}
