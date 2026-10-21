package com.b2bweb.testkit.contract.content;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

/** Preview security semantics shared by the double and the future real content provider. */
public abstract class ContentPreviewContract {
  protected static final long ISSUER = 117;

  protected abstract ContentApi api();

  protected abstract void reset();

  protected abstract void site(long id);

  protected abstract void login();

  protected abstract void anonymous();

  protected abstract void issuer(boolean active, boolean member);

  protected abstract void editGrant(boolean enabled, Set<String> locales);

  protected abstract void advance(Duration duration);

  protected abstract Instant now();

  protected abstract JsonNode layout(String marker);

  protected abstract void draftLayout(long localization, JsonNode layout);

  protected abstract long eventCount();

  @BeforeEach
  void prepare() {
    reset();
    site(1);
    issuer(true, true);
    editGrant(true, Set.of("zh-CN", "en"));
    anonymous();
  }

  private ContentCreated create() {
    return api().create(new CreateContentCommand("SAMPLE", "zh-CN", "preview source", null));
  }

  private PreviewToken editor(long id) {
    return api()
        .createPreviewToken(
            id, new PreviewMode.Draft(), PreviewPurpose.EDITOR, Duration.ofMinutes(2));
  }

  @Test
  public void explicitPurposeHasIndependentLifetimeLimits() {
    login();
    var content = create();
    for (var purpose : PreviewPurpose.values()) {
      Duration max =
          switch (purpose) {
            case PREVIEW -> Duration.ofHours(1);
            case SHARE -> Duration.ofDays(7);
            case EDITOR -> Duration.ofHours(12);
          };
      var token =
          api().createPreviewToken(content.localizationId(), new PreviewMode.Draft(), purpose, max);
      assertThat(api().resolvePreviewToken(token.token()).orElseThrow().purpose())
          .isEqualTo(purpose);
      assertThatThrownBy(
              () ->
                  api()
                      .createPreviewToken(
                          content.localizationId(),
                          new PreviewMode.Draft(),
                          purpose,
                          max.plusSeconds(1)))
          .isInstanceOf(BusinessException.class);
      assertThatThrownBy(
              () ->
                  api()
                      .createPreviewToken(
                          content.localizationId(),
                          new PreviewMode.Draft(),
                          purpose,
                          Duration.ZERO))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Test
  public void defaultsAndCompatibilityNeverInferEditorPurpose() {
    login();
    var content = create();
    var before = now();
    var editor =
        api()
            .createPreviewToken(
                content.localizationId(), new PreviewMode.Draft(), PreviewPurpose.EDITOR, null);
    assertThat(editor.expiresAt()).isEqualTo(before.plus(Duration.ofHours(12)));
    var preview = api().createPreviewToken(content.localizationId(), new PreviewMode.Draft(), null);
    assertThat(preview.expiresAt()).isEqualTo(before.plus(Duration.ofHours(1)));
    var legacyLong =
        api()
            .createPreviewToken(
                content.localizationId(), new PreviewMode.Draft(), Duration.ofHours(12));
    assertThat(api().resolvePreviewToken(legacyLong.token()).orElseThrow().purpose())
        .isEqualTo(PreviewPurpose.SHARE);
    assertThat(api().resolvePreviewToken(preview.token()).orElseThrow().purpose())
        .isEqualTo(PreviewPurpose.PREVIEW);
  }

  @Test
  public void editorRequiresLoggedInAuthorizedIssuerAndDraftMode() {
    var content = create();
    assertThatThrownBy(() -> editor(content.localizationId()))
        .isInstanceOf(PlatformException.class);
    login();
    editGrant(false, Set.of());
    assertThatThrownBy(() -> editor(content.localizationId()))
        .isInstanceOf(ForbiddenException.class);
    editGrant(true, Set.of("en"));
    assertThatThrownBy(() -> editor(content.localizationId()))
        .isInstanceOf(ForbiddenException.class);
    editGrant(true, Set.of("zh-CN"));
    var target = api().resolvePreviewToken(editor(content.localizationId()).token()).orElseThrow();
    assertThat(target.issuedBy()).isEqualTo(ISSUER);
    api()
        .publish(
            new PublishCommand(
                content.localizationId(),
                api().findLocalization(content.localizationId()).orElseThrow().version(),
                null));
    assertThatThrownBy(
            () ->
                api()
                    .createPreviewToken(
                        content.localizationId(),
                        new PreviewMode.Revision(1),
                        PreviewPurpose.EDITOR,
                        Duration.ofMinutes(2)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                api()
                    .assemblePreview(
                        new PreviewRequest(
                            content.localizationId(),
                            new PreviewMode.Revision(1),
                            Map.of(),
                            PreviewPurpose.EDITOR)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  public void editorIsRecheckedWithoutCurrentLoginAfterRoleLocaleMembershipAndAccountChanges() {
    login();
    var token = editor(create().localizationId());
    anonymous();
    assertThat(api().resolvePreviewToken(token.token())).isPresent();
    editGrant(false, Set.of());
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    editGrant(true, Set.of("en"));
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    editGrant(true, Set.of("zh-CN"));
    issuer(true, false);
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    issuer(false, true);
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
  }

  @Test
  public void editorRejectsCrossSiteTamperingAndTheExactExpiryBoundary() {
    login();
    var token = editor(create().localizationId());
    assertThat(api().resolvePreviewToken(token.token() + "changed")).isEmpty();
    site(2);
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    site(1);
    advance(Duration.ofMinutes(2));
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
  }

  @Test
  public void shareAndPreviewCannotCarryUnsavedOverrides() {
    var content = create();
    var overrides = Map.of("layout", layout("override"));
    for (var purpose : List.of(PreviewPurpose.PREVIEW, PreviewPurpose.SHARE))
      assertThatThrownBy(
              () ->
                  api()
                      .assemblePreview(
                          new PreviewRequest(
                              content.localizationId(),
                              new PreviewMode.Draft(),
                              overrides,
                              purpose)))
          .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                api()
                    .assemblePreview(
                        new PreviewRequest(
                            content.localizationId(), new PreviewMode.Draft(), overrides)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  public void editorRejectsReservedPartsUnknownPartsAndInvalidLayout() {
    login();
    var content = create();
    for (String key : List.of("card", "content", "terms", "unknown"))
      assertThatThrownBy(
              () ->
                  api()
                      .assemblePreview(
                          new PreviewRequest(
                              content.localizationId(),
                              new PreviewMode.Draft(),
                              Map.of(key, layout("bad")),
                              PreviewPurpose.EDITOR)))
          .isInstanceOf(BusinessException.class);
    var invalid = tools.jackson.databind.json.JsonMapper.builder().build().readTree("\"invalid\"");
    assertThatThrownBy(
            () ->
                api()
                    .assemblePreview(
                        new PreviewRequest(
                            content.localizationId(),
                            new PreviewMode.Draft(),
                            Map.of("layout", invalid),
                            PreviewPurpose.EDITOR)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  public void unsavedEditorMergeDoesNotChangeDraftPublishedSnapshotOrEvents() {
    login();
    var content = create();
    var saved = layout("saved");
    var unsaved = layout("unsaved");
    draftLayout(content.localizationId(), saved);
    api()
        .publish(
            new PublishCommand(
                content.localizationId(),
                api().findLocalization(content.localizationId()).orElseThrow().version(),
                null));
    long events = eventCount();
    var preview =
        api()
            .assemblePreview(
                new PreviewRequest(
                    content.localizationId(),
                    new PreviewMode.Draft(),
                    Map.of("layout", unsaved),
                    PreviewPurpose.EDITOR));
    assertThat(preview.payload().path("parts").path("layout")).isEqualTo(unsaved);
    assertThat(
            api()
                .findPublishedSnapshot(content.localizationId())
                .orElseThrow()
                .payload()
                .path("parts")
                .path("layout"))
        .isEqualTo(saved);
    assertThat(
            api()
                .assemblePreview(
                    new PreviewRequest(content.localizationId(), new PreviewMode.Draft(), Map.of()))
                .payload()
                .path("parts")
                .path("layout"))
        .isEqualTo(saved);
    assertThat(eventCount()).isEqualTo(events);
  }

  @Test
  public void automaticTargetLocaleIsReadableButCannotAcceptLayoutOverrides() {
    login();
    var content = create();
    var target = api().ensureTargetLocalization(content.contentId(), "en");
    assertThat(
            api()
                .assemblePreview(
                    new PreviewRequest(
                        target.localizationId(),
                        new PreviewMode.Draft(),
                        Map.of(),
                        PreviewPurpose.EDITOR)))
        .isNotNull();
    assertThatThrownBy(
            () ->
                api()
                    .assemblePreview(
                        new PreviewRequest(
                            target.localizationId(),
                            new PreviewMode.Draft(),
                            Map.of("layout", layout("override")),
                            PreviewPurpose.EDITOR)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  public void removedContentOrLocalizationInvalidatesPreviewInsteadOfThrowing() {
    var content = create();
    var token =
        api()
            .createPreviewToken(
                content.localizationId(), new PreviewMode.Draft(), Duration.ofMinutes(10));
    api().trash(content.contentId());
    assertThat(api().resolvePreviewToken(token.token())).isEmpty();
    var second = create();
    var locale = api().ensureTargetLocalization(second.contentId(), "en");
    var localized =
        api()
            .createPreviewToken(
                locale.localizationId(), new PreviewMode.Draft(), Duration.ofMinutes(10));
    api().deleteLocalization(locale.localizationId());
    assertThat(api().resolvePreviewToken(localized.token())).isEmpty();
  }

  @Test
  public void nonexistentRevisionIsAResourceError() {
    var content = create();
    assertThatThrownBy(
            () ->
                api()
                    .createPreviewToken(
                        content.localizationId(),
                        new PreviewMode.Revision(99),
                        Duration.ofMinutes(10)))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(
            () ->
                api()
                    .assemblePreview(
                        new PreviewRequest(
                            content.localizationId(), new PreviewMode.Revision(99), Map.of())))
        .isInstanceOf(NotFoundException.class);
  }
}
