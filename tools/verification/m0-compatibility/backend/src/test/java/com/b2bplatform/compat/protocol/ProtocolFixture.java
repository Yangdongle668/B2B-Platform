package com.b2bplatform.compat.protocol;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/** Fixtures may seed/inspect all adapters; the protocol model is deliberately isolated. */
abstract class ProtocolFixture {
    @BeforeEach void seedAuthoritativeSchema() throws Exception {
        try (var c = connect()) {
            for (String table : new String[]{"ntf_message", "ntf_template", "frm_submission", "frm_form", "trn_job_item", "trn_job", "cnt_review", "cnt_revision", "pg_layout", "pg_page", "seo_meta", "cnt_localization", "cnt_content", "mda_usage", "mda_media_localization", "mda_media", "sys_job"}) {
                update(c, "DELETE FROM " + table + " WHERE site_id=?", SITE);
            }
            update(c, "INSERT INTO mda_media(id,site_id,kind,visibility,storage_key,original_name,mime,size,sha256,status,gated_form_key) VALUES(?,?,'DOCUMENT','PRIVATE','probe-object','probe.pdf','application/pdf',4,?,'READY','download-probe')", MEDIA, SITE, "0".repeat(64));
            update(c, "INSERT INTO cnt_content(id,site_id,type,owner_module) VALUES(?,?,'PAGE','page')", CONTENT, SITE);
            update(c, "INSERT INTO cnt_localization(id,site_id,content_id,content_type,locale,draft_title,translation_mode) VALUES(?,?,?,'PAGE','zh-CN','source','SOURCE')", SOURCE, SITE, CONTENT);
            update(c, "INSERT INTO cnt_revision(id,site_id,localization_id,revision_no,payload,payload_hash,published_at) VALUES(?,?,?,1,JSON_OBJECT('body','source'),?,NOW(3))", SOURCE + 10, SITE, SOURCE, hash("source"));
            update(c, "UPDATE cnt_localization SET publish_state='PUBLISHED',published_revision_id=?,published_revision_no=1 WHERE id=?", SOURCE + 10, SOURCE);
            update(c, "INSERT INTO cnt_localization(id,site_id,content_id,content_type,locale,draft_title,translation_mode,source_locale,source_revision_no,translation_state) VALUES(?,?,?,'PAGE','en','original','AUTO','zh-CN',1,'QUEUED')", TARGET, SITE, CONTENT);
            update(c, "INSERT INTO pg_page(content_id,site_id,tree_path) VALUES(?,?,?)", CONTENT, SITE, "/" + CONTENT + "/");
            update(c, "INSERT INTO pg_layout(id,site_id,content_id,locale,content_type,document) VALUES(?,?,?,'en','PAGE',JSON_OBJECT('body','original'))", TARGET, SITE, CONTENT);
            update(c, "INSERT INTO seo_meta(id,site_id,content_id,localization_id,locale,title_override) VALUES(?,?,?,?,'en','original')", TARGET, SITE, CONTENT, TARGET);
            update(c, "INSERT INTO trn_job(id,site_id,job_type,trigger_type,title,target_locale) VALUES(?,?,'CONTENT','PUBLISH','验证翻译','en')", JOB, SITE);
            update(c, "INSERT INTO trn_job_item(id,site_id,job_id,item_kind,target_locale,content_id,content_type,source_localization_id,source_revision_no,target_localization_id) VALUES(?,?,?,'CONTENT','en',?,'PAGE',?,1,?)", ITEM, SITE, JOB, CONTENT, SOURCE, TARGET);
            update(c, "INSERT INTO frm_form(id,site_id,form_key,name,purpose) VALUES(?,?,'download-probe','验证下载','DOWNLOAD')", FORM, SITE);
            for (String locale : new String[]{"zh-CN", "en"}) {
                update(c, "INSERT INTO ntf_template(id,site_id,template_key,locale,audience,module_id,name,subject,body) VALUES(?,?,'form.download.customer',?,'CUSTOMER','form','验证下载','下载','{{downloadUrl}}')", locale.equals("en") ? 761 : 760, SITE, locale);
            }
            c.commit();
        }
    }
    @FunctionalInterface interface Work { void run(Connection c) throws Exception; }
    static void committed(Work work) throws Exception {
        try (var c = connect()) {
            try { work.run(c); c.commit(); }
            catch (Exception e) { c.rollback(); throw e; }
        }
    }
    static long value(String sql, Object... args) throws Exception {
        try (var c = connect()) { return scalar(c, sql, args); }
    }
    static Object field(String sql, Object... args) throws Exception {
        try (var c = connect()) { return one(c, sql, args).values().iterator().next(); }
    }
    static void olderTrash() throws Exception {
        committed(c -> update(c, "UPDATE mda_media SET deleted_at=NOW(3)-INTERVAL 31 DAY WHERE id=?", MEDIA));
    }
    static void publishedReference() throws Exception { committed(c -> Media.reference(c, SITE, true)); }
    static void capture() throws Exception { committed(Translation::capture); }

    static final class Contender implements AutoCloseable {
        final CompletableFuture<Void> future;
        final AtomicLong mysqlConnectionId = new AtomicLong();
        final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        Contender(Work work) {
            future = CompletableFuture.runAsync(() -> {
                try (var c = connect()) {
                    mysqlConnectionId.set(scalar(c, "SELECT CONNECTION_ID()"));
                    try { work.run(c); c.commit(); }
                    catch (Exception e) { c.rollback(); throw e; }
                } catch (Exception e) { throw new CompletionException(e); }
            }, executor);
        }
        void awaitDatabaseLockWait() {
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(20)).untilAsserted(() -> {
                assertThat(mysqlConnectionId.get()).isPositive();
                try (var observer = connect()) {
                    assertThat(scalar(observer, "SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID WHERE t.PROCESSLIST_ID=?", mysqlConnectionId.get())).isPositive();
                }
                assertThat(future.isDone()).isFalse();
            });
        }
        void succeeds() throws Exception { future.get(10, TimeUnit.SECONDS); }
        void rejected(String reason) {
            assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS)).hasRootCauseInstanceOf(Rejected.class).hasRootCauseMessage(reason);
        }
        public void close() throws Exception {
            // Tests always release blocker before joining; bounded cleanup on assertion failure.
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        }
    }
}
