package com.b2bweb.application;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.content.PublishTrigger;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;
import com.b2bweb.core.context.SiteContexts;
import com.b2bweb.core.cache.*;
import com.b2bweb.testkit.port.FakeCdnPurger;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest(properties="spring.profiles.active=test")
@Import(CacheRecoveryTest.Fixtures.class)
class CacheRecoveryTest {
 @TestConfiguration static class Fixtures { @Bean FakeCdnPurger testCdn(){return new FakeCdnPurger();} }
 @Autowired CacheRecovery recovery; @Autowired RedisTaggedCache cache; @Autowired FakeCdnPurger cdn;
 @Autowired JdbcTemplate jdbc; @Autowired SiteContexts sites; @Autowired ApplicationEventPublisher publisher;
 @Autowired PlatformTransactionManager manager; @Autowired StringRedisTemplate redis;
 @Autowired IncompleteEventPublications publications;
 @BeforeEach void seed(){
  jdbc.update("INSERT INTO sys_site(id,code,name,theme_key,timezone,status) VALUES(764,'cache-recovery','恢复测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE'");
  cdn.available(true);
 }
 @Test void closedGateAndObsoleteRecoveryCannotReadWriteOrOpenNewEpoch(){
  sites.runAs(764,()->{
   var first=cache.beginRecovery(UUID.randomUUID().toString()); assertThat(cache.beginLoad()).isEmpty();
   var next=cache.beginRecovery(UUID.randomUUID().toString()); assertThat(cache.finishRecovery(first.get(0),first.get(1))).isFalse();
   assertThat(cache.finishRecovery(next.get(0),next.get(1))).isTrue();
   cache.put("delivery.recovery","a","old",Set.of(CacheTag.site(764)),Duration.ofSeconds(20));
   // Simulate a restored control record from a different Redis process. Lua reads live run_id.
   redis.opsForHash().put("sys:core:cache:control","boot","previous-process");
   assertThat(cache.get("delivery.recovery","a",String.class)).isEmpty(); assertThat(cache.beginLoad()).isEmpty();
   assertThat(cache.finishRecovery("previous-process",next.get(1))).isFalse();
  });
 }
 @Test void durableBoundaryAndCdnFailureKeepGateClosedUntilConfirmed(){
  cache.beginRecovery(UUID.randomUUID().toString()); cdn.available(false);
  try { assertThat(recovery.recover()).isFalse(); } catch (com.b2bweb.api.common.ExternalServiceException expected) { /* CDN confirmation failed; gate stays closed. */ }
  assertThat(jdbc.queryForObject("SELECT status FROM sys_cache_recovery WHERE id=1",String.class)).isEqualTo("DRAINING");
  sites.runAs(764,()->{
   assertThat(cache.beginLoad()).isEmpty();
   var value=cache.getOrLoadWithFreshness("delivery.recovery","uncached",String.class,Duration.ofSeconds(20),Duration.ZERO,()->Optional.of(new CacheEntry<>("origin",Set.of(),Instant.now().plusSeconds(20)))).orElseThrow();
   assertThat(value.freshUntil()).isBeforeOrEqualTo(Instant.now());
  });
  cdn.available(true); org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(20)).until(recovery::recover);
  assertThat(jdbc.queryForObject("SELECT status FROM sys_cache_recovery WHERE id=1",String.class)).isEqualTo("READY");
 }
 @Test void sourcePublicationBarrierIsHeldUntilTransactionCommits() throws Exception {
  var held=new CountDownLatch(1); var release=new CountDownLatch(1);
  try(var executor=Executors.newSingleThreadExecutor()){
   var source=executor.submit(()->new TransactionTemplate(manager).executeWithoutResult(status->{
    sites.runAs(764,()->publisher.publishEvent(new ContentCreatedEvent(new EventMeta("764001",Instant.now(),764L,"system"),11,"PAGE","page",12)));
    held.countDown(); try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
   }));
   try{
    assertThat(held.await(5,TimeUnit.SECONDS)).isTrue();
    try(var connection=jdbc.getDataSource().getConnection(); var statement=connection.createStatement()){
     statement.execute("SET innodb_lock_wait_timeout=1");
     assertThatThrownBy(()->statement.executeQuery("SELECT id FROM sys_cache_recovery WHERE id=1 FOR UPDATE")).isInstanceOf(java.sql.SQLException.class);
    }
   }finally{release.countDown();}
   source.get(5,TimeUnit.SECONDS);
  }
 }
 @Test void reliableInvalidationIsReplayedAfterCdnFailure(){
  long id=764002; cdn.available(false);
  new TransactionTemplate(manager).executeWithoutResult(status->sites.runAs(764,()->publisher.publishEvent(new ContentPublishedEvent(new EventMeta(Long.toString(id),Instant.now(),764L,"system"),11,"PAGE","zh-CN",12,13,1,"/zh/page",null,true,true,PublishTrigger.MANUAL))));
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE LIKE '%ContentPublishedEvent' AND SERIALIZED_EVENT LIKE '%764002%' AND COMPLETION_DATE IS NULL",Long.class)).isPositive());
  cache.beginRecovery(UUID.randomUUID().toString()); assertThat(recovery.recover()).isFalse();
  cdn.available(true); publications.resubmitIncompletePublicationsOlderThan(Duration.ZERO);
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE LIKE '%ContentPublishedEvent' AND SERIALIZED_EVENT LIKE '%764002%' AND COMPLETION_DATE IS NULL",Long.class)).isZero());
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(20)).until(recovery::recover);
  assertThat(cdn.tagPurges()).anySatisfy(tags->assertThat(tags).contains(CacheTag.content(764,11)));
 }
 @Test void resourceChangesInvalidateTheSiteEvenWhenNoContentIsPublished(){
  sites.runAs(764,()->{
   var gate=cache.beginRecovery(UUID.randomUUID().toString());assertThat(cache.finishRecovery(gate.get(0),gate.get(1))).isTrue();
   cache.put("delivery.resource","settings","before",Set.of(CacheTag.site(764)),Duration.ofSeconds(30));
   assertThat(cache.get("delivery.resource","settings",String.class)).contains("before");
   new TransactionTemplate(manager).executeWithoutResult(status->publisher.publishEvent(new SiteResourceChangedEvent(new EventMeta("764003",Instant.now(),764L,"system"),"url.pattern","product",ChangeKind.UPDATED)));
  });
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(()->sites.runAs(764,()->assertThat(cache.get("delivery.resource","settings",String.class)).isEmpty()));
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE LIKE '%SiteResourceChangedEvent' AND SERIALIZED_EVENT LIKE '%764003%' AND LISTENER_ID LIKE '%CacheInvalidationListener%' AND COMPLETION_DATE IS NOT NULL",Long.class)).isPositive());
  assertThat(cdn.tagPurges()).anySatisfy(tags->assertThat(tags).contains(CacheTag.site(764)));
 }
}
