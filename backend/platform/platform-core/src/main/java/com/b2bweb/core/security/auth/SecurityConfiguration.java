package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@Configuration(proxyBeanMethods = false)
@EnableRedisIndexedHttpSession(
    redisNamespace = "sys:core:session",
    maxInactiveIntervalInSeconds = 7200)
public class SecurityConfiguration {
  @Bean
  org.springframework.session.web.http.CookieSerializer cookieSerializer() {
    var cookie = new org.springframework.session.web.http.DefaultCookieSerializer();
    cookie.setCookieName("__Host-SESSION");
    cookie.setUseSecureCookie(true);
    cookie.setUseHttpOnlyCookie(true);
    cookie.setSameSite("Lax");
    cookie.setCookiePath("/");
    return cookie;
  }

  @Bean
  PasswordEncoder passwords() {
    return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
  }

  @Bean
  AesGcmSecrets secrets(@Value("${B2BWEB_SECRET_KEYS}") String encoded) {
    return SecretKeyring.parse(encoded).cipher();
  }

  @Bean
  Accounts accounts(
      JdbcTemplate jdbc,
      PlatformTransactionManager manager,
      PasswordEncoder passwords,
      AesGcmSecrets cipher,
      Clock clock,
      IdGenerator ids,
      @Value("${B2BWEB_RECOVERY_CODE_KEY}") String master) {
    return new Accounts(
        jdbc,
        new TransactionTemplate(manager),
        passwords,
        cipher,
        clock,
        ids,
        Base64.getDecoder().decode(master));
  }

  @Bean
  PermissionChecker permissionChecker(JdbcTemplate jdbc, SiteContexts sites, UserContexts users) {
    return new DatabasePermissions(jdbc, sites, users);
  }

  @Bean
  PermissionAspect permissionAspect(
      PermissionChecker permissions,
      UserContexts users,
      Clock clock,
      @Value("${b2bweb.core.security.reauth-window:10m}") java.time.Duration window) {
    return new PermissionAspect(permissions, users, clock, window);
  }

  @Bean
  com.b2bweb.api.security.UserDirectory userDirectory(
      JdbcTemplate jdbc, SiteContexts sites, PermissionChecker permissions) {
    return new com.b2bweb.core.directory.DatabaseUsers(jdbc, sites, permissions);
  }

  @Bean
  @Order(0)
  SecurityFilterChain management(
      HttpSecurity http, @Value("${management.server.port:8081}") int port) throws Exception {
    return http.securityMatcher(
            request ->
                request.getLocalPort() == port && request.getRequestURI().startsWith("/actuator/"))
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(c -> c.anyRequest().permitAll())
        .build();
  }

  @Bean
  @Order(1)
  SecurityFilterChain admin(
      HttpSecurity http,
      Accounts accounts,
      SiteContexts sites,
      UserContexts users,
      JdbcTemplate jdbc,
      Clock clock,
      com.b2bweb.core.module.ModuleCatalog catalog,
      com.b2bweb.api.site.SiteApi siteApi,
      com.b2bweb.api.port.RateLimiter limits)
      throws Exception {
    var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookieCustomizer(c -> c.secure(true).sameSite("Lax").path("/"));
    return http.securityMatcher("/api/admin/**")
        .csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(
            new SessionIdentityFilter(accounts, sites, users, jdbc, clock),
            AuthorizationFilter.class)
        .addFilterAfter(new ModuleGuardFilter(catalog, siteApi, sites), SessionIdentityFilter.class)
        .addFilterAfter(new AdminRateLimitFilter(limits, users), ModuleGuardFilter.class)
        .exceptionHandling(
            c ->
                c.authenticationEntryPoint(
                        (req, res, e) -> Problems.write(res, 401, "CORE_NOT_AUTHENTICATED", "请先登录"))
                    .accessDeniedHandler(
                        (req, res, e) -> Problems.write(res, 403, "CORE_FORBIDDEN", "请求校验失败")))
        .authorizeHttpRequests(
            c ->
                c.requestMatchers("/api/admin/v1/system/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain publicApi(
      HttpSecurity http,
      SiteContexts sites,
      UserContexts users,
      com.b2bweb.api.site.SiteApi api,
      com.b2bweb.core.module.ModuleCatalog catalog,
      org.springframework.core.env.Environment env)
      throws Exception {
    return http.securityMatcher("/api/public/**")
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .addFilterBefore(
            new RequestSiteFilter(sites, users, api, new InternalTrust(env), false),
            AuthorizationFilter.class)
        .addFilterAfter(new ModuleGuardFilter(catalog, api, sites), RequestSiteFilter.class)
        .authorizeHttpRequests(c -> c.anyRequest().permitAll())
        .build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain internalApi(
      HttpSecurity http,
      SiteContexts sites,
      UserContexts users,
      com.b2bweb.api.site.SiteApi api,
      com.b2bweb.core.module.ModuleCatalog catalog,
      org.springframework.core.env.Environment env)
      throws Exception {
    return http.securityMatcher("/api/internal/**")
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .addFilterBefore(
            new RequestSiteFilter(sites, users, api, new InternalTrust(env), true),
            AuthorizationFilter.class)
        .addFilterAfter(new ModuleGuardFilter(catalog, api, sites), RequestSiteFilter.class)
        .authorizeHttpRequests(c -> c.anyRequest().permitAll())
        .build();
  }

  @Bean
  @Order(4)
  SecurityFilterChain deny(HttpSecurity http, org.springframework.core.env.Environment env)
      throws Exception {
    return http.csrf(c -> c.disable())
        .requestCache(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            c -> {
              if (env.matchesProfiles("local", "test"))
                c.requestMatchers("/v3/api-docs/**").permitAll();
              c.anyRequest().denyAll();
            })
        .exceptionHandling(
            c ->
                c.authenticationEntryPoint(
                    (req, res, e) -> Problems.write(res, 403, "CORE_FORBIDDEN", "请求被拒绝")))
        .build();
  }
}
