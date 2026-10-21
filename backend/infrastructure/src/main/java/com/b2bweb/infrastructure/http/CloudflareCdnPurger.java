package com.b2bweb.infrastructure.http;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.net.*;
import java.time.*;
import java.util.*;

public final class CloudflareCdnPurger implements CdnPurger {
  private final URI endpoint;
  private final Secret token;
  private final int batch;
  private final boolean tags;
  private final JsonHttp http = new JsonHttp();

  public CloudflareCdnPurger(URI endpoint, Secret token, int batch, boolean supportsTags) {
    if (batch < 1 || batch > 30) throw new IllegalArgumentException("CDN标签批次必须在1–30之间");
    this.endpoint = endpoint;
    this.token = token;
    this.batch = batch;
    this.tags = supportsTags;
  }

  public boolean supportsTagPurge() {
    return tags;
  }

  public void purgeTags(Collection<CacheTag> values) {
    if (values.isEmpty()) return;
    if (values.stream().map(t -> t.value().split(":", 2)[0]).distinct().count() != 1)
      throw new IllegalArgumentException("CDN标签必须属于同一站点");
    if (!tags) {
      purge(Map.of("purge_everything", true));
      return;
    }
    List<String> list = values.stream().map(CacheTag::value).distinct().toList();
    for (int i = 0; i < list.size(); i += batch)
      purge(Map.of("tags", list.subList(i, Math.min(i + batch, list.size()))));
  }

  public void purgeUrls(Collection<URI> values) {
    if (values.size() > 500) throw new IllegalArgumentException("单批URL最多500个");
    if (values.isEmpty()) return;
    for (var uri : values)
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
        throw new IllegalArgumentException("CDN URL无效");
    List<String> list = values.stream().map(URI::toString).toList();
    for (int i = 0; i < list.size(); i += 30)
      purge(Map.of("files", list.subList(i, Math.min(i + 30, list.size()))));
  }

  private void purge(Object body) {
    var result =
        http.post(
            endpoint,
            body,
            Map.of("Authorization", "Bearer " + token.value()),
            Duration.ofSeconds(10),
            InfraErrors.CDN_PURGE_FAILED);
    JsonHttp.success(result, InfraErrors.CDN_PURGE_FAILED);
    if (!result.body().path("success").asBoolean())
      throw new ExternalServiceException(InfraErrors.CDN_PURGE_FAILED, "CDN清除未成功", true, null);
  }
}
