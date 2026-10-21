package com.b2bweb.core;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.module.*;
import com.b2bweb.core.persistence.*;
import com.b2bweb.core.support.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Import(SiteLineConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@org.springframework.scheduling.annotation.EnableScheduling
public class CoreConfiguration {
  @Bean
  @Primary
  org.springframework.context.ApplicationEventPublisher transactionalDomainEvents(
      org.springframework.context.ApplicationContext context, JdbcTemplate jdbc) {
    return new com.b2bweb.core.event.TransactionalDomainEvents(context, jdbc);
  }

  @Bean
  com.b2bweb.core.cache.RedisTaggedCache taggedCache(org.springframework.data.redis.core.StringRedisTemplate redis, SiteContexts sites, tools.jackson.databind.json.JsonMapper json, Clock clock) {
    return new com.b2bweb.core.cache.RedisTaggedCache(redis, sites, json, clock);
  }

  @Bean(destroyMethod="close")
  com.b2bweb.core.support.cache.LocalCaches localCaches(org.springframework.data.redis.connection.RedisConnectionFactory factory, tools.jackson.databind.json.JsonMapper json, io.micrometer.core.instrument.MeterRegistry metrics) {
    return new com.b2bweb.core.support.cache.LocalCaches(factory,json,metrics);
  }

  @Bean
  com.b2bweb.core.cache.CacheRecovery cacheRecovery(javax.sql.DataSource source, org.springframework.transaction.PlatformTransactionManager manager,
      com.b2bweb.core.cache.RedisTaggedCache cache, SiteApi api, SiteContexts sites,
      org.springframework.beans.factory.ObjectProvider<com.b2bweb.api.port.CdnPurger> cdns, org.springframework.modulith.events.IncompleteEventPublications publications, ModuleMigrations migrated, @Value("${b2bweb.core.cache.recovery-enabled:true}") boolean enabled) {
    return new com.b2bweb.core.cache.CacheRecovery(source,manager,cache,api,sites,cdns,publications,migrated,enabled);
  }

  @Bean
  com.b2bweb.core.job.DatabaseJobs jobs(
      JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager manager,
      SiteContexts sites,
      UserContexts users,
      SiteApi siteApi,
      com.b2bweb.api.extension.ExtensionRegistry extensions,
      com.b2bweb.api.port.ObjectStorage storage,
      IdGenerator ids,
      Clock clock) {
    return new com.b2bweb.core.job.DatabaseJobs(
        jdbc, manager, sites, users, siteApi, extensions, storage, ids, clock);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean(destroyMethod="close")
  LeasedTsidIds ids(javax.sql.DataSource source, Clock clock, @Value("${b2bweb.node-id:0}") int node) {
    return new LeasedTsidIds(source, clock, node);
  }

  @Bean
  static ModuleCatalog moduleCatalog() throws Exception {
    return new ModuleCatalog();
  }

  @Bean
  ModuleMigrations migrations(
      javax.sql.DataSource source,
      ModuleCatalog catalog,
      IdGenerator ids,
      @Value("${b2bweb.core.modules.migrate-on-startup:false}") boolean migrate,
      @Value("${B2BWEB_DB_URL}") String url,
      @Value("${b2bweb.core.migration.datasource.username:}") String migrationUser,
      @Value("${b2bweb.core.migration.datasource.password:}") String migrationPassword) {
    if (!migrate) return new ModuleMigrations(source, catalog, ids, false);
    if (migrationUser.isBlank()) throw new IllegalStateException("迁移账号未配置");
    return new ModuleMigrations(
        new org.springframework.jdbc.datasource.DriverManagerDataSource(
            url, migrationUser, migrationPassword),
        catalog,
        ids,
        true);
  }

  @Bean
  com.b2bweb.core.support.event.ProcessedEvents processedEvents(
      JdbcTemplate jdbc, Clock clock, ModuleMigrations migrated) {
    return new com.b2bweb.core.support.event.JdbcProcessedEvents(jdbc, clock);
  }

  @Bean
  ModuleLifecycle moduleLifecycle(
      JdbcTemplate jdbc,
      ModuleCatalog catalog,
      List<com.b2bweb.api.extension.PlatformModule> modules,
      SiteContexts sites,
      UserContexts users,
      org.springframework.transaction.PlatformTransactionManager manager,
      IdGenerator ids,
      ModuleMigrations migrated) {
    return new ModuleLifecycle(jdbc, catalog, modules, sites, users, manager, ids, migrated);
  }

  @Bean
  UserContexts users() {
    return new UserContexts();
  }

  @Bean
  SiteContexts sites(JdbcTemplate jdbc, ModuleMigrations migrated) {
    return new SiteContexts(
        id ->
            jdbc.query(
                "SELECT * FROM sys_site WHERE id=?",
                rs -> {
                  if (!rs.next()) throw new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在");
                  return new SiteContext(
                      id,
                      rs.getString("code"),
                      rs.getString("source_locale"),
                      rs.getString("default_locale"),
                      ZoneId.of(rs.getString("timezone")),
                      null,
                      ContextOrigin.SYSTEM);
                },
                id));
  }

  @Bean
  SiteApi siteApi(
      JdbcTemplate jdbc,
      SiteContexts contexts,
      @Value("${B2BWEB_ADMIN_ORIGIN:https://admin.example.invalid}") String adminOrigin) {
    return new DatabaseSiteApi(jdbc, contexts, adminOrigin);
  }

  @Bean
  com.b2bweb.core.event.ListenerContexts listenerContexts(
      SiteContexts sites, UserContexts users, SiteApi api, io.micrometer.core.instrument.MeterRegistry metrics) {
    return new com.b2bweb.core.event.ListenerContexts(sites, users, api, metrics);
  }

  @Bean
  com.b2bweb.api.site.SiteProfileApi siteProfiles(JdbcTemplate jdbc, SiteContexts sites) {
    return new com.b2bweb.core.directory.DatabaseSiteProfiles(jdbc, sites);
  }

  @Bean
  com.b2bweb.api.site.UiTextApi uiTexts(JdbcTemplate jdbc, SiteContexts sites, SiteApi api) {
    return new com.b2bweb.core.directory.DatabaseUiTexts(jdbc, sites, api);
  }

  @Bean
  com.b2bweb.api.common.DictApi dicts(
      JdbcTemplate jdbc,
      IdGenerator ids,
      org.springframework.transaction.PlatformTransactionManager manager) {
    return new com.b2bweb.core.directory.DatabaseDicts(jdbc, ids, manager);
  }

  @Bean
  com.b2bweb.api.security.AuditApi audit(
      JdbcTemplate jdbc,
      IdGenerator ids,
      SiteContexts sites,
      UserContexts users,
      org.springframework.transaction.PlatformTransactionManager manager, Clock clock) {
    return new com.b2bweb.core.directory.DatabaseAudit(jdbc, ids, sites, users, manager,clock);
  }

  @Bean
  com.b2bweb.core.settings.SettingsSchemas settingsSchemas(ModuleCatalog catalog) {
    return new com.b2bweb.core.settings.SettingsSchemas(catalog);
  }

  @Bean
  com.b2bweb.api.site.SettingsApi settings(
      JdbcTemplate jdbc,
      com.b2bweb.core.settings.SettingsSchemas schemas,
      SiteContexts sites,
      UserContexts users,
      IdGenerator ids,
      com.b2bweb.api.security.SecretCipher cipher,
      org.springframework.transaction.PlatformTransactionManager manager,
      EventMetaFactory meta,
      org.springframework.context.ApplicationEventPublisher events,
      com.b2bweb.api.security.AuditApi audit,
      ProjectionChangeApi projections,
      SiteApi siteApi) {
    return new com.b2bweb.core.settings.DatabaseSettings(
        jdbc,
        schemas,
        sites,
        users,
        ids,
        cipher,
        manager,
        meta,
        events,
        audit,
        projections,
        siteApi);
  }

  @Bean
  com.b2bweb.core.support.schema.ComponentSchemaCatalog componentSchemas(ModuleCatalog modules) throws java.io.IOException {
    return new com.b2bweb.core.support.schema.ComponentSchemaCatalog(modules);
  }

  @Bean
  TransactionMutexApi transactionMutex(JdbcTemplate jdbc, SiteContexts sites, IdGenerator ids) {
    return new TransactionMutex(jdbc, sites, ids, Set.of("content.route_tree"));
  }

  @Bean
  com.b2bweb.core.security.ModuleEnabledAspect moduleEnabledAspect(SiteApi api) {
    return new com.b2bweb.core.security.ModuleEnabledAspect(api);
  }

  @Bean
  com.b2bweb.api.extension.ExtensionRegistry extensions(
      org.springframework.beans.factory.ListableBeanFactory beans,
      SiteContexts sites,
      SiteApi api) {
    return new Extensions(
        beans.getBeansWithAnnotation(com.b2bweb.api.extension.Extension.class).values(),
        sites,
        api::isModuleEnabled);
  }

  @Bean
  EventMetaFactory events(SiteContexts sites, UserContexts users, IdGenerator ids, Clock clock) {
    return new EventMetaFactory() {
      public EventMeta next() {
        return next(sites.current().map(SiteContext::siteId).orElse(null), users.actor());
      }

      public EventMeta next(Long site, ActorRef actor) {
        return new EventMeta(ids.nextIdString(), clock.instant(), site, actor.toEventActor());
      }
    };
  }
}
