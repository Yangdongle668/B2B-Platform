package com.b2bweb.testkit.conformance;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.translation.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
public abstract class TranslatableResourceProviderConformance {
 protected abstract TranslatableResourceProvider provider();
 protected abstract TranslationContext context();
 protected abstract String resourceType();
 protected abstract void site(long id);
 protected abstract void seed(long site,String id,String locale,JsonNode value);
 protected abstract JsonNode target();
 protected abstract String unitId();
 @Test public void cursorIsStableSiteScopedAndApplyKeepsOtherFields() {
  var json=tools.jackson.databind.json.JsonMapper.builder().build();site(1);
  for(String id:List.of("a","b","c"))seed(1,id,"zh-CN",json.createObjectNode().put("title","中文 "+id));
  seed(2,"other","zh-CN",json.createObjectNode().put("title","其他站点"));
  var one=provider().resourceIds(resourceType(),CursorRequest.first(1));assertThat(one.items()).hasSize(1);assertThat(one.nextCursor()).isNotNull();
  var two=provider().resourceIds(resourceType(),new CursorRequest(one.nextCursor(),10));assertThat(two.items()).doesNotContainAnyElementsOf(one.items());assertThat(two.items()).doesNotContain("other");
  site(2);assertThatThrownBy(()->provider().resourceIds(resourceType(),new CursorRequest(one.nextCursor(),1))).isInstanceOf(IllegalArgumentException.class);
  site(1);seed(1,"a","en",json.createObjectNode().put("title","Old").put("code","A-123"));
  assertThat(provider().extract(context())).extracting(TranslationUnit::text).containsExactly("中文 a");provider().apply(context(),Map.of(unitId(),"English"));assertThat(target().path("title").asString()).isEqualTo("English");assertThat(target().path("code").asString()).isEqualTo("A-123");
 }
}
