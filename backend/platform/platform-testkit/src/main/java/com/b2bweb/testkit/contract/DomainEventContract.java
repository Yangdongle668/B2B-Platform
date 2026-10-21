package com.b2bweb.testkit.contract;

import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.event.*;
import com.b2bweb.testkit.event.EventSamples;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import tools.jackson.databind.json.JsonMapper;

/** Run against both the standalone testkit mapper and the application's actual configured mapper. */
public abstract class DomainEventContract {
 protected abstract JsonMapper json();
 @Test public void everyEventHasExplicitSampleAndImmutableComponents() throws Exception {
  var scanner=new ClassPathScanningCandidateComponentProvider(false);scanner.addIncludeFilter(new AssignableTypeFilter(DomainEvent.class));
  Set<Class<?>> found=new HashSet<>();
  for(var candidate:scanner.findCandidateComponents("com.b2bweb.api.event")) {
   Class<?> type=Class.forName(candidate.getBeanClassName());found.add(type);
   assertThat(type.isRecord()).as(type.getSimpleName()).isTrue();assertThat(type.getSimpleName()).endsWith("Event");
   var components=type.getRecordComponents();assertThat(components[0].getName()).isEqualTo("meta");assertThat(components[0].getType()).isEqualTo(EventMeta.class);
   var sample=EventSamples.all().get(type);assertThat(sample).as("explicit sample for %s",type).isNotNull();
   for(var component:components) {
    assertThat(component.getName()).as("PII heuristic %s",type).isNotIn("email","phone","name","ip","message","fields");
    var value=component.getAccessor().invoke(sample);
    if(value instanceof List<?> list) assertThatThrownBy(()->((List<Object>)list).add(new Object())).isInstanceOf(UnsupportedOperationException.class);
    if(value instanceof Set<?> set) assertThatThrownBy(()->((Set<Object>)set).add(new Object())).isInstanceOf(UnsupportedOperationException.class);
    if(value instanceof Map<?,?> map) assertThatThrownBy(()->((Map<Object,Object>)map).put(new Object(),new Object())).isInstanceOf(UnsupportedOperationException.class);
   }
  }
  assertThat(found).containsExactlyInAnyOrderElementsOf(EventSamples.all().keySet());
 }
 @Test public void roundTripUsesStringIdsAndVersionWithoutPrecisionLoss() {
  for(var sample:EventSamples.all().values()) {
   String encoded=json().writeValueAsString(sample);var tree=json().readTree(encoded);
   assertThat(tree.path("schemaVersion").asInt()).as(sample.eventType()).isEqualTo(sample.schemaVersion());
   assertThat(tree.path("meta").path("siteId").isString()).isTrue();
   for(var field:sample.getClass().getRecordComponents()) if(field.getName().endsWith("Id") && !tree.path(field.getName()).isNull())assertThat(tree.path(field.getName()).isString()).as(sample.eventType()+"."+field.getName()).isTrue();
   assertThat(json().readValue(encoded,sample.getClass())).isEqualTo(sample);
  }
 }
}
