package com.b2bweb.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.mail.SmtpMailTransport;
import com.b2bweb.infrastructure.search.MeilisearchClient;
import com.b2bweb.infrastructure.storage.S3ObjectStorage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.json.JsonMapper;

/** Required local integration services: absence fails the build instead of skipping contracts. */
class LocalAdaptersTest {
  static S3Client s3;
  static S3Presigner signer;
  static ObjectStorage storage;
  static final JsonMapper JSON = JsonMapper.builder().build();

  static String required(String name) {
    String value = System.getenv(name);
    assertThat(value).as(name + " is required by the verification runner").isNotBlank();
    return value;
  }

  @BeforeAll
  static void connect() {
    URI endpoint = URI.create(required("B2BWEB_S3_ENDPOINT"));
    var credentials =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                required("B2BWEB_S3_ACCESS_KEY"), required("B2BWEB_S3_SECRET_KEY")));
    s3 =
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .forcePathStyle(true)
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .proxyConfiguration(
                        software.amazon.awssdk.http.urlconnection.ProxyConfiguration.builder()
                            .addNonProxyHost("storage")
                            .build()))
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofSeconds(120))
                    .retryPolicy(b -> b.numRetries(0))
                    .build())
            .build();
    signer =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    for (String bucket : List.of("public-assets", "private-assets")) {
      try { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); }
      catch (BucketAlreadyOwnedByYouException alreadyOwned) { /* Shared contract suites reuse the same disposable service. */ }
    }
    storage =
        new S3ObjectStorage(
            s3,
            signer,
            "public-assets",
            "private-assets",
            URI.create("https://cdn.example.test/assets/"));
  }

  @AfterAll
  static void close() {
    if (signer != null) signer.close();
    if (s3 != null) s3.close();
  }

  @Test
  void objectRoundTripSeparatesVisibilityAndDeleteIsIdempotent() throws Exception {
    var key = new StorageKey("1/test/manual.txt");
    byte[] value = "中文说明书".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    storage.put(
        key, new ByteArrayInputStream(value), value.length, "text/plain", Visibility.PRIVATE);
    assertThat(storage.stat(key, Visibility.PRIVATE).orElseThrow().size()).isEqualTo(value.length);
    assertThat(storage.stat(key, Visibility.PUBLIC)).isEmpty();
    try (var input = storage.get(key, Visibility.PRIVATE)) {
      assertThat(input.readAllBytes()).isEqualTo(value);
    }
    storage.delete(key, Visibility.PRIVATE);
    storage.delete(key, Visibility.PRIVATE);
    assertThatThrownBy(() -> storage.get(key, Visibility.PRIVATE))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void lengthMismatchNeverLeavesAPartialObject() {
    var key = new StorageKey("1/test/wrong.txt");
    for (long size : List.of(1L, 3L))
      assertThatThrownBy(
              () ->
                  storage.put(
                      key,
                      new ByteArrayInputStream(new byte[2]),
                      size,
                      "text/plain",
                      Visibility.PUBLIC))
          .isInstanceOf(IllegalArgumentException.class);
    assertThat(storage.stat(key, Visibility.PUBLIC)).isEmpty();
  }

  @Test
  void presignedPrivateGetAndDownloadNameWork() throws Exception {
    var key = new StorageKey("1/test/private.txt");
    storage.put(key, new ByteArrayInputStream(new byte[] {7}), 1, "text/plain", Visibility.PRIVATE);
    var url = storage.presignedGet(key, Duration.ofMinutes(1), "产品 说明.pdf");
    try (var http = HttpClient.newHttpClient()) {
      var result =
          http.send(
              HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
      assertThat(result.statusCode()).isEqualTo(200);
      assertThat(result.headers().firstValue("Content-Disposition").orElseThrow())
          .contains("filename*=UTF-8''", "%20");
    }
    assertThat(storage.publicUrl(key))
        .isEqualTo(URI.create("https://cdn.example.test/assets/1/test/private.txt"));
    assertThatThrownBy(() -> storage.presignedGet(key, Duration.ofSeconds(59), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static MailMessage message(Map<String, String> headers) {
    return new MailMessage(
        "<m0-adapter@example.test>",
        new MailAddress("sender@example.test", "平台"),
        List.of(new MailAddress("reader@example.test", "读者")),
        new MailAddress("reply@example.test", null),
        "M0 邮件验证",
        "<p>正文 &amp; 校验</p>",
        "正文 & 校验",
        headers);
  }

  @Test
  void mailpitReceivesMultipartWithStableMessageId() throws Exception {
    var config = new MailServerConfig("mailpit", 1025, null, null, SmtpSecurity.NONE);
    var mail = new SmtpMailTransport(config, Clock.systemUTC(), true);
    var result = mail.send(message(Map.of("X-B2B-Trace", "m0")), null);
    assertThat(result.messageId()).isEqualTo("<m0-adapter@example.test>");
    try (var http = HttpClient.newHttpClient()) {
      var response =
          http.send(
              HttpRequest.newBuilder(
                      URI.create(required("B2BWEB_MAILPIT_URL") + "/api/v1/messages"))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      var messages = JSON.readTree(response.body()).path("messages");
      assertThat(messages.size()).isGreaterThan(0);
      var detail =
          http.send(
              HttpRequest.newBuilder(
                      URI.create(
                          required("B2BWEB_MAILPIT_URL")
                              + "/api/v1/message/"
                              + messages.get(0).path("ID").asString()))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(detail.body()).contains("m0-adapter@example.test", "正文");
      var raw =
          http.send(
              HttpRequest.newBuilder(
                      URI.create(
                          required("B2BWEB_MAILPIT_URL")
                              + "/api/v1/message/"
                              + messages.get(0).path("ID").asString()
                              + "/raw"))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(raw.statusCode()).isEqualTo(200);
      assertThat(raw.body())
          .contains(
              "X-B2B-Trace: m0", "multipart/alternative", "Message-ID: <m0-adapter@example.test>");
    }
  }

  @Test
  void mailRejectsHeaderInjectionAndPlaintextInProduction() {
    var config = new MailServerConfig("mailpit", 1025, null, null, SmtpSecurity.NONE);
    assertThatThrownBy(
            () ->
                new SmtpMailTransport(config, Clock.systemUTC(), true)
                    .send(message(Map.of("Subject", "override")), null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SmtpMailTransport(config, Clock.systemUTC(), true)
                    .send(message(Map.of("X-Trace", "a\r\nBcc: secret@example.test")), null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SmtpMailTransport(config, Clock.systemUTC(), false)
                    .send(message(Map.of()), null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void realSearchAdapterHandlesTasksFiltersScanAndPrivateSwap() {
    var search =
        new MeilisearchClient(
            URI.create(required("B2BWEB_MEILI_URL")),
            Secret.of(required("B2BWEB_MEILI_MASTER_KEY")));
    String name = "m0_" + UUID.randomUUID().toString().replace("-", "");
    String copy = name + "_copy";
    var settings =
        new SearchIndexSettings(
            "id",
            List.of("title"),
            List.of("siteId"),
            List.of("id"),
            List.of("id"),
            null,
            Map.of(),
            Set.of());
    try {
      search
          .ensureIndex(name, settings)
          .ifPresent(
              id ->
                  assertThat(search.await(id, Duration.ofSeconds(30)).state())
                      .isEqualTo(SearchTaskState.SUCCEEDED));
      assertThat(search.ensureIndex(name, settings)).isEmpty();
      assertThat(
              search
                  .await(
                      search.upsert(
                          name,
                          List.of(
                              JSON.createObjectNode()
                                  .put("id", "9007199254740993")
                                  .put("title", "Industrial motor")
                                  .put("siteId", "1"),
                              JSON.createObjectNode()
                                  .put("id", "9007199254740994")
                                  .put("title", "Industrial motor")
                                  .put("siteId", "2"))),
                      Duration.ofSeconds(30))
                  .state())
          .isEqualTo(SearchTaskState.SUCCEEDED);
      assertThat(search.count(name)).isEqualTo(2);
      assertThat(
              search
                  .search(
                      name,
                      new SearchRequest(
                          "motor",
                          "siteId = '1'",
                          List.of("siteId"),
                          List.of(),
                          0,
                          10,
                          List.of("*"),
                          List.of()))
                  .hits())
          .hasSize(1);
      var page = search.scanDocuments(name, new CursorRequest(null, 1));
      assertThat(page.items()).hasSize(1);
      assertThat(page.nextCursor()).isNotNull();
      assertThat(search.scanDocuments(name, new CursorRequest(page.nextCursor(), 1)).items())
          .hasSize(1);
      search.ensureIndex(copy, settings).ifPresent(id -> search.await(id, Duration.ofSeconds(30)));
      assertThatThrownBy(() -> search.scanDocuments(copy, new CursorRequest(page.nextCursor(), 1)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(search.await(search.swap(name, copy), Duration.ofSeconds(30)).state())
          .isEqualTo(SearchTaskState.SUCCEEDED);
      assertThat(search.count(copy)).isEqualTo(2);
      assertThat(search.count(name)).isZero();
      assertThat(
              search
                  .await(search.deleteByFilter(copy, "siteId = '2'"), Duration.ofSeconds(30))
                  .state())
          .isEqualTo(SearchTaskState.SUCCEEDED);
      assertThat(search.count(copy)).isEqualTo(1);
    } finally {
      search.deleteIndex(name);
      search.deleteIndex(copy);
    }
  }
}
