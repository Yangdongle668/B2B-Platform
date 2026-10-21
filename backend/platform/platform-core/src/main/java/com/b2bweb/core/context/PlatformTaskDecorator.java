package com.b2bweb.core.context;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Capture at submission, restore every worker-owned context after completion or failure. */
@Component
public final class PlatformTaskDecorator implements TaskDecorator {
  private final SiteContexts sites;
  private final UserContexts users;

  public PlatformTaskDecorator(SiteContexts sites, UserContexts users) {
    this.sites = sites;
    this.users = users;
  }

  public Runnable decorate(Runnable task) {
    var site = sites.current().orElse(null);
    var user = users.current().orElse(null);
    var actor = users.actor();
    var reauth = users.lastReauthentication().orElse(java.time.Instant.EPOCH);
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var mdc = MDC.getCopyOfContextMap();
    return () -> {
      var previous = SecurityContextHolder.getContext();
      var previousMdc = MDC.getCopyOfContextMap();
      var security = SecurityContextHolder.createEmptyContext();
      security.setAuthentication(authentication);
      SecurityContextHolder.setContext(security);
      if (mdc == null) MDC.clear();
      else MDC.setContextMap(mdc);
      try (var siteScope = site == null ? sites.withoutSite() : sites.open(site);
          var userScope = user == null ? users.withoutUser() : users.open(user);
          var actorScope = users.asActor(actor);
          var verification = users.withReauthentication(reauth)) {
        task.run();
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException("异步上下文恢复失败", e);
      } finally {
        if (previous.getAuthentication() == null) SecurityContextHolder.clearContext();
        else SecurityContextHolder.setContext(previous);
        if (previousMdc == null) MDC.clear();
        else MDC.setContextMap(previousMdc);
      }
    };
  }
}
