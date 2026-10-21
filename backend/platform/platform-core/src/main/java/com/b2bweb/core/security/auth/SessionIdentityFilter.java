package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class SessionIdentityFilter extends OncePerRequestFilter {
  final Accounts accounts;
  final SiteContexts sites;
  final UserContexts users;
  final JdbcTemplate jdbc;
  final Clock clock;

  SessionIdentityFilter(
      Accounts accounts, SiteContexts sites, UserContexts users, JdbcTemplate jdbc, Clock clock) {
    this.accounts = accounts;
    this.sites = sites;
    this.users = users;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var session = req.getSession(false);
    res.setHeader("Cache-Control", "no-store");
    if (session == null || session.getAttribute("uid") == null) {
      chain.doFilter(req, res);
      return;
    }
    try {
      if (clock.millis() - session.getCreationTime() >= Duration.ofHours(12).toMillis()) {
        session.invalidate();
        Problems.write(res, 401, "CORE_NOT_AUTHENTICATED", "会话已过期");
        return;
      }
      long id = (Long) session.getAttribute("uid");
      var data = accounts.user(id);
      boolean mfa = Boolean.TRUE.equals(session.getAttribute("mfa"));
      String stage = accounts.stage(id, mfa);
      boolean authPath = req.getRequestURI().startsWith("/api/admin/v1/system/auth/");
      if (!stage.equals("AUTHENTICATED") && !authPath) {
        Problems.write(
            res,
            403,
            stage.equals("PASSWORD_CHANGE_REQUIRED")
                ? "CORE_PASSWORD_CHANGE_REQUIRED"
                : "CORE_MFA_REQUIRED",
            "请完成身份验证");
        return;
      }
      var user =
          new CurrentUser(
              id,
              (String) data.get("display_name"),
              (String) data.get("email"),
              Accounts.flag(data.get("system_admin")),
              Set.of(),
              mfa);
      var auth = UsernamePasswordAuthenticationToken.authenticated(id, null, List.of());
      SecurityContextHolder.getContext().setAuthentication(auth);
      try (var userScope = users.open(user);
          var reauth =
              users.withReauthentication(
                  session.getAttribute("reauth") instanceof Long at
                      ? Instant.ofEpochMilli(at)
                      : Instant.EPOCH)) {
        if (authPath) {
          chain.doFilter(req, res);
          return;
        }
        String header = req.getHeader("X-Site-Id");
        if (header == null || !header.matches("[1-9][0-9]{0,18}")) {
          Problems.write(res, 400, "CORE_SITE_REQUIRED", "请选择站点");
          return;
        }
        long site;
        try {
          site = Long.parseLong(header);
        } catch (NumberFormatException e) {
          Problems.write(res, 400, "CORE_SITE_REQUIRED", "站点编号无效");
          return;
        }
        var siteInfo =
            jdbc.queryForList("SELECT status FROM sys_site WHERE id=?", String.class, site);
        if (siteInfo.isEmpty()) {
          Problems.write(res, 404, "CORE_SITE_NOT_FOUND", "站点不存在");
          return;
        }
        if (siteInfo.getFirst().equals("ARCHIVED")
            && !Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())) {
          Problems.write(res, 403, "CORE_FORBIDDEN", "归档站点只读");
          return;
        }
        if (!user.systemAdmin()
            && jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_site_role ur JOIN sys_role r ON r.id=ur.role_id"
                        + " WHERE ur.site_id=? AND ur.user_id=? AND r.deleted_at IS NULL",
                    Integer.class,
                    site,
                    id)
                == 0) {
          Problems.write(res, 403, "CORE_FORBIDDEN", "没有站点访问权限");
          return;
        }
        sites.runAs(
            site,
            com.b2bweb.api.site.ContextOrigin.ADMIN,
            null,
            () -> {
              try {
                chain.doFilter(req, res);
              } catch (IOException | ServletException e) {
                throw new FilterFailure(e);
              }
            });
      }
    } catch (PlatformException e) {
      Problems.write(res, e.errorCode().status(), e.errorCode().code(), e.errorCode().title());
    } catch (FilterFailure e) {
      if (e.getCause() instanceof IOException io) throw io;
      throw (ServletException) e.getCause();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  static final class FilterFailure extends RuntimeException {
    FilterFailure(Exception e) {
      super(e);
    }
  }
}
