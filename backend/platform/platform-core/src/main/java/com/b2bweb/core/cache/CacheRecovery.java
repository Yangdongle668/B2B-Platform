package com.b2bweb.core.cache;

import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.port.CdnPurger;
import com.b2bweb.api.site.*;
import com.b2bweb.core.module.ModuleMigrations;
import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Persistent recovery boundary. All network work executes outside database transactions. */
public final class CacheRecovery {
  final boolean scheduledEnabled;
  final DataSource source;
  final JdbcTemplate jdbc;
  final TransactionTemplate tx;
  final RedisTaggedCache cache;
  final SiteApi siteApi;
  final SiteContextProvider sites;
  final ObjectProvider<CdnPurger> cdns;
  final IncompleteEventPublications publications;
  public CacheRecovery(DataSource source, PlatformTransactionManager manager, RedisTaggedCache cache,
      SiteApi siteApi, SiteContextProvider sites, ObjectProvider<CdnPurger> cdns,
      IncompleteEventPublications publications, ModuleMigrations migrated, boolean scheduledEnabled) {
    this.scheduledEnabled=scheduledEnabled; this.source=source; jdbc=new JdbcTemplate(source); tx=new TransactionTemplate(manager);
    this.cache=cache; this.siteApi=siteApi; this.sites=sites; this.cdns=cdns; this.publications=publications;
  }
  @Scheduled(fixedDelayString = "${b2bweb.core.cache.recovery-delay-ms:5000}")
  public void scheduled() {
    if (!scheduledEnabled) return;
    try { recover(); }
    catch (RuntimeException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("缓存恢复尚未完成，继续绕过缓存：{}", e.getClass().getSimpleName()); }
  }
  public boolean recover() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("缓存恢复不能加入业务事务");
    try (var connection=source.getConnection(); var lock=connection.prepareStatement("SELECT GET_LOCK('core:cache-recovery',0)")) {
      try(var result=lock.executeQuery()) { if(!result.next() || result.getInt(1)!=1) return false; }
      try { return exclusive(); }
      finally { try(var release=connection.prepareStatement("SELECT RELEASE_LOCK('core:cache-recovery')")) { release.executeQuery().close(); } }
    } catch(java.sql.SQLException e) { throw new IllegalStateException("缓存恢复锁不可用",e); }
  }
  private boolean exclusive() {
    var state=cache.recoveryState();
    var ledger=jdbc.queryForMap("SELECT boot,epoch,status FROM sys_cache_recovery WHERE id=1");
    if(state.get(2).equals("1") && state.get(0).equals(ledger.get("boot")) && state.get(1).equals(ledger.get("epoch")) && ledger.get("status").equals("READY")) return true;
    List<String> boundary=tx.execute(status -> {
      var row=jdbc.queryForMap("SELECT boot,epoch,status FROM sys_cache_recovery WHERE id=1 FOR UPDATE");
      var current=cache.recoveryState();
      if(current.get(0).equals(row.get("boot")) && current.get(1).equals(row.get("epoch")) && row.get("status").equals("DRAINING")) return List.of(current.get(0),current.get(1));
      var started=cache.beginRecovery(UUID.randomUUID().toString());
      jdbc.update("DELETE FROM sys_cache_recovery_publication");
      jdbc.update("INSERT INTO sys_cache_recovery_publication(epoch,publication_id) SELECT ?,ID FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL",started.get(1));
      jdbc.update("UPDATE sys_cache_recovery SET boot=?,epoch=?,status='DRAINING',started_at=UTC_TIMESTAMP(6) WHERE id=1",started.get(0),started.get(1));
      return List.of(started.get(0),started.get(1));
    });
    long pending=jdbc.queryForObject("SELECT COUNT(*) FROM sys_cache_recovery_publication b JOIN EVENT_PUBLICATION p ON p.ID=b.publication_id WHERE b.epoch=? AND p.COMPLETION_DATE IS NULL",Long.class,boundary.get(1));
    if(pending>0) { publications.resubmitIncompletePublicationsOlderThan(Duration.ofSeconds(5)); return false; }
    var cdn=cdns.getIfAvailable(); if(cdn==null) return false;
    for(var site:siteApi.listOperationalSites()) sites.runAs(site.siteId(),() -> cdn.purgeTags(Set.of(CacheTag.site(site.siteId()))));
    return Boolean.TRUE.equals(tx.execute(status -> {
      var row=jdbc.queryForMap("SELECT boot,epoch FROM sys_cache_recovery WHERE id=1 FOR UPDATE");
      if(!row.get("boot").equals(boundary.get(0)) || !row.get("epoch").equals(boundary.get(1)) || !cache.finishRecovery(boundary.get(0),boundary.get(1))) return false;
      jdbc.update("UPDATE sys_cache_recovery SET status='READY' WHERE id=1");
      return true;
    }));
  }
}
