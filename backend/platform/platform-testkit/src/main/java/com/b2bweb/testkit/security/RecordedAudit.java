package com.b2bweb.testkit.security;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordedAudit implements AuditApi {
  public record Recorded(Long siteId, long actorId, AuditEntry entry) {}

  private final tools.jackson.databind.json.JsonMapper json;
  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final CurrentUserProvider users;
  private final List<Recorded> independent = new CopyOnWriteArrayList<>();

  public RecordedAudit(MemoryStore store, SiteContextProvider sites, CurrentUserProvider users) {
    var mask=new tools.jackson.databind.module.SimpleModule("audit-secret-mask");
    mask.addSerializer(com.b2bweb.api.common.Secret.class,new tools.jackson.databind.ValueSerializer<com.b2bweb.api.common.Secret>() {
      public void serialize(com.b2bweb.api.common.Secret value,tools.jackson.core.JsonGenerator generator,tools.jackson.databind.SerializationContext context){generator.writeString("****");}
    });
    json=tools.jackson.databind.json.JsonMapper.builder().addModule(mask).build();
    this.store = store;
    this.sites = sites;
    this.users = users;
  }

  private Recorded entry(AuditEntry value) {
    Map<String, AuditChange> diff = new LinkedHashMap<>();
    value
        .diff()
        .forEach(
            (key, change) ->
                diff.put(
                    key,
                    key.matches("(?i).*(password|secret|token|recovery|api.?key|authorization).*") || change.from() instanceof com.b2bweb.api.common.Secret || change.to() instanceof com.b2bweb.api.common.Secret
                        ? AuditChange.masked()
                        : new AuditChange(snapshot(change.from()),snapshot(change.to()))));
    String encoded=json.writeValueAsString(diff);
    if(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536){
      diff.put("__truncated",new AuditChange(null,true));var keys=new ArrayList<>(diff.keySet());keys.remove("__truncated");Collections.reverse(keys);
      for(String key:keys){diff.remove(key);if(json.writeValueAsString(diff).getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=65536)break;}
    }
    var masked =
        new AuditEntry(
            value.moduleId(),
            value.action(),
            value.targetType(),
            value.targetId(),
            value.summary(),
            diff,
            value.result(),
            value.sensitive(),
            value.systemLevel());
    return new Recorded(
        value.systemLevel() ? null : sites.requireSiteId(),
        users.actor().userId(),
        masked);
  }

  private Object snapshot(Object value){return value==null?null:json.readValue(json.writeValueAsString(value),Object.class);}

  @SuppressWarnings("unchecked")
  public void record(AuditEntry value) {
    var captured = entry(value);
    store.atomic(
        rows -> {
          var found =
              new ArrayList<>((List<Recorded>) rows.getOrDefault("audit-records", List.of()));
          found.add(captured);
          rows.put("audit-records", List.copyOf(found));
          return null;
        });
  }

  public void recordIndependently(AuditEntry value) {
    independent.add(entry(value));
  }

  @SuppressWarnings("unchecked")
  public List<Recorded> records() {
    return store.atomic(
        rows -> {
          var all = new ArrayList<>((List<Recorded>) rows.getOrDefault("audit-records", List.of()));
          all.addAll(independent);
          return List.copyOf(all);
        });
  }
}
