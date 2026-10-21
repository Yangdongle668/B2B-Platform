package com.b2bweb.testkit.job;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.*;
import com.b2bweb.testkit.security.FakeCurrentUserProvider;
import com.b2bweb.testkit.site.FakeSiteContextProvider;
import java.io.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/**
 * Deterministic persistent-queue double: execution is explicit through runNext(), never a
 * background race.
 */
public final class InMemoryJobApi implements JobApi {
  private record Entry(long site, JobRequest request, JobInfo info) {}

  private final MemoryStore store;
  private final FakeSiteContextProvider sites;
  private final FakeCurrentUserProvider users;
  private final SiteApi api;
  private final IdGenerator ids;
  private final Clock clock;
  private final ObjectStorage storage;
  private final Map<String, JobHandler> handlers = new LinkedHashMap<>();
  private final JsonMapper json = JsonMapper.builder().build();

  public InMemoryJobApi(
      MemoryStore store,
      FakeSiteContextProvider sites,
      FakeCurrentUserProvider users,
      SiteApi api,
      IdGenerator ids,
      Clock clock,
      ObjectStorage storage) {
    this.store = store;
    this.sites = sites;
    this.users = users;
    this.api = api;
    this.ids = ids;
    this.clock = clock;
    this.storage = storage;
  }

  public void register(String module, JobHandler handler) {
    if (!handler.jobType().startsWith(module + ".")
        || handlers.putIfAbsent(handler.jobType(), handler) != null)
      throw new IllegalArgumentException("重复或错误的任务处理器");
  }

  private long site() {
    return sites.current().map(SiteContext::siteId).orElse(0L);
  }

  private void enabled(long site, String module) {
    if (site != 0
        && (api.find(site).filter(s -> s.status() != SiteStatus.ARCHIVED).isEmpty()
            || !api.isModuleEnabled(site, module))) throw new ModuleDisabledException(module);
  }

  private String key(long id) {
    return "job:" + id;
  }

  private JobInfo copy(JobInfo info) {
    return Records.copy(
        info,
        Collections.singletonMap(
            "result", info.result() == null ? null : info.result().deepCopy()));
  }

  private JobInfo fresh(long id, JobRequest r) {
    return new JobInfo(
        id,
        r.moduleId(),
        r.jobType(),
        r.title(),
        JobStatus.PENDING,
        r.parentJobId(),
        new JobProgress(null, 0, 0, null),
        null,
        null,
        null,
        null,
        0,
        r.maxAttempts(),
        false,
        users.actor().userId(),
        clock.instant(),
        null,
        null);
  }

  private Entry require(Map<String, Object> rows, long id) {
    var value = (Entry) rows.get(key(id));
    if (value == null || value.site() != site())
      throw new NotFoundException(CoreErrors.JOB_NOT_FOUND, "任务不存在");
    return value;
  }

  public JobInfo submit(JobRequest input) {
    if (!handlers.containsKey(input.jobType()))
      throw new BusinessException(CoreErrors.JOB_TYPE_UNKNOWN, "任务处理器未注册");
    if (!input.jobType().startsWith(input.moduleId() + ".")
        || input.maxAttempts() < 1
        || input.maxAttempts() > 20) throw new IllegalArgumentException("任务参数不合法");
    long site = site();
    enabled(site, input.moduleId());
    JobRequest r =
        Records.copy(
            input,
            Collections.singletonMap(
                "params", input.params() == null ? null : input.params().deepCopy()));
    return store.atomic(
        rows -> {
          if (r.parentJobId() != null) require(rows, r.parentJobId());
          if (r.idempotencyKey() != null)
            for (Object value : rows.values())
              if (value instanceof Entry e
                  && e.site() == site
                  && e.request().jobType().equals(r.jobType())
                  && Objects.equals(e.request().idempotencyKey(), r.idempotencyKey())) {
                if (!Objects.equals(e.request().params(), r.params())
                    || !Objects.equals(e.request().parentJobId(), r.parentJobId()))
                  throw new BusinessException(
                      ErrorCode.of("CORE_IDEMPOTENCY_KEY_REUSED", 409, "幂等键重复使用"), "任务参数不一致");
                return copy(e.info());
              }
          var info = fresh(ids.nextId(), r);
          rows.put(key(info.jobId()), new Entry(site, r, info));
          return copy(info);
        });
  }

  public Optional<JobInfo> find(long id) {
    return store.atomic(
        rows -> {
          var entry = (Entry) rows.get(key(id));
          return entry != null && entry.site() == site()
              ? Optional.of(copy(entry.info()))
              : Optional.empty();
        });
  }

  private void change(long id, Map<String, Object> changes) {
    store.atomic(
        rows -> {
          var e = require(rows, id);
          rows.put(key(id), new Entry(e.site(), e.request(), Records.copy(e.info(), changes)));
          return null;
        });
  }

  private BusinessException state() {
    return new BusinessException(CoreErrors.JOB_STATE_INVALID, "任务状态不允许该操作");
  }

  public void requestCancel(long id) {
    store.atomic(
        rows -> {
          var entry = require(rows, id);
          if (!Set.of(JobStatus.PENDING, JobStatus.RUNNING).contains(entry.info().status()))
            throw state();
          Map<String, Object> changes = new HashMap<>();
          changes.put("cancelRequested", true);
          if (entry.info().status() == JobStatus.PENDING) {
            changes.put("status", JobStatus.CANCELED);
            changes.put("finishedAt", clock.instant());
          }
          change(id, changes);
          return null;
        });
  }

  public JobInfo retry(long id) {
    return store.atomic(
        rows -> {
          var e = require(rows, id);
          if (e.info().status() != JobStatus.FAILED) throw state();
          enabled(site(), e.info().moduleId());
          Map<String, Object> changes = new HashMap<>();
          changes.put("status", JobStatus.PENDING);
          changes.put("cancelRequested", false);
          changes.put("errorCode", null);
          changes.put("errorMessage", null);
          changes.put("finishedAt", null);
          change(id, changes);
          return find(id).orElseThrow();
        });
  }

  public PageResult<JobInfo> list(JobQuery query, PageRequest page) {
    var orders = page.sort().isEmpty() ? List.of(SortOrder.desc("createdAt")) : page.sort();
    Comparator<JobInfo> sort = (a, b) -> 0;
    for (var order : orders) {
      Comparator<JobInfo> field =
          switch (order.field()) {
            case "createdAt" -> Comparator.comparing(JobInfo::createdAt);
            case "finishedAt" ->
                Comparator.comparing(
                    JobInfo::finishedAt, Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> throw new IllegalArgumentException("不支持的排序字段");
          };
      sort = sort.thenComparing(order.descending() ? field.reversed() : field);
    }
    sort = sort.thenComparing(Comparator.comparingLong(JobInfo::jobId).reversed());
    final var comparator = sort;
    return store.atomic(
        rows -> {
          List<JobInfo> found =
              rows.values().stream()
                  .filter(Entry.class::isInstance)
                  .map(Entry.class::cast)
                  .filter(e -> e.site() == site())
                  .map(Entry::info)
                  .filter(i -> query.moduleId() == null || i.moduleId().equals(query.moduleId()))
                  .filter(i -> query.jobType() == null || i.jobType().equals(query.jobType()))
                  .filter(i -> query.createdBy() == null || i.createdBy() == query.createdBy())
                  .filter(i -> query.statuses().isEmpty() || query.statuses().contains(i.status()))
                  .sorted(comparator)
                  .toList();
          return new PageResult<>(
              found.stream().skip(page.offset()).limit(page.size()).map(this::copy).toList(),
              page.page(),
              page.size(),
              found.size());
        });
  }

  public StoredFile storeInputFile(String name, String type, long size, InputStream input) {
    var key = StorageKey.jobFile(site(), YearMonth.now(clock), ids.nextId(), "file", "bin");
    storage.put(key, input, size, type, Visibility.PRIVATE);
    return new StoredFile(key.value(), name, type, size);
  }

  public URI resultDownloadUrl(long id, Duration ttl) {
    var info = find(id).orElseThrow(() -> new NotFoundException(CoreErrors.JOB_NOT_FOUND, "任务不存在"));
    if (info.resultFile() == null || !info.resultFile().expiresAt().isAfter(clock.instant()))
      throw new NotFoundException(CoreErrors.JOB_NOT_FOUND, "结果不存在或已过期");
    String file = store.atomic(rows -> (String) rows.get("job-file:" + id));
    return storage.presignedGet(new StorageKey(file), ttl, info.resultFile().fileName());
  }

  public boolean runNext() {
    Entry chosen =
        store.atomic(
            rows -> {
              var found =
                  rows.values().stream()
                      .filter(Entry.class::isInstance)
                      .map(Entry.class::cast)
                      .filter(
                          e ->
                              e.info().status() == JobStatus.PENDING
                                  && (e.request().scheduledAt() == null
                                      || !e.request().scheduledAt().isAfter(clock.instant())))
                      .min(Comparator.comparingLong(e -> e.info().jobId()))
                      .orElse(null);
              if (found == null) return null;
              var running =
                  Records.copy(
                      found.info(),
                      Map.of(
                          "status",
                          JobStatus.RUNNING,
                          "attempts",
                          found.info().attempts() + 1,
                          "startedAt",
                          clock.instant()));
              var entry = new Entry(found.site(), found.request(), running);
              rows.put(key(running.jobId()), entry);
              return entry;
            });
    if (chosen == null) return false;
    SiteContext before = sites.current().orElse(null);
    CurrentUser old = users.current().orElse(null);
    try (var actor =
        users.asActor(
            chosen.info().createdBy() == 0
                ? ActorRef.SYSTEM
                : ActorRef.user(
                    chosen.info().createdBy(), Long.toString(chosen.info().createdBy())))) {
      if (chosen.site() != 0) {
        sites.use(chosen.site());
        var context = sites.require();
        sites.useContext(
            new SiteContext(
                context.siteId(),
                context.siteCode(),
                context.sourceLocale(),
                context.defaultLocale(),
                context.timezone(),
                null,
                ContextOrigin.JOB));
      } else sites.clear();
      users.clear();
      enabled(chosen.site(), chosen.info().moduleId());
      var context = new Execution(chosen);
      var outcome = handlers.get(chosen.info().jobType()).execute(context);
      context.checkpoint();
      Map<String, Object> changes = new HashMap<>();
      changes.put("status", JobStatus.SUCCEEDED);
      changes.put("result", outcome.result() == null ? null : outcome.result().deepCopy());
      changes.put("finishedAt", clock.instant());
      change(chosen.info().jobId(), changes);
    } catch (JobCanceledException e) {
      change(
          chosen.info().jobId(),
          Map.of("status", JobStatus.CANCELED, "finishedAt", clock.instant()));
    } catch (ModuleDisabledException e) {
      change(
          chosen.info().jobId(),
          Map.of(
              "status",
              JobStatus.CANCELED,
              "errorCode",
              "CORE_MODULE_DISABLED",
              "finishedAt",
              clock.instant()));
    } catch (Exception e) {
      var info = chosen.info();
      Map<String, Object> changes = new HashMap<>();
      changes.put("status", JobStatus.FAILED);
      changes.put("finishedAt", clock.instant());
      changes.put(
          "errorCode", e instanceof PlatformException p ? p.errorCode().code() : "CORE_JOB_FAILED");
      changes.put("errorMessage", e instanceof PlatformException p ? p.detail() : "任务执行失败");
      change(info.jobId(), changes);
      if (info.attempts() < info.maxAttempts()) {
        retry(info.jobId());
        store.atomic(
            rows -> {
              var entry = require(rows, info.jobId());
              var r =
                  Records.copy(
                      entry.request(),
                      Map.of(
                          "scheduledAt",
                          clock
                              .instant()
                              .plusSeconds(60L * (1L << Math.min(info.attempts(), 10)))));
              rows.put(key(info.jobId()), new Entry(entry.site(), r, entry.info()));
              return null;
            });
      }
    } finally {
      if (before == null) sites.clear();
      else sites.useContext(before);
      if (old != null) users.use(old);
      else users.clear();
    }
    return true;
  }

  private final class Execution implements JobExecutionContext {
    final Entry entry;

    Execution(Entry e) {
      entry = e;
    }

    public long jobId() {
      return entry.info().jobId();
    }

    public long siteId() {
      return entry.site();
    }

    public ActorRef initiator() {
      return entry.info().createdBy() == 0
          ? ActorRef.SYSTEM
          : ActorRef.user(entry.info().createdBy(), Long.toString(entry.info().createdBy()));
    }

    public int attempt() {
      return entry.info().attempts();
    }

    public JsonNode params() {
      return entry.request().params() == null
          ? json.createObjectNode()
          : entry.request().params().deepCopy();
    }

    public <T> T params(Class<T> type) {
      return json.treeToValue(params(), type);
    }

    public void progress(Integer total, int done, int failed, String message) {
      checkpoint();
      change(jobId(), Map.of("progress", new JobProgress(total, done, failed, message)));
    }

    public void checkpoint() {
      if (isCancelRequested()) throw new JobCanceledException();
    }

    public boolean isCancelRequested() {
      return find(jobId()).orElseThrow().cancelRequested();
    }

    public InputStream openInputFile(String value) {
      var key = new StorageKey(value);
      if (!key.value().startsWith("jobs/") || !key.belongsToSite(siteId()))
        throw new ForbiddenException(CoreErrors.FORBIDDEN, "任务文件不属于当前站点");
      return storage.get(key, Visibility.PRIVATE);
    }

    public void writeResultFile(String name, String type, ThrowingConsumer<OutputStream> writer)
        throws IOException {
      checkpoint();
      var output = new ByteArrayOutputStream();
      writer.accept(output);
      byte[] bytes = output.toByteArray();
      var key = StorageKey.jobFile(siteId(), YearMonth.now(clock), ids.nextId(), "result", "bin");
      storage.put(key, new ByteArrayInputStream(bytes), bytes.length, type, Visibility.PRIVATE);
      store.atomic(
          rows -> {
            String old = (String) rows.put("job-file:" + jobId(), key.value());
            if (old != null) storage.delete(new StorageKey(old), Visibility.PRIVATE);
            return null;
          });
      change(
          jobId(),
          Map.of(
              "resultFile",
              new JobFile(name, bytes.length, clock.instant().plus(Duration.ofDays(7)))));
    }
  }
}
