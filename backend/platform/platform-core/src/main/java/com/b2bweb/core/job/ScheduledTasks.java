package com.b2bweb.core.job;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import jakarta.annotation.PreDestroy;
import java.time.*;
import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "b2bweb.core.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ScheduledTasks implements ApplicationRunner {
  private final ExtensionRegistry extensions;
  private final SiteApi api;
  private final SiteContexts sites;
  private final UserContexts users;
  private final Clock clock;
  private final io.micrometer.core.instrument.MeterRegistry metrics;
  private final java.util.concurrent.ConcurrentHashMap<String,java.util.concurrent.atomic.AtomicLong> lastSuccess=new java.util.concurrent.ConcurrentHashMap<>();
  private final LockingTaskExecutor locks;
  private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

  public ScheduledTasks(
      ExtensionRegistry extensions,
      SiteApi api,
      SiteContexts sites,
      UserContexts users,
      Clock clock,
      JdbcTemplate jdbc) {
    this(extensions,api,sites,users,clock,jdbc,new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
  }
  @org.springframework.beans.factory.annotation.Autowired
  public ScheduledTasks(ExtensionRegistry extensions,SiteApi api,SiteContexts sites,UserContexts users,Clock clock,JdbcTemplate jdbc,io.micrometer.core.instrument.MeterRegistry metrics) {
    this.metrics=metrics;
    this.extensions = extensions;
    this.api = api;
    this.sites = sites;
    this.users = users;
    this.clock = clock;
    locks =
        new DefaultLockingTaskExecutor(
            new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                    .withJdbcTemplate(jdbc)
                    .withTableName("sys_lock")
                    .withColumnNames(
                        new JdbcTemplateLockProvider.ColumnNames(
                            "lock_name", "lock_until", "locked_at", "locked_by"))
                    .usingDbTime()
                    .build()));
    scheduler.setPoolSize(4);
    scheduler.setThreadNamePrefix("platform-scheduled-");
    scheduler.initialize();
  }

  public void run(ApplicationArguments args) {
    for (var task : extensions.allRegardlessOfSite(ScheduledTask.class)) {
      scheduler.schedule(() -> runOnce(task), new CronTrigger(task.cron(), ZoneOffset.UTC));
    }
  }

  public void runOnce(ScheduledTask task) {
    Instant scheduled = clock.instant();
    locks.executeWithLock(
        (Runnable)
            () -> {
              try (var actor = users.asActor(ActorRef.SYSTEM)) {
                if (task.scope() == ScheduleScope.SYSTEM) invoke(task, null, scheduled);
                else
                  for (var site : api.listOperationalSites())
                    if (api.isModuleEnabled(site.siteId(), extensions.moduleOf(task)))
                      sites.runAs(
                          site.siteId(),
                          () -> {
                            var s = sites.require();
                            try (var context =
                                sites.open(
                                    new SiteContext(
                                        s.siteId(),
                                        s.siteCode(),
                                        s.sourceLocale(),
                                        s.defaultLocale(),
                                        s.timezone(),
                                        null,
                                        ContextOrigin.SCHEDULED))) {
                              invoke(task, site.siteId(), scheduled);
                            }
                          });
              } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(getClass())
                    .error("定时任务上下文失败 task={}", task.name(), e);
              }
            },
        new LockConfiguration(scheduled, task.name(), task.lockAtMostFor(), Duration.ofSeconds(5)));
  }

  private void invoke(ScheduledTask task, Long site, Instant scheduled) {
    var sample=io.micrometer.core.instrument.Timer.start(metrics);String outcome="success";
    try {
      task.run(new ScheduledTaskContext(site, scheduled, clock.instant()));
      lastSuccess.computeIfAbsent(task.name(),name->{var value=new java.util.concurrent.atomic.AtomicLong();io.micrometer.core.instrument.Gauge.builder("b2bweb.core.scheduled.last_success",value,java.util.concurrent.atomic.AtomicLong::doubleValue).tag("task",name).register(metrics);return value;}).set(clock.instant().getEpochSecond());
    } catch (Exception e) {
      outcome="failure";
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("定时任务失败 task={} site={}", task.name(), site, e);
    } finally {sample.stop(metrics.timer("b2bweb.core.scheduled.duration","task",task.name(),"outcome",outcome));}
  }

  @PreDestroy
  public void close() {
    scheduler.shutdown();
  }
}
