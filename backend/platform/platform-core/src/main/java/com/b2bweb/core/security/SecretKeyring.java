package com.b2bweb.core.security;

import java.util.*;

public record SecretKeyring(String current, Map<String, byte[]> keys) {
  public static SecretKeyring parse(String encoded) {
    Map<String, byte[]> keys = new LinkedHashMap<>();
    for (String item : encoded.split(",", -1)) {
      String[] pair = item.split(":", 2);
      if (pair.length != 2 || !pair[0].matches("[A-Za-z0-9_-]{1,32}"))
        throw new IllegalArgumentException("密钥列表格式非法");
      byte[] value = Base64.getDecoder().decode(pair[1]);
      if (value.length != 32 || keys.putIfAbsent(pair[0], value) != null)
        throw new IllegalArgumentException("密钥长度或编号非法");
    }
    return new SecretKeyring(keys.keySet().iterator().next(), Collections.unmodifiableMap(keys));
  }

  public AesGcmSecrets cipher() {
    return new AesGcmSecrets(current, keys);
  }

  @Override
  public String toString() {
    return "SecretKeyring[****]";
  }
}
