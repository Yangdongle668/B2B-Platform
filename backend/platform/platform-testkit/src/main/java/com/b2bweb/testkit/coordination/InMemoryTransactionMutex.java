package com.b2bweb.testkit.coordination;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

/**
 * The shared MemoryStore transaction lock deliberately provides stronger serialization than per-row
 * SQL.
 */
public final class InMemoryTransactionMutex implements TransactionMutexApi {
  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final Set<String> names;

  public InMemoryTransactionMutex(MemoryStore store, SiteContextProvider sites, Set<String> names) {
    this.store = store;
    this.sites = sites;
    this.names = Set.copyOf(names);
  }

  private String key(String name) {
    if (!org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) throw new IllegalStateException("事务互斥需要调用方事务");
    if (!names.contains(name)) throw new IllegalArgumentException("互斥名称未登记");
    return "transaction-mutex:" + sites.requireSiteId() + ":" + name;
  }

  public long lock(String name) {
    String key = key(name);
    return store.atomic(rows -> ((Number) rows.getOrDefault(key, 0L)).longValue());
  }

  public long advance(String name) {
    String key = key(name);
    return store.atomic(
        rows -> {
          long next = Math.addExact(((Number) rows.getOrDefault(key, 0L)).longValue(), 1);
          rows.put(key, next);
          return next;
        });
  }
}
