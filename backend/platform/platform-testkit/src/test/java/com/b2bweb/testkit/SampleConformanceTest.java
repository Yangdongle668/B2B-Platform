package com.b2bweb.testkit;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.testkit.conformance.*;
import com.b2bweb.testkit.example.SampleExtensions;
import org.junit.jupiter.api.Nested;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
class SampleConformanceTest {
 @Nested class ContentTranslation extends TranslatableContentProviderConformance {
  final TestPlatform p=TestPlatform.standard().useSite(1);final SampleExtensions sample=new SampleExtensions(p);final JsonMapper json=JsonMapper.builder().build();
  protected TranslatableContentProvider provider(){return sample.new ContentTranslation();}protected TranslationContext context(){return TranslationContext.forContent(1,new TranslationSubject.Content(1,"SAMPLE",11,22,1),"en",1,null);}protected void seedSource(JsonNode v){sample.seed(1,11,"zh-CN",v);}protected void seedTarget(JsonNode v){sample.seed(1,22,"en",v);}protected JsonNode target(){return sample.translated(1,22,"en");}protected String unitId(){return "sample.title";}protected JsonNode value(String title){return json.createObjectNode().put("title",title);}
 }
 @Nested class ResourceTranslation extends TranslatableResourceProviderConformance {
  final TestPlatform p=TestPlatform.standard().useSite(1);final SampleExtensions sample=new SampleExtensions(p);
  protected TranslatableResourceProvider provider(){return sample.new ResourceTranslation();}protected TranslationContext context(){return TranslationContext.forResource(1,"sample.label","a","en",1);}protected String resourceType(){return "sample.label";}protected void site(long id){p.useSite(id);}protected void seed(long site,String id,String locale,JsonNode v){sample.resource(site,id,locale,v);}protected JsonNode target(){return sample.resourceValue(1,"a","en");}protected String unitId(){return "sample.title";}
 }
 @Nested class Search extends SearchDocumentProviderConformance {
  final TestPlatform p=TestPlatform.standard().useSite(1);final SampleExtensions sample=new SampleExtensions(p);final JsonMapper json=JsonMapper.builder().build();
  protected SearchDocumentProvider provider(){return sample.new SearchDocuments();}protected SearchDocumentContext context(JsonNode v){return new SearchDocumentContext(1,1,11,"SAMPLE","zh-CN",1,v,null);}protected JsonNode snapshot(String title){return json.createObjectNode().set("sample",json.createObjectNode().put("title",title));}protected void unrelatedDraft(String title){sample.seed(1,11,"zh-CN",json.createObjectNode().put("title",title));}
 }
}
