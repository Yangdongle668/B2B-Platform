package com.b2bweb.core.security.auth;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/** Tokens never grant forwarding trust without a permitted socket peer address. */
final class InternalTrust {
  private final List<byte[]> tokens;
  private final List<IpAddressMatcher> networks;

  InternalTrust(Environment env) {
    String raw =
        env.getProperty(
            "B2BWEB_INTERNAL_TOKENS", env.getProperty("b2bweb.core.internal.tokens", ""));
    tokens =
        Arrays.stream(raw.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .map(InternalTrust::hash)
            .toList();
    String cidrs = env.getProperty("b2bweb.core.internal.allowed-cidrs", "127.0.0.1/32,::1/128");
    networks =
        Arrays.stream(cidrs.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .map(IpAddressMatcher::new)
            .toList();
  }

  private static byte[] hash(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  boolean token(String input) {
    if (input == null || input.length() > 4096) return false;
    byte[] candidate = hash(input);
    boolean match = false;
    for (byte[] token : tokens) match |= MessageDigest.isEqual(token, candidate);
    return match;
  }

  boolean trusted(String token, String address) {
    if (!token(token)) return false;
    try {
      return networks.stream().anyMatch(n -> n.matches(address));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
