package com.b2bweb.core.context;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import java.util.*;
import java.util.function.*;

/** A scope must be closed on every request/worker exit; never inherited by pooled threads. */
public final class SiteContexts implements SiteContextProvider {
  private final ThreadLocal<SiteContext> local = new ThreadLocal<>();
  private final LongFunction<SiteContext> resolver;

  public SiteContexts(LongFunction<SiteContext> resolver) {
    this.resolver = resolver;
  }

  public Optional<SiteContext> current() {
    return Optional.ofNullable(local.get());
  }

  public SiteContext require() {
    return current().orElseThrow(NoSiteContextException::new);
  }

  public long requireSiteId() {
    return require().siteId();
  }

  public Scope open(SiteContext context) {
    var previous = local.get();
    local.set(Objects.requireNonNull(context));
    return new Scope(previous, Thread.currentThread());
  }

  public final class Scope implements AutoCloseable {
    private final SiteContext previous;
    private final Thread owner;
    private boolean closed;

    Scope(SiteContext previous, Thread owner) {
      this.previous = previous;
      this.owner = owner;
    }

    public void close() {
      if (Thread.currentThread() != owner) throw new IllegalStateException("上下文必须在原线程结束");
      if (!closed) {
        closed = true;
        if (previous == null) local.remove();
        else local.set(previous);
      }
    }
  }

  public <T> T callAs(long siteId, Supplier<T> action) {
    try (var ignored = open(resolver.apply(siteId))) {
      return action.get();
    }
  }

  public void runAs(long siteId, Runnable action) {
    callAs(
        siteId,
        () -> {
          action.run();
          return null;
        });
  }
}
