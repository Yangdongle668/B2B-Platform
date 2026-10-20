package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.url.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class RemainingServicesTest {
  static void transaction(Runnable action) {
    var manager =
        new AbstractPlatformTransactionManager() {
          protected Object doGetTransaction() {
            return new Object();
          }

          protected void doBegin(Object tx, TransactionDefinition definition) {}

          protected void doCommit(DefaultTransactionStatus status) {}

          protected void doRollback(DefaultTransactionStatus status) {}
        };
    new TransactionTemplate(manager).executeWithoutResult(status -> action.run());
  }

  @Test
  void routeHistoryTracksCurrentLocalizationAndDoesNotCreateRedirectChains() {
    var p = TestPlatform.standard().useSite(1);
    p.urlApi().applyRoute(new ApplyRouteCommand(1, 11, "SAMPLE", "zh-CN", "/zh/first", null));
    p.urlApi().applyRoute(new ApplyRouteCommand(1, 11, "SAMPLE", "zh-CN", "/zh/second", null));
    p.urlApi().applyRoute(new ApplyRouteCommand(1, 11, "SAMPLE", "zh-CN", "/zh/third", null));
    assertThat(p.urlApi().resolve("/zh/first"))
        .isEqualTo(new RouteResolution.Redirect(301, "/zh/third", RedirectReason.REDIRECT_RULE));
    p.urlApi().applyRoute(new ApplyRouteCommand(2, 22, "SAMPLE", "zh-CN", "/zh/first", null));
    assertThat(p.urlApi().resolve("/zh/first")).isInstanceOf(RouteResolution.Content.class);
    p.useSite(2);
    assertThat(p.urlApi().pathOf(11)).isEmpty();
  }

  @Test
  void routeRetirementAndLocaleVisibilityAreExplicit() {
    var p = TestPlatform.standard().useSite(1);
    p.urlApi().applyRoute(new ApplyRouteCommand(1, 11, "SAMPLE", "zh-CN", "/zh/first", null));
    p.urlApi().retireRoute(new RetireRouteCommand(11, RetireMode.GONE, null));
    assertThat(p.urlApi().resolve("/zh/first")).isInstanceOf(RouteResolution.Gone.class);
    p.urlApi().applyRoute(new ApplyRouteCommand(2, 22, "SAMPLE", "en", "/en/second", null));
    assertThat(p.urlApi().resolve("/en/second")).isInstanceOf(RouteResolution.NotFound.class);
    assertThat(p.urlApi().generateSlug("精密设备", "zh-CN")).isEqualTo("jing-mi-she-bei");
  }

  @Test
  void reservationRechecksPathUniquenessAndRedirectLoopsFail() {
    var p = TestPlatform.standard().useSite(1);
    p.urlApi().applyRoute(new ApplyRouteCommand(1, 11, "SAMPLE", "zh-CN", "/zh/first", null));
    var request = new RouteRequest(2, 22, "SAMPLE", "zh-CN", "first", "/{slug}", RouteHints.none());
    assertThat(p.urlApi().reservePath(request)).isInstanceOf(PathReservation.Conflict.class);
    assertThat(p.urlApi().suggestAvailableSlug(request)).isEqualTo("first-2");
    p.urlApi()
        .addRedirect(
            new CreateRedirectCommand(
                "/zh/a",
                RedirectMatchType.EXACT,
                RedirectTargetKind.PATH,
                null,
                "/zh/b",
                301,
                RedirectOrigin.MANUAL,
                null));
    assertThatThrownBy(
            () ->
                p.urlApi()
                    .addRedirect(
                        new CreateRedirectCommand(
                            "/zh/b",
                            RedirectMatchType.EXACT,
                            RedirectTargetKind.PATH,
                            null,
                            "/zh/a",
                            301,
                            RedirectOrigin.MANUAL,
                            null)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void taxonomyAncestorsAndMissingTranslationNeverCrossSites() {
    var p = TestPlatform.standard().useSite(1);
    p.taxonomyApi()
        .seed(
            new VocabularyInfo(
                "category", "分类", true, true, true, true, Set.of("SAMPLE"), true, 0));
    p.taxonomyApi().seed(new TermInfo(1, "category", null, "/1/", 0, 0, null, "电池"), Map.of());
    p.taxonomyApi()
        .seed(
            new TermInfo(2, "category", 1L, "/1/2/", 1, 0, null, "锂电池"),
            Map.of("en", "Lithium battery"));
    assertThat(p.taxonomyApi().ancestors(2)).extracting(TermInfo::termId).containsExactly(1L);
    assertThat(p.taxonomyApi().isSameOrDescendant(2, 1)).isTrue();
    assertThat(p.taxonomyApi().termViews(List.of(1L), "en").get(1L).name()).isNull();
    p.useSite(2);
    assertThat(p.taxonomyApi().findTerm(1)).isEmpty();
  }

  @Test
  void mediaSoftDeletePreservesObjectAndProtectedReferencesBlockForce() {
    var p = TestPlatform.standard().useSite(1);
    byte[] value = "%PDF-1.7 test".getBytes();
    var uploaded =
        p.mediaApi()
            .upload(
                new UploadCommand(
                    "file.pdf",
                    null,
                    value.length,
                    Visibility.PRIVATE,
                    UploadSource.FORM_ATTACHMENT,
                    null,
                    false,
                    Set.of(),
                    null),
                new ByteArrayInputStream(value))
            .media();
    p.mediaApi().status(uploaded.mediaId(), MediaStatus.READY);
    transaction(
        () ->
            p.referenceWriteApi()
                .replaceMedia(
                    new MediaReferenceOwner("CONTENT", 1, "zh-CN", ReferenceStage.PUBLISHED),
                    List.of(new MediaReference(uploaded.mediaId(), "product", "documents[0]"))));
    assertThatThrownBy(() -> p.mediaApi().delete(uploaded.mediaId(), true))
        .isInstanceOf(BusinessException.class);
    transaction(
        () ->
            p.referenceWriteApi()
                .replaceMedia(
                    new MediaReferenceOwner("CONTENT", 1, "zh-CN", ReferenceStage.PUBLISHED),
                    List.of()));
    p.mediaApi().delete(uploaded.mediaId(), false);
    assertThat(p.mediaApi().find(uploaded.mediaId())).isEmpty();
    p.mediaApi().restore(uploaded.mediaId());
    assertThat(p.mediaApi().find(uploaded.mediaId())).isPresent();
  }

  @Test
  void gatedTicketRequiresPublishedReferenceAndIsInvalidatedByGateVersion() {
    var p = TestPlatform.standard().useSite(1);
    byte[] value = "%PDF-1.7 test".getBytes();
    long id =
        p.mediaApi()
            .upload(
                new UploadCommand(
                    "file.pdf",
                    null,
                    value.length,
                    Visibility.PRIVATE,
                    UploadSource.FORM_ATTACHMENT,
                    null,
                    false,
                    Set.of(),
                    null),
                new ByteArrayInputStream(value))
            .media()
            .mediaId();
    p.mediaApi().status(id, MediaStatus.READY);
    p.mediaApi().setDownloadGate(id, "lead");
    DownloadTicket[] ticket = {null};
    transaction(
        () -> {
          p.referenceWriteApi()
              .replaceMedia(
                  new MediaReferenceOwner("CONTENT", 1, "zh-CN", ReferenceStage.PUBLISHED),
                  List.of(new MediaReference(id, "product", "documents[0]")));
          ticket[0] =
              p.mediaApi()
                  .issueGatedDownload(
                      new GatedDownloadCommand(
                          123, id, "lead", 1, "zh-CN", p.clock().instant().plusSeconds(3600)));
        });
    assertThat(p.mediaApi().validateTicket(ticket[0])).isTrue();
    p.mediaApi().setDownloadGate(id, "other");
    assertThat(p.mediaApi().validateTicket(ticket[0])).isFalse();
  }

  @Test
  void projectionCaptureAndMutexJoinTheSourceTransaction() {
    var p = TestPlatform.standard().useSite(1);
    assertThatThrownBy(() -> p.transactionMutexApi().lock("content.route_tree"))
        .isInstanceOf(IllegalStateException.class);
    transaction(
        () -> {
          assertThat(p.transactionMutexApi().advance("content.route_tree")).isEqualTo(1);
          p.projectionChangeApi().searchChanged(Set.of("zh-CN"), Set.of(1L), false);
          p.projectionChangeApi().searchChanged(Set.of("en"), Set.of(1L, 2L), false);
          assertThat(p.projectionChangeApi().captures()).isEmpty();
        });
    assertThat(p.projectionChangeApi().captures()).hasSize(1);
    assertThat(p.projectionChangeApi().captures().getFirst().ids())
        .containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void failedCaptureRollsBackDraftSettingAndGeneration() {
    var p = TestPlatform.standard().useSite(1);
    p.projectionChangeApi().failCapture(true);
    assertThatThrownBy(
            () ->
                transaction(
                    () -> {
                      p.settingsApi()
                          .put(
                              "inquiry.retention", p.settingsApi().getJson("inquiry.retention"), 0);
                      p.transactionMutexApi().advance("content.route_tree");
                      p.projectionChangeApi().searchChanged(Set.of("en"), Set.of(1L), false);
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(p.settingsApi().meta("inquiry.retention").version()).isZero();
    transaction(() -> assertThat(p.transactionMutexApi().lock("content.route_tree")).isZero());
  }
}
