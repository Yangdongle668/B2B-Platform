package com.b2bweb.delivery.preview;

import com.b2bweb.api.content.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.translation.TranslationMode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** M0 preview transport. Assembly stays in ContentApi and its registered contributors. */
@RestController
@RequestMapping("/api/public/v1/delivery/preview")
public class PreviewController {
  private static final int MAX_BODY = 1_048_576;
  private final ObjectProvider<ContentApi> contents;
  private final SiteContextProvider sites;
  private final SiteApi siteApi;
  private final JsonMapper json;

  public PreviewController(ObjectProvider<ContentApi> contents, SiteContextProvider sites,
      SiteApi siteApi, JsonMapper json) {
    this.contents = contents; this.sites = sites; this.siteApi = siteApi; this.json = json;
  }

  public record PreviewResponse(PreviewSnapshot snapshot, String siteId, String locale,
      Set<String> enabledModules, boolean readonly) {}

  @GetMapping
  public ResponseEntity<?> get(@RequestParam String token) {
    return preview(token, Map.of(), false);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> editor(HttpServletRequest request) throws IOException {
    byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
    if (bytes.length > MAX_BODY) return problem(413, "DELIVERY_PREVIEW_TOO_LARGE", "预览请求超过大小限制");
    JsonNode body;
    try { body = json.readTree(bytes); }
    catch (RuntimeException e) { return problem(400, "DELIVERY_PREVIEW_INVALID_REQUEST", "预览请求格式不正确"); }
    if (body == null || !body.isObject() || !body.path("token").isString()
        || !body.path("partOverrides").isObject()
        || body.propertyNames().stream().anyMatch(k -> !Set.of("token", "partOverrides").contains(k)))
      return problem(400, "DELIVERY_PREVIEW_INVALID_REQUEST", "预览请求字段不正确");
    Map<String, JsonNode> parts = new HashMap<>();
    for (var property : body.path("partOverrides").properties()) {
      parts.put(property.getKey(), property.getValue());
    }
    return preview(body.path("token").asString(), parts, true);
  }

  private ResponseEntity<?> preview(String token, Map<String, JsonNode> parts, boolean editor) {
    if (token == null || token.isBlank() || token.length() > 4096)
      return problem(404, "DELIVERY_PREVIEW_INVALID", "预览令牌无效或已过期");
    try {
      ContentApi content = contents.getIfAvailable();
      if (content == null) return unavailable();
      var resolved = content.resolvePreviewToken(token);
      if (resolved.isEmpty()) return invalid();
      var target = resolved.get();
      if (target.siteId() != sites.requireSiteId()
          || editor && (target.purpose() != PreviewPurpose.EDITOR
              || !(target.mode() instanceof PreviewMode.Draft) || target.issuedBy() == null)) return invalid();
      if (parts.keySet().stream().anyMatch(key -> !key.equals("layout")))
        return problem(422, "DELIVERY_PREVIEW_INVALID_LAYOUT", "只允许覆盖布局部分");
      var localization = content.findLocalization(target.localizationId());
      if (localization.isEmpty()) return invalid();
      boolean readonly = localization.get().translationMode() == TranslationMode.AUTO;
      if (readonly && !parts.isEmpty())
        return problem(422, "DELIVERY_PREVIEW_INVALID_LAYOUT", "自动翻译语言只能预览");
      var snapshot = content.assemblePreview(new PreviewRequest(target.localizationId(), target.mode(), parts, target.purpose()));
      return response(200, new PreviewResponse(snapshot, Long.toString(target.siteId()), target.locale(), siteApi.enabledModules(), readonly));
    } catch (com.b2bweb.api.common.PlatformException e) {
      if (e.errorCode().status() >= 500) return unavailable();
      if (e.errorCode().status() == 422 || e.errorCode().status() == 400)
        return problem(422, "DELIVERY_PREVIEW_INVALID_LAYOUT", "布局不符合约束");
      return invalid();
    } catch (IllegalArgumentException e) {
      return problem(422, "DELIVERY_PREVIEW_INVALID_LAYOUT", "布局不符合约束");
    } catch (RuntimeException e) { return unavailable(); }
  }
  private ResponseEntity<?> invalid() { return problem(404, "DELIVERY_PREVIEW_INVALID", "预览令牌无效或已过期"); }
  private ResponseEntity<?> unavailable() { return problem(503, "DELIVERY_PREVIEW_UNAVAILABLE", "预览服务暂时不可用"); }
  private ResponseEntity<?> problem(int status, String code, String detail) {
    return response(status, Map.of("type", "about:blank", "status", status, "code", code, "title", detail, "detail", detail));
  }
  private ResponseEntity<?> response(int status, Object body) {
    var builder = ResponseEntity.status(status).header("Cache-Control", "no-store")
        .header("CDN-Cache-Control", "no-store").header("X-Robots-Tag", "noindex, nofollow")
        .contentType(status >= 400 ? MediaType.APPLICATION_PROBLEM_JSON : MediaType.APPLICATION_JSON);
    if (status == 503) builder.header("Retry-After", "120");
    return builder.body(body);
  }
}
