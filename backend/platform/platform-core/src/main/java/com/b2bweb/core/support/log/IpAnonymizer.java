package com.b2bweb.core.support.log;

import java.net.*;
import java.util.*;

public final class IpAnonymizer {
  private IpAnonymizer() {}

  public static String anonymize(String value) {
    if (value == null || !value.matches("[0-9a-fA-F:.]+")) return "unknown";
    try {
      byte[] bytes = InetAddress.getByName(value).getAddress();
      if (bytes.length == 4) bytes[3] = 0;
      else Arrays.fill(bytes, 6, 16, (byte) 0);
      return InetAddress.getByAddress(bytes).getHostAddress();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }
}
