package com.b2bweb.testkit;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.contract.security.PermissionCheckerContract;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;

class PermissionDoubleTest extends PermissionCheckerContract {
  TestPlatform platform;

  @BeforeEach
  void setup() {
    platform = TestPlatform.standard();
    for (long id : List.of(771L, 772L))
      platform
          .siteApi()
          .addSite(
              new SiteInfo(
                  id,
                  "permissions-" + id,
                  "权限测试",
                  "industrial",
                  "zh-CN",
                  "zh-CN",
                  UrlStrategy.SUBDIRECTORY,
                  TrailingSlash.NEVER,
                  ZoneOffset.UTC,
                  SiteStatus.ACTIVE,
                  "example.test",
                  0));
    site(771);
  }

  protected PermissionChecker api() {
    return platform.permissionChecker();
  }

  protected void account(boolean active, boolean administrator) {
    platform
        .userDirectory()
        .seed(
            new UserSummary(USER, "契约用户", "permissions@example.test", active),
            administrator,
            Set.of(771L));
  }

  protected void current(CurrentUser user) {
    if (user == null) platform.currentUserProvider().clear();
    else platform.useUser(user);
  }

  protected void site(long id) {
    platform.useSite(id);
  }

  protected void grant(DataScope data, LocaleScope locales) {
    platform.permissionChecker().grant(USER, PERMISSION, data, locales);
  }

  protected void revoke() {
    platform.permissionChecker().revoke(USER, PERMISSION);
  }
}
