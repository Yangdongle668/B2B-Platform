package com.b2bweb.infrastructure.http;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.net.*;
import java.time.*;
import java.util.*;

public final class HttpIndexNowClient implements IndexNowClient {
  private final URI endpoint;
  private final JsonHttp http = new JsonHttp();

  public HttpIndexNowClient(URI endpoint) {
    this.endpoint = endpoint;
  }

  public IndexNowResult submit(IndexNowSubmission submission) {
    if (!valid(submission.keyLocation(), submission.host())
        || submission.urls().stream().anyMatch(u -> !valid(u, submission.host())))
      throw new IllegalArgumentException("IndexNow URL必须属于指定主机");
    var result =
        http.post(
            endpoint,
            Map.of(
                "host",
                submission.host(),
                "key",
                submission.key(),
                "keyLocation",
                submission.keyLocation().toString(),
                "urlList",
                submission.urls().stream().map(URI::toString).toList()),
            Map.of(),
            Duration.ofSeconds(10),
            InfraErrors.INDEXNOW_FAILED);
    if (!Set.of(200, 202, 400, 403, 422).contains(result.status()))
      throw new ExternalServiceException(
          InfraErrors.INDEXNOW_FAILED,
          "IndexNow请求失败",
          result.status() == 429 || result.status() >= 500,
          null);
    return new IndexNowResult(result.status(), submission.urls().size());
  }

  private static boolean valid(URI uri, String host) {
    return Set.of("https", "http").contains(uri.getScheme())
        && host.equalsIgnoreCase(uri.getHost())
        && uri.getRawUserInfo() == null
        && uri.getRawFragment() == null;
  }
}
