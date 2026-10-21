package com.b2bweb.application;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.site.SettingsApi;
import com.b2bweb.core.directory.AuditRetention;
import com.b2bweb.core.support.LeasedTsidIds;
import com.b2bweb.core.support.cache.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import tools.jackson.databind.json.JsonMapper;
@SpringBootTest(properties="spring.profiles.active=test")
class RuntimeMaintenanceTest {
 @Autowired DataSource source;
 @Autowired JdbcTemplate jdbc;
 @Autowired SettingsApi settings;
 @Autowired com.b2bweb.api.security.AuditApi audit;
 @Autowired RedisConnectionFactory redis;
 @Autowired JsonMapper json;
 @Test void concurrentInstancesDoNotShareNodesAndStaleHolderStopsPermanently() throws Exception {
  try(var first=new LeasedTsidIds(source,Clock.systemUTC(),811);var second=new LeasedTsidIds(source,Clock.systemUTC(),811)) {
   try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
    var a=executor.submit(()->{first.start();return first.nextId();});var b=executor.submit(()->{second.start();return second.nextId();});assertThat(a.get()).isNotEqualTo(b.get());
   }
   assertThat(first.node()).isNotEqualTo(second.node());
   jdbc.update("UPDATE sys_lock SET locked_by='controlled-successor' WHERE lock_name=?","core:tsid-node:"+first.node());
   first.renew();assertThat(first.available()).isFalse();assertThatThrownBy(first::nextId).isInstanceOf(IllegalStateException.class);
   assertThat(second.nextId()).isPositive();second.close();assertThatThrownBy(second::nextId).isInstanceOf(IllegalStateException.class);
   // Close keeps the quarantine instead of immediately recycling a node.
   assertThat(jdbc.queryForObject("SELECT lock_until>CURRENT_TIMESTAMP(3) FROM sys_lock WHERE lock_name=?",Boolean.class,"core:tsid-node:"+second.node())).isTrue();
  }
 }
 @Test void auditRetentionHonoursExactBoundaryAndBatches() {
  Instant now=Instant.parse("2026-10-21T04:00:00Z");int days=settings.getSystem("core.retention",tools.jackson.databind.JsonNode.class).path("auditDays").asInt();Instant boundary=now.minus(Duration.ofDays(days));
  jdbc.update("DELETE FROM sys_audit_log WHERE action='contract.retention'");
  try {
   for(int i=0;i<1002;i++)jdbc.update("INSERT INTO sys_audit_log(id,module_id,action,summary,created_at) VALUES(?,'core','contract.retention','保留期测试',?)",881000L+i,java.sql.Timestamp.from(i<1001?boundary.minusMillis(1):boundary));
   var task=new AuditRetention(jdbc,settings,Clock.fixed(now,ZoneOffset.UTC));assertThat(task.clean()).isGreaterThanOrEqualTo(1001);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_audit_log WHERE action='contract.retention'",Integer.class)).isEqualTo(1);
   assertThat(task.clean()).isZero();
  } finally {jdbc.update("DELETE FROM sys_audit_log WHERE action='contract.retention'");}
 }
 @Test void cacheStatisticsAreExportedWithBoundedNames() throws Exception {
  var registry=new SimpleMeterRegistry();try(var caches=new LocalCaches(redis,json,registry)) {
   BroadcastCache<String,String> cache=caches.create("core.metrics_contract",new LocalCacheSpec(2,Duration.ofSeconds(10)));cache.getIfPresent("missing");cache.get("key",ignored->"value");cache.getIfPresent("key");
   assertThat(registry.get("cache.gets").tags("cache","core.metrics_contract","result","hit").functionCounter().count()).isPositive();
   assertThat(registry.getMeters()).allSatisfy(meter->assertThat(meter.getId().getTags()).noneSatisfy(tag->assertThat(tag.getKey()).isIn("userId","path","contentId")));
  }
 }
 @Test void auditBoundsUtf8DiffAndMasksNestedSecretsAndCapturesRequestMetadata() {
  var request=new org.springframework.mock.web.MockHttpServletRequest();request.setRemoteAddr("198.51.100.17");request.addHeader("User-Agent","x".repeat(600));
  var previous=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
  org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
  org.slf4j.MDC.put("traceId","1".repeat(32));
  try {
   audit.record(new com.b2bweb.api.security.AuditEntry("core","contract.masked",null,null,"审计掩码",Map.of("nested",new com.b2bweb.api.security.AuditChange(null,Map.of("value",com.b2bweb.api.common.Secret.of("sensitive-fixture-value")))) ,com.b2bweb.api.security.AuditResult.SUCCESS,true,true));
   String encoded=jdbc.queryForObject("SELECT diff FROM sys_audit_log WHERE action='contract.masked' ORDER BY id DESC LIMIT 1",String.class);assertThat(encoded).contains("****").doesNotContain("sensitive-fixture-value");
   audit.record(new com.b2bweb.api.security.AuditEntry("core","contract.large_audit",null,null,"超长差异",Map.of("body",new com.b2bweb.api.security.AuditChange(null,"汉".repeat(40000))),com.b2bweb.api.security.AuditResult.SUCCESS,false,true));
   var row=jdbc.queryForMap("SELECT diff,ip,user_agent,trace_id FROM sys_audit_log WHERE action='contract.large_audit' ORDER BY id DESC LIMIT 1");String diff=(String)row.get("diff");assertThat(diff.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(65536);assertThat(json.readTree(diff).path("__truncated").path("to").asBoolean()).isTrue();
   assertThat(row.get("ip")).isEqualTo("198.51.100.17");assertThat((String)row.get("user_agent")).hasSize(500);assertThat(row.get("trace_id")).isEqualTo("1".repeat(32));
  } finally {
   org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previous);org.slf4j.MDC.remove("traceId");jdbc.update("DELETE FROM sys_audit_log WHERE action IN ('contract.masked','contract.large_audit')");
  }
 }
}
