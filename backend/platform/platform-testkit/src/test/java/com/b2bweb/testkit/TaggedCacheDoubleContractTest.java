package com.b2bweb.testkit;
import com.b2bweb.api.port.TaggedCache;
import com.b2bweb.testkit.contract.port.TaggedCacheContract;
import java.time.Instant;
class TaggedCacheDoubleContractTest extends TaggedCacheContract {
  final TestPlatform platform=TestPlatform.standard();
  protected TaggedCache api(){return platform.implementation(TaggedCache.class);}
  protected void site(long id){platform.useSite(id);}
  protected Instant now(){return platform.clock().instant();}
}
