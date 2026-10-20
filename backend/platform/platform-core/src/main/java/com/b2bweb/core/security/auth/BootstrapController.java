package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.module.SiteModuleService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/system")
public class BootstrapController {
  private final SiteApi sites;
  private final CurrentUserProvider users;
  private final PermissionChecker permissions;
  private final JdbcTemplate jdbc;
  private final SiteModuleService modules;

  public BootstrapController(
      SiteApi sites,
      CurrentUserProvider users,
      PermissionChecker permissions,
      JdbcTemplate jdbc,
      SiteModuleService modules) {
    this.sites = sites;
    this.users = users;
    this.permissions = permissions;
    this.jdbc = jdbc;
    this.modules = modules;
  }

  public record Menu(String key, String parent, String name, String route, String icon) {}

  public record Bootstrap(
      SiteInfo site, String displayName, List<ModuleInfo> modules, List<Menu> menus) {}

  @GetMapping("/bootstrap")
  public Bootstrap bootstrap() {
    var enabled = sites.enabledModules();
    var menus =
        jdbc
            .queryForList(
                "SELECT * FROM sys_menu WHERE removed_at IS NULL ORDER BY sort_order,menu_key")
            .stream()
            .filter(row -> enabled.contains((String) row.get("module_id")))
            .filter(
                row ->
                    row.get("permission_code") == null
                        || permissions.has((String) row.get("permission_code")))
            .map(
                row ->
                    new Menu(
                        (String) row.get("menu_key"),
                        (String) row.get("parent_key"),
                        (String) row.get("name"),
                        (String) row.get("route"),
                        (String) row.get("icon")))
            .toList();
    return new Bootstrap(
        sites.current(),
        users.require().name(),
        sites.modules().stream().filter(m -> enabled.contains(m.moduleId())).toList(),
        menus);
  }

  private void requireSite(long id) {
    if (id != sites.current().siteId())
      throw new ForbiddenException(CoreErrors.FORBIDDEN, "请求站点不一致");
  }

  @GetMapping("/sites/{siteId}/modules/{moduleId}/disable-impact")
  @RequiresPermission("system:module:manage")
  public ImpactReport impact(@PathVariable long siteId, @PathVariable String moduleId) {
    requireSite(siteId);
    return modules.impact(moduleId);
  }

  public record Disable(boolean confirm, ModuleDisablePolicy policy) {}

  public record Changed(String result) {}

  @PostMapping("/sites/{siteId}/modules/{moduleId}/enable")
  @RequiresPermission(value = "system:module:manage", reauthenticate = true)
  public Changed enable(@PathVariable long siteId, @PathVariable String moduleId) {
    requireSite(siteId);
    modules.enable(moduleId);
    return new Changed("enabled");
  }

  @PostMapping("/sites/{siteId}/modules/{moduleId}/disable")
  @RequiresPermission(value = "system:module:manage", reauthenticate = true)
  public Changed disable(
      @PathVariable long siteId, @PathVariable String moduleId, @RequestBody Disable body) {
    requireSite(siteId);
    modules.disable(
        moduleId,
        body.confirm(),
        body.policy() == null ? ModuleDisablePolicy.notFound() : body.policy());
    return new Changed("disabled");
  }
}
