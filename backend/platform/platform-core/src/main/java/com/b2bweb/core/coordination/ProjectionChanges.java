package com.b2bweb.core.coordination;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.SearchChangeCapture;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.SiteContexts;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** Buffer intent only; flush after every source mutation but before the transaction commits. */
@Component
public final class ProjectionChanges implements ProjectionChangeApi {
  private final ObjectProvider<SearchChangeCapture> captures;
  private final SiteContexts sites;
  private final SiteApi api;

  private static final class Intent {
    final SortedSet<String> locales = new TreeSet<>();
    final SortedSet<Long> ids = new TreeSet<>();
    boolean full;

    void add(Set<String> locales, Set<Long> ids, boolean full) {
      this.locales.addAll(locales);
      this.ids.addAll(ids);
      this.full |= full || this.ids.size() > 500;
      if (this.full) this.ids.clear();
    }
  }

  private final class Pending implements TransactionSynchronization {
    final SortedMap<Long, Intent> intents = new TreeMap<>();

    public int getOrder() {
      return Integer.MAX_VALUE - 100;
    }

    public void suspend() {
      TransactionSynchronizationManager.unbindResource(ProjectionChanges.this);
    }

    public void resume() {
      TransactionSynchronizationManager.bindResource(ProjectionChanges.this, this);
    }

    public void beforeCommit(boolean readOnly) {
      if (intents.isEmpty()) return;
      var all = captures.orderedStream().toList();
      if (all.size() != 1) throw new IllegalStateException("搜索捕获接收器必须且只能有一个");
      for (var entry : intents.entrySet()) {
        var intent = entry.getValue();
        sites.runAs(
            entry.getKey(),
            () ->
                all.getFirst()
                    .append(
                        Collections.unmodifiableSortedSet(intent.locales),
                        Collections.unmodifiableSortedSet(intent.ids),
                        intent.full));
      }
    }

    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(ProjectionChanges.this);
    }
  }

  public ProjectionChanges(
      ObjectProvider<SearchChangeCapture> captures, SiteContexts sites, SiteApi api) {
    this.captures = captures;
    this.sites = sites;
    this.api = api;
  }

  public void searchChanged(Set<String> locales, Set<Long> ids, boolean full) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
      throw new IllegalStateException("搜索变更捕获需要源写事务");
    long site = sites.requireSiteId();
    if (full && !ids.isEmpty()
        || ids.stream().anyMatch(id -> id == null || id <= 0)
        || locales.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("搜索捕获范围无效");
    Set<String> configured = new HashSet<>();
    api.locales().forEach(l -> configured.add(l.locale()));
    if (!configured.containsAll(locales)) throw new IllegalArgumentException("搜索捕获语言未配置");
    if (locales.isEmpty() || !full && ids.isEmpty()) return;
    Pending pending = (Pending) TransactionSynchronizationManager.getResource(this);
    if (pending == null) {
      pending = new Pending();
      TransactionSynchronizationManager.bindResource(this, pending);
      TransactionSynchronizationManager.registerSynchronization(pending);
    }
    pending
        .intents
        .computeIfAbsent(site, ignored -> new Intent())
        .add(Set.copyOf(locales), Set.copyOf(ids), full);
  }
}
