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
import tools.jackson.databind.JsonNode;

public abstract class PublishContributorConformance {
  protected abstract PublishContributor provider();

  protected abstract PublishContext context();

  protected abstract void seed();

  @Test
  public void snapshotRoundtripIsStableAndValidationDoesNotThrow() {
    seed();
    var provider = provider();
    assertThat(provider.partKey()).matches("[a-z][a-z0-9_]*");
    var first = provider.contribute(context());
    assertThat(first).isNotNull();
    assertThat(provider.contribute(context())).isEqualTo(first);
    rejectUrls(first);
    assertThatCode(() -> provider.validate(context(), new ValidationResult()))
        .doesNotThrowAnyException();
    provider.restore(context(), first.deepCopy());
    assertThat(provider.contribute(context())).isEqualTo(first);
  }

  private void rejectUrls(JsonNode node) {
    if (node.isString()) assertThat(node.asString()).doesNotMatch("(?i)https?://.*");
    if (node.isContainer()) node.forEach(this::rejectUrls);
  }
}
