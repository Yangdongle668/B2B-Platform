package com.b2bweb.core.security.auth;

import com.b2bweb.api.site.*;
import com.b2bweb.core.module.ModuleCatalog;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs inside the already established request site. No URL prefix implies a permission grant. */
final class ModuleGuardFilter extends OncePerRequestFilter {
  private final Map<String, String> owners = new HashMap<>();
  private final SiteApi api;
  private final SiteContextProvider sites;

  ModuleGuardFilter(ModuleCatalog catalog, SiteApi api, SiteContextProvider sites) {
    this.api = api;
    this.sites = sites;
    catalog
        .descriptors()
        .forEach(
            (id, node) -> {
              if (node.path("apiPrefix").isString())
                owners.put(node.path("apiPrefix").asString(), id);
            });
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var matcher =
        java.util.regex.Pattern.compile("^/api/(?:admin|public|internal)/v1/([^/]+)(?:/.*)?$")
            .matcher(request.getRequestURI());
    if (matcher.matches() && !matcher.group(1).equals("system")) {
      String owner = owners.get(matcher.group(1));
      if (owner != null) {
        boolean enabled =
            sites.current().isPresent()
                ? api.isModuleEnabled(owner)
                : api.modules().stream()
                    .anyMatch(
                        m -> m.moduleId().equals(owner) && m.status() == ModuleStatus.INSTALLED);
        if (!enabled) {
          Problems.write(response, 404, "CORE_MODULE_DISABLED", "模块已停用");
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }
}
