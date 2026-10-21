package com.b2bweb.testkit.example;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.seo.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.testkit.TestPlatform;
import com.b2bweb.testkit.common.MemoryStore;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Executable examples with explicit fixture state. These are never component-scanned in production.
 */
public final class SampleExtensions {
  private final TestPlatform platform;
  private final MemoryStore store = new MemoryStore();
  private final JsonMapper json = JsonMapper.builder().build();

  public SampleExtensions(TestPlatform platform) {
    this.platform = platform;
  }

  public void seed(long site, long localization, String locale, JsonNode value) {
    put("content:" + site + ":" + localization + ":" + locale, value);
  }

  public void resource(long site, String id, String locale, JsonNode value) {
    put("resource:" + site + ":" + id + ":" + locale, value);
  }

  public JsonNode resourceValue(long site,String id,String locale) { return get("resource:"+site+":"+id+":"+locale); }

  public void personal(long site, String email, JsonNode value) {
    put("personal:" + site + ":" + email.toLowerCase(Locale.ROOT), value);
  }

  private void put(String key, JsonNode value) {
    store.atomic(
        rows -> {
          rows.put(key, value.deepCopy());
          return null;
        });
  }

  private JsonNode get(String key) {
    return store.atomic(
        rows -> {
          var value = (JsonNode) rows.get(key);
          return value == null ? json.createObjectNode() : value.deepCopy();
        });
  }

  private JsonNode draft(long site, long localization, String locale) {
    return get("content:" + site + ":" + localization + ":" + locale);
  }

  public JsonNode translated(long site, long localization, String locale) {
    return draft(site, localization, locale);
  }

  public List<Object> all() {
    return List.of(
        new ContentTypes(),
        new Publishing(),
        new Impacts(),
        new Templates(),
        new DataSource(),
        new Delivery(),
        new Bootstrap(),
        new Breadcrumbs(),
        new SchemaOrg(),
        new SeoVariables(),
        new TitleRule(),
        new Sitemap(),
        new SearchDocuments(),
        new Relations(),
        new MediaReferences(),
        new Variants(),
        new PersonalData(),
        new Notifications(),
        new ContentTranslation(),
        new ResourceTranslation(),
        new Lifecycle(),
        new EchoJob(),
        new RefreshTask(),
        new ReferenceSink(),
        new Capture());
  }

  private <T> CursorPage<T> page(List<T> items, CursorRequest cursor, String scope) {
    int offset = 0;
    if (cursor.cursor() != null) {
      String token =
          new String(Base64.getUrlDecoder().decode(cursor.cursor()), StandardCharsets.UTF_8);
      if (!token.startsWith(scope + ":")) throw new IllegalArgumentException("游标范围不匹配");
      offset = Integer.parseInt(token.substring(scope.length() + 1));
    }
    if (offset < 0 || offset > items.size()) throw new IllegalArgumentException("游标无效");
    int next = Math.min(items.size(), offset + cursor.size());
    return new CursorPage<>(
        items.subList(offset, next),
        next < items.size()
            ? Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((scope + ":" + next).getBytes(StandardCharsets.UTF_8))
            : null);
  }

  @Extension(module = "sample")
  public final class ContentTypes implements ContentTypeProvider {
    public List<ContentTypeDefinition> contentTypes() {
      return List.of(
          new ContentTypeDefinition(
              "SAMPLE",
              "sample",
              "示例内容",
              false,
              null,
              List.of("sample.detail"),
              Set.of(),
              false,
              true,
              true,
              true,
              true,
              "/sample/edit"));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class Publishing implements PublishContributor {
    public String partKey() {
      return "sample";
    }

    public void validate(PublishContext ctx, ValidationResult result) {
      if (draft(ctx.siteId(), ctx.localizationId(), ctx.locale())
          .path("title")
          .asString("")
          .isBlank()) result.error("SAMPLE_TITLE_REQUIRED", "title", "请输入标题");
    }

    public JsonNode contribute(PublishContext ctx) {
      return ctx.overridePart(partKey())
          .orElseGet(() -> draft(ctx.siteId(), ctx.localizationId(), ctx.locale()))
          .deepCopy();
    }

    public void restore(PublishContext ctx, JsonNode part) {
      seed(ctx.siteId(), ctx.localizationId(), ctx.locale(), part);
    }
  }

  @Extension(module = "sample")
  public final class Impacts implements ContentImpactProvider {
    public ImpactReport analyze(ContentImpactContext ctx) {
      long count =
          store.atomic(
              rows ->
                  rows.keySet().stream()
                      .filter(
                          k ->
                              k.startsWith(
                                  "reference:" + ctx.siteId() + ":" + ctx.contentId() + ":"))
                      .count());
      return count == 0
          ? ImpactReport.none()
          : new ImpactReport(
              List.of(
                  new ImpactItem(
                      "sample",
                      Severity.WARNING,
                      "SAMPLE_REFERENCED",
                      "仍有引用",
                      Math.toIntExact(count),
                      List.of(ctx.contentId()),
                      "/sample/references")));
    }
  }

  @Extension(module = "sample")
  public final class Templates implements TemplateProvider {
    public List<TemplateDefinition> templates() {
      return List.of(
          new TemplateDefinition("sample.detail", 1, Set.of("SAMPLE"), "示例详情", List.of(), false));
    }
  }

  @Extension(module = "sample")
  public final class DataSource implements DataSourceProvider {
    public String key() {
      return "sample.items";
    }

    public String label() {
      return "示例列表";
    }

    public JsonNode paramsSchema() {
      return json.readTree(
          "{\"type\":\"object\",\"properties\":{\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}},\"additionalProperties\":false}");
    }

    public void validateParams(JsonNode params, ValidationResult result) {
      if (params.has("limit")
          && (params.path("limit").asInt() < 1 || params.path("limit").asInt() > 100))
        result.error("SAMPLE_LIMIT_INVALID", "limit", "数量必须在1至100之间");
    }

    public DataSourceResult resolve(DataSourceRequest request) {
      var items =
          store.atomic(
              rows ->
                  rows.entrySet().stream()
                      .filter(
                          e ->
                              e.getKey().startsWith("resource:" + request.siteId() + ":")
                                  && e.getKey().endsWith(":" + request.locale()))
                      .sorted(Map.Entry.comparingByKey())
                      .limit(request.limit())
                      .map(e -> ((JsonNode) e.getValue()).deepCopy())
                      .toList());
      return new DataSourceResult(
          json.valueToTree(items), items.size(), Set.of(CacheTag.site(request.siteId())));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class Delivery implements DeliveryContributor {
    public String sectionKey() {
      return "sample";
    }

    public void contribute(DeliveryContext ctx, DeliveryOutput out) {
      out.put(sectionKey(), ctx.snapshot().path("sample").deepCopy())
          .addCacheTag(CacheTag.content(ctx.siteId(), ctx.contentId()));
    }
  }

  @Extension(module = "sample")
  public final class Bootstrap implements SiteBootstrapContributor {
    public String sectionKey() {
      return "sample";
    }

    public void contribute(SiteBootstrapContext ctx, DeliveryOutput out) {
      out.put(
              sectionKey(),
              json.createObjectNode().put("sourceLocale", "zh-CN").put("locale", ctx.locale()))
          .addCacheTag(CacheTag.settings(ctx.siteId()));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class Breadcrumbs implements BreadcrumbProvider {
    public List<BreadcrumbItem> trail(BreadcrumbContext ctx) {
      String title = ctx.snapshot().path("sample").path("title").asString("");
      return title.isBlank()
          ? List.of()
          : List.of(new BreadcrumbItem(title, null, ctx.contentId(), null));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class SchemaOrg implements SchemaOrgProvider {
    public List<JsonNode> nodes(SchemaOrgContext ctx) {
      return List.of(
          json.createObjectNode()
              .put("@type", "CreativeWork")
              .put("@id", ctx.nodeId("sample"))
              .put("url", ctx.pageUrl())
              .put("isPartOf", ctx.websiteId()));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class SeoVariables implements SeoVariableProvider {
    public List<SeoVariableDefinition> variables() {
      return List.of(new SeoVariableDefinition("sample.title", "示例标题", "精密设备"));
    }

    public Map<String, String> resolve(PublishContext ctx) {
      return Map.of(
          "sample.title",
          draft(ctx.siteId(), ctx.localizationId(), ctx.locale()).path("title").asString(""));
    }
  }

  @Extension(module = "sample")
  public final class TitleRule implements SeoRule {
    public String id() {
      return "sample.title";
    }

    public Severity severity() {
      return Severity.WARNING;
    }

    public Set<String> contentTypes() {
      return Set.of("SAMPLE");
    }

    public List<SeoIssue> check(SeoAuditContext ctx) {
      return ctx.snapshot().path("sample").path("title").asString("").isBlank()
          ? List.of(new SeoIssue(id(), severity(), "SAMPLE_TITLE_EMPTY", "页面标题为空", "sample.title"))
          : List.of();
    }
  }

  @Extension(module = "sample")
  public final class Sitemap implements SitemapProvider {
    public String key() {
      return "sample.sitemap";
    }

    public CursorPage<SitemapEntry> entries(SitemapContext ctx, CursorRequest cursor) {
      List<SitemapEntry> items =
          store.atomic(
              rows ->
                  rows.entrySet().stream()
                      .filter(
                          e ->
                              e.getKey().startsWith("resource:" + ctx.siteId() + ":")
                                  && e.getKey().endsWith(":" + ctx.locale()))
                      .sorted(Map.Entry.comparingByKey())
                      .map(e -> (JsonNode) e.getValue())
                      .filter(n -> n.path("path").isString())
                      .map(
                          n ->
                              new SitemapEntry(
                                  n.path("path").asString(), null, Map.of(), List.of()))
                      .toList());
      return page(items, cursor, "sitemap:" + ctx.siteId() + ":" + ctx.locale());
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class SearchDocuments implements SearchDocumentProvider {
    public Optional<SearchDocumentFields> build(SearchDocumentContext ctx) {
      var sample = ctx.snapshot().path("sample");
      if (sample.isMissingNode()) return Optional.empty();
      return Optional.of(
          new SearchDocumentFields(
              sample.path("title").asString(""),
              null,
              sample.path("body").asString(""),
              Set.of(),
              json.createObjectNode(),
              List.of(),
              null,
              0));
    }
  }

  @Extension(module = "sample")
  public final class Relations implements RelationTypeProvider {
    public List<RelationTypeDefinition> relationTypes() {
      return List.of(
          new RelationTypeDefinition(
              "sample.similar", "相似内容", true, null, Set.of("SAMPLE"), Set.of("SAMPLE"), true));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class MediaReferences implements MediaReferenceCollector {
    public List<MediaReference> collectDraft(MediaReferenceContext ctx) {
      var references = ReferenceSet.scan(draft(ctx.siteId(), ctx.localizationId(), ctx.locale()));
      return references.mediaIds().stream()
          .sorted()
          .map(id -> new MediaReference(id, "sample", "image"))
          .toList();
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class Variants implements ContentVariantProvider {
    public Map<Long, List<ContentVariant>> variants(Collection<Long> ids, String locale) {
      Map<Long, List<ContentVariant>> result = new LinkedHashMap<>();
      for (long id : ids) {
        var data =
            get(
                "variant:"
                    + platform.siteContextProvider().requireSiteId()
                    + ":"
                    + id
                    + ":"
                    + locale);
        if (data.path("code").isString())
          result.put(
              id,
              List.of(
                  new ContentVariant(
                      id, id, data.path("code").asString(), data.path("label").asString(null), 0)));
      }
      return Map.copyOf(result);
    }
  }

  @Extension(module = "sample")
  public final class PersonalData implements PersonalDataProvider {
    public String key() {
      return "sample.personal";
    }

    public String label() {
      return "示例联系人";
    }

    private String key(PersonalDataRequest r) {
      return "personal:" + r.siteId() + ":" + r.normalizedEmail();
    }

    public PersonalDataSummary find(PersonalDataRequest r) {
      boolean present = store.atomic(rows -> rows.containsKey(key(r)));
      return new PersonalDataSummary(
          key(),
          r.siteId(),
          present ? 1 : 0,
          present ? List.of("sample.contact") : List.of(),
          null,
          null);
    }

    public JsonNode export(PersonalDataRequest r) {
      return get(key(r));
    }

    public int rectify(PersonalDataRequest r, PersonalDataCorrection correction) {
      return store.atomic(
          rows -> {
            var old = (JsonNode) rows.get(key(r));
            if (old == null) return 0;
            var value = (ObjectNode) old.deepCopy();
            if (correction.name() != null) value.put("name", correction.name());
            if (correction.phone() != null) value.put("phone", correction.phone());
            if (correction.company() != null) value.put("company", correction.company());
            String target =
                correction.email() == null
                    ? key(r)
                    : "personal:"
                        + r.siteId()
                        + ":"
                        + correction.email().strip().toLowerCase(Locale.ROOT);
            rows.remove(key(r));
            rows.put(target, value);
            return 1;
          });
    }

    public int erase(PersonalDataRequest r) {
      return store.atomic(rows -> rows.remove(key(r)) == null ? 0 : 1);
    }

    public int objectToMarketing(PersonalDataRequest r) {
      return flag(r, "marketing", false);
    }

    public int restrict(PersonalDataRequest r, boolean restricted) {
      return flag(r, "restricted", restricted);
    }

    private int flag(PersonalDataRequest r, String name, boolean value) {
      return store.atomic(
          rows -> {
            var existing = (JsonNode) rows.get(key(r));
            if (existing == null) return 0;
            var changed = (ObjectNode) existing.deepCopy();
            changed.put(name, value);
            rows.put(key(r), changed);
            return 1;
          });
    }
  }

  @Extension(module = "sample")
  public final class Notifications implements NotificationTemplateProvider {
    public List<NotificationTemplateDefinition> templates() {
      return List.of(
          new NotificationTemplateDefinition(
              "sample.received",
              "收到消息",
              NotificationAudience.CUSTOMER,
              NotificationChannel.EMAIL,
              "已收到您的消息",
              "我们会尽快回复。",
              List.of(),
              1));
    }
  }

  @Extension(module = "sample", contentTypes = "SAMPLE")
  public final class ContentTranslation implements TranslatableContentProvider {
    public List<TranslationUnit> extract(TranslationContext ctx) {
      var value = draft(ctx.siteId(), ctx.content().sourceLocalizationId(), ctx.sourceLocale());
      return value.path("title").isString()
          ? List.of(TranslationUnit.text("sample.title", value.path("title").asString(), "示例标题"))
          : List.of();
    }

    public void apply(TranslationContext ctx, Map<String, String> translations) {
      var value =
          (ObjectNode)
              draft(ctx.siteId(), ctx.content().targetLocalizationId(), ctx.targetLocale());
      if (translations.containsKey("sample.title"))
        value.put("title", translations.get("sample.title"));
      seed(ctx.siteId(), ctx.content().targetLocalizationId(), ctx.targetLocale(), value);
    }
  }

  @Extension(module = "sample")
  public final class ResourceTranslation implements TranslatableResourceProvider {
    public Set<String> resourceTypes() {
      return Set.of("sample.label");
    }

    public List<TranslationUnit> extract(TranslationContext ctx) {
      var value =
          get(
              "resource:"
                  + ctx.siteId()
                  + ":"
                  + ctx.resource().resourceId()
                  + ":"
                  + ctx.sourceLocale());
      return value.path("title").isString()
          ? List.of(TranslationUnit.text("sample.title", value.path("title").asString(), "示例文案"))
          : List.of();
    }

    public void apply(TranslationContext ctx, Map<String, String> translations) {
      var value =
          (ObjectNode)
              get(
                  "resource:"
                      + ctx.siteId()
                      + ":"
                      + ctx.resource().resourceId()
                      + ":"
                      + ctx.targetLocale());
      if (translations.containsKey("sample.title"))
        value.put("title", translations.get("sample.title"));
      resource(ctx.siteId(), ctx.resource().resourceId(), ctx.targetLocale(), value);
    }

    public CursorPage<String> resourceIds(String type, CursorRequest cursor) {
      if (!supports(type)) throw new IllegalArgumentException("资源类型不支持");
      long site = platform.siteContextProvider().requireSiteId();
      List<String> values =
          store.atomic(
              rows ->
                  rows.keySet().stream()
                      .filter(k -> k.startsWith("resource:" + site + ":") && k.endsWith(":zh-CN"))
                      .map(k -> k.substring(("resource:" + site + ":").length(), k.length() - 6))
                      .sorted()
                      .toList());
      return page(values, cursor, "resources:" + site + ":" + type);
    }
  }

  @Extension(module = "sample")
  public final class Lifecycle implements PlatformModule {
    public ModuleDescriptor descriptor() {
      return new ModuleDescriptor(
          "sample",
          "示例模块",
          SemVer.parse("1.0.0"),
          "L3",
          ">=1.0.0 <2.0.0",
          Map.of(),
          Map.of(),
          true,
          null,
          "sample",
          List.of(),
          Set.of(),
          Set.of(),
          json.createObjectNode().put("id", "sample"));
    }

    public void onInstall(ModuleContext ctx) {
      put("lifecycle:installed", json.createObjectNode().put("version", ctx.version().toString()));
    }

    public void onStartup(ModuleContext ctx) {
      store.atomic(
          rows -> {
            rows.put(
                "lifecycle:starts",
                ((Number) rows.getOrDefault("lifecycle:starts", 0)).intValue() + 1);
            return null;
          });
    }

    public void onEnable(SiteModuleContext ctx) {
      put("lifecycle:site:" + ctx.siteId(), json.createObjectNode().put("enabled", true));
    }

    public void onDisable(SiteModuleContext ctx) {
      put("lifecycle:site:" + ctx.siteId(), json.createObjectNode().put("enabled", false));
    }
  }

  @Extension(module = "sample")
  public final class EchoJob implements JobHandler {
    public String jobType() {
      return "sample.echo";
    }

    public JobOutcome execute(JobExecutionContext ctx) {
      ctx.checkpoint();
      ctx.progress(1, 1, 0, "已完成");
      return new JobOutcome(ctx.params().deepCopy());
    }
  }

  @Extension(module = "sample")
  public final class RefreshTask implements ScheduledTask {
    public String name() {
      return "sample.refresh";
    }

    public String cron() {
      return "0 * * * * *";
    }

    public ScheduleScope scope() {
      return ScheduleScope.PER_SITE;
    }

    public void run(ScheduledTaskContext ctx) {
      put(
          "schedule:" + ctx.siteId(),
          json.createObjectNode().put("lastRun", ctx.startedAt().toString()));
    }
  }

  @Extension(module = "sample")
  public final class ReferenceSink implements MediaReferenceSink {
    public void replace(MediaReferenceOwner owner, List<MediaReference> references) {
      platform.mediaApi().replace(owner, references);
    }
  }

  @Extension(module = "sample")
  public final class Capture implements SearchChangeCapture {
    public void append(Set<String> locales, Set<Long> ids, boolean full) {
      long site = platform.siteContextProvider().requireSiteId();
      put("capture:" + site, json.createObjectNode().set("locales", json.valueToTree(locales)));
    }
  }
}
