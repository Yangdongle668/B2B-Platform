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

public abstract class DataSourceProviderConformance {
  protected abstract DataSourceProvider provider();

  protected abstract DataSourceRequest request(int limit);

  protected abstract void seedPublishedAndUnpublished();

  protected abstract Set<String> expectedPublishedIds();

  @Test
  public void resultIsBoundedTaggedAndContainsOnlyPublishedLanguage() {
    seedPublishedAndUnpublished();
    var result = provider().resolve(request(1));
    assertThat(result.itemCount()).isBetween(0, 1);
    assertThat(result.cacheTags())
        .isNotEmpty()
        .allMatch(t -> t.value().startsWith("s" + request(1).siteId()));
    var ids = ReferenceSet.scan(result.data()).contentIds();
    assertThat(ids.stream().map(Object::toString).toList()).isSubsetOf(expectedPublishedIds());
    var validation = new ValidationResult();
    provider().validateParams(request(1).params(), validation);
    assertThat(validation.hasErrors()).isFalse();
  }
}
