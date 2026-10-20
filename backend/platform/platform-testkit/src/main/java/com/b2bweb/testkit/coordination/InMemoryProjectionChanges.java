package com.b2bweb.testkit.coordination;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;
import org.springframework.transaction.support.*;

public final class InMemoryProjectionChanges implements ProjectionChangeApi {
  public record Captured(long siteId, List<String> locales, Set<Long> ids, boolean fullRefresh) {
    public Captured {
      locales = List.copyOf(locales);
      ids = Set.copyOf(ids);
    }
  }

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final SiteApi api;
  private boolean fail;

  private static final class Intent {
    final SortedSet<String> locales = new TreeSet<>();
    final Set<Long> ids = new TreeSet<>();
    boolean full;
  }

  private final class Pending implements TransactionSynchronization {
    final SortedMap<Long, Intent> intents = new TreeMap<>();

    public int getOrder() {
      return Integer.MAX_VALUE - 100;
    }

    public void suspend() {
      TransactionSynchronizationManager.unbindResource(InMemoryProjectionChanges.this);
    }

    public void resume() {
      TransactionSynchronizationManager.bindResource(InMemoryProjectionChanges.this, this);
    }

    public void beforeCommit(boolean readOnly) {
      if (fail) throw new IllegalStateException("受控搜索捕获失败");
      store.atomic(
          rows -> {
            @SuppressWarnings("unchecked")
            var previous = (List<Captured>) rows.getOrDefault("projection-captures", List.of());
            var records = new ArrayList<>(previous);
            intents.forEach(
                (site, intent) ->
                    records.add(
                        new Captured(
                            site, new ArrayList<>(intent.locales), intent.ids, intent.full)));
            rows.put("projection-captures", List.copyOf(records));
            return null;
          });
    }

    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(InMemoryProjectionChanges.this);
    }
  }

  public InMemoryProjectionChanges(MemoryStore store, SiteContextProvider sites, SiteApi api) {
    this.store = store;
    this.sites = sites;
    this.api = api;
  }

  public void failCapture(boolean value) {
    fail = value;
  }

  public void searchChanged(Set<String> locales, Set<Long> ids, boolean full) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
      throw new IllegalStateException("搜索捕获需要源写事务");
    long site = sites.requireSiteId();
    if (full && !ids.isEmpty() || ids.stream().anyMatch(id -> id <= 0))
      throw new IllegalArgumentException("搜索捕获范围无效");
    Set<String> configured = new HashSet<>();
    api.locales().forEach(l -> configured.add(l.locale()));
    if (!configured.containsAll(locales)) throw new IllegalArgumentException("搜索捕获语言未配置");
    if (locales.isEmpty() || !full && ids.isEmpty()) return;
    store.atomic(rows -> null);
    var pending = (Pending) TransactionSynchronizationManager.getResource(this);
    if (pending == null) {
      pending = new Pending();
      TransactionSynchronizationManager.bindResource(this, pending);
      TransactionSynchronizationManager.registerSynchronization(pending);
    }
    var intent = pending.intents.computeIfAbsent(site, ignored -> new Intent());
    intent.locales.addAll(locales);
    intent.ids.addAll(ids);
    intent.full |= full || intent.ids.size() > 500;
    if (intent.full) intent.ids.clear();
  }

  @SuppressWarnings("unchecked")
  public List<Captured> captures() {
    return store.atomic(
        rows -> List.copyOf((List<Captured>) rows.getOrDefault("projection-captures", List.of())));
  }
}
