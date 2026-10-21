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
public abstract class TransactionMutexApiContract {
 protected abstract TransactionMutexApi api();
 protected abstract void site(long id);
 protected abstract void transaction(Runnable action,boolean rollback);
 @Test public void requiresTransactionAndRollsBackGeneration() {
  site(1);assertThatThrownBy(()->api().lock("content.route_tree")).isInstanceOf(IllegalStateException.class);
  transaction(()->assertThat(api().advance("content.route_tree")).isEqualTo(1),true);
  transaction(()->{assertThat(api().lock("content.route_tree")).isZero();assertThat(api().advance("content.route_tree")).isEqualTo(1);},false);
  site(2);transaction(()->assertThat(api().lock("content.route_tree")).isZero(),false);
 }

}
