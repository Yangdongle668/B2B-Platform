package com.b2bweb.core.directory;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class DatabaseUsers implements UserDirectory {
  final JdbcTemplate jdbc;
  final SiteContextProvider sites;
  final PermissionChecker permissions;

  public DatabaseUsers(
      JdbcTemplate jdbc, SiteContextProvider sites, PermissionChecker permissions) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.permissions = permissions;
  }

  public Optional<UserSummary> find(long id) {
    return jdbc
        .query(
            "SELECT id,display_name,email,status,deleted_at FROM sys_user WHERE id=?",
            (r, n) ->
                new UserSummary(
                    r.getLong("id"),
                    r.getString("display_name"),
                    r.getString("email"),
                    r.getString("status").equals("ACTIVE") && r.getTimestamp("deleted_at") == null),
            id)
        .stream()
        .findFirst();
  }

  public Map<Long, UserSummary> findAll(Collection<Long> ids) {
    Map<Long, UserSummary> out = new LinkedHashMap<>();
    for (long id : ids) find(id).ifPresent(u -> out.put(id, u));
    return Map.copyOf(out);
  }

  public boolean isActiveMember(long id) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_user u WHERE u.id=? AND u.status='ACTIVE' AND u.deleted_at IS"
                + " NULL AND (u.system_admin=1 OR EXISTS(SELECT 1 FROM sys_user_site_role ur JOIN"
                + " sys_role r ON r.id=ur.role_id WHERE ur.user_id=u.id AND ur.site_id=? AND"
                + " r.deleted_at IS NULL))",
            Integer.class,
            id,
            sites.requireSiteId())
        > 0;
  }

  public List<UserSummary> usersWithPermission(String permission, String locale) {
    long site = sites.requireSiteId();
    List<UserSummary> out = new ArrayList<>();
    for (long id :
        jdbc.queryForList(
            "SELECT id FROM sys_user WHERE status='ACTIVE' AND deleted_at IS NULL ORDER BY id",
            Long.class)) {
      if (!permissions.userHas(id, permission)) continue;
      if (locale == null
          || jdbc.queryForObject(
              "SELECT system_admin FROM sys_user WHERE id=?", Boolean.class, id)) {
        find(id).ifPresent(out::add);
        continue;
      }
      var rows =
          jdbc.queryForList(
              "SELECT ur.locales,p.locale_scoped FROM sys_user_site_role ur JOIN sys_role r ON"
                  + " r.id=ur.role_id JOIN sys_role_permission rp ON rp.role_id=r.id JOIN"
                  + " sys_permission p ON p.id=rp.permission_id WHERE ur.site_id=? AND ur.user_id=?"
                  + " AND p.code=? AND p.removed_at IS NULL AND r.deleted_at IS NULL",
              site,
              id,
              permission);
      boolean allowed =
          rows.stream()
              .anyMatch(
                  row -> {
                    Object flag = row.get("locale_scoped");
                    if (Boolean.FALSE.equals(flag)
                        || flag instanceof Number n && n.intValue() == 0
                        || row.get("locales") == null) return true;
                    for (var l : JsonMapper.builder().build().readTree((String) row.get("locales")))
                      if (l.asString().equals(locale)) return true;
                    return false;
                  });
      if (allowed) find(id).ifPresent(out::add);
    }
    return List.copyOf(out);
  }
}
