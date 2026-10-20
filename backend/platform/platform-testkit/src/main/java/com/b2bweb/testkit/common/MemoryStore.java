package com.b2bweb.testkit.common;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.springframework.transaction.support.*;

/** Shared test-platform store; transaction deltas become visible only after commit. */
public final class MemoryStore {
  private final ThreadLocal<Map<String, Object>> executing = new ThreadLocal<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Map<String, Object> committed = new HashMap<>();
  private static final Object DELETED = new Object();

  private final class Delta implements TransactionSynchronization {
    final Map<String, Object> changes = new HashMap<>();

    public void suspend() {
      TransactionSynchronizationManager.unbindResource(MemoryStore.this);
    }

    public void resume() {
      TransactionSynchronizationManager.bindResource(MemoryStore.this, this);
    }

    public void afterCompletion(int status) {
      try {
        if (status == STATUS_COMMITTED)
          changes.forEach(
              (k, v) -> {
                if (v == DELETED) committed.remove(k);
                else committed.put(k, v);
              });
      } finally {
        TransactionSynchronizationManager.unbindResourceIfPossible(MemoryStore.this);
        lock.unlock();
      }
    }
  }

  private Delta delta() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()) return null;
    var existing = (Delta) TransactionSynchronizationManager.getResource(this);
    if (existing != null) return existing;
    lock.lock();
    var created = new Delta();
    TransactionSynchronizationManager.bindResource(this, created);
    TransactionSynchronizationManager.registerSynchronization(created);
    return created;
  }

  public <T> T atomic(Function<Map<String, Object>, T> action) {
    if (executing.get() != null) {
      var current = executing.get();
      var before = new HashMap<>(current);
      try {
        return action.apply(current);
      } catch (RuntimeException | Error failure) {
        current.clear();
        current.putAll(before);
        throw failure;
      }
    }
    lock.lock();
    try {
      var delta = delta();
      Map<String, Object> view = new HashMap<>(committed);
      if (delta != null)
        delta.changes.forEach(
            (k, v) -> {
              if (v == DELETED) view.remove(k);
              else view.put(k, v);
            });
      Map<String, Object> before = new HashMap<>(view);
      T result;
      executing.set(view);
      try {
        result = action.apply(view);
      } finally {
        executing.remove();
      }
      Set<String> keys = new HashSet<>(before.keySet());
      keys.addAll(view.keySet());
      for (String key : keys)
        if (!Objects.equals(before.get(key), view.get(key))
            || before.containsKey(key) != view.containsKey(key)) {
          Object value = view.containsKey(key) ? view.get(key) : DELETED;
          if (delta != null) delta.changes.put(key, value);
          else if (value == DELETED) committed.remove(key);
          else committed.put(key, value);
        }
      return result;
    } finally {
      lock.unlock();
    }
  }

  public void clear() {
    atomic(
        rows -> {
          rows.clear();
          return null;
        });
  }
}
