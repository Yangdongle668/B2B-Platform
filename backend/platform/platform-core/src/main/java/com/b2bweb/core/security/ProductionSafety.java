package com.b2bweb.core.security;

import java.util.*;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ProductionSafety {
  @Bean
  public static BeanFactoryPostProcessor productionSafetyCheck(Environment env) {
    return factory -> {
      Set<String> profiles = Set.of(env.getActiveProfiles());
      if (profiles.isEmpty()) throw new IllegalStateException("必须显式指定运行环境");
      if (!profiles.contains("prod") && !profiles.contains("staging")) return;
      if (org.springframework.util.ClassUtils.isPresent(
          "com.b2bweb.application.dev.LocalStandIns", getClassLoader()))
        throw new IllegalStateException("开发构件不能用于生产或预发布");
      if (profiles.contains("local") || profiles.contains("test"))
        throw new IllegalStateException("生产环境禁止开发或测试配置");
      var keys = SecretKeyring.parse(env.getRequiredProperty("B2BWEB_SECRET_KEYS"));
      for (var entry : keys.keys().entrySet())
        if (entry.getKey().matches("(?i).*(dev|test|example|default).*")
            || Arrays.equals(entry.getValue(), new byte[32]))
          throw new IllegalStateException("生产环境禁止开发密钥");
      if (Boolean.parseBoolean(env.getProperty("b2bweb.core.modules.migrate-on-startup", "false")))
        throw new IllegalStateException("生产启动禁止执行DDL");
      if (Objects.equals(
              env.getRequiredProperty("B2BWEB_DB_USER"),
              env.getRequiredProperty("B2BWEB_DB_MIGRATION_USER"))
          || env.getRequiredProperty("B2BWEB_DB_USER").equalsIgnoreCase("root"))
        throw new IllegalStateException("生产应用和迁移账号必须分离");
      for (String token : env.getRequiredProperty("B2BWEB_INTERNAL_TOKENS").split(","))
        if (token.length() < 32 || token.startsWith("dev") || token.startsWith("test"))
          throw new IllegalStateException("内部令牌不符合安全要求");
      if (env.getProperty("B2BWEB_NODE_ID") == null) throw new IllegalStateException("必须配置唯一节点编号");
      for (String capability : List.of("object-storage", "translation-engine", "virus-scanner")) {
        String type = env.getRequiredProperty("b2bweb.infra." + capability + ".type");
        if (Set.of("fake", "local").contains(type)) throw new IllegalStateException("生产环境禁止开发适配器");
      }
      for (String name : List.of("b2bweb.dev.stand-ins.enabled", "b2bweb.dev.seed.enabled"))
        if (env.getProperty(name, Boolean.class, false))
          throw new IllegalStateException("生产环境禁止开发开关");
      if (env.getProperty("b2bweb.infra.mail.security", "NONE").equals("NONE"))
        throw new IllegalStateException("生产SMTP必须启用TLS");
    };
  }

  private static ClassLoader getClassLoader() {
    return ProductionSafety.class.getClassLoader();
  }
}
