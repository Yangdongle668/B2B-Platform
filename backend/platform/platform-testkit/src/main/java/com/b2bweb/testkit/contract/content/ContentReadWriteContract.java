package com.b2bweb.testkit.contract.content;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.translation.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * Reusable behavioral contract; providers must supply real fixtures, never assertion-only mirrors.
 */
public abstract class ContentReadWriteContract {
  protected abstract ContentApi api();

  protected abstract void site(long id);

  protected abstract void reset();

  protected abstract void advance(Duration duration);

  @BeforeEach
  void before() {
    reset();
    site(1);
  }

  ContentCreated create() {
    return api().create(new CreateContentCommand("SAMPLE", "zh-CN", "source", null));
  }

  @Test
  void draftAndUnpublishedTranslationAreNeverPublishedReads() {
    var created = create();
    assertThat(api().findPublishedSnapshot(created.contentId(), "zh-CN")).isEmpty();
    assertThat(api().findPublishedSnapshot(created.contentId(), "en")).isEmpty();
    api().publish(new PublishCommand(created.localizationId(), 0, null));
    assertThat(api().findPublishedSnapshot(created.contentId(), "en")).isEmpty();
  }

  @Test
  void crossSiteIdsAreNotVisible() {
    var created = create();
    site(2);
    assertThat(api().findContent(created.contentId())).isEmpty();
    assertThat(api().findLocalization(created.localizationId())).isEmpty();
    assertThat(api().findPublishedSnapshot(created.localizationId())).isEmpty();
  }

  @Test
  void staleEditorCannotOverwriteNewVersion() {
    var created = create();
    api()
        .updateDraftMeta(
            new UpdateDraftMetaCommand(created.localizationId(), 0, "new", null, false));
    assertThatThrownBy(
            () ->
                api()
                    .updateDraftMeta(
                        new UpdateDraftMetaCommand(
                            created.localizationId(), 0, "stale", null, false)))
        .isInstanceOf(VersionConflictException.class);
    assertThat(api().findLocalization(created.localizationId()).orElseThrow().draftTitle())
        .isEqualTo("new");
  }

  @Test
  void sharedSaveAdvancesEachLocaleExactlyOnce() {
    var created = create();
    var target = api().ensureTargetLocalization(created.contentId(), "en");
    api()
        .updateDraftMeta(
            new UpdateDraftMetaCommand(created.localizationId(), 0, "new", null, true));
    assertThat(api().findLocalization(created.localizationId()).orElseThrow().version())
        .isEqualTo(1);
    assertThat(api().findLocalization(target.localizationId()).orElseThrow().version())
        .isEqualTo(1);
  }

  @Test
  void manualAndAutomaticLocaleCreationHaveDistinctModes() {
    var created = create();
    var automatic = api().ensureTargetLocalization(created.contentId(), "en");
    assertThat(automatic.translationMode()).isEqualTo(TranslationMode.AUTO);
    assertThat(automatic.slug()).isNull();
    assertThat(api().ensureTargetLocalization(created.contentId(), "en")).isEqualTo(automatic);
  }

  @Test
  void policyChangesInvalidateOldApprovalVersionsAndNoopPolicyDoesNotMutate() {
    var created = create();
    var policy = api().setTranslationReviewRequirement(created.contentId(), 0, true);
    var l = api().findLocalization(created.localizationId()).orElseThrow();
    assertThat(policy.version()).isEqualTo(1);
    assertThat(l.version()).isEqualTo(1);
    assertThat(api().setTranslationReviewRequirement(created.contentId(), 1, true))
        .isEqualTo(policy);
    assertThatThrownBy(() -> api().publish(new PublishCommand(created.localizationId(), 1, null)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> api().approve(created.localizationId(), 0, null))
        .isInstanceOf(VersionConflictException.class);
  }

  @Test
  void snapshotRemainsImmutableWhenDraftChangesOrReturnedJsonIsMutated() {
    var created = create();
    api().publish(new PublishCommand(created.localizationId(), 0, null));
    var first = api().findPublishedSnapshot(created.localizationId()).orElseThrow();
    ((tools.jackson.databind.node.ObjectNode) first.payload()).put("injected", true);
    assertThat(
            api()
                .findPublishedSnapshot(created.localizationId())
                .orElseThrow()
                .payload()
                .has("injected"))
        .isFalse();
    api()
        .updateDraftMeta(
            new UpdateDraftMetaCommand(created.localizationId(), 1, "draft changed", null, false));
    assertThat(
            api()
                .findPublishedSnapshot(created.localizationId())
                .orElseThrow()
                .payload()
                .path("card")
                .path("title")
                .asString())
        .isEqualTo("source");
  }

  @Test
  void trashRequiresUnpublishingAndRestoreDoesNotRepublish() {
    var created = create();
    api().publish(new PublishCommand(created.localizationId(), 0, null));
    assertThatThrownBy(() -> api().trash(created.contentId()))
        .isInstanceOf(BusinessException.class);
    api().unpublishAll(created.contentId(), UnpublishStrategy.GONE, null);
    api().trash(created.contentId());
    assertThat(api().findContent(created.contentId()).orElseThrow().trashed()).isTrue();
    api().restore(created.contentId());
    assertThat(api().findPublishedSnapshot(created.localizationId())).isEmpty();
  }

  @Test
  void scanIsStableByLocalizationAndRejectsAnotherSiteCursor() {
    var a = create();
    var b = create();
    api().publish(new PublishCommand(a.localizationId(), 0, null));
    api().publish(new PublishCommand(b.localizationId(), 0, null));
    var query = new PublishedScan("zh-CN", Set.of("SAMPLE"), false, null);
    var first = api().scanPublished(query, CursorRequest.first(1));
    assertThat(first.items()).hasSize(1);
    assertThat(first.hasMore()).isTrue();
    var second = api().scanPublished(query, new CursorRequest(first.nextCursor(), 1));
    assertThat(second.items().getFirst().localizationId())
        .isGreaterThan(first.items().getFirst().localizationId());
    site(2);
    assertThatThrownBy(() -> api().scanPublished(query, new CursorRequest(first.nextCursor(), 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void previewIsBoundToSiteAndExpires() {
    var created = create();
    var token =
        api()
            .createPreviewToken(
                created.localizationId(), new PreviewMode.Draft(), Duration.ofMinutes(10));
    assertThat(api().resolvePreviewToken(token.token())).isPresent();
    site(2);
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    site(1);
    advance(Duration.ofMinutes(11));
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
  }
}
