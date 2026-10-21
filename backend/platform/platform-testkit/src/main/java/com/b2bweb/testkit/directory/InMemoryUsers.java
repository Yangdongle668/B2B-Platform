package com.b2bweb.testkit.directory;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.testkit.common.MemoryStore;
import com.b2bweb.testkit.security.*;
import java.util.*;

public final class InMemoryUsers implements UserDirectory {
  private UserDirectory delegate;

  public void delegate(UserDirectory source) {
    delegate = Objects.requireNonNull(source);
  }

  private record Entry(UserSummary user, boolean admin, Set<Long> sites) {}

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final FakeCurrentUserProvider current;
  private final PermissionChecker permissions;

  public InMemoryUsers(
      MemoryStore store,
      SiteContextProvider sites,
      FakeCurrentUserProvider current,
      PermissionChecker permissions) {
    this.store = store;
    this.sites = sites;
    this.current = current;
    this.permissions = permissions;
  }

  public void seed(UserSummary user, boolean admin, Set<Long> membership) {
    if (delegate != null) throw new IllegalStateException("真实用户目录桥接时不能写入内存用户夹具");
    if (permissions instanceof FakePermissionChecker fake) {
      fake.registerAdmin(user.userId(), admin && user.active());
      fake.registerActive(user.userId(), user.active());
    }
    store.atomic(
        rows -> {
          rows.put(
              "directory-user:" + user.userId(), new Entry(user, admin, Set.copyOf(membership)));
          return null;
        });
  }

  public Optional<UserSummary> find(long id) {
    if (delegate != null) return delegate.find(id);
    return store.atomic(
        rows -> Optional.ofNullable((Entry) rows.get("directory-user:" + id)).map(Entry::user));
  }

  public Map<Long, UserSummary> findAll(Collection<Long> ids) {
    if (delegate != null) return delegate.findAll(ids);
    Map<Long, UserSummary> result = new LinkedHashMap<>();
    for (long id : ids) find(id).ifPresent(user -> result.put(id, user));
    return Map.copyOf(result);
  }

  public boolean isActiveMember(long id) {
    if (delegate != null) return delegate.isActiveMember(id);
    return store.atomic(
        rows -> {
          var e = (Entry) rows.get("directory-user:" + id);
          return e != null
              && e.user().active()
              && (e.admin() || e.sites().contains(sites.requireSiteId()));
        });
  }

  public List<UserSummary> usersWithPermission(String permission, String locale) {
    if (delegate != null) return delegate.usersWithPermission(permission, locale);
    return store.atomic(
        rows -> {
          List<UserSummary> result = new ArrayList<>();
          var before = current.current().orElse(null);
          try {
            for (Object value : rows.values())
              if (value instanceof Entry e && isActiveMember(e.user().userId())) {
                current.use(
                    new CurrentUser(
                        e.user().userId(),
                        e.user().name(),
                        e.user().email(),
                        e.admin(),
                        Set.of(),
                        true));
                if (permissions.has(permission)
                    && (locale == null || permissions.canAccessLocale(permission, locale)))
                  result.add(e.user());
              }
          } finally {
            if (before == null) current.clear();
            else current.use(before);
          }
          result.sort(Comparator.comparingLong(UserSummary::userId));
          return List.copyOf(result);
        });
  }
}
