package com.b2bweb.testkit.content;

import static com.b2bweb.testkit.common.Records.copy;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.testkit.common.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serialized aggregate model. It is test data, never a persistence or production content service.
 */
public final class InMemoryContentApi implements ContentApi {
  final MemoryStore store;
  final SiteContextProvider sites;
  final SiteApi siteApi;
  final IdGenerator ids;
  final Clock clock;
  final EventMetaFactory meta;
  final Consumer<DomainEvent> events;
  final Map<String, ContentTypeDefinition> types = new HashMap<>();
  final Set<String> reviewTypes = new HashSet<>();
  final JsonMapper json = JsonMapper.builder().build();
  Function<Set<String>, JobInfo> routeJobs;

  public InMemoryContentApi(
      MemoryStore store,
      SiteContextProvider sites,
      SiteApi siteApi,
      IdGenerator ids,
      Clock clock,
      EventMetaFactory meta,
      Consumer<DomainEvent> events) {
    this.store = store;
    this.sites = sites;
    this.siteApi = siteApi;
    this.ids = ids;
    this.clock = clock;
    this.meta = meta;
    this.events = events;
  }

  String prefix() {
    return sites.requireSiteId() + ":content:";
  }

  public void addType(ContentTypeDefinition type) {
    types.put(type.type(), type);
  }

  public void reviewRequired(String type, boolean required) {
    if (required) reviewTypes.add(type);
    else reviewTypes.remove(type);
  }

  public void routeJobs(Function<Set<String>, JobInfo> submit) {
    routeJobs = submit;
  }

  ContentInfo content(Map<String, Object> rows, long id) {
    var c = (ContentInfo) rows.get(prefix() + "info:" + id);
    if (c == null) throw new NotFoundException(ContentErrors.NOT_FOUND, "内容不存在");
    return c;
  }

  LocalizationInfo loc(Map<String, Object> rows, long id) {
    var l = (LocalizationInfo) rows.get(prefix() + "locale:" + id);
    if (l == null) throw new NotFoundException(ContentErrors.LOCALIZATION_NOT_FOUND, "本地化不存在");
    content(rows, l.contentId());
    return l;
  }

  LocalizationInfo save(Map<String, Object> rows, LocalizationInfo value) {
    rows.put(prefix() + "locale:" + value.localizationId(), value);
    return value;
  }

  void version(LocalizationInfo value, Integer expected) {
    if (expected != null && expected != value.version())
      throw new VersionConflictException(
          ContentErrors.VERSION_CONFLICT, "草稿版本已变化", value.version());
  }

  boolean visible(ContentInfo c) {
    return !c.trashed() && siteApi.isModuleEnabled(c.ownerModule());
  }

  List<LocalizationInfo> locales(Map<String, Object> rows, long cid) {
    String prefix = prefix() + "locale:";
    return rows.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .map(e -> (LocalizationInfo) e.getValue())
        .filter(l -> l.contentId() == cid)
        .sorted(Comparator.comparingLong(LocalizationInfo::localizationId))
        .toList();
  }

  public LocalizationInfo lockDraftForWrite(long id, int expected) {
    return store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          return l;
        });
  }

  public ContentCreated create(CreateContentCommand command) {
    return store.atomic(
        rows -> {
          var type = contentType(command.contentType()).orElse(null);
          if (type == null) throw new BusinessException(ContentErrors.TYPE_UNKNOWN, "内容类型未注册");
          if (!command.locale().equals("zh-CN"))
            throw new BusinessException(ContentErrors.LOCALE_NOT_SOURCE, "新内容必须从源语言创建");
          long id = ids.nextId();
          var c =
              new ContentInfo(
                  id,
                  type.type(),
                  type.module(),
                  type.routable(),
                  "zh-CN",
                  null,
                  meta.next().actorRef().userId(),
                  clock.instant(),
                  0,
                  false);
          rows.put(prefix() + "info:" + id, c);
          var l =
              createLocalization(
                  new CreateLocalizationCommand(
                      id, command.locale(), command.title(), command.slug()));
          events.accept(
              new ContentCreatedEvent(
                  meta.next(), id, type.type(), type.module(), l.localizationId()));
          return new ContentCreated(id, l.localizationId(), l.locale(), l.slug(), l.version());
        });
  }

  public LocalizationInfo createLocalization(CreateLocalizationCommand command) {
    return createLocalization(command, TranslationMode.MANUAL);
  }

  LocalizationInfo createLocalization(
      CreateLocalizationCommand command, TranslationMode targetMode) {
    return store.atomic(
        rows -> {
          var c = content(rows, command.contentId());
          if (locales(rows, c.contentId()).stream()
              .anyMatch(l -> l.locale().equals(command.locale())))
            throw new BusinessException(ContentErrors.LOCALE_EXISTS, "本地化已存在");
          if (siteApi.locale(command.locale()).isEmpty())
            throw new BusinessException(CoreErrors.LOCALE_NOT_ENABLED, "语言未配置");
          boolean source = command.locale().equals(c.primaryLocale());
          var l =
              new LocalizationInfo(
                  ids.nextId(),
                  c.contentId(),
                  c.contentType(),
                  command.locale(),
                  command.slug(),
                  command.title(),
                  DraftState.EDITING,
                  PublishState.NEVER,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  UnpublishStrategy.GONE,
                  null,
                  source ? TranslationMode.SOURCE : targetMode,
                  TranslationState.NONE,
                  source ? null : "zh-CN",
                  null,
                  null,
                  true,
                  0,
                  clock.instant(),
                  c.translationReviewRequired());
          save(rows, l);
          events.accept(
              new LocalizationCreatedEvent(
                  meta.next(),
                  c.contentId(),
                  c.contentType(),
                  l.localizationId(),
                  l.locale(),
                  l.translationMode()));
          return l;
        });
  }

  public DraftMetaUpdated updateDraftMeta(UpdateDraftMetaCommand command) {
    return store.atomic(
        rows -> {
          var l = loc(rows, command.localizationId());
          version(l, command.expectedVersion());
          if (l.translationMode() == TranslationMode.AUTO && command.expectedVersion() == null)
            throw new BusinessException(ContentErrors.STATE_INVALID, "译文写入必须指定版本");
          l =
              copy(
                  l,
                  "draftTitle",
                  command.title() == null ? l.draftTitle() : command.title(),
                  "slug",
                  command.slug() == null ? l.slug() : command.slug(),
                  "draftState",
                  DraftState.EDITING,
                  "hasUnpublishedChanges",
                  true,
                  "version",
                  l.version() + 1,
                  "updatedAt",
                  clock.instant());
          save(rows, l);
          if (command.sharedDataChanged())
            for (var other : locales(rows, l.contentId()))
              if (other.localizationId() != l.localizationId()) {
                var changed =
                    copy(
                        other,
                        "hasUnpublishedChanges",
                        true,
                        "draftState",
                        DraftState.EDITING,
                        "version",
                        other.version() + 1,
                        "updatedAt",
                        clock.instant());
                save(rows, changed);
                events.accept(
                    new ContentDraftChangedEvent(
                        meta.next(),
                        changed.contentId(),
                        changed.contentType(),
                        changed.localizationId(),
                        changed.locale(),
                        changed.version(),
                        true));
              }
          events.accept(
              new ContentDraftChangedEvent(
                  meta.next(),
                  l.contentId(),
                  l.contentType(),
                  l.localizationId(),
                  l.locale(),
                  l.version(),
                  command.sharedDataChanged()));
          return new DraftMetaUpdated(
              l.localizationId(), l.version(), l.draftState(), l.slug(), true);
        });
  }

  public void markDraftChanged(long id) {
    store.atomic(
        rows -> {
          content(rows, id);
          for (var l : locales(rows, id)) {
            save(
                rows,
                copy(
                    l,
                    "hasUnpublishedChanges",
                    true,
                    "draftState",
                    DraftState.EDITING,
                    "version",
                    l.version() + 1,
                    "updatedAt",
                    clock.instant()));
            events.accept(
                new ContentDraftChangedEvent(
                    meta.next(),
                    l.contentId(),
                    l.contentType(),
                    l.localizationId(),
                    l.locale(),
                    l.version() + 1,
                    true));
          }
          return null;
        });
  }

  public void submitForReview(long id, int expected, String comment) {
    store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          if (!reviewTypes.contains(l.contentType())
              && !content(rows, l.contentId()).translationReviewRequired())
            throw new BusinessException(ContentErrors.REVIEW_NOT_ENABLED, "未开启审核");
          if (l.draftState() != DraftState.EDITING)
            throw new BusinessException(ContentErrors.STATE_INVALID, "当前状态不能提交审核");
          save(rows, copy(l, "draftState", DraftState.IN_REVIEW, "version", l.version() + 1));
          events.accept(
              new ReviewRequestedEvent(
                  meta.next(),
                  l.contentId(),
                  l.contentType(),
                  id,
                  l.locale(),
                  l.draftTitle(),
                  l.version() + 1,
                  meta.next().actorRef().userId(),
                  comment));
          return null;
        });
  }

  public void approve(long id, int expected, String comment) {
    review(id, expected, comment, true);
  }

  public void reject(long id, int expected, String comment) {
    review(id, expected, comment, false);
  }

  void review(long id, int expected, String comment, boolean approved) {
    store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          if (l.draftState() != DraftState.IN_REVIEW)
            throw new BusinessException(ContentErrors.STATE_INVALID, "当前没有待处理审核");
          save(
              rows,
              copy(
                  l,
                  "draftState",
                  approved ? DraftState.APPROVED : DraftState.EDITING,
                  "version",
                  l.version() + 1));
          events.accept(
              new ReviewCompletedEvent(
                  meta.next(),
                  l.contentId(),
                  l.contentType(),
                  id,
                  l.locale(),
                  l.draftTitle(),
                  approved ? ReviewDecision.APPROVED : ReviewDecision.REJECTED,
                  0,
                  meta.next().actorRef().userId(),
                  comment));
          return null;
        });
  }

  public void putDraftPart(long id, String key, JsonNode value) {
    store.atomic(
        rows -> {
          loc(rows, id);
          rows.put(prefix() + "part:" + id + ":" + key, value.deepCopy());
          return null;
        });
  }

  JsonNode payload(Map<String, Object> rows, LocalizationInfo l) {
    var value = json.createObjectNode();
    value
        .putObject("content")
        .put("id", Long.toString(l.contentId()))
        .put("type", l.contentType())
        .put("locale", l.locale());
    value.putObject("card").put("title", l.draftTitle());
    var parts = value.putObject("parts");
    String prefix = prefix() + "part:" + l.localizationId() + ":";
    rows.forEach(
        (k, v) -> {
          if (k.startsWith(prefix))
            parts.set(k.substring(prefix.length()), ((JsonNode) v).deepCopy());
        });
    return value;
  }

  public PublishResult publish(PublishCommand command) {
    return store.atomic(
        rows -> {
          var l = loc(rows, command.localizationId());
          var c = content(rows, l.contentId());
          version(l, command.expectedVersion());
          if (!visible(c)) throw new BusinessException(ContentErrors.STATE_INVALID, "内容已回收或模块停用");
          if ((c.translationReviewRequired() || reviewTypes.contains(c.contentType()))
              && l.draftState() != DraftState.APPROVED)
            throw new BusinessException(ContentErrors.REVIEW_REQUIRED, "需要当前草稿审核");
          if (l.draftTitle().isBlank())
            throw new BusinessException(ContentErrors.STATE_INVALID, "标题不能为空");
          var data = payload(rows, l);
          String hash = sha(data.toString());
          var previous =
              l.publishedRevisionNo() == null
                  ? Optional.<PublishedSnapshot>empty()
                  : revision(rows, l.localizationId(), l.publishedRevisionNo());
          boolean changed = previous.map(s -> !s.payloadHash().equals(hash)).orElse(true);
          if (!changed && l.isPublished())
            return new PublishResult(
                l.localizationId(),
                previous.orElseThrow().revisionId(),
                previous.orElseThrow().revisionNo(),
                l.pubPath(),
                l.pubPath(),
                false,
                false,
                List.of());
          int revision = l.publishedRevisionNo() == null ? 1 : l.publishedRevisionNo() + 1;
          long rid = ids.nextId();
          String path =
              c.routable()
                  ? "/"
                      + siteApi.locale(l.locale()).orElseThrow().urlPrefix()
                      + "/"
                      + Objects.requireNonNullElse(l.slug(), Long.toString(c.contentId()))
                  : null;
          var snapshot =
              new PublishedSnapshot(
                  c.contentId(),
                  l.localizationId(),
                  l.contentType(),
                  l.locale(),
                  rid,
                  revision,
                  data,
                  hash,
                  path,
                  clock.instant(),
                  clock.instant(),
                  meta.next().actorRef().userId(),
                  PublishTrigger.MANUAL);
          rows.put(prefix() + "revision:" + l.localizationId() + ":" + revision, snapshot);
          rows.put(
              prefix() + "published-terms:" + l.localizationId(),
              rows.getOrDefault(prefix() + "draft-terms:" + l.localizationId(), List.of()));
          save(
              rows,
              copy(
                  l,
                  "draftState",
                  DraftState.EDITING,
                  "publishState",
                  PublishState.PUBLISHED,
                  "publishedRevisionId",
                  rid,
                  "publishedRevisionNo",
                  revision,
                  "firstPublishedAt",
                  l.firstPublishedAt() == null ? clock.instant() : l.firstPublishedAt(),
                  "publishedAt",
                  clock.instant(),
                  "pubPath",
                  path,
                  "hasUnpublishedChanges",
                  false,
                  "version",
                  l.version() + 1));
          events.accept(
              new ContentPublishedEvent(
                  meta.next(),
                  c.contentId(),
                  c.contentType(),
                  l.locale(),
                  l.localizationId(),
                  rid,
                  revision,
                  path,
                  l.pubPath(),
                  l.publishedRevisionNo() == null,
                  changed,
                  PublishTrigger.MANUAL));
          return new PublishResult(
              l.localizationId(),
              rid,
              revision,
              path,
              l.pubPath(),
              l.publishedRevisionNo() == null,
              changed,
              List.of());
        });
  }

  static String sha(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  Optional<PublishedSnapshot> revision(Map<String, Object> rows, long id, int revision) {
    return Optional.ofNullable(
        (PublishedSnapshot) rows.get(prefix() + "revision:" + id + ":" + revision));
  }

  public LocalizationInfo schedule(ScheduleCommand command) {
    return store.atomic(
        rows -> {
          var l = loc(rows, command.localizationId());
          version(l, command.expectedVersion());
          if (command.publishAt() != null && !command.publishAt().isAfter(clock.instant()))
            throw new BusinessException(ContentErrors.STATE_INVALID, "定时时间必须在未来");
          return save(
              rows,
              copy(
                  l,
                  "scheduledPublishAt",
                  command.publishAt(),
                  "scheduledUnpublishAt",
                  command.unpublishAt(),
                  "unpublishStrategy",
                  command.unpublishStrategy() == null
                      ? l.unpublishStrategy()
                      : command.unpublishStrategy(),
                  "unpublishRedirectContentId",
                  command.unpublishRedirectContentId(),
                  "publishState",
                  l.isPublished() ? PublishState.PUBLISHED : PublishState.SCHEDULED,
                  "version",
                  l.version() + 1));
        });
  }

  public LocalizationInfo cancelSchedule(long id, int expected) {
    return store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          return save(
              rows,
              copy(
                  l,
                  "scheduledPublishAt",
                  null,
                  "scheduledUnpublishAt",
                  null,
                  "publishState",
                  l.isPublished() ? PublishState.PUBLISHED : PublishState.NEVER,
                  "version",
                  l.version() + 1));
        });
  }

  public void unpublish(UnpublishCommand command) {
    store.atomic(
        rows -> {
          var l = loc(rows, command.localizationId());
          version(l, command.expectedVersion());
          if (!l.isPublished()) return null;
          save(
              rows,
              copy(
                  l,
                  "publishState",
                  PublishState.UNPUBLISHED,
                  "version",
                  l.version() + 1,
                  "pubPath",
                  null));
          events.accept(
              new ContentUnpublishedEvent(
                  meta.next(),
                  l.contentId(),
                  l.contentType(),
                  l.locale(),
                  l.localizationId(),
                  l.publishedRevisionNo(),
                  l.pubPath(),
                  command.strategy(),
                  command.redirectContentId(),
                  false));
          return null;
        });
  }

  public int unpublishAll(long cid, UnpublishStrategy strategy, Long redirect) {
    return store.atomic(
        rows -> {
          int count = 0;
          for (var l : locales(rows, cid))
            if (l.isPublished()) {
              unpublish(new UnpublishCommand(l.localizationId(), l.version(), strategy, redirect));
              count++;
            }
          return count;
        });
  }

  public void rollback(long id, int revision, int expected) {
    store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          var snapshot =
              revision(rows, id, revision)
                  .orElseThrow(
                      () -> new NotFoundException(ContentErrors.REVISION_NOT_FOUND, "修订不存在"));
          for (var part : snapshot.payload().path("parts").properties())
            rows.put(prefix() + "part:" + id + ":" + part.getKey(), part.getValue().deepCopy());
          save(
              rows,
              copy(
                  l,
                  "draftTitle",
                  snapshot.payload().path("card").path("title").asString(),
                  "draftState",
                  DraftState.EDITING,
                  "hasUnpublishedChanges",
                  true,
                  "version",
                  l.version() + 1));
          return null;
        });
  }

  public void trash(long cid) {
    store.atomic(
        rows -> {
          var c = content(rows, cid);
          if (c.trashed()) return null;
          if (locales(rows, cid).stream().anyMatch(LocalizationInfo::isPublished))
            throw new BusinessException(ContentErrors.HAS_PUBLISHED_LOCALIZATION, "请先下线已发布版本");
          rows.put(
              prefix() + "info:" + cid,
              copy(c, "trashedAt", clock.instant(), "version", c.version() + 1));
          events.accept(
              new ContentTrashedEvent(
                  meta.next(),
                  cid,
                  c.contentType(),
                  locales(rows, cid).stream().map(LocalizationInfo::localizationId).toList()));
          return null;
        });
  }

  public void restore(long cid) {
    store.atomic(
        rows -> {
          var c = content(rows, cid);
          if (c.trashed()) {
            rows.put(
                prefix() + "info:" + cid, copy(c, "trashedAt", null, "version", c.version() + 1));
            events.accept(new ContentRestoredEvent(meta.next(), cid, c.contentType()));
          }
          return null;
        });
  }

  public void purge(long cid) {
    store.atomic(
        rows -> {
          var c = content(rows, cid);
          if (!c.trashed()) throw new BusinessException(ContentErrors.STATE_INVALID, "必须先移入回收站");
          var locs = locales(rows, cid);
          events.accept(
              new ContentPurgedEvent(
                  meta.next(),
                  cid,
                  c.contentType(),
                  c.ownerModule(),
                  locs.stream().map(LocalizationInfo::localizationId).toList()));
          for (var l : locs) removeLocale(rows, l);
          rows.remove(prefix() + "info:" + cid);
          return null;
        });
  }

  void removeLocale(Map<String, Object> rows, LocalizationInfo l) {
    rows.remove(prefix() + "locale:" + l.localizationId());
    String revisions = prefix() + "revision:" + l.localizationId() + ":";
    String parts = prefix() + "part:" + l.localizationId() + ":";
    rows.keySet().removeIf(k -> k.startsWith(revisions) || k.startsWith(parts));
  }

  public void deleteLocalization(long id) {
    store.atomic(
        rows -> {
          var l = loc(rows, id);
          if (l.isPublished())
            throw new BusinessException(ContentErrors.HAS_PUBLISHED_LOCALIZATION, "请先下线本地化");
          if (l.isSource()) throw new BusinessException(ContentErrors.STATE_INVALID, "不能删除源语言本地化");
          removeLocale(rows, l);
          events.accept(
              new LocalizationDeletedEvent(
                  meta.next(), l.contentId(), l.contentType(), id, l.locale()));
          return null;
        });
  }

  public List<ValidationIssue> validateForPublish(long id) {
    return store.atomic(
        rows -> {
          var l = loc(rows, id);
          List<ValidationIssue> issues = new ArrayList<>();
          if (l.draftTitle().isBlank())
            issues.add(
                new ValidationIssue(
                    Severity.ERROR, "CONTENT_TITLE_REQUIRED", "标题不能为空", "title", "content"));
          return List.copyOf(issues);
        });
  }

  public ImpactReport analyzeImpact(long id, String locale, ImpactAction action) {
    findContent(id).orElseThrow(() -> new NotFoundException(ContentErrors.NOT_FOUND, "内容不存在"));
    return ImpactReport.none();
  }

  public LocalizationInfo ensureTargetLocalization(long cid, String locale) {
    if (locale.equals("zh-CN"))
      throw new BusinessException(ContentErrors.LOCALE_IS_SOURCE, "目标不能为源语言");
    return findLocalization(cid, locale)
        .orElseGet(
            () -> {
              var source = findLocalization(cid, "zh-CN").orElseThrow();
              return createLocalization(
                  new CreateLocalizationCommand(cid, locale, source.draftTitle(), null),
                  TranslationMode.AUTO);
            });
  }

  public boolean updateTranslationState(TranslationStateUpdate update) {
    return store.atomic(
        rows -> {
          var l = loc(rows, update.localizationId());
          if (l.translationMode() == TranslationMode.MANUAL
              || l.translationMode() == TranslationMode.SOURCE) return false;
          save(
              rows,
              copy(
                  l,
                  "translationState",
                  update.state(),
                  "sourceRevisionNo",
                  update.sourceRevisionNo()));
          return true;
        });
  }

  public LocalizationInfo changeTranslationMode(long id, TranslationMode mode, Integer expected) {
    return store.atomic(
        rows -> {
          var l = loc(rows, id);
          version(l, expected);
          if (l.isSource() || mode == TranslationMode.SOURCE)
            throw new BusinessException(ContentErrors.STATE_INVALID, "不能变更源语言模式");
          if (mode == l.translationMode()) return l;
          var updated = save(rows, copy(l, "translationMode", mode, "version", l.version() + 1));
          events.accept(
              new TranslationModeChangedEvent(
                  meta.next(),
                  l.contentId(),
                  l.contentType(),
                  id,
                  l.locale(),
                  l.translationMode(),
                  mode));
          return updated;
        });
  }

  public Optional<ContentInfo> findContent(long id) {
    return store.atomic(
        rows -> Optional.ofNullable((ContentInfo) rows.get(prefix() + "info:" + id)));
  }

  public Map<Long, ContentInfo> findContents(Collection<Long> ids) {
    Map<Long, ContentInfo> out = new LinkedHashMap<>();
    for (long id : ids) findContent(id).ifPresent(c -> out.put(id, c));
    return Map.copyOf(out);
  }

  public Optional<LocalizationInfo> findLocalization(long id) {
    return store.atomic(
        rows -> Optional.ofNullable((LocalizationInfo) rows.get(prefix() + "locale:" + id)));
  }

  public Optional<LocalizationInfo> findLocalization(long cid, String locale) {
    return localizations(cid).stream().filter(l -> l.locale().equals(locale)).findFirst();
  }

  public Map<Long, LocalizationInfo> findLocalizations(Collection<Long> ids) {
    Map<Long, LocalizationInfo> out = new LinkedHashMap<>();
    for (long id : ids) findLocalization(id).ifPresent(l -> out.put(id, l));
    return Map.copyOf(out);
  }

  public List<LocalizationInfo> localizations(long cid) {
    var order = siteApi.locales();
    return store.atomic(
        rows ->
            locales(rows, cid).stream()
                .sorted(
                    Comparator.comparingInt(
                        (LocalizationInfo l) ->
                            l.isSource()
                                ? -1
                                : order.stream()
                                    .filter(i -> i.locale().equals(l.locale()))
                                    .map(SiteLocaleInfo::sortOrder)
                                    .findFirst()
                                    .orElse(Integer.MAX_VALUE)))
                .toList());
  }

  public List<ContentTypeDefinition> contentTypes() {
    return types.values().stream()
        .filter(t -> siteApi.isModuleEnabled(t.module()))
        .sorted(Comparator.comparing(ContentTypeDefinition::type))
        .toList();
  }

  public Optional<ContentTypeDefinition> contentType(String type) {
    return contentTypes().stream().filter(t -> t.type().equals(type)).findFirst();
  }

  public Optional<PublishedSnapshot> findPublishedSnapshot(long id) {
    return store.atomic(
        rows -> {
          var l = (LocalizationInfo) rows.get(prefix() + "locale:" + id);
          if (l == null || !l.isPublished() || !visible(content(rows, l.contentId())))
            return Optional.empty();
          return revision(rows, id, l.publishedRevisionNo())
              .map(s -> copy(s, "payload", s.payload().deepCopy()));
        });
  }

  public Optional<PublishedSnapshot> findPublishedSnapshot(long cid, String locale) {
    return findLocalization(cid, locale).flatMap(l -> findPublishedSnapshot(l.localizationId()));
  }

  public Optional<PublishedSnapshot> findRevision(long id, int number) {
    return store.atomic(
        rows -> {
          loc(rows, id);
          return revision(rows, id, number).map(s -> copy(s, "payload", s.payload().deepCopy()));
        });
  }

  public PageResult<RevisionInfo> revisions(long id, PageRequest page) {
    return store.atomic(
        rows -> {
          loc(rows, id);
          String prefix = prefix() + "revision:" + id + ":";
          var values =
              rows.entrySet().stream()
                  .filter(e -> e.getKey().startsWith(prefix))
                  .map(e -> (PublishedSnapshot) e.getValue())
                  .sorted(Comparator.comparingInt(PublishedSnapshot::revisionNo).reversed())
                  .map(
                      s ->
                          new RevisionInfo(
                              s.revisionId(),
                              s.revisionNo(),
                              s.payloadHash(),
                              null,
                              s.publishedBy(),
                              s.publishedAt(),
                              s.trigger(),
                              s.payload()
                                  .toString()
                                  .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                  .length))
                  .toList();
          return new PageResult<>(
              values.stream().skip(page.offset()).limit(page.size()).toList(),
              page.page(),
              page.size(),
              values.size());
        });
  }

  PublishedCard card(PublishedSnapshot s) {
    var l = findLocalization(s.localizationId()).orElseThrow();
    return new PublishedCard(
        s.contentId(),
        s.localizationId(),
        s.contentType(),
        s.locale(),
        s.payload().path("card").path("title").asString(),
        null,
        null,
        s.path(),
        findContent(s.contentId()).orElseThrow().routable(),
        l.firstPublishedAt(),
        s.publishedAt(),
        s.contentModifiedAt());
  }

  public Map<Long, PublishedCard> findPublishedCards(Collection<Long> ids, String locale) {
    Map<Long, PublishedCard> result = new LinkedHashMap<>();
    for (long id : ids) findPublishedSnapshot(id, locale).ifPresent(s -> result.put(id, card(s)));
    return Map.copyOf(result);
  }

  public List<PublishedCard> publishedVersions(long cid) {
    return localizations(cid).stream()
        .map(l -> findPublishedSnapshot(l.localizationId()))
        .flatMap(Optional::stream)
        .map(this::card)
        .toList();
  }

  public void draftTerms(long localizationId, List<PublishedTermRef> terms) {
    store.atomic(
        rows -> {
          loc(rows, localizationId);
          rows.put(prefix() + "draft-terms:" + localizationId, List.copyOf(terms));
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  List<PublishedTermRef> terms(Map<String, Object> rows, long id) {
    return (List<PublishedTermRef>)
        rows.getOrDefault(prefix() + "published-terms:" + id, List.of());
  }

  List<PublishedCard> allPublished(String locale) {
    return store.atomic(
        rows -> {
          String prefix = prefix() + "locale:";
          return rows.entrySet().stream()
              .filter(e -> e.getKey().startsWith(prefix))
              .map(e -> (LocalizationInfo) e.getValue())
              .filter(l -> l.locale().equals(locale))
              .map(l -> findPublishedSnapshot(l.localizationId()))
              .flatMap(Optional::stream)
              .map(this::card)
              .toList();
        });
  }

  public List<PublishedCard> queryPublished(PublishedQuery query) {
    if (query.limit() < 1 || query.limit() > 100)
      throw new IllegalArgumentException("limit必须在1至100");
    return store.atomic(
        rows -> {
          Comparator<PublishedCard> order = Comparator.comparingLong(PublishedCard::localizationId);
          for (var sort : query.sort().reversed()) {
            Comparator<PublishedCard> field =
                switch (sort.field()) {
                  case "title" -> Comparator.comparing(PublishedCard::title);
                  case "publishedAt" -> Comparator.comparing(PublishedCard::publishedAt);
                  case "firstPublishedAt" -> Comparator.comparing(PublishedCard::firstPublishedAt);
                  default -> throw new IllegalArgumentException("排序字段未注册");
                };
            order =
                sort.descending()
                    ? field.reversed().thenComparing(order)
                    : field.thenComparing(order);
          }
          return allPublished(query.locale()).stream()
              .filter(
                  c ->
                      query.contentTypes().isEmpty()
                          || query.contentTypes().contains(c.contentType()))
              .filter(
                  c -> query.contentIds().isEmpty() || query.contentIds().contains(c.contentId()))
              .filter(c -> !query.excludeContentIds().contains(c.contentId()))
              .filter(
                  c ->
                      query.anyTermIds().isEmpty()
                          || terms(rows, c.localizationId()).stream()
                              .anyMatch(t -> query.anyTermIds().contains(t.termId())))
              .sorted(order)
              .limit(query.limit())
              .toList();
        });
  }

  public CursorPage<PublishedCard> scanPublished(PublishedScan scan, CursorRequest cursor) {
    String scope =
        sha(
            sites.requireSiteId()
                + ":"
                + scan.locale()
                + ":"
                + new TreeSet<>(scan.contentTypes())
                + ":"
                + scan.routableOnly()
                + ":"
                + scan.modifiedSince());
    long after = 0;
    if (cursor.cursor() != null) {
      String value =
          new String(
              Base64.getUrlDecoder().decode(cursor.cursor()),
              java.nio.charset.StandardCharsets.UTF_8);
      if (!value.startsWith(scope + ":")) throw new IllegalArgumentException("扫描游标范围不匹配");
      after = Long.parseLong(value.substring(scope.length() + 1));
    }
    final long minimum = after;
    var all =
        allPublished(scan.locale()).stream()
            .filter(
                c ->
                    c.localizationId() > minimum
                        && (scan.contentTypes().isEmpty()
                            || scan.contentTypes().contains(c.contentType()))
                        && (!scan.routableOnly() || c.routable())
                        && (scan.modifiedSince() == null
                            || c.contentModifiedAt().isAfter(scan.modifiedSince())))
            .sorted(Comparator.comparingLong(PublishedCard::localizationId))
            .toList();
    var values = all.stream().limit(cursor.size()).toList();
    String next =
        all.size() > values.size()
            ? Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    (scope + ":" + values.getLast().localizationId())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))
            : null;
    return new CursorPage<>(values, next);
  }

  public Map<Long, List<PublishedTermRef>> publishedTerms(Collection<Long> ids, String locale) {
    return store.atomic(
        rows -> {
          Map<Long, List<PublishedTermRef>> result = new LinkedHashMap<>();
          for (long id : ids)
            findPublishedSnapshot(id, locale)
                .ifPresent(s -> result.put(id, terms(rows, s.localizationId())));
          return Map.copyOf(result);
        });
  }

  public List<Long> contentIdsByPublishedTerms(
      Collection<Long> termIds, String locale, Set<String> types, int limit) {
    if (limit < 1 || limit > 2000) throw new IllegalArgumentException("limit必须在1至2000");
    if (termIds.isEmpty()) return List.of();
    return store.atomic(
        rows ->
            allPublished(locale).stream()
                .filter(c -> c.routable() && (types.isEmpty() || types.contains(c.contentType())))
                .map(
                    c ->
                        Map.entry(
                            c,
                            terms(rows, c.localizationId()).stream()
                                .filter(t -> termIds.contains(t.termId()))
                                .count()))
                .filter(e -> e.getValue() > 0)
                .sorted(
                    Map.Entry.<PublishedCard, Long>comparingByValue()
                        .reversed()
                        .thenComparingLong(e -> e.getKey().contentId()))
                .limit(limit)
                .map(e -> e.getKey().contentId())
                .toList());
  }

  public Map<String, Long> countPublished(Set<String> types) {
    return store.atomic(
        rows -> {
          Map<String, Long> out = new LinkedHashMap<>();
          for (String type : types) out.put(type, 0L);
          String prefix = prefix() + "locale:";
          for (var entry : rows.entrySet())
            if (entry.getKey().startsWith(prefix)) {
              var l = (LocalizationInfo) entry.getValue();
              if (types.contains(l.contentType())
                  && l.isPublished()
                  && visible(content(rows, l.contentId())))
                out.merge(l.contentType(), 1L, Long::sum);
            }
          return Map.copyOf(out);
        });
  }

  public PreviewToken createPreviewToken(long id, PreviewMode mode, Duration ttl) {
    if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofDays(7)) > 0)
      throw new BusinessException(ContentErrors.PREVIEW_TTL_INVALID, "预览期限非法");
    var l =
        findLocalization(id)
            .orElseThrow(
                () -> new NotFoundException(ContentErrors.LOCALIZATION_NOT_FOUND, "本地化不存在"));
    String token = UUID.randomUUID().toString();
    var target =
        new PreviewTarget(
            sites.requireSiteId(), l.contentId(), id, l.locale(), mode, clock.instant().plus(ttl));
    store.atomic(
        rows -> {
          rows.put("preview-token:" + token, target);
          return null;
        });
    return new PreviewToken(token, target.expiresAt());
  }

  public Optional<PreviewTarget> resolvePreviewToken(String token) {
    return store.atomic(
        rows ->
            Optional.ofNullable((PreviewTarget) rows.get("preview-token:" + token))
                .filter(
                    t ->
                        t.expiresAt().isAfter(clock.instant())
                            && t.siteId() == sites.requireSiteId()));
  }

  public PreviewSnapshot assemblePreview(PreviewRequest request) {
    return store.atomic(
        rows -> {
          var l = loc(rows, request.localizationId());
          JsonNode payload =
              request.mode() instanceof PreviewMode.Revision revision
                  ? findRevision(l.localizationId(), revision.revisionNo()).orElseThrow().payload()
                  : payload(rows, l);
          if (!request.partOverrides().isEmpty() && request.mode() instanceof PreviewMode.Revision)
            throw new BusinessException(ContentErrors.STATE_INVALID, "修订预览不允许覆盖");
          var copy = (tools.jackson.databind.node.ObjectNode) payload.deepCopy();
          var parts = (tools.jackson.databind.node.ObjectNode) copy.path("parts");
          request.partOverrides().forEach((k, v) -> parts.set(k, v.deepCopy()));
          return new PreviewSnapshot(
              l.contentId(), l.localizationId(), l.contentType(), l.locale(), copy, List.of());
        });
  }

  public JobInfo requestRouteRecalculation(Set<String> locales) {
    if (routeJobs == null) throw new IllegalStateException("Register a route job fixture");
    return routeJobs.apply(Set.copyOf(locales));
  }

  public ContentInfo setTranslationReviewRequirement(long cid, int expected, boolean required) {
    return store.atomic(
        rows -> {
          var c = content(rows, cid);
          if (c.version() != expected)
            throw new VersionConflictException(
                ContentErrors.VERSION_CONFLICT, "内容版本已变化", c.version());
          if (c.translationReviewRequired() == required) return c;
          var updated = copy(c, "translationReviewRequired", required, "version", c.version() + 1);
          rows.put(prefix() + "info:" + cid, updated);
          for (var l : locales(rows, cid))
            save(
                rows,
                copy(
                    l,
                    "translationReviewRequired",
                    required,
                    "draftState",
                    DraftState.EDITING,
                    "version",
                    l.version() + 1));
          return updated;
        });
  }
}
