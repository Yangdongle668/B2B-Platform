package com.b2bweb.core.module;

import java.util.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** Immutable runtime inventory. Startup fails on duplicate/missing/cyclic dependencies. */
public final class ModuleCatalog
    implements org.springframework.beans.factory.config.BeanFactoryPostProcessor {
  private final SortedMap<String, JsonNode> modules = new TreeMap<>();

  public ModuleCatalog() throws Exception {
    var mapper = JsonMapper.builder().build();
    com.networknt.schema.Schema schema;
    try (var input = getClass().getResourceAsStream("/META-INF/b2bweb/module.schema.json")) {
      schema =
          com.networknt.schema.SchemaRegistry.withDefaultDialect(
                  com.networknt.schema.SpecificationVersion.DRAFT_2020_12)
              .getSchema(mapper.readTree(input));
    }
    Set<String> prefixes = new HashSet<>();
    for (var resource :
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:META-INF/b2bweb/modules/*/module.json")) {
      try (var in = resource.getInputStream()) {
        var node = mapper.readTree(in);
        String id = node.path("id").asString();
        if (!schema.validate(node).isEmpty())
          throw new IllegalStateException("模块描述符不符合Schema: " + resource);
        if (!prefixes.add(node.path("apiPrefix").asString())
            || node.path("apiPrefix").asString().equals("system"))
          throw new IllegalStateException("模块路由前缀冲突: " + id);
        com.b2bweb.api.common.SemVer.parse(node.path("version").asString());
        if (!id.matches("[a-z][a-z0-9]{1,31}") || modules.putIfAbsent(id, node) != null)
          throw new IllegalStateException("重复或非法模块ID");
      }
    }
    order();
    validateRanges();
  }

  /**
   * Candidate module declarations target the future 1.0 contract; the Maven artifact stays
   * 0.1.0-SNAPSHOT until G0 approval.
   */
  public void validateRanges() {
    var platformTarget = new org.semver4j.Semver("1.0.0");
    for (var item : modules.entrySet()) {
      var node = item.getValue();
      if (!platformTarget.satisfies(node.path("platform").asString()))
        throw new IllegalStateException("模块平台契约目标不兼容: " + item.getKey());
      for (String kind : List.of("requires", "optional"))
        for (var dep : node.path(kind).properties())
          if (modules.containsKey(dep.getKey())
              && !new org.semver4j.Semver(modules.get(dep.getKey()).path("version").asString())
                  .satisfies(dep.getValue().asString()))
            throw new IllegalStateException("模块依赖版本不兼容: " + item.getKey() + " -> " + dep.getKey());
    }
  }

  public void postProcessBeanFactory(
      org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory) {
    for (String id : order()) factory.registerSingleton(id + "ModuleDescriptor", descriptor(id));
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor(String id) {
    var n = modules.get(id);
    if (n == null) throw new IllegalArgumentException("模块未登记: " + id);
    Map<String, String> requires = new LinkedHashMap<>(), optional = new LinkedHashMap<>();
    n.path("requires").properties().forEach(e -> requires.put(e.getKey(), e.getValue().asString()));
    n.path("optional").properties().forEach(e -> optional.put(e.getKey(), e.getValue().asString()));
    List<String> permissions = new ArrayList<>();
    n.path("permissions").forEach(p -> permissions.add(p.path("code").asString()));
    Set<String> publishes = new LinkedHashSet<>(), subscribes = new LinkedHashSet<>();
    n.path("events").path("publishes").forEach(e -> publishes.add(e.asString()));
    n.path("events").path("subscribes").forEach(e -> subscribes.add(e.asString()));
    return new com.b2bweb.api.extension.ModuleDescriptor(
        id,
        n.path("name").asString(),
        com.b2bweb.api.common.SemVer.parse(n.path("version").asString()),
        n.path("layer").asString(),
        n.path("platform").asString(),
        requires,
        optional,
        n.path("disableable").asBoolean(),
        n.path("tablePrefix").asString(null),
        n.path("apiPrefix").asString(null),
        permissions,
        publishes,
        subscribes,
        n.deepCopy());
  }

  public List<String> order() {
    List<String> ordered = new ArrayList<>();
    Set<String> visiting = new HashSet<>();
    for (String id : modules.keySet()) visit(id, visiting, ordered);
    return List.copyOf(ordered);
  }

  private void visit(String id, Set<String> visiting, List<String> ordered) {
    if (ordered.contains(id)) return;
    if (!visiting.add(id)) throw new IllegalStateException("模块依赖环: " + id);
    var node = modules.get(id);
    if (node == null) throw new IllegalStateException("缺少依赖模块: " + id);
    for (String key : List.of("requires", "optional"))
      for (var dep : node.path(key).properties())
        if (modules.containsKey(dep.getKey()) || key.equals("requires"))
          visit(dep.getKey(), visiting, ordered);
    visiting.remove(id);
    ordered.add(id);
  }

  public Map<String, JsonNode> descriptors() {
    return Collections.unmodifiableMap(modules);
  }
}
