package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=test")
class OpenApiExportTest {
  @Value("${local.server.port}")
  int port;

  @Test
  void executableApplicationExportsItsActualOpenApi() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body())
          .contains("/api/admin/v1/system/auth/login")
          .doesNotContain("/api/admin/v1/test/site");
      Path path = Path.of("target/openapi.json");
      Files.createDirectories(path.getParent());
      var json = tools.jackson.databind.json.JsonMapper.builder().build();
      var schema = (tools.jackson.databind.node.ObjectNode) json.readTree(response.body());
      schema.remove("servers");
      assertThat(
              schema
                  .path("components")
                  .path("schemas")
                  .path("SiteInfo")
                  .path("properties")
                  .path("siteId")
                  .path("type")
                  .asString())
          .isEqualTo("string");
      assertThat(
              schema
                  .path("components")
                  .path("schemas")
                  .path("JobInfo")
                  .path("properties")
                  .path("jobId")
                  .path("type")
                  .asString())
          .isEqualTo("string");
      Files.writeString(
          path, json.writerWithDefaultPrettyPrinter().writeValueAsString(schema) + "\n");
    }
  }
}
