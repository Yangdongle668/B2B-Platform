package com.b2bweb.core.context;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import java.util.*;

public final class UserContexts implements CurrentUserProvider {
  private final ThreadLocal<CurrentUser> local = new ThreadLocal<>();
  private final ThreadLocal<java.time.Instant> lastReauthentication = new ThreadLocal<>();
  private final ThreadLocal<ActorRef> actorOverride = new ThreadLocal<>();

  public Optional<CurrentUser> current() {
    return Optional.ofNullable(local.get());
  }

  public CurrentUser require() {
    return current().orElseThrow(() -> new BusinessException(CoreErrors.NOT_AUTHENTICATED, "请先登录"));
  }

  public ActorRef actor() {
    return Optional.ofNullable(actorOverride.get())
        .orElseGet(() -> current().map(CurrentUser::toActor).orElse(ActorRef.SYSTEM));
  }

  public AutoCloseable asActor(ActorRef actor) {
    var before = actorOverride.get();
    actorOverride.set(actor);
    return () -> {
      if (before == null) actorOverride.remove();
      else actorOverride.set(before);
    };
  }

  public Optional<java.time.Instant> lastReauthentication() {
    return Optional.ofNullable(lastReauthentication.get());
  }

  public AutoCloseable withReauthentication(java.time.Instant instant) {
    var before = lastReauthentication.get();
    lastReauthentication.set(instant);
    return () -> {
      if (before == null) lastReauthentication.remove();
      else lastReauthentication.set(before);
    };
  }

  public AutoCloseable withoutUser() {
    var previous = local.get();
    local.remove();
    var mdc = com.b2bweb.core.support.log.ContextMdc.bind("userId", null);
    return () -> {
      mdc.close();
      if (previous == null) local.remove();
      else local.set(previous);
    };
  }

  public AutoCloseable open(CurrentUser user) {
    if (user == null) return withoutUser();
    var previous = local.get();
    local.set(user);
    var mdc = com.b2bweb.core.support.log.ContextMdc.bind("userId", Long.toString(user.userId()));
    return () -> {
      mdc.close();
      if (previous == null) local.remove();
      else local.set(previous);
    };
  }
}
