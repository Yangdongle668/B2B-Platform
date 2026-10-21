package com.b2bweb.core.module;

import com.b2bweb.api.common.IdGenerator;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** Owner histories are immutable; production application startup only validates them. */
public final class ModuleMigrations {
  public ModuleMigrations(DataSource source, ModuleCatalog catalog) {
    this(
        source,
        catalog,
        new com.b2bweb.core.support.TsidIds(java.time.Clock.systemUTC(), 1023),
        false);
  }

  public ModuleMigrations(DataSource source, ModuleCatalog catalog, IdGenerator ids) {
    this(source, catalog, ids, true);
  }

  public ModuleMigrations(
      DataSource source, ModuleCatalog catalog, IdGenerator ids, boolean migrate) {
    if (!migrate) {
      validateOnly(source, "core");
      if(ids instanceof com.b2bweb.core.support.LeasedTsidIds leased) leased.start();
      for (String id : catalog.order()) validateOnly(source, id);
      return;
    }
    try (var connection = source.getConnection();
        var statement = connection.createStatement()) {
      String lock = "b2b-module-migrations";
      try (var result = statement.executeQuery("SELECT GET_LOCK('" + lock + "',30)")) {
        if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("迁移锁超时");
      }
      try {
        configured(source, "core").migrate();
        if(ids instanceof com.b2bweb.core.support.LeasedTsidIds leased) leased.start();
        var resources = new ModuleResources(source, ids);
        try (var input =
            ModuleMigrations.class.getResourceAsStream("/META-INF/b2bweb/core-module.json")) {
          var core = tools.jackson.databind.json.JsonMapper.builder().build().readTree(input);
          resources.pending("core", core);
          resources.installed("core", core);
        }
        for (String id : catalog.order()) {
          var descriptor = catalog.descriptors().get(id);
          resources.pending(id, descriptor);
          try {
            configured(source, id).migrate();
          } catch (RuntimeException failed) {
            resources.failed(id);
            throw failed;
          }
        }
      } finally {
        statement.executeQuery("SELECT RELEASE_LOCK('" + lock + "')").close();
      }
    } catch (java.sql.SQLException | java.io.IOException e) {
      throw new IllegalStateException("模块迁移失败", e);
    }
  }

  private void validateOnly(DataSource source, String id) {
    var flyway = configured(source, id);
    flyway.validate();
    if (flyway.info().pending().length > 0)
      throw new IllegalStateException("模块存在未执行迁移，请先运行migrate: " + id);
  }

  private Flyway configured(DataSource source, String id) {
    if (!id.matches("[a-z][a-z0-9]{1,31}")) throw new IllegalArgumentException("模块ID非法");
    return Flyway.configure()
        .dataSource(source)
        .locations("classpath:db/migration/" + id)
        .table("flyway_" + id + "_history")
        .baselineOnMigrate(!id.equals("core"))
        .baselineVersion("0")
        .validateMigrationNaming(true)
        .validateOnMigrate(true)
        .load();
  }
}
