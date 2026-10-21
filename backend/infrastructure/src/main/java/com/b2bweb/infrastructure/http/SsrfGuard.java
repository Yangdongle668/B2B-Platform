package com.b2bweb.infrastructure.http;

import java.net.*;
import java.util.*;

/** Resolves once. The transport MUST use all and only the returned addresses. */
public final class SsrfGuard {
  private static final java.util.concurrent.ExecutorService DNS =
      new java.util.concurrent.ThreadPoolExecutor(
          0,
          16,
          60,
          java.util.concurrent.TimeUnit.SECONDS,
          new java.util.concurrent.SynchronousQueue<>(),
          task -> {
            var thread = new Thread(task, "ssrf-dns");
            thread.setDaemon(true);
            return thread;
          },
          new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

  public record Target(URI uri, List<InetAddress> addresses) {
    public Target {
      addresses = List.copyOf(addresses);
    }
  }

  public Target validate(URI uri, Set<String> allowedHosts, boolean httpsOnly)
      throws java.io.IOException {
    return validate(uri, allowedHosts, httpsOnly, java.time.Duration.ofSeconds(5));
  }

  public Target validate(
      URI uri, Set<String> allowedHosts, boolean httpsOnly, java.time.Duration timeout)
      throws java.io.IOException {
    if (uri == null
        || uri.getHost() == null
        || uri.getRawUserInfo() != null
        || uri.getRawFragment() != null
        || !Set.of("http", "https").contains(uri.getScheme())
        || httpsOnly && !"https".equals(uri.getScheme()))
      throw new IllegalArgumentException("远程URL格式不允许");
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    if (allowedHosts != null && allowedHosts.stream().noneMatch(rule -> matches(host, rule)))
      throw new HostNotAllowed();
    java.util.concurrent.Future<InetAddress[]> lookup;
    try {
      lookup = DNS.submit(() -> InetAddress.getAllByName(host));
    } catch (java.util.concurrent.RejectedExecutionException e) {
      throw new java.io.IOException("DNS查询繁忙");
    }
    List<InetAddress> addresses;
    try {
      addresses =
          List.of(
              lookup.get(
                  Math.max(1, timeout.toNanos()), java.util.concurrent.TimeUnit.NANOSECONDS));
    } catch (java.util.concurrent.TimeoutException e) {
      lookup.cancel(true);
      throw new java.net.SocketTimeoutException("DNS查询超时");
    } catch (InterruptedException e) {
      lookup.cancel(true);
      Thread.currentThread().interrupt();
      throw new java.io.IOException("DNS查询中断");
    } catch (java.util.concurrent.ExecutionException e) {
      throw new java.net.UnknownHostException();
    }
    if (addresses.isEmpty() || addresses.stream().anyMatch(a -> !publicAddress(a)))
      throw new AddressForbidden();
    return new Target(uri, addresses);
  }

  static boolean matches(String host, String pattern) {
    return pattern.startsWith("*.")
        ? host.endsWith(pattern.substring(1)) && host.length() > pattern.length() - 1
        : host.equals(pattern);
  }

  public static boolean publicAddress(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) return false;
    byte[] b = address.getAddress();
    if (b.length == 4) {
      int a = b[0] & 255, c = b[1] & 255, d = b[2] & 255;
      return !(a == 0
          || a == 10
          || a == 127
          || a >= 224
          || a == 100 && c >= 64 && c <= 127
          || a == 169 && c == 254
          || a == 172 && c >= 16 && c <= 31
          || a == 192 && (c == 168 || c == 0 || c == 2)
          || a == 198 && (c == 18 || c == 19 || c == 51 && d == 100)
          || a == 203 && c == 0 && d == 113);
    }
    // Restrict IPv6 to globally routable unicast; reject docs, NAT64 and transition ranges.
    int a = b[0] & 255, c = b[1] & 255;
    return (a & 0xe0) == 0x20
        && !(a == 0x20
            && c == 0x01
            && ((b[2] & 255) == 0x0d && (b[3] & 255) == 0xb8
                || (b[2] & 255) == 0
                || (b[2] & 255) == 2))
        && !(a == 0x20 && c == 2)
        && !(a == 0x3f && c == 0xff && (b[2] & 0xf0) == 0);
  }

  public static final class HostNotAllowed extends IllegalArgumentException {}

  public static final class AddressForbidden extends IllegalArgumentException {}
}
