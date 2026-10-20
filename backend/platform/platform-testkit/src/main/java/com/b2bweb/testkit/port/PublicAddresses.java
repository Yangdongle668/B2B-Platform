package com.b2bweb.testkit.port;

import java.net.*;

/** Literal-only fixture validation. No DNS request is ever made by test ports. */
final class PublicAddresses {
  static boolean isPublicLiteral(String value) {
    if (value == null || !(value.matches("[0-9]+(?:\\.[0-9]+){3}") || value.contains(":")))
      return false;
    try {
      InetAddress address = InetAddress.getByName(value);
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()) return false;
      byte[] b = address.getAddress();
      if (b.length == 4) {
        int a = b[0] & 255, c = b[1] & 255;
        if (a == 0
            || a >= 224
            || a == 100 && c >= 64 && c <= 127
            || a == 169 && c == 254
            || a == 192 && c == 0
            || a == 198 && (c == 18 || c == 19)) return false;
      } else if ((b[0] & 254) == 252
          || ((b[0] & 255) == 32 && (b[1] & 255) == 1 && (b[2] & 255) == 13 && (b[3] & 255) == 184))
        return false;
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
