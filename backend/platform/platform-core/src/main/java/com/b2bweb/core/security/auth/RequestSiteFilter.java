package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

final class RequestSiteFilter extends OncePerRequestFilter {
  private final SiteContexts sites;
  private final UserContexts users;
  private final SiteApi api;
  private final InternalTrust trust;
  private final boolean internal;

  RequestSiteFilter(
      SiteContexts sites, UserContexts users, SiteApi api, InternalTrust trust, boolean internal) {
    this.sites = sites;
    this.users = users;
    this.api = api;
    this.trust = trust;
    this.internal = internal;
  }

  private String host(String value) {
    if (value == null
        || value.isBlank()
        || value.contains("/")
        || value.contains("\\")
        || value.contains("@")
        || value.contains(",")
        || value.contains("#")
        || value.contains("?")
        || value.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException();
    String hostname = value;
    int colon = value.lastIndexOf(':');
    if (colon >= 0) {
      if (value.indexOf(':') != colon) throw new IllegalArgumentException();
      int port = Integer.parseInt(value.substring(colon + 1));
      if (port < 1 || port > 65535) throw new IllegalArgumentException();
      hostname = value.substring(0, colon);
    }
    String ascii =
        IDN.toASCII(hostname, IDN.USE_STD3_ASCII_RULES)
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\.$", "");
    if (ascii.isBlank()) throw new IllegalArgumentException();
    return ascii;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try (var user = users.withoutUser();
        var actor = users.asActor(ActorRef.SYSTEM)) {
      boolean trusted =
          trust.trusted(request.getHeader("X-Internal-Token"), request.getRemoteAddr());
      if (internal && !trusted) {
        Problems.write(response, 403, "CORE_INTERNAL_FORBIDDEN", "内部请求校验失败");
        return;
      }
      if (internal) {
        String id = request.getHeader("X-Site-Id");
        if (id == null) {
          try (var scope = sites.withoutSite()) {
            chain.doFilter(request, response);
          }
          return;
        }
        long site;
        try {
          site = Long.parseLong(id);
          if (site <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
          Problems.write(response, 400, "CORE_SITE_REQUIRED", "站点编号无效");
          return;
        }
        invoke(site, ContextOrigin.INTERNAL, null, request, response, chain);
        return;
      }
      String incoming =
          trusted && request.getHeader("X-Site-Host") != null
              ? request.getHeader("X-Site-Host")
              : request.getHeader("Host");
      String normalized;
      try {
        normalized = host(incoming);
      } catch (IllegalArgumentException e) {
        Problems.write(response, 400, "CORE_SITE_HOST_INVALID", "站点域名无效");
        return;
      }
      var resolution = api.resolveHost(normalized);
      if (resolution.isEmpty()) {
        Problems.write(response, 404, "CORE_SITE_NOT_FOUND", "站点不存在");
        return;
      }
      long site = resolution.get().siteId();
      String explicit = request.getParameter("locale");
      String locale =
          sites.callAs(
              site,
              () -> {
                if (explicit != null)
                  return api.locale(explicit)
                      .orElseThrow(
                          () -> new BusinessException(CoreErrors.LOCALE_NOT_ENABLED, "站点未启用该语言"))
                      .locale();
                String path = request.getParameter("path");
                if (path == null) return null;
                String prefix = path.replaceFirst("^/+", "").split("/", 2)[0];
                return api.localeByUrlPrefix(prefix).map(SiteLocaleInfo::locale).orElse(null);
              });
      invoke(site, ContextOrigin.PUBLIC, locale, request, response, chain);
    } catch (PlatformException e) {
      Problems.write(response, e.errorCode().status(), e.errorCode().code(), e.detail());
    } catch (SessionIdentityFilter.FilterFailure e) {
      if (e.getCause() instanceof IOException io) throw io;
      throw (ServletException) e.getCause();
    } catch (IOException | ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void invoke(
      long site,
      ContextOrigin origin,
      String locale,
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) {
    sites.runAs(
        site,
        origin,
        locale,
        () -> {
          try {
            chain.doFilter(request, response);
          } catch (IOException | ServletException e) {
            throw new SessionIdentityFilter.FilterFailure(e);
          }
        });
  }
}
