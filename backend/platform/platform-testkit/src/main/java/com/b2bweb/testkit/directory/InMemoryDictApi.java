package com.b2bweb.testkit.directory;

import com.b2bweb.api.common.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemoryDictApi implements DictApi {
  private final MemoryStore store;

  public InMemoryDictApi(MemoryStore store) {
    this.store = store;
  }

  private String key(String type) {
    return "dict:" + type;
  }

  @SuppressWarnings("unchecked")
  private Map<String, DictItem> entries(Map<String, Object> rows, String type) {
    var found = (Map<String, DictItem>) rows.get(key(type));
    if (found == null) throw new NotFoundException(CoreErrors.DICT_TYPE_NOT_FOUND, "字典类型不存在");
    return new LinkedHashMap<>(found);
  }

  public List<DictItem> items(String type) {
    return allItems(type).stream().filter(DictItem::enabled).toList();
  }

  public List<DictItem> allItems(String type) {
    return store.atomic(
        rows ->
            entries(rows, type).values().stream()
                .sorted(Comparator.comparingInt(DictItem::sortOrder).thenComparing(DictItem::value))
                .toList());
  }

  public Optional<String> label(String type, String value) {
    return allItems(type).stream()
        .filter(i -> i.value().equals(value))
        .map(DictItem::label)
        .findFirst();
  }

  public boolean isValid(String type, String value) {
    return items(type).stream().anyMatch(i -> i.value().equals(value));
  }

  public void ensureBuiltin(DictTypeDefinition definition) {
    store.atomic(
        rows -> {
          Map<String, DictItem> existing =
              rows.containsKey(key(definition.typeCode()))
                  ? entries(rows, definition.typeCode())
                  : new LinkedHashMap<>();
          for (var item : definition.items()) {
            var old = existing.get(item.value());
            if (old != null && !old.builtin()) continue;
            existing.put(
                item.value(),
                new DictItem(
                    item.value(),
                    item.label(),
                    item.sortOrder(),
                    old == null || old.enabled(),
                    true,
                    null));
          }
          rows.put(key(definition.typeCode()), Map.copyOf(existing));
          return null;
        });
  }

  public void seed(String type, DictItem item) {
    store.atomic(
        rows -> {
          var values = entries(rows, type);
          values.put(item.value(), item);
          rows.put(key(type), Map.copyOf(values));
          return null;
        });
  }
}
