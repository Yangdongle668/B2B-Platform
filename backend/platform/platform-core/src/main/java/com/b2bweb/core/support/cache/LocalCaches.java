package com.b2bweb.core.support.cache;

import com.github.benmanes.caffeine.cache.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.*;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;

/** TTL bounds missed messages; reconnect clears all caches before subscription is usable again. */
public final class LocalCaches implements AutoCloseable {
  static final String CHANNEL="sys:core:cache-evict";
  final String origin=UUID.randomUUID().toString();
  final JsonMapper json;
  final StringRedisTemplate redis;
  final RedisMessageListenerContainer listener;
  final Map<String,Cache<String,Object>> caches=new ConcurrentHashMap<>();
  public LocalCaches(RedisConnectionFactory factory, JsonMapper json) {
    this.json=json; redis=new StringRedisTemplate(factory);
    listener=new RedisMessageListenerContainer(); listener.setConnectionFactory(factory);
    class Subscriber implements MessageListener, SubscriptionListener {
      public void onMessage(Message message, byte[] pattern) {
        try {
          var data=json.readTree(new String(message.getBody(),StandardCharsets.UTF_8));
          if(data.path("origin").asString().equals(origin)) return;
          var cache=caches.get(data.path("cache").asString()); if(cache==null) return;
          if(data.path("all").asBoolean()) cache.invalidateAll(); else cache.invalidate(data.path("key").asString());
        } catch(RuntimeException e) { clear(); }
      }
      public void onChannelSubscribed(byte[] channel,long count) { clear(); }
    }
    listener.addMessageListener(new Subscriber(),new ChannelTopic(CHANNEL));
    listener.afterPropertiesSet(); listener.start();
  }
  public <K,V> BroadcastCache<K,V> create(String name, LocalCacheSpec spec) {
    if(!name.matches("[a-z][a-z0-9]*\\.[a-z][a-z0-9_.]*")) throw new IllegalArgumentException("本地缓存名称无效");
    Cache<String,Object> cache=Caffeine.newBuilder().maximumSize(spec.maximumSize()).expireAfterWrite(spec.ttl()).build();
    if(caches.putIfAbsent(name,cache)!=null) throw new IllegalArgumentException("本地缓存名称重复");
    return new BroadcastCache<>() {
      @SuppressWarnings("unchecked") public Optional<V> getIfPresent(K key) { return Optional.ofNullable((V)cache.getIfPresent(json.writeValueAsString(key))); }
      @SuppressWarnings("unchecked") public V get(K key,Function<? super K,? extends V> loader) { return (V)cache.get(json.writeValueAsString(key),ignored -> loader.apply(key)); }
      public void invalidate(K key) { evict(name,cache,json.writeValueAsString(key),false); }
      public void invalidateAll() { evict(name,cache,"",true); }
    };
  }
  private void evict(String name,Cache<String,Object> cache,String key,boolean all) {
    Runnable local=() -> { if(all) cache.invalidateAll(); else cache.invalidate(key); };
    local.run();
    Runnable committed=() -> {
      local.run();
      try { redis.convertAndSend(CHANNEL,json.writeValueAsString(Map.of("origin",origin,"cache",name,"key",key,"all",all))); }
      catch(org.springframework.dao.DataAccessException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("本地缓存广播失败，期限兜底：{}",name); }
    };
    if(TransactionSynchronizationManager.isActualTransactionActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { public void afterCommit() { committed.run(); } });
    else committed.run();
  }
  public void clear() { caches.values().forEach(Cache::invalidateAll); }
  public void close() throws Exception { listener.stop(); listener.destroy(); clear(); }
}
