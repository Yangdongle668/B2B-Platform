package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.b2bweb.api.common.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.module.*;
import com.b2bweb.core.security.auth.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Import(PlatformIntegrationTest.Fixture.class)
class PlatformIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired SiteContexts sites;
  @Autowired ModuleCatalog modules;
  @Autowired TransactionMutexApi mutex;

  @org.mybatis.spring.annotation.MapperScan(
      basePackageClasses = com.b2bweb.application.fixtures.SiteRowMapper.class)
  @TestConfiguration(proxyBeanMethods = false)
  static class Fixture {
    @Bean
    com.b2bweb.product.listener.RuntimeObserver observer(SiteContexts sites, UserContexts users) {
      return new com.b2bweb.product.listener.RuntimeObserver(sites, users);
    }

    @Bean
    FixtureController fixtureController(SiteContexts sites) {
      return new FixtureController(sites);
    }
  }

  @RestController
  static class FixtureController {
    final SiteContexts sites;

    FixtureController(SiteContexts sites) {
      this.sites = sites;
    }

    @GetMapping("/api/admin/v1/test/site")
    Map<String, String> site() {
      return Map.of("siteId", Long.toString(sites.requireSiteId()));
    }
  }

  @Autowired com.b2bweb.product.listener.RuntimeObserver observer;
  @Autowired org.springframework.context.ApplicationEventPublisher publisher;
  @Autowired org.springframework.modulith.events.IncompleteEventPublications incomplete;
  @Autowired com.b2bweb.application.fixtures.SiteRowMapper mapper;
  @Autowired com.b2bweb.api.site.SiteApi siteApi;
  @Autowired com.b2bweb.core.support.event.ProcessedEvents processed;
  @Autowired com.b2bweb.api.event.EventMetaFactory events;
  @Autowired org.springframework.transaction.PlatformTransactionManager manager;
  @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;

  @BeforeEach
  void seed() {
    var keys = redis.keys("sys:core:auth:*");
    if (!keys.isEmpty()) redis.delete(keys);
    jdbc.update("DELETE FROM cnt_content WHERE id IN (10011,10012)");
    jdbc.update("DELETE FROM sys_user_site_role");
    jdbc.update("DELETE FROM sys_user_mfa");
    jdbc.update("DELETE FROM sys_user");
    jdbc.update("DELETE FROM sys_site WHERE id IN (701,702)");
    for (long id : List.of(701L, 702L))
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'测试站点','industrial','UTC','ACTIVE')",
          id,
          "test-" + id);
    jdbc.update(
        "INSERT INTO sys_user(id,email,display_name,password_hash)"
            + " VALUES(801,'editor@example.test','编辑',?)",
        encoder.encode("correct-password-2026"));
    jdbc.update(
        "INSERT INTO sys_user(id,email,display_name,password_hash,system_admin)"
            + " VALUES(802,'admin@example.test','管理员',?,1)",
        encoder.encode("correct-password-2026"));
    jdbc.update(
        "INSERT INTO sys_role(id,code,name) VALUES(901,'test-editor','编辑') ON DUPLICATE KEY UPDATE"
            + " name=name");
    jdbc.update(
        "INSERT INTO sys_user_site_role(id,site_id,user_id,role_id) VALUES(1001,701,801,901)");
    jdbc.update(
        "INSERT INTO cnt_content(id,site_id,type,owner_module)"
            + " VALUES(10011,701,'PAGE','page'),(10012,702,'PAGE','page')");
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder csrfPost(String path)
      throws Exception {
    var c =
        mvc.perform(get("/api/admin/v1/system/auth/csrf").secure(true))
            .andReturn()
            .getResponse()
            .getCookie("XSRF-TOKEN");
    assertThat(c).isNotNull();
    return post(path).cookie(c).header("X-XSRF-TOKEN", c.getValue());
  }

  jakarta.servlet.http.Cookie login(String email) throws Exception {
    var result =
        mvc.perform(
                csrfPost("/api/admin/v1/system/auth/login")
                    .secure(true)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + email + "\",\"password\":\"correct-password-2026\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var cookie = result.getResponse().getCookie("__Host-SESSION");
    assertThat(cookie).isNotNull();
    return cookie;
  }

  @Test
  void independentModuleHistoriesAndAuthoritativeTablesExist() {
    assertThat(modules.order()).hasSize(Integer.getInteger("b2b.expected-module-count", 17));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND"
                    + " table_name LIKE 'flyway_%_history'",
                Integer.class))
        .isEqualTo(Integer.getInteger("b2b.expected-module-count", 17) + 1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_media_history WHERE success=1 AND type='SQL'",
                Integer.class))
        .isEqualTo(2);
    new ModuleMigrations(Objects.requireNonNull(jdbc.getDataSource()), modules);
  }

  @Test
  void anonymousAdminIs401AndCsrfIsMandatory() throws Exception {
    mvc.perform(get("/api/admin/v1/test/site")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/admin/v1/system/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"editor@example.test\",\"password\":\"correct-password-2026\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void csrfEndpointIssuesReadableSecureCookie() throws Exception {
    mvc.perform(get("/api/admin/v1/system/auth/csrf").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"))
        .andExpect(cookie().secure("XSRF-TOKEN", true));
  }

  @Test
  void redisBackedSessionHasSecureCookieAndRejectsCrossSite() throws Exception {
    var cookie = login("editor@example.test");
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    mvc.perform(get("/api/admin/v1/test/site").cookie(cookie).header("X-Site-Id", "701"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.siteId").value("701"));
    mvc.perform(get("/api/admin/v1/test/site").cookie(cookie).header("X-Site-Id", "702"))
        .andExpect(status().isForbidden());
    assertThat(sites.current()).isEmpty();
  }

  @Test
  void inactiveUserCannotKeepUsingExistingSession() throws Exception {
    var cookie = login("editor@example.test");
    jdbc.update("UPDATE sys_user SET status='DISABLED' WHERE id=801");
    mvc.perform(get("/api/admin/v1/test/site").cookie(cookie).header("X-Site-Id", "701"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void privilegedUserMustEnrollMfaBeforeAnyAdminResource() throws Exception {
    var cookie = login("admin@example.test");
    mvc.perform(get("/api/admin/v1/test/site").cookie(cookie).header("X-Site-Id", "701"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CORE_MFA_REQUIRED"));
  }

  @Test
  void wrongPasswordCommitsFailureCounterAndIndependentAudit() throws Exception {
    mvc.perform(
            csrfPost("/api/admin/v1/system/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"editor@example.test\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    assertThat(
            jdbc.queryForObject(
                "SELECT failed_login_count FROM sys_user WHERE id=801", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE user_id=801 AND"
                    + " action='user.login_failed'",
                Integer.class))
        .isPositive();
  }

  @Test
  void mutexRejectsCallsOutsideAnExistingTransaction() {
    sites.runAs(
        701,
        () ->
            assertThatThrownBy(() -> mutex.lock("content.route_tree"))
                .isInstanceOf(IllegalStateException.class));
  }

  @Test
  void realMybatisInterceptorDeniesCrossSiteReadsAndWrites() {
    sites.runAs(
        701,
        () -> {
          assertThat(mapper.find(10011)).isNotNull();
          assertThat(mapper.find(10012)).isNull();
          assertThat(mapper.update(10012)).isZero();
        });
    assertThatThrownBy(() -> mapper.find(10011))
        .hasRootCauseInstanceOf(NoSiteContextException.class);
    assertThat(mapper.globalUsers()).isEqualTo(2);
  }

  @Test
  void siteLanguagesDoNotFallbackAndMissingModulesAreDisabled() {
    sites.runAs(
        701,
        () -> {
          assertThat(siteApi.locale("en")).isEmpty();
          assertThat(siteApi.isPublicLocale("en")).isFalse();
          assertThat(siteApi.isModuleEnabled("missing")).isFalse();
          assertThat(siteApi.modules())
              .hasSize(Integer.getInteger("b2b.expected-module-count", 17) + 1);
        });
  }

  @Test
  void processedEventMarkerCommitsOrRollsBackWithListenerTransaction() {
    sites.runAs(
        701,
        () -> {
          var event = new com.b2bweb.api.event.ModuleDisabledEvent(events.next(), "product");
          var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
          assertThatThrownBy(() -> processed.firstTime("product.cleanup", event))
              .isInstanceOf(IllegalStateException.class);
          tx.executeWithoutResult(
              t -> {
                assertThat(processed.firstTime("product.cleanup", event)).isTrue();
                t.setRollbackOnly();
              });
          tx.executeWithoutResult(
              t -> assertThat(processed.firstTime("product.cleanup", event)).isTrue());
          tx.executeWithoutResult(
              t -> assertThat(processed.firstTime("product.cleanup", event)).isFalse());
        });
  }

  @Test
  void totpUsesRfc6238AndMfaActivationRotatesSessionAndRejectsReplay() throws Exception {
    assertThat(Totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1)).isEqualTo("287082");
    var login = login("admin@example.test");
    var setup =
        mvc.perform(csrfPost("/api/admin/v1/system/auth/mfa/setup").cookie(login))
            .andExpect(status().isOk())
            .andReturn();
    var json = tools.jackson.databind.json.JsonMapper.builder().build();
    String secret =
        json.readTree(setup.getResponse().getContentAsString()).path("secret").asString();
    String code = Totp.code(secret, java.time.Instant.now().getEpochSecond() / 30);
    var activated =
        mvc.perform(
                csrfPost("/api/admin/v1/system/auth/mfa/activate")
                    .cookie(login)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("AUTHENTICATED"))
            .andReturn();
    var cookie = activated.getResponse().getCookie("__Host-SESSION");
    assertThat(cookie).isNotNull();
    assertThat(cookie.getValue()).isNotEqualTo(login.getValue());
    var codes = json.readTree(activated.getResponse().getContentAsString()).path("recoveryCodes");
    assertThat(codes.size()).isEqualTo(10);
    String recovery = codes.get(0).asString();
    assertThat(
            jdbc.queryForObject(
                "SELECT CAST(recovery_code_hashes AS CHAR) FROM sys_user_mfa WHERE user_id=802",
                String.class))
        .doesNotContain(recovery);
    mvc.perform(get("/api/admin/v1/test/site").cookie(cookie).header("X-Site-Id", "701"))
        .andExpect(status().isOk());
    mvc.perform(
            csrfPost("/api/admin/v1/system/auth/mfa/verify")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isUnauthorized());
    var recovered =
        mvc.perform(
                csrfPost("/api/admin/v1/system/auth/mfa/verify")
                    .cookie(cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + recovery + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    mvc.perform(
            csrfPost("/api/admin/v1/system/auth/mfa/verify")
                .cookie(recovered.getResponse().getCookie("__Host-SESSION"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + recovery + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void actualCoreRestoresEventSiteActorAndReplaysFailedPersistentDelivery() {
    jdbc.update(
        "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(2001,701,'product',1) ON"
            + " DUPLICATE KEY UPDATE enabled=1");
    var event =
        new com.b2bweb.api.event.ModuleEnabledEvent(
            events.next(701L, ActorRef.user(801, "编辑")), "product", false);
    observer.failNext();
    new org.springframework.transaction.support.TransactionTemplate(manager)
        .executeWithoutResult(tx -> publisher.publishEvent(event));
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE STATUS='FAILED' AND"
                                + " JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.meta.eventId'))=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(1));
    assertThat(
            jdbc.queryForObject(
                "SELECT JSON_TYPE(JSON_EXTRACT(SERIALIZED_EVENT,'$.meta.siteId')) FROM"
                    + " EVENT_PUBLICATION WHERE"
                    + " JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.meta.eventId'))=?",
                String.class,
                event.eventId()))
        .isEqualTo("STRING");
    incomplete.resubmitIncompletePublicationsOlderThan(java.time.Duration.ZERO);
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(observer.received(event.eventId())).isEqualTo("701:801"));
    assertThat(sites.current()).isEmpty();
  }

  @Test
  void applicationDatasourceCannotExecuteDdl() {
    assertThatThrownBy(() -> jdbc.execute("CREATE TABLE app_must_not_create(id INT PRIMARY KEY)"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }
}
