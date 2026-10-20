package com.b2bweb.testkit.event;

import com.b2bweb.api.event.DomainEvent;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.transaction.support.*;

public final class RecordedEvents {
  private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

  public void publish(DomainEvent event) {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            public void afterCommit() {
              events.add(event);
            }
          });
    else events.add(event);
  }

  public List<DomainEvent> all() {
    return List.copyOf(events);
  }

  public <T extends DomainEvent> List<T> ofType(Class<T> type) {
    return events.stream().filter(type::isInstance).map(type::cast).toList();
  }

  public void clear() {
    events.clear();
  }
}
