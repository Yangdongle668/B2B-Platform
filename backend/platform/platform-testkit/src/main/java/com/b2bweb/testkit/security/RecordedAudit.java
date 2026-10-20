package com.b2bweb.testkit.security;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordedAudit implements AuditApi {
  public record Recorded(Long siteId, long actorId, AuditEntry entry) {}

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final CurrentUserProvider users;
  private final List<Recorded> independent = new CopyOnWriteArrayList<>();

  public RecordedAudit(MemoryStore store, SiteContextProvider sites, CurrentUserProvider users) {
    this.store = store;
    this.sites = sites;
    this.users = users;
  }

  private Recorded entry(AuditEntry value) {
    Map<String, AuditChange> diff = new LinkedHashMap<>();
    value
        .diff()
        .forEach(
            (key, change) ->
                diff.put(
                    key,
                    key.matches("(?i).*(password|secret|token|recovery|api.?key|authorization).*")
                        ? AuditChange.masked()
                        : change));
    var masked =
        new AuditEntry(
            value.moduleId(),
            value.action(),
            value.targetType(),
            value.targetId(),
            value.summary(),
            diff,
            value.result(),
            value.sensitive(),
            value.systemLevel());
    return new Recorded(
        value.systemLevel() ? null : sites.current().map(s -> s.siteId()).orElse(null),
        users.actor().userId(),
        masked);
  }

  @SuppressWarnings("unchecked")
  public void record(AuditEntry value) {
    var captured = entry(value);
    store.atomic(
        rows -> {
          var found =
              new ArrayList<>((List<Recorded>) rows.getOrDefault("audit-records", List.of()));
          found.add(captured);
          rows.put("audit-records", List.copyOf(found));
          return null;
        });
  }

  public void recordIndependently(AuditEntry value) {
    independent.add(entry(value));
  }

  @SuppressWarnings("unchecked")
  public List<Recorded> records() {
    return store.atomic(
        rows -> {
          var all = new ArrayList<>((List<Recorded>) rows.getOrDefault("audit-records", List.of()));
          all.addAll(independent);
          return List.copyOf(all);
        });
  }
}
