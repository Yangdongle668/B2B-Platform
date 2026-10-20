package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.PlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {
  @Test
  void allBusinessModulesRespectTheirDeclaredDependencies() {
    var modules = ApplicationModules.of(PlatformApplication.class);
    modules.verify();
    for (String id :
        java.util.List.of(
            "content",
            "url",
            "media",
            "taxonomy",
            "form",
            "notification",
            "translation",
            "product",
            "article",
            "page",
            "inquiry",
            "navigation",
            "delivery",
            "seo",
            "graph",
            "search",
            "tracking")) assertThat(modules.getModuleByName(id)).as(id).isPresent();
  }
}
