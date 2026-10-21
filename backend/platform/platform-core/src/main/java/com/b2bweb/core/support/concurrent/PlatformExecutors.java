package com.b2bweb.core.support.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.Executor;
import org.springframework.core.task.TaskDecorator;

/**
 * Tasks capture context on submission, use virtual threads, and release workers during shutdown.
 */
public final class PlatformExecutors implements AutoCloseable {
  private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
  private final TaskDecorator contexts;

  public PlatformExecutors(TaskDecorator contexts) {
    this.contexts = contexts;
  }

  public Executor virtual() {
    return task -> workers.execute(contexts.decorate(task));
  }

  public <T> CompletableFuture<T> submit(java.util.function.Supplier<T> task) {
    return CompletableFuture.supplyAsync(task, virtual());
  }

  public void close() {
    workers.shutdown();
    try {
      if (!workers.awaitTermination(30, TimeUnit.SECONDS)) workers.shutdownNow();
    } catch (InterruptedException e) {
      workers.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
