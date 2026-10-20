package com.b2bweb.testkit.coordination;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.*;

public final class FakeReferenceWrites implements ReferenceWriteApi {
  private final MediaReferenceSink sink;
  private final SiteContextProvider sites;

  public FakeReferenceWrites(MediaReferenceSink sink, SiteContextProvider sites) {
    this.sink = sink;
    this.sites = sites;
  }

  public void replaceMedia(MediaReferenceOwner owner, List<MediaReference> references) {
    if (!org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) throw new IllegalStateException("媒体引用需要源事务");
    sites.requireSiteId();
    sink.replace(owner, List.copyOf(references));
  }
}
