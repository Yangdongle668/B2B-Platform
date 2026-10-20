package com.b2bplatform.compat.protocol;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.json.JsonMapper;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;

/** Isolated page-route protocol model; adapters own their SQL prefixes.
 * No real ContentTypeProvider, event dispatcher, scheduler or URL-pattern engine is implemented here.
 */
final class RouteProtocolModel {
    static final long ROUTE_SITE = 801, ROOT = 930, CHILD = 910, GRANDCHILD = 900, SIBLING = 920, OTHER = 940;
    static final String MUTEX = "content.route_tree";
    static final AtomicLong IDS = new AtomicLong(200_000);
    static final JsonMapper JSON = JsonMapper.builder().build();
    static Connection routeConnection() throws SQLException {
        var c = connect(); c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); return c;
    }
    static String json(Object value) { return JSON.writeValueAsString(value); }
    record Key(String locale, long contentId) {}
    record Node(long contentId, long localizationId, String locale, String slug, Long parent) {
        Key key() { return new Key(locale, contentId); }
    }
    record Lease(String worker, int attempt) {}
    record Task(long id, long generation, Set<String> locales, Key cursor, int done, String status, boolean cancel) {}
    record Batch(String status, int processed, int changed, Long replacement) {}

    static final class Mutex {
        static long lock(Connection c, long site, String name) throws SQLException {
            require(!c.getAutoCommit(), "CORE_MUTEX_TRANSACTION_REQUIRED");
            require(MUTEX.equals(name), "CORE_MUTEX_NAME_INVALID");
            // X-lock even on duplicate: avoids an INSERT IGNORE shared-lock -> X-lock upgrade race.
            update(c, "INSERT INTO sys_transaction_mutex(id,site_id,name) VALUES(?,?,?) ON DUPLICATE KEY UPDATE id=id", IDS.incrementAndGet(), site, name);
            return scalar(c, "SELECT generation FROM sys_transaction_mutex WHERE site_id=? AND name=? FOR UPDATE", site, name);
        }
        static long advance(Connection c) throws SQLException {
            lock(c, ROUTE_SITE, MUTEX);
            update(c, "UPDATE sys_transaction_mutex SET generation=generation+1 WHERE site_id=? AND name=?", ROUTE_SITE, MUTEX);
            return scalar(c, "SELECT generation FROM sys_transaction_mutex WHERE site_id=? AND name=? FOR UPDATE", ROUTE_SITE, MUTEX);
        }
    }
    static final class PageTree {
        static Map<Long, Long> hints(Connection c) throws SQLException {
            var result = new HashMap<Long, Long>();
            for (var r : rows(c, "SELECT content_id,parent_content_id FROM pg_page WHERE site_id=?", ROUTE_SITE))
                result.put(number(r.get("content_id")), r.get("parent_content_id") == null ? null : number(r.get("parent_content_id")));
            return result;
        }
        static void move(Connection c, long id, long parent) throws SQLException {
            var hints = hints(c); require(hints.containsKey(id) && hints.containsKey(parent), "CONTENT_ROUTE_PARENT_MISSING");
            hints.put(id, parent);
            var pending = new TreeSet<>(hints.keySet()); var paths = new HashMap<Long, String>(); var depths = new HashMap<Long, Integer>();
            while (!pending.isEmpty()) {
                var ready = pending.stream().filter(n -> hints.get(n) == null || paths.containsKey(hints.get(n))).toList();
                require(!ready.isEmpty(), "CONTENT_ROUTE_CYCLE");
                long node = ready.getFirst(); Long p = hints.get(node);
                String path = (p == null ? "/" : paths.get(p)) + node + "/";
                int depth = p == null ? 1 : depths.get(p) + 1;
                update(c, "UPDATE pg_page SET parent_content_id=?,tree_path=?,depth=?,version=version+1 WHERE site_id=? AND content_id=?", p, path, depth, ROUTE_SITE, node);
                paths.put(node, path); depths.put(node, depth); pending.remove(node);
            }
        }
    }
    static final class RouteTasks {
        static Set<String> canonical(Set<String> locales) {
            require(!locales.isEmpty() && Set.of("en", "de").containsAll(locales), "CONTENT_ROUTE_LOCALE_INVALID");
            return new TreeSet<>(locales);
        }
        static long request(Connection c, Set<String> locales) throws SQLException {
            var scope = canonical(locales); long generation = Mutex.lock(c, ROUTE_SITE, MUTEX);
            String key = "routes:" + generation + ":" + hash(String.join(",", scope)).substring(0, 20);
            var existing = rows(c, "SELECT id FROM sys_job WHERE site_id=? AND job_type='content.recalculate_routes' AND idempotency_key=? FOR UPDATE", ROUTE_SITE, key);
            if (!existing.isEmpty()) return number(existing.getFirst().get("id"));
            long id = IDS.incrementAndGet();
            update(c, "INSERT INTO sys_job(id,site_id,module_id,job_type,title,idempotency_key,params,status,worker_id,attempts,max_attempts) VALUES(?,?,'content','content.recalculate_routes','验证路由重算',?,JSON_OBJECT('generation',?,'locales',CAST(? AS JSON)),'RUNNING','worker-a',1,6)", id, ROUTE_SITE, key, generation, json(scope));
            return id;
        }
        static Task lock(Connection c, long id, Lease lease) throws SQLException {
            var row = one(c, "SELECT *,JSON_UNQUOTE(JSON_EXTRACT(params,'$.generation')) AS route_generation,JSON_EXTRACT(params,'$.locales') AS locales,JSON_UNQUOTE(JSON_EXTRACT(params,'$.cursorLocale')) AS cursor_locale,JSON_UNQUOTE(JSON_EXTRACT(params,'$.cursorContentId')) AS cursor_content FROM sys_job WHERE site_id=? AND id=? FOR UPDATE", ROUTE_SITE, id);
            require(lease.worker().equals(row.get("worker_id")) && number(row.get("attempts")) == lease.attempt(), "CORE_JOB_LEASE_LOST");
            var locales = new TreeSet<>(List.of(JSON.readValue((String) row.get("locales"), String[].class)));
            Object cursor = row.get("cursor_content");
            Key key = cursor == null || cursor.equals("null") ? null : new Key((String) row.get("cursor_locale"), Long.parseLong((String) cursor));
            return new Task(id, Long.parseLong((String) row.get("route_generation")), locales, key, (int) number(row.get("progress_done")), (String) row.get("status"), number(row.get("cancel_requested")) == 1);
        }
        static void checkpoint(Connection c, Task task, Key cursor, int done, boolean finished) throws SQLException {
            update(c, "UPDATE sys_job SET params=JSON_SET(params,'$.cursorLocale',?,'$.cursorContentId',?),progress_done=?,status=?,version=version+1 WHERE id=? AND site_id=?", cursor.locale(), cursor.contentId(), done, finished ? "SUCCEEDED" : "RUNNING", task.id(), ROUTE_SITE);
        }
        static void cancel(Connection c, long id, String reason) throws SQLException {
            update(c, "UPDATE sys_job SET status='CANCELED',error_code=?,version=version+1 WHERE id=? AND site_id=?", reason, id, ROUTE_SITE);
        }
    }
    static final class Urls {
        static String path(Connection c, long localizationId) throws SQLException {
            return (String) one(c, "SELECT path FROM url_route WHERE site_id=? AND localization_id=? AND state='ACTIVE'", ROUTE_SITE, localizationId).get("path");
        }
        static boolean apply(Connection c, Node node, String next) throws SQLException {
            var route = one(c, "SELECT path FROM url_route WHERE site_id=? AND localization_id=? FOR UPDATE", ROUTE_SITE, node.localizationId());
            String old = (String) route.get("path"); if (old.equals(next)) return false;
            require(rows(c, "SELECT id FROM url_route WHERE site_id=? AND path=? AND localization_id<>? FOR UPDATE", ROUTE_SITE, next, node.localizationId()).isEmpty(), "URL_PATH_CONFLICT");
            var redirects = rows(c, "SELECT origin,target_localization_id FROM url_redirect WHERE site_id=? AND source_path=? AND match_type='EXACT' FOR UPDATE", ROUTE_SITE, old);
            if (redirects.isEmpty()) update(c, "INSERT INTO url_redirect(id,site_id,source_path,target_kind,target_localization_id,origin) VALUES(?,?,?,'LOCALIZATION',?,'AUTO')", IDS.incrementAndGet(), ROUTE_SITE, old, node.localizationId());
            else require("AUTO".equals(redirects.getFirst().get("origin")) && number(redirects.getFirst().get("target_localization_id")) == node.localizationId(), "URL_REDIRECT_CONFLICT");
            update(c, "UPDATE url_route SET path=? WHERE site_id=? AND localization_id=?", next, ROUTE_SITE, node.localizationId());
            return true;
        }
    }
    static final class SearchCapture {
        static void append(Connection c, Map<String, List<Long>> changed) throws SQLException {
            // All source/route/job writes finish first; no source locks after this sorted capture.
            for (String locale : new TreeSet<>(changed.keySet())) {
                var ids = changed.get(locale); if (ids.isEmpty()) continue;
                long seq = scalar(c, "SELECT change_seq FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE", ROUTE_SITE, locale) + 1;
                update(c, "UPDATE sch_index_state SET change_seq=? WHERE site_id=? AND locale=?", seq, ROUTE_SITE, locale);
                update(c, "INSERT INTO sch_change(id,site_id,locale,sequence_no,localization_ids) VALUES(?,?,?,?,CAST(? AS JSON))", IDS.incrementAndGet(), ROUTE_SITE, locale, seq, json(ids));
            }
        }
    }
    static final class ContentRoutes {
        static void lockSource(Connection c) throws SQLException {
            rows(c, "SELECT id FROM cnt_content WHERE site_id=? ORDER BY id FOR UPDATE", ROUTE_SITE);
            rows(c, "SELECT id FROM cnt_localization WHERE site_id=? ORDER BY content_id,id FOR UPDATE", ROUTE_SITE);
        }
        static long rename(Connection c, long id, String slug) throws SQLException {
            require(c.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED, "CONTENT_ROUTE_ISOLATION_REQUIRED");
            long generation = Mutex.lock(c, ROUTE_SITE, MUTEX); lockSource(c);
            String old = (String) one(c, "SELECT slug FROM cnt_localization WHERE site_id=? AND content_id=? AND locale='en' FOR UPDATE", ROUTE_SITE, id).get("slug");
            if (old.equals(slug)) return generation;
            update(c, "UPDATE cnt_localization SET slug=?,version=version+1 WHERE site_id=? AND content_id=? AND locale='en'", slug, ROUTE_SITE, id);
            generation = Mutex.advance(c); RouteTasks.request(c, Set.of("en")); return generation;
        }
        static long move(Connection c, long id, long parent) throws SQLException {
            require(c.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED, "CONTENT_ROUTE_ISOLATION_REQUIRED");
            Mutex.lock(c, ROUTE_SITE, MUTEX); lockSource(c); PageTree.move(c, id, parent);
            long generation = Mutex.advance(c); RouteTasks.request(c, Set.of("de", "en")); return generation;
        }
        static List<Node> ordered(Connection c, Set<String> locales) throws SQLException {
            var hints = PageTree.hints(c); var all = new ArrayList<Node>();
            for (String locale : new TreeSet<>(locales)) {
                var pending = new TreeMap<Long, Node>();
                for (var r : rows(c, "SELECT l.id,l.content_id,l.slug FROM cnt_content c JOIN cnt_localization l ON c.id=l.content_id AND c.site_id=l.site_id WHERE c.site_id=? AND c.type='PAGE' AND c.routable=1 AND l.locale=? AND l.publish_state='PUBLISHED'", ROUTE_SITE, locale)) {
                    long id = number(r.get("content_id")); require(hints.containsKey(id), "CONTENT_ROUTE_HINTS_MISSING");
                    pending.put(id, new Node(id, number(r.get("id")), locale, (String) r.get("slug"), hints.get(id)));
                }
                var done = new HashSet<Long>();
                while (!pending.isEmpty()) {
                    var ready = pending.values().stream().filter(n -> n.parent() == null || done.contains(n.parent())).toList();
                    require(!ready.isEmpty(), "CONTENT_ROUTE_CYCLE_OR_PARENT_MISSING");
                    Node node = ready.getFirst(); all.add(node); done.add(node.contentId()); pending.remove(node.contentId());
                }
            }
            return all;
        }
        static Batch batch(Connection c, long jobId, Lease lease, int size, boolean failAfterWrites) throws SQLException {
            require(c.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED, "CONTENT_ROUTE_ISOLATION_REQUIRED");
            require(size >= 1 && size <= 100, "CONTENT_ROUTE_BATCH_INVALID");
            long generation = Mutex.lock(c, ROUTE_SITE, MUTEX);
            Task task = RouteTasks.lock(c, jobId, lease); // mutex before job/source locks
            if (task.cancel()) { RouteTasks.cancel(c, jobId, "CORE_JOB_CANCEL_REQUESTED"); return new Batch("CANCELED", 0, 0, null); }
            if (task.status().equals("SUCCEEDED") || task.status().equals("CANCELED")) return new Batch(task.status(), 0, 0, null);
            require(task.status().equals("RUNNING") || task.status().equals("PENDING"), "CORE_JOB_STATE_INVALID");
            if (task.generation() != generation) {
                RouteTasks.cancel(c, jobId, "CONTENT_ROUTE_SUPERSEDED");
                return new Batch("SUPERSEDED", 0, 0, RouteTasks.request(c, task.locales()));
            }
            lockSource(c); var ordered = ordered(c, task.locales()); int start = 0;
            if (task.cursor() != null) {
                var keys = ordered.stream().map(Node::key).toList(); int position = keys.indexOf(task.cursor());
                require(position >= 0 && position + 1 == task.done(), "CONTENT_ROUTE_CURSOR_INVALID"); start = position + 1;
            }
            int end = Math.min(start + size, ordered.size()), changedCount = 0;
            var changes = new TreeMap<String, List<Long>>();
            for (int i = start; i < end; i++) {
                Node node = ordered.get(i);
                String base = "/" + node.locale();
                if (node.parent() != null) {
                    long parentLoc = scalar(c, "SELECT id FROM cnt_localization WHERE site_id=? AND content_id=? AND locale=?", ROUTE_SITE, node.parent(), node.locale());
                    base = Urls.path(c, parentLoc);
                }
                String next = base + "/" + node.slug();
                if (Urls.apply(c, node, next)) {
                    update(c, "UPDATE cnt_localization SET pub_path=? WHERE site_id=? AND id=?", next, ROUTE_SITE, node.localizationId());
                    changes.computeIfAbsent(node.locale(), ignored -> new ArrayList<>()).add(node.localizationId()); changedCount++;
                }
            }
            if (end > start) RouteTasks.checkpoint(c, task, ordered.get(end - 1).key(), end, end == ordered.size());
            else update(c, "UPDATE sys_job SET status='SUCCEEDED' WHERE site_id=? AND id=?", ROUTE_SITE, jobId);
            SearchCapture.append(c, changes);
            require(!failAfterWrites, "INJECTED_ROUTE_BATCH_FAILURE");
            return new Batch(end == ordered.size() ? "SUCCEEDED" : "RUNNING", end - start, changedCount, null);
        }
    }
}
