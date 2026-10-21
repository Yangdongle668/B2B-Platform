package com.b2bweb.page;

import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.Extension;
import java.util.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Backend registration of exactly the same versioned template JSON consumed by Admin and Nuxt. */
@Extension(module = "page")
public final class PageTemplates implements TemplateProvider {
  private final List<TemplateDefinition> templates;

  public PageTemplates() throws java.io.IOException {
    var json = JsonMapper.builder().build();
    var found = new TreeMap<String, TemplateDefinition>();
    for (var resource :
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:component-schemas/templates/page/*.json")) {
      JsonNode value;
      try (var in = resource.getInputStream()) {
        value = json.readTree(in);
      }
      Set<String> types = new HashSet<>();
      value.path("contentTypes").forEach(t -> types.add(t.asString()));
      var regions = new ArrayList<TemplateRegion>();
      for (var region : value.path("regions")) {
        Set<String> allowed = new HashSet<>();
        if (region.path("allowed").isString()) allowed.add(region.path("allowed").asString());
        else region.path("allowed").forEach(t -> allowed.add(t.asString()));
        regions.add(
            new TemplateRegion(
                region.path("key").asString(),
                region.path("fixed").asBoolean() ? RegionKind.FIXED : RegionKind.SLOT,
                region.path("component").asString(null),
                allowed,
                24,
                null,
                region.has("default")
                    ? region.path("default").deepCopy()
                    : json.createArrayNode()));
      }
      var definition =
          new TemplateDefinition(
              value.path("key").asString(),
              value.path("version").asInt(),
              types,
              value.path("label").asString(),
              regions,
              value.path("legal").asBoolean());
      if (found.putIfAbsent(definition.key(), definition) != null)
        throw new IllegalStateException("模板定义重复");
    }
    templates = List.copyOf(found.values());
  }

  public List<TemplateDefinition> templates() {
    return templates;
  }
}
