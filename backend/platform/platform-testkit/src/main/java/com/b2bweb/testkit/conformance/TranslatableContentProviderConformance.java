package com.b2bweb.testkit.conformance;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.translation.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
/** Consumer semantics: structural extraction and narrow patching may not destroy business fields. */
public abstract class TranslatableContentProviderConformance {
 protected abstract TranslatableContentProvider provider();
 protected abstract TranslationContext context();
 protected abstract void seedSource(JsonNode value);
 protected abstract void seedTarget(JsonNode value);
 protected abstract JsonNode target();
 protected abstract String unitId();
 protected abstract JsonNode value(String title);
 @Test public void extractionIsStableAndApplyPreservesUntranslatedBusinessFields() {
  var source=(tools.jackson.databind.node.ObjectNode)value("型号 A-123 <1>100</1> W");source.put("modelNo","A-123");seedSource(source);
  var target=(tools.jackson.databind.node.ObjectNode)value("旧译文");target.put("modelNo","A-123");target.putObject("business").put("enabled",true);target.putObject("image").put("$media","9007199254740993");seedTarget(target);
  var first=provider().extract(context());assertThat(first).isEqualTo(provider().extract(context()));assertThat(first).extracting(TranslationUnit::key).doesNotHaveDuplicates();assertThat(first).extracting(TranslationUnit::text).contains("型号 A-123 <1>100</1> W");
  provider().apply(context(),Map.of(unitId(),"Model A-123 <1>100</1> W"));
  assertThat(target().path("modelNo").asString()).isEqualTo("A-123");assertThat(target().path("business")).isEqualTo(target.path("business"));assertThat(target().path("image")).isEqualTo(target.path("image"));
  assertThat(source.path("title").asString()).isEqualTo("型号 A-123 <1>100</1> W");
  var before=target().deepCopy();provider().apply(context(),Map.of());assertThat(target()).isEqualTo(before);
 }
}
