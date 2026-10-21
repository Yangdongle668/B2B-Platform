package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;
import com.b2bweb.core.support.schema.ComponentSchemaCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest
@ActiveProfiles("test")
class ComponentSchemasTest {
  @Autowired ComponentSchemaCatalog schemas;
  final JsonMapper json = JsonMapper.builder().build();
  ObjectNode layout() { return (ObjectNode) json.readTree("{\"version\":1,\"sections\":[{\"id\":\"hero-one\",\"component\":\"hero\",\"componentVersion\":1,\"props\":{\"title\":\"标题\",\"description\":\"说明\"}}]}"); }
  @Test void sharedJsonIsCompiledAndReturnedAsDefensiveCopies() {
    assertThat(schemas.types()).contains("hero"); assertThat(schemas.moduleOf("hero")).isEqualTo("page");
    ((ObjectNode) schemas.component("hero")).put("version", 99);
    assertThat(schemas.component("hero", 1).path("version").asInt()).isEqualTo(1);
    assertThat(schemas.translatablePaths("hero", 1)).containsExactly("/title", "/description");
    assertThat(schemas.validateLayout(layout(), "page")).isEmpty();
  }
  @Test void unknownFieldsUrlsInvalidPropsAndVersionsAreRejectedWithPointers() {
    var layout = layout(); var section = (ObjectNode) layout.path("sections").path(0);
    ((ObjectNode) section.path("props")).put("actionPath", "/zh/secret");
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.field()).startsWith("/sections/0/props"));
    ((ObjectNode) section.path("props")).remove("actionPath"); section.put("componentVersion", 2);
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.field()).endsWith("componentVersion"));
    section.put("componentVersion", 4_294_967_297L);
    assertThat(schemas.validateLayout(layout, "page")).isNotEmpty();
    section.put("componentVersion", 1); section.put("hidden", "false");
    assertThat(schemas.validateLayout(layout, "page")).isNotEmpty();
  }
  @Test void tokensDuplicateIdsAndUnsupportedChildrenAreRejected() {
    var layout = layout(); var section = (ObjectNode) layout.path("sections").path(0);
    section.putObject("style").put("background", "url(evil)");
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.code()).isEqualTo("token"));
    section.remove("style"); ((tools.jackson.databind.node.ArrayNode) layout.path("sections")).add(section.deepCopy());
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.code()).isEqualTo("id"));
    ((tools.jackson.databind.node.ArrayNode) layout.path("sections")).remove(1);
    section.putObject("children").putArray("main").add(section.deepCopy().put("id", "nested"));
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.code()).isEqualTo("slot"));
  }
  @Test void archivedSchemasCollectTypedReferencesAndCompoundTranslationPaths() {
    // This component exists only under src/test/resources and is never shipped in the application.
    var layout = json.readTree("{\"version\":1,\"sections\":[{\"id\":\"old\",\"component\":\"rich-text\",\"componentVersion\":1,\"props\":{\"headline\":\"标题\",\"image\":{\"$media\":\"101\"},\"related\":{\"$content\":\"202\"},\"form\":\"lead-form\",\"cta\":{\"label\":\"按钮\",\"link\":{\"$content\":\"303\"},\"style\":\"primary\"}}}]}");
    assertThat(schemas.refKeys(layout, "media")).containsExactly("101");
    assertThat(schemas.refKeys(layout, "content")).containsExactly("202", "303");
    assertThat(schemas.refKeys(layout, "form")).containsExactly("lead-form");
    assertThat(schemas.translatablePaths("rich-text", 1)).containsExactly("/headline", "/cta/label");
    assertThat(schemas.translatablePaths("rich-text", 2)).contains("/caption");
    assertThat(schemas.validateLayout(layout, "page")).anySatisfy(error -> assertThat(error.code()).isEqualTo("version"));
    ((ObjectNode) layout.path("sections").path(0)).put("componentVersion", 2);
    assertThat(schemas.validateLayout(layout, "page")).isEmpty();
    ((ObjectNode) layout.path("sections").path(0).path("props")).putObject("image").put("url", "/private.jpg");
    assertThat(schemas.validateLayout(layout, "page")).isNotEmpty();
  }

}
