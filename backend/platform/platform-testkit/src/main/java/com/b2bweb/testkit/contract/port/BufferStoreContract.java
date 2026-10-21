package com.b2bweb.testkit.contract.port;
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
public abstract class BufferStoreContract {
 protected abstract BufferStore api();
 protected abstract void site(long id);
 protected abstract void advance(Duration elapsed);
 @Test public void earliestDeadlineAndBoundedAtomicDrain() {
  site(1); api().schedule("graph.recompute","42",Duration.ofSeconds(10));api().schedule("graph.recompute","42",Duration.ofSeconds(30));
  assertThat(api().pollDue("graph.recompute",10)).isEmpty(); advance(Duration.ofSeconds(10));
  assertThat(api().pollDue("graph.recompute",1)).containsExactly("42"); assertThat(api().size("graph.recompute")).isZero();
  api().increment("url.not_found","a",2);api().increment("url.not_found","a",3);api().increment("url.not_found","b",4);
  var first=api().drainCounters("url.not_found",1);var second=api().drainCounters("url.not_found",10);
  assertThat(first).hasSize(1);assertThat(second).hasSize(1);var all=new HashMap<>(first);all.putAll(second);assertThat(all).containsEntry("a",5L).containsEntry("b",4L);
  assertThat(api().drainCounters("url.not_found",10)).isEmpty();
 }
 @Test public void countersAndQueuesAreSiteIsolated() {
  site(1);api().increment("url.not_found","a",1);api().schedule("graph.recompute","a",Duration.ZERO);
  site(2);assertThat(api().drainCounters("url.not_found",10)).isEmpty();assertThat(api().pollDue("graph.recompute",10)).isEmpty();
  site(1);assertThat(api().pollDue("graph.recompute",10)).containsExactly("a");assertThat(api().drainCounters("url.not_found",10)).containsEntry("a",1L);
 }

}
