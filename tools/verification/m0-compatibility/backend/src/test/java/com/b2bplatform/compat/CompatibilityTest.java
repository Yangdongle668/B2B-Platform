package com.b2bplatform.compat;
import com.b2bplatform.compat.catalog.*;
import com.b2bplatform.compat.infra.ProbePersistence;
import com.b2bplatform.compat.receipts.ProbeListener;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.IncompleteEventPublications;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompatibilityTest {
    @Autowired ProbeMapper mapper;
    @Autowired ProbeWriter writer;
    @Autowired ProbeListener listener;
    @Autowired JdbcTemplate jdbc;
    @Autowired IncompleteEventPublications incomplete;
    @BeforeEach void before() { ProbePersistence.site(1); }
    @AfterEach void after() { ProbePersistence.clearSite(); }
    private int receipts(long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM probe_receipt WHERE id=?", Integer.class, id); }
    @Test @Order(1) void flywayAndSiteFilteredReads() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", Integer.class)).isEqualTo(1);
        assertThat(mapper.selectById(1L)).isNotNull();
        assertThat(mapper.selectById(2L)).isNull();
    }
    @Test @Order(2) void siteFilteredWritesRejectAnotherSite() {
        var row = new ProbeRow(); row.setId(2L); row.setTitle("should not write");
        assertThat(mapper.updateById(row)).isZero();
        assertThat(jdbc.queryForObject("SELECT title FROM probe_document WHERE id=2", String.class)).isEqualTo("site B");
    }
    @Test @Order(3) void committedEventIsDeliveredAfterCommit() {
        writer.create(101, false);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(receipts(101)).isEqualTo(1));
        assertThat(mapper.selectById(101L)).isNotNull();
    }
    @Test @Order(4) void rollbackPersistsNeitherDocumentNorPublication() {
        assertThatThrownBy(() -> writer.create(102, true)).isInstanceOf(IllegalStateException.class);
        assertThat(mapper.selectById(102L)).isNull();
        assertThat(receipts(102)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE serialized_event LIKE '%102%'", Integer.class)).isZero();
    }
    @Test @Order(5) void failedListenerIsPersistedAndCanBeResubmitted() {
        listener.failNext(); writer.create(103, false);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE STATUS='FAILED' AND SERIALIZED_EVENT LIKE '%103%'", Integer.class)).isEqualTo(1));
        incomplete.resubmitIncompletePublicationsOlderThan(Duration.ZERO);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(receipts(103)).isEqualTo(1));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE completion_date IS NULL AND serialized_event LIKE '%103%'", Integer.class)).isZero());
    }
    @Test @Order(6) void modulithBoundariesVerify() { ApplicationModules.of(ProbeApplication.class).verify(); }
    @Test @Order(7) void java25VirtualThreadsAreAvailable() throws Exception {
        var task = new java.util.concurrent.FutureTask<Boolean>(() -> Thread.currentThread().isVirtual());
        Thread.ofVirtual().start(task); assertThat(task.get()).isTrue();
    }
}
