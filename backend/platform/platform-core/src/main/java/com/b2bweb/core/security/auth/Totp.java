package com.b2bweb.core.security.auth;

import java.nio.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public final class Totp {
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  public static String secret() {
    byte[] bytes = new byte[20];
    new SecureRandom().nextBytes(bytes);
    StringBuilder out = new StringBuilder();
    int bits = 0, value = 0;
    for (byte b : bytes) {
      value = (value << 8) | (b & 255);
      bits += 8;
      while (bits >= 5) {
        bits -= 5;
        out.append(ALPHABET.charAt((value >> bits) & 31));
      }
    }
    return out.toString();
  }

  static byte[] decode(String text) {
    var out = new java.io.ByteArrayOutputStream();
    int bits = 0, value = 0;
    for (char c : text.toCharArray()) {
      int digit = ALPHABET.indexOf(c);
      if (digit < 0) throw new IllegalArgumentException("密钥格式非法");
      value = (value << 5) | digit;
      bits += 5;
      if (bits >= 8) {
        bits -= 8;
        out.write((value >> bits) & 255);
      }
    }
    return out.toByteArray();
  }

  public static String code(String secret, long step) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
      byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
      int offset = h[h.length - 1] & 15;
      int n = ByteBuffer.wrap(h, offset, 4).getInt() & 0x7fffffff;
      return String.format(java.util.Locale.ROOT, "%06d", n % 1000000);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean matches(String secret, long step, String candidate) {
    return candidate != null
        && candidate.matches("[0-9]{6}")
        && MessageDigest.isEqual(
            code(secret, step).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            candidate.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  }
}
