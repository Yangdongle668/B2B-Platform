package com.b2bweb.core.support;

import com.b2bweb.api.common.IdGenerator;
import java.time.*;

/** 41-bit UTC milliseconds from 2020 + 10-bit externally unique node + 12-bit sequence. */
public final class TsidIds implements IdGenerator {
  private static final long EPOCH = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli();
  private final Clock clock;
  private final int node;
  private final boolean strict;
  private long last = -1;
  private int sequence;

  public TsidIds(Clock clock, int node) {
    this(clock,node,false);
  }

  public TsidIds(Clock clock,int node,boolean strict) {
    this.strict=strict;
    if (node < 0 || node > 1023) throw new IllegalArgumentException("节点编号必须为0至1023");
    this.clock = clock;
    this.node = node;
  }

  public synchronized long nextId() {
    long now = clock.millis() - EPOCH;
    if(strict && (now<last || now==last && sequence==4095)) {
      long deadline=System.nanoTime()+Duration.ofSeconds(1).toNanos();
      if(last-now>1000) throw new IllegalStateException("编号时钟回拨超过1秒");
      while(now<last || now==last && sequence==4095) {
        if(System.nanoTime()>=deadline) throw new IllegalStateException("编号时钟未恢复，停止发号");
        java.util.concurrent.locks.LockSupport.parkNanos(100000);
        now=clock.millis()-EPOCH;
      }
    }
    now=Math.max(now,last);
    if (now == last) {
      if (++sequence > 4095) {
        now++;
        sequence = 0;
      }
    } else sequence = 0;
    if (now < 0 || now > 0x1ffffffffffL) throw new IllegalStateException("TSID时间超界");
    last = now;
    return (now << 22) | ((long) node << 12) | sequence;
  }

  public Instant timestampOf(long id) {
    if (id <= 0) throw new IllegalArgumentException("非法ID");
    return Instant.ofEpochMilli(EPOCH + (id >>> 22));
  }
}
