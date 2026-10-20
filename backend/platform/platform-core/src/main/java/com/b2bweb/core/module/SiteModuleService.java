package com.b2bweb.core.module;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SiteModuleService {
  private final JdbcTemplate jdbc;
  private final ModuleCatalog catalog;
  private final Map<String, PlatformModule> modules = new HashMap<>();
  private final SiteContexts sites;
  private final UserContexts users;
  private final SiteApi api;
  private final IdGenerator ids;
  private final TransactionTemplate tx;
  private final EventMetaFactory meta;
  private final ApplicationEventPublisher events;
  private final AuditApi audit;
  private final ProjectionChangeApi projections;

  public SiteModuleService(
      JdbcTemplate jdbc,
      ModuleCatalog catalog,
      List<PlatformModule> implementations,
      SiteContexts sites,
      UserContexts users,
      SiteApi api,
      IdGenerator ids,
      PlatformTransactionManager manager,
      EventMetaFactory meta,
      ApplicationEventPublisher events,
      AuditApi audit,
      ProjectionChangeApi projections) {
    this.jdbc = jdbc;
    this.catalog = catalog;
    implementations.forEach(m -> modules.put(m.descriptor().id(), m));
    this.sites = sites;
    this.users = users;
    this.api = api;
    this.ids = ids;
    tx = new TransactionTemplate(manager);
    this.meta = meta;
    this.events = events;
    this.audit = audit;
    this.projections = projections;
  }

  private PlatformModule module(String id) {
    var m = modules.get(id);
    if (m == null
        || api.modules().stream()
            .noneMatch(v -> v.moduleId().equals(id) && v.status() == ModuleStatus.INSTALLED))
      throw new NotFoundException(CoreErrors.EXTENSION_KEY_UNKNOWN, "模块未安装");
    return m;
  }

  private SiteModuleContext context(String id, boolean first) {
    return new SiteModuleContext(sites.requireSiteId(), id, users.actor(), first);
  }

  public ImpactReport impact(String id) {
    return module(id).analyzeDisableImpact(context(id, false));
  }

  public void enable(String id) {
    toggle(id, true, true, ModuleDisablePolicy.notFound());
  }

  public void disable(String id, boolean confirm, ModuleDisablePolicy policy) {
    toggle(id, false, confirm, policy);
  }

  private void toggle(String id, boolean enable, boolean confirm, ModuleDisablePolicy policy) {
    long site = sites.requireSiteId();
    tx.executeWithoutResult(
        status -> {
          var state =
              jdbc.queryForObject(
                  "SELECT status FROM sys_site WHERE id=? FOR UPDATE", String.class, site);
          if ("ARCHIVED".equals(state))
            throw new ForbiddenException(CoreErrors.FORBIDDEN, "归档站点只读");
          var module = module(id);
          if (!enable && !module.descriptor().disableable())
            throw new BusinessException(CoreErrors.FORBIDDEN, "基础模块不可停用");
          if (policy.action() != ModuleDisableAction.NOT_FOUND)
            throw new ValidationException(CoreErrors.VALIDATION_FAILED, "301目标校验尚未开放", List.of());
          jdbc.update(
              "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(?,?,?,0) ON"
                  + " DUPLICATE KEY UPDATE id=id",
              ids.nextId(),
              site,
              id);
          var row =
              jdbc.queryForMap(
                  "SELECT enabled,enabled_at FROM sys_site_module WHERE site_id=? AND module_id=?"
                      + " FOR UPDATE",
                  site,
                  id);
          boolean wasEnabled =
              Boolean.TRUE.equals(row.get("enabled"))
                  || row.get("enabled") instanceof Number n && n.intValue() != 0;
          if (wasEnabled == enable) return;
          if (enable) {
            for (String dependency : module.descriptor().requires().keySet())
              if (!api.isModuleEnabled(site, dependency))
                throw new BusinessException(
                    ErrorCode.of("CORE_MODULE_DEPENDENCY_DISABLED", 409, "依赖模块未启用"),
                    "依赖模块未启用: " + dependency);
          } else {
            List<String> dependents =
                catalog.descriptors().entrySet().stream()
                    .filter(
                        e ->
                            e.getValue().path("requires").has(id)
                                && api.isModuleEnabled(site, e.getKey()))
                    .map(Map.Entry::getKey)
                    .toList();
            if (!dependents.isEmpty())
              throw new BusinessException(
                  ErrorCode.of("CORE_MODULE_REQUIRED", 409, "仍有启用模块依赖此模块"),
                  "依赖模块: " + String.join(",", dependents));
            var impact = module.analyzeDisableImpact(context(id, false));
            if (impact.blocking() || !confirm)
              throw new BusinessException(
                  ErrorCode.of("CORE_MODULE_CONFIRMATION_REQUIRED", 409, "停用需要确认"), "请确认停用影响");
          }
          if (enable) {
            boolean first = row.get("enabled_at") == null;
            jdbc.update(
                "UPDATE sys_site_module SET"
                    + " enabled=1,enabled_at=CURRENT_TIMESTAMP(3),version=version+1,updated_by=?"
                    + " WHERE site_id=? AND module_id=?",
                users.actor().userId(),
                site,
                id);
            module.onEnable(context(id, first));
            events.publishEvent(new ModuleEnabledEvent(meta.next(), id, first));
          } else {
            jdbc.update(
                "UPDATE sys_site_module SET"
                    + " enabled=0,disabled_at=CURRENT_TIMESTAMP(3),config=CAST(? AS"
                    + " JSON),version=version+1,updated_by=? WHERE site_id=? AND module_id=?",
                "{\"onDisable\":\"NOT_FOUND\",\"redirectContentId\":null}",
                users.actor().userId(),
                site,
                id);
            module.onDisable(context(id, false));
            jdbc.update(
                "UPDATE sys_job SET"
                    + " status='CANCELED',error_code='CORE_MODULE_DISABLED',error_message='模块已停用',finished_at=CURRENT_TIMESTAMP(3),version=version+1"
                    + " WHERE site_id=? AND module_id=? AND status='PENDING'",
                site,
                id);
            events.publishEvent(new ModuleDisabledEvent(meta.next(), id));
          }
          projections.searchChanged(
              api.locales().stream()
                  .map(SiteLocaleInfo::locale)
                  .collect(java.util.stream.Collectors.toSet()),
              Set.of(),
              true);
          audit.record(
              new AuditEntry(
                  "core",
                  enable ? "module.enable" : "module.disable",
                  "MODULE",
                  id,
                  enable ? "启用模块" : "停用模块",
                  Map.of(),
                  AuditResult.SUCCESS,
                  true,
                  false));
        });
  }
}
