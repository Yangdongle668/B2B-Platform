package com.b2bweb.core.job;

import com.b2bweb.api.site.SettingsApi;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobMaintenance {
  private final DatabaseJobs jobs;
  private final SettingsApi settings;

  public JobMaintenance(DatabaseJobs jobs, SettingsApi settings) {
    this.jobs = jobs;
    this.settings = settings;
  }

  @Scheduled(cron = "0 20 3 * * *", zone = "UTC")
  public void cleanup() {
    jobs.cleanup(
        settings
            .getSystem("core.retention", tools.jackson.databind.JsonNode.class)
            .path("jobDays")
            .asInt());
  }
}
