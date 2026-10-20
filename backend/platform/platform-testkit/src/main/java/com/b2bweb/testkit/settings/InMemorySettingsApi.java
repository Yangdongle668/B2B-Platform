package com.b2bweb.testkit.settings;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.security.SecretCipher;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import com.networknt.schema.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

public final class InMemorySettingsApi implements SettingsApi {
  private record Definition(String owner, JsonNode schema, Schema validator) {}

  private record Saved(JsonNode value, int version, Instant at) {}

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final SiteApi api;
  private final SecretCipher cipher;
  private final Clock clock;
  private final EventMetaFactory meta;
  private final Consumer<DomainEvent> events;
  private final Map<String, Definition> definitions = new TreeMap<>();
  private final JsonMapper json = JsonMapper.builder().build();

  public InMemorySettingsApi(
      MemoryStore store,
      SiteContextProvider sites,
      SiteApi api,
      SecretCipher cipher,
      Clock clock,
      EventMetaFactory meta,
      Consumer<DomainEvent> events) {
    this.store = store;
    this.sites = sites;
    this.api = api;
    this.cipher = cipher;
    this.clock = clock;
    this.meta = meta;
    this.events = events;
    for (String owner : List.of("core", "content", "form", "inquiry", "translation")) {
      String path = "/META-INF/b2bweb/testkit/settings/" + owner + "/settings.schema.json";
      try (var input = getClass().getResourceAsStream(path)) {
        if (input == null) throw new IllegalStateException("缺少测试设置Schema: " + path);
        declare(owner, json.readTree(input));
      } catch (java.io.IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public void declare(String owner, JsonNode root) {
    for (var property : root.path("properties").properties()) {
      var schema = property.getValue();
      var validator =
          SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
      if (!schema.has("default") || !validator.validate(schema.path("default")).isEmpty())
        throw new IllegalArgumentException("设置默认值不符合Schema");
      if (definitions.putIfAbsent(
              owner + "." + property.getKey(), new Definition(owner, schema.deepCopy(), validator))
          != null) throw new IllegalArgumentException("设置重复");
    }
  }

  private Definition definition(String key) {
    var found = definitions.get(key);
    if (found == null) throw new BusinessException(CoreErrors.SETTING_NOT_DECLARED, "设置键未声明");
    return found;
  }

  private String key(long site, String key) {
    return "setting:" + site + ":" + key;
  }

  private JsonNode read(long site, String key) {
    var d = definition(key);
    return store.atomic(
        rows -> {
          var saved = (Saved) rows.get(key(site, key));
          return (saved == null ? d.schema().path("default") : saved.value()).deepCopy();
        });
  }

  public <T> T get(String key, Class<T> type) {
    return get(sites.requireSiteId(), key, type);
  }

  public JsonNode getJson(String key) {
    return read(sites.requireSiteId(), key);
  }

  public <T> T get(long site, String key, Class<T> type) {
    return json.treeToValue(read(site, key), type);
  }

  public <T> T getSystem(String key, Class<T> type) {
    return get(0, key, type);
  }

  public SettingMeta meta(String key) {
    var d = definition(key);
    return store.atomic(
        rows -> {
          var saved = (Saved) rows.get(key(sites.requireSiteId(), key));
          return saved == null
              ? new SettingMeta(key, d.owner(), true, 0, null)
              : new SettingMeta(key, d.owner(), false, saved.version(), saved.at());
        });
  }

  public int put(String key, JsonNode input, Integer expected) {
    return write(sites.requireSiteId(), key, input, expected);
  }

  public int putSystem(String key, JsonNode input, Integer expected) {
    return write(0, key, input, expected);
  }

  private void writable(long site) {
    if (site != 0
        && api.find(site)
                .orElseThrow(() -> new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在"))
                .status()
            == SiteStatus.ARCHIVED) throw new ForbiddenException(CoreErrors.FORBIDDEN, "归档站点只读");
  }

  private int write(long site, String key, JsonNode input, Integer expected) {
    var definition = definition(key);
    if (!definition.validator().validate(input).isEmpty())
      throw new ValidationException(CoreErrors.SETTING_INVALID, "设置不符合Schema", List.of());
    writable(site);
    return store.atomic(
        rows -> {
          var old = (Saved) rows.get(key(site, key));
          int version = old == null ? 0 : old.version();
          if (expected != null && expected != version)
            throw new VersionConflictException(
                CoreErrors.SETTING_VERSION_CONFLICT, "设置已被修改", version);
          var value =
              encrypted(
                  definition.schema(),
                  input,
                  old == null ? definition.schema().path("default") : old.value());
          rows.put(key(site, key), new Saved(value, version + 1, clock.instant()));
          if (site != 0) changed(key);
          return version + 1;
        });
  }

  private JsonNode encrypted(JsonNode schema, JsonNode value, JsonNode before) {
    if (schema.path("x-secret").asBoolean() && !value.isNull()) {
      String text = value.asString();
      if (text.isBlank() || text.startsWith("****")) return before.deepCopy();
      if (text.startsWith("v1:"))
        throw new ValidationException(CoreErrors.SETTING_INVALID, "不得提交密文信封", List.of());
      return json.getNodeFactory().stringNode(cipher.encrypt(Secret.of(text)));
    }
    if (value.isObject()) {
      var copy = (ObjectNode) value.deepCopy();
      schema
          .path("properties")
          .properties()
          .forEach(
              p -> {
                if (copy.has(p.getKey()))
                  copy.set(
                      p.getKey(),
                      encrypted(p.getValue(), copy.path(p.getKey()), before.path(p.getKey())));
              });
      return copy;
    }
    if (value.isArray()) {
      var copy = json.createArrayNode();
      for (int i = 0; i < value.size(); i++)
        copy.add(encrypted(schema.path("items"), value.get(i), before.path(i)));
      return copy;
    }
    return value.deepCopy();
  }

  private void changed(String key) {
    events.accept(
        new SiteSettingsChangedEvent(
            meta.next(), Set.of(SiteChangeScope.SETTINGS), Set.of(key), List.of(), Set.of()));
  }

  public void reset(String key) {
    definition(key);
    long site = sites.requireSiteId();
    writable(site);
    store.atomic(
        rows -> {
          rows.remove(key(site, key));
          changed(key);
          return null;
        });
  }
}
