package com.b2bweb.infrastructure.mail;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.eclipse.angus.mail.smtp.*;

/** Per-send immutable SMTP settings; no global Jakarta Mail session or credential mutation. */
public final class SmtpMailTransport implements MailTransport {
  private final MailServerConfig defaults;
  private final Clock clock;
  private final boolean allowPlaintext;

  public SmtpMailTransport(MailServerConfig defaults, Clock clock, boolean allowPlaintext) {
    this.defaults = defaults;
    this.clock = clock;
    this.allowPlaintext = allowPlaintext;
  }

  private static String header(String value) {
    if (value.contains("\r") || value.contains("\n"))
      throw new IllegalArgumentException("邮件头含非法换行");
    return value;
  }

  private static InternetAddress address(MailAddress value) throws Exception {
    var address =
        new InternetAddress(
            header(value.email()),
            value.name() == null ? null : header(value.name()),
            StandardCharsets.UTF_8.name());
    address.validate();
    return address;
  }

  public MailSendResult send(MailMessage input, MailServerConfig override) {
    var server = override == null ? defaults : override;
    if (server.security() == SmtpSecurity.NONE && !allowPlaintext)
      throw new IllegalStateException("正式环境禁止明文 SMTP");
    Properties props = new Properties();
    props.setProperty("mail.smtp.connectiontimeout", "30000");
    props.setProperty("mail.smtp.timeout", "30000");
    props.setProperty("mail.smtp.writetimeout", "30000");
    props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
    props.setProperty(
        "mail.smtp.ssl.enable", String.valueOf(server.security() == SmtpSecurity.TLS));
    props.setProperty(
        "mail.smtp.starttls.enable", String.valueOf(server.security() == SmtpSecurity.STARTTLS));
    props.setProperty(
        "mail.smtp.starttls.required", String.valueOf(server.security() == SmtpSecurity.STARTTLS));
    props.setProperty("mail.smtp.auth", String.valueOf(server.username() != null));
    try {
      var session = Session.getInstance(props);
      var message =
          new MimeMessage(session) {
            @Override
            protected void updateMessageID() throws MessagingException {
              setHeader("Message-ID", header(input.messageId()));
            }
          };
      message.setFrom(address(input.from()));
      for (var to : input.to()) message.addRecipient(Message.RecipientType.TO, address(to));
      if (input.replyTo() != null) message.setReplyTo(new Address[] {address(input.replyTo())});
      message.setSubject(header(input.subject()), "UTF-8");
      for (var item : input.headers().entrySet()) {
        String name = header(item.getKey());
        if (!name.matches("[A-Za-z0-9-]+")
            || Set.of(
                    "from",
                    "to",
                    "reply-to",
                    "subject",
                    "message-id",
                    "content-type",
                    "mime-version",
                    "bcc",
                    "cc")
                .contains(name.toLowerCase(Locale.ROOT)))
          throw new IllegalArgumentException("不可覆盖保留邮件头");
        message.setHeader(name, header(item.getValue()));
      }
      var multipart = new MimeMultipart("alternative");
      var text = new MimeBodyPart();
      text.setText(input.textBody(), "UTF-8");
      multipart.addBodyPart(text);
      var html = new MimeBodyPart();
      html.setContent(input.htmlBody(), "text/html; charset=UTF-8");
      multipart.addBodyPart(html);
      message.setContent(multipart);
      message.saveChanges();
      try (var transport = (SMTPTransport) session.getTransport("smtp")) {
        transport.connect(
            server.host(),
            server.port(),
            server.username(),
            server.password() == null ? null : server.password().value());
        transport.sendMessage(message, message.getAllRecipients());
        return new MailSendResult(
            input.messageId(), transport.getLastServerResponse().strip(), clock.instant());
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      boolean retryable = !(e instanceof AuthenticationFailedException);
      for (Exception cause = e; cause != null; ) {
        if (cause instanceof SMTPSendFailedException smtp && smtp.getReturnCode() >= 500)
          retryable = false;
        if (cause instanceof SMTPAddressFailedException smtp && smtp.getReturnCode() >= 500)
          retryable = false;
        cause = cause instanceof MessagingException mail ? mail.getNextException() : null;
      }
      throw new ExternalServiceException(
          retryable ? InfraErrors.MAIL_TEMPORARY_FAILURE : InfraErrors.MAIL_REJECTED,
          "邮件投递失败",
          retryable,
          e);
    }
  }
}
