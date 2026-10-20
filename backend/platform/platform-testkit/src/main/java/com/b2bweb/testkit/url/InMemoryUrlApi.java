package com.b2bweb.testkit.url;

import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.url.*;
import com.b2bweb.testkit.common.*;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;

public final class InMemoryUrlApi implements UrlApi {
  private record Route(long content, long localization, String type, String locale, String path) {}

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final SiteApi api;
  private final IdGenerator ids;
  private final EventMetaFactory meta;
  private final Consumer<DomainEvent> events;

  public InMemoryUrlApi(
      MemoryStore store,
      SiteContextProvider sites,
      SiteApi api,
      IdGenerator ids,
      EventMetaFactory meta,
      Consumer<DomainEvent> events) {
    this.store = store;
    this.sites = sites;
    this.api = api;
    this.ids = ids;
    this.meta = meta;
    this.events = events;
  }

  private String root() {
    return "url:" + sites.requireSiteId() + ":";
  }

  private String route(long id) {
    return root() + "route:" + id;
  }

  private String redirect(String source) {
    return root() + "redirect:" + source;
  }

  private BusinessException conflict() {
    return new BusinessException(UrlErrors.PATH_CONFLICT, "URL路径已被占用");
  }

  private List<Route> routes(Map<String, Object> rows) {
    String prefix = root() + "route:";
    return rows.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .map(e -> (Route) e.getValue())
        .toList();
  }

  private List<RedirectInfo> redirects(Map<String, Object> rows) {
    String prefix = root() + "redirect:";
    return rows.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .map(e -> (RedirectInfo) e.getValue())
        .toList();
  }

  private String slash(String value) {
    if (value.equals("/")) return value;
    return api.current().trailingSlash() == TrailingSlash.ALWAYS
        ? value.replaceAll("/+$", "") + "/"
        : value.replaceAll("/+$", "");
  }

  private String path(RouteRequest request) {
    var locale =
        api.locale(request.locale())
            .orElseThrow(() -> new BusinessException(CoreErrors.LOCALE_NOT_ENABLED, "站点未启用该语言"));
    if (request.hints().localeRoot()) return localeHomePath(request.locale());
    String pattern =
        Objects.requireNonNullElse(
            request.hints().defaultPatternOverride(), request.defaultPattern());
    if (pattern == null) throw new IllegalArgumentException("缺少URL规则");
    Map<String, String> vars = new HashMap<>(request.hints().variables());
    vars.put("slug", request.slug());
    vars.put("locale", locale.urlPrefix());
    vars.put("contentId", Long.toString(request.contentId()));
    if (request.hints().parentContentId() != null) {
      String parent =
          store.atomic(
              rows ->
                  routes(rows).stream()
                      .filter(
                          r ->
                              r.content() == request.hints().parentContentId()
                                  && r.locale().equals(request.locale()))
                      .map(Route::path)
                      .findFirst()
                      .orElseThrow(() -> new IllegalArgumentException("父路径不存在")));
      vars.put("parentPath", parent);
    }
    String result = pattern;
    for (var entry : vars.entrySet())
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    if (result.contains("{")
        || !result.startsWith("/")
        || result.contains("..")
        || result.contains("?")
        || result.contains("#")) throw new IllegalArgumentException("URL规则变量缺失或路径无效");
    if (!result.startsWith("/" + locale.urlPrefix() + "/")
        && !result.equals("/" + locale.urlPrefix())) result = "/" + locale.urlPrefix() + result;
    result = slash(result.replaceAll("/+", "/"));
    if (result.length() > 2048) throw new IllegalArgumentException("URL路径过长");
    return result;
  }

  public PathReservation reservePath(RouteRequest request) {
    if (!validateSlug(request.slug(), request.locale()).isEmpty())
      return new PathReservation.Invalid(request.slug(), "URL_PATH_RESERVED", "slug不合法或为保留字");
    String path;
    try {
      path = path(request);
    } catch (IllegalArgumentException e) {
      return new PathReservation.Invalid(request.slug(), "URL_PATTERN_INVALID", "URL规则不合法");
    }
    return store.atomic(
        rows -> {
          var occupied =
              routes(rows).stream()
                  .filter(
                      r -> r.path().equals(path) && r.localization() != request.localizationId())
                  .findFirst();
          if (occupied.isPresent()) {
            var r = occupied.get();
            return new PathReservation.Conflict(
                path, r.content(), r.localization(), suggestAvailableSlug(request));
          }
          var previous = (RedirectInfo) rows.get(redirect(path));
          return new PathReservation.Available(
              path, previous == null ? null : previous.targetLocalizationId());
        });
  }

  public String suggestAvailableSlug(RouteRequest request) {
    return store.atomic(
        rows -> {
          for (int i = 1; i <= 50; i++) {
            String slug = request.slug() + (i == 1 ? "" : "-" + i);
            String candidate = path(Records.copy(request, "slug", slug));
            if (routes(rows).stream()
                .noneMatch(
                    r ->
                        r.path().equals(candidate) && r.localization() != request.localizationId()))
              return slug;
          }
          throw conflict();
        });
  }

  public RouteChange applyRoute(ApplyRouteCommand command) {
    long site = sites.requireSiteId();
    if (!command.path().startsWith(localeHomePath(command.locale()).replaceAll("/+$", ""))
        || command.path().length() > 2048) throw new IllegalArgumentException("路由必须带对应语言前缀");
    return store.atomic(
        rows -> {
          if (routes(rows).stream()
              .anyMatch(
                  r ->
                      r.path().equals(command.path())
                          && r.localization() != command.localizationId())) throw conflict();
          var old = (Route) rows.get(route(command.localizationId()));
          String previous = old == null ? null : old.path();
          if (Objects.equals(previous, command.path()))
            return new RouteChange(
                command.localizationId(), previous, command.path(), false, List.of());
          if (old != null)
            rows.put(
                redirect(old.path()),
                new RedirectInfo(
                    ids.nextId(),
                    old.path(),
                    RedirectMatchType.EXACT,
                    RedirectTargetKind.LOCALIZATION,
                    command.localizationId(),
                    null,
                    301,
                    RedirectOrigin.AUTO));
          boolean removed = rows.remove(redirect(command.path())) != null;
          rows.put(
              route(command.localizationId()),
              new Route(
                  command.contentId(),
                  command.localizationId(),
                  command.contentType(),
                  command.locale(),
                  command.path()));
          events.accept(
              new ContentPathChangedEvent(
                  meta.next(),
                  command.contentId(),
                  command.localizationId(),
                  command.locale(),
                  previous,
                  command.path(),
                  PathChangeReason.PUBLISHED,
                  null));
          return new RouteChange(
              command.localizationId(),
              previous,
              command.path(),
              true,
              removed ? List.of(command.path()) : List.of());
        });
  }

  public RouteChange retireRoute(RetireRouteCommand command) {
    return store.atomic(
        rows -> {
          var old = (Route) rows.remove(route(command.localizationId()));
          for (var redirect : new ArrayList<>(redirects(rows)))
            if (Objects.equals(redirect.targetLocalizationId(), command.localizationId()))
              rows.put(redirect(redirect.sourcePath()), retired(redirect.sourcePath(), command));
          if (old != null) {
            rows.put(redirect(old.path()), retired(old.path(), command));
            events.accept(
                new ContentPathChangedEvent(
                    meta.next(),
                    old.content(),
                    old.localization(),
                    old.locale(),
                    old.path(),
                    null,
                    PathChangeReason.RETIRED,
                    command.mode()));
          }
          return new RouteChange(
              command.localizationId(),
              old == null ? null : old.path(),
              null,
              old != null,
              List.of());
        });
  }

  private RedirectInfo retired(String source, RetireRouteCommand command) {
    boolean gone = command.mode() == RetireMode.GONE;
    if (!gone
        && (command.targetLocalizationId() == null
            || command.targetLocalizationId() == command.localizationId()
            || pathOf(command.targetLocalizationId()).isEmpty()))
      throw new IllegalArgumentException("退役跳转目标不可用");
    return new RedirectInfo(
        ids.nextId(),
        source,
        RedirectMatchType.EXACT,
        gone ? RedirectTargetKind.GONE : RedirectTargetKind.LOCALIZATION,
        gone ? null : command.targetLocalizationId(),
        null,
        gone ? 410 : 301,
        RedirectOrigin.AUTO);
  }

  public void releaseRoutes(long content) {
    store.atomic(
        rows -> {
          for (var route : new ArrayList<>(routes(rows)))
            if (route.content() == content)
              retireRoute(new RetireRouteCommand(route.localization(), RetireMode.GONE, null));
          return null;
        });
  }

  public String generateSlug(String text, String locale) {
    String normalized =
        Normalizer.normalize(text, Normalizer.Form.NFC).strip().toLowerCase(Locale.ROOT);
    if (Set.of("ja", "ko", "th").contains(locale))
      return normalized.replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
    String latin =
        com.ibm.icu.text.Transliterator.getInstance("Any-Latin; Latin-ASCII")
            .transliterate(normalized)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    return latin.length() <= 80 ? latin : latin.substring(0, 80).replaceAll("-$", "");
  }

  public List<ValidationIssue> validateSlug(String slug, String locale) {
    if (slug.isBlank()
        || slug.length() > 160
        || Set.of("api", "admin", "assets", "_nuxt", "robots.txt", "sitemap.xml").contains(slug)
        || !Normalizer.isNormalized(slug, Normalizer.Form.NFC)
        || !slug.matches("[\\p{L}\\p{N}]+(?:-[\\p{L}\\p{N}]+)*"))
      return List.of(
          new ValidationIssue(Severity.ERROR, "URL_SLUG_INVALID", "slug格式不合法", "slug", "url"));
    return List.of();
  }

  public Optional<String> pathOf(long id) {
    return store.atomic(rows -> Optional.ofNullable((Route) rows.get(route(id))).map(Route::path));
  }

  public Map<Long, String> pathsOf(Collection<Long> ids) {
    if (ids.size() > 500) throw new IllegalArgumentException("批次超限");
    Map<Long, String> result = new LinkedHashMap<>();
    ids.forEach(id -> pathOf(id).ifPresent(path -> result.put(id, path)));
    return Map.copyOf(result);
  }

  public String localeHomePath(String locale) {
    return slash(
        "/"
            + api.locale(locale)
                .orElseThrow(() -> new BusinessException(CoreErrors.LOCALE_NOT_ENABLED, "语言未配置"))
                .urlPrefix());
  }

  public String absoluteUrl(String path) {
    if (!path.startsWith("/") || path.startsWith("//"))
      throw new IllegalArgumentException("必须为站内绝对路径");
    return api.primaryOrigin() + path;
  }

  public void recordNotFound(String path, String referrer) {
    store.atomic(
        rows -> {
          String key = root() + "404:" + path;
          rows.put(key, ((Number) rows.getOrDefault(key, 0L)).longValue() + 1);
          return null;
        });
  }

  public RouteResolution resolve(String raw) {
    if (raw.equals("/"))
      return new RouteResolution.Redirect(
          302, localeHomePath(api.current().defaultLocale()), RedirectReason.ROOT);
    String normalized = slash(raw.replaceAll("/+", "/"));
    String prefix = normalized.substring(1).split("/", 2)[0];
    if (api.localeByUrlPrefix(prefix).filter(SiteLocaleInfo::publiclyVisible).isEmpty())
      return new RouteResolution.NotFound(raw);
    if (!raw.equals(normalized))
      return new RouteResolution.Redirect(301, normalized, RedirectReason.NORMALIZE);
    return store.atomic(
        rows -> {
          var route = routes(rows).stream().filter(r -> r.path().equals(raw)).findFirst();
          if (route.isPresent()) {
            var r = route.get();
            return new RouteResolution.Content(r.content(), r.localization(), r.locale(), r.path());
          }
          var redirect =
              redirects(rows).stream()
                  .filter(
                      r ->
                          r.matchType() == RedirectMatchType.EXACT && r.sourcePath().equals(raw)
                              || r.matchType() == RedirectMatchType.PREFIX
                                  && raw.startsWith(r.sourcePath())
                              || r.matchType() == RedirectMatchType.REGEX
                                  && java.util.regex.Pattern.compile(r.sourcePath())
                                      .matcher(raw)
                                      .find())
                  .sorted(
                      Comparator.comparingInt(
                          r ->
                              r.matchType() == RedirectMatchType.EXACT
                                  ? 0
                                  : r.matchType() == RedirectMatchType.PREFIX ? 1 : 2))
                  .findFirst();
          if (redirect.isEmpty()) return new RouteResolution.NotFound(raw);
          var r = redirect.get();
          if (r.targetKind() == RedirectTargetKind.GONE) return new RouteResolution.Gone(raw);
          String target =
              r.targetKind() == RedirectTargetKind.LOCALIZATION
                  ? pathOf(r.targetLocalizationId()).orElse(null)
                  : r.targetPath();
          if (target == null) return new RouteResolution.Gone(raw);
          if (r.matchType() == RedirectMatchType.PREFIX)
            target = target + raw.substring(r.sourcePath().length());
          if (r.matchType() == RedirectMatchType.REGEX)
            target =
                java.util.regex.Pattern.compile(r.sourcePath()).matcher(raw).replaceFirst(target);
          return new RouteResolution.Redirect(r.statusCode(), target, RedirectReason.REDIRECT_RULE);
        });
  }

  public RedirectInfo addRedirect(CreateRedirectCommand command) {
    return store.atomic(
        rows -> {
          if (routes(rows).stream().anyMatch(r -> r.path().equals(command.sourcePath())))
            throw new BusinessException(UrlErrors.REDIRECT_SOURCE_IS_ROUTE, "重定向源已是路由");
          if (command.matchType() != RedirectMatchType.EXACT
              && redirects(rows).stream()
                      .filter(r -> r.matchType() != RedirectMatchType.EXACT)
                      .count()
                  >= 200) throw new BusinessException(UrlErrors.REDIRECT_LIMIT, "重定向规则超限");
          if (!Set.of(301, 302, 308, 410).contains(command.statusCode()))
            throw new IllegalArgumentException("重定向状态无效");
          String target = command.targetPath();
          Long localization = command.targetLocalizationId();
          RedirectTargetKind kind = command.targetKind();
          Set<String> visited = new HashSet<>(Set.of(command.sourcePath()));
          while (kind != RedirectTargetKind.LOCALIZATION
              && kind != RedirectTargetKind.GONE
              && target != null
              && target.startsWith("/")) {
            if (!visited.add(target))
              throw new BusinessException(UrlErrors.REDIRECT_LOOP, "重定向形成环路");
            String current = target;
            var route = routes(rows).stream().filter(r -> r.path().equals(current)).findFirst();
            if (route.isPresent()) {
              localization = route.get().localization();
              kind = RedirectTargetKind.LOCALIZATION;
              target = null;
              break;
            }
            var next = (RedirectInfo) rows.get(redirect(current));
            if (next == null) break;
            kind = next.targetKind();
            localization = next.targetLocalizationId();
            target = next.targetPath();
          }
          if (kind == RedirectTargetKind.LOCALIZATION
              && (localization == null || pathOf(localization).isEmpty()))
            throw new IllegalArgumentException("跳转目标不可用");
          var result =
              new RedirectInfo(
                  ids.nextId(),
                  command.sourcePath(),
                  command.matchType(),
                  kind,
                  localization,
                  target,
                  command.statusCode(),
                  command.origin());
          rows.put(redirect(command.sourcePath()), result);
          return result;
        });
  }
}
