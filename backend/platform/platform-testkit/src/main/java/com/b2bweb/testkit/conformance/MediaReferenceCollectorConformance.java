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

public abstract class MediaReferenceCollectorConformance {
  protected abstract MediaReferenceCollector provider();

  protected abstract MediaReferenceContext context();

  protected abstract void seed();

  protected abstract Set<Long> expectedIds();

  @Test
  public void stableCompleteReferencesUseValidPaths() {
    seed();
    var first = provider().collectDraft(context());
    assertThat(first).isEqualTo(provider().collectDraft(context()));
    assertThat(first)
        .extracting(MediaReference::mediaId)
        .containsExactlyInAnyOrderElementsOf(expectedIds());
    for (var ref : first) {
      assertThat(ref.part()).matches("[a-z][a-z0-9_]*");
      assertThat(ref.fieldPath())
          .matches("[A-Za-z_][A-Za-z0-9_]*(?:\\[[0-9]+\\]|\\.[A-Za-z_][A-Za-z0-9_]*)*");
    }
  }
}
