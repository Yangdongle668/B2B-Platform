package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class CoreDoublesTest {
  @Test
  void settingsUseCanonicalSchemasAndDetachedVersions() {
    var p = TestPlatform.standard().useSite(1);
    var value = p.settingsApi().getJson("inquiry.retention");
    assertThat(value.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(p.settingsApi().put("inquiry.retention", value, 0)).isEqualTo(1);
    assertThatThrownBy(() -> p.settingsApi().put("inquiry.retention", value, 0))
        .isInstanceOf(VersionConflictException.class);
    p.useSite(2);
    assertThat(p.settingsApi().meta("inquiry.retention").version()).isZero();
    assertThatThrownBy(
            () ->
                p.settingsApi()
                    .put("inquiry.retention", JsonMapper.builder().build().booleanNode(true), 0))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void settingsSecretsAreEncryptedAndMasksPreserveValue() {
    var p = TestPlatform.standard();
    var value =
        (tools.jackson.databind.node.ObjectNode)
            p.settingsApi().getSystem("core.smtp", tools.jackson.databind.JsonNode.class);
    value.put("password", "a-test-password-value");
    p.settingsApi().putSystem("core.smtp", value, 0);
    String encrypted =
        p.settingsApi()
            .getSystem("core.smtp", tools.jackson.databind.JsonNode.class)
            .path("password")
            .asString();
    assertThat(encrypted).doesNotContain("a-test-password-value");
    assertThat(p.secretCipher().decrypt(encrypted).value()).isEqualTo("a-test-password-value");
    value.put("password", "****value");
    p.settingsApi().putSystem("core.smtp", value, 1);
    assertThat(
            p.settingsApi()
                .getSystem("core.smtp", tools.jackson.databind.JsonNode.class)
                .path("password")
                .asString())
        .isEqualTo(encrypted);
  }

  @Test
  void dictionarySyncKeepsCustomItemsAndDisabledBuiltinHistory() {
    var p = TestPlatform.empty();
    var definition =
        new DictTypeDefinition(
            "core.reason",
            "core",
            "原因",
            null,
            true,
            List.of(new DictTypeDefinition.DictItemDefinition("closed", "关闭", 0)));
    p.dictApi().ensureBuiltin(definition);
    p.dictApi().seed("core.reason", new DictItem("closed", "关闭", 0, false, true, null));
    p.dictApi().seed("core.reason", new DictItem("custom", "自定义", 1, true, false, null));
    p.dictApi().ensureBuiltin(definition);
    assertThat(p.dictApi().items("core.reason"))
        .extracting(DictItem::value)
        .containsExactly("custom");
    assertThat(p.dictApi().label("core.reason", "closed")).contains("关闭");
  }

  @Test
  void userDirectoryIncludesActiveAdminsAndRespectsLocaleGrants() {
    var p = TestPlatform.standard().useSite(1);
    p.userDirectory().seed(new UserSummary(11, "用户", "user@example.test", true), false, Set.of(1L));
    p.userDirectory().seed(new UserSummary(12, "管理员", "admin@example.test", true), true, Set.of());
    p.permissionChecker().grant(11, "content:edit", DataScope.OWN, LocaleScope.of(Set.of("zh-CN")));
    assertThat(p.userDirectory().usersWithPermission("content:edit", "en"))
        .extracting(UserSummary::userId)
        .containsExactly(12L);
    assertThat(p.currentUserProvider().current()).isEmpty();
    assertThat(p.permissionChecker().userHas(12, "any:permission")).isTrue();
    p.useSite(2);
    assertThat(p.userDirectory().isActiveMember(11)).isFalse();
  }

  @Test
  void auditMasksSensitiveChangesAndCipherDetectsTampering() {
    var p = TestPlatform.standard().useSite(1);
    p.auditApi()
        .record(
            new AuditEntry(
                "core",
                "config.update",
                null,
                null,
                "更新",
                Map.of("apiKey", new AuditChange("before", "after")),
                AuditResult.SUCCESS,
                true,
                false));
    assertThat(p.auditApi().records().getFirst().entry().diff().get("apiKey"))
        .isEqualTo(AuditChange.masked());
    String envelope = p.secretCipher().encrypt(Secret.of("plaintext"));
    assertThatThrownBy(
            () -> p.secretCipher().decrypt(envelope.substring(0, envelope.length() - 4) + "AAAA"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void missingProfileLanguageHasNoFallbackText() {
    var p = TestPlatform.standard().useSite(1);
    assertThat(p.siteProfileApi().get("en").companyName()).isNull();
    assertThat(p.siteProfileApi().get("en").locale()).isEqualTo("en");
  }
}
