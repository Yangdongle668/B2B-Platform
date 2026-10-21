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
public abstract class SecretCipherContract {
 protected abstract SecretCipher api();
 protected abstract SecretCipher oldKey();
 @Test public void authenticatedRandomizedRoundTripAndTamperRejection() {
  var value=Secret.of("sensitive-fixture-value");String first=api().encrypt(value),second=api().encrypt(value);
  assertThat(first).isNotEqualTo(second).doesNotContain(value.value());assertThat(api().isEnvelope(first)).isTrue();assertThat(api().decrypt(first).value()).isEqualTo(value.value());assertThat(api().needsReEncryption(first)).isFalse();
  for(String bad:List.of("plain text",first.substring(0,first.length()-4)+"AAAA"))assertThatThrownBy(()->api().decrypt(bad)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(CoreErrors.SECRET_DECRYPT_FAILED));
 }
 @Test public void oldKeyIsReadableButMarkedForRotationAndMaskIsBounded() {
  String old=oldKey().encrypt(Secret.of("rotate-me"));assertThat(api().needsReEncryption(old)).isTrue();assertThat(api().decrypt(old).value()).isEqualTo("rotate-me");
  assertThat(api().mask(Secret.of("short"))).isEqualTo("****");assertThat(api().mask(Secret.of("long-password"))).isEqualTo("****word");
 }

}
