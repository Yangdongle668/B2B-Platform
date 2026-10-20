package com.b2bweb.core.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.SecretCipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class AesGcmSecrets implements SecretCipher {
  private final String current;
  private final Map<String, byte[]> keys;
  private final SecureRandom random = new SecureRandom();

  public AesGcmSecrets(String current, Map<String, byte[]> keys) {
    if (!current.matches("[A-Za-z0-9_-]{1,32}")) throw new IllegalArgumentException("密钥编号非法");
    var copy = new HashMap<String, byte[]>();
    keys.forEach(
        (k, v) -> {
          if (v.length != 32) throw new IllegalArgumentException("主密钥必须为256位");
          copy.put(k, v.clone());
        });
    if (!copy.containsKey(current)) throw new IllegalArgumentException("缺少当前主密钥");
    this.current = current;
    this.keys = Map.copyOf(copy);
  }

  static String b64(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }

  static byte[] decode(String value) {
    return Base64.getDecoder().decode(value);
  }

  public String encrypt(Secret value) {
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(keys.get(current), "AES"),
          new GCMParameterSpec(128, nonce));
      cipher.updateAAD(("v1:" + current).getBytes(StandardCharsets.UTF_8));
      byte[] encrypted = cipher.doFinal(value.value().getBytes(StandardCharsets.UTF_8));
      byte[] packed = new byte[nonce.length + encrypted.length];
      System.arraycopy(nonce, 0, packed, 0, nonce.length);
      System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
      return "v1:" + current + ":" + b64(packed);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("加密失败", e);
    }
  }

  public Secret decrypt(String envelope) {
    try {
      String[] p = envelope.split(":", -1);
      if (p.length != 3 || !p[0].equals("v1") || !keys.containsKey(p[1]))
        throw new IllegalArgumentException();
      byte[] packed = decode(p[2]);
      if (packed.length < 28) throw new IllegalArgumentException();
      byte[] nonce = Arrays.copyOfRange(packed, 0, 12);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(keys.get(p[1]), "AES"),
          new GCMParameterSpec(128, nonce));
      cipher.updateAAD(("v1:" + p[1]).getBytes(StandardCharsets.UTF_8));
      return Secret.of(
          new String(
              cipher.doFinal(Arrays.copyOfRange(packed, 12, packed.length)),
              StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new BusinessException(CoreErrors.SECRET_DECRYPT_FAILED, "密文认证失败");
    }
  }

  public boolean isEnvelope(String value) {
    return value.matches("v1:[A-Za-z0-9_-]{1,32}:[A-Za-z0-9+/]+={0,2}");
  }

  public boolean needsReEncryption(String value) {
    decrypt(value);
    return !value.split(":")[1].equals(current);
  }

  public String mask(Secret value) {
    String text = value.value();
    return text.length() <= 8 ? "****" : "****" + text.substring(text.length() - 4);
  }
}
