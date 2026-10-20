package com.b2bweb.core.settings;

import com.b2bweb.api.common.*;
import com.b2bweb.core.module.ModuleCatalog;
import com.networknt.schema.*;
import java.util.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** Immutable compiled owner schemas. Defaults are validated during startup. */
public final class SettingsSchemas {
  public record Definition(String owner, JsonNode schema, Schema compiled) {}

  private final Map<String, Definition> definitions = new TreeMap<>();
  private final SchemaRegistry registry =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

  public SettingsSchemas(ModuleCatalog catalog) {
    load("core", "/META-INF/b2bweb/settings.schema.json");
    catalog
        .descriptors()
        .forEach(
            (id, node) -> {
              if (node.path("settings").isString())
                load(id, "/META-INF/b2bweb/modules/" + id + "/" + node.path("settings").asString());
            });
  }

  private void load(String owner, String path) {
    if (path.contains("..")) throw new IllegalStateException("设置Schema路径非法");
    try (var input = getClass().getResourceAsStream(path)) {
      if (input == null) throw new IllegalStateException("缺少设置Schema: " + path);
      var root = JsonMapper.builder().build().readTree(input);
      if (!root.path("type").asString().equals("object")
          || !root.path("additionalProperties").isBoolean()
          || root.path("additionalProperties").asBoolean())
        throw new IllegalStateException("设置Schema必须是封闭对象");
      for (var prop : root.path("properties").properties()) {
        String key = owner + "." + prop.getKey();
        var schema = prop.getValue();
        if (!prop.getKey().matches("[a-z][a-zA-Z0-9]*")
            || !schema.path("title").isString()
            || !schema.has("default")) throw new IllegalStateException("设置声明缺少名称或默认值: " + key);
        var definition = new Definition(owner, schema.deepCopy(), registry.getSchema(schema));
        if (!definition.compiled().validate(schema.path("default")).isEmpty())
          throw new IllegalStateException("设置默认值不符合Schema: " + key);
        if (definitions.putIfAbsent(key, definition) != null)
          throw new IllegalStateException("设置键重复: " + key);
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("设置Schema读取失败: " + path, e);
    }
  }

  public Definition require(String key) {
    var found = definitions.get(key);
    if (found == null) throw new BusinessException(CoreErrors.SETTING_NOT_DECLARED, "设置键未声明");
    return found;
  }

  public Set<String> keys() {
    return Collections.unmodifiableSet(definitions.keySet());
  }

  public void validate(String key, JsonNode value) {
    var errors = require(key).compiled().validate(value);
    if (!errors.isEmpty())
      throw new ValidationException(
          CoreErrors.SETTING_INVALID,
          "设置不符合定义",
          errors.stream()
              .map(
                  e ->
                      new FieldError(
                          e.getInstanceLocation().toString(), e.getKeyword(), "字段不符合设置约束"))
              .toList());
  }
}
