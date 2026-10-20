package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.core.security.ProductionSafety;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyTest {
  MockEnvironment valid() {
    var e = new MockEnvironment();
    e.setActiveProfiles("prod");
    byte[] key = new byte[32];
    Arrays.fill(key, (byte) 37);
    return e.withProperty("B2BWEB_SECRET_KEYS", "k1:" + Base64.getEncoder().encodeToString(key))
        .withProperty("B2BWEB_DB_USER", "app")
        .withProperty("B2BWEB_DB_MIGRATION_USER", "migrator")
        .withProperty("B2BWEB_INTERNAL_TOKENS", "a-unique-token-of-at-least-thirty-two-bytes")
        .withProperty("B2BWEB_NODE_ID", "1")
        .withProperty("b2bweb.infra.object-storage.type", "s3")
        .withProperty("b2bweb.infra.translation-engine.type", "openai-compatible")
        .withProperty("b2bweb.infra.virus-scanner.type", "clamav")
        .withProperty("b2bweb.infra.mail.security", "STARTTLS");
  }

  void check(MockEnvironment env) {
    ProductionSafety.productionSafetyCheck(env)
        .postProcessBeanFactory(new DefaultListableBeanFactory());
  }

  @Test
  void productionAcceptsOnlyProductionArtifactsAndSafeConfiguration() {
    if(org.springframework.util.ClassUtils.isPresent("com.b2bweb.application.dev.LocalStandIns",getClass().getClassLoader()))assertThatThrownBy(()->check(valid())).isInstanceOf(IllegalStateException.class).hasMessageContaining("开发构件");
    else assertThatCode(() -> check(valid())).doesNotThrowAnyException();
  }

  @Test
  void productionRejectsDevelopmentProfileAndCredentials() {
    var dev = valid();
    dev.setActiveProfiles("prod", "local");
    assertThatThrownBy(() -> check(dev)).isInstanceOf(IllegalStateException.class);
    var superuser = valid().withProperty("B2BWEB_DB_USER", "root");
    assertThatThrownBy(() -> check(superuser)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void productionRejectsStartupDdlAndFakeAdapters() {
    assertThatThrownBy(
            () -> check(valid().withProperty("b2bweb.core.modules.migrate-on-startup", "true")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> check(valid().withProperty("b2bweb.infra.translation-engine.type", "fake")))
        .isInstanceOf(IllegalStateException.class);
  }
}
