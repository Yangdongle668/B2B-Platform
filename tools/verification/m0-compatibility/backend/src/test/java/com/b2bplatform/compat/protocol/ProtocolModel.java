package com.b2bplatform.compat.protocol;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Executable protocol model, NOT production services or a replacement for platform-api.
 * Uses the authoritative document schema in a disposable database. Nested adapters own
 * their table prefixes; orchestration shares one caller-owned JDBC transaction.
 */
final class ProtocolModel {
    static final long SITE = 701, MEDIA = 710, CONTENT = 720, SOURCE = 721, TARGET = 722;
    static final long JOB = 730, ITEM = 731, PURGE = 740, FORM = 750;

    static Connection connect() throws SQLException {
        String url = Objects.requireNonNull(System.getenv("PROBE_PROTOCOL_JDBC_URL"), "Run verify_backend.py to create the disposable document database");
        require(url.startsWith("jdbc:mysql://mysql:3306/remediation?"), "Only the disposable runner database is allowed");
        var c = DriverManager.getConnection(url, "root", "");
        update(c, "SET SESSION innodb_lock_wait_timeout=8");
        update(c, "SET SESSION time_zone='+00:00'");
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        c.setAutoCommit(false);
        return c;
    }
    static int update(Connection c, String sql, Object... args) throws SQLException {
        try (var p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p.executeUpdate();
        }
    }
    static List<Map<String, Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (var p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            try (var r = p.executeQuery()) {
                var result = new ArrayList<Map<String, Object>>();
                while (r.next()) {
                    var row = new HashMap<String, Object>();
                    for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) row.put(r.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), r.getObject(i));
                    result.add(row);
                }
                return result;
            }
        }
    }
    static Map<String, Object> one(Connection c, String sql, Object... args) throws SQLException {
        var rows = rows(c, sql, args);
        if (rows.isEmpty()) throw new Rejected("NOT_FOUND");
        return rows.getFirst();
    }
    static long number(Object x) { return x instanceof Boolean b ? (b ? 1L : 0L) : ((Number) x).longValue(); }
    static long scalar(Connection c, String sql, Object... args) throws SQLException { return number(one(c, sql, args).values().iterator().next()); }
    static class Rejected extends RuntimeException { Rejected(String message) { super(message); } }
    static void require(boolean accepted, String error) { if (!accepted) throw new Rejected(error); }
    static String hash(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    static final class Tasks {
        static void purge(Connection c) throws SQLException {
            update(c, "INSERT INTO sys_job(id,site_id,module_id,job_type,title,params,max_attempts) VALUES(?,?,'media','media.purge_object','验证清理',JSON_OBJECT('mediaId',?,'storageKey','probe-object'),6)", PURGE, SITE, MEDIA);
        }
        static void complete(Connection c) throws SQLException { update(c, "UPDATE sys_job SET status='SUCCEEDED' WHERE id=? AND site_id=?", PURGE, SITE); }
    }
    static final class Media {
        static Map<String, Object> lock(Connection c, long site, boolean shared) throws SQLException {
            return one(c, "SELECT *, (deleted_at <= NOW(3)-INTERVAL 30 DAY) AS purge_due FROM mda_media WHERE id=? AND site_id=? " + (shared ? "FOR SHARE" : "FOR UPDATE"), MEDIA, site);
        }
        static void usable(Map<String, Object> media, boolean published) {
            require(media.get("deleted_at") == null && media.get("purge_job_id") == null, "MEDIA_DELETED");
            require("READY".equals(media.get("status")) || !published && "PROCESSING".equals(media.get("status")), "MEDIA_NOT_READY");
        }
        static void reference(Connection c, long site, boolean published) throws SQLException {
            usable(lock(c, site, true), published);
            update(c, "INSERT INTO mda_usage(id,site_id,media_id,owner_type,owner_id,locale,stage,part,field_path) VALUES(?,?,?,'CONTENT',?,'en',?,'layout','image')", MEDIA + 1, site, MEDIA, CONTENT, published ? "PUBLISHED" : "DRAFT");
        }
        static void unreferenced(Connection c) throws SQLException {
            require(rows(c, "SELECT id FROM mda_usage WHERE site_id=? AND media_id=? LIMIT 1 FOR UPDATE", SITE, MEDIA).isEmpty(), "MEDIA_IN_USE");
        }
        static void softDelete(Connection c) throws SQLException {
            var media = lock(c, SITE, false);
            if (media.get("deleted_at") != null) return;
            unreferenced(c);
            update(c, "UPDATE mda_media SET deleted_at=NOW(3),version=version+1,download_gate_version=download_gate_version+1 WHERE id=? AND site_id=?", MEDIA, SITE);
        }
        static void restore(Connection c, Path blob) throws SQLException {
            var media = lock(c, SITE, false);
            require(media.get("purge_job_id") == null, "MEDIA_PURGE_STARTED");
            require(Files.exists(blob) && "READY".equals(media.get("status")), "MEDIA_OBJECT_UNAVAILABLE");
            if (media.get("deleted_at") == null) return;
            update(c, "UPDATE mda_media SET deleted_at=NULL,version=version+1,download_gate_version=download_gate_version+1 WHERE id=? AND site_id=?", MEDIA, SITE);
        }
        static void registerPurge(Connection c) throws SQLException {
            var media = lock(c, SITE, false);
            require(media.get("deleted_at") != null && number(media.get("purge_due")) == 1, "MEDIA_RETENTION");
            unreferenced(c);
            if (media.get("purge_job_id") != null) return;
            Tasks.purge(c);
            update(c, "UPDATE mda_media SET purge_job_id=? WHERE id=? AND site_id=?", PURGE, MEDIA, SITE);
        }
        static void purgeObject(Path blob, boolean fail, boolean crashBeforeAck) throws Exception {
            if (fail) throw new IOException("Injected object-store failure");
            Files.deleteIfExists(blob); // idempotent external side effect
            if (crashBeforeAck) throw new IOException("Injected process loss after external delete");
            try (var c = connect()) {
                var media = lock(c, SITE, false);
                require(number(media.get("purge_job_id")) == PURGE, "MEDIA_PURGE_CHANGED");
                update(c, "DELETE FROM mda_media_localization WHERE media_id=? AND site_id=?", MEDIA, SITE);
                update(c, "DELETE FROM mda_media WHERE id=? AND site_id=?", MEDIA, SITE);
                Tasks.complete(c);
                c.commit();
            }
        }
        static void gate(Connection c, String form) throws SQLException {
            lock(c, SITE, false);
            update(c, "UPDATE mda_media SET gated_form_key=?,download_gate_version=download_gate_version+1,version=version+1 WHERE id=? AND site_id=?", form, MEDIA, SITE);
        }
        static long gateForDownload(Connection c) throws SQLException {
            var media = lock(c, SITE, true); usable(media, true);
            require("PRIVATE".equals(media.get("visibility")) && "download-probe".equals(media.get("gated_form_key")), "MEDIA_GATE_MISMATCH");
            require(!rows(c, "SELECT id FROM mda_usage WHERE site_id=? AND media_id=? AND owner_type='CONTENT' AND owner_id=? AND locale='en' AND stage='PUBLISHED' LIMIT 1 FOR SHARE", SITE, MEDIA, CONTENT).isEmpty(), "MEDIA_PUBLISHED_REFERENCE_MISSING");
            return number(media.get("download_gate_version"));
        }
    }

    static final class Content {
        static Map<String, Object> parent(Connection c) throws SQLException {
            return one(c, "SELECT * FROM cnt_content WHERE id=? AND site_id=? FOR UPDATE", CONTENT, SITE);
        }
        static Map<String, Object> target(Connection c, Integer expected) throws SQLException {
            require(expected != null, "CONTENT_VERSION_REQUIRED"); parent(c);
            var target = one(c, "SELECT * FROM cnt_localization WHERE id=? AND site_id=? FOR UPDATE", TARGET, SITE);
            require(number(target.get("version")) == expected, "CONTENT_VERSION_CONFLICT");
            return target;
        }
        static void policy(Connection c, int expectedContentVersion, boolean required) throws SQLException {
            var parent = parent(c);
            require(number(parent.get("version")) == expectedContentVersion, "CONTENT_VERSION_CONFLICT");
            rows(c, "SELECT id FROM cnt_localization WHERE content_id=? AND site_id=? ORDER BY id FOR UPDATE", CONTENT, SITE);
            if (number(parent.get("translation_review_required")) == (required ? 1 : 0)) return;
            Pages.template(c, required ? "legal" : "default");
            update(c, "UPDATE cnt_content SET translation_review_required=?,version=version+1 WHERE id=? AND site_id=?", required, CONTENT, SITE);
            update(c, "UPDATE cnt_localization SET version=version+1,draft_state=IF(draft_state IN ('IN_REVIEW','APPROVED'),'EDITING',draft_state) WHERE content_id=? AND site_id=?", CONTENT, SITE);
            update(c, "UPDATE cnt_review SET status='CANCELED' WHERE site_id=? AND localization_id IN (?,?) AND status='PENDING'", SITE, SOURCE, TARGET);
        }
        static int readTargetVersion(Connection c) throws SQLException { return (int) scalar(c, "SELECT version FROM cnt_localization WHERE id=? AND site_id=?", TARGET, SITE); }
        static long currentSourceRevision(Connection c) throws SQLException { return scalar(c, "SELECT published_revision_no FROM cnt_localization WHERE id=? AND site_id=? FOR UPDATE", SOURCE, SITE); }
        static void ownerLast(Connection c, int expected) throws SQLException {
            require(update(c, "UPDATE cnt_localization SET draft_title='translated-title',draft_hash=?,version=version+1,translation_state='TRANSLATED',source_revision_no=1 WHERE id=? AND site_id=? AND version=?", hash("translated-layout|translated-seo"), TARGET, SITE, expected) == 1, "CONTENT_VERSION_CONFLICT");
        }
        static boolean completionAllowed(Connection c, int eventVersion, int sourceRevision, boolean providerReview) throws SQLException {
            var parent = parent(c);
            var target = one(c, "SELECT * FROM cnt_localization WHERE id=? AND site_id=? FOR UPDATE", TARGET, SITE);
            long revision = scalar(c, "SELECT published_revision_no FROM cnt_localization WHERE id=? AND site_id=? FOR UPDATE", SOURCE, SITE);
            return number(target.get("version")) == eventVersion && revision == sourceRevision
                    && "AUTO".equals(target.get("translation_mode")) && "TRANSLATED".equals(target.get("translation_state"))
                    && number(parent.get("translation_review_required")) == 0 && !providerReview;
        }
    }
    static final class Pages {
        static void template(Connection c, String key) throws SQLException { update(c, "UPDATE pg_page SET template_key=?,version=version+1 WHERE content_id=? AND site_id=?", key, CONTENT, SITE); }
        static void apply(Connection c) throws SQLException { update(c, "UPDATE pg_layout SET document=JSON_OBJECT('body','translated-layout'),version=version+1 WHERE content_id=? AND site_id=? AND locale='en'", CONTENT, SITE); }
    }
    static final class Seo {
        static void apply(Connection c) throws SQLException { update(c, "UPDATE seo_meta SET title_override='translated-seo',version=version+1 WHERE localization_id=? AND site_id=?", TARGET, SITE); }
    }
    static final class Translation {
        static void capture(Connection c) throws SQLException {
            var item = one(c, "SELECT * FROM trn_job_item WHERE id=? AND site_id=? FOR UPDATE", ITEM, SITE);
            if (item.get("expected_target_version") != null) return; // recovery keeps the old output's version
            require("PENDING".equals(item.get("status")), "TRANSLATION_CAPTURE_STATE_INVALID");
            int version = Content.readTargetVersion(c);
            update(c, "UPDATE trn_job_item SET expected_target_version=?,status='RUNNING',worker_id='worker-a' WHERE id=? AND site_id=?", version, ITEM, SITE);
        }
        static boolean apply(Connection c, String worker, boolean failAfterContributors) throws SQLException {
            var item = one(c, "SELECT * FROM trn_job_item WHERE id=? AND site_id=? FOR UPDATE", ITEM, SITE);
            require(worker.equals(item.get("worker_id")) && "RUNNING".equals(item.get("status")), "TRANSLATION_LEASE_LOST");
            Integer expected = (Integer) item.get("expected_target_version");
            Map<String, Object> target;
            try { target = Content.target(c, expected); }
            catch (Rejected e) {
                if (!e.getMessage().equals("CONTENT_VERSION_CONFLICT")) throw e;
                update(c, "UPDATE trn_job_item SET status='SUPERSEDED' WHERE id=? AND site_id=?", ITEM, SITE);
                return false;
            }
            if (!"AUTO".equals(target.get("translation_mode")) || Content.currentSourceRevision(c) != number(item.get("source_revision_no"))) {
                update(c, "UPDATE trn_job_item SET status='SUPERSEDED' WHERE id=? AND site_id=?", ITEM, SITE); return false;
            }
            Seo.apply(c); Pages.apply(c);
            require(!failAfterContributors, "INJECTED_PROVIDER_FAILURE");
            Content.ownerLast(c, expected);
            update(c, "UPDATE trn_job_item SET status='SUCCEEDED' WHERE id=? AND site_id=?", ITEM, SITE);
            return true;
        }
    }

    record Grant(String url, Instant expiresAt) {}
    enum Failure { NONE, SIGN, TEMPLATE, ENQUEUE, BEFORE_COMMIT }
    static final class Signer {
        final AtomicInteger calls = new AtomicInteger();
        Grant sign(long id, long gate, Instant expires, boolean fail) {
            calls.incrementAndGet(); require(!fail, "INJECTED_SIGN_FAILURE");
            // Public fixture key only. Production uses configured keyId/secret storage.
            String claims = SITE + ":" + id + ":" + MEDIA + ":" + CONTENT + ":en:download-probe:" + gate + ":" + expires;
            try {
                var mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec("public-test-fixture-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                return new Grant("/downloads/probe?claims=" + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + "&sig=" + HexFormat.of().formatHex(mac.doFinal(claims.getBytes(StandardCharsets.UTF_8))), expires);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
    }
    static final class Notifications {
        static void enqueue(Connection c, long id, Grant grant, Failure fault) throws SQLException {
            require(fault != Failure.TEMPLATE && !rows(c, "SELECT id FROM ntf_template WHERE site_id=? AND template_key='form.download.customer' AND locale='en' AND enabled=1 FOR SHARE", SITE).isEmpty(), "NOTIFICATION_TEMPLATE_NOT_FOUND");
            // A genuine MySQL CHECK violation injects an outbox-write failure.
            update(c, "INSERT INTO ntf_message(id,site_id,channel,recipient,template_key,locale,payload,source_module,source_id,dedupe_key) VALUES(?,?,'EMAIL',?,'form.download.customer','en',JSON_OBJECT('downloadUrl',?,'expiresAt',?),'form',?,?)", id, SITE, fault == Failure.ENQUEUE ? null : "probe@example.invalid", grant.url(), grant.expiresAt().toString(), id, "download:" + id + ":" + MEDIA);
        }
    }
    static final class Forms {
        static Grant replay(Connection c, String key) throws SQLException {
            var found = rows(c, "SELECT JSON_UNQUOTE(JSON_EXTRACT(download_grant,'$.url')) AS url,JSON_UNQUOTE(JSON_EXTRACT(download_grant,'$.expiresAt')) AS expires_at FROM frm_submission WHERE site_id=? AND idempotency_key=?", SITE, key);
            if (found.isEmpty()) return null;
            var row = found.getFirst();
            return row.get("url") == null ? null : new Grant((String) row.get("url"), Instant.parse((String) row.get("expires_at")));
        }
        static Grant write(Connection c, long id, String key, String verdict, boolean test, Instant now, Failure fault, Signer signer) throws SQLException {
            update(c, "INSERT INTO frm_submission(id,site_id,form_id,locale,payload,content_id,download_media_id,spam_verdict,is_test,idempotency_key) VALUES(?,?,?,'en',JSON_OBJECT('email','probe@example.invalid'),?,?,?,?,?)", id, SITE, FORM, CONTENT, MEDIA, verdict, test, key);
            if (!"ACCEPT".equals(verdict) || test) return null;
            long gate = Media.gateForDownload(c);
            Grant grant = signer.sign(id, gate, now.plusSeconds(600), fault == Failure.SIGN);
            update(c, "UPDATE frm_submission SET download_grant=JSON_OBJECT('url',?,'expiresAt',?,'contentId',?,'locale','en') WHERE id=? AND site_id=?", grant.url(), grant.expiresAt().toString(), CONTENT, id, SITE);
            Notifications.enqueue(c, id, grant, fault);
            return grant;
        }
        static Grant submit(long id, String key, String verdict, boolean test, Instant now, Failure fault, Signer signer, List<Grant> exposed) throws Exception {
            Grant result;
            try (var c = connect()) {
                // Request fingerprint comparison belongs to production FormApi, outside this minimal model.
                if (scalar(c, "SELECT COUNT(*) FROM frm_submission WHERE site_id=? AND idempotency_key=?", SITE, key) > 0) result = replay(c, key);
                else result = write(c, id, key, verdict, test, now, fault, signer);
                require(fault != Failure.BEFORE_COMMIT, "INJECTED_COMMIT_BOUNDARY_FAILURE");
                c.commit();
            }
            if (result != null) exposed.add(result); // only reachable after successful commit
            return result;
        }
    }
}
