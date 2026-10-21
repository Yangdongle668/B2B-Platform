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
  final SiteApi siteApi;
  final Map<Long, Boolean> enabled = new HashMap<>();

  public InMemoryTranslationApi(SiteContextProvider sites, SiteApi siteApi, Clock clock) {
    this.sites = sites;
    this.siteApi = siteApi;
    this.clock = clock;
  }

  public void available(boolean enabled) {
    this.enabled.put(sites.requireSiteId(), enabled);
  }

  public boolean isTextTranslationAvailable() {
    sites.requireSiteId();
    return siteApi.isModuleEnabled("translation") && enabled.getOrDefault(sites.requireSiteId(), true);
  }

  public TextTranslation translateText(TextTranslationRequest request) {
    if (!siteApi.isModuleEnabled("translation")) throw new ModuleDisabledException("translation");
    if (!isTextTranslationAvailable()) throw new BusinessException(TranslationErrors.FEATURE_DISABLED, "站点已关闭该翻译功能");
    if (request.text() == null || request.text().isBlank()) throw new IllegalArgumentException("翻译文本不能为空");
    if (request.text().length() > 5000) throw new BusinessException(TranslationErrors.TEXT_TOO_LONG, "待翻译文本过长");
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
