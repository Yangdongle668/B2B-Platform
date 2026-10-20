package com.b2bweb.testkit.site;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

public final class FakeSiteContextProvider implements SiteContextProvider {
  private SiteContextProvider delegate;

  public void delegate(SiteContextProvider source) {
    delegate = source;
  }

  final ThreadLocal<SiteContext> current = new ThreadLocal<>();
  final Map<Long, SiteContext> sites = new HashMap<>();

  public FakeSiteContextProvider() {
    this(true);
  }

  public FakeSiteContextProvider(boolean defaults) {
    if (defaults)
      for (long id : List.of(1L, 2L))
        sites.put(
            id,
            new SiteContext(
                id, "s" + id, "zh-CN", "zh-CN", ZoneOffset.UTC, null, ContextOrigin.SYSTEM));
  }

  public void useContext(SiteContext context) {
    if (!sites.containsKey(context.siteId()))
      throw new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在");
    current.set(context);
  }

  public void add(SiteContext site) {
    sites.put(site.siteId(), site);
  }

  public void use(long id) {
    var site = sites.get(id);
    if (site == null) throw new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在");
    current.set(site);
  }

  public void clear() {
    current.remove();
  }

  public Optional<SiteContext> current() {
    return Optional.ofNullable(current.get())
        .or(() -> delegate == null ? Optional.empty() : delegate.current());
  }

  public SiteContext require() {
    return current().orElseThrow(NoSiteContextException::new);
  }

  public long requireSiteId() {
    return require().siteId();
  }

  public <T> T callAs(long id, Supplier<T> action) {
    var before = current.get();
    use(id);
    try {
      return action.get();
    } finally {
      if (before == null) clear();
      else current.set(before);
    }
  }

  public void runAs(long id, Runnable action) {
    callAs(
        id,
        () -> {
          action.run();
          return null;
        });
  }
}
