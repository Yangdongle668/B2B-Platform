package com.b2bweb.core.event;

import com.b2bweb.api.event.DomainEvent;
import com.b2bweb.api.site.SiteApi;
import com.b2bweb.core.context.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;

/**
 * Runs inside async dispatch, restores source site/actor and honors disabled-module cleanup rules.
 */
@Aspect
@Order(-2147483548)
public class ListenerContexts {
  final SiteContexts sites;
  final UserContexts users;
  final SiteApi siteApi;

  public ListenerContexts(SiteContexts sites, UserContexts users, SiteApi siteApi) {
    this.sites = sites;
    this.users = users;
    this.siteApi = siteApi;
  }

  @Around("@annotation(org.springframework.modulith.events.ApplicationModuleListener)")
  public Object invoke(ProceedingJoinPoint call) throws Throwable {
    DomainEvent event = null;
    for (Object arg : call.getArgs())
      if (arg instanceof DomainEvent e) {
        event = e;
        break;
      }
    if (event == null) return call.proceed();
    var matcher =
        java.util.regex.Pattern.compile("com\\.b2bweb\\.([a-z][a-z0-9]*)\\.")
            .matcher(call.getTarget().getClass().getName());
    String module = matcher.find() ? matcher.group(1) : "core";
    if (event.siteId() != null
        && !event.deliverWhenModuleDisabled()
        && !siteApi.isModuleEnabled(event.siteId(), module)) return null;
    try (var user = users.withoutUser();
        var actor = users.asActor(com.b2bweb.api.common.ActorRef.SYSTEM)) {
      if (event.siteId() == null) {
        try (var site = sites.withoutSite()) {
          return call.proceed();
        }
      }
      final Throwable[] failure = {null};
      final Object[] result = {null};
      sites.runAs(
          event.siteId(),
          com.b2bweb.api.site.ContextOrigin.EVENT,
          null,
          () -> {
            try {
              result[0] = call.proceed();
            } catch (Throwable e) {
              failure[0] = e;
            }
          });
      if (failure[0] != null) throw failure[0];
      return result[0];
    }
  }
}
