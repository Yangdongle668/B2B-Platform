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
public abstract class MailTransportContract {
 protected abstract MailTransport api();
 protected abstract MailMessage received();
 protected MailMessage message() {return new MailMessage("<contract@example.test>",new MailAddress("sender@example.test","平台"),List.of(new MailAddress("reader@example.test","读者")),new MailAddress("reply@example.test",null),"测试邮件","<p>正文 &amp; 测试</p>","正文 & 测试",Map.of("X-B2B-Trace","contract"));}
 @Test public void recipientReplyToAndBodiesAreExact() {
  var m=message();var result=api().send(m,null);assertThat(result.messageId()).isEqualTo(m.messageId());assertThat(result.acceptedAt()).isNotNull();
  assertThat(received()).isEqualTo(m);
 }
 @Test public void reservedHeadersCannotOverrideEnvelope() {
  var m=message();var injected=new MailMessage(m.messageId(),m.from(),m.to(),m.replyTo(),m.subject(),m.htmlBody(),m.textBody(),Map.of("Subject","override"));
  assertThatThrownBy(()->api().send(injected,null)).isInstanceOf(IllegalArgumentException.class);
 }

}
