package com.b2bweb.core.security.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import tools.jackson.databind.json.JsonMapper;

public final class Problems {
  public static void write(HttpServletResponse response, int status, String code, String title)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response
        .getWriter()
        .write(
            JsonMapper.builder()
                .build()
                .createObjectNode()
                .put("type", "about:blank")
                .put("status", status)
                .put("title", title)
                .put("code", code)
                .toString());
  }
}
