package com.b2bplatform.compat.cache;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Executable single-node cache protocol only; not production infrastructure. */
public final class CacheProtocolModel implements AutoCloseable {
    public record Fence(String incarnation, long counter) {}
    public record Recovery(String boot, String epoch) {}
    public record Value(String body, Instant freshUntil) {}
    static final String CONTROL = "m0-cache:control";
    static final String SCRIPT;
    static {
        try (var stream = CacheProtocolModel.class.getResourceAsStream("/cache/protocol.lua")) {
            SCRIPT = new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    final RedisClient client;
    volatile StatefulRedisConnection<String,String> connection;
    public CacheProtocolModel() {
        String uri = Objects.requireNonNull(System.getenv("PROBE_REDIS_URI"), "Run verify_backend.py");
        if (!uri.equals("redis://redis:6379/0")) throw new IllegalArgumentException("Only disposable runner Redis is allowed");
        client = RedisClient.create(uri); client.setDefaultTimeout(Duration.ofSeconds(2));
        client.setOptions(ClientOptions.builder().socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build());
        reconnect();
    }
    public synchronized void reconnect() { if (connection != null) connection.close(); connection=client.connect(); }
    public void disconnect() { connection.close(); }
    public RedisCommands<String,String> redis() { return connection.sync(); }
    static String prefix(long site) { if (site<=0) throw new IllegalArgumentException("site"); return "m0-cache:s"+site+":"; }
    static String fenceKey(long site) { return prefix(site)+"fence"; }
    static String entryKey(long site,String namespace,String key) { return prefix(site)+"value:"+namespace+":"+key; }
    static String tagKey(long site,String tag) {
        if (!tag.startsWith("s"+site+":")) throw new IllegalArgumentException("Cross-site cache tag");
        return prefix(site)+"tag:"+tag;
    }
    Object execute(String op,long site,String namespace,String key,String... extra) {
        var args=new ArrayList<String>(List.of(op,UUID.randomUUID().toString(),UUID.randomUUID().toString()));
        args.addAll(List.of(extra));
        ScriptOutputType type=Set.of("get","capture","begin").contains(op)?ScriptOutputType.MULTI:ScriptOutputType.INTEGER;
        return redis().eval(SCRIPT,type,new String[]{CONTROL,fenceKey(site),entryKey(site,namespace,key)},args.toArray(String[]::new));
    }
    public Recovery beginRecovery() {
        var result=(List<?>)execute("begin",1,"control","unused");
        return new Recovery((String)result.get(0),(String)result.get(1));
    }
    public boolean finishRecovery(Recovery recovery,BooleanSupplier pendingAtBarrier) {
        if (pendingAtBarrier.getAsBoolean()) return false;
        return ((Number)redis().eval(SCRIPT,ScriptOutputType.INTEGER,new String[]{CONTROL,fenceKey(1),"unused"},"finish",recovery.epoch(),recovery.boot())).longValue()==1;
    }
    public void recoverQuiescentFixture() {var r=beginRecovery();if(!finishRecovery(r,()->false))throw new IllegalStateException("Recovery failed");}
    public Optional<Fence> capture(long site) {
        try {
            var value=(List<?>)execute("capture",site,"control","unused");
            return value.isEmpty()?Optional.empty():Optional.of(new Fence((String)value.get(0),Long.parseLong((String)value.get(1))));
        } catch(RedisException e) { return Optional.empty(); }
    }
    public Optional<Value> get(long site,String namespace,String key) {
        try {
            var value=(List<?>)execute("get",site,namespace,key);
            return value.isEmpty()?Optional.empty():Optional.of(new Value((String)value.get(0),Instant.ofEpochMilli(Long.parseLong((String)value.get(1)))));
        } catch(RedisException e) { return Optional.empty(); }
    }
    public boolean put(long site,String namespace,String key,Value value,Set<String> tags,Fence fence) {
        var args=new ArrayList<String>(List.of(fence.incarnation(),Long.toString(fence.counter()),Long.toString(value.freshUntil().toEpochMilli()),value.body()));
        tags.stream().sorted().map(t->tagKey(site,t)).forEach(args::add);
        try {return ((Number)execute("put",site,namespace,key,args.toArray(String[]::new))).longValue()==1;}
        catch(RedisException e) {return false;}
    }
    public long invalidate(long site,Collection<String> tags) {
        var args=new ArrayList<String>(List.of("tags"));tags.stream().sorted().map(t->tagKey(site,t)).forEach(args::add);
        return ((Number)execute("invalidate",site,"control","unused",args.toArray(String[]::new))).longValue();
    }
    public void evict(long site,String namespace,String key) {execute("invalidate",site,namespace,key,"key",entryKey(site,namespace,key));}
    public void evictNamespace(long site,String namespace) {execute("invalidate",site,namespace,"unused","namespace",entryKey(site,namespace,"*"));}
    public Value getOrLoad(long site,String namespace,String key,Set<String> tags,Supplier<Value> loader) {
        var hit=get(site,namespace,key);if(hit.isPresent())return hit.get();
        var before=capture(site);Value result=loader.get();before.ifPresent(f->put(site,namespace,key,result,tags,f));return result;
    }
    public String runId() {
        return redis().info("server").lines().filter(l->l.startsWith("run_id:")).findFirst().orElseThrow().substring(7).trim();
    }
    public void restartFromSnapshot() {
        redis().save();
        try {redis().shutdown(true);} catch(RedisException expectedDisconnect) { /* process exited after save */ }
    }
    @Override public void close() {connection.close();client.shutdown();}
}
