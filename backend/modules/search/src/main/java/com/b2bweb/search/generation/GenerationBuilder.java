package com.b2bweb.search.generation;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.JsonNode;

/**
 * Single local flow per attempt. Recovery creates a new build; it never resumes uncertain writes.
 */
public final class GenerationBuilder {
  public interface Source {
    /** Synchronous scan; invokes writeBatch on the calling execution thread only. */
    void scan(java.util.function.Consumer<List<JsonNode>> writeBatch);

    /** Returns current public documents keyed by localization ID; omitted IDs are deleted. */
    Map<Long, JsonNode> current(Set<Long> localizationIds);
  }

  private final GenerationStore store;
  private final SearchEngineClient engine;

  public GenerationBuilder(GenerationStore store, SearchEngineClient engine) {
    this.store = store;
    this.engine = engine;
  }

  public boolean build(
      String locale,
      long job,
      boolean copy,
      SearchIndexSettings settings,
      Source source,
      java.util.function.BooleanSupplier cancelled) {
    Thread execution = Thread.currentThread();
    var build = store.begin(locale, job, copy);
    boolean published = false;
    try {
      engine.ensureIndex(build.index(), settings).ifPresent(task -> await(build, task));
      if (build.base() != null) {
        CursorRequest cursor = CursorRequest.first(500);
        do {
          check(build, cancelled);
          var page = engine.scanDocuments(build.base(), cursor);
          if (!page.items().isEmpty()) write(build, page.items());
          if (page.nextCursor() == null) break;
          cursor = new CursorRequest(page.nextCursor(), 500);
        } while (true);
      } else
        source.scan(
            batch -> {
              if (Thread.currentThread() != execution)
                throw new IllegalStateException("扫描回调必须由单一执行流调用");
              check(build, cancelled);
              if (!batch.isEmpty()) write(build, batch);
            });
      long applied = build.start();
      store.checkpoint(build, applied);
      // A hot source keeps serving the previous generation and retries in a new attempt later.
      for (int pass = 0; pass < 100; pass++) {
        check(build, cancelled);
        long high = store.highWater(build);
        while (applied < high) {
          var changes = store.changes(build, applied, high);
          if (changes.isEmpty()) throw new IllegalStateException("搜索变更日志存在缺口");
          for (var change : changes) {
            if (change.sequence() != applied + 1 || change.full()) return false;
            check(build, cancelled);
            var current = source.current(change.ids());
            if (!current.isEmpty()) write(build, new ArrayList<>(current.values()));
            var deleted =
                change.ids().stream()
                    .filter(id -> !current.containsKey(id))
                    .map(Object::toString)
                    .toList();
            if (!deleted.isEmpty()) await(build, engine.delete(build.index(), deleted));
            applied = change.sequence();
            store.checkpoint(build, applied);
          }
        }
        check(build, cancelled);
        if (store.publish(build, applied)) {
          published = true;
          return true;
        }
      }
      return false;
    } finally {
      if (!published) store.abandon(build);
    }
  }

  private void check(GenerationStore.Build build, java.util.function.BooleanSupplier cancelled) {
    if (cancelled.getAsBoolean() || !store.owned(build))
      throw new IllegalStateException("索引构建已取消或失效");
  }

  private void write(GenerationStore.Build build, List<JsonNode> docs) {
    if (!store.owned(build)) throw new IllegalStateException("构建已失效或已经发布");
    if (docs.size() > 1000) throw new IllegalArgumentException("索引批量超过1000");
    await(build, engine.upsert(build.index(), docs));
  }

  private void await(GenerationStore.Build build, long task) {
    store.task(build, task);
    for (int observations = 0; observations < 120; observations++) {
      var status = engine.await(task, Duration.ofSeconds(5));
      switch (status.state()) {
        case SUCCEEDED:
          return;
        case ENQUEUED, PROCESSING:
          break;
        default:
          throw new IllegalStateException("索引引擎任务未成功");
      }
    }
    throw new IllegalStateException("索引引擎任务超时，废弃本代次");
  }

  public void collect(String locale) {
    for (String index : store.garbage(locale, Duration.ofMinutes(5))) engine.deleteIndex(index);
    store.pruneChanges(locale, 1000);
  }
}
