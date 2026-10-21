package com.b2bweb.core.support.cache;
import java.util.Optional;
import java.util.function.Function;
public interface BroadcastCache<K,V> {
  Optional<V> getIfPresent(K key);
  V get(K key, Function<? super K,? extends V> loader);
  void invalidate(K key);
  void invalidateAll();
}
