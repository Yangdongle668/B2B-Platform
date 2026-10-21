package com.b2bweb.core.support.web;

public final class ETags {
  private ETags() {}

  public static String of(int version) {
    if (version < 0) throw new IllegalArgumentException("版本不能为负");
    return "\"v" + version + "\"";
  }

  public static int parse(String value) {
    if (value == null || !value.matches("\"v(?:0|[1-9][0-9]*)\""))
      throw new IllegalArgumentException("If-Match格式无效");
    return Integer.parseInt(value.substring(2, value.length() - 1));
  }
}
