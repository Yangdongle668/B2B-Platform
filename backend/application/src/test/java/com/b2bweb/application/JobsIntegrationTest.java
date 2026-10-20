package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.Extension;
import com.b2bweb.core.context.*;
import com.b2bweb.core.job.DatabaseJobs;
import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(JobsIntegrationTest.Fixtures.class)
class JobsIntegrationTest extends com.b2bweb.testkit.contract.job.JobSubmissionContract {
  protected JobApi api() {
    return jobs;
  }

  protected <T> T atSite(long site, java.util.function.Supplier<T> action) {
    return sites.callAs(site, action);
  }

  protected void rollback(Runnable action) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              action.run();
              status.setRollbackOnly();
            });
  }

  @Autowired DatabaseJobs jobs;
  @Autowired SiteContexts sites;
  @Autowired UserContexts users;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  @Autowired S3Client s3;
  final JsonMapper json = JsonMapper.builder().build();

  @TestConfiguration
  static class Fixtures {
    @Bean
    ProbeHandler probeHandler(SiteContexts sites, UserContexts users) {
      return new ProbeHandler(sites, users);
    }
  }

  @Extension(module = "core")
  static class ProbeHandler implements JobHandler {
    private final SiteContexts sites;
    private final UserContexts users;

    ProbeHandler(SiteContexts sites, UserContexts users) {
      this.sites = sites;
      this.users = users;
    }

    public String jobType() {
      return "core.m0Probe";
    }

    public Duration heartbeatTimeout() {
      return Duration.ofSeconds(1);
    }

    public JobOutcome execute(JobExecutionContext ctx) throws Exception {
      ctx.checkpoint();
      assertThat(sites.require().origin()).isEqualTo(com.b2bweb.api.site.ContextOrigin.JOB);
      assertThat(sites.requireSiteId()).isEqualTo(ctx.siteId());
      assertThat(users.actor().userId()).isEqualTo(ctx.initiator().userId());
      assertThat(users.current()).isEmpty();
      assertThat(
              org.springframework.transaction.support.TransactionSynchronizationManager
                  .isActualTransactionActive())
          .isFalse();
      if (ctx.params().path("fail").asBoolean())
        throw new BusinessException(ErrorCode.of("CORE_PROBE_FAILURE", 422, "测试失败"), "受控失败");
      if (ctx.params().has("fileKey"))
        try (var input = ctx.openInputFile(ctx.params().path("fileKey").asString())) {
          assertThat(input.readAllBytes()).isEqualTo(new byte[] {1, 2, 3});
        }
      if (ctx.params().path("resultFile").asBoolean())
        ctx.writeResultFile("结果.txt", "text/plain", out -> out.write("ok".getBytes()));
      ctx.progress(1, 1, 0, "已完成");
      return new JobOutcome(ctx.params());
    }
  }

  @BeforeEach
  void seed() {
    jdbc.update("DELETE FROM sys_job WHERE job_type='core.m0Probe'");
    for (long site : List.of(721L, 722L))
      jdbc.update(
          "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
              + " VALUES(?,?,'任务测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
              + " status='ACTIVE'",
          site,
          "jobs-" + site);
  }

  JobRequest request(String key, String payload, int attempts) {
    return new JobRequest(
        "core", "core.m0Probe", "测试任务", json.readTree(payload), key, null, null, attempts);
  }

  @Test
  void submissionRollsBackWithSourceAndCommittedTaskIsIdempotent() {
    sites.runAs(
        721,
        () -> {
          var tx = new TransactionTemplate(manager);
          long[] id = {0};
          tx.executeWithoutResult(
              status -> {
                id[0] = jobs.submit(request("rollback", "{}", 1)).jobId();
                status.setRollbackOnly();
              });
          assertThat(jobs.find(id[0])).isEmpty();
          var first = jobs.submit(request("same", "{\"a\":1,\"b\":2}", 1));
          assertThat(jobs.submit(request("same", "{\"b\":2,\"a\":1}", 1)).jobId())
              .isEqualTo(first.jobId());
          assertThatThrownBy(() -> jobs.submit(request("same", "{\"a\":2}", 1)))
              .isInstanceOf(BusinessException.class)
              .extracting(e -> ((BusinessException) e).errorCode().code())
              .isEqualTo("CORE_IDEMPOTENCY_KEY_REUSED");
        });
  }

  @Test
  void siteScopeIncludesParentsAndCannotBeForgedById() {
    long id = sites.callAs(721, () -> jobs.submit(request("site", "{}", 1)).jobId());
    sites.runAs(
        722,
        () -> {
          assertThat(jobs.find(id)).isEmpty();
          assertThatThrownBy(() -> jobs.requestCancel(id)).isInstanceOf(NotFoundException.class);
          var child =
              new JobRequest(
                  "core", "core.m0Probe", "子任务", json.createObjectNode(), null, id, null, 1);
          assertThatThrownBy(() -> jobs.submit(child)).isInstanceOf(NotFoundException.class);
        });
  }

  @Test
  void pendingCancellationAndStateTransitionsAreExplicit() {
    sites.runAs(
        721,
        () -> {
          long id = jobs.submit(request(null, "{}", 1)).jobId();
          jobs.requestCancel(id);
          assertThat(jobs.find(id).orElseThrow().status()).isEqualTo(JobStatus.CANCELED);
          assertThatThrownBy(() -> jobs.retry(id)).isInstanceOf(BusinessException.class);
          assertThat(jobs.claim("worker")).isEmpty();
        });
  }

  @Test
  void claimedJobRestoresSiteAndActorWithoutOpeningHandlerTransaction() throws Exception {
    long id;
    try (var actor = users.asActor(ActorRef.user(817, "发起人"))) {
      id = sites.callAs(721, () -> jobs.submit(request(null, "{\"ok\":true}", 1)).jobId());
    }
    var claim = jobs.claim("worker").orElseThrow();
    assertThat(jobs.claim("other")).isEmpty();
    jobs.execute(claim);
    var result = sites.callAs(721, () -> jobs.find(id).orElseThrow());
    assertThat(result.status()).isEqualTo(JobStatus.SUCCEEDED);
    assertThat(result.createdBy()).isEqualTo(817);
    assertThat(result.progress().done()).isEqualTo(1);
    assertThat(sites.current()).isEmpty();
    assertThat(users.actor()).isEqualTo(ActorRef.SYSTEM);
  }

  @Test
  void runningCancellationIsCooperativeAndNeverPublishesSuccess() {
    long id = sites.callAs(721, () -> jobs.submit(request(null, "{}", 1)).jobId());
    var claim = jobs.claim("worker").orElseThrow();
    sites.runAs(721, () -> jobs.requestCancel(id));
    jobs.execute(claim);
    assertThat(sites.callAs(721, () -> jobs.find(id).orElseThrow().status()))
        .isEqualTo(JobStatus.CANCELED);
  }

  @Test
  void reaperFencesOldClaimEvenWhenWorkerIdIsReused() {
    long id = sites.callAs(721, () -> jobs.submit(request(null, "{}", 2)).jobId());
    var old = jobs.claim("same-worker").orElseThrow();
    jdbc.update(
        "UPDATE sys_job SET heartbeat_at=TIMESTAMPADD(SECOND,-10,CURRENT_TIMESTAMP(3)) WHERE id=?",
        id);
    jobs.reap();
    var current = jobs.claim("same-worker").orElseThrow();
    assertThat(current.version()).isGreaterThan(old.version());
    jobs.execute(old);
    assertThat(sites.callAs(721, () -> jobs.find(id).orElseThrow().status()))
        .isEqualTo(JobStatus.RUNNING);
    jobs.execute(current);
    assertThat(sites.callAs(721, () -> jobs.find(id).orElseThrow().status()))
        .isEqualTo(JobStatus.SUCCEEDED);
  }

  @Test
  void businessFailureHasStableCodeAndManualRetryPreservesAttempts() {
    long id = sites.callAs(721, () -> jobs.submit(request(null, "{\"fail\":true}", 1)).jobId());
    jobs.execute(jobs.claim("worker").orElseThrow());
    sites.runAs(
        721,
        () -> {
          var failed = jobs.find(id).orElseThrow();
          assertThat(failed.errorCode()).isEqualTo("CORE_PROBE_FAILURE");
          assertThat(failed.status()).isEqualTo(JobStatus.FAILED);
          assertThat(jobs.retry(id).attempts()).isEqualTo(1);
          assertThat(jobs.find(id).orElseThrow().status()).isEqualTo(JobStatus.PENDING);
        });
  }

  @Test
  void inputAndResultFilesUseRealPrivateS3Objects() {
    try {
      s3.createBucket(CreateBucketRequest.builder().bucket("private-assets").build());
    } catch (S3Exception exists) {
      if (exists.statusCode() != 409) throw exists;
    }
    long id =
        sites.callAs(
            721,
            () -> {
              var stored =
                  jobs.storeInputFile(
                      "输入.bin",
                      "application/octet-stream",
                      3,
                      new ByteArrayInputStream(new byte[] {1, 2, 3}));
              return jobs.submit(
                      request(
                          null,
                          "{\"fileKey\":\"" + stored.fileKey() + "\",\"resultFile\":true}",
                          1))
                  .jobId();
            });
    jobs.execute(jobs.claim("worker").orElseThrow());
    sites.runAs(
        721,
        () -> {
          var result = jobs.find(id).orElseThrow();
          assertThat(result.status()).isEqualTo(JobStatus.SUCCEEDED);
          assertThat(result.resultFile().size()).isEqualTo(2);
          assertThat(jobs.resultDownloadUrl(id, Duration.ofMinutes(1)).toString())
              .contains("X-Amz-Signature");
        });
  }
}
