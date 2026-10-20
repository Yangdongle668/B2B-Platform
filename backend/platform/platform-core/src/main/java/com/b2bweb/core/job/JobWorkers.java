package com.b2bweb.core.job;

import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "b2bweb.core.jobs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JobWorkers {
  private final DatabaseJobs jobs;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private volatile boolean ready;

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void ready() {
    ready = true;
  }

  private final Semaphore slots;
  private final String worker = UUID.randomUUID().toString();

  public JobWorkers(
      DatabaseJobs jobs,
      @org.springframework.beans.factory.annotation.Value("${b2bweb.core.jobs.concurrency:4}")
          int concurrency) {
    if (concurrency < 1 || concurrency > 64) throw new IllegalArgumentException("任务并发数无效");
    this.jobs = jobs;
    slots = new Semaphore(concurrency);
  }

  @Scheduled(fixedDelay = 2000)
  public void poll() {
    if (!ready) return;
    while (slots.tryAcquire()) {
      java.util.Optional<DatabaseJobs.Claim> claimed;
      try {
        claimed = jobs.claim(worker);
      } catch (RuntimeException e) {
        slots.release();
        throw e;
      }
      if (claimed.isEmpty()) {
        slots.release();
        break;
      }
      executor.submit(
          () -> {
            try {
              jobs.execute(claimed.get());
            } finally {
              slots.release();
            }
          });
    }
  }

  @Scheduled(cron = "30 * * * * *", zone = "UTC")
  public void reap() {
    jobs.reap();
  }

  @PreDestroy
  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
