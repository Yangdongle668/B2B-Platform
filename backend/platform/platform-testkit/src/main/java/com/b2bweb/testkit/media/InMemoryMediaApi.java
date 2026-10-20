package com.b2bweb.testkit.media;

import com.b2bweb.api.common.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fixture media lifecycle, local object storage and protected references. No network or malware
 * scanner runs here.
 */
public final class InMemoryMediaApi implements MediaApi {
  private record Entry(MediaInfo info, StorageKey key, Instant deleted, long gateVersion) {}

  private record Owned(MediaReferenceOwner owner, List<MediaReference> refs) {}

  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final SiteApi api;
  private final IdGenerator ids;
  private final Clock clock;
  private final ObjectStorage storage;
  private final ImageUrlSigner signer;
  private final RemoteFileFetcher fetcher;
  private final CurrentUserProvider users;
  private final byte[] ticketKey = new byte[32];
  private final JsonMapper json = JsonMapper.builder().build();

  public InMemoryMediaApi(
      MemoryStore store,
      SiteContextProvider sites,
      SiteApi api,
      IdGenerator ids,
      Clock clock,
      ObjectStorage storage,
      ImageUrlSigner signer,
      RemoteFileFetcher fetcher,
      CurrentUserProvider users) {
    this.store = store;
    this.sites = sites;
    this.api = api;
    this.ids = ids;
    this.clock = clock;
    this.storage = storage;
    this.signer = signer;
    this.fetcher = fetcher;
    this.users = users;
    new SecureRandom().nextBytes(ticketKey);
  }

  private String root() {
    return "media:" + sites.requireSiteId() + ":";
  }

  private String key(long id) {
    return root() + "asset:" + id;
  }

  public void seed(MediaInfo info, StorageKey key) {
    if (!key.belongsToSite(sites.requireSiteId())) throw new IllegalArgumentException("存储键不属于当前站点");
    store.atomic(
        rows -> {
          rows.put(key(info.mediaId()), new Entry(info, key, null, 0));
          return null;
        });
  }

  public void localize(long id, String locale, String alt, String title, String caption) {
    require(id);
    store.atomic(
        rows -> {
          rows.put(root() + "text:" + id + ":" + locale, Arrays.asList(alt, title, caption));
          return null;
        });
  }

  private Entry require(long id) {
    return store.atomic(
        rows -> {
          var entry = (Entry) rows.get(key(id));
          if (entry == null || entry.deleted() != null)
            throw new NotFoundException(MediaErrors.NOT_FOUND, "媒体不存在");
          return entry;
        });
  }

  public Optional<MediaInfo> find(long id) {
    return store.atomic(
        rows -> {
          var e = (Entry) rows.get(key(id));
          return e == null || e.deleted() != null ? Optional.empty() : Optional.of(e.info());
        });
  }

  public Map<Long, MediaInfo> findAll(Collection<Long> values) {
    if (values.size() > 500) throw new IllegalArgumentException("媒体批次最多500项");
    Map<Long, MediaInfo> found = new LinkedHashMap<>();
    values.forEach(id -> find(id).ifPresent(v -> found.put(id, v)));
    return Map.copyOf(found);
  }

  private void ready(MediaInfo info) {
    if (info.status() == MediaStatus.QUARANTINED)
      throw new BusinessException(MediaErrors.QUARANTINED, "文件未通过扫描");
    if (!info.isReady()) throw new BusinessException(MediaErrors.NOT_READY, "文件仍在处理中");
  }

  public void status(long id, MediaStatus status) {
    store.atomic(
        rows -> {
          var e = require(id);
          rows.put(
              key(id),
              new Entry(
                  Records.copy(e.info(), "status", status), e.key(), e.deleted(), e.gateVersion()));
          return null;
        });
  }

  public UploadResult upload(UploadCommand command, InputStream input) {
    long maximum =
        Math.min(
            command.source() == UploadSource.FORM_ATTACHMENT
                ? 20L * 1024 * 1024
                : 50L * 1024 * 1024,
            command.maxBytes() == null ? Long.MAX_VALUE : command.maxBytes());
    if (command.size() < 1 || command.size() > maximum)
      throw new BusinessException(MediaErrors.TOO_LARGE, "文件大小超限");
    byte[] value;
    try {
      value = input.readNBytes((int) maximum + 1);
    } catch (IOException e) {
      throw new BusinessException(MediaErrors.TYPE_NOT_ALLOWED, "文件无法读取");
    }
    if (value.length != command.size() || value.length > maximum)
      throw new BusinessException(MediaErrors.TOO_LARGE, "文件大小与声明不一致");
    String ext =
        command
            .fileName()
            .substring(command.fileName().lastIndexOf('.') + 1)
            .toLowerCase(Locale.ROOT);
    String mime =
        switch (ext) {
          case "jpg", "jpeg" -> "image/jpeg";
          case "png" -> "image/png";
          case "webp" -> "image/webp";
          case "avif" -> "image/avif";
          case "svg" -> "image/svg+xml";
          case "pdf" -> "application/pdf";
          case "zip" -> "application/zip";
          case "mp4" -> "video/mp4";
          default -> throw new BusinessException(MediaErrors.TYPE_NOT_ALLOWED, "不允许的文件扩展名");
        };
    MediaKind kind =
        mime.startsWith("image/")
            ? MediaKind.IMAGE
            : mime.startsWith("video/")
                ? MediaKind.VIDEO
                : ext.equals("zip") ? MediaKind.ARCHIVE : MediaKind.DOCUMENT;
    if (!command.allowedKinds().isEmpty() && !command.allowedKinds().contains(kind))
      throw new BusinessException(MediaErrors.TYPE_NOT_ALLOWED, "文件类型不允许");
    if (ext.equals("svg")
        && (!users.current().map(CurrentUser::systemAdmin).orElse(false)
            || new String(value, StandardCharsets.UTF_8)
                .matches(
                    "(?is).*(<script|<!DOCTYPE|<foreignObject|\\son[a-z]+\\s*=|(?:href|src)\\s*=).*")))
      throw new BusinessException(MediaErrors.SVG_FORBIDDEN, "SVG不满足安全夹具约束");
    if (ext.equals("pdf")
            && !new String(value, 0, Math.min(value.length, 5), StandardCharsets.US_ASCII)
                .startsWith("%PDF-")
        || Set.of("jpg", "jpeg").contains(ext)
            && (value.length < 2 || (value[0] & 255) != 255 || (value[1] & 255) != 216)
        || ext.equals("png") && (value.length < 8 || value[0] != (byte) 137 || value[1] != 80))
      throw new BusinessException(MediaErrors.MIME_MISMATCH, "文件头与扩展名不一致");
    try {
      String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
      if (command.reuseDuplicate()) {
        var duplicate =
            store.atomic(
                rows ->
                    rows.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(root() + "asset:"))
                        .map(e -> (Entry) e.getValue())
                        .filter(
                            e ->
                                e.deleted() == null
                                    && e.info().sha256().equals(sha)
                                    && e.info().visibility() == command.visibility())
                        .findFirst());
        if (duplicate.isPresent()) return new UploadResult(duplicate.get().info(), true);
      }
      long id = ids.nextId();
      var key = StorageKey.media(sites.requireSiteId(), YearMonth.now(clock), id, "file", ext);
      Integer width = null, height = null;
      if (kind == MediaKind.IMAGE) {
        var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(value));
        if (image != null) {
          width = image.getWidth();
          height = image.getHeight();
        }
      }
      var info =
          new MediaInfo(
              id,
              kind,
              command.visibility(),
              kind == MediaKind.DOCUMENT || kind == MediaKind.ARCHIVE
                  ? MediaStatus.PROCESSING
                  : MediaStatus.READY,
              command.fileName(),
              mime,
              value.length,
              sha,
              width,
              height,
              null,
              null,
              null,
              null,
              null,
              command.folderId(),
              null,
              command.source(),
              clock.instant());
      storage.put(key, new ByteArrayInputStream(value), value.length, mime, command.visibility());
      seed(info, key);
      return new UploadResult(info, false);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public void allowRemoteHosts(Set<String> hosts) {
    store.atomic(
        rows -> {
          rows.put(root() + "remote-hosts", Set.copyOf(hosts));
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  public UploadResult uploadFromUrl(RemoteUploadCommand command) {
    try (var file =
            fetcher.fetch(
                RemoteFetchRequest.image(
                    command.url(),
                    store.atomic(
                        rows ->
                            (Set<String>) rows.getOrDefault(root() + "remote-hosts", Set.of()))));
        var input = file.open()) {
      return upload(
          new UploadCommand(
              command.fileName() == null
                  ? Objects.requireNonNullElse(file.fileName(), "file.jpg")
                  : command.fileName(),
              file.contentType(),
              file.size(),
              command.visibility(),
              command.source(),
              command.folderId(),
              command.reuseDuplicate(),
              command.allowedKinds(),
              null),
          input);
    } catch (IOException e) {
      throw new BusinessException(MediaErrors.REMOTE_FETCH_FAILED, "远程文件夹具读取失败");
    }
  }

  @SuppressWarnings("unchecked")
  public Map<Long, MediaView> views(Collection<Long> values, String locale) {
    Map<Long, MediaView> result = new LinkedHashMap<>();
    for (var info : findAll(values).values()) {
      var texts =
          store.atomic(
              rows ->
                  (List<String>)
                      rows.getOrDefault(
                          root() + "text:" + info.mediaId() + ":" + locale,
                          Arrays.asList(null, null, null)));
      result.put(
          info.mediaId(),
          new MediaView(
              info.mediaId(),
              locale,
              info.kind(),
              info.visibility(),
              info.status(),
              info.originalName(),
              info.mime(),
              info.size(),
              info.width(),
              info.height(),
              texts.get(0),
              texts.get(1),
              texts.get(2),
              info.blurhash(),
              info.dominantColor(),
              publicFileUrl(info.mediaId()).orElse(null)));
    }
    return Map.copyOf(result);
  }

  public Optional<URI> imageUrl(long id, ImagePreset preset, int width, ImageFormat format) {
    return find(id)
        .filter(
            i ->
                i.isImage()
                    && i.isReady()
                    && i.visibility() == Visibility.PUBLIC
                    && i.width() != null)
        .map(
            info -> {
              int standard =
                  ImagePreset.STANDARD_WIDTHS.stream()
                      .filter(w -> w >= width)
                      .findFirst()
                      .orElse(1920);
              int actual = Math.min(Math.min(standard, preset.maxWidth()), info.width());
              return image(info, preset, actual, format);
            });
  }

  private URI image(MediaInfo info, ImagePreset preset, int width, ImageFormat format) {
    Integer height =
        preset.aspectRatio() == null ? null : (int) Math.round(width / preset.aspectRatio());
    return signer.sign(
        new ImageTransform(
            require(info.mediaId()).key(),
            preset.mode(),
            width,
            height,
            info.focalX(),
            info.focalY(),
            format,
            null));
  }

  public Map<Long, ImageSources> imageSources(
      Collection<Long> values, ImagePreset preset, String locale) {
    Map<Long, ImageSources> result = new LinkedHashMap<>();
    for (var info : findAll(values).values())
      if (info.isImage()
          && info.isReady()
          && info.visibility() == Visibility.PUBLIC
          && info.width() != null
          && info.height() != null) {
        int maximum = Math.min(info.width(), preset.maxWidth());
        List<Integer> widths =
            new ArrayList<>(
                ImagePreset.STANDARD_WIDTHS.stream().filter(w -> w <= maximum).toList());
        if (widths.isEmpty() || widths.getLast() != maximum) widths.add(maximum);
        ImageFormat original = info.mime().equals("image/png") ? ImageFormat.PNG : ImageFormat.JPEG;
        Map<ImageFormat, List<ImageCandidate>> candidates = new EnumMap<>(ImageFormat.class);
        for (var format :
            new LinkedHashSet<>(List.of(ImageFormat.AVIF, ImageFormat.WEBP, original)))
          candidates.put(
              format,
              widths.stream()
                  .map(w -> new ImageCandidate(w, image(info, preset, w, format)))
                  .toList());
        int height =
            preset.aspectRatio() == null
                ? (int) Math.round((double) maximum * info.height() / info.width())
                : (int) Math.round(maximum / preset.aspectRatio());
        String alt = views(List.of(info.mediaId()), locale).get(info.mediaId()).alt();
        result.put(
            info.mediaId(),
            new ImageSources(
                info.mediaId(),
                preset,
                maximum,
                height,
                alt,
                info.blurhash(),
                info.dominantColor(),
                image(info, preset, maximum, original),
                candidates));
      }
    return Map.copyOf(result);
  }

  public Optional<URI> publicFileUrl(long id) {
    return find(id)
        .filter(i -> i.isReady() && i.visibility() == Visibility.PUBLIC)
        .map(i -> storage.publicUrl(require(id).key()));
  }

  public URI privateDownloadUrl(long id, Duration ttl, String name) {
    var entry = require(id);
    ready(entry.info());
    if (entry.info().visibility() != Visibility.PRIVATE)
      throw new BusinessException(MediaErrors.NOT_PRIVATE, "文件不是私有文件");
    if (ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofHours(1)) > 0)
      throw new IllegalArgumentException("员工下载TTL必须在1分钟至1小时内");
    return storage.presignedGet(entry.key(), ttl, name);
  }

  public void setDownloadGate(long id, String form) {
    store.atomic(
        rows -> {
          var e = require(id);
          if (e.info().visibility() != Visibility.PRIVATE)
            throw new BusinessException(MediaErrors.NOT_PRIVATE, "门控只用于私有文件");
          if (!Objects.equals(e.info().downloadGateFormKey(), form))
            rows.put(
                key(id),
                new Entry(
                    Records.copy(e.info(), "downloadGateFormKey", form),
                    e.key(),
                    e.deleted(),
                    e.gateVersion() + 1));
          return null;
        });
  }

  public DownloadTicket issueGatedDownload(GatedDownloadCommand command) {
    if (!org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) throw new IllegalStateException("票据必须加入表单提交事务");
    return store.atomic(
        rows -> {
          var e = require(command.mediaId());
          ready(e.info());
          if (e.info().visibility() != Visibility.PRIVATE
              || !Objects.equals(e.info().downloadGateFormKey(), command.formKey())
              || command.formKey() == null
              || usages(command.mediaId()).stream()
                  .noneMatch(
                      u ->
                          u.published()
                              && Objects.equals(u.contentId(), command.contentId())
                              && Objects.equals(u.locale(), command.locale()))
              || !command.expiresAt().isAfter(clock.instant())
              || command.expiresAt().isAfter(clock.instant().plus(Duration.ofDays(7))))
            throw new BusinessException(MediaErrors.DOWNLOAD_NOT_ALLOWED, "门控票据条件不满足");
          var payload =
              json.createObjectNode()
                  .put("site", Long.toString(sites.requireSiteId()))
                  .put("submission", Long.toString(command.submissionId()))
                  .put("media", Long.toString(command.mediaId()))
                  .put("content", Long.toString(command.contentId()))
                  .put("locale", command.locale())
                  .put("form", command.formKey())
                  .put("version", e.gateVersion())
                  .put("expires", command.expiresAt().toEpochMilli());
          String body =
              Base64.getUrlEncoder()
                  .withoutPadding()
                  .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
          return new DownloadTicket(
              URI.create(
                  api.primaryOrigin()
                      + "/api/public/v1/media/downloads/"
                      + command.mediaId()
                      + "?ticket="
                      + body
                      + "."
                      + signature(body)),
              command.expiresAt());
        });
  }

  private String signature(String body) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(ticketKey, "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean validateTicket(DownloadTicket ticket) {
    try {
      String token = ticket.url().getRawQuery().substring("ticket=".length());
      String[] parts = token.split("\\.");
      if (parts.length != 2
          || !MessageDigest.isEqual(
              signature(parts[0]).getBytes(StandardCharsets.US_ASCII),
              parts[1].getBytes(StandardCharsets.US_ASCII))) return false;
      var value = json.readTree(Base64.getUrlDecoder().decode(parts[0]));
      if (value.path("site").asLong() != sites.requireSiteId()
          || value.path("expires").asLong() <= clock.millis()) return false;
      var entry = require(value.path("media").asLong());
      return entry.info().isReady()
          && entry.info().visibility() == Visibility.PRIVATE
          && entry.gateVersion() == value.path("version").asLong()
          && Objects.equals(entry.info().downloadGateFormKey(), value.path("form").asString())
          && usages(entry.info().mediaId()).stream()
              .anyMatch(
                  u ->
                      u.published()
                          && Objects.equals(u.contentId(), value.path("content").asLong())
                          && Objects.equals(u.locale(), value.path("locale").asString()));
    } catch (RuntimeException e) {
      return false;
    }
  }

  public List<MediaUsageInfo> usages(long id) {
    String prefix = root() + "owner:";
    return store.atomic(
        rows ->
            rows.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> (Owned) e.getValue())
                .flatMap(
                    e ->
                        e.refs().stream()
                            .filter(r -> r.mediaId() == id)
                            .map(
                                r ->
                                    new MediaUsageInfo(
                                        id,
                                        e.owner().ownerType().equals("CONTENT")
                                            ? e.owner().ownerId()
                                            : null,
                                        e.owner().locale(),
                                        e.owner().ownerType().equals("CONTENT")
                                            ? null
                                            : e.owner().ownerType(),
                                        e.owner().ownerType().equals("CONTENT")
                                            ? null
                                            : Long.toString(e.owner().ownerId()),
                                        r.part(),
                                        r.fieldPath(),
                                        e.owner().stage() == ReferenceStage.PUBLISHED)))
                .toList());
  }

  public void replace(MediaReferenceOwner owner, List<MediaReference> references) {
    store.atomic(
        rows -> {
          for (var ref : references) {
            var info = require(ref.mediaId()).info();
            if (info.status() == MediaStatus.QUARANTINED)
              throw new BusinessException(MediaErrors.QUARANTINED, "引用文件已隔离");
            if (owner.stage() == ReferenceStage.PUBLISHED) ready(info);
          }
          rows.put(
              root()
                  + "owner:"
                  + owner.ownerType()
                  + ":"
                  + owner.ownerId()
                  + ":"
                  + owner.locale()
                  + ":"
                  + owner.stage(),
              new Owned(owner, List.copyOf(new LinkedHashSet<>(references))));
          return null;
        });
  }

  public void delete(long id, boolean force) {
    store.atomic(
        rows -> {
          var entry = (Entry) rows.get(key(id));
          if (entry == null || entry.deleted() != null) return null;
          if (!usages(id).isEmpty()) throw new BusinessException(MediaErrors.IN_USE, "媒体仍被引用");
          if (force
              && (entry.info().visibility() != Visibility.PRIVATE
                  || entry.info().source() != UploadSource.FORM_ATTACHMENT))
            throw new BusinessException(MediaErrors.IN_USE, "只能强制清理无引用的模块私有附件");
          rows.put(
              key(id),
              new Entry(entry.info(), entry.key(), clock.instant(), entry.gateVersion() + 1));
          if (force) storage.delete(entry.key(), Visibility.PRIVATE);
          return null;
        });
  }

  public void restore(long id) {
    store.atomic(
        rows -> {
          var e = (Entry) rows.get(key(id));
          if (e == null) throw new NotFoundException(MediaErrors.NOT_FOUND, "媒体不存在");
          if (storage.stat(e.key(), e.info().visibility()).isEmpty())
            throw new NotFoundException(MediaErrors.NOT_FOUND, "媒体对象不存在");
          rows.put(key(id), new Entry(e.info(), e.key(), null, e.gateVersion() + 1));
          return null;
        });
  }
}
