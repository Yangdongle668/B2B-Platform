package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.job.ScheduledTasks;
import com.b2bweb.core.module.Extensions;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class ScheduledTasksTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired SiteContexts sites;
  @Autowired UserContexts users;
  @Autowired SiteApi api;

  @Extension(module = "tracking")
  static class Probe implements ScheduledTask {
    final List<Long> visited = new ArrayList<>();
    final String name;
    final SiteContexts sites;
    final UserContexts users;
    boolean failFirst;

    Probe(String name, SiteContexts sites, UserContexts users) {
      this.name = name;
      this.sites = sites;
      this.users = users;
    }

    public String name() {
      return name;
    }

    public String cron() {
      return "0 * * * * *";
    }

    public ScheduleScope scope() {
      return ScheduleScope.PER_SITE;
    }

    public void run(ScheduledTaskContext ctx) {
      assertThat(sites.require().origin()).isEqualTo(ContextOrigin.SCHEDULED);
      assertThat(users.actor()).isEqualTo(ActorRef.SYSTEM);
      visited.add(ctx.siteId());
      if (failFirst && ctx.siteId() == 741) throw new IllegalStateException("受控失败");
    }
  }

  @BeforeEach
  void seed() {
    for (long site : List.of(741L, 742L, 743L)) {
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'调度测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          site,
          "schedules-" + site);
      jdbc.update(
          "INSERT INTO sys_site_module(id,site_id,module_id,enabled) VALUES(?,?,'tracking',?) ON"
              + " DUPLICATE KEY UPDATE enabled=VALUES(enabled)",
          site * 10,
          site,
          site != 742);
    }
    jdbc.update("DELETE FROM sys_lock WHERE lock_name LIKE 'tracking.m0%'");
  }

  @Test
  void schedulerUsesSiteAndSystemActorSkipsDisabledAndContinuesAfterFailure() {
    var task = new Probe("tracking.m0Sites", sites, users);
    task.failFirst = true;
    var extensions = new Extensions(List.of(task), sites, api::isModuleEnabled);
    var runner = new ScheduledTasks(extensions, api, sites, users, Clock.systemUTC(), jdbc);
    try {
      runner.runOnce(task);
      assertThat(task.visited).contains(741L, 743L).doesNotContain(742L);
      assertThat(sites.current()).isEmpty();
      assertThat(users.current()).isEmpty();
    } finally {
      runner.close();
    }
  }

  @Test
  void twoInstancesShareDatabaseShedLockWithMinimumHold() {
    var task = new Probe("tracking.m0Lock", sites, users);
    var extensions = new Extensions(List.of(task), sites, api::isModuleEnabled);
    var first = new ScheduledTasks(extensions, api, sites, users, Clock.systemUTC(), jdbc);
    var second = new ScheduledTasks(extensions, api, sites, users, Clock.systemUTC(), jdbc);
    try {
      first.runOnce(task);
      long seen = task.visited.size();
      assertThat(seen).isPositive();
      second.runOnce(task);
      assertThat(task.visited).hasSize((int) seen);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM sys_lock WHERE lock_name='tracking.m0Lock' AND"
                      + " lock_until>CURRENT_TIMESTAMP(3)",
                  Integer.class))
          .isEqualTo(1);
    } finally {
      first.close();
      second.close();
    }
  }
}
