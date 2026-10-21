package com.b2bweb.testkit.conformance;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.delivery.*;
import org.junit.jupiter.api.Test;

public abstract class TemplateProviderConformance {
  protected abstract TemplateProvider provider();

  @Test
  public void definitionsResolveToUniqueOrderedRegions() {
    var definitions = provider().templates();
    assertThat(definitions)
        .isNotEmpty()
        .extracting(TemplateDefinition::key)
        .doesNotHaveDuplicates();
    for (var template : definitions) {
      assertThat(template.version()).isPositive();
      assertThat(template.contentTypes()).isNotEmpty();
      assertThat(provider().template(template.key())).contains(template);
      assertThat(template.regions()).extracting(TemplateRegion::key).doesNotHaveDuplicates();
      for (var region : template.regions()) {
        assertThat(region.key()).matches("[a-z][a-zA-Z0-9]*");
        assertThat(region.max()).isPositive();
        if (region.kind() == RegionKind.FIXED) assertThat(region.component()).isNotBlank();
        else assertThat(region.allowed()).isNotEmpty();
      }
    }
    assertThat(provider().template("contract-missing-template")).isEmpty();
  }
}
