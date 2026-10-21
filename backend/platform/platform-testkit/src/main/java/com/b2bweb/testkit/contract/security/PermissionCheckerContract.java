package com.b2bweb.testkit.contract.security;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.ForbiddenException;
import com.b2bweb.api.security.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Reusable authorization behavior; fixtures mutate persisted state without refreshing the user
 * snapshot.
 */
public abstract class PermissionCheckerContract {
  protected static final long USER = 77101;
  protected static final String PERMISSION = "system:m0-contract:view";

  protected abstract PermissionChecker api();

  protected abstract void account(boolean active, boolean administrator);

  protected abstract void current(CurrentUser user);

  protected abstract void site(long id);

  protected abstract void grant(DataScope data, LocaleScope locales);

  protected abstract void revoke();

  protected void login(boolean administrator) {
    current(
        new CurrentUser(USER, "契约用户", "permissions@example.test", administrator, Set.of(), true));
  }

  @Test
  public void contractAnonymousHasNoCurrentUserPermission() {
    account(true, true);
    current(null);
    assertThat(api().has(PERMISSION)).isFalse();
    assertThat(api().hasAny(PERMISSION, "system:missing:view")).isFalse();
    assertThat(api().dataScope(PERMISSION)).isEmpty();
    assertThat(api().canAccessLocale(PERMISSION, "zh-CN")).isFalse();
    assertThatThrownBy(() -> api().check(PERMISSION)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void contractRoleGrantsAreSiteScoped() {
    account(true, false);
    login(false);
    site(771);
    grant(DataScope.OWN, LocaleScope.of(Set.of("zh-CN")));
    assertThat(api().has(PERMISSION)).isTrue();
    site(772);
    assertThat(api().has(PERMISSION)).isFalse();
    assertThat(api().userHas(USER, PERMISSION)).isFalse();
    site(771);
    assertThat(api().has(PERMISSION)).isTrue();
  }

  @Test
  public void contractDataAndLocaleScopesDoNotExpand() {
    account(true, false);
    login(false);
    site(771);
    grant(DataScope.OWN, LocaleScope.of(Set.of("zh-CN")));
    assertThat(api().dataScope(PERMISSION)).contains(DataScope.OWN);
    assertThat(api().canAccessLocale(PERMISSION, "zh-CN")).isTrue();
    assertThat(api().canAccessLocale(PERMISSION, "en")).isFalse();
    assertThat(api().hasAny("system:missing:view", PERMISSION)).isTrue();
  }

  @Test
  public void contractExplicitUserLookupDoesNotRequireCurrentLogin() {
    account(true, false);
    current(null);
    site(771);
    grant(DataScope.ALL, LocaleScope.all());
    assertThat(api().userHas(USER, PERMISSION)).isTrue();
    assertThat(api().userHas(USER + 1, PERMISSION)).isFalse();
    assertThat(api().has(PERMISSION)).isFalse();
  }

  @Test
  public void contractDemotedAdministratorCannotTrustOldSnapshot() {
    account(true, true);
    login(true);
    site(771);
    assertThat(api().has(PERMISSION)).isTrue();
    account(true, false);
    assertThat(api().has(PERMISSION)).isFalse();
    assertThat(api().userHas(USER, PERMISSION)).isFalse();
    assertThat(api().dataScope(PERMISSION)).isEmpty();
    assertThat(api().canAccessLocale(PERMISSION, "zh-CN")).isFalse();
  }

  @Test
  public void contractDisabledAccountCannotUseRoleOrAdministratorGrants() {
    account(true, true);
    login(true);
    site(771);
    grant(DataScope.ALL, LocaleScope.all());
    account(false, true);
    assertThat(api().has(PERMISSION)).isFalse();
    assertThat(api().userHas(USER, PERMISSION)).isFalse();
    assertThat(api().dataScope(PERMISSION)).isEmpty();
    assertThat(api().canAccessLocale(PERMISSION, "zh-CN")).isFalse();
  }

  @Test
  public void contractRevokedRoleGrantTakesEffectImmediately() {
    account(true, false);
    login(false);
    site(771);
    grant(DataScope.GROUP, LocaleScope.all());
    assertThat(api().has(PERMISSION)).isTrue();
    assertThat(api().dataScope(PERMISSION)).contains(DataScope.GROUP);
    assertThat(api().canAccessLocale(PERMISSION, "en")).isTrue();
    revoke();
    assertThat(api().has(PERMISSION)).isFalse();
    assertThat(api().userHas(USER, PERMISSION)).isFalse();
    assertThatThrownBy(() -> api().check(PERMISSION)).isInstanceOf(ForbiddenException.class);
  }
}
