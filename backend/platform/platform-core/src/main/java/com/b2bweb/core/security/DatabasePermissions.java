package com.b2bweb.core.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class DatabasePermissions implements PermissionChecker {
  private final JdbcTemplate jdbc;
  private final SiteContextProvider sites;
  private final CurrentUserProvider users;

  public DatabasePermissions(
      JdbcTemplate jdbc, SiteContextProvider sites, CurrentUserProvider users) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.users = users;
  }

  private List<Map<String, Object>> grants(long user, String code) {
    return jdbc.queryForList(
        "SELECT rp.data_scope,ur.locales,p.locale_scoped FROM sys_user_site_role ur JOIN sys_user u"
            + " ON u.id=ur.user_id JOIN sys_role r ON r.id=ur.role_id JOIN sys_role_permission rp"
            + " ON rp.role_id=r.id JOIN sys_permission p ON p.id=rp.permission_id WHERE"
            + " ur.site_id=? AND ur.user_id=? AND p.code=? AND p.removed_at IS NULL AND"
            + " r.deleted_at IS NULL AND u.deleted_at IS NULL AND u.status='ACTIVE' AND"
            + " EXISTS(SELECT 1 FROM sys_module m WHERE m.module_id=p.module_id AND"
            + " m.status='INSTALLED' AND (m.disableable=0 OR EXISTS(SELECT 1 FROM sys_site_module"
            + " sm WHERE sm.site_id=ur.site_id AND sm.module_id=m.module_id AND sm.enabled=1)))",
        sites.requireSiteId(),
        user,
        code);
  }

  public boolean has(String code) {
    return users.current().map(u -> userHas(u.userId(), code)).orElse(false);
  }

  public boolean hasAny(String... codes) {
    return Arrays.stream(codes).anyMatch(this::has);
  }

  public void check(String code) {
    if (!has(code)) throw new ForbiddenException(CoreErrors.FORBIDDEN, "没有操作权限");
  }

  private boolean isAdmin(long id) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE id=? AND system_admin=1 AND status='ACTIVE' AND"
                + " deleted_at IS NULL",
            Integer.class,
            id)
        > 0;
  }

  public boolean userHas(long id, String code) {
    return isAdmin(id) || !grants(id, code).isEmpty();
  }

  public Optional<DataScope> dataScope(String code) {
    if (users.current().isEmpty()) return Optional.empty();
    if (!has(code)) return Optional.empty();
    if (isAdmin(users.require().userId())) return Optional.of(DataScope.ALL);
    return grants(users.require().userId(), code).stream()
        .map(
            g ->
                g.get("data_scope") == null
                    ? DataScope.ALL
                    : DataScope.valueOf((String) g.get("data_scope")))
        .min(Comparator.comparingInt(Enum::ordinal));
  }

  public LocaleScope localeScope(String code) {
    if (users.current().isEmpty()) return LocaleScope.of(Set.of());
    if (!has(code)) return LocaleScope.of(Set.of());
    if (isAdmin(users.require().userId())) return LocaleScope.all();
    var rows = grants(users.require().userId(), code);
    Set<String> locales = new HashSet<>();
    for (var row : rows) {
      Object flag = row.get("locale_scoped");
      if (Boolean.FALSE.equals(flag)
          || flag instanceof Number n && n.intValue() == 0
          || row.get("locales") == null) return LocaleScope.all();
      for (var l : JsonMapper.builder().build().readTree((String) row.get("locales")))
        locales.add(l.asString());
    }
    return LocaleScope.of(locales);
  }

  public boolean canAccessLocale(String code, String locale) {
    return has(code) && localeScope(code).allows(locale);
  }
}
