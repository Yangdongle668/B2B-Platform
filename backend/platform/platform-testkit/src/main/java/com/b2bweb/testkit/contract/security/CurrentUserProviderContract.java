package com.b2bweb.testkit.contract.security;
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
public abstract class CurrentUserProviderContract {
 protected abstract CurrentUserProvider api();
 protected abstract void current(CurrentUser user);
 @Test public void anonymousAndSystemActorAreDistinctFromAuthenticatedUser() {
  current(null);assertThat(api().current()).isEmpty();assertThat(api().actor()).isEqualTo(ActorRef.SYSTEM);assertThatThrownBy(()->api().require()).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(CoreErrors.NOT_AUTHENTICATED));
  var user=new CurrentUser(17,"测试用户","fixture@example.test",false,Set.of(),true);current(user);
  assertThat(api().current()).contains(user);assertThat(api().require()).isEqualTo(user);assertThat(api().actor().userId()).isEqualTo(17);
  current(null);assertThat(api().current()).isEmpty();assertThat(api().actor()).isEqualTo(ActorRef.SYSTEM);
 }

}
