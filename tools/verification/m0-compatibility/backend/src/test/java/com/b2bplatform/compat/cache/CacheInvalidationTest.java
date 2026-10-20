package com.b2bplatform.compat.cache;

import com.b2bplatform.compat.ProbeApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/** Real Modulith JDBC registry + Redis + loopback CDN failure injection. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheInvalidationTest {
    public record Invalidation(String eventId, long site, Set<String> tags) {}
    static final String TYPE = Invalidation.class.getName();
    static HttpServer cdn;
    static URI endpoint;
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicInteger purges = new AtomicInteger();
    static final AtomicLong documentId = new AtomicLong(20000);
    static ConfigurableApplicationContext context;
    JdbcTemplate jdbc;
    CacheProtocolModel cache;

    @TestConfiguration(proxyBeanMethods=false)
    static class Configuration {
        @Bean CacheProtocolModel cacheProtocol() { return new CacheProtocolModel(); }
        @Bean PurgeListener purgeListener(CacheProtocolModel cache) { return new PurgeListener(cache); }
    }
    static class PurgeListener {
        final CacheProtocolModel cache;
        final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        final ObjectMapper json = new ObjectMapper();
        PurgeListener(CacheProtocolModel cache) { this.cache=cache; }
        @ApplicationModuleListener
        public void receive(Invalidation event) throws Exception {
            cache.invalidate(event.site(), event.tags());
            var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(2))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(event))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200 || !json.readTree(response.body()).path("success").asBoolean(false))
                throw new IllegalStateException("CDN purge not acknowledged");
        }
    }
    static void openContext() {
        context=new SpringApplicationBuilder(ProbeApplication.class, Configuration.class)
            .web(WebApplicationType.NONE)
            .properties("spring.modulith.events.republish-outstanding-events-on-restart=false").run();
    }
    @BeforeAll static void start() throws Exception {
        cdn=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        cdn.createContext("/purge", exchange -> {
            exchange.getRequestBody().readAllBytes(); requests.incrementAndGet();
            String fault=mode.get();
            if(fault.equals("lost")) { purges.incrementAndGet(); exchange.close(); return; }
            if(fault.equals("ok")) purges.incrementAndGet();
            String body=switch(fault) {case "false" -> "{\"success\":false}"; case "malformed" -> "{"; default -> "{\"success\":true}";};
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fault.equals("503")?503:200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        cdn.start(); endpoint=URI.create("http://localhost:"+cdn.getAddress().getPort()+"/purge"); openContext();
    }
    @BeforeEach void setup() {
        jdbc=context.getBean(JdbcTemplate.class); cache=context.getBean(CacheProtocolModel.class);
        assertThat(pending()).isZero();
        cache.redis().flushdb(); cache.recoverQuiescentFixture();
        mode.set("ok"); requests.set(0); purges.set(0);
    }
    @AfterAll static void stop() {if(context!=null)context.close();if(cdn!=null)cdn.stop(0);}
    int pending() {return jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE=? AND COMPLETION_DATE IS NULL",Integer.class,TYPE);}
    int failed(String id) {return jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE=? AND JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.eventId'))=? AND STATUS='FAILED'",Integer.class,TYPE,id);}
    Invalidation publish(boolean rollback) {
        var event=new Invalidation(UUID.randomUUID().toString(),1,Set.of("s1:document"));
        long id=documentId.incrementAndGet();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx -> {
            jdbc.update("INSERT INTO probe_document VALUES (?,1,'cache transaction')",id);
            context.publishEvent(event); if(rollback)tx.setRollbackOnly();
        });
        if(rollback)assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM probe_document WHERE id=?",Integer.class,id)).isZero();
        return event;
    }
    void awaitFailed(Invalidation event) {await().atMost(Duration.ofSeconds(12)).untilAsserted(()->assertThat(failed(event.eventId())).isEqualTo(1));}
    void replayAndDrain() {
        context.getBean(IncompleteEventPublications.class).resubmitIncompletePublicationsOlderThan(Duration.ZERO);
        await().atMost(Duration.ofSeconds(12)).untilAsserted(()->assertThat(pending()).isZero());
    }
    @Test @Order(1) void rollbackLeavesNeitherSourceNorPublication() {
        var event=publish(true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE=? AND JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.eventId'))=?",Integer.class,TYPE,event.eventId())).isZero();
        assertThat(requests).hasValue(0);
    }
    @Test @Order(2) void committedInvalidationCompletesOnlyAfterRedisAndCdn() {
        var fence=cache.capture(1).orElseThrow();
        assertThat(cache.put(1,"page","a",new CacheProtocolModel.Value("old",java.time.Instant.now().plusSeconds(50)),Set.of("s1:document"),fence)).isTrue();
        publish(false); await().atMost(Duration.ofSeconds(12)).untilAsserted(()->assertThat(purges).hasValue(1));
        await().atMost(Duration.ofSeconds(12)).untilAsserted(()->assertThat(pending()).isZero());
        assertThat(cache.get(1,"page","a")).isEmpty();
        assertThat(cache.put(1,"page","a",new CacheProtocolModel.Value("late",java.time.Instant.now().plusSeconds(50)),Set.of("s1:document"),fence)).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"503","false","malformed"}) @Order(3)
    void unacknowledgedCdnKeepsDurableObligation(String fault) {
        mode.set(fault);var event=publish(false);awaitFailed(event); assertThat(pending()).isEqualTo(1);
        mode.set("ok");replayAndDrain();assertThat(purges).hasValue(1);assertThat(requests.get()).isGreaterThanOrEqualTo(2);
    }
    @Test @Order(4) void lostAckAfterPurgeSafelyPurgesAgain() {
        mode.set("lost");var event=publish(false);awaitFailed(event);assertThat(purges.get()).isGreaterThanOrEqualTo(1);
        mode.set("ok");replayAndDrain();assertThat(purges.get()).isGreaterThanOrEqualTo(2);
    }
    @Test @Order(5) void redisFailureRetainsObligationAndBlocksRecoveryReadiness() {
        cache.disconnect();var event=publish(false);awaitFailed(event);assertThat(requests).hasValue(0);
        cache.reconnect();var recovery=cache.beginRecovery();
        assertThat(cache.finishRecovery(recovery,()->pending()>0)).isFalse();assertThat(cache.capture(1)).isEmpty();
        replayAndDrain();assertThat(cache.finishRecovery(recovery,()->pending()>0)).isTrue();assertThat(cache.capture(1)).isPresent();
    }
    @Test @Order(6) void failedPublicationSurvivesActualApplicationContextRestart() {
        mode.set("503");var event=publish(false);awaitFailed(event);
        var recovery=cache.beginRecovery();context.close();openContext();
        jdbc=context.getBean(JdbcTemplate.class);cache=context.getBean(CacheProtocolModel.class);
        assertThat(failed(event.eventId())).isEqualTo(1);assertThat(cache.capture(1)).isEmpty();
        assertThat(cache.finishRecovery(recovery,()->pending()>0)).isFalse();
        mode.set("ok");replayAndDrain();assertThat(cache.finishRecovery(recovery,()->pending()>0)).isTrue();
    }
}
