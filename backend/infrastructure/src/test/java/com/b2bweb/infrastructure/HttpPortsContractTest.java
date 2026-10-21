package com.b2bweb.infrastructure;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.http.*;
import com.b2bweb.testkit.contract.port.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class HttpPortsContractTest {
  HttpServer server;
  int status = 200, count;
  final Map<String, CaptchaResult> captcha = new HashMap<>();
  final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        e -> {
          String path = e.getRequestURI().getPath();
          byte[] body = e.getRequestBody().readAllBytes();
          Object result = Map.of("success", status == 200);
          if (path.equals("/captcha")) {
            var fields = new HashMap<String, String>();
            for (String field : new String(body, StandardCharsets.UTF_8).split("&")) {
              var pair = field.split("=", 2);
              fields.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            var value = captcha.remove(fields.get("response"));
            if (value == null)
              value = new CaptchaResult(false, List.of("timeout-or-duplicate"), null, null, null);
            result =
                Map.of(
                    "success",
                    value.success(),
                    "hostname",
                    Objects.toString(value.hostname(), ""),
                    "action",
                    Objects.toString(value.action(), ""),
                    "error-codes",
                    value.errorCodes());
          }
          if (path.equals("/cdn") && status == 200) count++;
          byte[] bytes = json.writeValueAsBytes(result);
          e.getResponseHeaders().add("Content-Type", "application/json");
          e.sendResponseHeaders(status, bytes.length);
          e.getResponseBody().write(bytes);
          e.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  URI endpoint(String path) {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
  }

  @Nested
  class Captcha extends CaptchaVerifierContract {
    protected CaptchaVerifier api() {
      return new TurnstileCaptchaVerifier(endpoint("/captcha"));
    }

    protected void allow(String token, CaptchaResult result) {
      captcha.put(token, result);
    }
  }

  @Nested
  class IndexNow extends IndexNowClientContract {
    protected IndexNowClient api() {
      return new HttpIndexNowClient(endpoint("/index"));
    }

    protected void status(int value) {
      status = value;
    }
  }

  @Nested
  class Cdn extends CdnPurgerContract {
    protected CdnPurger api() {
      return new CloudflareCdnPurger(
          endpoint("/cdn"), com.b2bweb.api.common.Secret.of("fixture"), 30, true);
    }

    protected void available(boolean value) {
      status = value ? 200 : 503;
    }

    protected int successfulPurges() {
      return count;
    }
  }
}
