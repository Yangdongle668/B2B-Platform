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
public abstract class MxRecordCheckerContract {
 protected abstract MxRecordChecker api();
 protected abstract void seed(String domain,MxStatus status,List<String> hosts);
 @Test public void unknownIsAResultAndKnownHostsArePreserved() {
  assertThat(api().check("unavailable.example.test").status()).isEqualTo(MxStatus.UNKNOWN);
  seed("example.test",MxStatus.HAS_MX,List.of("mail.example.test"));
  var result=api().check("example.test");assertThat(result.status()).isEqualTo(MxStatus.HAS_MX);assertThat(result.checkedAt()).isNotNull();
 }

}
