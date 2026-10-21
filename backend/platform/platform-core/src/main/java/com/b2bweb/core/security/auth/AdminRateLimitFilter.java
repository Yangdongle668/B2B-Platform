package com.b2bweb.core.security.auth;

import com.b2bweb.api.port.*;
import com.b2bweb.core.context.UserContexts;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import org.springframework.web.filter.OncePerRequestFilter;

final class AdminRateLimitFilter extends OncePerRequestFilter {
  private final RateLimiter limits;
  private final UserContexts users;

  AdminRateLimitFilter(RateLimiter limits, UserContexts users) {
    this.limits = limits;
    this.users = users;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (users.current().isPresent()) {
      var decision =
          limits.tryConsume(
              new RateLimitRule("core.admin_api", 600, Duration.ofMinutes(1), false, false),
              Long.toString(users.require().userId()));
      if (!decision.allowed()) {
        response.setHeader(
            "Retry-After", Long.toString(Math.max(1, decision.retryAfter().toSeconds())));
        Problems.write(response, 429, "CORE_RATE_LIMITED", "请求过于频繁");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
