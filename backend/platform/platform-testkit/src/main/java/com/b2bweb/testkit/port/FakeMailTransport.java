package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class FakeMailTransport implements MailTransport {
  public record Sent(MailMessage message, MailServerConfig server) {}

  private final List<Sent> sent = new CopyOnWriteArrayList<>();
  private final Clock clock;
  private RuntimeException failure;

  public FakeMailTransport(Clock clock) {
    this.clock = clock;
  }

  public void failWith(RuntimeException error) {
    failure = error;
  }

  public MailSendResult send(MailMessage message, MailServerConfig server) {
    if (failure != null) throw failure;
    Set<String> reserved =
        Set.of(
            "from",
            "to",
            "reply-to",
            "subject",
            "message-id",
            "content-type",
            "mime-version",
            "bcc",
            "cc");
    for (var h : message.headers().entrySet())
      if (reserved.contains(h.getKey().toLowerCase(Locale.ROOT))
          || !h.getKey().matches("[A-Za-z0-9-]+")
          || h.getValue().contains("\r")
          || h.getValue().contains("\n")) throw new IllegalArgumentException("邮件头非法");
    if (message.subject().contains("\r")
        || message.subject().contains("\n")
        || message.messageId().contains("\r")
        || message.messageId().contains("\n")) throw new IllegalArgumentException("邮件头非法");
    sent.add(new Sent(message, server));
    return new MailSendResult(message.messageId(), "fake-" + sent.size(), clock.instant());
  }

  public List<Sent> sent() {
    return List.copyOf(sent);
  }

  public void reset() {
    sent.clear();
    failure = null;
  }
}
