package com.b2bweb.media.reference;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * M0 transaction sink. Source owner locking is the caller's responsibility; media never reads
 * source tables.
 */
@com.b2bweb.api.extension.Extension(module = "media")
public class JdbcMediaReferences implements MediaReferenceSink {
  private final JdbcTemplate jdbc;
  private final SiteContextProvider sites;
  private final IdGenerator ids;

  public JdbcMediaReferences(JdbcTemplate jdbc, SiteContextProvider sites, IdGenerator ids) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.ids = ids;
  }

  public void replace(MediaReferenceOwner owner, List<MediaReference> references) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("媒体引用必须加入源事务");
    long site = sites.requireSiteId();
    if (owner.ownerId() <= 0
        || owner.ownerType().isBlank()
        || owner.ownerType().length() > 40
        || owner.stage() == null
        || owner.locale() != null && owner.locale().length() > 16
        || !owner.ownerType().equals("CONTENT") && owner.stage() != ReferenceStage.PUBLISHED)
      throw new IllegalArgumentException("媒体引用所有者不合法");
    var copy = new LinkedHashSet<>(references);
    for (var ref : copy)
      if (ref.mediaId() <= 0
          || ref.part().isBlank()
          || ref.part().length() > 32
          || ref.fieldPath().isBlank()
          || ref.fieldPath().length() > 255) throw new IllegalArgumentException("媒体引用字段不合法");
    for (long id : copy.stream().mapToLong(MediaReference::mediaId).distinct().sorted().toArray()) {
      var rows =
          jdbc.queryForList(
              "SELECT status,deleted_at,visibility,purge_job_id FROM mda_media WHERE site_id=? AND"
                  + " id=? FOR SHARE",
              site,
              id);
      if (rows.isEmpty()
          || rows.getFirst().get("deleted_at") != null
          || rows.getFirst().get("purge_job_id") != null)
        throw new NotFoundException(MediaErrors.NOT_FOUND, "媒体不存在或已删除");
      var row = rows.getFirst();
      if (row.get("status").equals("QUARANTINED"))
        throw new BusinessException(MediaErrors.QUARANTINED, "文件未通过病毒扫描");
      if (owner.stage() == ReferenceStage.PUBLISHED && !row.get("status").equals("READY"))
        throw new BusinessException(MediaErrors.NOT_READY, "文件仍在处理中");
      // Private gated documents may be referenced; a reference never grants public object access.
    }
    jdbc.update(
        "DELETE FROM mda_usage WHERE site_id=? AND owner_type=? AND owner_id=? AND stage=? AND"
            + " locale<=>?",
        site,
        owner.ownerType(),
        owner.ownerId(),
        owner.stage().name(),
        owner.locale());
    for (var ref : copy)
      jdbc.update(
          "INSERT INTO"
              + " mda_usage(id,site_id,media_id,owner_type,owner_id,locale,stage,part,field_path)"
              + " VALUES(?,?,?,?,?,?,?,?,?)",
          ids.nextId(),
          site,
          ref.mediaId(),
          owner.ownerType(),
          owner.ownerId(),
          owner.locale(),
          owner.stage().name(),
          ref.part(),
          ref.fieldPath());
  }
}
