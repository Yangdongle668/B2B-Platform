package com.b2bweb.testkit.translation;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.translation.*;
import java.time.*;
import java.util.*;

public final class InMemoryTranslationApi implements TranslationApi {
  final Clock clock;
  final SiteContextProvider sites;
  final Map<String, SegmentOriginStats> stats = new HashMap<>();
  boolean enabled = true;

  public InMemoryTranslationApi(SiteContextProvider sites, Clock clock) {
    this.sites = sites;
    this.clock = clock;
  }

  public void available(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isTextTranslationAvailable() {
    sites.requireSiteId();
    return enabled;
  }

  public TextTranslation translateText(TextTranslationRequest request) {
    if (!isTextTranslationAvailable()) throw new ModuleDisabledException("translation");
    boolean same = Objects.equals(request.sourceLocale(), request.targetLocale());
    return new TextTranslation(
        same ? request.text() : "[" + request.targetLocale() + "] " + request.text(),
        request.sourceLocale(),
        same,
        "test-engine",
        "deterministic",
        0,
        0,
        clock.instant());
  }

  public void origins(SegmentOriginStats value) {
    stats.put(sites.requireSiteId() + ":" + value.localizationId(), value);
  }

  public Map<Long, SegmentOriginStats> segmentOrigins(Collection<Long> ids) {
    Map<Long, SegmentOriginStats> result = new LinkedHashMap<>();
    long site = sites.requireSiteId();
    for (long id : ids) {
      var value = stats.get(site + ":" + id);
      if (value != null) result.put(id, value);
    }
    return Map.copyOf(result);
  }
}
