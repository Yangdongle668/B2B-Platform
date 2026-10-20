package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.module.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class ModuleToggleTest {
  @Autowired SiteModuleService modules;
  @Autowired SiteContexts sites;
  @Autowired SiteApi api;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;

  @BeforeEach
  void seed() {
    for (long site : List.of(731L, 732L)) {
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'模块测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          site,
          "toggle-" + site);
      jdbc.update(
          "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(?,?,'tracking',1) ON"
              + " DUPLICATE KEY UPDATE enabled=1,config=NULL",
          site * 10,
          site);
    }
  }

  @Test
  void disableRequiresConfirmationAndDoesNotAffectOtherSite() {
    sites.runAs(
        731,
        () -> {
          assertThatThrownBy(
                  () -> modules.disable("tracking", false, ModuleDisablePolicy.notFound()))
              .isInstanceOf(BusinessException.class);
          assertThat(api.isModuleEnabled("tracking")).isTrue();
          modules.disable("tracking", true, ModuleDisablePolicy.notFound());
          assertThat(api.isModuleEnabled("tracking")).isFalse();
          assertThat(api.moduleDisablePolicy("tracking")).isEqualTo(ModuleDisablePolicy.notFound());
          modules.enable("tracking");
          assertThat(api.isModuleEnabled("tracking")).isTrue();
        });
    assertThat(api.isModuleEnabled(732, "tracking")).isTrue();
  }

  @Test
  void foundationCannotBeDisabledAndArchiveCannotBeWritten() {
    sites.runAs(
        731,
        () -> {
          assertThatThrownBy(() -> modules.disable("content", true, ModuleDisablePolicy.notFound()))
              .isInstanceOf(BusinessException.class);
          jdbc.update("UPDATE sys_site SET status='ARCHIVED' WHERE id=731");
          assertThatThrownBy(
                  () -> modules.disable("tracking", true, ModuleDisablePolicy.notFound()))
              .isInstanceOf(ForbiddenException.class);
          assertThat(api.isModuleEnabled("core")).isFalse();
        });
  }

  @Test
  void moduleChangeAuditAndEventRollBackWithCaller() {
    long audit =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_audit_log WHERE site_id=731 AND action='module.disable'",
            Long.class);
    sites.runAs(
        731,
        () ->
            new TransactionTemplate(manager)
                .executeWithoutResult(
                    status -> {
                      modules.disable("tracking", true, ModuleDisablePolicy.notFound());
                      assertThat(api.isModuleEnabled("tracking")).isFalse();
                      status.setRollbackOnly();
                    }));
    assertThat(api.isModuleEnabled(731, "tracking")).isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE site_id=731 AND action='module.disable'",
                Long.class))
        .isEqualTo(audit);
  }

  @Test
  void redirectCannotBeStoredBeforeTargetValidationExists() {
    sites.runAs(
        731,
        () ->
            assertThatThrownBy(
                    () ->
                        modules.disable(
                            "tracking",
                            true,
                            new ModuleDisablePolicy(ModuleDisableAction.REDIRECT, 99L)))
                .isInstanceOf(ValidationException.class));
  }
}
