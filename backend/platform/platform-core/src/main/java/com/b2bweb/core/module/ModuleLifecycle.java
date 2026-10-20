package com.b2bweb.core.module;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.core.context.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** The INSTALLED transition is committed together with callbacks and declarative resources. */
public final class ModuleLifecycle implements ApplicationRunner, Ordered {
  private final JdbcTemplate jdbc;
  private final ModuleCatalog catalog;
  private final Map<String, PlatformModule> modules = new HashMap<>();
  private final SiteContexts sites;
  private final UserContexts users;
  private final TransactionTemplate tx;
  private final ModuleResources resources;

  public ModuleLifecycle(
      JdbcTemplate jdbc,
      ModuleCatalog catalog,
      List<PlatformModule> implementations,
      SiteContexts sites,
      UserContexts users,
      PlatformTransactionManager manager,
      IdGenerator ids,
      ModuleMigrations migrated) {
    this.jdbc = jdbc;
    this.catalog = catalog;
    this.sites = sites;
    this.users = users;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    resources = new ModuleResources(Objects.requireNonNull(jdbc.getDataSource()), ids);
    for (var module : implementations) {
      String id = module.descriptor().id();
      if (modules.putIfAbsent(id, module) != null || !catalog.descriptors().containsKey(id))
        throw new IllegalStateException("重复或未声明的模块实现: " + id);
    }
    if (!modules.keySet().equals(catalog.descriptors().keySet()))
      throw new IllegalStateException("模块生命周期实现不完整");
  }

  public int getOrder() {
    return 0;
  }

  public void run(ApplicationArguments arguments) {
    // Callbacks must be serialized as well as DDL: two instances may finish validation together.
    try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
        var lock = connection.createStatement()) {
      try (var result = lock.executeQuery("SELECT GET_LOCK('b2b-module-lifecycle',60)")) {
        if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("模块初始化锁超时");
      }
      try {
        initialize();
      } finally {
        lock.executeQuery("SELECT RELEASE_LOCK('b2b-module-lifecycle')").close();
      }
    } catch (java.sql.SQLException e) {
      throw new IllegalStateException("模块初始化锁不可用", e);
    }
  }

  private void initialize() {
    Set<String> present = new HashSet<>(catalog.order());
    present.add("core");
    for (String existing :
        jdbc.queryForList("SELECT module_id FROM sys_module WHERE status<>'PURGED'", String.class))
      if (!present.contains(existing))
        jdbc.update(
            "UPDATE sys_module SET"
                + " status='MISSING',missing_since=COALESCE(missing_since,CURRENT_TIMESTAMP(3))"
                + " WHERE module_id=?",
            existing);
    for (String id : catalog.order()) {
      var module = modules.get(id);
      var descriptor = module.descriptor();
      var rows =
          jdbc.queryForList(
              "SELECT installed_version,status FROM sys_module WHERE module_id=?", id);
      String installed = rows.isEmpty() ? null : (String) rows.getFirst().get("installed_version");
      if (installed != null && SemVer.parse(installed).compareTo(descriptor.version()) > 0)
        throw new IllegalStateException("禁止模块降级: " + id);
      try (var actor = users.asActor(ActorRef.SYSTEM)) {
        tx.executeWithoutResult(
            status -> {
              ModuleContext context = context(id, descriptor.version());
              if (installed == null) module.onInstall(context);
              else if (SemVer.parse(installed).compareTo(descriptor.version()) < 0)
                module.onUpgrade(context, SemVer.parse(installed));
              module.onStartup(context);
              resources.installed(id, catalog.descriptors().get(id));
              initializeSites(module, context.siteIds());
            });
      } catch (Exception failure) {
        resources.failed(id);
        throw new IllegalStateException("模块生命周期失败: " + id, failure);
      }
    }
  }

  private ModuleContext context(String id, SemVer version) {
    return new ModuleContext() {
      public String moduleId() {
        return id;
      }

      public SemVer version() {
        return version;
      }

      public List<Long> siteIds() {
        return jdbc.queryForList(
            "SELECT id FROM sys_site WHERE status<>'ARCHIVED' ORDER BY id", Long.class);
      }

      public void forEachSite(java.util.function.LongConsumer action) {
        for (long site : siteIds()) sites.runAs(site, () -> action.accept(site));
      }

      public ActorRef actor() {
        return ActorRef.SYSTEM;
      }
    };
  }

  private void initializeSites(PlatformModule module, List<Long> siteIds) {
    for (long site : siteIds) {
      String id = module.descriptor().id();
      jdbc.update(
          "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(?,?,?,1) ON DUPLICATE"
              + " KEY UPDATE id=id",
          resources.ids.nextId(),
          site,
          id);
      var row =
          jdbc.queryForMap(
              "SELECT enabled,enabled_at FROM sys_site_module WHERE site_id=? AND module_id=? FOR"
                  + " UPDATE",
              site,
              id);
      if (Boolean.TRUE.equals(row.get("enabled"))
          || row.get("enabled") instanceof Number n && n.intValue() != 0) {
        boolean first = row.get("enabled_at") == null;
        if (first) {
          sites.runAs(
              site, () -> module.onEnable(new SiteModuleContext(site, id, ActorRef.SYSTEM, true)));
          jdbc.update(
              "UPDATE sys_site_module SET enabled_at=CURRENT_TIMESTAMP(3) WHERE site_id=? AND"
                  + " module_id=?",
              site,
              id);
        }
      }
    }
  }
}
