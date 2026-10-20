package com.b2bplatform.compat.cache;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static com.b2bplatform.compat.cache.CacheProtocolModel.*;

class CacheProtocolTest {
    static final long SITE=901;
    static final String NS="delivery.resolve", KEY="/en/probe", TAG="s901:content:1";
    CacheProtocolModel cache;
    @BeforeEach void connect() { cache=new CacheProtocolModel();cache.redis().flushdb();cache.recoverQuiescentFixture(); }
    @AfterEach void close() {cache.close();}
    Value value(String body) {return new Value(body,Instant.now().plusSeconds(55));}
    Fence capture() {return cache.capture(SITE).orElseThrow();}
    boolean put(String body,Fence fence) {return cache.put(SITE,NS,KEY,value(body),Set.of(TAG),fence);}
    String body() {return cache.get(SITE,NS,KEY).orElseThrow().body();}

    @Test void invalidationDuringBlockedLoaderRejectsItsOldFill() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var threads=Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Value> old=threads.submit(()->cache.getOrLoad(SITE,NS,KEY,Set.of(TAG),()->{
                entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("loader not released");}catch(InterruptedException e){throw new RuntimeException(e);}return value("old");
            }));
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            cache.invalidate(SITE,Set.of(TAG));assertThat(put("new",capture())).isTrue();
            release.countDown();assertThat(old.get(5,TimeUnit.SECONDS).body()).isEqualTo("old");
            assertThat(body()).isEqualTo("new"); // in-flight caller is distinct from cache publication
        } finally {release.countDown();}
    }
    @Test void capturingAfterComputationIsAnUnsafeNegativeControl() {
        Fence before=capture();Value old=value("computed-before-invalidation");cache.invalidate(SITE,Set.of(TAG));
        assertThat(cache.put(SITE,NS,KEY,old,Set.of(TAG),before)).isFalse();
        assertThat(cache.put(SITE,NS,KEY,old,Set.of(TAG),capture())).isTrue(); // intentionally wrong call order
        assertThat(body()).isEqualTo("computed-before-invalidation");
    }
    @Test void cacheHitAlsoChecksFenceEvenWhenPhysicalTagIndexWasLost() {
        assertThat(put("old",capture())).isTrue();cache.redis().del(tagKey(SITE,TAG));
        assertThat(cache.invalidate(SITE,Set.of(TAG))).isZero();
        assertThat(cache.redis().exists(entryKey(SITE,NS,KEY))).isEqualTo(1);
        assertThat(cache.get(SITE,NS,KEY)).isEmpty();
    }
    @Test void keyEvictionAdvancesFenceBeforeOldLoaderCanWrite() {
        Fence before=capture();cache.evict(SITE,NS,KEY);assertThat(put("late",before)).isFalse();
    }
    @Test void namespaceEvictionAdvancesFenceAndKeepsOtherSiteIndependent() {
        Fence before=capture();assertThat(put("old",before)).isTrue();
        Fence b=cache.capture(902).orElseThrow();assertThat(cache.put(902,NS,KEY,value("B"),Set.of("s902:content:1"),b)).isTrue();
        cache.evictNamespace(SITE,NS);assertThat(put("late",before)).isFalse();
        assertThat(cache.get(902,NS,KEY).orElseThrow().body()).isEqualTo("B");
    }
    @Test void foreignSiteTagsCannotChangeEitherSite() {
        Fence before=capture();assertThatThrownBy(()->cache.invalidate(SITE,Set.of("s902:content:1"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(capture()).isEqualTo(before);
        assertThatThrownBy(()->cache.put(SITE,NS,KEY,value("bad"),Set.of("s902:content:1"),before)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void expiredResultsAreNotWrittenOrRegisteredInTagSets() {
        assertThat(cache.put(SITE,NS,KEY,new Value("expired",Instant.now().minusMillis(1)),Set.of(TAG),capture())).isFalse();
        assertThat(cache.redis().exists(entryKey(SITE,NS,KEY),tagKey(SITE,TAG))).isZero();
    }
    @Test void missingFenceMetadataNeverRevalidatesSurvivingOldEntry() {
        Fence before=capture();assertThat(put("old",before)).isTrue();cache.redis().del(fenceKey(SITE));
        assertThat(cache.get(SITE,NS,KEY)).isEmpty();Fence after=capture();
        assertThat(after.incarnation()).isNotEqualTo(before.incarnation());assertThat(put("late",before)).isFalse();
    }
    @Test void fenceMetadataDoesNotExpireBeforeEntriesAndTagsOutliveLongestEntry() {
        Fence f=capture();
        cache.put(SITE,NS,"long",new Value("long",Instant.now().plusSeconds(90)),Set.of(TAG),f);
        cache.put(SITE,NS,"short",new Value("short",Instant.now().plusSeconds(2)),Set.of(TAG),f);
        assertThat(cache.redis().pttl(fenceKey(SITE))).isEqualTo(-1);
        assertThat(cache.redis().pttl(CONTROL)).isEqualTo(-1);
        assertThat(cache.redis().pttl(tagKey(SITE,TAG))).isGreaterThan(80_000);
        assertThat(cache.redis().pttl(entryKey(SITE,NS,"short"))).isBetween(1L,2_000L);
    }
    @Test void counterComparisonRemainsExactAboveLuaDoubleIntegerPrecision() {
        capture();cache.redis().hset(fenceKey(SITE),"counter","9007199254740992");Fence before=capture();
        cache.invalidate(SITE,Set.of(TAG));Fence after=capture();
        assertThat(after.counter()).isEqualTo(9_007_199_254_740_993L);assertThat(put("late",before)).isFalse();
    }
    @Test void signedLongCounterOverflowRotatesIncarnationInsteadOfReusingToken() {
        capture();cache.redis().hset(fenceKey(SITE),"counter",Long.toString(Long.MAX_VALUE));Fence before=capture();
        cache.invalidate(SITE,Set.of(TAG));Fence after=capture();
        assertThat(after.counter()).isZero();assertThat(after.incarnation()).isNotEqualTo(before.incarnation());assertThat(put("late",before)).isFalse();
    }
    @Test void recoveryGateBlocksReadsCapturesAndWritesUntilPendingBarrierIsDrained() {
        Fence before=capture();assertThat(put("old",before)).isTrue();Recovery recovery=cache.beginRecovery();
        assertThat(cache.get(SITE,NS,KEY)).isEmpty();assertThat(cache.capture(SITE)).isEmpty();assertThat(put("late",before)).isFalse();
        assertThat(cache.finishRecovery(recovery,()->true)).isFalse();assertThat(cache.capture(SITE)).isEmpty();
        cache.invalidate(SITE,Set.of(TAG)); // reliable invalidation is allowed while cache serving is gated
        assertThat(cache.finishRecovery(recovery,()->false)).isTrue();assertThat(put("late",before)).isFalse();
    }
    @Test void anOldRecoveryWorkerCannotOpenANewerRecoveryGate() {
        Recovery first=cache.beginRecovery(),second=cache.beginRecovery();
        assertThat(cache.finishRecovery(first,()->false)).isFalse();assertThat(cache.capture(SITE)).isEmpty();
        assertThat(cache.finishRecovery(second,()->false)).isTrue();assertThat(cache.capture(SITE)).isPresent();
    }
    @Test void disconnectedRedisBypassesCacheAndNeverPretendsInvalidationSucceeded() {
        Fence before=capture();cache.disconnect();
        assertThat(cache.get(SITE,NS,KEY)).isEmpty();assertThat(cache.capture(SITE)).isEmpty();assertThat(put("uncached",before)).isFalse();
        assertThat(cache.getOrLoad(SITE,NS,KEY,Set.of(TAG),()->value("database" )).body()).isEqualTo("database");
        assertThatThrownBy(()->cache.invalidate(SITE,Set.of(TAG))).isInstanceOf(RuntimeException.class);
        cache.reconnect();cache.recoverQuiescentFixture();assertThat(put("late",before)).isFalse();
    }
    @Test void redisProcessRestartWithRestoredRdbCannotReviveOldReadyFlagOrTokens() {
        Fence before=capture();assertThat(put("old",before)).isTrue();String oldBoot=cache.runId();
        String oldEpoch=cache.redis().hget(CONTROL,"epoch");cache.restartFromSnapshot();
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100)).ignoreExceptions().until(()->!cache.runId().equals(oldBoot));
        String newBoot=cache.runId();
        assertThat(cache.redis().hget(CONTROL,"epoch")).isEqualTo(oldEpoch); // state really was restored
        assertThat(cache.redis().hget(CONTROL,"ready")).isEqualTo("1");
        assertThat(cache.redis().exists(entryKey(SITE,NS,KEY))).isEqualTo(1);
        assertThat(cache.get(SITE,NS,KEY)).isEmpty();assertThat(cache.capture(SITE)).isEmpty();assertThat(put("late",before)).isFalse();
        cache.recoverQuiescentFixture();assertThat(capture().incarnation()).isNotEqualTo(before.incarnation());assertThat(put("new",capture())).isTrue();
        System.out.println("CACHE_RESTART_PROOF={\"oldBoot\":\""+oldBoot+"\",\"newBoot\":\""+newBoot+"\",\"rdbRestored\":true}");
    }
}
