package com.b2bweb.testkit;

import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.*;
import com.b2bweb.testkit.event.*;
import com.b2bweb.testkit.fixture.*;
import com.b2bweb.testkit.form.*;
import com.b2bweb.testkit.notification.*;
import com.b2bweb.testkit.port.*;
import com.b2bweb.testkit.security.*;
import com.b2bweb.testkit.site.*;
import com.b2bweb.testkit.translation.*;
import java.time.*;
import java.util.*;

/** Explicit, stateful doubles only. Unsupported service families are not default-return proxies. */
public final class TestPlatform {
  final MemoryStore store = new MemoryStore();
  final ManualClock clock = new ManualClock();
  final FakeIdGenerator ids = new FakeIdGenerator(clock);
  final FakeSiteContextProvider sites = new FakeSiteContextProvider(false);
  final FakeCurrentUserProvider users = new FakeCurrentUserProvider();
  final FakePermissionChecker permissions = new FakePermissionChecker(sites, users);
  final RecordedEvents events = new RecordedEvents();
  final com.b2bweb.api.event.EventMetaFactory meta =
      new com.b2bweb.api.event.EventMetaFactory() {
        public com.b2bweb.api.event.EventMeta next() {
          return next(
              sites.current().map(com.b2bweb.api.site.SiteContext::siteId).orElse(null),
              users.actor());
        }

        public com.b2bweb.api.event.EventMeta next(
            Long site, com.b2bweb.api.common.ActorRef actor) {
          return new com.b2bweb.api.event.EventMeta(
              ids.nextIdString(), clock.instant(), site, actor.toEventActor());
        }
      };
  final InMemorySiteApi siteApi = new InMemorySiteApi(store, sites);
  final InMemoryUiTextApi texts = new InMemoryUiTextApi(store, sites);
  final InMemoryTaggedCache cache = new InMemoryTaggedCache(sites, clock);
  final InMemoryBufferStore buffer = new InMemoryBufferStore(sites, clock);
  final InMemoryFormApi forms = new InMemoryFormApi(store, sites);
  final InMemoryNotificationApi notifications =
      new InMemoryNotificationApi(store, sites, clock, ids);
  final com.b2bweb.testkit.directory.InMemoryUsers directory =
      new com.b2bweb.testkit.directory.InMemoryUsers(store, sites, users, permissions);
  final com.b2bweb.testkit.content.InMemoryContentApi content =
      new com.b2bweb.testkit.content.InMemoryContentApi(
          store, sites, siteApi, users, directory, ids, clock, meta, events::publish);
  final InMemoryTranslationApi translation = new InMemoryTranslationApi(sites, clock);
  final InMemoryObjectStorage storage = new InMemoryObjectStorage(clock);
  final com.b2bweb.testkit.job.InMemoryJobApi jobs =
      new com.b2bweb.testkit.job.InMemoryJobApi(store, sites, users, siteApi, ids, clock, storage);

  public com.b2bweb.testkit.job.InMemoryJobApi jobApi() {
    return jobs;
  }

  public InMemoryObjectStorage objectStorage() {
    return storage;
  }

  final com.b2bweb.testkit.security.FakeSecretCipher cipher =
      new com.b2bweb.testkit.security.FakeSecretCipher();
  final com.b2bweb.testkit.settings.InMemorySettingsApi settings =
      new com.b2bweb.testkit.settings.InMemorySettingsApi(
          store, sites, siteApi, cipher, clock, meta, events::publish);
  final com.b2bweb.testkit.directory.InMemoryDictApi dicts =
      new com.b2bweb.testkit.directory.InMemoryDictApi(store);
  final com.b2bweb.testkit.directory.InMemorySiteProfiles profiles =
      new com.b2bweb.testkit.directory.InMemorySiteProfiles(store, sites);
  final com.b2bweb.testkit.security.RecordedAudit audit =
      new com.b2bweb.testkit.security.RecordedAudit(store, sites, users);

  public com.b2bweb.testkit.settings.InMemorySettingsApi settingsApi() {
    return settings;
  }

  public com.b2bweb.testkit.directory.InMemoryDictApi dictApi() {
    return dicts;
  }

  public com.b2bweb.testkit.directory.InMemorySiteProfiles siteProfileApi() {
    return profiles;
  }

  public com.b2bweb.testkit.directory.InMemoryUsers userDirectory() {
    return directory;
  }

  public com.b2bweb.testkit.security.FakeSecretCipher secretCipher() {
    return cipher;
  }

  public com.b2bweb.testkit.security.RecordedAudit auditApi() {
    return audit;
  }

  public com.b2bweb.testkit.extension.FakeExtensionRegistry extensionRegistry(
      Object... implementations) {
    return new com.b2bweb.testkit.extension.FakeExtensionRegistry(
        Arrays.asList(implementations), sites, siteApi::isModuleEnabled);
  }

  final FakeImageUrlSigner imageSigner = new FakeImageUrlSigner();
  final FakeRemoteFileFetcher remote = new FakeRemoteFileFetcher();
  final com.b2bweb.testkit.media.InMemoryMediaApi media =
      new com.b2bweb.testkit.media.InMemoryMediaApi(
          store, sites, siteApi, ids, clock, storage, imageSigner, remote, users);
  final com.b2bweb.testkit.url.InMemoryUrlApi urls =
      new com.b2bweb.testkit.url.InMemoryUrlApi(store, sites, siteApi, ids, meta, events::publish);
  final com.b2bweb.testkit.taxonomy.InMemoryTaxonomyApi taxonomy =
      new com.b2bweb.testkit.taxonomy.InMemoryTaxonomyApi(store, sites, content);
  final com.b2bweb.testkit.coordination.InMemoryTransactionMutex mutex =
      new com.b2bweb.testkit.coordination.InMemoryTransactionMutex(
          store, sites, Set.of("content.route_tree"));
  final com.b2bweb.testkit.coordination.FakeReferenceWrites references =
      new com.b2bweb.testkit.coordination.FakeReferenceWrites(media::replace, sites);
  final com.b2bweb.testkit.coordination.InMemoryProjectionChanges projections =
      new com.b2bweb.testkit.coordination.InMemoryProjectionChanges(store, sites, siteApi);
  final FakeCdnPurger cdn = new FakeCdnPurger();
  final FakeGeoIpResolver geo = new FakeGeoIpResolver();
  final FakeMxRecordChecker mx = new FakeMxRecordChecker(clock);
  final FakeIndexNowClient indexNow = new FakeIndexNowClient();
  final FakeWebhookTransport webhooks = new FakeWebhookTransport(clock);
  final InMemorySearchEngine searchEngine = new InMemorySearchEngine();
  final FakeMailTransport mail = new FakeMailTransport(clock);
  final FakeCaptchaVerifier captcha = new FakeCaptchaVerifier();
  final FakeVirusScanner scanner = new FakeVirusScanner(clock);
  final FakeTranslationEngine engine = new FakeTranslationEngine();
  final InMemoryRateLimiter limiter = new InMemoryRateLimiter(sites, clock);
  final InMemoryDistributedLock locks = new InMemoryDistributedLock(clock);

  public com.b2bweb.testkit.media.InMemoryMediaApi mediaApi() {
    return media;
  }

  public com.b2bweb.testkit.url.InMemoryUrlApi urlApi() {
    return urls;
  }

  public com.b2bweb.testkit.taxonomy.InMemoryTaxonomyApi taxonomyApi() {
    return taxonomy;
  }

  public com.b2bweb.testkit.coordination.InMemoryTransactionMutex transactionMutexApi() {
    return mutex;
  }

  public com.b2bweb.testkit.coordination.FakeReferenceWrites referenceWriteApi() {
    return references;
  }

  public com.b2bweb.testkit.coordination.InMemoryProjectionChanges projectionChangeApi() {
    return projections;
  }

  public FakeCdnPurger cdnPurger() {
    return cdn;
  }

  public FakeGeoIpResolver geoIpResolver() {
    return geo;
  }

  public FakeMxRecordChecker mxRecordChecker() {
    return mx;
  }

  public FakeIndexNowClient indexNowClient() {
    return indexNow;
  }

  public FakeWebhookTransport webhookTransport() {
    return webhooks;
  }

  public InMemorySearchEngine searchEngineClient() {
    return searchEngine;
  }

  public FakeImageUrlSigner imageUrlSigner() {
    return imageSigner;
  }

  public FakeRemoteFileFetcher remoteFileFetcher() {
    return remote;
  }

  public FakeMailTransport mailTransport() {
    return mail;
  }

  public FakeCaptchaVerifier captchaVerifier() {
    return captcha;
  }

  public FakeVirusScanner virusScanner() {
    return scanner;
  }

  public FakeTranslationEngine translationEngine() {
    return engine;
  }

  public InMemoryRateLimiter rateLimiter() {
    return limiter;
  }

  public InMemoryDistributedLock distributedLock() {
    return locks;
  }

  public <T> T implementation(Class<T> type) {
    Map<Class<?>, Object> implementations =
        Map.ofEntries(
            Map.entry(com.b2bweb.api.site.SiteApi.class, siteApi),
            Map.entry(com.b2bweb.api.site.SiteContextProvider.class, sites),
            Map.entry(com.b2bweb.api.site.SettingsApi.class, settings),
            Map.entry(com.b2bweb.api.site.SiteProfileApi.class, profiles),
            Map.entry(com.b2bweb.api.site.UiTextApi.class, texts),
            Map.entry(com.b2bweb.api.security.CurrentUserProvider.class, users),
            Map.entry(com.b2bweb.api.security.PermissionChecker.class, permissions),
            Map.entry(com.b2bweb.api.security.UserDirectory.class, directory),
            Map.entry(com.b2bweb.api.security.AuditApi.class, audit),
            Map.entry(com.b2bweb.api.security.SecretCipher.class, cipher),
            Map.entry(com.b2bweb.api.common.IdGenerator.class, ids),
            Map.entry(com.b2bweb.api.common.JobApi.class, jobs),
            Map.entry(com.b2bweb.api.common.DictApi.class, dicts),
            Map.entry(com.b2bweb.api.event.EventMetaFactory.class, meta),
            Map.entry(com.b2bweb.api.extension.ExtensionRegistry.class, extensionRegistry()),
            Map.entry(com.b2bweb.api.content.ContentApi.class, content),
            Map.entry(com.b2bweb.api.url.UrlApi.class, urls),
            Map.entry(com.b2bweb.api.media.MediaApi.class, media),
            Map.entry(com.b2bweb.api.taxonomy.TaxonomyApi.class, taxonomy),
            Map.entry(com.b2bweb.api.form.FormApi.class, forms),
            Map.entry(com.b2bweb.api.notification.NotificationApi.class, notifications),
            Map.entry(com.b2bweb.api.translation.TranslationApi.class, translation),
            Map.entry(com.b2bweb.api.common.ReferenceWriteApi.class, references),
            Map.entry(com.b2bweb.api.common.ProjectionChangeApi.class, projections),
            Map.entry(com.b2bweb.api.common.TransactionMutexApi.class, mutex),
            Map.entry(com.b2bweb.api.port.ObjectStorage.class, storage),
            Map.entry(com.b2bweb.api.port.ImageUrlSigner.class, imageSigner),
            Map.entry(com.b2bweb.api.port.VirusScanner.class, scanner),
            Map.entry(com.b2bweb.api.port.MailTransport.class, mail),
            Map.entry(com.b2bweb.api.port.WebhookTransport.class, webhooks),
            Map.entry(com.b2bweb.api.port.SearchEngineClient.class, searchEngine),
            Map.entry(com.b2bweb.api.port.CdnPurger.class, cdn),
            Map.entry(com.b2bweb.api.port.IndexNowClient.class, indexNow),
            Map.entry(com.b2bweb.api.port.TranslationEngine.class, engine),
            Map.entry(com.b2bweb.api.port.CaptchaVerifier.class, captcha),
            Map.entry(com.b2bweb.api.port.GeoIpResolver.class, geo),
            Map.entry(com.b2bweb.api.port.MxRecordChecker.class, mx),
            Map.entry(com.b2bweb.api.port.RemoteFileFetcher.class, remote),
            Map.entry(com.b2bweb.api.port.TaggedCache.class, cache),
            Map.entry(com.b2bweb.api.port.RateLimiter.class, limiter),
            Map.entry(com.b2bweb.api.port.DistributedLock.class, locks),
            Map.entry(com.b2bweb.api.port.BufferStore.class, buffer));
    Object value = implementations.get(type);
    if (value == null) throw new IllegalArgumentException("测试契约未登记: " + type.getName());
    return type.cast(value);
  }

  public TestPlatform bridge(
      SiteContextProvider sourceSites,
      SiteApi sourceApi,
      CurrentUserProvider sourceUser,
      UserDirectory sourceDirectory,
      Clock sourceClock) {
    directory.delegate(sourceDirectory);
    return bridge(sourceSites, sourceApi, sourceUser, sourceClock);
  }

  public TestPlatform bridge(
      SiteContextProvider sourceSites,
      SiteApi sourceApi,
      CurrentUserProvider sourceUser,
      Clock sourceClock) {
    sites.delegate(sourceSites);
    siteApi.delegate(sourceApi);
    users.delegate(sourceUser);
    clock.follow(sourceClock);
    return this;
  }

  public static TestPlatform empty() {
    return new TestPlatform();
  }

  public static TestPlatform standard() {
    var p = new TestPlatform();
    for (long id : List.of(1L, 2L)) {
      p.siteApi.addSite(
          new SiteInfo(
              id,
              "s" + id,
              "测试站点",
              "industrial",
              "zh-CN",
              "zh-CN",
              UrlStrategy.SUBDIRECTORY,
              TrailingSlash.NEVER,
              ZoneOffset.UTC,
              SiteStatus.ACTIVE,
              "s" + id + ".example.test",
              0));
      p.sites.use(id);
      p.siteApi.setLocales(
          List.of(
              new SiteLocaleInfo("zh-CN", "zh", "zh", true, false, 0, true),
              new SiteLocaleInfo("en", "en", "en", false, true, 1, false)));
    }
    p.siteApi.addModule(
        new ModuleInfo(
            "core", "系统", "L0", "1.0.0", false, ModuleStatus.INSTALLED, "system", "sys_"));
    for (String id : List.of("form", "notification", "translation")) {
      p.siteApi.addModule(
          new ModuleInfo(id, id, "L2", "1.0.0", true, ModuleStatus.INSTALLED, id, null));
      for (long site : List.of(1L, 2L)) {
        p.sites.use(site);
        p.siteApi.setModuleEnabled(id, true);
      }
    }
    p.siteApi.addModule(
        new ModuleInfo(
            "sample", "示例", "L3", "1.0.0", true, ModuleStatus.INSTALLED, "sample", null));
    for (long site : List.of(1L, 2L)) {
      p.sites.use(site);
      p.siteApi.setModuleEnabled("sample", true);
    }
    p.content.addType(
        new com.b2bweb.api.content.ContentTypeDefinition(
            "SAMPLE",
            "sample",
            "示例内容",
            false,
            null,
            List.of(),
            Set.of(),
            false,
            true,
            true,
            true,
            true,
            "/sample/edit"));
    p.sites.clear();
    return p;
  }

  public TestPlatform useSite(long id) {
    sites.use(id);
    return this;
  }

  public TestPlatform withoutSite() {
    sites.clear();
    return this;
  }

  public TestPlatform useUser(CurrentUser user) {
    users.use(user);
    return this;
  }

  public FakeSiteContextProvider siteContextProvider() {
    return sites;
  }

  public InMemorySiteApi siteApi() {
    return siteApi;
  }

  public InMemoryUiTextApi uiTextApi() {
    return texts;
  }

  public FakeCurrentUserProvider currentUserProvider() {
    return users;
  }

  public FakePermissionChecker permissionChecker() {
    return permissions;
  }

  public com.b2bweb.testkit.content.InMemoryContentApi contentApi() {
    return content;
  }

  public com.b2bweb.api.event.EventMetaFactory eventMetaFactory() {
    return meta;
  }

  public InMemoryFormApi formApi() {
    return forms;
  }

  public InMemoryNotificationApi notificationApi() {
    return notifications;
  }

  public InMemoryTranslationApi translationApi() {
    return translation;
  }

  public InMemoryTaggedCache taggedCache() {
    return cache;
  }

  public InMemoryBufferStore bufferStore() {
    return buffer;
  }

  public ManualClock clock() {
    return clock;
  }

  public FakeIdGenerator ids() {
    return ids;
  }

  public RecordedEvents events() {
    return events;
  }
}
