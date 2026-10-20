package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/**
 * Deterministic engine tasks with detached JSON, filter/sort/facet semantics and generation-bound
 * cursors.
 */
public final class InMemorySearchEngine implements SearchEngineClient {
  private record Index(SearchIndexSettings settings, SortedMap<String, JsonNode> docs) {}

  private final SortedMap<String, Index> indexes = new TreeMap<>();
  private final Map<Long, SearchTaskStatus> tasks = new HashMap<>();
  private final Set<String> frozen = new HashSet<>();
  private long nextTask;
  private boolean failNext;
  private final JsonMapper json = JsonMapper.builder().build();

  public void failNextMutation() {
    failNext = true;
  }

  public void freeze(String name) {
    require(name);
    frozen.add(name);
  }

  private String name(String name) {
    if (!name.matches("[a-zA-Z0-9_-]{1,200}")) throw new IllegalArgumentException("索引名非法");
    return name;
  }

  private Index require(String name) {
    name(name);
    var index = indexes.get(name);
    if (index == null)
      throw new ExternalServiceException(InfraErrors.SEARCH_UNAVAILABLE, "索引不存在", false, null);
    return index;
  }

  private long mutation(String name, Runnable action) {
    if (frozen.contains(name)) throw new IllegalStateException("不可变代次禁止写入");
    long id = ++nextTask;
    boolean fail = failNext;
    failNext = false;
    tasks.put(
        id,
        new SearchTaskStatus(
            id,
            fail ? SearchTaskState.FAILED : SearchTaskState.SUCCEEDED,
            fail ? "受控引擎任务失败" : null));
    if (!fail) action.run();
    return id;
  }

  public synchronized Optional<Long> ensureIndex(String name, SearchIndexSettings settings) {
    name(name);
    if (!settings.primaryKey().equals("id")) throw new IllegalArgumentException("主键必须为id");
    var old = indexes.get(name);
    if (old != null && old.settings().equals(settings)) return Optional.empty();
    return Optional.of(
        mutation(
            name,
            () ->
                indexes.put(
                    name, new Index(settings, old == null ? new TreeMap<>() : old.docs()))));
  }

  public synchronized void deleteIndex(String name) {
    name(name);
    indexes.remove(name);
    frozen.remove(name);
  }

  public synchronized List<String> listIndexes(String prefix) {
    return indexes.keySet().stream().filter(n -> n.startsWith(prefix)).toList();
  }

  public synchronized long upsert(String name, List<JsonNode> docs) {
    var index = require(name);
    if (docs.size() > 1000
        || docs.stream().anyMatch(d -> !d.isObject() || !d.path("id").isString()))
      throw new IllegalArgumentException("文档必须含字符串主键，单批最多1000");
    var detached = docs.stream().map(JsonNode::deepCopy).toList();
    return mutation(
        name, () -> detached.forEach(d -> index.docs().put(d.path("id").asString(), d)));
  }

  public synchronized long delete(String name, Collection<String> ids) {
    var index = require(name);
    if (ids.size() > 1000) throw new IllegalArgumentException("删除批次超限");
    return mutation(name, () -> ids.forEach(index.docs()::remove));
  }

  public synchronized long deleteByFilter(String name, String filter) {
    var index = require(name);
    var predicate = filter(index, filter);
    return mutation(name, () -> index.docs().values().removeIf(predicate));
  }

  public synchronized long swap(String name, String other) {
    var a = require(name);
    var b = require(other);
    if (frozen.contains(other)) throw new IllegalStateException("不可变代次不能交换");
    return mutation(
        name,
        () -> {
          indexes.put(name, b);
          indexes.put(other, a);
        });
  }

  public synchronized SearchTaskStatus await(long id, Duration timeout) {
    if (timeout.isNegative()) throw new IllegalArgumentException("等待超时无效");
    var task = tasks.get(id);
    if (task == null) throw new IllegalArgumentException("任务不存在");
    return task;
  }

  public synchronized long count(String name) {
    return require(name).docs().size();
  }

  public synchronized CursorPage<JsonNode> scanDocuments(String name, CursorRequest cursor) {
    var index = require(name);
    int offset = 0;
    if (cursor.cursor() != null) {
      String value =
          new String(Base64.getUrlDecoder().decode(cursor.cursor()), StandardCharsets.UTF_8);
      if (!value.startsWith(name + ":")) throw new IllegalArgumentException("游标不属于该代次");
      offset = Integer.parseInt(value.substring(name.length() + 1));
      if (offset < 0) throw new IllegalArgumentException("游标无效");
    }
    var docs =
        index.docs().values().stream()
            .skip(offset)
            .limit(cursor.size())
            .map(JsonNode::deepCopy)
            .toList();
    int next = offset + docs.size();
    String token =
        next < index.docs().size()
            ? Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((name + ":" + next).getBytes(StandardCharsets.UTF_8))
            : null;
    return new CursorPage<>(docs, token);
  }

  private JsonNode field(JsonNode doc, String field) {
    var node = doc;
    for (String part : field.split("\\.")) node = node.path(part);
    return node;
  }

  private boolean contains(JsonNode node, String expected) {
    if (node.isArray()) {
      for (var item : node) if (contains(item, expected)) return true;
      return false;
    }
    return !node.isMissingNode() && !node.isNull() && node.asString().equals(expected);
  }

  private Predicate<JsonNode> filter(Index index, String filter) {
    if (filter == null || filter.isBlank()) return d -> true;
    return new Parser(index, filter).parse();
  }

  private final class Parser {
    final Index index;
    final String input;
    int at;

    Parser(Index index, String input) {
      this.index = index;
      this.input = input;
    }

    void spaces() {
      while (at < input.length() && Character.isWhitespace(input.charAt(at))) at++;
    }

    boolean eat(String token) {
      spaces();
      if (input.startsWith(token, at)) {
        at += token.length();
        return true;
      }
      return false;
    }

    Predicate<JsonNode> parse() {
      var result = and();
      spaces();
      if (at != input.length()) throw new IllegalArgumentException("不支持的搜索过滤表达式");
      return result;
    }

    Predicate<JsonNode> and() {
      var result = term();
      while (eat("AND")) result = result.and(term());
      return result;
    }

    Predicate<JsonNode> term() {
      if (eat("(")) {
        var result = and();
        if (!eat(")")) throw new IllegalArgumentException("过滤括号不匹配");
        return result;
      }
      spaces();
      int start = at;
      while (at < input.length()
          && (Character.isLetterOrDigit(input.charAt(at)) || "_.".indexOf(input.charAt(at)) >= 0))
        at++;
      String key = input.substring(start, at);
      if (!index.settings().filterableAttributes().contains(key))
        throw new IllegalArgumentException("过滤字段未登记: " + key);
      if (eat("=")) {
        String expected = string();
        return doc -> contains(field(doc, key), expected);
      }
      if (eat("IN")) {
        if (!eat("[")) throw new IllegalArgumentException("过滤IN无效");
        Set<String> values = new HashSet<>();
        if (!eat("]")) {
          do {
            values.add(string());
          } while (eat(","));
          if (!eat("]")) throw new IllegalArgumentException("过滤IN无效");
        }
        return doc -> values.stream().anyMatch(v -> contains(field(doc, key), v));
      }
      throw new IllegalArgumentException("不支持的过滤运算符");
    }

    String string() {
      spaces();
      if (at >= input.length() || input.charAt(at) != '"')
        throw new IllegalArgumentException("过滤值必须为转义字符串");
      int start = at++;
      boolean escape = false;
      while (at < input.length()) {
        char c = input.charAt(at++);
        if (c == '"' && !escape) return json.readTree(input.substring(start, at)).asString();
        if (c == '\\' && !escape) escape = true;
        else escape = false;
      }
      throw new IllegalArgumentException("过滤字符串未结束");
    }
  }

  public synchronized SearchResponse search(String name, SearchRequest request) {
    var index = require(name);
    var predicate = filter(index, request.filter());
    String query = request.query().toLowerCase(Locale.ROOT);
    var docs =
        index.docs().values().stream()
            .filter(predicate)
            .filter(
                d ->
                    query.isBlank()
                        || index.settings().searchableAttributes().stream()
                            .anyMatch(
                                f ->
                                    field(d, f)
                                        .toString()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(query)))
            .toList();
    Comparator<JsonNode> comparator = (a, b) -> 0;
    for (String sort : request.sort()) {
      String[] part = sort.split(":", -1);
      if (part.length != 2
          || !Set.of("asc", "desc").contains(part[1])
          || !index.settings().sortableAttributes().contains(part[0]))
        throw new IllegalArgumentException("排序字段未登记");
      Comparator<JsonNode> next =
          (a, b) -> {
            var av = field(a, part[0]);
            var bv = field(b, part[0]);
            return av.isNumber() && bv.isNumber()
                ? Double.compare(av.asDouble(), bv.asDouble())
                : av.asString("").compareTo(bv.asString(""));
          };
      comparator = comparator.thenComparing(part[1].equals("desc") ? next.reversed() : next);
    }
    Map<String, Map<String, Long>> facets = new TreeMap<>();
    for (String facet : request.facets()) {
      if (!index.settings().filterableAttributes().contains(facet))
        throw new IllegalArgumentException("分面字段未登记");
      Map<String, Long> counts = new TreeMap<>();
      for (var d : docs) {
        var value = field(d, facet);
        if (value.isArray()) for (var item : value) counts.merge(item.asString(), 1L, Long::sum);
        else if (!value.isMissingNode() && !value.isNull())
          counts.merge(value.asString(), 1L, Long::sum);
      }
      facets.put(facet, Map.copyOf(counts));
    }
    var hits =
        docs.stream()
            .sorted(comparator.thenComparing(d -> d.path("id").asString()))
            .skip(request.offset())
            .limit(request.limit())
            .map(
                d -> {
                  if (request.attributesToRetrieve().isEmpty()
                      || request.attributesToRetrieve().contains("*")) return d.deepCopy();
                  var result = json.createObjectNode();
                  for (String attr : request.attributesToRetrieve())
                    if (d.has(attr)) result.set(attr, d.path(attr).deepCopy());
                  return (JsonNode) result;
                })
            .toList();
    return new SearchResponse(hits, docs.size(), facets, 0);
  }
}
