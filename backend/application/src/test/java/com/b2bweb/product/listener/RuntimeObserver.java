package com.b2bweb.product.listener;

import com.b2bweb.api.event.ModuleEnabledEvent;
import com.b2bweb.api.security.CurrentUserProvider;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.modulith.events.ApplicationModuleListener;

public class RuntimeObserver {
  public final ConcurrentMap<String, String> received = new ConcurrentHashMap<>();
  public final AtomicBoolean failNext = new AtomicBoolean();
  private final SiteContextProvider sites;
  private final CurrentUserProvider users;

  public RuntimeObserver(SiteContextProvider sites, CurrentUserProvider users) {
    this.sites = sites;
    this.users = users;
  }

  public void failNext() {
    failNext.set(true);
  }

  public String received(String id) {
    return received.get(id);
  }

  @ApplicationModuleListener
  public void onEnabled(ModuleEnabledEvent event) {
    if (failNext.compareAndSet(true, false)) throw new IllegalStateException("injected failure");
    received.put(
        event.eventId(),
        sites.requireSiteId()
            + ":"
            + users.actor().toEventActor()
            + ":"
            + sites.require().origin()
            + ":"
            + event.actor()
            + ":"
            + users.current().isEmpty());
  }
}
