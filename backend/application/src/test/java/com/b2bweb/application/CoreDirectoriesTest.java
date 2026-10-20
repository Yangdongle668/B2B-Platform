package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
class CoreDirectoriesTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired SiteContexts sites;
  @Autowired SettingsApi settings;
  @Autowired SiteApi siteApi;
  @Autowired UiTextApi texts;
  @Autowired DictApi dicts;
  @Autowired AuditApi audit;
  @Autowired SecretCipher cipher;
  @Autowired UserDirectory users;
  @Autowired PlatformTransactionManager manager;
  final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach
  void seed() {
    for (long site : List.of(711L, 712L))
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'测试站点','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          site,
          "directories-" + site);
    jdbc.update("DELETE FROM sys_setting WHERE site_id IN (711,712) OR setting_key='core.smtp'");
    jdbc.update("DELETE FROM sys_ui_text WHERE site_id IN (711,712)");
  }

  @Test
  void settingsDefaultsAreDetachedAndVersionsAreSiteScoped() {
    sites.runAs(
        711,
        () -> {
          assertThat(settings.meta("inquiry.retention").version()).isZero();
          var original = settings.getJson("inquiry.retention");
          ((tools.jackson.databind.node.ObjectNode) original).put("closedYears", 7);
          assertThat(settings.getJson("inquiry.retention").path("closedYears").asInt())
              .isEqualTo(3);
          assertThat(settings.put("inquiry.retention", original, 0)).isEqualTo(1);
          assertThatThrownBy(() -> settings.put("inquiry.retention", original, 0))
              .isInstanceOf(VersionConflictException.class);
        });
    sites.runAs(
        712,
        () ->
            assertThat(settings.getJson("inquiry.retention").path("closedYears").asInt())
                .isEqualTo(3));
    sites.runAs(
        711,
        () -> {
          settings.reset("inquiry.retention");
          assertThat(settings.meta("inquiry.retention").defaultValue()).isTrue();
        });
  }

  @Test
  void settingsRejectUnknownKeysInvalidValuesAndArchiveWrites() {
    sites.runAs(
        711,
        () -> {
          assertThatThrownBy(() -> settings.getJson("inquiry.typo"))
              .isInstanceOf(BusinessException.class);
          assertThatThrownBy(
                  () -> settings.put("inquiry.messageTranslation", json.booleanNode(true), 0))
              .isInstanceOf(ValidationException.class);
          assertThatThrownBy(
                  () ->
                      settings.put(
                          "inquiry.retention",
                          json.readTree("{\"schemaVersion\":1,\"closedYears\":0,\"spamDays\":30}"),
                          0))
              .isInstanceOf(ValidationException.class);
          jdbc.update("UPDATE sys_site SET status='ARCHIVED' WHERE id=711");
          assertThatThrownBy(
                  () -> settings.put("inquiry.retention", settings.getJson("inquiry.retention"), 0))
              .isInstanceOf(ForbiddenException.class);
        });
  }

  @Test
  void secretSettingsEncryptMaskPreserveAndRollBackAtomically() {
    var value =
        (tools.jackson.databind.node.ObjectNode)
            settings.getSystem("core.smtp", tools.jackson.databind.JsonNode.class);
    value.put("password", "never-store-this-password");
    assertThat(settings.putSystem("core.smtp", value, 0)).isEqualTo(1);
    var saved = settings.getSystem("core.smtp", tools.jackson.databind.JsonNode.class);
    assertThat(saved.path("password").asString()).startsWith("v1:");
    assertThat(cipher.decrypt(saved.path("password").asString()).value())
        .isEqualTo("never-store-this-password");
    value.put("password", "****word");
    settings.putSystem("core.smtp", value, 1);
    assertThat(
            settings.getSystem("core.smtp", tools.jackson.databind.JsonNode.class).path("password"))
        .isEqualTo(saved.path("password"));
    var tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(
        status -> {
          value.put("password", "replacement-password");
          settings.putSystem("core.smtp", value, 2);
          status.setRollbackOnly();
        });
    assertThat(
            settings.getSystem("core.smtp", tools.jackson.databind.JsonNode.class).path("password"))
        .isEqualTo(saved.path("password"));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE diff LIKE '%never-store-this-password%'",
                Integer.class))
        .isZero();
  }

  @Test
  void uiTextsOverrideOnlyRequestedLanguageAndRemainPlainText() {
    jdbc.update(
        "INSERT INTO sys_ui_text(id,site_id,text_key,locale,text_value)"
            + " VALUES(71101,711,'common.home','zh-CN','<b>站点A</b>'),(71102,712,'common.home','zh-CN','站点B')");
    sites.runAs(
        711,
        () -> {
          assertThat(texts.text("common.home", "zh-CN")).contains("<b>站点A</b>");
          assertThat(texts.text("common.home", "en")).isEmpty();
          assertThat(texts.allTexts("zh-CN")).isNotEmpty();
        });
    sites.runAs(712, () -> assertThat(texts.text("common.home", "zh-CN")).contains("站点B"));
  }

  @Test
  void builtinDictionarySyncPreservesCustomItemsAndDisabledLabels() {
    var type =
        new DictTypeDefinition(
            "core.m0Reason",
            "core",
            "测试原因",
            null,
            true,
            List.of(new DictTypeDefinition.DictItemDefinition("archived", "归档", 0)));
    dicts.ensureBuiltin(type);
    long id =
        jdbc.queryForObject(
            "SELECT id FROM sys_dict_type WHERE type_code='core.m0Reason'", Long.class);
    jdbc.update(
        "INSERT INTO sys_dict_item(id,dict_type_id,item_value,label,sort_order,builtin)"
            + " VALUES(71103,?,'custom','自定义',10,0) ON DUPLICATE KEY UPDATE label='自定义'",
        id);
    jdbc.update(
        "UPDATE sys_dict_item SET enabled=0 WHERE dict_type_id=? AND item_value='archived'", id);
    dicts.ensureBuiltin(type);
    assertThat(dicts.items("core.m0Reason")).extracting(DictItem::value).containsExactly("custom");
    assertThat(dicts.label("core.m0Reason", "archived")).contains("归档");
    assertThat(dicts.isValid("core.m0Reason", "archived")).isFalse();
  }

  @Test
  void independentAuditSurvivesCallerRollbackAndMasksCredentials() {
    var outer = new TransactionTemplate(manager);
    long before =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_audit_log WHERE action='m0.independent'", Long.class);
    sites.runAs(
        711,
        () ->
            outer.executeWithoutResult(
                status -> {
                  audit.recordIndependently(
                      new AuditEntry(
                          "core",
                          "m0.independent",
                          null,
                          null,
                          "独立审计",
                          Map.of("apiKey", new AuditChange("old-secret", "new-secret")),
                          AuditResult.FAILURE,
                          true,
                          false));
                  audit.record(AuditEntry.success("core", "m0.joined", "TEST", "711", "应回滚"));
                  status.setRollbackOnly();
                }));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE action='m0.independent'", Long.class))
        .isEqualTo(before + 1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE action='m0.joined'", Long.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT CAST(diff AS CHAR) FROM sys_audit_log WHERE action='m0.independent' ORDER"
                    + " BY id DESC LIMIT 1",
                String.class))
        .doesNotContain("old-secret", "new-secret");
  }

  @Test
  void unknownSiteNeverEnablesCore() {
    assertThat(siteApi.isModuleEnabled(99999999L, "core")).isFalse();
    assertThat(sites.callAs(711, () -> users.find(99999999L))).isEmpty();
  }
}
