package com.b2bweb.infrastructure.http;

import com.b2bweb.api.port.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

public final class TurnstileCaptchaVerifier implements CaptchaVerifier {
  private final URI endpoint;
  private final JsonHttp http = new JsonHttp();

  public TurnstileCaptchaVerifier(URI endpoint) {
    this.endpoint = endpoint;
  }

  public CaptchaResult verify(CaptchaVerification verification) {
    var fields = new LinkedHashMap<String, String>();
    fields.put("secret", verification.secretKey().value());
    fields.put("response", verification.token());
    if (verification.remoteIp() != null) fields.put("remoteip", verification.remoteIp());
    if (verification.idempotencyKey() != null)
      fields.put("idempotency_key", verification.idempotencyKey());
    String body =
        fields.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(java.util.stream.Collectors.joining("&"));
    var result =
        http.postBytes(
            endpoint,
            body.getBytes(StandardCharsets.UTF_8),
            "application/x-www-form-urlencoded",
            Map.of(),
            Duration.ofSeconds(5),
            InfraErrors.CAPTCHA_UNAVAILABLE);
    JsonHttp.success(result, InfraErrors.CAPTCHA_UNAVAILABLE);
    var node = result.body();
    if (!node.path("success").isBoolean())
      throw new com.b2bweb.api.common.ExternalServiceException(
          InfraErrors.CAPTCHA_UNAVAILABLE, "人机验证响应无效", true, null);
    var errors = new ArrayList<String>();
    node.path("error-codes").forEach(n -> errors.add(n.asString()));
    String time = node.path("challenge_ts").asString(null);
    return new CaptchaResult(
        node.path("success").asBoolean(),
        errors,
        node.path("hostname").asString(null),
        node.path("action").asString(null),
        parse(time));
  }

  private static Instant parse(String value) {
    if (value == null) return null;
    try {
      return Instant.parse(value);
    } catch (java.time.format.DateTimeParseException e) {
      throw new com.b2bweb.api.common.ExternalServiceException(
          InfraErrors.CAPTCHA_UNAVAILABLE, "人机验证响应格式无效", true, null);
    }
  }
}
