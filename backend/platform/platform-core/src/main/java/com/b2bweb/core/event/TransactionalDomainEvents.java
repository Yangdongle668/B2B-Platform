package com.b2bweb.core.event;

import com.b2bweb.api.event.DomainEvent;
import org.springframework.context.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Ordinary module injections cannot silently lose domain events outside a source transaction. */
public final class TransactionalDomainEvents implements ApplicationEventPublisher {
  private final ApplicationContext delegate;

  public TransactionalDomainEvents(ApplicationContext delegate) {
    this.delegate = delegate;
  }

  public void publishEvent(Object event) {
    if (event instanceof DomainEvent
        && !TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("领域事件必须在源事务中发布");
    delegate.publishEvent(event);
  }
}
