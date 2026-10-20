package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.module.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties = "spring.profiles.active=test")
class ModuleLifecycleTest {
  @Autowired ModuleCatalog catalog;
  @Autowired JdbcTemplate jdbc;
  @Autowired SiteContexts sites;
  @Autowired UserContexts users;
  @Autowired PlatformTransactionManager manager;
  @Autowired IdGenerator ids;
  @Autowired ModuleMigrations migrations;

  ModuleLifecycle lifecycle(AtomicInteger installs, boolean failUpgrade) {
    List<PlatformModule> modules = new ArrayList<>();
    for (String id : catalog.order())
      modules.add(
          new PlatformModule() {
            public ModuleDescriptor descriptor() {
              return catalog.descriptor(id);
            }

            public void onInstall(ModuleContext context) {
              if (id.equals("tracking")) {
                assertThat(sites.current()).isEmpty();
                assertThat(users.actor()).isEqualTo(ActorRef.SYSTEM);
                installs.incrementAndGet();
                try {
                  Thread.sleep(30);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new RuntimeException(e);
                }
              }
            }

            public void onUpgrade(ModuleContext context, SemVer from) {
              if (id.equals("tracking") && failUpgrade) {
                jdbc.update(
                    "INSERT INTO sys_dict_type(id,type_code,module_id,name)"
                        + " VALUES(77901,'core.failedLifecycle','core','应回滚')");
                throw new IllegalStateException("受控升级失败");
              }
            }
          });
    return new ModuleLifecycle(jdbc, catalog, modules, sites, users, manager, ids, migrations);
  }

  @AfterEach
  void restore() {
    jdbc.update(
        "UPDATE sys_module SET"
            + " installed_version='1.0.0',target_version=NULL,status='INSTALLED',last_error=NULL"
            + " WHERE module_id='tracking'");
    jdbc.update("DELETE FROM sys_dict_type WHERE type_code='core.failedLifecycle'");
  }

  @Test
  void concurrentInstancesRunInstallCallbackOnlyOnce() throws Exception {
    jdbc.update(
        "UPDATE sys_module SET installed_version=NULL,status='INSTALLING' WHERE"
            + " module_id='tracking'");
    var count = new AtomicInteger();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(() -> lifecycle(count, false).run(null));
      var second = executor.submit(() -> lifecycle(count, false).run(null));
      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);
    }
    assertThat(count).hasValue(1);
  }

  @Test
  void failedUpgradeRollsBackCallbackDataAndMarksFailed() {
    jdbc.update(
        "UPDATE sys_module SET installed_version='0.9.0',status='UPGRADING' WHERE"
            + " module_id='tracking'");
    assertThatThrownBy(() -> lifecycle(new AtomicInteger(), true).run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("tracking");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM sys_module WHERE module_id='tracking'", String.class))
        .isEqualTo("FAILED");
    assertThat(
            jdbc.queryForObject(
                "SELECT installed_version FROM sys_module WHERE module_id='tracking'",
                String.class))
        .isEqualTo("0.9.0");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type WHERE type_code='core.failedLifecycle'",
                Integer.class))
        .isZero();
  }
  @Test
  void independentMigrateIsIdempotentAndKeepsInstalledModulesAvailable(){
    var env=new HashMap<>(System.getenv());env.put("B2BWEB_NODE_ID","1023");
    com.b2bweb.core.PlatformCommands.migrate(env);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_module WHERE status='INSTALLED'",Integer.class)).isEqualTo(Integer.getInteger("b2b.expected-module-count",17)+1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_module WHERE target_version IS NOT NULL",Integer.class)).isZero();
  }
}
