package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.CdnPurger;
import com.b2bweb.api.port.InfraErrors;
import java.net.URI;
import java.util.*;

public final class FakeCdnPurger implements CdnPurger {
  private final List<List<CacheTag>> tags = new ArrayList<>();
  private final List<List<URI>> urls = new ArrayList<>();
  private boolean available = true;

  public void available(boolean value) {
    available = value;
  }

  private void requireAvailable() {
    if (!available)
      throw new ExternalServiceException(InfraErrors.CDN_PURGE_FAILED, "受控CDN故障", true, null);
  }

  public synchronized void purgeTags(Collection<CacheTag> values) {
    var copy = List.copyOf(values);
    if (copy.stream().map(t -> t.value().split(":")[0]).distinct().count() > 1)
      throw new IllegalArgumentException("清除标签必须属于同一站点");
    requireAvailable();
    tags.add(copy);
  }

  public synchronized void purgeUrls(Collection<URI> values) {
    if (values.size() > 500 || values.stream().anyMatch(v -> !v.isAbsolute()))
      throw new IllegalArgumentException("URL清除范围无效");
    requireAvailable();
    urls.add(List.copyOf(values));
  }

  public boolean supportsTagPurge() {
    return true;
  }

  public List<List<CacheTag>> tagPurges() {
    return List.copyOf(tags);
  }

  public List<List<URI>> urlPurges() {
    return List.copyOf(urls);
  }
}
