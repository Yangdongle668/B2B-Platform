package com.b2bweb.infrastructure.http;

import com.b2bweb.api.common.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class JsonHttp {
  record Result(int status, JsonNode body) {}

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  Result post(
      URI uri, Object body, Map<String, String> headers, Duration timeout, ErrorCode error) {
    return postBytes(
        uri, JSON.writeValueAsBytes(body), "application/json", headers, timeout, error);
  }

  Result postBytes(
      URI uri,
      byte[] body,
      String type,
      Map<String, String> headers,
      Duration timeout,
      ErrorCode error) {
    RemoteCalls.outsideTransaction();
    long started = System.nanoTime();
    try {
      var builder =
          HttpRequest.newBuilder(uri)
              .timeout(timeout)
              .header("Content-Type", type)
              .POST(HttpRequest.BodyPublishers.ofByteArray(body));
      headers.forEach(builder::header);
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
      try (var in = response.body();
          var watchdog = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()) {
        var closing =
            watchdog.schedule(
                () -> {
                  try {
                    in.close();
                  } catch (java.io.IOException ignored) {
                  }
                },
                Math.max(1, timeout.toNanos() - (System.nanoTime() - started)),
                java.util.concurrent.TimeUnit.NANOSECONDS);
        try {
          byte[] bytes = in.readNBytes(1_048_577);
          if (bytes.length > 1_048_576) throw new java.io.IOException();
          JsonNode node;
          try {
            node = bytes.length == 0 ? JSON.createObjectNode() : JSON.readTree(bytes);
          } catch (RuntimeException invalid) {
            node = JSON.createObjectNode();
          }
          return new Result(response.statusCode(), node);
        } finally {
          closing.cancel(false);
          watchdog.shutdownNow();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ExternalServiceException(error, "远程服务调用中断", true, null);
    } catch (java.io.IOException | RuntimeException e) {
      if (e instanceof ExternalServiceException x) throw x;
      throw new ExternalServiceException(error, "远程服务调用失败", true, null);
    }
  }

  static void success(Result result, ErrorCode error) {
    if (result.status() < 200 || result.status() >= 300)
      throw new ExternalServiceException(
          error, "远程服务拒绝请求", result.status() == 429 || result.status() >= 500, null);
  }
}
