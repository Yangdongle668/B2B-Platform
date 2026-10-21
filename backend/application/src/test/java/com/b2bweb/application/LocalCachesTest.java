package com.b2bweb.application;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.core.support.cache.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
@SpringBootTest(properties="spring.profiles.active=test")
class LocalCachesTest {
 @Autowired RedisConnectionFactory factory; @Autowired JsonMapper json; @Autowired PlatformTransactionManager manager;
 @Test void commitInvalidatesAgainAndBroadcastsToAnotherInstance() throws Exception {
  try(var first=new LocalCaches(factory,json);var second=new LocalCaches(factory,json)){
   BroadcastCache<String,String> a=first.create("core.contract",new LocalCacheSpec(20,Duration.ofSeconds(10)));
   BroadcastCache<String,String> b=second.create("core.contract",new LocalCacheSpec(20,Duration.ofSeconds(10)));
   assertThat(a.get("key",key->"old")).isEqualTo("old"); assertThat(b.get("key",key->"old")).isEqualTo("old");
   new TransactionTemplate(manager).executeWithoutResult(status->{a.invalidate("key"); assertThat(a.get("key",key->"old-in-transaction")).isEqualTo("old-in-transaction"); assertThat(b.getIfPresent("key")).contains("old");});
   assertThat(a.getIfPresent("key")).isEmpty();
   org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(()->assertThat(b.getIfPresent("key")).isEmpty());
   a.get("one",key->"one");b.get("two",key->"two");a.invalidateAll();
   org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(()->assertThat(b.getIfPresent("two")).isEmpty());
  }
 }
 @Test void rollbackDoesNotPublishAndTtlBoundsLostMessages() throws Exception {
  try(var first=new LocalCaches(factory,json);var second=new LocalCaches(factory,json)){
   BroadcastCache<String,String> a=first.create("core.rollback",new LocalCacheSpec(2,Duration.ofMillis(250)));
   BroadcastCache<String,String> b=second.create("core.rollback",new LocalCacheSpec(2,Duration.ofSeconds(10)));
   a.get("key",key->"old");b.get("key",key->"old");
   new TransactionTemplate(manager).executeWithoutResult(status->{a.invalidate("key");status.setRollbackOnly();});
   assertThat(b.getIfPresent("key")).contains("old");
   a.get("key",key->"current"); org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(()->assertThat(a.getIfPresent("key")).isEmpty());
   assertThatThrownBy(()->first.create("core.rollback",new LocalCacheSpec(1,Duration.ofSeconds(1)))).isInstanceOf(IllegalArgumentException.class);
  }
 }
}
