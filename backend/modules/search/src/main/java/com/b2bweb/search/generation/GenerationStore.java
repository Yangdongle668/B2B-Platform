package com.b2bweb.search.generation;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

/**
 * Formal database barrier for private generations. No remote I/O occurs under these transactions.
 */
@Component
public final class GenerationStore {
  public record Build(
      long id,
      long site,
      String locale,
      String index,
      String base,
      long activeVersion,
      long fence,
      long start) {}

  public record Change(long sequence, boolean full, Set<Long> ids) {
    public Change {
      ids = Set.copyOf(ids);
    }
  }

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final SiteContextProvider sites;
  private final IdGenerator ids;

  public GenerationStore(
      JdbcTemplate jdbc,
      PlatformTransactionManager manager,
      SiteContextProvider sites,
      IdGenerator ids) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
    this.sites = sites;
    this.ids = ids;
  }

  private void outside() {
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("索引构建不得加入外部事务");
  }

  public Build begin(String locale, long job, boolean copy) {
    outside();
    if (locale == null || !locale.matches("[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*") || job <= 0)
      throw new IllegalArgumentException("构建参数无效");
    long site = sites.requireSiteId();
    return tx.execute(
        status -> {
          jdbc.update(
              "INSERT INTO sch_index_state(id,site_id,locale) VALUES(?,?,?) ON DUPLICATE KEY UPDATE"
                  + " id=id",
              ids.nextId(),
              site,
              locale);
          var state =
              jdbc.queryForMap(
                  "SELECT * FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",
                  site,
                  locale);
          long fence = number(state, "build_fence") + 1,
              version = number(state, "active_version"),
              id = ids.nextId();
          String base = copy ? (String) state.get("active_index") : null;
          long start = base == null ? number(state, "change_seq") : number(state, "applied_seq");
          String index = com.b2bweb.api.port.SearchIndexNames.content(site, locale) + "_g_" + id;
          jdbc.update(
              "UPDATE sch_build SET status='ABANDONED' WHERE site_id=? AND locale=? AND status IN"
                  + " ('BUILDING','READY')",
              site,
              locale);
          jdbc.update(
              "UPDATE sch_index_state SET build_fence=? WHERE site_id=? AND locale=?",
              fence,
              site,
              locale);
          jdbc.update(
              "INSERT INTO"
                  + " sch_build(id,site_id,locale,job_id,physical_index,base_index,expected_active_version,build_fence,start_seq,applied_seq)"
                  + " VALUES(?,?,?,?,?,?,?,?,?,?)",
              id,
              site,
              locale,
              job,
              index,
              base,
              version,
              fence,
              start,
              start);
          return new Build(id, site, locale, index, base, version, fence, start);
        });
  }

  public long highWater(Build build) {
    checkSite(build);
    return jdbc.queryForObject(
        "SELECT change_seq FROM sch_index_state WHERE site_id=? AND locale=?",
        Long.class,
        build.site(),
        build.locale());
  }

  public List<Change> changes(Build build, long after, long through) {
    checkSite(build);
    return jdbc.query(
        "SELECT sequence_no,full_refresh,localization_ids FROM sch_change WHERE site_id=? AND"
            + " locale=? AND sequence_no>? AND sequence_no<=? ORDER BY sequence_no LIMIT 500",
        (rs, n) -> {
          var values =
              tools.jackson.databind.json.JsonMapper.builder().build().readTree(rs.getString(3));
          Set<Long> found = new HashSet<>();
          values.forEach(v -> found.add(Long.parseLong(v.asString())));
          return new Change(rs.getLong(1), rs.getBoolean(2), found);
        },
        build.site(),
        build.locale(),
        after,
        through);
  }

  public void task(Build build, long task) {
    checkSite(build);
    int changed =
        jdbc.update(
            "UPDATE sch_build SET last_engine_task_id=?,heartbeat_at=CURRENT_TIMESTAMP(3) WHERE"
                + " id=? AND site_id=? AND status='BUILDING' AND build_fence=?",
            task,
            build.id(),
            build.site(),
            build.fence());
    if (changed != 1) throw new IllegalStateException("构建已失效");
  }

  public void checkpoint(Build build, long sequence) {
    checkSite(build);
    int changed =
        jdbc.update(
            "UPDATE sch_build SET applied_seq=?,heartbeat_at=CURRENT_TIMESTAMP(3) WHERE id=? AND"
                + " site_id=? AND status='BUILDING' AND build_fence=? AND applied_seq<=?",
            sequence,
            build.id(),
            build.site(),
            build.fence(),
            sequence);
    if (changed != 1) throw new IllegalStateException("构建已失效或水位倒退");
  }

  /**
   * Returns false for a new source commit or a successor attempt; never publishes across either
   * barrier.
   */
  public boolean publish(Build build, long sequence) {
    outside();
    checkSite(build);
    return Boolean.TRUE.equals(
        tx.execute(
            status -> {
              var state =
                  jdbc.queryForMap(
                      "SELECT * FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",
                      build.site(),
                      build.locale());
              if (build.index().equals(state.get("active_index"))) return true;
              if (number(state, "change_seq") != sequence
                  || number(state, "active_version") != build.activeVersion()
                  || number(state, "build_fence") != build.fence()) return false;
              int ready =
                  jdbc.update(
                      "UPDATE sch_build SET status='READY' WHERE id=? AND site_id=? AND"
                          + " status='BUILDING' AND applied_seq=? AND build_fence=?",
                      build.id(),
                      build.site(),
                      sequence,
                      build.fence());
              if (ready != 1) return false;
              jdbc.update(
                  "UPDATE sch_build SET status='RETIRED' WHERE site_id=? AND locale=? AND"
                      + " status='ACTIVE'",
                  build.site(),
                  build.locale());
              jdbc.update(
                  "UPDATE sch_index_state SET"
                      + " active_index=?,active_version=active_version+1,applied_seq=? WHERE"
                      + " site_id=? AND locale=?",
                  build.index(),
                  sequence,
                  build.site(),
                  build.locale());
              jdbc.update(
                  "UPDATE sch_build SET status='ACTIVE' WHERE id=? AND site_id=?",
                  build.id(),
                  build.site());
              return true;
            }));
  }

  public boolean owned(Build build) {
    checkSite(build);
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sch_build b JOIN sch_index_state s ON s.site_id=b.site_id AND"
                + " s.locale=b.locale WHERE b.id=? AND b.site_id=? AND b.status='BUILDING' AND"
                + " s.build_fence=b.build_fence",
            Integer.class,
            build.id(),
            build.site())
        == 1;
  }

  public void abandon(Build build) {
    checkSite(build);
    jdbc.update(
        "UPDATE sch_build SET status='ABANDONED' WHERE id=? AND site_id=? AND status IN"
            + " ('BUILDING','READY')",
        build.id(),
        build.site());
  }

  public Optional<String> active(String locale) {
    return jdbc
        .query(
            "SELECT active_index FROM sch_index_state WHERE site_id=? AND locale=?",
            (rs, n) -> Optional.ofNullable(rs.getString(1)),
            sites.requireSiteId(),
            locale)
        .stream()
        .findFirst()
        .orElse(Optional.empty());
  }

  /**
   * Retired targets cannot be adopted by new builds; new copy bases are always the active pointer.
   */
  public List<String> garbage(String locale, Duration retention) {
    outside();
    if (retention.compareTo(Duration.ofMinutes(5)) < 0)
      throw new IllegalArgumentException("代次至少保留5分钟");
    long site = sites.requireSiteId();
    return jdbc.query(
        "SELECT b.physical_index FROM sch_build b WHERE b.site_id=? AND b.locale=? AND b.status IN"
            + " ('RETIRED','ABANDONED','FAILED') AND"
            + " b.updated_at<TIMESTAMPADD(SECOND,?,CURRENT_TIMESTAMP(3)) AND NOT EXISTS(SELECT 1"
            + " FROM sch_index_state s WHERE s.site_id=b.site_id AND"
            + " s.active_index=b.physical_index) AND NOT EXISTS(SELECT 1 FROM (SELECT base_index"
            + " FROM sch_build WHERE site_id=? AND status IN ('BUILDING','READY')) live WHERE"
            + " live.base_index=b.physical_index)",
        (rs, n) -> rs.getString(1),
        site,
        locale,
        -retention.toSeconds(),
        site);
  }

  public int pruneChanges(String locale, int maximum) {
    outside();
    if (maximum < 1 || maximum > 1000) throw new IllegalArgumentException("清理批次无效");
    long site = sites.requireSiteId();
    return tx.execute(
        status -> {
          var state =
              jdbc.queryForMap(
                  "SELECT applied_seq FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",
                  site,
                  locale);
          Long start =
              jdbc.queryForObject(
                  "SELECT MIN(start_seq) FROM sch_build WHERE site_id=? AND locale=? AND status IN"
                      + " ('BUILDING','READY')",
                  Long.class,
                  site,
                  locale);
          long watermark = number(state, "applied_seq");
          if (start != null) watermark = Math.min(watermark, start);
          return jdbc.update(
              "DELETE FROM sch_change WHERE site_id=? AND locale=? AND sequence_no<=? ORDER BY"
                  + " sequence_no LIMIT ?",
              site,
              locale,
              watermark,
              maximum);
        });
  }

  private void checkSite(Build build) {
    if (sites.requireSiteId() != build.site()) throw new IllegalArgumentException("构建不属于当前站点");
  }

  private static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
