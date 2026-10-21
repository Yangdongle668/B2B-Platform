package com.b2bweb.infrastructure.http;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SafeHttpTest {
  @Test
  void deniesReservedAddressesAndWildcardApexAndUnsafeProtocols() throws Exception {
    for (String ip :
        List.of(
            "127.0.0.1",
            "10.0.0.1",
            "169.254.169.254",
            "100.64.0.1",
            "198.18.0.1",
            "192.0.2.1",
            "::1",
            "fc00::1",
            "2001:db8::1",
            "64:ff9b::7f00:1"))
      assertThat(SsrfGuard.publicAddress(InetAddress.getByName(ip))).as(ip).isFalse();
    assertThat(SsrfGuard.publicAddress(InetAddress.getByName("8.8.8.8"))).isTrue();
    assertThat(SsrfGuard.matches("example.com", "*.example.com")).isFalse();
    assertThat(SsrfGuard.matches("x.example.com", "*.example.com")).isTrue();
    var guard = new SsrfGuard();
    assertThatThrownBy(() -> guard.validate(URI.create("https://u:p@example.com"), null, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> guard.validate(URI.create("http://8.8.8.8"), null, true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fetchRejectsPrivateTargetsBeforeConnectingAndTransactionBeforeDns() throws Exception {
    var fetch = new SafeHttpRemoteFileFetcher(Files.createTempDirectory("safe-fetch-test"));
    assertThatThrownBy(
            () ->
                fetch.fetch(
                    RemoteFetchRequest.image(
                        URI.create("http://127.0.0.1/image"), Set.of("127.0.0.1"))))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.errorCode()).isEqualTo(InfraErrors.FETCH_ADDRESS_FORBIDDEN));
    org.springframework.transaction.support.TransactionSynchronizationManager
        .setActualTransactionActive(true);
    try {
      assertThatThrownBy(
              () ->
                  fetch.fetch(
                      RemoteFetchRequest.image(
                          URI.create("https://example.test/image"), Set.of("example.test"))))
          .isInstanceOf(IllegalStateException.class);
    } finally {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .setActualTransactionActive(false);
    }
  }

  @Test
  void pinnedTransportUsesValidatedAddressPreservesHostAndDoesNotFollowRedirects()
      throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    List<String> hosts = new ArrayList<>();
    server.createContext(
        "/",
        e -> {
          hosts.add(e.getRequestHeaders().getFirst("Host"));
          e.getResponseHeaders().add("Location", "http://127.0.0.1/private");
          e.sendResponseHeaders(302, -1);
          e.close();
        });
    server.start();
    try {
      var target =
          new SsrfGuard.Target(
              URI.create("http://public-fixture.invalid:" + server.getAddress().getPort() + "/"),
              List.of(InetAddress.getLoopbackAddress()));
      int status =
          new PinnedHttp()
              .exchange(
                  target,
                  "GET",
                  Map.of(),
                  null,
                  Duration.ofSeconds(2),
                  (code, headers, in) -> code);
      assertThat(status).isEqualTo(302);
      assertThat(hosts).containsExactly("public-fixture.invalid:" + server.getAddress().getPort());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void webhookSignatureUsesTimestampDotExactUtf8Body() throws Exception {
    String value = HttpWebhookTransport.signature(Secret.of("key"), "123", "{\"a\":1}");
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(new javax.crypto.spec.SecretKeySpec("key".getBytes(), "HmacSHA256"));
    assertThat(value)
        .isEqualTo(
            "v1="
                + HexFormat.of()
                    .formatHex(
                        mac.doFinal(
                            "123.{\"a\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    assertThat(
            SafeHttpRemoteFileFetcher.fileName(
                "attachment; filename=\"../../bad.pdf\"", URI.create("https://example.test/a")))
        .isEqualTo("bad.pdf");
  }

  @Test
  void ignoredRedirectBodyIsClosedWithoutDraining() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(workers);
    server.createContext(
        "/",
        exchange -> {
          try {
            exchange.sendResponseHeaders(302, 0);
            exchange.getResponseBody().write('x');
            exchange.getResponseBody().flush();
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    try {
      var target =
          new SsrfGuard.Target(
              URI.create("http://public-fixture.invalid:" + server.getAddress().getPort() + "/"),
              List.of(InetAddress.getLoopbackAddress()));
      long started = System.nanoTime();
      int status =
          new PinnedHttp()
              .exchange(
                  target,
                  "GET",
                  Map.of(),
                  null,
                  Duration.ofSeconds(2),
                  (code, headers, in) -> code);
      assertThat(status).isEqualTo(302);
      assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(1));
    } finally {
      server.stop(0);
      workers.shutdownNow();
      workers.close();
    }
  }
}
