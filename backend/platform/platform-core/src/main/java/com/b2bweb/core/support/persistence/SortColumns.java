package com.b2bweb.core.support.persistence;

import java.util.*;

/** Resolves only trusted compile-time columns; user fields can never become SQL. */
public final class SortColumns {
  private final Map<String, String> columns;

  public SortColumns(Map<String, String> columns) {
    if (columns.values().stream()
        .anyMatch(c -> !c.matches("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)?")))
      throw new IllegalArgumentException("排序列名无效");
    this.columns = Map.copyOf(columns);
  }

  public String column(String field) {
    String value = columns.get(field);
    if (value == null) throw new IllegalArgumentException("排序字段不允许");
    return value;
  }
}
