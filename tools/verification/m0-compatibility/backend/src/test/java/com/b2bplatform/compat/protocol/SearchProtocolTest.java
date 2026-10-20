package com.b2bplatform.compat.protocol;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.*;import java.net.http.*;import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;

/** Real MySQL control tables and disposable Meilisearch; synthetic capacity documents. */
class SearchProtocolTest {
    static final long SITE=992;
    static final AtomicLong IDS=new AtomicLong(9920000);
    static final ObjectMapper JSON=new ObjectMapper();
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    static final String BASE=Objects.requireNonNull(System.getenv("PROBE_SEARCH_URL"));
    record Build(long id,String index,String base,long version,long fence,long start) {}
    static JsonNode request(String method,String path,JsonNode body) throws Exception {
        if(!BASE.equals("http://meilisearch:7700"))throw new IllegalStateException("Only disposable search engine allowed");
        var req=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var res=HTTP.send(req,HttpResponse.BodyHandlers.ofString());
        if(res.statusCode()>=400)throw new IllegalStateException("Engine HTTP "+res.statusCode()+": "+res.body());
        return JSON.readTree(res.body());
    }
    static long task(JsonNode accepted) throws Exception {
        long id=accepted.path("taskUid").asLong(-1);assertThat(id).isNotNegative();
        long until=System.nanoTime()+Duration.ofSeconds(120).toNanos();
        while(System.nanoTime()<until) {
            var t=request("GET","/tasks/"+id,null);String status=t.path("status").asText();
            if(status.equals("succeeded"))return id;
            if(status.equals("failed")||status.equals("canceled"))throw new IllegalStateException("Engine task "+t);
            Thread.sleep(20);
        }
        throw new IllegalStateException("Engine task timeout");
    }
    @BeforeEach void reset() throws Exception {
        try(var c=connect()) {for(String table:List.of("sch_build","sch_change","sch_index_state"))update(c,"DELETE FROM "+table+" WHERE site_id=?",SITE);c.commit();}
        for(var row:request("GET","/indexes?limit=1000",null).path("results"))if(row.path("uid").asText().startsWith("m0_search_"))task(request("DELETE","/indexes/"+row.path("uid").asText(),null));
    }
    static void state(Connection c,String locale) throws Exception {update(c,"INSERT INTO sch_index_state(id,site_id,locale) VALUES(?,?,?) ON DUPLICATE KEY UPDATE id=id",IDS.incrementAndGet(),SITE,locale);}
    static long capture(Connection c,String locale,boolean full,long id) throws Exception {
        state(c,locale);update(c,"UPDATE sch_index_state SET change_seq=change_seq+1 WHERE site_id=? AND locale=?",SITE,locale);
        long seq=scalar(c,"SELECT change_seq FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",SITE,locale);
        update(c,"INSERT INTO sch_change(id,site_id,locale,sequence_no,localization_ids,full_refresh) VALUES(?,?,?,?,CAST(? AS JSON),?)",IDS.incrementAndGet(),SITE,locale,seq,full?"[]":"[\""+id+"\"]",full);return seq;
    }
    static Build claim(String locale,boolean full) throws Exception {
        try(var c=connect()) {
            state(c,locale);var s=one(c,"SELECT * FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",SITE,locale);
            long id=IDS.incrementAndGet(),f=number(s.get("build_fence"))+1,v=number(s.get("active_version"));String base=full?null:(String)s.get("active_index");
            long start=base==null?number(s.get("change_seq")):number(s.get("applied_seq"));String index="m0_search_"+locale+"_g_"+id;
            update(c,"UPDATE sch_index_state SET build_fence=? WHERE site_id=? AND locale=?",f,SITE,locale);
            update(c,"INSERT INTO sch_build(id,site_id,locale,job_id,physical_index,base_index,expected_active_version,build_fence,start_seq,applied_seq) VALUES(?,?,?,?,?,?,?,?,?,?)",id,SITE,locale,id,index,base,v,f,start,start);c.commit();
            task(request("POST","/indexes",JSON.createObjectNode().put("uid",index).put("primaryKey","id")));return new Build(id,index,base,v,f,start);
        }
    }
    static void write(Build b,ArrayNode docs) throws Exception {
        try(var c=connect()) {assertThat(one(c,"SELECT status FROM sch_build WHERE id=?",b.id()).get("status")).isEqualTo("BUILDING");}
        try {
            var accepted=request("POST","/indexes/"+b.index()+"/documents",docs);
            try(var c=connect()){update(c,"UPDATE sch_build SET last_engine_task_id=? WHERE id=?",accepted.path("taskUid").asLong(),b.id());c.commit();}
            task(accepted);
        } catch(Exception failure) {
            try(var c=connect()){update(c,"UPDATE sch_build SET status='ABANDONED' WHERE id=?",b.id());c.commit();}
            throw failure;
        }
    }
    static long copy(Build b) throws Exception {
        if(b.base()==null)return 0;long count=0;
        while(true) {
            var docs=request("GET","/indexes/"+b.base()+"/documents?offset="+count+"&limit=500",null).path("results");
            if(docs.isEmpty())return count;write(b,(ArrayNode)docs);count+=docs.size();
        }
    }
    static boolean publish(Build b,String locale,long h) throws Exception {
        try(var c=connect()) {
            var s=one(c,"SELECT * FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",SITE,locale);
            var build=one(c,"SELECT status,last_engine_task_id FROM sch_build WHERE id=? FOR UPDATE",b.id());
            if(!build.get("status").equals("BUILDING") || build.get("last_engine_task_id")==null){c.rollback();return false;}
            if(b.base()!=null&&scalar(c,"SELECT COUNT(*) FROM sch_change WHERE site_id=? AND locale=? AND full_refresh=1 AND sequence_no>? AND sequence_no<=?",SITE,locale,b.start(),h)>0){update(c,"UPDATE sch_build SET status='ABANDONED' WHERE id=?",b.id());c.commit();return false;}
            if(number(s.get("change_seq"))!=h||number(s.get("build_fence"))!=b.fence()||number(s.get("active_version"))!=b.version()){c.rollback();return false;}
            update(c,"UPDATE sch_build SET status='RETIRED' WHERE site_id=? AND locale=? AND status='ACTIVE'",SITE,locale);
            update(c,"UPDATE sch_index_state SET active_index=?,active_version=active_version+1,applied_seq=? WHERE site_id=? AND locale=?",b.index(),h,SITE,locale);
            update(c,"UPDATE sch_build SET status='ACTIVE',applied_seq=? WHERE id=?",h,b.id());c.commit();return true;
        }
    }
    static ArrayNode documents(int n) {var docs=JSON.createArrayNode();for(int i=0;i<n;i++)docs.addObject().put("id",Integer.toString(i+1)).put("type",i<10000?"PRODUCT":"ARTICLE").put("title","industrial precision equipment "+i).put("path","/zh/probe/"+i).put("revision",1);return docs;}
    @Test void captureRollsBackWithSourceTransaction() throws Exception {
        try(var c=connect()){capture(c,"en",false,1);c.rollback();}
        try(var c=connect()){assertThat(scalar(c,"SELECT COUNT(*) FROM sch_change WHERE site_id=?",SITE)).isZero();}
    }
    @Test void incrementalCopyStartsAtAppliedSequenceRatherThanCurrentHighWater() throws Exception {
        var a=claim("en",true);write(a,documents(1));assertThat(publish(a,"en",0)).isTrue();
        try(var c=connect()){capture(c,"en",false,1);c.commit();}
        var b=claim("en",false);assertThat(b.start()).isZero();assertThat(copy(b)).isEqualTo(1);
        assertThat(publish(b,"en",0)).isFalse();
        var replacement=JSON.createArrayNode();replacement.addObject().put("id","1").put("revision",2);write(b,replacement);
        assertThat(publish(b,"en",1)).isTrue();
    }
    @Test void supersededWorkerCanOnlyWriteItsPrivateGeneration() throws Exception {
        var old=claim("en",true);var newer=claim("en",true);write(newer,documents(2));assertThat(publish(newer,"en",0)).isTrue();
        write(old,documents(1));assertThat(publish(old,"en",0)).isFalse();
        assertThat(request("GET","/indexes/"+newer.index()+"/stats",null).path("numberOfDocuments").asInt()).isEqualTo(2);
    }
    @Test void lostPointerCommitResponseIsResolvedByReadingPersistentPointer() throws Exception {
        var b=claim("en",true);write(b,documents(1));assertThat(publish(b,"en",0)).isTrue();
        assertThat(publish(b,"en",0)).isFalse();
        try(var c=connect()){assertThat(one(c,"SELECT active_index FROM sch_index_state WHERE site_id=? AND locale='en'",SITE).get("active_index")).isEqualTo(b.index());}
    }
    @Test void failedEngineTaskCannotBePublished() throws Exception {
        var b=claim("en",true);var bad=JSON.createArrayNode();bad.addObject().put("title","missing primary key");
        assertThatThrownBy(()->write(b,bad)).isInstanceOf(IllegalStateException.class);assertThat(publish(b,"en",0)).isFalse();
        try(var c=connect()){assertThat(one(c,"SELECT active_index FROM sch_index_state WHERE site_id=? AND locale='en'",SITE).get("active_index")).isNull();}
    }
    @Test void garbageCollectionProtectsActiveAndLiveBuildBase() throws Exception {
        var a=claim("en",true);write(a,documents(1));publish(a,"en",0);var b=claim("en",false);
        try(var c=connect()) {
            long refs=scalar(c,"SELECT COUNT(*) FROM sch_build b WHERE b.site_id=? AND (b.status IN ('ACTIVE','BUILDING','READY') OR EXISTS(SELECT 1 FROM sch_build live WHERE live.site_id=b.site_id AND live.status IN ('BUILDING','READY') AND live.base_index=b.physical_index))",SITE);
            assertThat(refs).isEqualTo(2);
        }
        assertThat(b.base()).isEqualTo(a.index());
    }
    @Test void fullRefreshAfterCopyStartCannotPublishAnIncrementalCandidate() throws Exception {
        var initial=claim("en",true);write(initial,documents(1));assertThat(publish(initial,"en",0)).isTrue();
        var copy=claim("en",false);copy(copy);
        try(var c=connect()){capture(c,"en",true,0);c.commit();}
        assertThat(publish(copy,"en",1)).isFalse();
        var full=claim("en",true);write(full,documents(2));assertThat(publish(full,"en",1)).isTrue();
    }
    @Test void mutationWithLostAcceptedResponseIsAbandonedAndCannotDamageNewActive() throws Exception {
        var old=claim("en",true);
        // The engine accepts work, then the caller loses the HTTP response. Never retry this UID.
        request("POST","/indexes/"+old.index()+"/documents",documents(1));
        try(var c=connect()){update(c,"UPDATE sch_build SET status='ABANDONED' WHERE id=?",old.id());c.commit();}
        var current=claim("en",true);write(current,documents(2));assertThat(publish(current,"en",0)).isTrue();
        assertThat(publish(old,"en",0)).isFalse();assertThat(request("GET","/indexes/"+current.index()+"/stats",null).path("numberOfDocuments").asInt()).isEqualTo(2);
    }
    @Test void tenLocaleCapacityAndSourceCaptureMeetDeclaredCandidateBudgets() throws Exception {
        long began=System.nanoTime();var docs=documents(15000);List<Long> query=new ArrayList<>(),captures=new ArrayList<>();
        for(int lang=0;lang<10;lang++) {
            String locale="l"+lang;var a=claim(locale,true);write(a,docs);assertThat(publish(a,locale,0)).isTrue();
            var b=claim(locale,false);assertThat(copy(b)).isEqualTo(15000);assertThat(publish(b,locale,0)).isTrue();
            for(int i=0;i<10;i++){long t=System.nanoTime();assertThat(request("POST","/indexes/"+b.index()+"/search",JSON.createObjectNode().put("q","precision").put("limit",20)).path("hits").size()).isEqualTo(20);query.add(System.nanoTime()-t);}
        }
        double seconds=(System.nanoTime()-began)/1e9;
        for(int i=0;i<100;i++) {
            long t=System.nanoTime();try(var c=connect()){
                update(c,"INSERT INTO cnt_content(id,site_id,type,owner_module) VALUES(?,?,'PRODUCT','product') ON DUPLICATE KEY UPDATE version=version+1",99210000L+i,SITE);
                for(int lang=0;lang<10;lang++)capture(c,"l"+lang,false,i+1);c.commit();}captures.add(System.nanoTime()-t);
        }
        long catchupBegan=System.nanoTime();
        for(int lang=0;lang<10;lang++){
            String locale="l"+lang;var b=claim(locale,false);assertThat(b.start()).isZero();assertThat(copy(b)).isEqualTo(15000);
            Set<String> changed=new LinkedHashSet<>();
            try(var c=connect()){try(var statement=c.prepareStatement("SELECT localization_ids FROM sch_change WHERE site_id=? AND locale=? AND sequence_no>? AND sequence_no<=100 ORDER BY sequence_no")){statement.setLong(1,SITE);statement.setString(2,locale);statement.setLong(3,b.start());try(var rows=statement.executeQuery()){while(rows.next())for(var id:JSON.readTree(rows.getString(1)))changed.add(id.asText());}}}
            assertThat(changed).hasSize(100);var upserts=JSON.createArrayNode();var deleted=JSON.createArrayNode();
            for(String id:changed){if(Integer.parseInt(id)%5==0)deleted.add(id);else upserts.addObject().put("id",id).put("title","industrial precision equipment changed").put("revision",2);}
            write(b,upserts);task(request("POST","/indexes/"+b.index()+"/documents/delete-batch",deleted));
            assertThat(publish(b,locale,100)).isTrue();
            assertThat(request("GET","/indexes/"+b.index()+"/stats",null).path("numberOfDocuments").asInt()).isEqualTo(14980);
            assertThat(request("GET","/indexes/"+b.index()+"/documents/1",null).path("revision").asInt()).isEqualTo(2);
        }
        double catchupSeconds=(System.nanoTime()-catchupBegan)/1e9;assertThat(catchupSeconds).isLessThan(600);
        Collections.sort(query);Collections.sort(captures);double q95=query.get(94)/1e6,c95=captures.get(94)/1e6;
        System.out.println("SEARCH_CAPACITY_PROOF={\"locales\":10,\"documentsPerLocale\":15000,\"generationDocumentsWritten\":300000,\"initialAndCopySeconds\":"+seconds+",\"queryP95Ms\":"+q95+",\"captureTenLocalesP95Ms\":"+c95+",\"coalescedCatchupSeconds\":"+catchupSeconds+",\"sourceSqlIncluded\":true,\"idType\":\"string\"}");
        assertThat(seconds).isLessThan(600);assertThat(q95).isLessThan(200);assertThat(c95).isLessThan(50);
    }
}
