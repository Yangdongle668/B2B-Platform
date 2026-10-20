package com.b2bplatform.compat.cache;

import com.sun.net.httpserver.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** Real HTTP transport, deterministic injected clock; SSR/edge surrogates, not Nuxt/CDN deployment. */
class FreshnessProtocolTest {
    static final String DEADLINE="X-Content-Fresh-Until";
    static final long START=Instant.parse("2030-01-01T00:00:00Z").toEpochMilli();
    final AtomicLong now=new AtomicLong(START);
    final HttpClient http=HttpClient.newHttpClient();
    HttpServer backend,ssr,edge;
    long documentUntil=START+55000,siteUntil=START+55000,renderMillis=0;
    int status=200,originCalls=0;
    boolean unavailable=false,preview=false,privateResponse=false,malformed=false,missingSite=false;
    Cached cached;
    record Cached(String body,int status,long until,long storedAt,Map<String,String> headers) {}
    @BeforeEach void start() throws Exception {
        backend=server();backend.createContext("/document",ex->origin(ex,false));backend.createContext("/site",ex->origin(ex,true));backend.start();
        ssr=server();ssr.createContext("/page",this::render);ssr.start();
        edge=server();edge.createContext("/page",this::serveEdge);edge.start();
    }
    static HttpServer server() throws Exception {return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);}
    static URI uri(HttpServer server,String path) {return URI.create("http://localhost:"+server.getAddress().getPort()+path);}
    @AfterEach void stop() {edge.stop(0);ssr.stop(0);backend.stop(0);http.close();}
    void origin(HttpExchange ex,boolean site) throws java.io.IOException {
        originCalls++;
        if(unavailable) {send(ex,503,"unavailable",Map.of("Cache-Control","no-store"));return;}
        var headers=new HashMap<String,String>();
        if(!(site&&missingSite))headers.put(DEADLINE,malformed&&!site?"invalid":Instant.ofEpochMilli(site?siteUntil:documentUntil).toString());
        send(ex,site?200:status,site?"site":"document",headers);
    }
    HttpResponse<String> fetch(URI uri) throws Exception {return http.send(HttpRequest.newBuilder(uri).GET().build(),HttpResponse.BodyHandlers.ofString());}
    OptionalLong deadline(HttpResponse<?> response) {
        try {return response.headers().firstValue(DEADLINE).map(v->OptionalLong.of(Instant.parse(v).toEpochMilli())).orElse(OptionalLong.empty());}
        catch(DateTimeException e) {return OptionalLong.empty();}
    }
    void render(HttpExchange ex) throws java.io.IOException {
        try {
            long started=now.get();var doc=fetch(uri(backend,"/document"));var site=fetch(uri(backend,"/site"));now.addAndGet(renderMillis);
            if(doc.statusCode()>=500||site.statusCode()>=500) {send(ex,503,"unavailable",Map.of("Cache-Control","no-store","CDN-Cache-Control","no-store","Retry-After","120"));return;}
            var docDeadline=deadline(doc);var siteDeadline=deadline(site);
            long until=docDeadline.isPresent()&&siteDeadline.isPresent()?Math.min(started+55000,Math.min(docDeadline.getAsLong(),siteDeadline.getAsLong())):0;
            long ttl=Math.max(0,Math.min(55,(until-now.get())/1000));
            boolean store=ttl>0&&now.get()-started<=5000&&!preview&&!privateResponse;
            var headers=new HashMap<String,String>();
            headers.put("Cache-Control",store?"public, max-age=0, must-revalidate":"no-store");
            headers.put("CDN-Cache-Control",store?"public, max-age="+ttl:"no-store");
            if(store)headers.put(DEADLINE,Instant.ofEpochMilli(until).toString());
            if(doc.statusCode()==301)headers.put("Location","/replacement");
            send(ex,doc.statusCode(),"rendered:"+doc.body()+":"+site.body(),headers);
        }catch(Exception failure) {send(ex,503,"unavailable",Map.of("Cache-Control","no-store","CDN-Cache-Control","no-store","Retry-After","120"));}
    }
    void serveEdge(HttpExchange ex) throws java.io.IOException {
        try {
            if(cached!=null&&now.get()<cached.until()) {
                var headers=new HashMap<>(cached.headers());headers.put("Age",Long.toString((now.get()-cached.storedAt())/1000));
                send(ex,cached.status(),cached.body(),headers);return;
            }
            cached=null;var response=fetch(uri(ssr,"/page"));var headers=new HashMap<String,String>();
            for(String key:List.of("Cache-Control","CDN-Cache-Control",DEADLINE,"Location","Retry-After"))response.headers().firstValue(key).ifPresent(v->headers.put(key,v));
            String policy=headers.getOrDefault("CDN-Cache-Control","no-store");
            if(!policy.contains("no-store")&&deadline(response).isPresent()) {
                long ttl=Long.parseLong(policy.substring(policy.indexOf("max-age=")+8));
                long until=Math.min(deadline(response).getAsLong(),now.get()+ttl*1000);
                cached=new Cached(response.body(),response.statusCode(),until,now.get(),Map.copyOf(headers));
            }
            headers.put("Age","0");send(ex,response.statusCode(),response.body(),headers);
        }catch(Exception failure) {send(ex,503,"unavailable",Map.of("Cache-Control","no-store","CDN-Cache-Control","no-store","Retry-After","120"));}
    }
    static void send(HttpExchange ex,int status,String body,Map<String,String> headers) throws java.io.IOException {
        headers.forEach((k,v)->ex.getResponseHeaders().set(k,v));byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status,bytes.length);ex.getResponseBody().write(bytes);ex.close();
    }
    HttpResponse<String> page() throws Exception {return fetch(uri(edge,"/page"));}
    String header(HttpResponse<?> response,String name) {return response.headers().firstValue(name).orElse("");}
    void noStore(HttpResponse<?> response) {assertThat(header(response,"CDN-Cache-Control")).isEqualTo("no-store");assertThat(cached).isNull();}
    @Test void earliestDependencyAndRenderTimeReduceSharedBudget() throws Exception {
        siteUntil=START+41000;renderMillis=2300;var response=page();
        assertThat(header(response,"CDN-Cache-Control")).isEqualTo("public, max-age=38");
        assertThat(header(response,DEADLINE)).isEqualTo(Instant.ofEpochMilli(siteUntil).toString());
        assertThat(header(response,"Cache-Control")).isEqualTo("public, max-age=0, must-revalidate");
    }
    @Test void edgeHitPreservesDeadlineAndAccumulatesAge() throws Exception {
        var first=page();now.addAndGet(19000);var hit=page();
        assertThat(header(hit,DEADLINE)).isEqualTo(header(first,DEADLINE));assertThat(header(hit,"Age")).isEqualTo("19");assertThat(originCalls).isEqualTo(2);
    }
    @Test void expiredEdgeEntryAndFailedOriginReturn503WithoutStale() throws Exception {
        page();now.addAndGet(55000);unavailable=true;var response=page();
        assertThat(response.statusCode()).isEqualTo(503);assertThat(header(response,"Retry-After")).isEqualTo("120");noStore(response);
    }
    @Test void untrustedBrowserDeadlineCannotExtendBudget() throws Exception {
        var response=http.send(HttpRequest.newBuilder(uri(edge,"/page")).header(DEADLINE,"2099-01-01T00:00:00Z").GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(header(response,DEADLINE)).isEqualTo(Instant.ofEpochMilli(documentUntil).toString());
    }
    @Test void malformedBackendDeadlineDisablesCaching() throws Exception {malformed=true;noStore(page());}
    @Test void missingDependencyDeadlineDisablesCaching() throws Exception {missingSite=true;noStore(page());}
    @Test void loaderOverFiveSecondsDisablesCaching() throws Exception {renderMillis=6000;noStore(page());}
    @Test void subsecondRemainingBudgetRoundsDownToNoStore() throws Exception {siteUntil=START+999;noStore(page());}
    @ParameterizedTest @ValueSource(ints={200,301,404,410})
    void publicStatusesShareAbsoluteDeadline(int code) throws Exception {
        status=code;var response=page();assertThat(response.statusCode()).isEqualTo(code);
        assertThat(header(response,DEADLINE)).isEqualTo(Instant.ofEpochMilli(documentUntil).toString());
        now.addAndGet(55000);noStore(page());
    }
    @Test void previewResponseIsNeverCached() throws Exception {preview=true;noStore(page());}
    @Test void privateResponseIsNeverCached() throws Exception {privateResponse=true;noStore(page());}
}
