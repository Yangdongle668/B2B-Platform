package com.b2bweb.core.module;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseSiteApi implements SiteApi {
  final JdbcTemplate jdbc;
  final SiteContextProvider contexts;
  final String adminOrigin;

  public DatabaseSiteApi(JdbcTemplate jdbc, SiteContextProvider contexts, String adminOrigin) {
    this.jdbc = jdbc;
    this.contexts = contexts;
    this.adminOrigin = adminOrigin;
  }

  public SiteInfo current() {
    return find(contexts.requireSiteId())
        .orElseThrow(() -> new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在"));
  }

  public Optional<SiteInfo> find(long id) {
    return jdbc
        .query(
            "SELECT s.*,COALESCE((SELECT domain FROM sys_site_domain d WHERE d.site_id=s.id AND"
                + " d.type='PRIMARY' LIMIT 1),'') AS primary_domain FROM sys_site s WHERE s.id=?",
            (r, n) ->
                new SiteInfo(
                    r.getLong("id"),
                    r.getString("code"),
                    r.getString("name"),
                    r.getString("theme_key"),
                    r.getString("source_locale"),
                    r.getString("default_locale"),
                    UrlStrategy.valueOf(r.getString("url_strategy")),
                    TrailingSlash.valueOf(r.getString("trailing_slash")),
                    ZoneId.of(r.getString("timezone")),
                    SiteStatus.valueOf(r.getString("status")),
                    r.getString("primary_domain"),
                    r.getInt("version")),
            id)
        .stream()
        .findFirst();
  }

  public List<SiteInfo> listOperationalSites() {
    return jdbc
        .queryForList(
            "SELECT id FROM sys_site WHERE status IN ('SETUP','ACTIVE') ORDER BY id", Long.class)
        .stream()
        .map(id -> find(id).orElseThrow())
        .toList();
  }

  public Optional<HostResolution> resolveHost(String host) {
    if (!host.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")) return Optional.empty();
    return jdbc
        .query(
            "SELECT d.site_id,d.type,d.locale,s.status,(SELECT domain FROM sys_site_domain p WHERE"
                + " p.site_id=d.site_id AND p.type='PRIMARY' LIMIT 1) AS primary_domain FROM"
                + " sys_site_domain d JOIN sys_site s ON s.id=d.site_id WHERE d.domain=? AND"
                + " s.status<>'ARCHIVED'",
            (r, n) ->
                new HostResolution(
                    r.getLong("site_id"),
                    host,
                    DomainType.valueOf(r.getString("type")),
                    r.getString("primary_domain"),
                    r.getString("locale"),
                    SiteStatus.valueOf(r.getString("status"))),
            host)
        .stream()
        .findFirst();
  }

  public List<SiteLocaleInfo> locales() {
    return jdbc.query(
        "SELECT * FROM sys_site_locale WHERE site_id=? ORDER BY sort_order,locale",
        (r, n) ->
            new SiteLocaleInfo(
                r.getString("locale"),
                r.getString("url_prefix"),
                r.getString("hreflang"),
                r.getBoolean("public"),
                r.getBoolean("auto_translate"),
                r.getInt("sort_order"),
                r.getString("locale").equals("zh-CN")),
        contexts.requireSiteId());
  }

  public Optional<SiteLocaleInfo> locale(String locale) {
    return locales().stream().filter(l -> l.locale().equals(locale)).findFirst();
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

  public boolean isPublicLocale(String locale) {
    return locale(locale).map(SiteLocaleInfo::publiclyVisible).orElse(false);
  }

  public boolean isModuleEnabled(String id) {
    return isModuleEnabled(contexts.requireSiteId(), id);
  }

  public boolean isModuleEnabled(long site, String id) {
    if (find(site).filter(s -> s.status() != SiteStatus.ARCHIVED).isEmpty()) return false;
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_module m WHERE m.module_id=? AND m.status='INSTALLED' AND"
                + " (m.disableable=0 OR EXISTS(SELECT 1 FROM sys_site_module sm WHERE"
                + " sm.module_id=m.module_id AND sm.site_id=? AND sm.enabled=1))",
            Integer.class,
            id,
            site)
        > 0;
  }

  public ModuleDisablePolicy moduleDisablePolicy(String id) {
    if (modules().stream()
        .noneMatch(m -> m.moduleId().equals(id) && m.status() == ModuleStatus.INSTALLED))
      throw new NotFoundException(CoreErrors.EXTENSION_KEY_UNKNOWN, "模块未安装");
    var rows =
        jdbc.queryForList(
            "SELECT config FROM sys_site_module WHERE site_id=? AND module_id=?",
            String.class,
            contexts.requireSiteId(),
            id);
    if (rows.isEmpty() || rows.getFirst() == null) return ModuleDisablePolicy.notFound();
    var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(rows.getFirst());
    String action = node.path("onDisable").asString("NOT_FOUND");
    return new ModuleDisablePolicy(
        ModuleDisableAction.valueOf(action),
        node.path("redirectContentId").isNull() || node.path("redirectContentId").isMissingNode()
            ? null
            : Long.parseLong(node.path("redirectContentId").asString()));
  }

  public Set<String> enabledModules() {
    Set<String> result = new LinkedHashSet<>();
    for (var module : modules())
      if (isModuleEnabled(module.moduleId())) result.add(module.moduleId());
    return Set.copyOf(result);
  }

  public String primaryOrigin() {
    String domain = current().primaryDomain();
    if (domain.isBlank()) throw new IllegalStateException("站点尚未设置主域名");
    return "https://" + domain;
  }

  public String adminOrigin() {
    return adminOrigin;
  }

  public List<ModuleInfo> modules() {
    return jdbc.query(
        "SELECT * FROM sys_module ORDER BY layer,module_id",
        (r, n) ->
            new ModuleInfo(
                r.getString("module_id"),
                r.getString("name"),
                r.getString("layer"),
                Objects.requireNonNullElse(
                    r.getString("installed_version"), r.getString("target_version")),
                r.getBoolean("disableable"),
                ModuleStatus.valueOf(r.getString("status")),
                r.getString("api_prefix"),
                r.getString("table_prefix")));
  }
}
