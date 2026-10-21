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
public abstract class CdnPurgerContract {
 protected abstract CdnPurger api();
 protected abstract void available(boolean value);
 protected abstract int successfulPurges();
 @Test public void acknowledgedPurgesAndCrossSiteRejection() {
  api().purgeTags(List.of(CacheTag.site(1)));int count=successfulPurges();assertThat(count).isPositive();
  assertThatThrownBy(()->api().purgeTags(List.of(CacheTag.site(1),CacheTag.site(2)))).isInstanceOf(IllegalArgumentException.class);
  available(false);assertThatThrownBy(()->api().purgeTags(List.of(CacheTag.site(1)))).isInstanceOfSatisfying(ExternalServiceException.class,e->{assertThat(e.retryable()).isTrue();assertThat(e.errorCode()).isEqualTo(InfraErrors.CDN_PURGE_FAILED);});
  assertThat(successfulPurges()).isEqualTo(count);
 }
 @Test public void urlBatchCannotExceedFiveHundred() {
  assertThatThrownBy(()->api().purgeUrls(Collections.nCopies(501,URI.create("https://site.example.test/zh/a")))).isInstanceOf(IllegalArgumentException.class);
 }

}
