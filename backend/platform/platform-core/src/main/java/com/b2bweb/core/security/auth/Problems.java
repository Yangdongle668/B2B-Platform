package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.PlatformException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

public final class Problems {
  private static final JsonMapper JSON = mapper();

  private static JsonMapper mapper() {
    var module = new SimpleModule();
    module.addSerializer(Long.class, ToStringSerializer.instance);
    module.addSerializer(Long.TYPE, ToStringSerializer.instance);
    return JsonMapper.builder().addModule(module).build();
  }

  public static void write(HttpServletResponse response, int status, String code, String title)
      throws IOException {
    write(response, status, code, title, title, java.util.List.of(), java.util.Map.of());
  }

  public static void write(HttpServletResponse response, PlatformException error)
      throws IOException {
    write(
        response,
        error.errorCode().status(),
        error.errorCode().code(),
        error.errorCode().title(),
        error.detail(),
        error.errors(),
        error.properties());
  }

  public static void write(
      HttpServletResponse response,
      int status,
      String code,
      String title,
      String detail,
      java.util.List<com.b2bweb.api.common.FieldError> errors,
      java.util.Map<String, Object> properties)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store");
    var body =
        JSON.createObjectNode()
            .put("type", "about:blank")
            .put("status", status)
            .put("title", title)
            .put("code", code)
            .put("detail", detail);
    if (!errors.isEmpty()) body.set("errors", JSON.valueToTree(errors));
    properties.forEach(
        (key, value) -> {
          if (!java.util.Set.of("type", "status", "title", "code", "detail", "errors", "instance")
              .contains(key)) body.set(key, JSON.valueToTree(value));
        });
    response.getWriter().write(body.toString());
  }
}
