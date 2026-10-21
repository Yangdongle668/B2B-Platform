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
public abstract class ReferenceWriteApiContract {
 protected abstract ReferenceWriteApi api();
 protected abstract void transaction(Runnable action,boolean rollback);
 protected abstract long readyMedia();
 protected abstract int referenceCount(long media);
 @Test public void replacesWithinSourceTransactionAndRollbackPreservesPreviousSet() {
  long id=readyMedia();var owner=new MediaReferenceOwner("CONTENT",1,"zh-CN",ReferenceStage.PUBLISHED);var refs=List.of(new MediaReference(id,"sample","documents[0]"));
  assertThatThrownBy(()->api().replaceMedia(owner,refs)).isInstanceOf(IllegalStateException.class);
  transaction(()->api().replaceMedia(owner,refs),false);assertThat(referenceCount(id)).isEqualTo(1);
  transaction(()->api().replaceMedia(owner,List.of()),true);assertThat(referenceCount(id)).isEqualTo(1);
  transaction(()->api().replaceMedia(owner,List.of()),false);assertThat(referenceCount(id)).isZero();
 }

}
