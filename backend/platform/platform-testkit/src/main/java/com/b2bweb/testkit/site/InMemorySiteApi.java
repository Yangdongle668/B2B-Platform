package com.b2bweb.testkit.site;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemorySiteApi implements SiteApi {
  private SiteApi delegate;

  public void delegate(SiteApi source) {
    delegate = source;
  }

  final MemoryStore store;
  final SiteContextProvider contexts;
  final Map<String, ModuleInfo> modules = new LinkedHashMap<>();
  String adminOrigin = "https://admin.example.test";

  public InMemorySiteApi(MemoryStore store, SiteContextProvider contexts) {
    this.store = store;
    this.contexts = contexts;
  }

  public void addSite(SiteInfo site) {
    if (contexts instanceof FakeSiteContextProvider fake)
      fake.add(
          new SiteContext(
              site.siteId(),
              site.code(),
              site.sourceLocale(),
              site.defaultLocale(),
              site.timezone(),
              null,
              ContextOrigin.SYSTEM));
    store.atomic(
        rows -> {
          rows.put("site:" + site.siteId(), site);
          return null;
        });
  }

  public void addModule(ModuleInfo module) {
    modules.put(module.moduleId(), module);
  }

  public void setLocales(List<SiteLocaleInfo> locales) {
    long site = contexts.requireSiteId();
    store.atomic(
        rows -> {
          rows.put("site-locales:" + site, List.copyOf(locales));
          return null;
        });
  }

  public void setModuleEnabled(String module, boolean enabled) {
    var info = modules.get(module);
    if (info == null) throw new IllegalArgumentException("未注册模块");
    if (!enabled && !info.disableable())
      throw new BusinessException(CoreErrors.FORBIDDEN, "基础模块不可停用");
    store.atomic(
        rows -> {
          rows.put("site-module:" + contexts.requireSiteId() + ":" + module, enabled);
          return null;
        });
  }

  public SiteInfo current() {
    if (delegate != null) return delegate.current();
    return find(contexts.requireSiteId())
        .orElseThrow(() -> new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在"));
  }

  public Optional<SiteInfo> find(long id) {
    if (delegate != null) return delegate.find(id);
    return store.atomic(rows -> Optional.ofNullable((SiteInfo) rows.get("site:" + id)));
  }

  public List<SiteInfo> listOperationalSites() {
    if (delegate != null) return delegate.listOperationalSites();
    return store.atomic(
        rows ->
            rows.entrySet().stream()
                .filter(e -> e.getKey().startsWith("site:"))
                .map(e -> (SiteInfo) e.getValue())
                .filter(s -> s.status() != SiteStatus.ARCHIVED)
                .sorted(Comparator.comparingLong(SiteInfo::siteId))
                .toList());
  }

  public Optional<HostResolution> resolveHost(String host) {
    if (delegate != null) return delegate.resolveHost(host);
    return listOperationalSites().stream()
        .filter(s -> s.primaryDomain().equals(host))
        .map(
            s ->
                new HostResolution(
                    s.siteId(), host, DomainType.PRIMARY, s.primaryDomain(), null, s.status()))
        .findFirst();
  }

  @SuppressWarnings("unchecked")
  public List<SiteLocaleInfo> locales() {
    if (delegate != null) return delegate.locales();
    return store.atomic(
        rows ->
            (List<SiteLocaleInfo>)
                rows.getOrDefault("site-locales:" + contexts.requireSiteId(), List.of()));
  }

  public Optional<SiteLocaleInfo> locale(String code) {
    return locales().stream().filter(l -> l.locale().equals(code)).findFirst();
  }

  public Optional<SiteLocaleInfo> localeByUrlPrefix(String prefix) {
    return locales().stream().filter(l -> l.urlPrefix().equals(prefix)).findFirst();
  }

  public List<String> publicLocales() {
    return locales().stream()
        .filter(SiteLocaleInfo::publiclyVisible)
        .map(SiteLocaleInfo::locale)
        .toList();
  }

  public List<String> autoTranslateLocales() {
    return locales().stream()
        .filter(l -> !l.source() && l.autoTranslate())
        .map(SiteLocaleInfo::locale)
        .toList();
  }

  public boolean isPublicLocale(String code) {
    return locale(code).map(SiteLocaleInfo::publiclyVisible).orElse(false);
  }

  public boolean isModuleEnabled(String id) {
    return isModuleEnabled(contexts.requireSiteId(), id);
  }

  public boolean isModuleEnabled(long site, String id) {
    if (delegate != null) return delegate.isModuleEnabled(site, id);
    var module = modules.get(id);
    if (module == null
        || module.status() != ModuleStatus.INSTALLED
        || find(site).filter(s -> s.status() != SiteStatus.ARCHIVED).isEmpty()) return false;
    return !module.disableable()
        || store.atomic(rows -> Boolean.TRUE.equals(rows.get("site-module:" + site + ":" + id)));
  }

  public ModuleDisablePolicy moduleDisablePolicy(String id) {
    if (delegate != null) return delegate.moduleDisablePolicy(id);
    if (modules().stream().noneMatch(m -> m.moduleId().equals(id)))
      throw new NotFoundException(CoreErrors.EXTENSION_KEY_UNKNOWN, "模块未登记");
    contexts.requireSiteId();
    return ModuleDisablePolicy.notFound();
  }

  public Set<String> enabledModules() {
    if (delegate != null) return delegate.enabledModules();
    Set<String> out = new LinkedHashSet<>();
    for (String id : modules.keySet()) if (isModuleEnabled(id)) out.add(id);
    return Set.copyOf(out);
  }

  public String primaryOrigin() {
    if (delegate != null) return delegate.primaryOrigin();
    return "https://" + current().primaryDomain();
  }

  public String adminOrigin() {
    if (delegate != null) return delegate.adminOrigin();
    return adminOrigin;
  }

  public List<ModuleInfo> modules() {
    if (delegate != null) return delegate.modules();
    return List.copyOf(modules.values());
  }
}
