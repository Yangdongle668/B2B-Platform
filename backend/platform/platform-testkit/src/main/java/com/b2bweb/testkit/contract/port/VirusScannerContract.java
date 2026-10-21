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
public abstract class VirusScannerContract {
 protected abstract VirusScanner api();
 protected abstract void available(boolean value);
 @Test public void scansContentAcrossReadBoundariesAndPreservesCallerStream() {
  byte[] value=("a".repeat(8180)+"EICAR-STANDARD-ANTIVIRUS-TEST-FILE").getBytes(java.nio.charset.StandardCharsets.UTF_8);
  var input=new ByteArrayInputStream(value){public void close(){throw new AssertionError("caller owns input");}};
  assertThat(api().scan(input,value.length,"test.txt").verdict()).isEqualTo(ScanVerdict.INFECTED);
  assertThat(api().scan(new ByteArrayInputStream(new byte[]{1,2}),2,"ordinary.txt").verdict()).isEqualTo(ScanVerdict.CLEAN);
 }
 @Test public void outageIsRetryableAndNeverClean() {
  available(false);assertThat(api().available()).isFalse();
  assertThatThrownBy(()->api().scan(new ByteArrayInputStream(new byte[]{1}),1,"a.txt")).isInstanceOfSatisfying(ExternalServiceException.class,e->{assertThat(e.errorCode()).isEqualTo(InfraErrors.VIRUS_SCANNER_UNAVAILABLE);assertThat(e.retryable()).isTrue();});
 }

}
