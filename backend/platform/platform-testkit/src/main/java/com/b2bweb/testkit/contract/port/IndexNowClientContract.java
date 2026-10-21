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
public abstract class IndexNowClientContract {
 protected abstract IndexNowClient api();
 protected abstract void status(int value);
 protected IndexNowSubmission submission() { return new IndexNowSubmission("site.example.test","fixture-key",URI.create("https://site.example.test/key.txt"),List.of(URI.create("https://site.example.test/zh/a"))); }
 @Test public void acceptedAndPermanentErrorsAreResultsButTransientErrorsRetry() {
  for(int value:List.of(200,202,400,403,422)){status(value);assertThat(api().submit(submission()).status()).isEqualTo(value);}
  for(int value:List.of(429,503)){status(value);assertThatThrownBy(()->api().submit(submission())).isInstanceOfSatisfying(ExternalServiceException.class,e->{assertThat(e.retryable()).isTrue();assertThat(e.errorCode()).isEqualTo(InfraErrors.INDEXNOW_FAILED);});}
 }

}
