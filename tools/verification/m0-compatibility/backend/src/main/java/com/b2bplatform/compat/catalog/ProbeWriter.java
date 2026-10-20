package com.b2bplatform.compat.catalog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProbeWriter {
    private final ProbeMapper mapper;
    private final ApplicationEventPublisher events;
    public ProbeWriter(ProbeMapper mapper, ApplicationEventPublisher events) { this.mapper = mapper; this.events = events; }
    @Transactional
    public void create(long id, boolean rollback) {
        var row = new ProbeRow(); row.setId(id); row.setSiteId(1L); row.setTitle("probe");
        mapper.insert(row);
        events.publishEvent(new DocumentCreated(id, 1));
        if (rollback) throw new IllegalStateException("intentional rollback");
    }
}
