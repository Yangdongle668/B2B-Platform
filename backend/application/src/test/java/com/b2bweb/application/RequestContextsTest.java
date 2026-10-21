package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.security.*;
import com.b2bweb.core.context.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Import(RequestContextsTest.Fixtures.class)
class RequestContextsTest {
  static final String TOKEN = "test-" + UUID.randomUUID();

  @org.springframework.test.context.DynamicPropertySource
  static void token(org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("B2BWEB_INTERNAL_TOKENS", () -> TOKEN);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired SiteContexts sites;
  @Autowired UserContexts users;
  @Autowired PlatformTaskDecorator decorator;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired EventMetaFactory meta;
  @Autowired PermissionChecker permissions;

  @TestConfiguration
  static class Fixtures {
    @Bean
    Endpoints contextEndpoints(SiteContexts sites, UserContexts users) {
      return new Endpoints(sites, users);
    }
  }

  @io.swagger.v3.oas.annotations.Hidden
  @RestController
  static class Endpoints {
    final SiteContexts sites;
    final UserContexts users;

    Endpoints(SiteContexts sites, UserContexts users) {
      this.sites = sites;
      this.users = users;
    }

    @GetMapping({
      "/api/public/v1/system/context-fixture",
      "/api/internal/v1/system/context-fixture"
    })
    Map<String, String> current() {
      return Map.of(
          "site",
          sites.current().map(s -> Long.toString(s.siteId())).orElse("none"),
          "origin",
          sites.current().map(s -> s.origin().name()).orElse("none"),
          "locale",
          sites
              .current()
              .map(s -> s.requestLocale() == null ? "none" : s.requestLocale())
              .orElse("none"),
          "actor",
          users.actor().toEventActor(),
          "user",
          Boolean.toString(users.current().isPresent()));
    }
  }

  @BeforeEach
  void seed() {
    for (long site : List.of(761L, 762L)) {
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'上下文测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          site,
          "requests-" + site);
      jdbc.update(
          "INSERT INTO sys_site_domain(id,site_id,domain,type) VALUES(?,?,?,'PRIMARY') ON DUPLICATE"
              + " KEY UPDATE domain=VALUES(domain)",
          site * 10,
          site,
          "s" + site + ".example.test");
      jdbc.update(
          "INSERT INTO sys_site_locale(id,site_id,locale,url_prefix,hreflang,`public`)"
              + " VALUES(?,?,'zh-CN','zh','zh',1) ON DUPLICATE KEY UPDATE id=id",
          site * 10 + 1,
          site);
    }
  }

  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM sys_user WHERE id=76101");
  }

  @Test
  void publicHostAndLocaleAreResolvedAndForgedSiteHeadersCannotOverrideThem() throws Exception {
    mvc.perform(
            get("/api/public/v1/system/context-fixture")
                .header("Host", "S761.EXAMPLE.TEST:443")
                .header("X-Site-Id", "762")
                .header("X-Site-Host", "s762.example.test")
                .param("path", "/zh/example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.site").value("761"))
        .andExpect(jsonPath("$.origin").value("PUBLIC"))
        .andExpect(jsonPath("$.locale").value("zh-CN"))
        .andExpect(jsonPath("$.user").value("false"));
    assertThat(sites.current()).isEmpty();
  }

  @Test
  void signedForwardingRequiresBothTokenAndTrustedSocketPeer() throws Exception {
    mvc.perform(
            get("/api/public/v1/system/context-fixture")
                .header("Host", "s761.example.test")
                .header("X-Site-Host", "s762.example.test")
                .header("X-Internal-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.site").value("762"));
    mvc.perform(
            get("/api/public/v1/system/context-fixture")
                .with(
                    r -> {
                      r.setRemoteAddr("8.8.8.8");
                      return r;
                    })
                .header("Host", "s761.example.test")
                .header("X-Site-Host", "s762.example.test")
                .header("X-Internal-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.site").value("761"));
  }

  @Test
  void unknownHostsAndUnconfiguredLocalesFailWithoutFallback() throws Exception {
    mvc.perform(get("/api/public/v1/system/context-fixture").header("Host", "unknown.example.test"))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/public/v1/system/context-fixture")
                .header("Host", "s761.example.test")
                .param("locale", "en"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CORE_LOCALE_NOT_ENABLED"));
  }

  @Test
  void internalRequestsRequireTokenAndNetworkAndHaveNoLoggedInUser() throws Exception {
    mvc.perform(get("/api/internal/v1/system/context-fixture")).andExpect(status().isForbidden());
    mvc.perform(
            get("/api/internal/v1/system/context-fixture")
                .header("X-Internal-Token", TOKEN)
                .header("X-Site-Id", "761"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.origin").value("INTERNAL"))
        .andExpect(jsonPath("$.actor").value("system"))
        .andExpect(jsonPath("$.user").value("false"));
    mvc.perform(
            get("/api/internal/v1/system/context-fixture")
                .header("X-Internal-Token", TOKEN)
                .with(
                    r -> {
                      r.setRemoteAddr("8.8.8.8");
                      return r;
                    }))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/internal/v1/system/context-fixture").header("X-Internal-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.site").value("none"));
  }

  @Test
  void decoratorCapturesAtSubmissionAndCleansPooledThreadAfterFailure() throws Exception {
    var user = new CurrentUser(76101, "任务发起人", "async@example.test", false, Set.of(), true);
    Runnable[] task = {null};
    sites.runAs(
        761,
        () -> {
          try (var scope = users.open(user)) {
            task[0] =
                decorator.decorate(
                    () -> {
                      assertThat(sites.requireSiteId()).isEqualTo(761);
                      assertThat(users.require().userId()).isEqualTo(76101);
                      throw new IllegalArgumentException("受控任务失败");
                    });
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    try (var executor = Executors.newSingleThreadExecutor()) {
      assertThatThrownBy(() -> executor.submit(task[0]).get())
          .hasCauseInstanceOf(IllegalArgumentException.class);
      executor
          .submit(
              () -> {
                assertThat(sites.current()).isEmpty();
                assertThat(users.current()).isEmpty();
                assertThat(users.actor()).isEqualTo(ActorRef.SYSTEM);
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
              })
          .get();
    }
  }

  @Test
  void domainEventPublicationOutsideTransactionIsRejected() {
    assertThatThrownBy(
            () ->
                publisher.publishEvent(
                    new ModuleEnabledEvent(meta.next(761L, ActorRef.SYSTEM), "tracking", false)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("源事务");
  }

  @Test
  void cachedAsyncUserCannotRetainRevokedAdministratorPermission() throws Exception {
    jdbc.update(
        "INSERT INTO sys_user(id,email,display_name,password_hash,system_admin,status)"
            + " VALUES(76101,'async-context@example.test','任务用户','unused',1,'ACTIVE') ON DUPLICATE"
            + " KEY UPDATE status='ACTIVE',system_admin=1");
    try (var scope =
        users.open(
            new CurrentUser(76101, "任务用户", "async-context@example.test", true, Set.of(), true))) {
      sites.runAs(
          761,
          () -> {
            assertThat(permissions.has("system:module:manage")).isTrue();
            jdbc.update("UPDATE sys_user SET status='DISABLED',system_admin=0 WHERE id=76101");
            assertThat(permissions.has("system:module:manage")).isFalse();
            assertThat(permissions.dataScope("system:module:manage")).isEmpty();
            assertThat(permissions.localeScope("system:module:manage").allows("zh-CN")).isFalse();
          });
    }
  }
}
