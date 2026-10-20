package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.core.security.AesGcmSecrets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/** Database transitions commit before HTTP authentication failures are returned. */
public final class Accounts {
  final JdbcTemplate jdbc;
  final TransactionTemplate tx;
  final PasswordEncoder passwords;
  final AesGcmSecrets cipher;
  final Clock clock;
  final IdGenerator ids;
  final String dummy;
  final byte[] recoveryKey;

  public Accounts(
      JdbcTemplate jdbc,
      TransactionTemplate tx,
      PasswordEncoder passwords,
      AesGcmSecrets cipher,
      Clock clock,
      IdGenerator ids,
      byte[] master) {
    this.jdbc = jdbc;
    this.tx = tx;
    this.passwords = passwords;
    this.cipher = cipher;
    this.clock = clock;
    this.ids = ids;
    dummy = passwords.encode(UUID.randomUUID().toString());
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(new javax.crypto.spec.SecretKeySpec(master, "HmacSHA256"));
      recoveryKey =
          mac.doFinal("b2b:recovery-key:v1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public Optional<Long> login(String email, String password) {
    return tx.execute(
        status -> {
          var rows =
              jdbc.queryForList(
                  "SELECT * FROM sys_user WHERE email_normalized=? AND deleted_at IS NULL FOR"
                      + " UPDATE",
                  email.strip().toLowerCase(Locale.ROOT));
          var user = rows.isEmpty() ? null : rows.getFirst();
          boolean valid =
              passwords.matches(
                  password, user == null ? dummy : (String) user.get("password_hash"));
          if (user == null) {
            audit(null, "user.login_failed", "FAILURE");
            return Optional.empty();
          }
          long id = ((Number) user.get("id")).longValue();
          var locked = (Timestamp) user.get("locked_until");
          if (!valid
              || !user.get("status").equals("ACTIVE")
              || (locked != null && locked.toInstant().isAfter(clock.instant()))) {
            int failures = ((Number) user.get("failed_login_count")).intValue() + 1;
            long minutes = failures < 5 ? 0 : Math.min(60, 1L << Math.min(6, failures - 5));
            jdbc.update(
                "UPDATE sys_user SET failed_login_count=?,last_failed_login_at=?,locked_until=?"
                    + " WHERE id=?",
                failures,
                Timestamp.from(clock.instant()),
                minutes == 0 ? null : Timestamp.from(clock.instant().plusSeconds(minutes * 60)),
                id);
            audit(id, "user.login_failed", "FAILURE");
            return Optional.empty();
          }
          jdbc.update(
              "UPDATE sys_user SET failed_login_count=0,locked_until=NULL,last_login_at=? WHERE"
                  + " id=?",
              Timestamp.from(clock.instant()),
              id);
          audit(id, "user.login", "SUCCESS");
          return Optional.of(id);
        });
  }

  public Map<String, Object> user(long id) {
    var users =
        jdbc.queryForList(
            "SELECT * FROM sys_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL", id);
    if (users.isEmpty()) throw new BusinessException(CoreErrors.NOT_AUTHENTICATED, "请重新登录");
    return users.getFirst();
  }

  public static boolean flag(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }

  public String stage(long id, boolean verified) {
    var u = user(id);
    if (!verified) {
      boolean bound =
          jdbc.queryForObject(
                  "SELECT COUNT(*) FROM sys_user_mfa WHERE user_id=? AND status='ACTIVE'",
                  Integer.class,
                  id)
              > 0;
      boolean required =
          flag(u.get("system_admin"))
              || jdbc.queryForObject(
                      "SELECT COUNT(*) FROM sys_user_site_role ur JOIN sys_role r ON"
                          + " r.id=ur.role_id WHERE ur.user_id=? AND r.deleted_at IS NULL AND"
                          + " (r.mfa_required=1 OR EXISTS(SELECT 1 FROM sys_role_permission rp JOIN"
                          + " sys_permission p ON p.id=rp.permission_id WHERE rp.role_id=r.id AND"
                          + " p.is_sensitive=1 AND p.removed_at IS NULL))",
                      Integer.class,
                      id)
                  > 0;
      if (bound || required) return bound ? "MFA_PENDING" : "MFA_ENROLLMENT_REQUIRED";
    }
    return flag(u.get("must_change_password")) ? "PASSWORD_CHANGE_REQUIRED" : "AUTHENTICATED";
  }

  public String setup(long id) {
    return tx.execute(
        status -> {
          user(id);
          var active =
              jdbc.queryForList("SELECT * FROM sys_user_mfa WHERE user_id=? FOR UPDATE", id);
          if (!active.isEmpty() && active.getFirst().get("status").equals("ACTIVE"))
            throw new BusinessException(CoreErrors.FORBIDDEN, "已绑定验证器");
          String secret = Totp.secret();
          jdbc.update(
              "INSERT INTO sys_user_mfa(id,user_id,secret_ciphertext) VALUES(?,?,?) ON DUPLICATE"
                  + " KEY UPDATE secret_ciphertext=VALUES(secret_ciphertext)",
              ids.nextId(),
              id,
              cipher.encrypt(Secret.of(secret)));
          audit(id, "user.mfa_setup", "SUCCESS");
          return secret;
        });
  }

  public boolean verify(long id, String code, boolean activate) {
    return tx.execute(
        status -> {
          var rows = jdbc.queryForList("SELECT * FROM sys_user_mfa WHERE user_id=? FOR UPDATE", id);
          if (rows.isEmpty()) return false;
          var row = rows.getFirst();
          if (!activate && !row.get("status").equals("ACTIVE")
              || activate && !row.get("status").equals("PENDING")) return false;
          if (code != null && code.matches("[0-9a-f]{32}") && row.get("status").equals("ACTIVE")) {
            var json = tools.jackson.databind.json.JsonMapper.builder().build();
            var hashes = json.readTree((String) row.get("recovery_code_hashes"));
            String expected = recoveryHash(code);
            var remaining = json.createArrayNode();
            boolean found = false;
            for (var h : hashes) {
              if (java.security.MessageDigest.isEqual(
                  h.asString().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                  expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) found = true;
              else remaining.add(h.asString());
            }
            if (found) {
              jdbc.update(
                  "UPDATE sys_user_mfa SET recovery_code_hashes=CAST(? AS"
                      + " JSON),last_used_at=?,version=version+1 WHERE user_id=?",
                  remaining.toString(),
                  Timestamp.from(clock.instant()),
                  id);
              audit(id, "user.mfa_recovered", "SUCCESS");
              return true;
            }
          }
          long current = clock.instant().getEpochSecond() / 30,
              last =
                  row.get("last_used_step") == null
                      ? -1
                      : ((Number) row.get("last_used_step")).longValue();
          String secret = cipher.decrypt((String) row.get("secret_ciphertext")).value();
          for (long step = current - 1; step <= current + 1; step++)
            if (step > last && Totp.matches(secret, step, code)) {
              jdbc.update(
                  "UPDATE sys_user_mfa SET"
                      + " status='ACTIVE',last_used_step=?,last_used_at=?,confirmed_at=COALESCE(confirmed_at,?),version=version+1"
                      + " WHERE user_id=?",
                  step,
                  Timestamp.from(clock.instant()),
                  Timestamp.from(clock.instant()),
                  id);
              audit(id, "user.mfa_verified", "SUCCESS");
              return true;
            }
          audit(id, "user.mfa_failed", "FAILURE");
          return false;
        });
  }

  public record Verification(boolean valid, List<String> recoveryCodes) {}

  public Verification verifyWithCodes(long id, String code, boolean activate) {
    return tx.execute(
        txStatus -> {
          if (!verify(id, code, activate)) return new Verification(false, List.of());
          if (!activate) return new Verification(true, List.of());
          List<String> codes = new ArrayList<>();
          var hashes = tools.jackson.databind.json.JsonMapper.builder().build().createArrayNode();
          for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[16];
            new java.security.SecureRandom().nextBytes(bytes);
            String value = HexFormat.of().formatHex(bytes);
            codes.add(value);
            hashes.add(recoveryHash(value));
          }
          jdbc.update(
              "UPDATE sys_user_mfa SET recovery_code_hashes=CAST(? AS"
                  + " JSON),recovery_codes_generated_at=? WHERE user_id=?",
              hashes.toString(),
              Timestamp.from(clock.instant()),
              id);
          return new Verification(true, List.copyOf(codes));
        });
  }

  String recoveryHash(String code) {
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(new javax.crypto.spec.SecretKeySpec(recoveryKey, "HmacSHA256"));
      return HexFormat.of()
          .formatHex(mac.doFinal(code.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void audit(Long user, String action, String result) {
    jdbc.update(
        "INSERT INTO sys_audit_log(id,user_id,module_id,action,summary,result)"
            + " VALUES(?,?,'core',?,'身份验证',?)",
        ids.nextId(),
        user == null ? 0 : user,
        action,
        result);
  }
}
