package com.b2bplatform.compat.receipts;
import com.b2bplatform.compat.catalog.DocumentCreated;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
@Component
public class ProbeListener {
    private final JdbcTemplate jdbc;
    private final AtomicBoolean failNext = new AtomicBoolean();
    public ProbeListener(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void failNext() { failNext.set(true); }
    @ApplicationModuleListener
    public void created(DocumentCreated event) {
        if (failNext.compareAndSet(true, false)) throw new IllegalStateException("intentional listener failure");
        jdbc.update("INSERT IGNORE INTO probe_receipt(id, site_id) VALUES (?, ?)", event.id(), event.siteId());
    }
}
