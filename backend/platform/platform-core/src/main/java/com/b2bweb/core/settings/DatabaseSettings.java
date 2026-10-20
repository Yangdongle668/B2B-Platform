package com.b2bweb.core.settings;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

/**
 * Exclusive creation + current read gives optimistic versions even after an earlier RR snapshot.
 */
public final class DatabaseSettings implements SettingsApi {
  private final JdbcTemplate jdbc;
  private final SettingsSchemas schemas;
  private final SiteContextProvider sites;
  private final CurrentUserProvider users;
  private final IdGenerator ids;
  private final SecretCipher cipher;
  private final TransactionTemplate tx;
  private final EventMetaFactory meta;
  private final ApplicationEventPublisher events;
  private final AuditApi audit;
  private final ProjectionChangeApi projections;
  private final SiteApi siteApi;
  private final JsonMapper json = JsonMapper.builder().build();

  public DatabaseSettings(
      JdbcTemplate jdbc,
      SettingsSchemas schemas,
      SiteContextProvider sites,
      CurrentUserProvider users,
      IdGenerator ids,
      SecretCipher cipher,
      PlatformTransactionManager manager,
      EventMetaFactory meta,
      ApplicationEventPublisher events,
      AuditApi audit,
      ProjectionChangeApi projections,
      SiteApi siteApi) {
    this.jdbc = jdbc;
    this.schemas = schemas;
    this.sites = sites;
    this.users = users;
    this.ids = ids;
    this.cipher = cipher;
    tx = new TransactionTemplate(manager);
    this.meta = meta;
    this.events = events;
    this.audit = audit;
    this.projections = projections;
    this.siteApi = siteApi;
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

  private JsonNode read(long site, String key) {
    var definition = schemas.require(key);
    var values =
        jdbc.queryForList(
            "SELECT setting_value FROM sys_setting WHERE site_id=? AND setting_key=?",
            String.class,
            site,
            key);
    return values.isEmpty()
        ? definition.schema().path("default").deepCopy()
        : json.readTree(values.getFirst());
  }

  public SettingMeta meta(String key) {
    var definition = schemas.require(key);
    var rows =
        jdbc.query(
            "SELECT version,updated_at FROM sys_setting WHERE site_id=? AND setting_key=?",
            (r, n) ->
                new SettingMeta(
                    key, definition.owner(), false, r.getInt(1), r.getTimestamp(2).toInstant()),
            sites.requireSiteId(),
            key);
    return rows.isEmpty()
        ? new SettingMeta(key, definition.owner(), true, 0, null)
        : rows.getFirst();
  }

  public int put(String key, JsonNode value, Integer expected) {
    return write(sites.requireSiteId(), key, value, expected);
  }

  public int putSystem(String key, JsonNode value, Integer expected) {
    return write(0, key, value, expected);
  }

  private void assertWritable(long site) {
    var status = jdbc.queryForList("SELECT status FROM sys_site WHERE id=?", String.class, site);
    if (status.isEmpty()) throw new NotFoundException(CoreErrors.SITE_NOT_FOUND, "站点不存在");
    if (status.getFirst().equals("ARCHIVED"))
      throw new ForbiddenException(CoreErrors.FORBIDDEN, "归档站点只读");
  }

  private int write(long site, String key, JsonNode input, Integer expected) {
    var definition = schemas.require(key);
    schemas.validate(key, input);
    if (site != 0) assertWritable(site);
    return tx.execute(
        status -> {
          // Empty/default rows are inside this same transaction; any validation/conflict rolls them
          // back.
          jdbc.update(
              "INSERT INTO sys_setting(id,site_id,module_id,setting_key,setting_value,version)"
                  + " VALUES(?,?,?,?,CAST(? AS JSON),0) ON DUPLICATE KEY UPDATE id=id",
              ids.nextId(),
              site,
              definition.owner(),
              key,
              definition.schema().path("default").toString());
          var old =
              jdbc.queryForMap(
                  "SELECT setting_value,version FROM sys_setting WHERE site_id=? AND setting_key=?"
                      + " FOR UPDATE",
                  site,
                  key);
          int version = ((Number) old.get("version")).intValue();
          if (expected != null && expected != version)
            throw new VersionConflictException(
                CoreErrors.SETTING_VERSION_CONFLICT, "设置已被修改", version);
          JsonNode encrypted =
              encrypt(definition.schema(), input, json.readTree((String) old.get("setting_value")));
          int next = Math.addExact(version, 1);
          jdbc.update(
              "UPDATE sys_setting SET setting_value=CAST(? AS"
                  + " JSON),encrypted=?,version=?,updated_by=? WHERE site_id=? AND setting_key=?",
              encrypted.toString(),
              hasSecret(definition.schema()),
              next,
              users.actor().userId(),
              site,
              key);
          audit.record(
              new AuditEntry(
                  "core",
                  "setting.update",
                  "SETTING",
                  key,
                  "修改设置",
                  Map.of(),
                  AuditResult.SUCCESS,
                  true,
                  site == 0));
          if (site != 0) {
            capture();
            events.publishEvent(
                new SiteSettingsChangedEvent(
                    meta.next(),
                    Set.of(SiteChangeScope.SETTINGS),
                    Set.of(key),
                    List.of(),
                    Set.of()));
          }
          return next;
        });
  }

  private boolean hasSecret(JsonNode schema) {
    if (schema.path("x-secret").asBoolean(false)) return true;
    for (var p : schema.path("properties").properties()) if (hasSecret(p.getValue())) return true;
    return schema.has("items") && hasSecret(schema.path("items"));
  }

  private JsonNode encrypt(JsonNode schema, JsonNode value, JsonNode previous) {
    if (schema.path("x-secret").asBoolean(false) && !value.isNull()) {
      String text = value.asString();
      if (text.isBlank() || text.startsWith("****")) {
        if (previous.isNull() || previous.isMissingNode()) return json.nullNode();
        return previous.deepCopy();
      }
      if (text.startsWith("v1:"))
        throw new ValidationException(CoreErrors.SETTING_INVALID, "不得提交密文信封", List.of());
      return json.getNodeFactory().stringNode(cipher.encrypt(Secret.of(text)));
    }
    if (value.isObject()) {
      ObjectNode copy = (ObjectNode) value.deepCopy();
      for (var p : schema.path("properties").properties())
        if (copy.has(p.getKey()))
          copy.set(
              p.getKey(), encrypt(p.getValue(), copy.path(p.getKey()), previous.path(p.getKey())));
      return copy;
    }
    if (value.isArray()) {
      ArrayNode copy = json.createArrayNode();
      for (int i = 0; i < value.size(); i++)
        copy.add(encrypt(schema.path("items"), value.get(i), previous.path(i)));
      return copy;
    }
    return value.deepCopy();
  }

  private void capture() {
    projections.searchChanged(
        siteApi.locales().stream()
            .map(SiteLocaleInfo::locale)
            .collect(java.util.stream.Collectors.toSet()),
        Set.of(),
        true);
  }

  public void reset(String key) {
    schemas.require(key);
    long site = sites.requireSiteId();
    assertWritable(site);
    tx.executeWithoutResult(
        status -> {
          jdbc.update("DELETE FROM sys_setting WHERE site_id=? AND setting_key=?", site, key);
          audit.record(AuditEntry.success("core", "setting.reset", "SETTING", key, "恢复默认设置"));
          capture();
          events.publishEvent(
              new SiteSettingsChangedEvent(
                  meta.next(), Set.of(SiteChangeScope.SETTINGS), Set.of(key), List.of(), Set.of()));
        });
  }
}
