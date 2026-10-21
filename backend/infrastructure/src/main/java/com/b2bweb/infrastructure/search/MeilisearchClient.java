package com.b2bweb.infrastructure.search;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

/** No mutation is automatically retried: unknown acceptance must abandon the private generation. */
public final class MeilisearchClient implements SearchEngineClient {
  final URI base;
  final Secret key;
  final JsonMapper json = JsonMapper.builder().build();
  final HttpClient http;

  public MeilisearchClient(URI base, Secret key) {
    this.base = base;
    this.key = key;
    http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  String index(String name) {
    if (!name.matches("[a-zA-Z0-9_-]{1,200}")) throw new IllegalArgumentException("索引名非法");
    return "/indexes/" + name;
  }

  JsonNode call(String method, String path, JsonNode body, boolean missingAllowed) {
    com.b2bweb.infrastructure.http.RemoteCalls.outsideTransaction();
    try {
      var request =
          HttpRequest.newBuilder(base.resolve(path))
              .timeout(path.endsWith("/search") ? Duration.ofSeconds(2) : Duration.ofSeconds(30))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + key.value())
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(body.toString()))
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404 && missingAllowed) return json.nullNode();
      if (response.statusCode() >= 300)
        throw new ExternalServiceException(
            InfraErrors.SEARCH_UNAVAILABLE, "搜索服务请求失败", response.statusCode() >= 500, null);
      return json.readTree(response.body());
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ExternalServiceException(InfraErrors.SEARCH_UNAVAILABLE, "搜索服务连接失败", true, e);
    }
  }

  long task(JsonNode accepted) {
    return accepted.path("taskUid").asLong();
  }

  public Optional<Long> ensureIndex(String name, SearchIndexSettings settings) {
    String path = index(name);
    var exists = call("GET", path, null, true);
    if (exists.isNull()) {
      long created =
          task(
              call(
                  "POST",
                  "/indexes",
                  json.createObjectNode().put("uid", name).put("primaryKey", settings.primaryKey()),
                  false));
      var state = await(created, Duration.ofSeconds(30));
      if (state.state() != SearchTaskState.SUCCEEDED)
        throw new ExternalServiceException(InfraErrors.SEARCH_TASK_FAILED, "索引创建未成功", true, null);
    } else if (!exists.path("primaryKey").asString().equals(settings.primaryKey()))
      throw new IllegalArgumentException("索引主键不匹配");
    var desired = json.createObjectNode();
    desired.set("searchableAttributes", json.valueToTree(settings.searchableAttributes()));
    desired.set("filterableAttributes", json.valueToTree(settings.filterableAttributes()));
    desired.set("sortableAttributes", json.valueToTree(settings.sortableAttributes()));
    desired.set("stopWords", json.valueToTree(settings.stopWords()));
    desired.set("synonyms", json.valueToTree(settings.synonyms()));
    desired.set(
        "typoTolerance",
        json.createObjectNode()
            .set(
                "disableOnAttributes",
                json.valueToTree(settings.typoToleranceDisabledAttributes())));
    var localized = json.createArrayNode();
    if (settings.localizedLanguage() != null) {
      var item = localized.addObject();
      item.putArray("locales").add(settings.localizedLanguage());
      item.putArray("attributePatterns").add("*");
    }
    desired.set("localizedAttributes", localized);
    var actual = call("GET", path + "/settings", null, false);
    boolean same = true;
    for (var prop : desired.properties()) {
      var current = actual.path(prop.getKey());
      if (prop.getKey().equals("typoTolerance")) current = current.path("disableOnAttributes");
      var expected =
          prop.getKey().equals("typoTolerance")
              ? prop.getValue().path("disableOnAttributes")
              : prop.getValue();
      if (!current.equals(expected)) same = false;
    }
    return same
        ? Optional.empty()
        : Optional.of(task(call("PATCH", path + "/settings", desired, false)));
  }

  public void deleteIndex(String name) {
    var accepted = call("DELETE", index(name), null, true);
    if (!accepted.isNull()
        && await(task(accepted), Duration.ofSeconds(30)).state() != SearchTaskState.SUCCEEDED)
      throw new ExternalServiceException(InfraErrors.SEARCH_TASK_FAILED, "索引删除未成功", true, null);
  }

  public List<String> listIndexes(String prefix) {
    List<String> out = new ArrayList<>();
    int offset = 0;
    while (true) {
      var rows = call("GET", "/indexes?limit=1000&offset=" + offset, null, false).path("results");
      for (var row : rows) {
        String name = row.path("uid").asString();
        if (name.startsWith(prefix)) out.add(name);
      }
      if (rows.size() < 1000) break;
      offset += rows.size();
    }
    return List.copyOf(out);
  }

  public long upsert(String name, List<JsonNode> docs) {
    if (docs.size() > 1000
        || docs.stream().anyMatch(d -> !d.isObject() || !d.path("id").isString()))
      throw new IllegalArgumentException("文档必须含字符串主键，单批最多1000");
    return task(call("POST", index(name) + "/documents", json.valueToTree(docs), false));
  }

  public long delete(String name, Collection<String> ids) {
    if (ids.size() > 1000) throw new IllegalArgumentException("单批最多1000");
    return task(
        call("POST", index(name) + "/documents/delete-batch", json.valueToTree(ids), false));
  }

  public long deleteByFilter(String name, String filter) {
    return task(
        call(
            "POST",
            index(name) + "/documents/delete",
            json.createObjectNode().put("filter", filter),
            false));
  }

  public long swap(String name, String other) {
    index(name);
    index(other);
    var pairs = json.createArrayNode();
    pairs.addObject().putArray("indexes").add(name).add(other);
    return task(call("POST", "/swap-indexes", pairs, false));
  }

  public SearchTaskStatus await(long id, Duration timeout) {
    long end = System.nanoTime() + timeout.toNanos();
    while (true) {
      var status = call("GET", "/tasks/" + id, null, false);
      var state =
          SearchTaskState.valueOf(status.path("status").asString().toUpperCase(Locale.ROOT));
      var result =
          new SearchTaskStatus(
              id, state, status.path("error").isNull() ? null : status.path("error").toString());
      if (Set.of(SearchTaskState.SUCCEEDED, SearchTaskState.FAILED, SearchTaskState.CANCELED)
              .contains(state)
          || System.nanoTime() >= end) return result;
      try {
        Thread.sleep(25);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return result;
      }
    }
  }

  public SearchResponse search(String name, SearchRequest input) {
    var body =
        json.createObjectNode()
            .put("q", input.query())
            .put("offset", input.offset())
            .put("limit", input.limit());
    if (input.filter() != null) body.put("filter", input.filter());
    body.set("facets", json.valueToTree(input.facets()));
    body.set("sort", json.valueToTree(input.sort()));
    if (!input.attributesToRetrieve().isEmpty())
      body.set("attributesToRetrieve", json.valueToTree(input.attributesToRetrieve()));
    body.set("attributesToHighlight", json.valueToTree(input.attributesToHighlight()));
    var res = call("POST", index(name) + "/search", body, false);
    List<JsonNode> hits = new ArrayList<>();
    res.path("hits").forEach(hits::add);
    Map<String, Map<String, Long>> facets = new HashMap<>();
    for (var f : res.path("facetDistribution").properties()) {
      Map<String, Long> values = new HashMap<>();
      for (var v : f.getValue().properties()) values.put(v.getKey(), v.getValue().asLong());
      facets.put(f.getKey(), values);
    }
    return new SearchResponse(
        hits,
        res.path("estimatedTotalHits").asLong(),
        facets,
        res.path("processingTimeMs").asInt());
  }

  public long count(String name) {
    return call("GET", index(name) + "/stats", null, false).path("numberOfDocuments").asLong();
  }

  public CursorPage<JsonNode> scanDocuments(String name, CursorRequest request) {
    index(name);
    int offset = 0;
    if (request.cursor() != null) {
      String decoded =
          new String(Base64.getUrlDecoder().decode(request.cursor()), StandardCharsets.UTF_8);
      if (!decoded.startsWith(name + ":")) throw new IllegalArgumentException("游标不属于该代次");
      offset = Integer.parseInt(decoded.substring(name.length() + 1));
      if (offset < 0) throw new IllegalArgumentException("游标非法");
    }
    var res =
        call(
            "GET",
            index(name) + "/documents?limit=" + request.size() + "&offset=" + offset,
            null,
            false);
    List<JsonNode> rows = new ArrayList<>();
    res.path("results").forEach(rows::add);
    int next = offset + rows.size();
    String cursor =
        next < res.path("total").asInt()
            ? Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((name + ":" + next).getBytes(StandardCharsets.UTF_8))
            : null;
    return new CursorPage<>(rows, cursor);
  }
}
