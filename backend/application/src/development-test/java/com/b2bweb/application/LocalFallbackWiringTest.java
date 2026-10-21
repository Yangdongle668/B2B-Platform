package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.content.ContentApi;
import com.b2bweb.api.port.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
    properties = {
      "spring.profiles.active=local",
      "b2bweb.core.jobs.enabled=false",
      "b2bweb.core.scheduling.enabled=false",
      "b2bweb.core.cache.recovery-enabled=false"
    })
class LocalFallbackWiringTest {
  @Autowired ApplicationContext context;

  @Test
  void localProfileHasExactlyOneProviderPerPortAndRealAdaptersWin() {
    for (Class<?> port :
        List.of(
            ObjectStorage.class,
            ImageUrlSigner.class,
            VirusScanner.class,
            MailTransport.class,
            WebhookTransport.class,
            SearchEngineClient.class,
            CdnPurger.class,
            IndexNowClient.class,
            TranslationEngine.class,
            CaptchaVerifier.class,
            GeoIpResolver.class,
            MxRecordChecker.class,
            RemoteFileFetcher.class,
            TaggedCache.class,
            RateLimiter.class,
            DistributedLock.class,
            BufferStore.class))
      assertThat(context.getBeanNamesForType(port)).as(port.getSimpleName()).hasSize(1);
    assertThat(context.getBeanNamesForType(RateLimiter.class)).containsExactly("rateLimiter");
    assertThat(context.getBeanNamesForType(WebhookTransport.class))
        .containsExactly("webhookTransport");
    assertThat(context.getBeanNamesForType(ContentApi.class)).containsExactly("localContent");
  }
}
