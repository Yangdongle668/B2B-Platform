package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.testkit.fixture.ManualClock;
import com.b2bweb.testkit.port.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ExtraPortsTest {
  @Test
  void cdnRecordsOnlySuccessfulPurgesAndRejectsCrossSite() {
    var fake = new FakeCdnPurger();
    fake.purgeTags(List.of(CacheTag.site(1)));
    assertThatThrownBy(() -> fake.purgeTags(List.of(CacheTag.site(1), CacheTag.site(2))))
        .isInstanceOf(IllegalArgumentException.class);
    fake.available(false);
    assertThatThrownBy(() -> fake.purgeTags(List.of(CacheTag.site(1))))
        .isInstanceOf(ExternalServiceException.class);
    assertThat(fake.tagPurges()).hasSize(1);
  }

  @Test
  void geoAndMxRequireExplicitFixturesAndNeverResolveDns() {
    var geo = new FakeGeoIpResolver();
    geo.seed("8.8.8.8", new GeoLocation("US", null, "NA"));
    geo.seed("127.0.0.1", new GeoLocation("US", null, "NA"));
    assertThat(geo.resolve("8.8.8.8")).isPresent();
    assertThat(geo.resolve("127.0.0.1")).isEmpty();
    assertThat(geo.resolve("localhost")).isEmpty();
    var mx = new FakeMxRecordChecker(new ManualClock());
    assertThat(mx.check("unknown.example.test").status()).isEqualTo(MxStatus.UNKNOWN);
  }

  @Test
  void webhookSignsExactBodyAndKeepsNonSuccessResponse() {
    var fake = new FakeWebhookTransport(new ManualClock());
    URI url = URI.create("https://hooks.example.test/events");
    fake.seed(url, "8.8.8.8", 503);
    var result =
        fake.post(
            new WebhookDelivery(
                url,
                "test.created",
                "123",
                "{\"untrusted\":true}",
                Secret.of("test-fixture-only"),
                null));
    assertThat(result.status()).isEqualTo(503);
    assertThat(fake.deliveries().getFirst().headers().get("X-B2B-Signature"))
        .startsWith("v1=")
        .hasSize(67);
    assertThatThrownBy(
            () ->
                fake.post(
                    new WebhookDelivery(
                        URI.create("http://localhost/test"),
                        "test.created",
                        "1",
                        "{}",
                        Secret.of("fixture"),
                        null)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void remoteFileFixturesEnforceHostIpTypeSizeAndClose() throws Exception {
    var fake = new FakeRemoteFileFetcher();
    URI url = URI.create("https://assets.example.test/photo.jpg");
    fake.seed(url, "image/jpeg", new byte[] {1, 2}, "8.8.8.8");
    var file = fake.fetch(RemoteFetchRequest.image(url, Set.of("*.example.test")));
    try (var in = file.open()) {
      assertThat(in.readAllBytes()).isEqualTo(new byte[] {1, 2});
    }
    file.close();
    assertThatThrownBy(file::open).isInstanceOf(IllegalStateException.class);
    fake.seed(url, "image/jpeg", new byte[] {1, 2}, "169.254.169.254");
    assertThatThrownBy(
            () -> fake.fetch(RemoteFetchRequest.image(url, Set.of("assets.example.test"))))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void searchUsesDetachedDocumentsFiltersFacetsAndGenerationScopedCursors() {
    var fake = new InMemorySearchEngine();
    var settings =
        new SearchIndexSettings(
            "id",
            List.of("title"),
            List.of("type"),
            List.of("rank"),
            List.of(),
            null,
            Map.of(),
            Set.of());
    fake.ensureIndex("s1_g1", settings);
    fake.ensureIndex("s1_g2", settings);
    var json = JsonMapper.builder().build();
    var one =
        json.createObjectNode()
            .put("id", "1")
            .put("type", "PRODUCT")
            .put("title", "precision")
            .put("rank", 2);
    var two =
        json.createObjectNode()
            .put("id", "2")
            .put("type", "ARTICLE")
            .put("title", "precision")
            .put("rank", 1);
    fake.upsert("s1_g1", List.of(one, two));
    one.put("title", "mutated");
    var response =
        fake.search(
            "s1_g1",
            new SearchRequest(
                "precision",
                SearchFilters.in("type", List.of("PRODUCT", "ARTICLE")),
                List.of("type"),
                List.of("rank:asc"),
                0,
                10,
                List.of(),
                List.of()));
    assertThat(response.hits()).extracting(d -> d.path("id").asString()).containsExactly("2", "1");
    assertThat(response.facetDistribution().get("type")).containsEntry("PRODUCT", 1L);
    String cursor = fake.scanDocuments("s1_g1", CursorRequest.first(1)).nextCursor();
    assertThatThrownBy(() -> fake.scanDocuments("s1_g2", new CursorRequest(cursor, 1)))
        .isInstanceOf(IllegalArgumentException.class);
    fake.freeze("s1_g1");
    assertThatThrownBy(() -> fake.delete("s1_g1", List.of("1")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void failedSearchMutationCannotChangePreviouslyAcceptedDocuments() {
    var fake = new InMemorySearchEngine();
    fake.ensureIndex(
        "s1_g1",
        new SearchIndexSettings(
            "id", List.of("title"), List.of(), List.of(), List.of(), null, Map.of(), Set.of()));
    fake.failNextMutation();
    long task =
        fake.upsert(
            "s1_g1", List.of(JsonMapper.builder().build().createObjectNode().put("id", "1")));
    assertThat(fake.await(task, Duration.ZERO).state()).isEqualTo(SearchTaskState.FAILED);
    assertThat(fake.count("s1_g1")).isZero();
  }
}
