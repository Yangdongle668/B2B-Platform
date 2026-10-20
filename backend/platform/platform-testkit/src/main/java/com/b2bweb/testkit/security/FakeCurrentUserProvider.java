package com.b2bweb.testkit.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import java.util.*;

public final class FakeCurrentUserProvider implements CurrentUserProvider {
  private CurrentUserProvider delegate;

  public void delegate(CurrentUserProvider source) {
    delegate = source;
  }

  private final ThreadLocal<ActorRef> override = new ThreadLocal<>();

  public AutoCloseable asActor(ActorRef actor) {
    var before = override.get();
    override.set(actor);
    return () -> {
      if (before == null) override.remove();
      else override.set(before);
    };
  }

  final ThreadLocal<CurrentUser> current = new ThreadLocal<>();

  public void use(CurrentUser user) {
    current.set(user);
  }

  public void clear() {
    current.remove();
  }

  public Optional<CurrentUser> current() {
    return Optional.ofNullable(current.get())
        .or(() -> delegate == null ? Optional.empty() : delegate.current());
  }

  public CurrentUser require() {
    return current().orElseThrow(() -> new BusinessException(CoreErrors.NOT_AUTHENTICATED, "未登录"));
  }

  public ActorRef actor() {
    return Optional.ofNullable(override.get())
        .orElseGet(
            () ->
                current()
                    .map(CurrentUser::toActor)
                    .orElseGet(() -> delegate == null ? ActorRef.SYSTEM : delegate.actor()));
  }
}
