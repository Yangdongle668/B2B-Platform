package com.b2bweb.delivery.assembly;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.ContentApi;
import com.b2bweb.api.media.MediaApi;
import com.b2bweb.api.taxonomy.TaxonomyApi;
import com.b2bweb.api.url.UrlApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class DeliveryAssembly {
  private final ObjectProvider<ContentApi> content;
  private final ObjectProvider<MediaApi> media;
  private final ObjectProvider<TaxonomyApi> taxonomy;
  private final ObjectProvider<UrlApi> urls;
  private final JsonMapper json;

  public DeliveryAssembly(
      ObjectProvider<ContentApi> content,
      ObjectProvider<MediaApi> media,
      ObjectProvider<TaxonomyApi> taxonomy,
      ObjectProvider<UrlApi> urls,
      JsonMapper json) {
    this.content = content;
    this.media = media;
    this.taxonomy = taxonomy;
    this.urls = urls;
    this.json = json;
  }

  public DeliveryReferences.Result references(long site, String locale, JsonNode snapshot) {
    var refs = ReferenceSet.scan(snapshot);
    if (refs.isEmpty())
      return new DeliveryReferences.Result(
          json.readTree("{\"content\":{},\"media\":{},\"terms\":{}}"), java.util.Set.of());
    if (content.getIfAvailable() == null
        || media.getIfAvailable() == null
        || taxonomy.getIfAvailable() == null
        || urls.getIfAvailable() == null) throw new IllegalStateException("引用提供方尚未就绪");
    return new DeliveryReferences(
            content.getObject(), media.getObject(), taxonomy.getObject(), urls.getObject(), json)
        .assemble(site, locale, refs);
  }
}
