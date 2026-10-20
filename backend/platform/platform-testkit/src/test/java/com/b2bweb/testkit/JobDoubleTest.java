package com.b2bweb.testkit;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.contract.job.JobSubmissionContract;
import com.b2bweb.testkit.fixture.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class JobDoubleTest extends JobSubmissionContract {
  TestPlatform platform;

  @BeforeEach
  void setup() {
    platform = TestPlatform.standard();
    for (long id : List.of(721L, 722L))
      platform
          .siteApi()
          .addSite(
              new SiteInfo(
                  id,
                  "s" + id,
                  "测试站点",
                  "industrial",
                  "zh-CN",
                  "zh-CN",
                  UrlStrategy.SUBDIRECTORY,
                  TrailingSlash.NEVER,
                  ZoneOffset.UTC,
                  SiteStatus.ACTIVE,
                  "example.test",
                  0));
    platform
        .jobApi()
        .register(
            "core",
            new JobHandler() {
              public String jobType() {
                return "core.m0Probe";
              }

              public JobOutcome execute(JobExecutionContext c) {
                c.checkpoint();
                return new JobOutcome(c.params());
              }
            });
  }

  protected JobApi api() {
    return platform.jobApi();
  }

  protected <T> T atSite(long site, Supplier<T> action) {
    return platform.siteContextProvider().callAs(site, action);
  }

  protected void rollback(Runnable action) {
    var manager =
        new AbstractPlatformTransactionManager() {
          protected Object doGetTransaction() {
            return new Object();
          }

          protected void doBegin(Object tx, TransactionDefinition definition) {}

          protected void doCommit(DefaultTransactionStatus status) {}

          protected void doRollback(DefaultTransactionStatus status) {}
        };
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              action.run();
              status.setRollbackOnly();
            });
  }

  @Test
  void executionIsExplicitAndRestoresCallerSite() {
    platform.useSite(721);
    long id = platform.jobApi().submit(sample("run", "{}")).jobId();
    assertThat(platform.jobApi().find(id).orElseThrow().status()).isEqualTo(JobStatus.PENDING);
    platform.useSite(722);
    assertThat(platform.jobApi().runNext()).isTrue();
    assertThat(platform.siteContextProvider().requireSiteId()).isEqualTo(722);
    platform.useSite(721);
    assertThat(platform.jobApi().find(id).orElseThrow().status()).isEqualTo(JobStatus.SUCCEEDED);
  }
}
