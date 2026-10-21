package com.b2bweb.delivery.assembly;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.port.Visibility;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.url.*;
import java.util.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/**
 * Request-owned assembly using public read APIs only. Never resolve draft content or private media.
 */
public final class DeliveryReferences {
  public record Result(JsonNode refs, Set<CacheTag> tags) {
    public Result {
      tags = Set.copyOf(tags);
    }
  }

  private final ContentApi contents;
  private final MediaApi media;
  private final TaxonomyApi taxonomy;
  private final UrlApi urls;
  private final JsonMapper json;

  public DeliveryReferences(
      ContentApi contents, MediaApi media, TaxonomyApi taxonomy, UrlApi urls, JsonMapper json) {
    this.contents = contents;
    this.media = media;
    this.taxonomy = taxonomy;
    this.urls = urls;
    this.json = json;
  }

  public Result assemble(long site, String locale, ReferenceSet references) {
    var tags = new HashSet<CacheTag>();
    var out = json.createObjectNode();
    var contentTable = out.putObject("content");
    var mediaTable = out.putObject("media");
    var termTable = out.putObject("terms");
    // Bound every public API batch; the complete page may reference more than 500 objects.
    for (var ids : batches(references.contentIds()))
      for (var item : contents.findPublishedCards(ids, locale).values()) {
        if (!item.locale().equals(locale)) continue;
        String path = item.path();
        var node =
            contentTable
                .putObject(Long.toString(item.contentId()))
                .put("title", item.title())
                .put("contentType", item.contentType());
        if (path != null) {
          node.put("path", path);
          node.put("url", urls.absoluteUrl(path));
        }
        tags.add(CacheTag.content(site, item.contentId()));
      }
    for (var ids : batches(references.mediaIds())) {
      var views = media.views(ids, locale);
      Map<ImagePreset, Map<Long, ImageSources>> sources = new EnumMap<>(ImagePreset.class);
      for (var preset : ImagePreset.values())
        sources.put(preset, media.imageSources(ids, preset, locale));
      for (var item : views.values()) {
        if (item.visibility() != Visibility.PUBLIC
            || item.status() != MediaStatus.READY
            || !locale.equals(item.locale())) continue;
        var node =
            mediaTable
                .putObject(Long.toString(item.mediaId()))
                .put("visibility", "PUBLIC")
                .put("fileName", item.fileName())
                .put("mime", item.mime())
                .put("size", item.size());
        if (item.alt() != null) node.put("alt", item.alt());
        if (item.width() != null) node.put("width", item.width());
        if (item.height() != null) node.put("height", item.height());
        if (item.publicUrl() != null) node.put("url", item.publicUrl().toString());
        var image = node.putObject("imageSources");
        for (var preset : ImagePreset.values()) {
          var source = sources.get(preset).get(item.mediaId());
          if (source != null && source.mediaId() == item.mediaId() && source.preset() == preset)
            image.set(preset.name(), json.valueToTree(source));
        }
        tags.add(CacheTag.media(site, item.mediaId()));
      }
    }
    for (var ids : batches(references.termIds()))
      for (var item : taxonomy.termViews(ids, locale).values()) {
        if (item.name() == null || !locale.equals(item.locale())) continue;
        var node =
            termTable
                .putObject(Long.toString(item.termId()))
                .put("name", item.name())
                .put("vocabularyKey", item.vocabularyKey());
        if (item.landingContentId() != null) {
          var cards = contents.findPublishedCards(List.of(item.landingContentId()), locale);
          var card = cards.get(item.landingContentId());
          if (card != null && card.path() != null) {
            node.put("path", card.path());
            node.put("url", urls.absoluteUrl(card.path()));
            tags.add(CacheTag.content(site, card.contentId()));
          }
        }
        tags.add(CacheTag.term(site, item.termId()));
      }
    return new Result(out, tags);
  }

  private static List<List<Long>> batches(Set<Long> values) {
    var sorted = values.stream().sorted().toList();
    var result = new ArrayList<List<Long>>();
    for (int i = 0; i < sorted.size(); i += 500)
      result.add(sorted.subList(i, Math.min(i + 500, sorted.size())));
    return result;
  }
}
