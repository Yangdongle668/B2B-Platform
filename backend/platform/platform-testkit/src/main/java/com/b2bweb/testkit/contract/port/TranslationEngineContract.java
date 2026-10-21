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
public abstract class TranslationEngineContract {
 protected abstract TranslationEngine api();
 protected abstract EngineConfig config();
 protected abstract void output(String id,String text);
 protected TranslationRequest request() {return new TranslationRequest(config(),"Fixed rules only","zh-CN","en",List.of(new EngineItem("u1","功率 <1>100</1> W",null,null),new EngineItem("u2","型号 A-123",null,null)),"content");}
 @Test public void exactIdsOrderAndUsage() {
  output("u1","Power <1>100</1> W");output("u2","Model A-123");var result=api().translate(request());
  assertThat(result.items()).containsExactly(new EngineTranslation("u1","Power <1>100</1> W"),new EngineTranslation("u2","Model A-123"));
  assertThat(result.usage().inputTokens()).isNotNegative();assertThat(result.usage().outputTokens()).isNotNegative();assertThat(result.model()).isNotBlank();assertThat(result.attempts()).isBetween(1,4);
 }
 @Test public void activeTransactionIsRejected() {
  org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
  try{assertThatThrownBy(()->api().translate(request())).isInstanceOf(IllegalStateException.class);}
  finally{org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);}
 }

}
