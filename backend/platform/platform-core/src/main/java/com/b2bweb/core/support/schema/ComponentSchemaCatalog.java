package com.b2bweb.core.support.schema;

import com.b2bweb.api.common.FieldError;
import com.b2bweb.core.module.ModuleCatalog;
import com.networknt.schema.*;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Compiles the exact JSON sources consumed by Admin and Nuxt, without network schema resolution. */
public final class ComponentSchemaCatalog {
  private record Entry(JsonNode definition, Schema properties) {}
  private final Map<String, SortedMap<Integer, Entry>> components = new TreeMap<>();
  private final Map<String, JsonNode> templates = new TreeMap<>();
  private final JsonNode common, styles;
  private final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
  private static final Set<String> SCOPES = Set.of("page", "slot", "children", "global-block");
  private static final Set<String> SECTION_KEYS = Set.of("id", "component", "componentVersion", "variant", "props", "hidden", "visibility", "style", "children");
  private final JsonMapper json = JsonMapper.builder().build();

  public ComponentSchemaCatalog(ModuleCatalog modules) throws IOException {
    common = read("defs/common.json"); styles = read("defs/style-options.json");
    var meta = registry.getSchema(read("meta/component.schema.json"));
    var resources = new PathMatchingResourcePatternResolver();
    for (String pattern : List.of("classpath*:component-schemas/components/*/*.json", "classpath*:component-schemas/components/*/history/*.json")) {
      for (var resource : resources.getResources(pattern)) {
        JsonNode definition;
        try (var input = resource.getInputStream()) { definition = json.readTree(input); }
        var errors = meta.validate(definition);
        if (!errors.isEmpty()) throw invalid(resource.toString(), errors.getFirst().getInstanceLocation().toString());
        String type = definition.path("type").asString(), module = definition.path("module").asString();
        if (!modules.descriptors().containsKey(module) || !resource.getURL().getPath().contains("/" + module + "/")) throw invalid(resource.toString(), "/module");
        boolean declared = false;
        for (var item : modules.descriptors().get(module).path("components")) if (item.asString().equals(type)) declared = true;
        if (!declared) throw invalid(resource.toString(), "/type");
        for (var option : definition.path("styleOptions")) if (!styles.has(option.asString())) throw invalid(resource.toString(), "/styleOptions");
        int version = definition.path("version").asInt();
        String expected = resource.getURL().getPath().contains("/history/") ? type + ".v" + version + ".json" : type + ".json";
        if (!expected.equals(resource.getFilename())) throw invalid(resource.toString(), "/type");
        ObjectNode schema = (ObjectNode) definition.path("propsSchema").deepCopy();
        localRefs(schema);
        ObjectNode bundled = (ObjectNode) common.path("$defs").deepCopy();
        for (var custom : schema.path("$defs").properties()) {
          if (bundled.has(custom.getKey())) throw invalid(resource.toString(), "/propsSchema/$defs/" + custom.getKey());
          bundled.set(custom.getKey(), custom.getValue().deepCopy());
        }
        schema.set("$defs", bundled);
        var entry = new Entry(definition.deepCopy(), registry.getSchema(schema));
        if (components.computeIfAbsent(type, ignored -> new TreeMap<>()).putIfAbsent(version, entry) != null) throw invalid(resource.toString(), "/version");
      }
    }
    if (components.isEmpty()) throw invalid("components", "/");
    var templateMeta = registry.getSchema(read("meta/template.schema.json"));
    for (var resource : resources.getResources("classpath*:component-schemas/templates/*/*.json")) {
      JsonNode definition;
      try (var input = resource.getInputStream()) { definition = json.readTree(input); }
      var errors = templateMeta.validate(definition);
      if (!errors.isEmpty()) throw invalid(resource.toString(), errors.getFirst().getInstanceLocation().toString());
      String key = definition.path("key").asString();
      if (!(key + ".json").equals(resource.getFilename()) || templates.putIfAbsent(key, definition.deepCopy()) != null) throw invalid(resource.toString(), "/key");
      Set<String> keys = new HashSet<>();
      for (var region : definition.path("regions")) {
        if (!keys.add(region.path("key").asString())) throw invalid(resource.toString(), "/regions");
        if (region.path("fixed").asBoolean(false)) {
          Entry component = entry(region.path("component").asString(), null);
          if (!component.properties().validate(region.path("props")).isEmpty()) throw invalid(resource.toString(), "/regions/props");
        } else {
          for (var allowed : region.path("allowed")) if (!contains(component(allowed.asString()).path("allowedIn"), "slot")) throw invalid(resource.toString(), "/regions/allowed");
          if (region.has("default")) {
            var layout = json.createObjectNode().put("version", 1); layout.set("sections", region.path("default"));
            if (!validateLayout(layout, "slot").isEmpty()) throw invalid(resource.toString(), "/regions/default");
          }
        }
      }
    }
  }
  private JsonNode read(String path) throws IOException {
    try (var input = getClass().getResourceAsStream("/component-schemas/" + path)) {
      if (input == null) throw invalid(path, "/"); return json.readTree(input);
    }
  }
  private IllegalStateException invalid(String file, String pointer) { return new IllegalStateException("组件Schema无效: " + file + " " + pointer); }
  private void localRefs(JsonNode node) {
    if (node.isObject()) {
      if (node.has("$id")) throw invalid("propsSchema", "/$id");
      if (node.has("$ref")) {
        String value = node.path("$ref").asString();
        if (value.startsWith("../../defs/common.json#/$defs/")) ((ObjectNode) node).put("$ref", value.substring("../../defs/common.json".length()));
        else if (!value.startsWith("#/$defs/")) throw invalid("propsSchema", "/$ref");
      }
      node.properties().forEach(property -> localRefs(property.getValue()));
    } else if (node.isArray()) node.forEach(this::localRefs);
  }
  private Entry entry(String type, Integer version) {
    var versions = components.get(type);
    if (versions == null || version != null && !versions.containsKey(version)) throw new IllegalArgumentException("组件或版本不存在: " + type);
    return versions.get(version == null ? versions.lastKey() : version);
  }
  public JsonNode component(String type) { return entry(type, null).definition().deepCopy(); }
  public JsonNode component(String type, int version) { return entry(type, version).definition().deepCopy(); }
  public String moduleOf(String type) { return entry(type, null).definition().path("module").asString(); }
  public JsonNode template(String key) {
    var template = templates.get(key); if (template == null) throw new IllegalArgumentException("模板不存在: " + key); return template.deepCopy();
  }
  public List<JsonNode> templates() { return templates.values().stream().map(JsonNode::deepCopy).toList(); }
  public Set<String> types() { return Collections.unmodifiableSet(components.keySet()); }
  private static boolean contains(JsonNode values, String target) { for (var item : values) if (item.asString().equals(target)) return true; return false; }
  private static void error(List<FieldError> errors, String path, String code) { errors.add(new FieldError(path, code, "布局不符合组件定义")); }
  public List<FieldError> validateLayout(JsonNode layout, String scope) {
    if (!SCOPES.contains(scope)) throw new IllegalArgumentException("布局位置无效");
    List<FieldError> errors = new ArrayList<>();
    if (!layout.isObject() || !layout.path("version").isIntegralNumber() || !layout.path("version").asString().equals("1") || !layout.path("sections").isArray() || json.writeValueAsBytes(layout).length > 1_048_576) { error(errors, "/", "layout"); return List.copyOf(errors); }
    for (var property : layout.properties()) if (!Set.of("version", "sections").contains(property.getKey())) error(errors, "/" + property.getKey(), "unknown");
    sections(layout.path("sections"), scope, 0, "/sections", new HashSet<>(), errors);
    return List.copyOf(errors);
  }
  private void sections(JsonNode list, String scope, int depth, String path, Set<String> ids, List<FieldError> errors) {
    if (depth > 2 || !list.isArray()) { error(errors, path, "depth"); return; }
    for (int i = 0; i < list.size(); i++) {
      JsonNode section = list.path(i); String pointer = path + "/" + i;
      if (!section.isObject() || ids.size() >= 500 || !section.path("id").asString().matches("[A-Za-z0-9_-]{1,80}") || !ids.add(section.path("id").asString())) { error(errors, pointer, "id"); continue; }
      for (var property : section.properties()) if (!SECTION_KEYS.contains(property.getKey())) error(errors, pointer + "/" + property.getKey(), "unknown");
      var versions = components.get(section.path("component").asString());
      if (versions == null) { error(errors, pointer + "/component", "unknown"); continue; }
      var entry = versions.get(versions.lastKey()); var definition = entry.definition();
      if (!section.path("componentVersion").isIntegralNumber() || !section.path("componentVersion").asString().equals(Integer.toString(versions.lastKey()))) error(errors, pointer + "/componentVersion", "version");
      if (!contains(definition.path("allowedIn"), scope)) error(errors, pointer, "scope");
      if (section.has("variant") && !contains(definition.path("variants"), section.path("variant").asString())) error(errors, pointer + "/variant", "variant");
      for (var error : entry.properties().validate(section.path("props"))) error(errors, pointer + "/props" + error.getInstanceLocation(), error.getKeyword());
      if (section.has("hidden") && !section.path("hidden").isBoolean()) error(errors, pointer + "/hidden", "type");
      if (section.has("style")) {
        if (!section.path("style").isObject()) error(errors, pointer + "/style", "type");
        for (var option : section.path("style").properties()) if (!contains(definition.path("styleOptions"), option.getKey()) || !option.getValue().isString() || !contains(styles.path(option.getKey()), option.getValue().asString())) error(errors, pointer + "/style/" + option.getKey(), "token");
      }
      if (section.has("visibility")) {
        if (!section.path("visibility").isObject()) error(errors, pointer + "/visibility", "type");
        for (var device : section.path("visibility").properties()) if (!Set.of("mobile", "tablet", "desktop").contains(device.getKey()) || !device.getValue().isBoolean()) error(errors, pointer + "/visibility/" + device.getKey(), "type");
      }
      if (section.has("children")) {
        if (!section.path("children").isObject()) error(errors, pointer + "/children", "type");
        for (var slot : section.path("children").properties()) {
          var children = definition.path("children");
          if (!contains(children.path("slots"), slot.getKey())) error(errors, pointer + "/children/" + slot.getKey(), "slot");
          if (!children.path("allowed").asString("").equals("*")) for (var child : slot.getValue()) if (!contains(children.path("allowed"), child.path("component").asString())) error(errors, pointer + "/children/" + slot.getKey(), "allowed");
          sections(slot.getValue(), "children", depth + 1, pointer + "/children/" + slot.getKey(), ids, errors);
        }
      }
    }
  }
  /** JSON Pointer patterns use '*' for array elements; rich text is extracted by its own walker. */
  public List<String> translatablePaths(String type, int version) {
    List<String> paths = new ArrayList<>(); translatable(entry(type, version).definition().path("propsSchema"), "", paths, new HashSet<>()); return List.copyOf(paths);
  }
  private void translatable(JsonNode schema, String path, List<String> paths, Set<String> visiting) {
    if (schema.path("x-translatable").isBoolean() && !schema.path("x-translatable").asBoolean()) return;
    if (schema.has("$ref")) {
      String reference = schema.path("$ref").asString(); String name = reference.substring(reference.lastIndexOf('/') + 1);
      if (Set.of("richText", "richTextInline").contains(name)) { paths.add(path); return; }
      if (visiting.add(name)) { translatable(common.path("$defs").path(name), path, paths, visiting); visiting.remove(name); } return;
    }
    if (schema.path("type").asString("").equals("string") && !schema.has("enum") && !schema.has("const") && !schema.has("pattern") && !schema.has("format")) { paths.add(path); return; }
    for (var property : schema.path("properties").properties()) translatable(property.getValue(), path + "/" + escape(property.getKey()), paths, visiting);
    if (schema.has("items")) translatable(schema.path("items"), path + "/*", paths, visiting);
  }
  public Set<String> refKeys(JsonNode layout, String kind) {
    Set<String> keys = new LinkedHashSet<>();
    if (!kind.matches("[a-z][a-z0-9-]*")) throw new IllegalArgumentException("引用类型无效");
    collectSections(layout.path("sections"), kind, keys, 0); return Collections.unmodifiableSet(keys);
  }
  private void collectSections(JsonNode sections, String kind, Set<String> keys, int depth) {
    if (depth > 2) return;
    for (var section : sections) {
      var versions = components.get(section.path("component").asString());
      if (versions == null) continue;
      var version = versions.get(section.path("componentVersion").asInt());
      if (version == null) continue;
      refs(version.definition().path("propsSchema"), section.path("props"), kind, keys, 0);
      for (var slot : section.path("children").properties()) collectSections(slot.getValue(), kind, keys, depth + 1);
    }
  }
  private void refs(JsonNode schema, JsonNode value, String kind, Set<String> keys, int depth) {
    if (depth > 30 || value.isMissingNode() || value.isNull()) return;
    if (schema.path("x-ref").asString("").equals(kind) && value.isString()) keys.add(value.asString());
    if (value.isObject() && value.path("$" + kind).isString()) keys.add(value.path("$" + kind).asString());
    if (schema.has("$ref")) { String reference = schema.path("$ref").asString(); refs(common.path("$defs").path(reference.substring(reference.lastIndexOf('/') + 1)), value, kind, keys, depth + 1); }
    for (var property : schema.path("properties").properties()) refs(property.getValue(), value.path(property.getKey()), kind, keys, depth + 1);
    if (schema.has("items") && value.isArray()) for (var item : value) refs(schema.path("items"), item, kind, keys, depth + 1);
    for (String union : List.of("oneOf", "anyOf", "allOf")) for (var alternative : schema.path(union)) refs(alternative, value, kind, keys, depth + 1);
  }
  private String escape(String value) { return value.replace("~", "~0").replace("/", "~1"); }
}
