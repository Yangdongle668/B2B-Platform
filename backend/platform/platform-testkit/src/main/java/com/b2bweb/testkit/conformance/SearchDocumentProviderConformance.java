package com.b2bweb.testkit.conformance;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.extension.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
public abstract class SearchDocumentProviderConformance {
 protected abstract SearchDocumentProvider provider();
 protected abstract SearchDocumentContext context(JsonNode snapshot);
 protected abstract JsonNode snapshot(String title);
 protected abstract void unrelatedDraft(String title);
 @Test public void projectionUsesImmutablePublishedSnapshotAndDoesNotReadDraft() {
  JsonNode published=snapshot("Published A-123");JsonNode original=published.deepCopy();var first=provider().build(context(published)).orElseThrow();unrelatedDraft("Unpublished secret");
  assertThat(provider().build(context(published))).contains(first);assertThat(published).isEqualTo(original);assertThat(first.title()).isEqualTo("Published A-123");
 }
}
