package com.b2bweb.testkit;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.security.*;
import com.b2bweb.testkit.contract.content.ContentPreviewContract;
import java.time.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ContentPreviewDoubleTest extends ContentPreviewContract {
  TestPlatform platform;

  protected ContentApi api() {
    return platform.contentApi();
  }

  protected void reset() {
    platform = TestPlatform.standard();
    platform
        .contentApi()
        .previewPart(
            "layout",
            node -> {
              if (!node.isObject() || !node.path("sections").isArray())
                throw new BusinessException(ContentErrors.STATE_INVALID, "布局Schema不合法");
            });
  }

  protected void site(long id) {
    platform.useSite(id);
  }

  protected void login() {
    platform.useUser(new CurrentUser(ISSUER, "编辑", "preview@example.test", false, Set.of(), true));
  }

  protected void anonymous() {
    platform.currentUserProvider().clear();
  }

  protected void issuer(boolean active, boolean member) {
    platform
        .userDirectory()
        .seed(
            new UserSummary(ISSUER, "编辑", "preview@example.test", active),
            false,
            member ? Set.of(1L) : Set.of());
  }

  protected void editGrant(boolean enabled, Set<String> locales) {
    if (enabled)
      platform
          .permissionChecker()
          .grant(
              ISSUER,
              Permissions.forContent("SAMPLE", Permissions.ContentAction.EDIT),
              DataScope.ALL,
              LocaleScope.of(locales));
    else
      platform
          .permissionChecker()
          .revoke(ISSUER, Permissions.forContent("SAMPLE", Permissions.ContentAction.EDIT));
  }

  protected void advance(Duration duration) {
    platform.clock().advance(duration);
  }

  protected Instant now() {
    return platform.clock().instant();
  }

  protected JsonNode layout(String marker) {
    var value = JsonMapper.builder().build().createObjectNode();
    value.putArray("sections").addObject().put("id", marker);
    return value;
  }

  protected void draftLayout(long localization, JsonNode layout) {
    platform.contentApi().putDraftPart(localization, "layout", layout);
  }

  protected long eventCount() {
    return platform.events().all().size();
  }
}
