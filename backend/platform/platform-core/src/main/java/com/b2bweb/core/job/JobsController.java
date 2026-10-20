package com.b2bweb.core.job;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/system/jobs")
public class JobsController {
  private final JobApi jobs;
  private final CurrentUserProvider users;
  private final PermissionChecker permissions;

  public JobsController(JobApi jobs, CurrentUserProvider users, PermissionChecker permissions) {
    this.jobs = jobs;
    this.users = users;
    this.permissions = permissions;
  }

  private JobInfo visible(long id, boolean write) {
    var user = users.require();
    var job =
        jobs.find(id).orElseThrow(() -> new NotFoundException(CoreErrors.JOB_NOT_FOUND, "任务不存在"));
    if (job.createdBy() != user.userId()
        && !user.systemAdmin()
        && (write || !permissions.has("system:job:view")))
      throw new ForbiddenException(CoreErrors.FORBIDDEN, "没有任务访问权限");
    return job;
  }

  @GetMapping
  public PageResult<JobInfo> list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return jobs.list(
        new JobQuery(
            null,
            null,
            Set.of(),
            permissions.has("system:job:view") ? null : users.require().userId()),
        PageRequest.of(page, size));
  }

  @GetMapping("/{id}")
  public JobInfo find(@PathVariable long id) {
    return visible(id, false);
  }

  @PostMapping("/{id}/cancel")
  public void cancel(@PathVariable long id) {
    visible(id, true);
    jobs.requestCancel(id);
  }

  @PostMapping("/{id}/retry")
  public JobInfo retry(@PathVariable long id) {
    visible(id, true);
    return jobs.retry(id);
  }

  public record Download(URI url) {}

  @GetMapping("/{id}/result-download")
  public Download download(@PathVariable long id) {
    visible(id, false);
    return new Download(jobs.resultDownloadUrl(id, Duration.ofMinutes(5)));
  }
}
