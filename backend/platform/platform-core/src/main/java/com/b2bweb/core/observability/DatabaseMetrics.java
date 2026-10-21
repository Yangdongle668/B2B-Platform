package com.b2bweb.core.observability;
import com.b2bweb.core.module.ModuleMigrations;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** A failed scrape is NaN, never a misleading empty queue. No personal data or IDs become labels. */
@Component
public final class DatabaseMetrics implements MeterBinder {
 private final JdbcTemplate jdbc;
 public DatabaseMetrics(JdbcTemplate jdbc,ModuleMigrations migrated){this.jdbc=jdbc;}
 private double query(String sql,Object... args){try{Number value=jdbc.queryForObject(sql,Number.class,args);return value==null?0:value.doubleValue();}catch(org.springframework.dao.DataAccessException e){return Double.NaN;}}
 public void bindTo(MeterRegistry registry){
  Gauge.builder("b2bweb.core.events.incomplete.count",this,m->m.query("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL")).register(registry);
  Gauge.builder("b2bweb.core.events.incomplete.oldest_age",this,m->m.query("SELECT COALESCE(GREATEST(0,TIMESTAMPDIFF(SECOND,MIN(PUBLICATION_DATE),CURRENT_TIMESTAMP(3))),0) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL")).baseUnit("seconds").register(registry);
  for(String status:java.util.List.of("PENDING","RUNNING"))Gauge.builder("b2bweb.core.jobs.queue",this,m->m.query("SELECT COUNT(*) FROM sys_job WHERE status=?",status)).tag("status",status).register(registry);
 }
}
