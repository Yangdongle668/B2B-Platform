package com.b2bweb.application.dev;

import com.b2bweb.api.content.PreviewPurpose;
import com.b2bweb.api.security.RequiresPermission;
import com.b2bweb.testkit.TestPlatform;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Local-only, authenticated fixture issuer. Not compiled into the production artifact. */
@Profile("local")
@io.swagger.v3.oas.annotations.Hidden
@RestController
public class PreviewFixtureController {
  private final TestPlatform platform;
  public PreviewFixtureController(TestPlatform platform) { this.platform = platform; }
  @PostMapping("/api/admin/v1/system/m0/preview-fixture")
  @RequiresPermission("content:page:edit")
  public Map<String, Object> create(@RequestParam(defaultValue = "EDITOR") PreviewPurpose purpose) {
    return platform.previewFixture(purpose);
  }
}
