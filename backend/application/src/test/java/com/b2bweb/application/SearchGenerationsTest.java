package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.core.context.SiteContexts;
import com.b2bweb.search.capture.JdbcSearchCapture;
import com.b2bweb.search.generation.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
class SearchGenerationsTest {
  @Autowired SearchEngineClient engine;
  @Autowired GenerationStore store;
  @Autowired JdbcSearchCapture capture;
  @Autowired JdbcTemplate jdbc;
  @Autowired SiteContexts sites;
  @Autowired PlatformTransactionManager manager;

  @BeforeEach
  void seed() {
    jdbc.update(
        "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
            + " VALUES(779,'generations','代次测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY"
            + " UPDATE name='代次测试'");
    jdbc.update("DELETE FROM sch_build WHERE site_id=779");
    jdbc.update("DELETE FROM sch_change WHERE site_id=779");
    jdbc.update("DELETE FROM sch_index_state WHERE site_id=779");
  }

  void change() {
    new TransactionTemplate(manager)
        .executeWithoutResult(status -> capture.append(Set.of("zh-CN"), Set.of(11L), false));
  }

  @AfterEach
  void cleanEngine() {
    for (String index : engine.listIndexes("s779_")) engine.deleteIndex(index);
  }

  @Test
  void sourceCommitAndSuccessorFenceBothBlockStalePublication() {
    sites.runAs(
        779,
        () -> {
          var old = store.begin("zh-CN", 1, false);
          change();
          assertThat(store.publish(old, old.start())).isFalse();
          store.checkpoint(old, 1);
          assertThat(store.publish(old, 1)).isTrue();
          assertThat(store.active("zh-CN")).contains(old.index());
          var crashed = store.begin("zh-CN", 2, true);
          assertThat(crashed.base()).isEqualTo(old.index());
          assertThat(crashed.start()).isEqualTo(1);
          var successor = store.begin("zh-CN", 3, false);
          assertThat(store.publish(crashed, 1)).isFalse();
          store.checkpoint(successor, 1);
          assertThat(store.publish(successor, 1)).isTrue();
          assertThat(store.active("zh-CN")).contains(successor.index());
          assertThat(store.publish(successor, 1)).isTrue();
        });
  }

  @Test
  void rollbackProducesNoJournalAndGcProtectsLiveCopyBaseAndRetention() {
    sites.runAs(
        779,
        () -> {
          var first = store.begin("zh-CN", 1, false);
          new TransactionTemplate(manager)
              .executeWithoutResult(
                  status -> {
                    capture.append(Set.of("zh-CN"), Set.of(1L), false);
                    status.setRollbackOnly();
                  });
          assertThat(store.highWater(first)).isZero();
          store.checkpoint(first, 0);
          assertThat(store.publish(first, 0)).isTrue();
          change();
          var copy = store.begin("zh-CN", 2, true);
          assertThat(copy.start()).isZero();
          assertThat(store.pruneChanges("zh-CN", 1000)).isZero();
          assertThat(store.garbage("zh-CN", Duration.ofMinutes(5))).isEmpty();
          store.checkpoint(copy, 1);
          assertThat(store.publish(copy, 1)).isTrue();
          assertThat(store.garbage("zh-CN", Duration.ofMinutes(5))).isEmpty();
          jdbc.update(
              "UPDATE sch_build SET updated_at=TIMESTAMPADD(MINUTE,-6,CURRENT_TIMESTAMP(3)) WHERE"
                  + " id=?",
              first.id());
          assertThat(store.garbage("zh-CN", Duration.ofMinutes(5))).containsExactly(first.index());
          assertThat(store.pruneChanges("zh-CN", 1000)).isEqualTo(1);
        });
  }

  @Test
  void completeBuilderPublishesOnlyAfterEngineTasksAndLeavesActiveImmutable() {
    sites.runAs(
        779,
        () -> {
          var worker = new GenerationBuilder(store, engine);
          var json = JsonMapper.builder().build();
          var settings =
              new SearchIndexSettings(
                  "id",
                  List.of("title"),
                  List.of(),
                  List.of(),
                  List.of(),
                  null,
                  Map.of(),
                  Set.of());
          var source =
              new GenerationBuilder.Source() {
                public void scan(java.util.function.Consumer<List<JsonNode>> batch) {
                  batch.accept(
                      List.of(json.createObjectNode().put("id", "11").put("title", "published")));
                  change();
                }

                public Map<Long, JsonNode> current(Set<Long> ids) {
                  return Map.of();
                }
              };
          assertThat(worker.build("zh-CN", 1, false, settings, source, () -> false)).isTrue();
          String active = store.active("zh-CN").orElseThrow();
          assertThat(engine.count(active)).isZero();
          var failed =
              new GenerationBuilder.Source() {
                public void scan(java.util.function.Consumer<List<JsonNode>> batch) {
                  throw new IllegalStateException("controlled crash before switch");
                }

                public Map<Long, JsonNode> current(Set<Long> ids) {
                  return Map.of();
                }
              };
          assertThatThrownBy(() -> worker.build("zh-CN", 2, false, settings, failed, () -> false))
              .isInstanceOf(IllegalStateException.class);
          assertThat(store.active("zh-CN")).contains(active);
        });
  }
}
