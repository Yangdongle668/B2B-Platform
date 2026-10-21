package com.b2bweb.delivery.cache;
import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/** Pure repeatable side effects: completion is recorded only after both providers confirm. */
@Component
public class CacheInvalidationListener {
  final ObjectProvider<TaggedCache> caches;
  final ObjectProvider<CdnPurger> cdns;
  public CacheInvalidationListener(ObjectProvider<TaggedCache> caches,ObjectProvider<CdnPurger> cdns) { this.caches=caches; this.cdns=cdns; }
  @ApplicationModuleListener(propagation=Propagation.NOT_SUPPORTED)
  public void changed(DomainEvent event) {
    Set<CacheTag> tags=tags(event); if(tags.isEmpty()) return;
    var cache=caches.getIfAvailable(); var cdn=cdns.getIfAvailable();
    if(cache==null || cdn==null) throw new IllegalStateException("缓存失效提供方尚未就绪");
    cache.invalidate(tags); cdn.purgeTags(tags);
  }
  public static Set<CacheTag> tags(DomainEvent event) {
    if(event.siteId()==null) return Set.of(); long site=event.siteId();
    return switch(event) {
      case ContentPublishedEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case ContentUnpublishedEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case ContentTrashedEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case ContentRestoredEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case ContentPurgedEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case LocalizationDeletedEvent e -> Set.of(CacheTag.content(site,e.contentId()),CacheTag.list(site,e.contentType()));
      case ContentPathChangedEvent e -> Set.of(CacheTag.site(site));
      case RedirectChangedEvent e -> Set.of(CacheTag.site(site));
      case MediaUploadedEvent e -> Set.of(CacheTag.media(site,e.mediaId()));
      case MediaUpdatedEvent e -> Set.of(CacheTag.media(site,e.mediaId()));
      case MediaDeletedEvent e -> Set.of(CacheTag.media(site,e.mediaId()));
      case TermChangedEvent e -> Set.of(CacheTag.site(site));
      case NavigationChangedEvent e -> Set.of(CacheTag.nav(site));
      case FormChangedEvent e -> Set.of(CacheTag.form(site,e.formId()));
      case RelationChangedEvent e -> {
        if(e.contentIds().size()>CacheTag.MAX_TAGS_PER_RESPONSE) yield Set.of(CacheTag.site(site));
        Set<CacheTag> graph=new HashSet<>(); e.contentIds().forEach(id -> graph.add(CacheTag.graph(site,id))); yield Set.copyOf(graph);
      }
      case SiteSettingsChangedEvent e -> Set.of(CacheTag.site(site));
      case SiteResourceChangedEvent e -> Set.of(CacheTag.site(site));
      case ModuleEnabledEvent e -> Set.of(CacheTag.site(site));
      case ModuleDisabledEvent e -> Set.of(CacheTag.site(site));
      case TranslationCompletedEvent e -> e.resource()!=null ? Set.of(CacheTag.site(site)) : Set.of();
      default -> Set.of();
    };
  }
}
