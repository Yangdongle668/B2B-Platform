package com.b2bplatform.compat.protocol;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;

class InitializationProtocolTest extends ProtocolFixture {
    static final long SITE=991;
    static final AtomicLong IDS=new AtomicLong(991000);
    @BeforeEach void clean() throws Exception {try(var c=connect()){update(c,"DELETE FROM grh_relation_set WHERE site_id=?",SITE);update(c,"DELETE FROM sys_job WHERE site_id=?",SITE);c.commit();}}
    static long graph(Connection c,long source) throws Exception {
        update(c,"INSERT INTO grh_relation_set(id,site_id,source_content_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE id=id",IDS.incrementAndGet(),SITE,source);
        return scalar(c,"SELECT version FROM grh_relation_set WHERE site_id=? AND source_content_id=? FOR UPDATE",SITE,source);
    }
    static long submit(Connection c,String key,String body) throws Exception {
        update(c,"INSERT INTO sys_job(id,site_id,module_id,job_type,title,idempotency_key,params) VALUES(?,?,'graph','graph.recompute','验证',?,CAST(? AS JSON)) ON DUPLICATE KEY UPDATE id=id",IDS.incrementAndGet(),SITE,key,body);
        var row=one(c,"SELECT id,CAST(params AS CHAR) AS params FROM sys_job WHERE site_id=? AND job_type='graph.recompute' AND idempotency_key=? FOR UPDATE",SITE,key);
        // JSON key order/spacing is not an idempotency conflict; MySQL normalizes both sides.
        if(scalar(c,"SELECT CAST(? AS JSON)=CAST(? AS JSON)",row.get("params"),body)!=1)throw new Rejected("CORE_IDEMPOTENCY_KEY_REUSED");
        return number(row.get("id"));
    }
    @Test void graphExclusiveInitializationReadsCurrentVersionAfterWaiting() throws Exception {
        try(var first=connect()) {
            graph(first,10);update(first,"UPDATE grh_relation_set SET version=1 WHERE site_id=? AND source_content_id=10",SITE);
            try(var other=new Contender(Connection.TRANSACTION_REPEATABLE_READ,c->{assertThat(graph(c,10)).isEqualTo(1);})) {
                other.awaitDatabaseLockWait();first.commit();other.succeeds();
            }
        }
    }
    @Test void jobDuplicateCurrentReadSeesWinnerBeyondEarlierRepeatableReadSnapshot() throws Exception {
        try(var first=connect()) {
            long winner=submit(first,"same","{\"x\":1}");
            try(var other=new Contender(Connection.TRANSACTION_REPEATABLE_READ,c->{
                assertThat(scalar(c,"SELECT COUNT(*) FROM sys_job WHERE site_id=?",SITE)).isZero();
                assertThat(submit(c,"same","{ \"x\": 1 }")).isEqualTo(winner);
            })) {other.awaitDatabaseLockWait();first.commit();other.succeeds();}
        }
    }
    @Test void repeatableReadPlainSelectIsAnUnsafeNegativeControl() throws Exception {
        try(var stale=connect();var first=connect()) {
            assertThat(scalar(stale,"SELECT COUNT(*) FROM sys_job WHERE site_id=?",SITE)).isZero();
            long id=submit(first,"snapshot","{}");first.commit();
            update(stale,"INSERT IGNORE INTO sys_job(id,site_id,module_id,job_type,title,idempotency_key) VALUES(?,?,'graph','graph.recompute','验证','snapshot')",IDS.incrementAndGet(),SITE);
            assertThat(rows(stale,"SELECT id FROM sys_job WHERE site_id=? AND idempotency_key='snapshot'",SITE)).isEmpty();
            assertThat(scalar(stale,"SELECT id FROM sys_job WHERE site_id=? AND idempotency_key='snapshot' FOR UPDATE",SITE)).isEqualTo(id);stale.rollback();
        }
    }
    @Test void differentJobPayloadCannotReuseIdempotencyKey() throws Exception {
        try(var c=connect()){submit(c,"key","{\"x\":1}");c.commit();}
        try(var c=connect()){assertThatThrownBy(()->submit(c,"key","{\"x\":2}")).isInstanceOf(Rejected.class);c.rollback();}
    }
    @Test void sourceRollbackLeavesNoJobOrGraphInitialization() throws Exception {
        try(var c=connect()){graph(c,20);submit(c,"rollback","{}");c.rollback();}
        try(var c=connect()){assertThat(scalar(c,"SELECT COUNT(*) FROM grh_relation_set WHERE site_id=?",SITE)).isZero();assertThat(scalar(c,"SELECT COUNT(*) FROM sys_job WHERE site_id=?",SITE)).isZero();}
    }
}
