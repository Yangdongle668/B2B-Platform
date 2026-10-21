package com.b2bweb.testkit.contract.event;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class EventMetaFactoryContract {
 protected abstract EventMetaFactory api();
 protected abstract void site(Long id);
 protected abstract Instant now();
 @Test public void exactMetadataAndExplicitSystemScope() {
  site(1L);var first=api().next();assertThat(first.siteId()).isEqualTo(1);assertThat(first.occurredAt()).isBetween(now().minusSeconds(2),now().plusSeconds(2));assertThat(first.eventId()).matches("[0-9]{1,20}");
  var second=api().next(null,ActorRef.SYSTEM);assertThat(second.eventId()).isNotEqualTo(first.eventId());assertThat(second.siteId()).isNull();assertThat(second.actor()).isEqualTo("system");
  assertThat(api().next(2L,ActorRef.user(17,"测试")).actor()).isEqualTo("17");
 }

}
