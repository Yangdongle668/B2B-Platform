package com.b2bweb.core;

/** Explicit assembly entry point; never starts HTTP, sessions, or worker pools. */
public final class PlatformCommands {
  private PlatformCommands() {}

  public static void migrate(java.util.Map<String, String> env) {
    try {
      String user =
          java.util.Objects.requireNonNull(env.get("B2BWEB_DB_MIGRATION_USER"), "缺少独立迁移账号");
      var source =
          new org.springframework.jdbc.datasource.DriverManagerDataSource(
              java.util.Objects.requireNonNull(env.get("B2BWEB_DB_URL")),
              user,
              java.util.Objects.requireNonNull(env.get("B2BWEB_DB_MIGRATION_PASSWORD")));
      var catalog = new com.b2bweb.core.module.ModuleCatalog();
      new com.b2bweb.core.module.ModuleMigrations(
          source,
          catalog,
          new com.b2bweb.core.support.TsidIds(
              java.time.Clock.systemUTC(),
              Integer.parseInt(java.util.Objects.requireNonNull(env.get("B2BWEB_NODE_ID"),"缺少迁移节点编号"))),
          true);
      System.out.println("迁移完成：core与" + catalog.order().size() + "个模块；未启动HTTP、会话或任务执行器。");
    } catch (Exception e) {
      throw new IllegalStateException("独立迁移失败", e);
    }
  }
}
