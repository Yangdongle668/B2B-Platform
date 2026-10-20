package com.b2bweb.core.persistence;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.SiteContextProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionMutex implements TransactionMutexApi {
  private final JdbcTemplate jdbc;
  private final SiteContextProvider sites;
  private final IdGenerator ids;
  private final java.util.Set<String> allowed;

  public TransactionMutex(
      JdbcTemplate jdbc,
      SiteContextProvider sites,
      IdGenerator ids,
      java.util.Set<String> allowed) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.ids = ids;
    this.allowed = java.util.Set.copyOf(allowed);
  }

  public long lock(String name) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("互斥要求调用方事务");
    if (!allowed.contains(name)) throw new IllegalArgumentException("未注册的事务互斥");
    long site = sites.requireSiteId();
    jdbc.update(
        "INSERT INTO sys_transaction_mutex(id,site_id,name) VALUES(?,?,?) ON DUPLICATE KEY UPDATE"
            + " id=id",
        ids.nextId(),
        site,
        name);
    return jdbc.queryForObject(
        "SELECT generation FROM sys_transaction_mutex WHERE site_id=? AND name=? FOR UPDATE",
        Long.class,
        site,
        name);
  }

  public long advance(String name) {
    long generation = lock(name);
    if (generation == Long.MAX_VALUE) throw new IllegalStateException("代次超界");
    jdbc.update(
        "UPDATE sys_transaction_mutex SET generation=generation+1 WHERE site_id=? AND name=?",
        sites.requireSiteId(),
        name);
    return generation + 1;
  }
}
