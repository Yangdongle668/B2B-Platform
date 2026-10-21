package com.b2bweb.testkit.contract.site;
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
public abstract class SiteContextProviderContract {
 protected abstract SiteContextProvider api();
 protected abstract void site(Long id);
 @Test public void missingContextFailsAndNestedFailureRestoresPreviousSite() {
  site(null);assertThat(api().current()).isEmpty();assertThatThrownBy(()->api().require()).isInstanceOf(NoSiteContextException.class);assertThatThrownBy(()->api().requireSiteId()).isInstanceOf(NoSiteContextException.class);
  site(1L);assertThat(api().callAs(2,()->api().requireSiteId())).isEqualTo(2);assertThat(api().requireSiteId()).isEqualTo(1);
  assertThatThrownBy(()->api().runAs(2,()->{assertThat(api().requireSiteId()).isEqualTo(2);throw new IllegalArgumentException("fixture");})).isInstanceOf(IllegalArgumentException.class);assertThat(api().requireSiteId()).isEqualTo(1);
 }

}
