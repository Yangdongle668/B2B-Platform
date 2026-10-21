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
public abstract class DistributedLockContract {
 protected abstract DistributedLock api();
 protected abstract void advance(Duration elapsed);
 @Test public void staleOwnerCannotExtendOrReleaseSuccessor() {
  String name="contract-"+UUID.randomUUID();var old=api().tryAcquire(name,Duration.ofSeconds(1)).orElseThrow();
  assertThat(api().tryAcquire(name,Duration.ofSeconds(1))).isEmpty();advance(Duration.ofSeconds(2));
  var current=api().tryAcquire(name,Duration.ofSeconds(10)).orElseThrow();
  assertThat(old.extend(Duration.ofSeconds(20))).isFalse();old.close();
  assertThat(api().tryAcquire(name,Duration.ofSeconds(1))).isEmpty();assertThat(current.extend(Duration.ofSeconds(20))).isTrue();current.close();current.close();
  try(var next=api().tryAcquire(name,Duration.ofSeconds(1)).orElseThrow()) { assertThat(next).isNotNull(); }
 }

}
