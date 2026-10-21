package com.b2bweb.application.dev;

import com.b2bweb.api.content.*;
import com.b2bweb.api.form.FormApi;
import com.b2bweb.api.media.MediaApi;
import com.b2bweb.api.notification.NotificationApi;
import com.b2bweb.api.port.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.taxonomy.TaxonomyApi;
import com.b2bweb.api.translation.TranslationApi;
import com.b2bweb.api.url.UrlApi;
import com.b2bweb.testkit.TestPlatform;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;

/**
 * Included only in the development Maven profile. Data is ephemeral and no business HTTP endpoints
 * are invented.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
@Profile("local")
public class LocalStandIns {
  @Bean
  TestPlatform localTestPlatform(
      SiteContextProvider sites,
      SiteApi api,
      CurrentUserProvider users,
      UserDirectory directory,
      Clock clock) {
    return TestPlatform.empty().bridge(sites, api, users, directory, clock);
  }

  @Bean
  @ConditionalOnMissingBean
  ContentApi localContent(TestPlatform p) {
    return p.implementation(ContentApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  UrlApi localUrls(TestPlatform p) {
    return p.implementation(UrlApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  MediaApi localMedia(TestPlatform p) {
    return p.implementation(MediaApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  TaxonomyApi localTaxonomy(TestPlatform p) {
    return p.implementation(TaxonomyApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  FormApi localForms(TestPlatform p) {
    return p.implementation(FormApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  NotificationApi localNotifications(TestPlatform p) {
    return p.implementation(NotificationApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  TranslationApi localTranslation(TestPlatform p) {
    return p.implementation(TranslationApi.class);
  }

  @Bean
  @ConditionalOnMissingBean
  ImageUrlSigner localImageUrlSigner(TestPlatform p) {
    return p.implementation(ImageUrlSigner.class);
  }

  @Bean
  @ConditionalOnMissingBean
  VirusScanner localVirusScanner(TestPlatform p) {
    return p.implementation(VirusScanner.class);
  }

  @Bean
  @ConditionalOnMissingBean
  WebhookTransport localWebhookTransport(TestPlatform p) {
    return p.implementation(WebhookTransport.class);
  }

  @Bean
  @ConditionalOnMissingBean
  CdnPurger localCdnPurger(TestPlatform p) {
    return p.implementation(CdnPurger.class);
  }

  @Bean
  @ConditionalOnMissingBean
  IndexNowClient localIndexNowClient(TestPlatform p) {
    return p.implementation(IndexNowClient.class);
  }

  @Bean
  @ConditionalOnMissingBean
  TranslationEngine localTranslationEngine(TestPlatform p) {
    return p.implementation(TranslationEngine.class);
  }

  @Bean
  @ConditionalOnMissingBean
  CaptchaVerifier localCaptchaVerifier(TestPlatform p) {
    return p.implementation(CaptchaVerifier.class);
  }

  @Bean
  @ConditionalOnMissingBean
  GeoIpResolver localGeoIpResolver(TestPlatform p) {
    return p.implementation(GeoIpResolver.class);
  }

  @Bean
  @ConditionalOnMissingBean
  MxRecordChecker localMxRecordChecker(TestPlatform p) {
    return p.implementation(MxRecordChecker.class);
  }

  @Bean
  @ConditionalOnMissingBean
  RemoteFileFetcher localRemoteFileFetcher(TestPlatform p) {
    return p.implementation(RemoteFileFetcher.class);
  }

  @Bean
  @ConditionalOnMissingBean
  TaggedCache localTaggedCache(TestPlatform p) {
    return p.implementation(TaggedCache.class);
  }

  @Bean
  @ConditionalOnMissingBean
  RateLimiter localRateLimiter(TestPlatform p) {
    return p.implementation(RateLimiter.class);
  }

  @Bean
  @ConditionalOnMissingBean
  DistributedLock localDistributedLock(TestPlatform p) {
    return p.implementation(DistributedLock.class);
  }

  @Bean
  @ConditionalOnMissingBean
  BufferStore localBufferStore(TestPlatform p) {
    return p.implementation(BufferStore.class);
  }
}
