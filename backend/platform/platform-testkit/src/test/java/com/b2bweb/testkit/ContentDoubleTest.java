package com.b2bweb.testkit;

import com.b2bweb.api.content.ContentApi;
import com.b2bweb.testkit.contract.content.ContentReadWriteContract;
import java.time.Duration;

class ContentDoubleTest extends ContentReadWriteContract {
  TestPlatform platform;

  protected ContentApi api() {
    return platform.contentApi();
  }

  protected void site(long id) {
    platform.useSite(id);
  }

  protected void reset() {
    platform = TestPlatform.standard();
  }

  protected void advance(Duration duration) {
    platform.clock().advance(duration);
  }
}
