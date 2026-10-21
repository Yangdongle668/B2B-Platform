package com.b2bweb.application;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.redis.*;
import com.b2bweb.testkit.TestPlatform;
import com.b2bweb.testkit.contract.port.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class RedisRuntimeContractsTest {
  @Autowired StringRedisTemplate redis;
  final TestPlatform platform = TestPlatform.standard();

  void advanceTime(Duration elapsed) {
    try {
      Thread.sleep(elapsed.plusMillis(30));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  @BeforeEach
  void clean() {
    redis.delete(
        List.of(
            "s1:rl:form.submit:hash",
            "s2:rl:form.submit:hash",
            "s1:buf:counter:url.not_found",
            "s2:buf:counter:url.not_found",
            "s1:buf:queue:graph.recompute",
            "s2:buf:queue:graph.recompute"));
  }

  @Nested
  class Locks extends DistributedLockContract {
    final DistributedLock api = new RedisDistributedLock(redis, platform.siteContextProvider());

    protected void site(Long id) {
      if (id == null) platform.siteContextProvider().clear();
      else platform.useSite(id);
    }

    protected DistributedLock api() {
      return api;
    }

    protected void advance(Duration d) {
      advanceTime(d);
    }
  }

  @Nested
  class Buffers extends BufferStoreContract {
    final BufferStore api = new RedisBufferStore(redis, platform.siteContextProvider());

    protected BufferStore api() {
      return api;
    }

    protected void site(long id) {
      platform.useSite(id);
    }

    protected void advance(Duration d) {
      advanceTime(d);
    }
  }

  @Nested
  class Limits extends RateLimiterContract {
    org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory unavailable;
    RateLimiter api = new RedisRateLimiter(redis, platform.siteContextProvider());

    protected RateLimiter api() {
      return api;
    }

    protected void site(long id) {
      platform.useSite(id);
    }

    protected void advance(Duration d) {
      advanceTime(d);
    }

    protected void available(boolean value) {
      if (!value) {
        unavailable =
            new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(
                new org.springframework.data.redis.connection.RedisStandaloneConfiguration(
                    "127.0.0.1", 1),
                org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
                    .builder()
                    .commandTimeout(Duration.ofMillis(100))
                    .build());
        unavailable.afterPropertiesSet();
        unavailable.start();
        api =
            new RedisRateLimiter(
                new StringRedisTemplate(unavailable), platform.siteContextProvider());
      }
    }

    @AfterEach
    void stop() {
      if (unavailable != null) unavailable.destroy();
    }
  }
}
