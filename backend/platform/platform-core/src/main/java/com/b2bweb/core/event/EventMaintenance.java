package com.b2bweb.core.event;

import com.b2bweb.core.module.ModuleMigrations;
import com.b2bweb.core.support.event.JdbcProcessedEvents;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.modulith.events.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

/** A connection-owned MySQL lock serializes maintenance across application instances. */
@Component
public class EventMaintenance {
  private final DataSource dataSource;
  private final IncompleteEventPublications incomplete;
  private final CompletedEventPublications complete;
  private final com.b2bweb.core.support.event.ProcessedEvents processed;

  public EventMaintenance(
      DataSource source,
      IncompleteEventPublications incomplete,
      CompletedEventPublications complete,
      com.b2bweb.core.support.event.ProcessedEvents processed,
      ModuleMigrations migrated) {
    dataSource = source;
    this.incomplete = incomplete;
    this.complete = complete;
    this.processed = processed;
  }

  private void exclusive(String name, Runnable action) {
    try (var connection = dataSource.getConnection();
        var statement = connection.prepareStatement("SELECT GET_LOCK(?,0)")) {
      statement.setString(1, name);
      try (var result = statement.executeQuery()) {
        if (!result.next() || result.getInt(1) != 1) return;
      }
      try {
        action.run();
      } finally {
        try (var release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
          release.setString(1, name);
          release.executeQuery().close();
        }
      }
    } catch (java.sql.SQLException e) {
      throw new IllegalStateException("事件维护锁不可用", e);
    }
  }

  @Scheduled(cron = "0 * * * * *", zone = "UTC")
  public void retry() {
    exclusive(
        "core:event-republish",
        () -> incomplete.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5)));
  }

  @Scheduled(cron = "0 10 3 * * *", zone = "UTC")
  public void clean() {
    exclusive(
        "core:event-cleanup",
        () -> {
          complete.deletePublicationsOlderThan(Duration.ofDays(7));
          if (processed instanceof JdbcProcessedEvents jdbc) jdbc.cleanCompleted();
        });
  }
}
