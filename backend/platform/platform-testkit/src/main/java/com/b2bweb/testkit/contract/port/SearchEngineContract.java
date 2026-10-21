package com.b2bweb.testkit.contract.port;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
public abstract class SearchEngineContract {
 protected abstract SearchEngineClient api();
 void done(long task){assertThat(api().await(task,Duration.ofSeconds(20)).state()).isEqualTo(SearchTaskState.SUCCEEDED);}
 @Test public void acceptedDocumentsAreDetachedFilterableAndCursorBelongsToGeneration(){
  String name="s1_contract_"+UUID.randomUUID().toString().replace("-",""),other=name+"_next";
  var settings=new SearchIndexSettings("id",List.of("title"),List.of("type"),List.of("rank"),List.of(),null,Map.of(),Set.of());
  try{
   api().ensureIndex(name,settings).ifPresent(this::done);api().ensureIndex(other,settings).ifPresent(this::done);
   var json=JsonMapper.builder().build();var one=json.createObjectNode().put("id","1").put("title","precision").put("type","PRODUCT").put("rank",2);var two=json.createObjectNode().put("id","2").put("title","precision").put("type","ARTICLE").put("rank",1);
   done(api().upsert(name,List.of(one,two)));one.put("title","mutated");
   var result=api().search(name,new SearchRequest("precision",SearchFilters.in("type",List.of("PRODUCT","ARTICLE")),List.of("type"),List.of("rank:asc"),0,10,List.of(),List.of()));
   assertThat(result.hits()).extracting(hit->hit.path("id").asString()).containsExactly("2","1");assertThat(result.facetDistribution().get("type")).containsEntry("PRODUCT",1L);
   var page=api().scanDocuments(name,CursorRequest.first(1));assertThat(page.items()).hasSize(1);assertThat(page.nextCursor()).isNotNull();
   assertThatThrownBy(()->api().scanDocuments(other,new CursorRequest(page.nextCursor(),1))).isInstanceOf(IllegalArgumentException.class);
   done(api().delete(name,List.of("1")));assertThat(api().count(name)).isEqualTo(1);
  }finally{api().deleteIndex(name);api().deleteIndex(other);}
 }
}
