package com.b2bweb.core.support.log;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class LogSafe {
  private LogSafe() {}

  public static String hash(String value) {
    if (value == null) return "null";
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)))
          .substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** A final safety net, never a reason to log request bodies or credentials. */
  public static String mask(String value) {
    if (value == null) return "";
    return value
        .replaceAll("(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9.-]+\\.[a-z]{2,}", "[email]")
        .replaceAll(
            "(?i)(bearer\\s+|(?:password|secret|token|api[_-]?key)[\"']?\\s*[:=]\\s*[\"']?)[^\\s,;\"']+",
            "$1[redacted]")
        .replaceAll("(?i)\\b(?:AKIA[A-Z0-9]{16}|sk-[A-Za-z0-9_-]{16,})\\b", "[key]");
  }
}
