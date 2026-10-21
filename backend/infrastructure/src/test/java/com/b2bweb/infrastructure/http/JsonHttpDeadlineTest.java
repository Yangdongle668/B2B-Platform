package com.b2bweb.infrastructure.http;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.ExternalServiceException;
import com.b2bweb.api.port.InfraErrors;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class JsonHttpDeadlineTest {
  @Test
  void totalDeadlineIncludesResponseHeadersAndSlowBody() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(workers);
    server.createContext(
        "/",
        exchange -> {
          try {
            Thread.sleep(600);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write('{');
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
      var http = new JsonHttp();
      long started = System.nanoTime();
      assertThatThrownBy(
              () ->
                  http.post(
                      URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
                      Map.of(),
                      Map.of(),
                      Duration.ofSeconds(1),
                      InfraErrors.INDEXNOW_FAILED))
          .isInstanceOf(ExternalServiceException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(1400));
    } finally {
      server.stop(0);
      workers.shutdownNow();
      workers.close();
    }
  }
}
