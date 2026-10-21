package com.b2bweb.testkit.contract.common;
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
public abstract class ProjectionChangeApiContract {
 protected abstract ProjectionChangeApi api();
 protected abstract void site(long id);
 protected abstract void transaction(Runnable action,boolean rollback);
 protected abstract int capturedCount();
 protected abstract Set<Long> capturedIds();
 @Test public void sourceTransactionCoalescesAndRollbackHasNoCapture() {
  site(1);assertThatThrownBy(()->api().searchChanged(Set.of("zh-CN"),Set.of(1L),false)).isInstanceOf(IllegalStateException.class);
  transaction(()->api().searchChanged(Set.of("zh-CN"),Set.of(1L),false),true);assertThat(capturedCount()).isZero();
  transaction(()->{api().searchChanged(Set.of("zh-CN"),Set.of(1L),false);api().searchChanged(Set.of("en"),Set.of(1L,2L),false);assertThat(capturedCount()).isZero();},false);
  assertThat(capturedCount()).isEqualTo(1);assertThat(capturedIds()).containsExactlyInAnyOrder(1L,2L);
 }

}
