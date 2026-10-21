package com.b2bweb.core.job;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** All executor updates are fenced by the claim version, not a reusable worker name. */
public final class DatabaseJobs implements JobApi {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx, independent;
  private final SiteContexts sites;
  private final UserContexts users;
  private final SiteApi siteApi;
  private final ExtensionRegistry extensions;
  private final ObjectStorage storage;
  private final IdGenerator ids;
  private final Clock clock;
  private final JsonMapper json = JsonMapper.builder().build();

  public DatabaseJobs(
      JdbcTemplate jdbc,
      PlatformTransactionManager manager,
      SiteContexts sites,
      UserContexts users,
      SiteApi siteApi,
      ExtensionRegistry extensions,
      ObjectStorage storage,
      IdGenerator ids,
      Clock clock) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.users = users;
    this.siteApi = siteApi;
    this.extensions = extensions;
    this.storage = storage;
    this.ids = ids;
    this.clock = clock;
    tx = new TransactionTemplate(manager);
    independent = new TransactionTemplate(manager);
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  private long site() {
    return sites.current().map(SiteContext::siteId).orElse(0L);
  }

  private JobHandler handler(String type) {
    return extensions.allRegardlessOfSite(JobHandler.class).stream()
        .filter(h -> h.jobType().equals(type))
        .findFirst()
        .orElseThrow(() -> new BusinessException(CoreErrors.JOB_TYPE_UNKNOWN, "任务处理器未注册"));
  }

  private void enabled(long site, String module) {
    if (site != 0
        && (siteApi.find(site).filter(s -> s.status() != SiteStatus.ARCHIVED).isEmpty()
            || !siteApi.isModuleEnabled(site, module))) throw new ModuleDisabledException(module);
  }

  public JobInfo submit(JobRequest request) {
    var h = handler(request.jobType());
    long site = site();
    if (!request.jobType().startsWith(request.moduleId() + ".")
        || !extensions.moduleOf(h).equals(request.moduleId())
        || request.maxAttempts() < 1
        || request.maxAttempts() > 20
        || request.title().isBlank()
        || request.title().length() > 200
        || request.idempotencyKey() != null
            && (request.idempotencyKey().isBlank() || request.idempotencyKey().length() > 64))
      throw new IllegalArgumentException("任务参数不合法");
    enabled(site, request.moduleId());
    return tx.execute(
        status -> {
          if (request.parentJobId() != null && find(request.parentJobId()).isEmpty())
            throw new NotFoundException(CoreErrors.JOB_NOT_FOUND, "前置任务不存在");
          long id = ids.nextId();
          String payload = request.params() == null ? null : request.params().toString();
          jdbc.update(
              "INSERT INTO"
                  + " sys_job(id,site_id,module_id,job_type,title,params,idempotency_key,parent_job_id,scheduled_at,max_attempts,created_by)"
                  + " VALUES(?,?,?,?,?,CAST(? AS JSON),?,?,?,?,?) ON DUPLICATE KEY UPDATE id=id",
              id,
              site,
              request.moduleId(),
              request.jobType(),
              request.title(),
              payload,
              request.idempotencyKey(),
              request.parentJobId(),
              Timestamp.from(
                  request.scheduledAt() == null ? clock.instant() : request.scheduledAt()),
              request.maxAttempts(),
              users.actor().userId());
          var row =
              request.idempotencyKey() == null
                  ? row(id, site, true)
                  : jdbc.queryForMap(
                      "SELECT * FROM sys_job WHERE site_id=? AND job_type=? AND idempotency_key=?"
                          + " FOR UPDATE",
                      site,
                      request.jobType(),
                      request.idempotencyKey());
          JsonNode saved = node(row.get("params"));
          if (!Objects.equals(saved, request.params())
              || !Objects.equals(number(row, "parent_job_id"), request.parentJobId()))
            throw new BusinessException(
                ErrorCode.of("CORE_IDEMPOTENCY_KEY_REUSED", 409, "幂等键已被其他请求使用"), "任务参数或前置任务不一致");
          return info(row);
        });
  }

  private Map<String, Object> row(long id, long site, boolean lock) {
    var rows =
        jdbc.queryForList(
            "SELECT * FROM sys_job WHERE id=? AND site_id=?" + (lock ? " FOR UPDATE" : ""),
            id,
            site);
    if (rows.isEmpty()) throw new NotFoundException(CoreErrors.JOB_NOT_FOUND, "任务不存在");
    return rows.getFirst();
  }

  public Optional<JobInfo> find(long id) {
    return jdbc.queryForList("SELECT * FROM sys_job WHERE id=? AND site_id=?", id, site()).stream()
        .findFirst()
        .map(this::info);
  }

  public PageResult<JobInfo> list(JobQuery query, PageRequest page) {
    StringBuilder where = new StringBuilder(" WHERE site_id=?");
    List<Object> args = new ArrayList<>(List.of(site()));
    if (query.moduleId() != null) {
      where.append(" AND module_id=?");
      args.add(query.moduleId());
    }
    if (query.jobType() != null) {
      where.append(" AND job_type=?");
      args.add(query.jobType());
    }
    if (query.createdBy() != null) {
      where.append(" AND created_by=?");
      args.add(query.createdBy());
    }
    if (!query.statuses().isEmpty()) {
      where.append(
          " AND status IN ("
              + String.join(",", Collections.nCopies(query.statuses().size(), "?"))
              + ")");
      query.statuses().stream().map(Enum::name).sorted().forEach(args::add);
    }
    var orders = page.sort().isEmpty() ? List.of(SortOrder.desc("createdAt")) : page.sort();
    List<String> sqlOrder = new ArrayList<>();
    for (var order : orders) {
      String column =
          switch (order.field()) {
            case "createdAt" -> "created_at";
            case "finishedAt" -> "finished_at";
            default -> throw new IllegalArgumentException("不支持的任务排序字段");
          };
      sqlOrder.add(column + (order.descending() ? " DESC" : " ASC"));
    }
    sqlOrder.add("id DESC");
    long count =
        jdbc.queryForObject("SELECT COUNT(*) FROM sys_job" + where, Long.class, args.toArray());
    args.add(page.size());
    args.add(page.offset());
    var rows =
        jdbc.queryForList(
            "SELECT * FROM sys_job"
                + where
                + " ORDER BY "
                + String.join(",", sqlOrder)
                + " LIMIT ? OFFSET ?",
            args.toArray());
    return new PageResult<>(
        rows.stream().map(this::info).toList(), page.page(), page.size(), count);
  }

  public void requestCancel(long id) {
    tx.executeWithoutResult(
        status -> {
          var row = row(id, site(), true);
          String state = (String) row.get("status");
          if (!Set.of("PENDING", "RUNNING").contains(state)) throw state();
          if (state.equals("PENDING"))
            jdbc.update(
                "UPDATE sys_job SET"
                    + " status='CANCELED',cancel_requested=1,finished_at=CURRENT_TIMESTAMP(3),version=version+1"
                    + " WHERE id=?",
                id);
          else jdbc.update("UPDATE sys_job SET cancel_requested=1 WHERE id=?", id);
        });
  }

  public JobInfo retry(long id) {
    return tx.execute(
        status -> {
          var row = row(id, site(), true);
          if (!row.get("status").equals("FAILED")) throw state();
          enabled(site(), (String) row.get("module_id"));
          jdbc.update(
              "UPDATE sys_job SET"
                  + " status='PENDING',cancel_requested=0,worker_id=NULL,heartbeat_at=NULL,error_code=NULL,error_message=NULL,finished_at=NULL,scheduled_at=CURRENT_TIMESTAMP(3),version=version+1"
                  + " WHERE id=?",
              id);
          return info(row(id, site(), true));
        });
  }

  private BusinessException state() {
    return new BusinessException(CoreErrors.JOB_STATE_INVALID, "任务状态不允许该操作");
  }

  public StoredFile storeInputFile(String name, String type, long size, InputStream input) {
    var key = fileKey(site());
    storage.put(key, input, size, type, Visibility.PRIVATE);
    return new StoredFile(key.value(), name, type, size);
  }

  private StorageKey fileKey(long site) {
    return StorageKey.jobFile(site, YearMonth.now(clock), ids.nextId(), "file", "bin");
  }

  public URI resultDownloadUrl(long id, Duration ttl) {
    var row = row(id, site(), false);
    if (row.get("result_file_key") == null
        || !instant(row, "result_expires_at").isAfter(clock.instant()))
      throw new NotFoundException(CoreErrors.JOB_NOT_FOUND, "任务结果不存在或已过期");
    return storage.presignedGet(
        new StorageKey((String) row.get("result_file_key")),
        ttl,
        (String) row.get("result_file_name"));
  }

  private io.micrometer.core.instrument.MeterRegistry metrics;

  @org.springframework.beans.factory.annotation.Autowired
  public void metrics(io.micrometer.core.instrument.MeterRegistry registry) {
    this.metrics = registry;
  }

  public record Claim(
      long jobId,
      long siteId,
      String moduleId,
      String jobType,
      long actor,
      int version,
      int attempt,
      JsonNode params) {}

  public Optional<Claim> claim(String worker) {
    return independent.execute(
        status -> {
          var rows =
              jdbc.queryForList(
                  "SELECT * FROM sys_job WHERE status='PENDING' AND"
                      + " scheduled_at<=CURRENT_TIMESTAMP(3) ORDER BY scheduled_at,id LIMIT 1 FOR"
                      + " UPDATE SKIP LOCKED");
          if (rows.isEmpty()) return Optional.empty();
          var row = rows.getFirst();
          long id = ((Number) row.get("id")).longValue();
          jdbc.update(
              "UPDATE sys_job SET"
                  + " status='RUNNING',worker_id=?,heartbeat_at=CURRENT_TIMESTAMP(3),started_at=CURRENT_TIMESTAMP(3),attempts=attempts+1,version=version+1"
                  + " WHERE id=?",
              worker,
              id);
          return Optional.of(
              new Claim(
                  id,
                  ((Number) row.get("site_id")).longValue(),
                  (String) row.get("module_id"),
                  (String) row.get("job_type"),
                  ((Number) row.get("created_by")).longValue(),
                  ((Number) row.get("version")).intValue() + 1,
                  ((Number) row.get("attempts")).intValue() + 1,
                  node(row.get("params"))));
        });
  }

  /**
   * Called without an ambient transaction. A lease lost during the handler can never publish a
   * result.
   */
  public void execute(Claim claim) {
    long started = System.nanoTime();
    String metricOutcome = "success";
    String previousJob = org.slf4j.MDC.get("jobId");
    org.slf4j.MDC.put("jobId", Long.toString(claim.jobId()));
    try {
      JobHandler h = handler(claim.jobType());
      enabled(claim.siteId(), claim.moduleId());
      var context = new Execution(claim);
      JobOutcome outcome;
      try (var actor =
          users.asActor(
              claim.actor() == 0
                  ? ActorRef.SYSTEM
                  : ActorRef.user(claim.actor(), Long.toString(claim.actor())))) {
        if (claim.siteId() == 0) outcome = h.execute(context);
        else {
          var base = sites.callAs(claim.siteId(), sites::require);
          try (var scope =
              sites.open(
                  new SiteContext(
                      base.siteId(),
                      base.siteCode(),
                      base.sourceLocale(),
                      base.defaultLocale(),
                      base.timezone(),
                      null,
                      ContextOrigin.JOB))) {
            outcome = h.execute(context);
          }
        }
      }
      context.checkpoint();
      finish(claim, "SUCCEEDED", outcome == null ? null : outcome.result(), null, null);
    } catch (JobCanceledException e) {
      metricOutcome = "canceled";
      finish(claim, "CANCELED", null, null, null);
    } catch (ModuleDisabledException e) {
      metricOutcome = "canceled";
      finish(claim, "CANCELED", null, "CORE_MODULE_DISABLED", "模块已停用");
    } catch (PlatformException e) {
      metricOutcome = "failure";
      fail(claim, e.errorCode().code(), e.detail());
    } catch (Exception e) {
      metricOutcome = "failure";
      org.slf4j.LoggerFactory.getLogger(getClass()).error("任务执行失败 jobId={}", claim.jobId(), e);
      fail(claim, "CORE_JOB_FAILED", "任务执行失败");
    } finally {
      if (previousJob == null) org.slf4j.MDC.remove("jobId");
      else org.slf4j.MDC.put("jobId", previousJob);
      if (metrics != null)
        metrics
            .timer("b2bweb.core.jobs.duration", "type", claim.jobType(), "outcome", metricOutcome)
            .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
  }

  private void fail(Claim c, String code, String message) {
    independent.executeWithoutResult(
        status -> {
          int changed =
              jdbc.update(
                  "UPDATE sys_job SET"
                      + " status=IF(attempts<max_attempts,'PENDING','FAILED'),scheduled_at=TIMESTAMPADD(MINUTE,POW(2,LEAST(attempts,10)),CURRENT_TIMESTAMP(3)),finished_at=IF(attempts<max_attempts,NULL,CURRENT_TIMESTAMP(3)),error_code=?,error_message=?,worker_id=NULL,version=version+1"
                      + " WHERE id=? AND status='RUNNING' AND version=?",
                  code,
                  message,
                  c.jobId(),
                  c.version());
        });
  }

  private boolean finish(Claim c, String status, JsonNode result, String code, String message) {
    return independent.execute(
        tx ->
            jdbc.update(
                    "UPDATE sys_job SET status=?,result=CAST(? AS"
                        + " JSON),error_code=?,error_message=?,finished_at=CURRENT_TIMESTAMP(3),worker_id=NULL,version=version+1"
                        + " WHERE id=? AND status='RUNNING' AND version=?",
                    status,
                    result == null ? null : result.toString(),
                    code,
                    message,
                    c.jobId(),
                    c.version())
                == 1);
  }

  public void reap() {
    for (var h : extensions.allRegardlessOfSite(JobHandler.class)) {
      long seconds = h.heartbeatTimeout().toSeconds();
      if (seconds < 1) throw new IllegalArgumentException("任务心跳超时必须为正");
      jdbc.update(
          "UPDATE sys_job SET"
              + " status=IF(attempts<max_attempts,'PENDING','FAILED'),scheduled_at=CURRENT_TIMESTAMP(3),finished_at=IF(attempts<max_attempts,NULL,CURRENT_TIMESTAMP(3)),worker_id=NULL,error_code='CORE_JOB_FAILED',error_message='执行实例失联',version=version+1"
              + " WHERE status='RUNNING' AND job_type=? AND"
              + " heartbeat_at<TIMESTAMPADD(SECOND,?,CURRENT_TIMESTAMP(3))",
          h.jobType(),
          -seconds);
    }
  }

  public void cleanup(int days) {
    if (days < 1) throw new IllegalArgumentException("任务保留期必须为正");
    for (var row :
        jdbc.queryForList(
            "SELECT id,result_file_key FROM sys_job WHERE result_expires_at<CURRENT_TIMESTAMP(3)"
                + " AND result_file_key IS NOT NULL LIMIT 500")) {
      String key = (String) row.get("result_file_key");
      storage.delete(new StorageKey(key), Visibility.PRIVATE);
      jdbc.update(
          "UPDATE sys_job SET"
              + " result_file_key=NULL,result_file_name=NULL,result_file_size=NULL,result_expires_at=NULL"
              + " WHERE id=? AND result_file_key=? AND result_expires_at<CURRENT_TIMESTAMP(3)",
          row.get("id"),
          key);
    }
    jdbc.update(
        "DELETE FROM sys_job WHERE status IN ('SUCCEEDED','FAILED','CANCELED') AND"
            + " finished_at<TIMESTAMPADD(DAY,?,CURRENT_TIMESTAMP(3)) LIMIT 1000",
        -days);
  }

  private final class Execution implements JobExecutionContext {
    private final Claim claim;
    private long lastProgress = Long.MIN_VALUE;

    Execution(Claim claim) {
      this.claim = claim;
    }

    public long jobId() {
      return claim.jobId();
    }

    public long siteId() {
      return claim.siteId();
    }

    public ActorRef initiator() {
      return claim.actor() == 0
          ? ActorRef.SYSTEM
          : ActorRef.user(claim.actor(), Long.toString(claim.actor()));
    }

    public int attempt() {
      return claim.attempt();
    }

    public JsonNode params() {
      return claim.params() == null ? json.createObjectNode() : claim.params().deepCopy();
    }

    public <T> T params(Class<T> type) {
      return json.treeToValue(params(), type);
    }

    private Map<String, Object> active() {
      var row =
          jdbc.queryForMap(
              "SELECT status,version,cancel_requested FROM sys_job WHERE id=? FOR UPDATE", jobId());
      if (!row.get("status").equals("RUNNING")
          || ((Number) row.get("version")).intValue() != claim.version()) throw state();
      return row;
    }

    private boolean canceled(Map<String, Object> row) {
      Object value = row.get("cancel_requested");
      return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue() != 0;
    }

    public boolean isCancelRequested() {
      return tx.execute(status -> canceled(active()));
    }

    public void checkpoint() {
      tx.executeWithoutResult(
          status -> {
            var row = active();
            if (canceled(row)) throw new JobCanceledException();
            enabled(siteId(), claim.moduleId());
            jdbc.update(
                "UPDATE sys_job SET heartbeat_at=CURRENT_TIMESTAMP(3) WHERE id=? AND version=? AND"
                    + " status='RUNNING'",
                jobId(),
                claim.version());
          });
    }

    public void progress(Integer total, int done, int failed, String message) {
      if (done < 0
          || failed < 0
          || total != null && (total < 0 || done + failed > total)
          || message != null && message.length() > 300)
        throw new IllegalArgumentException("任务进度无效");
      long now = clock.millis();
      if (lastProgress != Long.MIN_VALUE && now - lastProgress < 1000) return;
      tx.executeWithoutResult(
          status -> {
            var row = active();
            if (canceled(row)) throw new JobCanceledException();
            jdbc.update(
                "UPDATE sys_job SET"
                    + " progress_total=?,progress_done=?,progress_failed=?,progress_message=?,heartbeat_at=CURRENT_TIMESTAMP(3)"
                    + " WHERE id=? AND version=? AND status='RUNNING'",
                total,
                done,
                failed,
                message,
                jobId(),
                claim.version());
          });
      lastProgress = now;
    }

    public InputStream openInputFile(String file) {
      var key = new StorageKey(file);
      if (!key.value().startsWith("jobs/") || !key.belongsToSite(siteId()))
        throw new ForbiddenException(CoreErrors.FORBIDDEN, "不能读取其他站点的任务文件");
      checkpoint();
      return storage.get(key, Visibility.PRIVATE);
    }

    public void writeResultFile(String name, String type, ThrowingConsumer<OutputStream> writer)
        throws IOException {
      if (name.isBlank() || name.length() > 255 || name.contains("\r") || name.contains("\n"))
        throw new IllegalArgumentException("任务结果文件名无效");
      checkpoint();
      Path temporary = Files.createTempFile("b2b-job-", ".bin");
      var key = fileKey(siteId());
      boolean stored = false;
      try {
        try (var output = Files.newOutputStream(temporary)) {
          writer.accept(output);
        }
        long size = Files.size(temporary);
        try (var input = Files.newInputStream(temporary)) {
          storage.put(key, input, size, type, Visibility.PRIVATE);
        }
        stored = true;
        String previous =
            tx.execute(
                status -> {
                  var row = active();
                  if (canceled(row)) throw new JobCanceledException();
                  String old =
                      jdbc.queryForObject(
                          "SELECT result_file_key FROM sys_job WHERE id=?", String.class, jobId());
                  jdbc.update(
                      "UPDATE sys_job SET"
                          + " result_file_key=?,result_file_name=?,result_file_size=?,result_expires_at=?"
                          + " WHERE id=? AND status='RUNNING' AND version=?",
                      key.value(),
                      name,
                      size,
                      Timestamp.from(clock.instant().plus(Duration.ofDays(7))),
                      jobId(),
                      claim.version());
                  return old;
                });
        if (org.springframework.transaction.support.TransactionSynchronizationManager
            .isActualTransactionActive()) {
          final StorageKey added = key;
          final String old = previous;
          org.springframework.transaction.support.TransactionSynchronizationManager
              .registerSynchronization(
                  new org.springframework.transaction.support.TransactionSynchronization() {
                    public void afterCommit() {
                      if (old != null) storage.delete(new StorageKey(old), Visibility.PRIVATE);
                    }

                    public void afterCompletion(int result) {
                      if (result != STATUS_COMMITTED) storage.delete(added, Visibility.PRIVATE);
                    }
                  });
          stored = false;
        } else {
          stored = false;
          if (previous != null) storage.delete(new StorageKey(previous), Visibility.PRIVATE);
        }
      } finally {
        Files.deleteIfExists(temporary);
        if (stored) storage.delete(key, Visibility.PRIVATE);
      }
    }
  }

  private JsonNode node(Object value) {
    return value == null ? null : json.readTree(value.toString());
  }

  private static Long number(Map<String, Object> row, String key) {
    return row.get(key) == null ? null : ((Number) row.get(key)).longValue();
  }

  private static Instant instant(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null) return null;
    return value instanceof Timestamp t
        ? t.toInstant()
        : ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
  }

  private JobInfo info(Map<String, Object> r) {
    Integer total =
        r.get("progress_total") == null ? null : ((Number) r.get("progress_total")).intValue();
    return new JobInfo(
        number(r, "id"),
        (String) r.get("module_id"),
        (String) r.get("job_type"),
        (String) r.get("title"),
        JobStatus.valueOf((String) r.get("status")),
        number(r, "parent_job_id"),
        new JobProgress(
            total,
            ((Number) r.get("progress_done")).intValue(),
            ((Number) r.get("progress_failed")).intValue(),
            (String) r.get("progress_message")),
        node(r.get("result")),
        r.get("result_file_key") == null
            ? null
            : new JobFile(
                (String) r.get("result_file_name"),
                number(r, "result_file_size"),
                instant(r, "result_expires_at")),
        (String) r.get("error_code"),
        (String) r.get("error_message"),
        ((Number) r.get("attempts")).intValue(),
        ((Number) r.get("max_attempts")).intValue(),
        Boolean.TRUE.equals(r.get("cancel_requested"))
            || r.get("cancel_requested") instanceof Number n && n.intValue() != 0,
        number(r, "created_by"),
        instant(r, "created_at"),
        instant(r, "started_at"),
        instant(r, "finished_at"));
  }
}
