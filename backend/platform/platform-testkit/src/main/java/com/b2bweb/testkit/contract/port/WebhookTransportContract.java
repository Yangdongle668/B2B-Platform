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
public abstract class WebhookTransportContract {
 protected abstract WebhookTransport api();
 protected abstract void seed(URI url,String ip,int status);
 protected abstract Map<String,String> receivedHeaders();
 protected abstract String receivedBody();
 @Test public void exactHmacAndNonSuccessResponse() throws Exception {
  URI url=URI.create("https://hooks.example.test/events");seed(url,"8.8.8.8",503);String body="{\"untrusted\":true}";String secret="contract-fixture";
  var result=api().post(new WebhookDelivery(url,"test.created","123",body,Secret.of(secret),null));assertThat(result.status()).isEqualTo(503);assertThat(receivedBody()).isEqualTo(body);
  var headers=receivedHeaders();var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
  String expected="v1="+HexFormat.of().formatHex(mac.doFinal((headers.get("X-B2B-Timestamp")+"."+body).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  assertThat(headers).containsEntry("X-B2B-Signature",expected).containsEntry("X-B2B-Event","test.created").containsEntry("X-B2B-Delivery","123");
 }
 @Test public void privateDestinationIsRejected() {
  URI url=URI.create("https://hooks.example.test/events");seed(url,"127.0.0.1",200);
  assertThatThrownBy(()->api().post(new WebhookDelivery(url,"test.created","1","{}",Secret.of("fixture"),null))).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(InfraErrors.WEBHOOK_URL_FORBIDDEN));
 }

}
