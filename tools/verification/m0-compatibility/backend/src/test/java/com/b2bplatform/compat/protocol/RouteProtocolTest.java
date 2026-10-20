package com.b2bplatform.compat.protocol;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.*;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static com.b2bplatform.compat.protocol.RouteProtocolModel.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class RouteProtocolTest extends ProtocolFixture {
    static final Lease FIRST = new Lease("worker-a", 1);
    @BeforeEach void seedRouteTree() throws Exception {
        try (var c = routeConnection()) {
            for (String table : new String[]{"sch_change", "sch_index_state", "url_redirect", "url_route", "cnt_revision", "cnt_localization"})
                update(c, "DELETE FROM " + table + " WHERE site_id=?", ROUTE_SITE);
            update(c, "DELETE FROM pg_page WHERE site_id=? ORDER BY depth DESC", ROUTE_SITE);
            for (String table : new String[]{"cnt_content", "sys_job", "sys_transaction_mutex"})
                update(c, "DELETE FROM " + table + " WHERE site_id IN (?,?)", ROUTE_SITE, ROUTE_SITE + 1);
            long[][] tree = {{ROOT,0,1},{OTHER,0,1},{CHILD,ROOT,2},{GRANDCHILD,CHILD,3},{SIBLING,ROOT,2}};
            Map<Long, String> slugs = Map.of(ROOT,"root",OTHER,"other",CHILD,"child",GRANDCHILD,"grand",SIBLING,"sibling");
            Map<Long, String> paths = new HashMap<>(), treePaths = new HashMap<>();
            for (long[] entry : tree) {
                long id=entry[0], parent=entry[1];
                String path = (parent==0 ? "" : paths.get(parent)) + "/" + slugs.get(id); paths.put(id,path);
                String treePath = (parent==0 ? "/" : treePaths.get(parent)) + id + "/"; treePaths.put(id,treePath);
                update(c, "INSERT INTO cnt_content(id,site_id,type,owner_module,routable) VALUES(?,?,'PAGE','page',1)", id, ROUTE_SITE);
                update(c, "INSERT INTO pg_page(content_id,site_id,parent_content_id,tree_path,depth) VALUES(?,?,?,?,?)", id, ROUTE_SITE, parent==0 ? null : parent, treePath, entry[2]);
                for (String locale : List.of("en","de")) {
                    long loc=loc(id,locale), revision=loc+10_000_000;
                    update(c, "INSERT INTO cnt_localization(id,site_id,content_id,content_type,locale,slug,draft_title,translation_mode,source_locale,source_revision_no,publish_state,published_revision_id,published_revision_no,pub_path) VALUES(?,?,?,'PAGE',?,?,'route-probe','MANUAL','zh-CN',1,'PUBLISHED',?,1,?)", loc,ROUTE_SITE,id,locale,slugs.get(id),revision,"/"+locale+path);
                    update(c, "INSERT INTO cnt_revision(id,site_id,localization_id,revision_no,payload,payload_hash,published_at) VALUES(?,?,?,1,JSON_OBJECT('body','route-probe'),?,NOW(3))",revision,ROUTE_SITE,loc,hash("route-probe"));
                    update(c, "INSERT INTO url_route(id,site_id,locale,path,content_id,localization_id,content_type) VALUES(?,?,?,?,?,?,'PAGE')",loc,ROUTE_SITE,locale,"/"+locale+path,id,loc);
                }
            }
            for (String locale : List.of("de","en")) update(c, "INSERT INTO sch_index_state(id,site_id,locale) VALUES(?,?,?)", IDS.incrementAndGet(),ROUTE_SITE,locale);
            c.commit();
        }
    }
    static long loc(long content, String locale) { return content*10 + (locale.equals("en") ? 1 : 2); }
    static void routeCommitted(Work work) throws Exception {
        try (var c=routeConnection()) { try { work.run(c);c.commit(); } catch(Exception e) { c.rollback();throw e; } }
    }
    static long request(Set<String> locales) throws Exception {
        try (var c=routeConnection()) { long id=RouteTasks.request(c,locales);c.commit();return id; }
    }
    static long renamed(String slug) throws Exception { routeCommitted(c -> ContentRoutes.rename(c,ROOT,slug));return request(Set.of("en")); }
    static Batch run(long job, Lease lease, int size) throws Exception {
        try (var c=routeConnection()) { try { Batch b=ContentRoutes.batch(c,job,lease,size,false);c.commit();return b; } catch(Exception e) {c.rollback();throw e;} }
    }
    static String path(long id) throws Exception { return (String)field("SELECT path FROM url_route WHERE site_id=? AND localization_id=?",ROUTE_SITE,loc(id,"en")); }
    static long done(long job) throws Exception {return value("SELECT progress_done FROM sys_job WHERE site_id=? AND id=?",ROUTE_SITE,job);}
    static long redirects() throws Exception {return value("SELECT COUNT(*) FROM url_redirect WHERE site_id=?",ROUTE_SITE);}
    static long sequence() throws Exception {return value("SELECT change_seq FROM sch_index_state WHERE site_id=? AND locale='en'",ROUTE_SITE);}

    @Test void concurrentFirstCreationUsesOneExclusiveMutexRow() throws Exception {
        try(var owner=routeConnection()) {
            assertThat(Mutex.lock(owner,ROUTE_SITE,MUTEX)).isZero();
            try(var waiter=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> assertThat(Mutex.lock(c,ROUTE_SITE,MUTEX)).isZero())) {
                waiter.awaitDatabaseLockWait();owner.commit();waiter.succeeds();
            }
        }
        assertThat(value("SELECT COUNT(*) FROM sys_transaction_mutex WHERE site_id=? AND name=?",ROUTE_SITE,MUTEX)).isEqualTo(1);
    }
    @Test void insertIgnoreThenLockHasARealSharedLockUpgradeDeadlock() throws Exception {
        routeCommitted(c -> Mutex.lock(c,ROUTE_SITE,MUTEX));
        SQLException leftFailure=null, rightFailure=null;
        try(var left=routeConnection()) {
            update(left,"INSERT IGNORE INTO sys_transaction_mutex(id,site_id,name) VALUES(?,?,?)",IDS.incrementAndGet(),ROUTE_SITE,MUTEX);
            try(var right=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> {
                update(c,"INSERT IGNORE INTO sys_transaction_mutex(id,site_id,name) VALUES(?,?,?)",IDS.incrementAndGet(),ROUTE_SITE,MUTEX);
                scalar(c,"SELECT generation FROM sys_transaction_mutex WHERE site_id=? AND name=? FOR UPDATE",ROUTE_SITE,MUTEX);
            })) {
                right.awaitDatabaseLockWait();
                try {scalar(left,"SELECT generation FROM sys_transaction_mutex WHERE site_id=? AND name=? FOR UPDATE",ROUTE_SITE,MUTEX);left.commit();}
                catch(SQLException e) {leftFailure=e;left.rollback();}
                try {right.succeeds();}
                catch(ExecutionException e) {
                    Throwable root=e;while(root.getCause()!=null)root=root.getCause();
                    assertThat(root).isInstanceOf(SQLException.class);rightFailure=(SQLException)root;
                }
            }
        }
        assertThat((leftFailure==null) ^ (rightFailure==null)).isTrue();
        assertThat((leftFailure!=null ? leftFailure : rightFailure).getErrorCode()).isEqualTo(1213);
    }
    @Test void waiterReadsCurrentSourceAfterMutexInsteadOfItsEarlierRead() throws Exception {
        try(var editing=routeConnection()) {
            ContentRoutes.rename(editing,ROOT,"latest");long job=RouteTasks.request(editing,Set.of("en"));
            try(var waiting=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> {
                assertThat(one(c,"SELECT slug FROM cnt_localization WHERE site_id=? AND id=?",ROUTE_SITE,loc(ROOT,"en")).get("slug")).isEqualTo("root");
                assertThat(ContentRoutes.batch(c,job,FIRST,100,false).status()).isEqualTo("SUCCEEDED");
            })) {waiting.awaitDatabaseLockWait();editing.commit();waiting.succeeds();}
        }
        assertThat(path(GRANDCHILD)).isEqualTo("/en/latest/child/grand");
    }
    @Test void generationChangeDiscardsOldCursorAndRebuildsFromCurrentParentTree() throws Exception {
        long old=renamed("first");run(old,FIRST,2);assertThat(done(old)).isEqualTo(2);
        routeCommitted(c -> ContentRoutes.move(c,CHILD,OTHER));
        Batch stale=run(old,FIRST,2);assertThat(stale.status()).isEqualTo("SUPERSEDED");assertThat(stale.processed()).isZero();
        assertThat(done(old)).isEqualTo(2);
        run(stale.replacement(),FIRST,100);
        assertThat(path(CHILD)).isEqualTo("/en/other/child");assertThat(path(GRANDCHILD)).isEqualTo("/en/other/child/grand");
        long before=redirects();assertThat(run(old,FIRST,100).status()).isEqualTo("CANCELED");assertThat(redirects()).isEqualTo(before);
    }
    @Test void topologicalCheckpointCanResumeAtChildrenWithSmallerIds() throws Exception {
        long job=renamed("new");run(job,FIRST,1);
        assertThat(value("SELECT JSON_EXTRACT(params,'$.cursorContentId')+0 FROM sys_job WHERE id=?",job)).isEqualTo(ROOT);
        run(job,FIRST,1);assertThat(value("SELECT JSON_EXTRACT(params,'$.cursorContentId')+0 FROM sys_job WHERE id=?",job)).isEqualTo(CHILD);
        run(job,FIRST,1);assertThat(value("SELECT JSON_EXTRACT(params,'$.cursorContentId')+0 FROM sys_job WHERE id=?",job)).isEqualTo(GRANDCHILD);
        assertThat(run(job,FIRST,100).status()).isEqualTo("SUCCEEDED");assertThat(done(job)).isEqualTo(5);
        assertThat(path(GRANDCHILD)).isEqualTo("/en/new/child/grand");
    }
    @Test void failingBatchRollsBackRoutesRedirectsProjectionCheckpointAndSearchLogTogether() throws Exception {
        long job=renamed("new");
        assertThatThrownBy(() -> routeCommitted(c -> ContentRoutes.batch(c,job,FIRST,2,true))).isInstanceOf(Rejected.class).hasMessage("INJECTED_ROUTE_BATCH_FAILURE");
        assertThat(path(ROOT)).isEqualTo("/en/root");assertThat(redirects()).isZero();assertThat(done(job)).isZero();assertThat(sequence()).isZero();
        assertThat(field("SELECT pub_path FROM cnt_localization WHERE id=?",loc(ROOT,"en"))).isEqualTo("/en/root");
        assertThat(value("SELECT COUNT(*) FROM sch_change WHERE site_id=?",ROUTE_SITE)).isZero();
        run(job,FIRST,2);assertThat(done(job)).isEqualTo(2);assertThat(sequence()).isEqualTo(1);
        assertThat(value("SELECT JSON_LENGTH(localization_ids) FROM sch_change WHERE site_id=? AND locale='en'",ROUTE_SITE)).isEqualTo(2);
    }
    @Test void secondItemConflictRollsBackFirstItemAndRetryUsesSameCheckpoint() throws Exception {
        long job=renamed("new");
        routeCommitted(c -> update(c,"INSERT INTO url_route(id,site_id,locale,path,content_id,localization_id,content_type) VALUES(99999,?,'en','/en/new/child',9999,99999,'PRODUCT')",ROUTE_SITE));
        assertThatThrownBy(() -> run(job,FIRST,2)).isInstanceOf(Rejected.class).hasMessage("URL_PATH_CONFLICT");
        assertThat(path(ROOT)).isEqualTo("/en/root");assertThat(done(job)).isZero();assertThat(sequence()).isZero();assertThat(redirects()).isZero();
        assertThat(value("SELECT COUNT(*) FROM url_route WHERE id=99999")).isEqualTo(1);
        routeCommitted(c -> update(c,"UPDATE sys_job SET status='FAILED',error_code='URL_PATH_CONFLICT' WHERE id=?",job));
        assertThatThrownBy(() -> run(job,FIRST,100)).isInstanceOf(Rejected.class).hasMessage("CORE_JOB_STATE_INVALID");
        routeCommitted(c -> {update(c,"DELETE FROM url_route WHERE id=99999");update(c,"UPDATE sys_job SET status='RUNNING',worker_id='worker-b',attempts=2 WHERE id=?",job);});
        assertThat(run(job,new Lease("worker-b",2),100).status()).isEqualTo("SUCCEEDED");
        assertThat(path(GRANDCHILD)).isEqualTo("/en/new/child/grand");assertThat(done(job)).isEqualTo(5);
    }
    @Test void committedBatchWithLostResponseResumesWithoutDuplicateRedirectsOrSearchCapture() throws Exception {
        long job=renamed("new");run(job,FIRST,2); // discard the response; persisted checkpoint is authoritative
        run(job,FIRST,100);assertThat(done(job)).isEqualTo(5);assertThat(redirects()).isEqualTo(4);assertThat(sequence()).isEqualTo(2);
        run(job,FIRST,100);assertThat(redirects()).isEqualTo(4);assertThat(sequence()).isEqualTo(2);
    }
    @Test void repeatedPathChangesKeepOldRedirectsPointingDirectlyToCurrentLocalization() throws Exception {
        run(renamed("one"),FIRST,100);run(renamed("two"),FIRST,100);
        assertThat(path(ROOT)).isEqualTo("/en/two");
        assertThat(value("SELECT COUNT(*) FROM url_redirect WHERE site_id=? AND source_path IN ('/en/root','/en/one') AND target_kind='LOCALIZATION' AND target_localization_id=?",ROUTE_SITE,loc(ROOT,"en"))).isEqualTo(2);
    }
    @Test void cancellationPreservesCommittedPrefixAndNewGenerationRepairsFromCurrentFacts() throws Exception {
        long job=renamed("one");run(job,FIRST,2);
        routeCommitted(c -> update(c,"UPDATE sys_job SET cancel_requested=1 WHERE id=?",job));
        assertThat(run(job,FIRST,100).status()).isEqualTo("CANCELED");assertThat(done(job)).isEqualTo(2);
        assertThat(path(CHILD)).isEqualTo("/en/one/child");assertThat(path(GRANDCHILD)).isEqualTo("/en/root/child/grand");
        run(renamed("two"),FIRST,100);assertThat(path(GRANDCHILD)).isEqualTo("/en/two/child/grand");
    }
    @Test void workerReusingSameIdentityCannotCommitAnOldAttemptAfterWaiting() throws Exception {
        long job=renamed("new");
        try(var blocker=routeConnection()) {
            Mutex.lock(blocker,ROUTE_SITE,MUTEX);
            try(var old=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> ContentRoutes.batch(c,job,FIRST,100,false))) {
                old.awaitDatabaseLockWait();
                routeCommitted(c -> update(c,"UPDATE sys_job SET attempts=2,worker_id='worker-a' WHERE id=?",job));
                blocker.commit();old.rejected("CORE_JOB_LEASE_LOST");
            }
        }
        assertThat(done(job)).isZero();assertThat(redirects()).isZero();run(job,new Lease("worker-a",2),100);
        assertThat(path(ROOT)).isEqualTo("/en/new");
    }
    @Test void sameExistingMutexNameOnAnotherSiteDoesNotWaitForThisSite() throws Exception {
        // Steady-state isolation: initialize both unique-index records before comparing row locks.
        routeCommitted(c -> { Mutex.lock(c,ROUTE_SITE,MUTEX); Mutex.lock(c,ROUTE_SITE+1,MUTEX); });
        try(var owner=routeConnection()) {
            Mutex.lock(owner,ROUTE_SITE,MUTEX);
            try(var other=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> Mutex.lock(c,ROUTE_SITE+1,MUTEX))) {other.succeeds();}
            owner.commit();
        }
        assertThat(value("SELECT COUNT(*) FROM sys_transaction_mutex WHERE site_id IN (?,?)",ROUTE_SITE,ROUTE_SITE+1)).isEqualTo(2);
    }
    @Test void coldInitializationAcrossSitesMayWaitButNeverSharesGeneration() throws Exception {
        try(var owner=routeConnection()) {
            Mutex.advance(owner);
            try(var other=new Contender(Connection.TRANSACTION_READ_COMMITTED,c -> assertThat(Mutex.lock(c,ROUTE_SITE+1,MUTEX)).isZero())) {
                await().atMost(java.time.Duration.ofSeconds(5)).until(() -> {
                    if(other.future.isDone()) return true;
                    try(var observer=routeConnection()) {
                        return scalar(observer,"SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID WHERE t.PROCESSLIST_ID=?",other.mysqlConnectionId.get())>0;
                    }
                });
                try(var observer=routeConnection()) {
                    var locks=rows(observer,"SELECT r.index_name,r.lock_mode AS requesting_mode,r.lock_data AS requesting_key,b.lock_mode AS blocking_mode,b.lock_data AS blocking_key FROM performance_schema.data_lock_waits w JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID JOIN performance_schema.data_locks r ON r.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID JOIN performance_schema.data_locks b ON b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID WHERE t.PROCESSLIST_ID=?",other.mysqlConnectionId.get());
                    System.out.println("ROUTE_COLD_INIT_LOCKS="+json(locks));
                }
                owner.commit();other.succeeds();
            }
        }
        assertThat(value("SELECT generation FROM sys_transaction_mutex WHERE site_id=?",ROUTE_SITE)).isEqualTo(1);
        assertThat(value("SELECT generation FROM sys_transaction_mutex WHERE site_id=?",ROUTE_SITE+1)).isZero();
    }
    @Test void sourceRollbackAlsoRollsBackGenerationAndScheduledJob() throws Exception {
        try(var editing=routeConnection()) {ContentRoutes.rename(editing,ROOT,"uncommitted");editing.rollback();}
        assertThat(value("SELECT COUNT(*) FROM sys_job WHERE site_id=?",ROUTE_SITE)).isZero();
        assertThat(value("SELECT COUNT(*) FROM sys_transaction_mutex WHERE site_id=?",ROUTE_SITE)).isZero();
        assertThat(field("SELECT slug FROM cnt_localization WHERE id=?",loc(ROOT,"en"))).isEqualTo("root");
    }
    @Test void unchangedSourceDoesNotAdvanceGenerationOrScheduleWork() throws Exception {
        routeCommitted(c -> assertThat(ContentRoutes.rename(c,ROOT,"root")).isZero());
        assertThat(value("SELECT generation FROM sys_transaction_mutex WHERE site_id=?",ROUTE_SITE)).isZero();
        assertThat(value("SELECT COUNT(*) FROM sys_job WHERE site_id=?",ROUTE_SITE)).isZero();
    }
    @Test void repeatableReadRouteBatchIsRejectedBeforeAnyWrites() throws Exception {
        long job=renamed("new");
        assertThatThrownBy(() -> committed(c -> ContentRoutes.batch(c,job,FIRST,100,false))).isInstanceOf(Rejected.class).hasMessage("CONTENT_ROUTE_ISOLATION_REQUIRED");
        assertThat(done(job)).isZero();assertThat(path(ROOT)).isEqualTo("/en/root");
    }
    @Test void cyclicParentMoveRollsBackTreeAndGeneration() throws Exception {
        request(Set.of("en"));
        assertThatThrownBy(() -> routeCommitted(c -> ContentRoutes.move(c,ROOT,GRANDCHILD))).isInstanceOf(Rejected.class).hasMessage("CONTENT_ROUTE_CYCLE");
        assertThat(field("SELECT parent_content_id FROM pg_page WHERE content_id=?",ROOT)).isNull();
        assertThat(value("SELECT generation FROM sys_transaction_mutex WHERE site_id=?",ROUTE_SITE)).isZero();
    }
    @Test void canonicalLanguageScopeIsPartOfJobDeduplicationAndCheckpoint() throws Exception {
        long en=request(Set.of("en")),de=request(Set.of("de")),both=request(Set.of("en","de"));
        assertThat(en).isNotEqualTo(de).isNotEqualTo(both);
        assertThat(request(new LinkedHashSet<>(List.of("de","en")))).isEqualTo(both);
        run(both,FIRST,1);
        assertThat(field("SELECT JSON_UNQUOTE(JSON_EXTRACT(params,'$.cursorLocale')) FROM sys_job WHERE id=?",both)).isEqualTo("de");
        assertThat(run(both,FIRST,100).status()).isEqualTo("SUCCEEDED");assertThat(done(both)).isEqualTo(10);
        assertThat(value("SELECT COUNT(*) FROM sys_job WHERE site_id=?",ROUTE_SITE)).isEqualTo(3);
    }
    @Test void invalidCursorBatchSizeAndMutexNamespaceFailClosed() throws Exception {
        long job=renamed("new");
        for(int size:new int[]{0,101}) assertThatThrownBy(() -> run(job,FIRST,size)).isInstanceOf(Rejected.class).hasMessage("CONTENT_ROUTE_BATCH_INVALID");
        try(var autocommit=routeConnection()) {
            autocommit.setAutoCommit(true);
            assertThatThrownBy(() -> Mutex.lock(autocommit,ROUTE_SITE,MUTEX)).isInstanceOf(Rejected.class).hasMessage("CORE_MUTEX_TRANSACTION_REQUIRED");
        }
        assertThatThrownBy(() -> routeCommitted(c -> Mutex.lock(c,ROUTE_SITE,"user.supplied"))).isInstanceOf(Rejected.class).hasMessage("CORE_MUTEX_NAME_INVALID");
        routeCommitted(c -> update(c,"UPDATE sys_job SET params=JSON_SET(params,'$.cursorLocale','en','$.cursorContentId',999999),progress_done=1 WHERE id=?",job));
        assertThatThrownBy(() -> run(job,FIRST,100)).isInstanceOf(Rejected.class).hasMessage("CONTENT_ROUTE_CURSOR_INVALID");
        assertThat(path(ROOT)).isEqualTo("/en/root");assertThat(redirects()).isZero();assertThat(sequence()).isZero();
    }
}
