package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.extension.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.testkit.example.SampleExtensions;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ContractCatalogTest {
  @Test
  void everyCanonicalServiceAndPortHasAnExplicitTypedImplementation() throws Exception {
    var p = TestPlatform.standard();
    try (var input =
        getClass().getResourceAsStream("/META-INF/b2bweb/testkit/contract-catalog.json")) {
      var catalog = JsonMapper.builder().build().readTree(input);
      assertThat(catalog.path("services").size()).isEqualTo(25);
      assertThat(catalog.path("ports").size()).isEqualTo(17);
      for (String family : List.of("services", "ports"))
        for (var type : catalog.path(family)) {
          Class<?> contract = Class.forName(type.asString());
          Object implementation = p.implementation(contract);
          assertThat(contract.isInstance(implementation)).isTrue();
          assertThat(java.lang.reflect.Proxy.isProxyClass(implementation.getClass())).isFalse();
        }
    }
  }

  @Test
  void everyExtensionPointHasAnExecutableExampleAndRegistryFiltersDisabledModule()
      throws Exception {
    var p = TestPlatform.standard().useSite(1);
    var examples = new SampleExtensions(p);
    var all = examples.all();
    var registry = p.extensionRegistry(all.toArray());
    try (var input =
        getClass().getResourceAsStream("/META-INF/b2bweb/testkit/contract-catalog.json")) {
      var catalog = JsonMapper.builder().build().readTree(input);
      assertThat(catalog.path("extensions").size()).isEqualTo(25);
      for (var name : catalog.path("extensions")) {
        Class<?> type = Class.forName(name.asString());
        assertThat(all).anySatisfy(example -> assertThat(type.isInstance(example)).isTrue());
        assertThat(registry.all(type)).isNotEmpty();
      }
      p.siteApi().setModuleEnabled("sample", false);
      assertThat(registry.all(com.b2bweb.api.content.PublishContributor.class)).isEmpty();
    }
  }

  @Test
  void translationExampleKeepsNumbersAndUpdatesOnlyExplicitFields() {
    var p = TestPlatform.standard().useSite(1);
    var samples = new SampleExtensions(p);
    var json = JsonMapper.builder().build();
    samples.seed(1, 11, "zh-CN", json.readTree("{\"title\":\"型号A-123\",\"modelNo\":\"A-123\"}"));
    samples.seed(1, 22, "en", json.readTree("{\"modelNo\":\"A-123\",\"manual\":true}"));
    var ctx =
        TranslationContext.forContent(
            1, new TranslationSubject.Content(1, "SAMPLE", 11, 22, 1), "en", 1, null);
    var provider = samples.new ContentTranslation();
    assertThat(provider.extract(ctx)).extracting(TranslationUnit::text).containsExactly("型号A-123");
    provider.apply(ctx, Map.of("sample.title", "Model A-123"));
    assertThat(samples.translated(1, 22, "en").path("modelNo").asString()).isEqualTo("A-123");
    assertThat(samples.translated(1, 22, "en").path("manual").asBoolean()).isTrue();
  }
}
