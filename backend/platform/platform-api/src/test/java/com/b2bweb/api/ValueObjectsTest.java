package com.b2bweb.api;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.LocaleScope;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ValueObjectsTest {
  @Test
  void contentPermissionKeysMatchTheDeclaredHyphenatedResources() {
    assertThat(
            com.b2bweb.api.security.Permissions.forContent(
                ContentTypes.CASE_STUDY, com.b2bweb.api.security.Permissions.ContentAction.EDIT))
        .isEqualTo("content:case-study:edit");
    assertThat(
            com.b2bweb.api.security.Permissions.forContent(
                ContentTypes.TERM_PAGE, com.b2bweb.api.security.Permissions.ContentAction.EDIT))
        .isEqualTo("content:term-page:edit");
    assertThat(
            com.b2bweb.api.security.Permissions.forContent(
                ContentTypes.GLOBAL_BLOCK, com.b2bweb.api.security.Permissions.ContentAction.EDIT))
        .isEqualTo("content:global-block:edit");
    assertThatThrownBy(
            () ->
                com.b2bweb.api.security.Permissions.forContent(
                    "term-page", com.b2bweb.api.security.Permissions.ContentAction.EDIT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void semverOfficialPrecedenceAndLargeNumericIdentifiers() {
    var order =
        List.of(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0");
    for (int i = 1; i < order.size(); i++)
      assertThat(SemVer.parse(order.get(i))).isGreaterThan(SemVer.parse(order.get(i - 1)));
    assertThat(SemVer.parse("1.0.0-9999999999999999999999999999"))
        .isLessThan(SemVer.parse("1.0.0-alpha"));
    assertThat(SemVer.parse("1.2.3+build.7").compareTo(SemVer.parse("1.2.3"))).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "1.02.3", "1.2.3-01", "1.2.3-", "1.2.3+", "-1.2.3"})
  void invalidSemverRejected(String value) {
    assertThatThrownBy(() -> SemVer.parse(value)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nestedReferencesAndContentLinksAreCollectedWithoutNumericPrecisionLoss() {
    var json =
        JsonMapper.builder()
            .build()
            .readTree(
                "{\"$content\":\"9007199254740993\",\"blocks\":[{\"$media\":\"42\"},{\"$term\":3},{\"href\":\"content://7\"},{\"$media\":\"invalid\"}]}");
    var refs = ReferenceSet.scan(json);
    assertThat(refs.contentIds()).containsExactlyInAnyOrder(9007199254740993L, 7L);
    assertThat(refs.mediaIds()).containsExactly(42L);
    assertThat(refs.termIds()).containsExactly(3L);
    assertThat(refs.merge(ReferenceSet.empty())).isEqualTo(refs);
    assertThatThrownBy(() -> refs.contentIds().add(9L))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void validationSourceViewsShareIssuesAndPreserveAttribution() {
    var result = new ValidationResult();
    result.forSource("media").error("MEDIA_MISSING", "缺少媒体");
    result.forSource("seo").warning("SEO_MISSING", "缺少说明");
    assertThat(result.issues()).hasSize(2);
    assertThat(result.errors().getFirst().source()).isEqualTo("media");
  }

  @Test
  void secretNeverPrintsItsValueAndLocaleScopeHasSeparateFactoryAndAccessor() {
    assertThat(Secret.of("sensitive").toString()).doesNotContain("sensitive");
    assertThat(LocaleScope.all().unrestricted()).isTrue();
    assertThat(LocaleScope.of(Set.of("de")).allows("en")).isFalse();
  }
}
