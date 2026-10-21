package com.b2bweb.application;

import com.b2bweb.api.delivery.TemplateProvider;
import com.b2bweb.page.PageTemplates;
import com.b2bweb.testkit.conformance.TemplateProviderConformance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.profiles.active=test")
class PageTemplateContractTest extends TemplateProviderConformance {
  @Autowired PageTemplates templates;

  protected TemplateProvider provider() {
    return templates;
  }
}
