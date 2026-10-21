package com.b2bweb.application;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.testkit.contract.security.PermissionCheckerContract;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
class PermissionsIntegrationTest extends PermissionCheckerContract {
  @Autowired PermissionChecker permissions;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserContexts users;
  @Autowired SiteContexts sites;
  @Autowired PlatformTransactionManager manager;
  @Autowired com.b2bweb.core.security.auth.BootstrapController bootstrap;
  TransactionStatus transaction;
  AutoCloseable siteScope;
  AutoCloseable userScope;

  @BeforeEach
  void setup() {
    transaction = manager.getTransaction(new DefaultTransactionDefinition());
    org.assertj.core.api.Assertions.assertThat(
            TransactionSynchronizationManager.isActualTransactionActive())
        .isTrue();
    for (long id : List.of(771L, 772L))
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'权限测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          id,
          "permissions-" + id);
    jdbc.update(
        "INSERT INTO sys_role(id,code,name) VALUES(77101,'m0-contract-permissions','权限契约角色')");
    jdbc.update(
        "INSERT INTO sys_permission(id,code,module_id,name,locale_scoped)"
            + " VALUES(77101,?,'core','权限契约',1)",
        PERMISSION);
    site(771);
    current(null);
  }

  @AfterEach
  void restore() throws Exception {
    try {
      if (userScope != null) userScope.close();
      if (siteScope != null) siteScope.close();
    } finally {
      if (transaction != null) manager.rollback(transaction);
    }
  }

  protected PermissionChecker api() {
    return permissions;
  }

  @Test
  void bootstrapContainsOnlyEffectivePermissionScopesAndReflectsRevocation() {
    account(true, false);
    login(false);
    site(771);
    grant(DataScope.OWN, LocaleScope.of(java.util.Set.of("zh-CN")));
    var snapshot = bootstrap.bootstrap().permissions();
    org.assertj.core.api.Assertions.assertThat(snapshot).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(snapshot.getFirst().code()).isEqualTo(PERMISSION);
    org.assertj.core.api.Assertions.assertThat(snapshot.getFirst().dataScope())
        .isEqualTo(DataScope.OWN);
    org.assertj.core.api.Assertions.assertThat(snapshot.getFirst().localeScope().allows("en"))
        .isFalse();
    revoke();
    org.assertj.core.api.Assertions.assertThat(bootstrap.bootstrap().permissions()).isEmpty();
  }

  protected void account(boolean active, boolean administrator) {
    jdbc.update(
        "INSERT INTO sys_user(id,email,display_name,password_hash,status,system_admin)"
            + " VALUES(?,'permissions@example.test','契约用户','unused',?,?) ON DUPLICATE KEY UPDATE"
            + " status=VALUES(status),system_admin=VALUES(system_admin)",
        USER,
        active ? "ACTIVE" : "DISABLED",
        administrator);
  }

  protected void current(CurrentUser user) {
    try {
      if (userScope != null) userScope.close();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    userScope = user == null ? users.withoutUser() : users.open(user);
  }

  protected void site(long id) {
    try {
      if (siteScope != null) siteScope.close();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    siteScope =
        sites.open(
            new SiteContext(
                id,
                "permissions-" + id,
                "zh-CN",
                "zh-CN",
                ZoneOffset.UTC,
                null,
                ContextOrigin.ADMIN));
  }

  protected void grant(DataScope data, LocaleScope locales) {
    jdbc.update(
        "INSERT INTO sys_role_permission(role_id,permission_id,data_scope) VALUES(77101,77101,?)",
        data.name());
    jdbc.update(
        "INSERT INTO sys_user_site_role(id,site_id,user_id,role_id,locales)"
            + " VALUES(77101,?,?,77101,CAST(? AS JSON))",
        sites.requireSiteId(),
        USER,
        locales.unrestricted()
            ? null
            : JsonMapper.builder().build().writeValueAsString(locales.locales()));
  }

  protected void revoke() {
    jdbc.update("DELETE FROM sys_role_permission WHERE role_id=77101 AND permission_id=77101");
  }
}
