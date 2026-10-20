package com.b2bweb.core.support.event;

import com.b2bweb.api.event.DomainEvent;

public interface ProcessedEvents {
  boolean firstTime(String consumer, DomainEvent event);
}
