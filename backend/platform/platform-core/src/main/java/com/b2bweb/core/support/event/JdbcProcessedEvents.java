package com.b2bweb.core.support.event;

import com.b2bweb.api.event.DomainEvent;
import java.sql.Timestamp;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcProcessedEvents implements ProcessedEvents {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public JdbcProcessedEvents(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public boolean firstTime(String consumer, DomainEvent event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("事件幂等标记必须加入监听事务");
    if (consumer.length() > 96 || !consumer.matches("[a-z][a-z0-9]*\\.[a-z][a-z0-9_]*"))
      throw new IllegalArgumentException("消费者标识非法");
    return jdbc.update(
            "INSERT IGNORE INTO sys_processed_event(consumer,event_id,site_id,processed_at)"
                + " VALUES(?,?,?,?)",
            consumer,
            event.eventId(),
            event.siteId(),
            Timestamp.from(clock.instant()))
        == 1;
  }

  public int cleanCompleted() {
    return jdbc.update(
        "DELETE FROM sys_processed_event WHERE processed_at<? AND event_id NOT IN(SELECT"
            + " JSON_UNQUOTE(JSON_EXTRACT(SERIALIZED_EVENT,'$.meta.eventId')) FROM"
            + " EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL AND"
            + " JSON_EXTRACT(SERIALIZED_EVENT,'$.meta.eventId') IS NOT NULL) LIMIT 500",
        Timestamp.from(clock.instant().minus(Duration.ofDays(30))));
  }
}
