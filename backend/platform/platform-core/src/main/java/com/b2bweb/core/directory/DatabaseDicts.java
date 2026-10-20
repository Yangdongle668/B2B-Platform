package com.b2bweb.core.directory;

import com.b2bweb.api.common.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class DatabaseDicts implements DictApi {
  final JdbcTemplate jdbc;
  final IdGenerator ids;
  final TransactionTemplate tx;

  public DatabaseDicts(JdbcTemplate jdbc, IdGenerator ids, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.ids = ids;
    tx = new TransactionTemplate(manager);
  }

  long typeId(String code) {
    var found =
        jdbc.queryForList("SELECT id FROM sys_dict_type WHERE type_code=?", Long.class, code);
    if (found.isEmpty()) throw new NotFoundException(CoreErrors.DICT_TYPE_NOT_FOUND, "字典类型不存在");
    return found.getFirst();
  }

  public List<DictItem> allItems(String code) {
    return jdbc.query(
        "SELECT * FROM sys_dict_item WHERE dict_type_id=? ORDER BY sort_order,item_value",
        (r, n) ->
            new DictItem(
                r.getString("item_value"),
                r.getString("label"),
                r.getInt("sort_order"),
                r.getBoolean("enabled"),
                r.getBoolean("builtin"),
                r.getString("extra") == null
                    ? null
                    : JsonMapper.builder().build().readTree(r.getString("extra"))),
        typeId(code));
  }

  public List<DictItem> items(String code) {
    return allItems(code).stream().filter(DictItem::enabled).toList();
  }

  public Optional<String> label(String code, String value) {
    return allItems(code).stream()
        .filter(i -> i.value().equals(value))
        .map(DictItem::label)
        .findFirst();
  }

  public boolean isValid(String code, String value) {
    return items(code).stream().anyMatch(i -> i.value().equals(value));
  }

  public void ensureBuiltin(DictTypeDefinition definition) {
    tx.executeWithoutResult(
        status -> {
          jdbc.update(
              "INSERT INTO"
                  + " sys_dict_type(id,type_code,module_id,name,description,builtin,items_editable)"
                  + " VALUES(?,?,?,?,?,1,?) ON DUPLICATE KEY UPDATE"
                  + " name=VALUES(name),description=VALUES(description),items_editable=VALUES(items_editable)",
              ids.nextId(),
              definition.typeCode(),
              definition.moduleId(),
              definition.name(),
              definition.description(),
              definition.itemsEditable());
          long type = typeId(definition.typeCode());
          for (var item : definition.items())
            jdbc.update(
                "INSERT INTO sys_dict_item(id,dict_type_id,item_value,label,sort_order,builtin)"
                    + " VALUES(?,?,?,?,?,1) ON DUPLICATE KEY UPDATE"
                    + " label=IF(builtin=1,VALUES(label),label),sort_order=IF(builtin=1,VALUES(sort_order),sort_order)",
                ids.nextId(),
                type,
                item.value(),
                item.label(),
                item.sortOrder());
        });
  }
}
