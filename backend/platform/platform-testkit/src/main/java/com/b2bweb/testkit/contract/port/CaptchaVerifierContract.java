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
public abstract class CaptchaVerifierContract {
 protected abstract CaptchaVerifier api();
 protected abstract void allow(String token, CaptchaResult result);
 @Test public void successIsSingleUseAndUnknownFailsClosed() {
  allow("valid",new CaptchaResult(true,List.of(),"site.example.test","inquiry",Instant.EPOCH));
  var input=new CaptchaVerification("valid",Secret.of("fixture"),null,null);
  var result=api().verify(input);assertThat(result.success()).isTrue();assertThat(result.hostname()).isEqualTo("site.example.test");assertThat(result.action()).isEqualTo("inquiry");
  assertThat(api().verify(input).success()).isFalse();assertThat(api().verify(new CaptchaVerification("unknown",Secret.of("fixture"),null,null)).success()).isFalse();
 }

}
