package com.b2bweb.testkit.contract.security;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class AuditApiContract {
 protected abstract AuditApi api();
 protected abstract void site(long id);
 protected abstract void transaction(Runnable action,boolean rollback);
 protected abstract List<AuditEntry> records();
 protected AuditEntry entry(String action){return new AuditEntry("core",action,null,null,"测试审计",Map.of("apiKey",new AuditChange("before","after"),"credential",new AuditChange(null,Secret.of("sensitive-fixture-value"))),AuditResult.SUCCESS,true,false);}
 @Test public void joinedAuditRollsBackButIndependentFailureSurvives() {
  site(1);transaction(()->api().record(entry("contract.rollback")),true);assertThat(records()).isEmpty();
  transaction(()->{api().record(entry("contract.commit"));api().recordIndependently(entry("contract.independent"));},false);
  assertThat(records()).hasSize(2);for(var item:records()){assertThat(item.diff().get("apiKey")).isEqualTo(AuditChange.masked());assertThat(item.diff().get("credential")).isEqualTo(AuditChange.masked());}
  transaction(()->{api().record(entry("contract.rollback"));api().recordIndependently(entry("contract.independent.rollback"));},true);assertThat(records()).hasSize(3);
 }

}
