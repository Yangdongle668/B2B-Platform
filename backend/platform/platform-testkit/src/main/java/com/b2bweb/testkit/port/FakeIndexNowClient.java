package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.util.*;

public final class FakeIndexNowClient implements IndexNowClient {
  private final List<IndexNowSubmission> submitted = new ArrayList<>();
  private int status = 202;

  public void status(int status) {
    this.status = status;
  }

  public IndexNowResult submit(IndexNowSubmission request) {
    if (request.urls().stream().anyMatch(u -> !request.host().equalsIgnoreCase(u.getHost()))
        || !request.host().equalsIgnoreCase(request.keyLocation().getHost()))
      throw new IllegalArgumentException("IndexNow URL必须属于站点主域名");
    if (status == 429 || status >= 500)
      throw new com.b2bweb.api.common.ExternalServiceException(
          com.b2bweb.api.port.InfraErrors.INDEXNOW_FAILED, "受控IndexNow故障", true, null);
    submitted.add(request);
    return new IndexNowResult(status, request.urls().size());
  }

  public List<IndexNowSubmission> submissions() {
    return List.copyOf(submitted);
  }
}
