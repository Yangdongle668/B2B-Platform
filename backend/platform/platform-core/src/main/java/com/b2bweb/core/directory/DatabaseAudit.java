package com.b2bweb.core.directory;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class DatabaseAudit implements AuditApi {
  private io.micrometer.tracing.Tracer tracer;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public void tracing(io.micrometer.tracing.Tracer value) {
    tracer = value;
  }

  final JdbcTemplate jdbc;
  final IdGenerator ids;
  final SiteContextProvider sites;
  final CurrentUserProvider users;
  final TransactionTemplate joined, independent;
  final JsonMapper json;
  final java.time.Clock clock;

  public DatabaseAudit(
      JdbcTemplate jdbc,
      IdGenerator ids,
      SiteContextProvider sites,
      CurrentUserProvider users,
      PlatformTransactionManager manager) {
    this(jdbc, ids, sites, users, manager, java.time.Clock.systemUTC());
  }

  public DatabaseAudit(
      JdbcTemplate jdbc,
      IdGenerator ids,
      SiteContextProvider sites,
      CurrentUserProvider users,
      PlatformTransactionManager manager,
      java.time.Clock clock) {
    this.clock = clock;
    var mask = new tools.jackson.databind.module.SimpleModule("audit-secret-mask");
    mask.addSerializer(
        Secret.class,
        new tools.jackson.databind.ValueSerializer<Secret>() {
          public void serialize(
              Secret value,
              tools.jackson.core.JsonGenerator generator,
              tools.jackson.databind.SerializationContext context) {
            generator.writeString("****");
          }
        });
    json = JsonMapper.builder().addModule(mask).build();
    this.jdbc = jdbc;
    this.ids = ids;
    this.sites = sites;
    this.users = users;
    joined = new TransactionTemplate(manager);
    independent = new TransactionTemplate(manager);
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void record(AuditEntry value) {
    joined.executeWithoutResult(tx -> write(value));
  }

  public void recordIndependently(AuditEntry value) {
    independent.executeWithoutResult(tx -> write(value));
  }

  void write(AuditEntry value) {
    Map<String, AuditChange> diff = new LinkedHashMap<>();
    value
        .diff()
        .forEach(
            (key, change) ->
                diff.put(
                    key,
                    key.matches("(?i).*(password|secret|token|recovery|api.?key|authorization).*")
                            || change.from() instanceof Secret
                            || change.to() instanceof Secret
                        ? AuditChange.masked()
                        : change));
    String encoded = json.writeValueAsString(diff);
    if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536) {
      diff.put("__truncated", new AuditChange(null, true));
      var keys = new ArrayList<>(diff.keySet());
      keys.remove("__truncated");
      Collections.reverse(keys);
      for (String key : keys) {
        diff.remove(key);
        encoded = json.writeValueAsString(diff);
        if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 65536) break;
      }
    }
    var attributes =
        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    jakarta.servlet.http.HttpServletRequest request =
        attributes
                instanceof org.springframework.web.context.request.ServletRequestAttributes servlet
            ? servlet.getRequest()
            : null;
    String agent = request == null ? null : request.getHeader("User-Agent");
    if (agent != null && agent.length() > 500) agent = agent.substring(0, 500);
    String trace =
        tracer != null && tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId()
            : org.slf4j.MDC.get("traceId");
    if (trace != null && !trace.matches("[a-fA-F0-9]{32}")) trace = null;
    var actor = users.actor();
    jdbc.update(
        "INSERT INTO"
            + " sys_audit_log(id,site_id,user_id,user_name,module_id,action,target_type,target_id,summary,diff,result,is_sensitive,ip,user_agent,trace_id,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,CAST(? AS JSON),?,?,?,?,?,?)",
        ids.nextId(),
        value.systemLevel() ? null : sites.requireSiteId(),
        actor.userId(),
        actor.displayName(),
        value.moduleId(),
        value.action(),
        value.targetType(),
        value.targetId(),
        value.summary(),
        encoded,
        value.result().name(),
        value.sensitive(),
        request == null ? null : request.getRemoteAddr(),
        agent,
        trace,
        java.sql.Timestamp.from(clock.instant()));
  }
}
