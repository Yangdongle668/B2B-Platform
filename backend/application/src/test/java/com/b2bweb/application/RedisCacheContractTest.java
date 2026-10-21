package com.b2bweb.application;
import com.b2bweb.api.port.TaggedCache;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.SiteContexts;
import com.b2bweb.core.cache.RedisTaggedCache;
import com.b2bweb.testkit.contract.port.TaggedCacheContract;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties="spring.profiles.active=test")
class RedisCacheContractTest extends TaggedCacheContract {
  @Autowired RedisTaggedCache cache;
  @Autowired SiteContexts sites;
  boolean ready;
  protected TaggedCache api(){return cache;}
  protected void site(long id){
    if(!ready){var boundary=cache.beginRecovery(UUID.randomUUID().toString()); org.assertj.core.api.Assertions.assertThat(cache.finishRecovery(boundary.get(0),boundary.get(1))).isTrue();ready=true;}
    sites.open(new SiteContext(id,"cache-"+id,"zh-CN","zh-CN",ZoneOffset.UTC,null,ContextOrigin.SYSTEM));
  }
  protected Instant now(){return Instant.now();}
  @org.junit.jupiter.api.AfterEach void clear(){sites.withoutSite();}
}
