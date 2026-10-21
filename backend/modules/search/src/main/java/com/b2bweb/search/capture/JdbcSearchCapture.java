package com.b2bweb.search.capture;

import com.b2bweb.api.common.IdGenerator;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Transaction-bound source journal; generation workers share the same state-row barrier. */
@Extension(module = "search")
public class JdbcSearchCapture implements SearchChangeCapture {
  private final JdbcTemplate jdbc;
  private final SiteContextProvider sites;
  private final IdGenerator ids;

  public JdbcSearchCapture(JdbcTemplate jdbc, SiteContextProvider sites, IdGenerator ids) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.ids = ids;
  }

  public void append(Set<String> locales, Set<Long> localizationIds, boolean full) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("搜索捕获必须加入源事务");
    if (full && !localizationIds.isEmpty() || localizationIds.size() > 500)
      throw new IllegalArgumentException("搜索捕获范围无效");
    long site = sites.requireSiteId();
    var json = tools.jackson.databind.json.JsonMapper.builder().build();
    var values = json.createArrayNode();
    localizationIds.stream().sorted().forEach(id -> values.add(Long.toString(id)));
    for (String locale : new TreeSet<>(locales)) {
      jdbc.update(
          "INSERT INTO sch_index_state(id,site_id,locale) VALUES(?,?,?) ON DUPLICATE KEY UPDATE"
              + " id=id",
          ids.nextId(),
          site,
          locale);
      long sequence =
          jdbc.queryForObject(
                  "SELECT change_seq FROM sch_index_state WHERE site_id=? AND locale=? FOR UPDATE",
                  Long.class,
                  site,
                  locale)
              + 1;
      jdbc.update(
          "UPDATE sch_index_state SET change_seq=? WHERE site_id=? AND locale=?",
          sequence,
          site,
          locale);
      jdbc.update(
          "INSERT INTO sch_change(id,site_id,locale,sequence_no,localization_ids,full_refresh)"
              + " VALUES(?,?,?,?,CAST(? AS JSON),?)",
          ids.nextId(),
          site,
          locale,
          sequence,
          values.toString(),
          full);
    }
  }
}
