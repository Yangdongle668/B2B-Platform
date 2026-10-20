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
  final JdbcTemplate jdbc;
  final IdGenerator ids;
  final SiteContextProvider sites;
  final CurrentUserProvider users;
  final TransactionTemplate joined, independent;

  public DatabaseAudit(
      JdbcTemplate jdbc,
      IdGenerator ids,
      SiteContextProvider sites,
      CurrentUserProvider users,
      PlatformTransactionManager manager) {
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
                        ? AuditChange.masked()
                        : change));
    var actor = users.actor();
    jdbc.update(
        "INSERT INTO"
            + " sys_audit_log(id,site_id,user_id,user_name,module_id,action,target_type,target_id,summary,diff,result,is_sensitive)"
            + " VALUES(?,?,?,?,?,?,?,?,?,CAST(? AS JSON),?,?)",
        ids.nextId(),
        value.systemLevel() ? null : sites.requireSiteId(),
        actor.userId(),
        actor.displayName(),
        value.moduleId(),
        value.action(),
        value.targetType(),
        value.targetId(),
        value.summary(),
        JsonMapper.builder().build().writeValueAsString(diff),
        value.result().name(),
        value.sensitive());
  }
}
