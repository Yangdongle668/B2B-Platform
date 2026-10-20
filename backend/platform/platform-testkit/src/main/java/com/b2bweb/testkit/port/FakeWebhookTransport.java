package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class FakeWebhookTransport implements WebhookTransport {
  public record Sent(URI url, String body, Map<String, String> headers) {}

  private record Fixture(String address, WebhookResponse response) {}

  private final Map<URI, Fixture> fixtures = new HashMap<>();
  private final List<Sent> sent = new ArrayList<>();
  private final Clock clock;

  public FakeWebhookTransport(Clock clock) {
    this.clock = clock;
  }

  public void seed(URI url, String publicIp, int status) {
    fixtures.put(url, new Fixture(publicIp, new WebhookResponse(status, Duration.ZERO, null)));
  }

  public WebhookResponse post(WebhookDelivery delivery) {
    var url = delivery.url();
    var fixture = fixtures.get(url);
    if (!"https".equals(url.getScheme())
        || url.getUserInfo() != null
        || url.getHost() == null
        || url.getHost().equalsIgnoreCase("localhost")
        || fixture != null && !PublicAddresses.isPublicLiteral(fixture.address()))
      throw new BusinessException(InfraErrors.WEBHOOK_URL_FORBIDDEN, "Webhook地址不允许");
    if (fixture == null)
      throw new ExternalServiceException(
          InfraErrors.WEBHOOK_UNREACHABLE, "未配置Webhook响应夹具", true, null);
    String timestamp = Long.toString(clock.instant().getEpochSecond());
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              delivery.signingSecret().value().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String signature =
          HexFormat.of()
              .formatHex(
                  mac.doFinal(
                      (timestamp + "." + delivery.body()).getBytes(StandardCharsets.UTF_8)));
      sent.add(
          new Sent(
              url,
              delivery.body(),
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
                  "v1=" + signature)));
      return fixture.response();
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<Sent> deliveries() {
    return List.copyOf(sent);
  }
}
