package com.b2bweb.testkit.conformance;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.seo.*;
import java.util.*;
import org.junit.jupiter.api.Test;

public abstract class ContentTypeProviderConformance {
  protected abstract ContentTypeProvider provider();

  protected abstract RouteHintsContext context();

  @Test
  public void validUniqueDefinitionsAndStableHints() {
    var defs = provider().contentTypes();
    assertThat(defs).isNotEmpty();
    assertThat(defs).extracting(ContentTypeDefinition::type).doesNotHaveDuplicates();
    for (var def : defs) {
      assertThat(def.type()).matches("[A-Z][A-Z0-9_]*");
      assertThat(def.module()).matches("[a-z][a-z0-9-]*");
      assertThat(def.label()).isNotBlank();
      if (def.routable())
        assertThat(def.defaultUrlPattern()).startsWith("/").doesNotContain("//", "..", "?");
    }
    assertThat(provider().routeHints(context())).isEqualTo(provider().routeHints(context()));
  }
}
