package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.core.context.*;
import com.b2bweb.search.capture.JdbcSearchCapture;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@SpringBootTest(properties = "spring.profiles.active=test")
class CoordinatorsTest {
  @Autowired ReferenceWriteApi refs;
  @Autowired ProjectionChangeApi changes;
  @Autowired SiteContexts sites;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  @MockitoSpyBean JdbcSearchCapture capture;

  @BeforeEach
  void seed() {
    reset(capture);
    jdbc.update(
        "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
            + " VALUES(751,'coordinator','原名','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
            + " name='原名'");
    for (String locale : List.of("zh-CN", "en"))
      jdbc.update(
          "INSERT INTO sys_site_locale(id,site_id,locale,url_prefix,hreflang) VALUES(?,751,?,?,?)"
              + " ON DUPLICATE KEY UPDATE id=id",
          locale.equals("en") ? 75102L : 75103L,
          locale,
          locale.equals("en") ? "en" : "zh",
          locale);
    jdbc.update("DELETE FROM sch_change WHERE site_id=751");
    jdbc.update("DELETE FROM sch_index_state WHERE site_id=751");
    jdbc.update("DELETE FROM mda_usage WHERE site_id=751");
    jdbc.update(
        "INSERT INTO mda_media(id,site_id,kind,storage_key,original_name,mime,size,sha256,status)"
            + " VALUES(75104,751,'IMAGE','751/test.jpg','test.jpg','image/jpeg',1,REPEAT('a',64),'READY')"
            + " ON DUPLICATE KEY UPDATE status='READY',deleted_at=NULL");
  }

  void transaction(Runnable action) {
    sites.runAs(
        751, () -> new TransactionTemplate(manager).executeWithoutResult(status -> action.run()));
  }

  @Test
  void coordinatorsRejectCallsWithoutSourceTransaction() {
    sites.runAs(
        751,
        () -> {
          assertThatThrownBy(() -> changes.searchChanged(Set.of("en"), Set.of(1L), false))
              .isInstanceOf(IllegalStateException.class);
          assertThatThrownBy(
                  () ->
                      refs.replaceMedia(
                          new MediaReferenceOwner("CONTENT", 1, "zh-CN", ReferenceStage.DRAFT),
                          List.of()))
              .isInstanceOf(IllegalStateException.class);
        });
  }

  @Test
  void searchIntentCoalescesAndRunsOnlyAfterFinalSourceMutation() {
    doAnswer(
            call -> {
              assertThat(
                      jdbc.queryForObject("SELECT name FROM sys_site WHERE id=751", String.class))
                  .isEqualTo("源已更新");
              assertThat(new ArrayList<>((Set<String>) call.getArgument(0)))
                  .containsExactly("en", "zh-CN");
              return call.callRealMethod();
            })
        .when(capture)
        .append(anySet(), anySet(), anyBoolean());
    transaction(
        () -> {
          changes.searchChanged(Set.of("zh-CN"), Set.of(1L), false);
          changes.searchChanged(Set.of("en"), Set.of(1L, 2L), false);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT COUNT(*) FROM sch_change WHERE site_id=751", Integer.class))
              .isZero();
          jdbc.update("UPDATE sys_site SET name='源已更新' WHERE id=751");
        });
    assertThat(
            jdbc.queryForList(
                "SELECT locale FROM sch_change WHERE site_id=751 ORDER BY locale", String.class))
        .containsExactly("en", "zh-CN");
    assertThat(
            jdbc.queryForList(
                "SELECT CAST(localization_ids AS CHAR) FROM sch_change WHERE site_id=751",
                String.class))
        .allSatisfy(v -> assertThat(v).contains("\"1\"", "\"2\""));
  }

  @Test
  void captureFailureRollsBackSourceAndActualMediaReferenceWritesTogether() {
    doThrow(new IllegalStateException("受控捕获失败"))
        .when(capture)
        .append(anySet(), anySet(), anyBoolean());
    assertThatThrownBy(
            () ->
                transaction(
                    () -> {
                      jdbc.update("UPDATE sys_site SET name='源已更新' WHERE id=751");
                      refs.replaceMedia(
                          new MediaReferenceOwner("CONTENT", 1, "zh-CN", ReferenceStage.DRAFT),
                          List.of(new MediaReference(75104, "card", "image")));
                      changes.searchChanged(Set.of("en"), Set.of(1L), false);
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject("SELECT name FROM sys_site WHERE id=751", String.class))
        .isEqualTo("原名");
    assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM mda_usage WHERE site_id=751", Integer.class))
        .isZero();
  }

  @Test
  void moreThan500IdsUpgradesWholeTransactionToFullRefresh() {
    transaction(
        () -> {
          for (long i = 1; i <= 501; i++) changes.searchChanged(Set.of("en"), Set.of(i), false);
        });
    assertThat(
            jdbc.queryForObject(
                "SELECT full_refresh FROM sch_change WHERE site_id=751", Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT CAST(localization_ids AS CHAR) FROM sch_change WHERE site_id=751",
                String.class))
        .isEqualTo("[]");
  }
}
