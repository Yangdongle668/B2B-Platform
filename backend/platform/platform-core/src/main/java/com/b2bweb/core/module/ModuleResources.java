package com.b2bweb.core.module;

import com.b2bweb.api.common.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.*;

/** Declarative resources are synchronized only after successful owner migration. */
final class ModuleResources {
  final JdbcTemplate jdbc;
  final IdGenerator ids;

  ModuleResources(javax.sql.DataSource source, IdGenerator ids) {
    jdbc = new JdbcTemplate(source);
    this.ids = ids;
  }

  void pending(String id, JsonNode descriptor) {
    jdbc.update(
        "INSERT INTO"
            + " sys_module(id,module_id,name,layer,disableable,table_prefix,api_prefix,status,target_version,descriptor,descriptor_hash)"
            + " VALUES(?,?,?,?,?,?,?,'INSTALLING',?,CAST(? AS JSON),?) ON DUPLICATE KEY UPDATE"
            + " target_version=IF(installed_version=VALUES(target_version),NULL,VALUES(target_version)),"
            + " status=CASE WHEN installed_version IS NULL THEN 'INSTALLING'"
            + " WHEN installed_version=VALUES(target_version) AND status='INSTALLED' THEN 'INSTALLED'"
            + " ELSE 'UPGRADING' END",
        ids.nextId(),
        id,
        descriptor.path("name").asString(),
        descriptor.path("layer").asString(),
        descriptor.path("disableable").asBoolean(),
        descriptor.path("tablePrefix").asString(null),
        descriptor.path("apiPrefix").asString(null),
        descriptor.path("version").asString(),
        descriptor.toString(),
        hash(descriptor));
  }

  void failed(String id) {
    jdbc.update(
        "UPDATE sys_module SET status='FAILED',last_error='模块迁移或资源同步失败' WHERE module_id=?", id);
  }

  void installed(String id, JsonNode descriptor) {
    jdbc.update(
        "UPDATE sys_module SET"
            + " name=?,layer=?,disableable=?,table_prefix=?,api_prefix=?,status='INSTALLED',installed_version=?,target_version=NULL,descriptor=CAST(?"
            + " AS JSON),descriptor_hash=?,last_error=NULL,missing_since=NULL,installed_at=COALESCE(installed_at,CURRENT_TIMESTAMP(3))"
            + " WHERE module_id=?",
        descriptor.path("name").asString(),
        descriptor.path("layer").asString(),
        descriptor.path("disableable").asBoolean(),
        descriptor.path("tablePrefix").asString(null),
        descriptor.path("apiPrefix").asString(null),
        descriptor.path("version").asString(),
        descriptor.toString(),
        hash(descriptor),
        id);
    jdbc.update("UPDATE sys_menu SET removed_at=CURRENT_TIMESTAMP(3) WHERE module_id=?", id);
    for (var menu : descriptor.path("menus"))
      jdbc.update(
          "INSERT INTO"
              + " sys_menu(id,menu_key,module_id,parent_key,name,route,permission_code,icon,sort_order)"
              + " VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE"
              + " parent_key=VALUES(parent_key),name=VALUES(name),route=VALUES(route),permission_code=VALUES(permission_code),icon=VALUES(icon),sort_order=VALUES(sort_order),removed_at=NULL",
          ids.nextId(),
          menu.path("key").asString(),
          id,
          menu.path("parent").asString(null),
          menu.path("name").asString(),
          menu.path("route").asString(null),
          menu.path("permission").asString(null),
          menu.path("icon").asString(null),
          menu.path("order").asInt(100));
    jdbc.update("UPDATE sys_permission SET removed_at=CURRENT_TIMESTAMP(3) WHERE module_id=?", id);
    for (var permission : descriptor.path("permissions"))
      jdbc.update(
          "INSERT INTO"
              + " sys_permission(id,code,module_id,name,is_sensitive,data_scopes,locale_scoped)"
              + " VALUES(?,?,?,?,?,CAST(? AS JSON),?) ON DUPLICATE KEY UPDATE"
              + " name=VALUES(name),is_sensitive=VALUES(is_sensitive),data_scopes=VALUES(data_scopes),locale_scoped=VALUES(locale_scoped),removed_at=NULL",
          ids.nextId(),
          permission.path("code").asString(),
          id,
          permission.path("name").asString(permission.path("code").asString()),
          permission.path("sensitive").asBoolean(false),
          permission.path("dataScopes").isMissingNode()
              ? null
              : permission.path("dataScopes").toString(),
          permission.path("localeScoped").asBoolean(false));
    jdbc.update(
        "UPDATE sys_role r SET mfa_required=EXISTS(SELECT 1 FROM sys_role_permission rp JOIN"
            + " sys_permission p ON p.id=rp.permission_id WHERE rp.role_id=r.id AND"
            + " p.is_sensitive=1 AND p.removed_at IS NULL) WHERE r.deleted_at IS NULL");
  }

  static String hash(JsonNode node) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(node.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
