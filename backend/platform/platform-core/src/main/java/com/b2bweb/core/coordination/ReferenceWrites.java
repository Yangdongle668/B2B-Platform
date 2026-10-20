package com.b2bweb.core.coordination;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** A synchronous mandatory-TX boundary. Core never queries media or content tables. */
@Component
public final class ReferenceWrites implements ReferenceWriteApi {
  private final ObjectProvider<MediaReferenceSink> sinks;
  private final SiteContextProvider sites;

  public ReferenceWrites(ObjectProvider<MediaReferenceSink> sinks, SiteContextProvider sites) {
    this.sinks = sinks;
    this.sites = sites;
  }

  public void replaceMedia(MediaReferenceOwner owner, List<MediaReference> references) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("媒体引用必须加入源保存事务");
    sites.requireSiteId();
    Objects.requireNonNull(owner);
    var copy = List.copyOf(references);
    var all = sinks.orderedStream().toList();
    if (all.size() != 1) throw new IllegalStateException("媒体引用接收器必须且只能有一个");
    all.getFirst().replace(owner, copy);
  }
}
