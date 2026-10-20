package com.b2bweb.testkit.directory;

import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemorySiteProfiles implements SiteProfileApi {
  private final MemoryStore store;
  private final SiteContextProvider sites;

  public InMemorySiteProfiles(MemoryStore store, SiteContextProvider sites) {
    this.store = store;
    this.sites = sites;
  }

  public void seed(SiteProfileView value) {
    store.atomic(
        rows -> {
          rows.put("profile:" + sites.requireSiteId() + ":" + value.locale(), value);
          rows.put("profile-base:" + sites.requireSiteId(), value);
          return null;
        });
  }

  public SiteProfileView get(String locale) {
    return store.atomic(
        rows -> {
          var found = (SiteProfileView) rows.get("profile:" + sites.requireSiteId() + ":" + locale);
          if (found != null) return found;
          var base = (SiteProfileView) rows.get("profile-base:" + sites.requireSiteId());
          if (base == null)
            return new SiteProfileView(
                locale,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                new SiteProfileView.LegalInfo(
                    null, null, List.of(), null, null, null, null, null, null, null));
          return new SiteProfileView(
              locale,
              null,
              null,
              base.logoMediaId(),
              base.logoInverseMediaId(),
              base.faviconMediaId(),
              base.appIconMediaId(),
              base.email(),
              base.phone(),
              base.whatsapp(),
              base.addresses().stream()
                  .map(
                      a ->
                          new SiteProfileView.Address(
                              a.type(),
                              null,
                              "",
                              a.postalCode(),
                              "",
                              null,
                              a.countryCode(),
                              a.phone()))
                  .toList(),
              base.socialLinks(),
              null,
              base.foundedYear(),
              base.legal());
        });
  }
}
