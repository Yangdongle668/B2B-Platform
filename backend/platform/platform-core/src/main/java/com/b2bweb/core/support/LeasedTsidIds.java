package com.b2bweb.core.support;

import com.b2bweb.api.common.IdGenerator;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.scheduling.annotation.Scheduled;

/** A database lease is acquired only after core DDL exists; no ID is issued without a live lease. */
public final class LeasedTsidIds implements IdGenerator, AutoCloseable {
  private final DataSource source;
  private final Clock clock;
  private final int preferred;
  private final String owner = UUID.randomUUID().toString();
  private TsidIds generator;
  private int node = -1;
  private long validUntil;
  private boolean failed, closed;

  public LeasedTsidIds(DataSource source, Clock clock, int preferred) {
    if (preferred < 0 || preferred > 1023) throw new IllegalArgumentException("节点号必须为0至1023");
    this.source=source; this.clock=clock; this.preferred=preferred;
  }

  public synchronized void start() {
    if (failed || closed) throw new IllegalStateException("编号节点租约已失效，须重启实例");
    if (generator != null) return;
    long started=System.nanoTime();
    try (var connection=source.getConnection()) {
      connection.setAutoCommit(true);
      checkClock(connection);
      for (int attempt=0; attempt<1024; attempt++) {
        int candidate=(preferred+attempt)%1024;
        String name="core:tsid-node:"+candidate;
        try (var insert=connection.prepareStatement("INSERT INTO sys_lock(lock_name,lock_until,locked_at,locked_by) VALUES(?,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),'') ON DUPLICATE KEY UPDATE lock_name=lock_name")) {
          insert.setString(1,name); insert.executeUpdate();
        }
        try (var claim=connection.prepareStatement("UPDATE sys_lock SET locked_by=?,locked_at=CURRENT_TIMESTAMP(3),lock_until=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 5 MINUTE) WHERE lock_name=? AND lock_until<=CURRENT_TIMESTAMP(3)")) {
          claim.setString(1,owner); claim.setString(2,name);
          if (claim.executeUpdate()==1) {
            node=candidate; generator=new TsidIds(clock,node,true);
            // Conservative monotonic deadline includes SQL latency and a clock handoff margin.
            validUntil=started+Duration.ofSeconds(297).toNanos(); return;
          }
        }
      }
      throw new IllegalStateException("没有可用的编号节点");
    } catch (SQLException | RuntimeException e) {
      failed=true; throw new IllegalStateException("编号节点租约获取失败",e);
    }
  }

  private void checkClock(Connection connection) throws SQLException {
    try (var query=connection.createStatement(); var row=query.executeQuery("SELECT CURRENT_TIMESTAMP(3)")) {
      row.next();
      if (Math.abs(row.getTimestamp(1,java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))).toInstant().toEpochMilli()-clock.millis())>1000)
        throw new IllegalStateException("应用与数据库时钟偏差超过1秒");
    }
  }

  @Scheduled(fixedDelay=60000)
  public synchronized void renew() {
    if (generator==null || closed || failed) return;
    if (!available()) { failed=true; return; }
    long started=System.nanoTime();
    try (var connection=source.getConnection()) {
      connection.setAutoCommit(true); checkClock(connection);
      try (var update=connection.prepareStatement("UPDATE sys_lock SET lock_until=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 5 MINUTE) WHERE lock_name=? AND locked_by=? AND lock_until>CURRENT_TIMESTAMP(3)")) {
        update.setString(1,"core:tsid-node:"+node); update.setString(2,owner);
        if (update.executeUpdate()!=1) throw new IllegalStateException("编号租约持有者已改变");
      }
      validUntil=started+Duration.ofSeconds(297).toNanos();
    } catch (SQLException | RuntimeException e) {
      failed=true;
      org.slf4j.LoggerFactory.getLogger(getClass()).error("编号节点续租失败，停止发号并撤销就绪状态");
    }
  }

  public synchronized boolean available() { return generator!=null && !closed && !failed && System.nanoTime()<validUntil; }
  public synchronized int node() { return node; }
  public synchronized long nextId() {
    start();
    if (!available()) { failed=true; throw new IllegalStateException("编号节点租约已失效"); }
    try { return generator.nextId(); } catch (RuntimeException e) { failed=true; throw e; }
  }
  public Instant timestampOf(long id) { return new TsidIds(clock,0).timestampOf(id); }
  public synchronized void close() { closed=true; /* Expiry quarantines the node for safe handoff. */ }
}
