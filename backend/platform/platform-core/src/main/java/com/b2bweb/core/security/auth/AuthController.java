package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/system/auth")
public class AuthController {
  final Duration reauthWindow;
  final Accounts accounts;
  final StringRedisTemplate redis;
  final Clock clock;
  final org.springframework.session.data.redis.RedisIndexedSessionRepository sessions;

  AuthController(
      Accounts accounts,
      StringRedisTemplate redis,
      Clock clock,
      org.springframework.session.data.redis.RedisIndexedSessionRepository sessions,
      @org.springframework.beans.factory.annotation.Value(
              "${b2bweb.core.security.reauth-window:10m}")
          Duration reauthWindow) {
    this.accounts = accounts;
    this.redis = redis;
    this.clock = clock;
    this.sessions = sessions;
    this.reauthWindow = reauthWindow;
  }

  public record AuthState(String stage, String userId, List<String> recoveryCodes) {
    public AuthState {
      recoveryCodes = List.copyOf(recoveryCodes);
    }

    public AuthState(String stage, String userId) {
      this(stage, userId, List.of());
    }
  }

  public record CsrfResponse(String token, String headerName) {}

  public record Enrollment(String secret, String otpauthUri) {}

  public record Reauthenticated(String result) {}

  public record Login(
      @NotBlank @Email String email, @NotNull @Size(min = 1, max = 128) String password) {}

  public record Code(@NotBlank @Size(max = 128) String code) {}

  public record Password(@NotNull @Size(min = 12, max = 128) String password) {}

  public record Reauthentication(@NotNull @Size(max = 128) String password, String code) {}

  public record SiteSelection(String id, String name) {}

  public record Me(AuthState state, String displayName, List<SiteSelection> sites) {}

  @GetMapping("/me")
  public Me me(HttpServletRequest request) {
    long id = uid(request);
    var user = accounts.user(id);
    var state = state(request, id);
    if (!state.stage().equals("AUTHENTICATED"))
      return new Me(state, (String) user.get("display_name"), List.of());
    boolean admin = Accounts.flag(user.get("system_admin"));
    var sites =
        accounts.jdbc.query(
            "SELECT id,name FROM sys_site s WHERE status<>'ARCHIVED' AND (? OR EXISTS(SELECT 1 FROM"
                + " sys_user_site_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.site_id=s.id"
                + " AND ur.user_id=? AND r.deleted_at IS NULL)) ORDER BY id",
            (r, n) -> new SiteSelection(Long.toString(r.getLong("id")), r.getString("name")),
            admin,
            id);
    return new Me(state, (String) user.get("display_name"), sites);
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  void limit(String subject) {
    try {
      String digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      Long count =
          redis.execute(
              new DefaultRedisScript<>(
                  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60)"
                      + " end; return n",
                  Long.class),
              List.of("sys:core:auth:" + digest));
      if (count == null) throw new IllegalStateException();
      if (count > 10)
        throw new BusinessException(ErrorCode.of("CORE_RATE_LIMITED", 429, "尝试次数过多"), "请稍后重试");
    } catch (PlatformException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.of("CORE_AUTH_UNAVAILABLE", 503, "认证服务暂不可用"), "请稍后重试");
    }
  }

  long uid(HttpServletRequest request) {
    var s = request.getSession(false);
    if (s == null || s.getAttribute("uid") == null)
      throw new BusinessException(CoreErrors.NOT_AUTHENTICATED, "请先登录");
    return (Long) s.getAttribute("uid");
  }

  AuthState state(HttpServletRequest request, long id) {
    return new AuthState(
        accounts.stage(id, Boolean.TRUE.equals(request.getSession().getAttribute("mfa"))),
        Long.toString(id));
  }

  @PostMapping("/login")
  public AuthState login(@Valid @RequestBody Login body, HttpServletRequest request) {
    limit("login:" + body.email().strip().toLowerCase(Locale.ROOT) + ":" + request.getRemoteAddr());
    long id =
        accounts
            .login(body.email(), body.password())
            .orElseThrow(
                () -> new BusinessException(CoreErrors.NOT_AUTHENTICATED, "邮箱或密码错误，或尝试次数过多"));
    var old = request.getSession(false);
    if (old != null) old.invalidate();
    var session = request.getSession(true);
    request.changeSessionId();
    session.setAttribute("uid", id);
    session.setAttribute("mfa", false);
    session.setAttribute("reauth", clock.millis());
    session.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, Long.toString(id));
    return state(request, id);
  }

  @PostMapping("/mfa/setup")
  public Enrollment setup(HttpServletRequest request) {
    long id = uid(request);
    limit("mfa:" + id);
    String secret = accounts.setup(id);
    String email = (String) accounts.user(id).get("email");
    String label =
        java.net.URLEncoder.encode("B2B:" + email, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20");
    return new Enrollment(
        secret,
        "otpauth://totp/"
            + label
            + "?secret="
            + secret
            + "&issuer=B2B&algorithm=SHA1&digits=6&period=30");
  }

  @PostMapping("/mfa/activate")
  public AuthState activate(@Valid @RequestBody Code code, HttpServletRequest request) {
    return verify(code, request, true);
  }

  @PostMapping("/mfa/verify")
  public AuthState verify(@Valid @RequestBody Code code, HttpServletRequest request) {
    return verify(code, request, false);
  }

  AuthState verify(Code code, HttpServletRequest request, boolean activate) {
    long id = uid(request);
    limit("mfa:" + id);
    var verification = accounts.verifyWithCodes(id, code.code(), activate);
    if (!verification.valid())
      throw new BusinessException(CoreErrors.NOT_AUTHENTICATED, "验证码无效或已使用");
    request.changeSessionId();
    request.getSession().setAttribute("mfa", true);
    request.getSession().setAttribute("reauth", clock.millis());
    var state = state(request, id);
    return new AuthState(
        state.stage(), state.userId(), activate ? verification.recoveryCodes() : List.of());
  }

  @PostMapping("/reauthenticate")
  public Reauthenticated reauthenticate(
      @Valid @RequestBody Reauthentication body, HttpServletRequest request) {
    long id = uid(request);
    limit("reauth:" + id);
    var user = accounts.user(id);
    if (accounts
        .login((String) user.get("email"), body.password())
        .filter(found -> found == id)
        .isEmpty()) throw new BusinessException(CoreErrors.NOT_AUTHENTICATED, "验证失败");
    boolean bound =
        accounts.jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_mfa WHERE user_id=? AND status='ACTIVE'",
                Integer.class,
                id)
            > 0;
    if (bound && !accounts.verify(id, body.code(), false))
      throw new BusinessException(CoreErrors.NOT_AUTHENTICATED, "验证失败");
    request.getSession().setAttribute("reauth", clock.millis());
    return new Reauthenticated("verified");
  }

  @PostMapping("/password")
  public AuthState password(@Valid @RequestBody Password body, HttpServletRequest request) {
    long id = uid(request);
    String stage =
        accounts.stage(id, Boolean.TRUE.equals(request.getSession().getAttribute("mfa")));
    if (stage.startsWith("MFA_")) throw new BusinessException(CoreErrors.FORBIDDEN, "请先完成两步验证");
    Long verified = (Long) request.getSession().getAttribute("reauth");
    if (verified == null || clock.millis() - verified > reauthWindow.toMillis())
      throw new BusinessException(CoreErrors.FORBIDDEN, "请重新验证身份");
    accounts.tx.executeWithoutResult(
        tx -> {
          accounts.jdbc.update(
              "UPDATE sys_user SET"
                  + " password_hash=?,must_change_password=0,password_changed_at=?,version=version+1"
                  + " WHERE id=?",
              accounts.passwords.encode(body.password()),
              java.sql.Timestamp.from(clock.instant()),
              id);
          accounts.audit(id, "user.password_changed", "SUCCESS");
        });
    String current = request.getSession().getId();
    sessions.findByPrincipalName(Long.toString(id)).keySet().stream()
        .filter(sessionId -> !sessionId.equals(current))
        .forEach(sessions::deleteById);
    return state(request, id);
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    var session = request.getSession(false);
    if (session != null) {
      if (session.getAttribute("uid") instanceof Long id)
        accounts.audit(id, "user.logout", "SUCCESS");
      session.invalidate();
    }
    response.setStatus(204);
  }
}
