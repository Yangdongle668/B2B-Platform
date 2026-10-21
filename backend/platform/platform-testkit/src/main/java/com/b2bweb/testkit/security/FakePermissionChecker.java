package com.b2bweb.testkit.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import java.util.*;

public final class FakePermissionChecker implements PermissionChecker {
  private final Map<Long, Boolean> activeUsers = new HashMap<>();

  public void registerActive(long user, boolean active) {
    activeUsers.put(user, active);
  }

  private final Set<Long> admins = new HashSet<>();

  public void registerAdmin(long user, boolean admin) {
    if (admin) admins.add(user);
    else admins.remove(user);
  }

  private boolean administrator(CurrentUser user) {
    return activeUsers.containsKey(user.userId())
        ? admins.contains(user.userId())
        : user.systemAdmin();
  }

  record Grant(DataScope data, LocaleScope locales) {}

  final Map<String, Grant> grants = new HashMap<>();
  final SiteContextProvider sites;
  final CurrentUserProvider users;

  public FakePermissionChecker(SiteContextProvider sites, CurrentUserProvider users) {
    this.sites = sites;
    this.users = users;
  }

  String key(long id, String permission) {
    return sites.requireSiteId() + ":" + id + ":" + permission;
  }

  public void grant(long id, String permission, DataScope scope, LocaleScope locales) {
    grants.put(key(id, permission), new Grant(scope, locales));
  }

  public void revoke(long id, String permission) {
    grants.remove(key(id, permission));
  }

  public boolean has(String permission) {
    return users
        .current()
        .map(
            u ->
                !Boolean.FALSE.equals(activeUsers.get(u.userId()))
                    && (administrator(u) || grants.containsKey(key(u.userId(), permission))))
        .orElse(false);
  }

  public boolean hasAny(String... permissions) {
    return Arrays.stream(permissions).anyMatch(this::has);
  }

  public void check(String permission) {
    if (!has(permission)) throw new ForbiddenException(CoreErrors.FORBIDDEN, "没有操作权限");
  }

  public Optional<DataScope> dataScope(String permission) {
    if (!has(permission)) return Optional.empty();
    return Optional.of(
        administrator(users.require())
            ? DataScope.ALL
            : grants.get(key(users.require().userId(), permission)).data());
  }

  public LocaleScope localeScope(String permission) {
    if (!has(permission)) return LocaleScope.of(Set.of());
    return administrator(users.require())
        ? LocaleScope.all()
        : grants.get(key(users.require().userId(), permission)).locales();
  }

  public boolean canAccessLocale(String permission, String locale) {
    return has(permission) && localeScope(permission).allows(locale);
  }

  public boolean userHas(long id, String permission) {
    return !Boolean.FALSE.equals(activeUsers.get(id))
        && (admins.contains(id)
            || users.current().filter(u -> u.userId() == id && administrator(u)).isPresent()
            || grants.containsKey(key(id, permission)));
  }
}
