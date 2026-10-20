package com.b2bweb.core.security;

import com.b2bweb.api.common.IdGenerator;
import com.b2bweb.core.module.ModuleCatalog;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Only an explicit local profile and explicit flag permit seeding. Existing users are never reset.
 */
@Component
@Profile("local")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "b2bweb.dev.seed.enabled",
    havingValue = "true")
public class LocalSeed implements ApplicationRunner, Ordered {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final Environment env;
  private final IdGenerator ids;
  private final ModuleCatalog catalog;
  private final TransactionTemplate tx;

  public LocalSeed(
      JdbcTemplate jdbc,
      PasswordEncoder passwords,
      Environment env,
      IdGenerator ids,
      ModuleCatalog catalog,
      PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.env = env;
    this.ids = ids;
    this.catalog = catalog;
    tx = new TransactionTemplate(manager);
  }

  public int getOrder() {
    return 10;
  }

  public void run(ApplicationArguments args) {
    String password = env.getRequiredProperty("B2BWEB_BOOTSTRAP_PASSWORD");
    if (password.length() < 16) throw new IllegalStateException("本地初始密码至少16字符");
    tx.executeWithoutResult(
        status -> {
          if (jdbc.queryForObject(
                  "SELECT COUNT(*) FROM sys_user WHERE email_normalized='admin@local.test' AND"
                      + " deleted_at IS NULL",
                  Integer.class)
              == 0)
            jdbc.update(
                "INSERT INTO"
                    + " sys_user(id,email,display_name,password_hash,system_admin,must_change_password)"
                    + " VALUES(?,'admin@local.test','本地管理员',?,1,1)",
                ids.nextId(),
                passwords.encode(password));
          jdbc.update(
              "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
                  + " VALUES(1,'local','本地示例站点','industrial','Asia/Shanghai','ACTIVE') ON DUPLICATE"
                  + " KEY UPDATE id=id");
          jdbc.update(
              "INSERT INTO sys_site_locale(id,site_id,locale,url_prefix,hreflang,`public`)"
                  + " VALUES(?,1,'zh-CN','zh','zh-CN',1) ON DUPLICATE KEY UPDATE id=id",
              ids.nextId());
          for (String module : catalog.order())
            jdbc.update(
                "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(?,1,?,1) ON"
                    + " DUPLICATE KEY UPDATE id=id",
                ids.nextId(),
                module);
        });
  }
}
