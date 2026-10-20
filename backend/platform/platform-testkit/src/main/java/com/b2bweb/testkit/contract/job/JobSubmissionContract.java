package com.b2bweb.testkit.contract.job;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The same submission contract is run against the JDBC provider and the in-memory queue. */
public abstract class JobSubmissionContract {
  protected abstract JobApi api();

  protected abstract <T> T atSite(long site, Supplier<T> action);

  protected abstract void rollback(Runnable action);

  protected JobRequest sample(String key, String params) {
    return new JobRequest(
        "core",
        "core.m0Probe",
        "契约测试",
        JsonMapper.builder().build().readTree(params),
        key,
        null,
        null,
        1);
  }

  @Test
  public void contractIdempotencyUsesCanonicalJsonRatherThanPropertyOrder() {
    atSite(
        721,
        () -> {
          long id = api().submit(sample("canonical", "{\"a\":1,\"b\":2}")).jobId();
          assertThat(api().submit(sample("canonical", "{\"b\":2,\"a\":1}")).jobId()).isEqualTo(id);
          assertThatThrownBy(() -> api().submit(sample("canonical", "{\"a\":3}")))
              .isInstanceOf(BusinessException.class);
          return null;
        });
  }

  @Test
  public void contractTasksDisappearWhenSourceTransactionRollsBack() {
    atSite(
        721,
        () -> {
          long[] id = {0};
          rollback(() -> id[0] = api().submit(sample("source-rollback", "{}")).jobId());
          assertThat(api().find(id[0])).isEmpty();
          return null;
        });
  }

  @Test
  public void contractCurrentSiteFiltersFindAndCancel() {
    long id = atSite(721, () -> api().submit(sample("isolated", "{}")).jobId());
    atSite(
        722,
        () -> {
          assertThat(api().find(id)).isEmpty();
          assertThatThrownBy(() -> api().requestCancel(id)).isInstanceOf(NotFoundException.class);
          return null;
        });
  }

  @Test
  public void contractPendingCancelCannotBeRetriedAsFailure() {
    atSite(
        721,
        () -> {
          long id = api().submit(sample("cancel", "{}")).jobId();
          api().requestCancel(id);
          assertThat(api().find(id).orElseThrow().status()).isEqualTo(JobStatus.CANCELED);
          assertThatThrownBy(() -> api().retry(id)).isInstanceOf(BusinessException.class);
          return null;
        });
  }

  @Test
  public void contractUnknownTypeFailsAndListSortIsWhitelisted() {
    atSite(
        721,
        () -> {
          assertThatThrownBy(
                  () ->
                      api()
                          .submit(
                              new JobRequest(
                                  "core", "core.unknown", "未知任务", null, null, null, null, 1)))
              .isInstanceOf(BusinessException.class);
          assertThatThrownBy(
                  () ->
                      api()
                          .list(
                              new JobQuery(null, null, Set.of(), null),
                              new PageRequest(1, 10, List.of(SortOrder.asc("unsafe SQL")))))
              .isInstanceOf(IllegalArgumentException.class);
          return null;
        });
  }

  @Test
  public void contractReturnedJsonIsDetachedFromStoredResult() {
    atSite(
        721,
        () -> {
          var request = sample("json", "{\"input\":1}");
          long id = api().submit(request).jobId();
          ((tools.jackson.databind.node.ObjectNode) request.params()).put("input", 2);
          assertThat(api().submit(sample("json", "{\"input\":1}")).jobId()).isEqualTo(id);
          return null;
        });
  }
}
