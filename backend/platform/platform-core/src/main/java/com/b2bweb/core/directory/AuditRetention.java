package com.b2bweb.core.directory;
import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.Extension;
import com.b2bweb.api.site.SettingsApi;
import java.sql.Timestamp;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
/** The sole audit deletion path: bounded batches, run under the scheduler's database lock. */
@Extension(module="core")
public final class AuditRetention implements ScheduledTask {
 private final JdbcTemplate jdbc;
 private final SettingsApi settings;
 private final Clock clock;
 public AuditRetention(JdbcTemplate jdbc,SettingsApi settings,Clock clock){this.jdbc=jdbc;this.settings=settings;this.clock=clock;}
 public String name(){return "core.audit_retention";}
 public String cron(){return "0 30 3 * * *";}
 public ScheduleScope scope(){return ScheduleScope.SYSTEM;}
 public void run(ScheduledTaskContext context){clean();}
 public int clean(){
  int days=settings.getSystem("core.retention",tools.jackson.databind.JsonNode.class).path("auditDays").asInt();
  if(days<1)throw new IllegalStateException("审计保留期限必须大于0");
  var cutoff=Timestamp.from(clock.instant().minus(Duration.ofDays(days)));int total=0;
  for(int batch=0;batch<10;batch++){
   int count=jdbc.update("DELETE FROM sys_audit_log WHERE created_at<? ORDER BY created_at,id LIMIT 1000",cutoff);total+=count;if(count<1000)break;
  }
  return total;
 }
}
