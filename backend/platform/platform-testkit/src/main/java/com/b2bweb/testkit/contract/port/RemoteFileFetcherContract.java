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
public abstract class RemoteFileFetcherContract {
 protected abstract RemoteFileFetcher api();
 protected abstract void seed(URI url,String contentType,byte[] bytes,String resolvedIp);
 final URI url=URI.create("https://assets.example.test/photo.jpg");
 @Test public void enforcesHostIpSizeAndClose() throws Exception {
  seed(url,"image/jpeg",new byte[]{1,2},"8.8.8.8");var request=RemoteFetchRequest.image(url,Set.of("*.example.test"));
  var file=api().fetch(request);try(var in=file.open()){assertThat(in.readAllBytes()).containsExactly(1,2);}file.close();file.close();assertThatThrownBy(file::open).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->api().fetch(RemoteFetchRequest.image(url,Set.of("other.example.test")))).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(InfraErrors.FETCH_HOST_NOT_ALLOWED));
  assertThatThrownBy(()->api().fetch(new RemoteFetchRequest(url,Set.of("assets.example.test"),1,Duration.ofSeconds(1),0,Set.of("image/*")))).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(InfraErrors.FETCH_TOO_LARGE));
  seed(url,"image/jpeg",new byte[]{1,2},"169.254.169.254");assertThatThrownBy(()->api().fetch(request)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(InfraErrors.FETCH_ADDRESS_FORBIDDEN));
 }
 @Test public void transactionsAreRejectedBeforeFetching() {
  org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
  try{assertThatThrownBy(()->api().fetch(RemoteFetchRequest.image(url,Set.of("assets.example.test")))).isInstanceOf(IllegalStateException.class);}
  finally{org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);}
 }

}
