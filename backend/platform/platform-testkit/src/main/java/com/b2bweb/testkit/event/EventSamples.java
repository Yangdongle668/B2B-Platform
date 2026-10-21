package com.b2bweb.testkit.event;
import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.url.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.form.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.seo.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;
import java.util.*;
import java.time.Instant;
/** Explicit samples: adding a new event requires a new registered constructor here. */
public final class EventSamples {
 private EventSamples() {}
 public static Map<Class<? extends DomainEvent>,DomainEvent> all() {
 var meta=new EventMeta("9007199254740993",Instant.parse("2026-01-01T00:00:00Z"),1001L,"system");
 return Map.ofEntries(
  Map.entry(ContentCreatedEvent.class,new ContentCreatedEvent(meta,9007199254740993L,"SAMPLE","sample",9007199254740993L)),
  Map.entry(ContentDraftChangedEvent.class,new ContentDraftChangedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN",1,false)),
  Map.entry(ContentPathChangedEvent.class,new ContentPathChangedEvent(meta,9007199254740993L,9007199254740993L,"zh-CN","/zh/old","/zh/new",PathChangeReason.values()[0],RetireMode.values()[0])),
  Map.entry(ContentPublishedEvent.class,new ContentPublishedEvent(meta,9007199254740993L,"SAMPLE","zh-CN",9007199254740993L,9007199254740993L,1,"/zh/new","/zh/old",false,false,PublishTrigger.values()[0])),
  Map.entry(ContentPurgedEvent.class,new ContentPurgedEvent(meta,9007199254740993L,"SAMPLE","sample",List.of(9007199254740993L))),
  Map.entry(ContentRestoredEvent.class,new ContentRestoredEvent(meta,9007199254740993L,"SAMPLE")),
  Map.entry(ContentTrashedEvent.class,new ContentTrashedEvent(meta,9007199254740993L,"SAMPLE",List.of(9007199254740993L))),
  Map.entry(ContentUnpublishedEvent.class,new ContentUnpublishedEvent(meta,9007199254740993L,"SAMPLE","zh-CN",9007199254740993L,1,"/zh/old",UnpublishStrategy.values()[0],9007199254740993L,false)),
  Map.entry(FormChangedEvent.class,new FormChangedEvent(meta,9007199254740993L,"sample",ChangeKind.values()[0])),
  Map.entry(FormSubmittedEvent.class,new FormSubmittedEvent(meta,9007199254740993L,9007199254740993L,"sample",FormPurpose.values()[0],"zh-CN",SpamVerdict.values()[0],false)),
  Map.entry(InquiryAssignedEvent.class,new InquiryAssignedEvent(meta,9007199254740993L,"sample",9007199254740993L,9007199254740993L,AssignmentMethod.values()[0])),
  Map.entry(InquiryCreatedEvent.class,new InquiryCreatedEvent(meta,9007199254740993L,"sample",InquiryType.values()[0],9007199254740993L,"zh-CN",SpamVerdict.values()[0],9007199254740993L,false)),
  Map.entry(InquiryStatusChangedEvent.class,new InquiryStatusChangedEvent(meta,9007199254740993L,"sample",InquiryStatus.values()[0],InquiryStatus.values()[0],InquiryCloseReason.values()[0])),
  Map.entry(LocalizationCreatedEvent.class,new LocalizationCreatedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN",TranslationMode.values()[0])),
  Map.entry(LocalizationDeletedEvent.class,new LocalizationDeletedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN")),
  Map.entry(MediaDeletedEvent.class,new MediaDeletedEvent(meta,9007199254740993L,MediaKind.values()[0],Visibility.values()[0],UploadSource.values()[0],false)),
  Map.entry(MediaUpdatedEvent.class,new MediaUpdatedEvent(meta,9007199254740993L,MediaKind.values()[0],Visibility.values()[0],MediaStatus.values()[0],Set.of(MediaChange.values()[0]))),
  Map.entry(MediaUploadedEvent.class,new MediaUploadedEvent(meta,9007199254740993L,MediaKind.values()[0],Visibility.values()[0],MediaStatus.values()[0],UploadSource.values()[0])),
  Map.entry(ModuleDisabledEvent.class,new ModuleDisabledEvent(meta,"sample")),
  Map.entry(ModuleEnabledEvent.class,new ModuleEnabledEvent(meta,"sample",false)),
  Map.entry(NavigationChangedEvent.class,new NavigationChangedEvent(meta,9007199254740993L,"sample",ChangeKind.values()[0])),
  Map.entry(RedirectChangedEvent.class,new RedirectChangedEvent(meta,ChangeKind.values()[0],9007199254740993L,"sample",RedirectMatchType.values()[0],1)),
  Map.entry(RelationChangedEvent.class,new RelationChangedEvent(meta,Set.of(9007199254740993L),RelationChangeSource.values()[0])),
  Map.entry(ReviewCompletedEvent.class,new ReviewCompletedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN","sample",ReviewDecision.values()[0],9007199254740993L,9007199254740993L,"sample")),
  Map.entry(ReviewRequestedEvent.class,new ReviewRequestedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN","sample",1,9007199254740993L,"sample")),
  Map.entry(SiteResourceChangedEvent.class,new SiteResourceChangedEvent(meta,"sample","sample",ChangeKind.values()[0])),
  Map.entry(SiteSettingsChangedEvent.class,new SiteSettingsChangedEvent(meta,Set.of(SiteChangeScope.values()[0]),Set.of("sample.value"),List.of(new LocaleChange("en",LocaleChangeType.values()[0])),Set.of("sample.value"))),
  Map.entry(TermChangedEvent.class,new TermChangedEvent(meta,9007199254740993L,"sample",TermChange.values()[0],9007199254740993L,9007199254740993L,9007199254740993L,List.of(9007199254740993L))),
  Map.entry(TranslatableResourceChangedEvent.class,new TranslatableResourceChangedEvent(meta,"sample","sample",ResourceChange.values()[0])),
  Map.entry(TranslationCompletedEvent.class,new TranslationCompletedEvent(meta,9007199254740993L,new TranslationSubject.Content(9007199254740993L,"SAMPLE",9007199254740994L,9007199254740995L,1),null,"en",TranslationState.TRANSLATED,false,false,new TranslationStats(1,0,1,0,10,10),1)),
  Map.entry(TranslationFailedEvent.class,new TranslationFailedEvent(meta,9007199254740993L,new TranslationSubject.Content(9007199254740993L,"SAMPLE",9007199254740994L,9007199254740995L,1),null,"en","sample","sample","sample",1)),
  Map.entry(TranslationModeChangedEvent.class,new TranslationModeChangedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN",TranslationMode.values()[0],TranslationMode.values()[0])),
  Map.entry(TranslationOutdatedEvent.class,new TranslationOutdatedEvent(meta,9007199254740993L,"SAMPLE",9007199254740993L,"zh-CN","sample",1,1)),
  Map.entry(UserDeactivatedEvent.class,new UserDeactivatedEvent(meta,9007199254740993L,UserDeactivationReason.values()[0],Set.of(9007199254740993L))));
 }
}
