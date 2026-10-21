package com.b2bweb.core;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.core.observability.ErrorReporting;
import io.sentry.SentryEvent;
import io.sentry.protocol.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ErrorReportingTest {
  @Test
  void removesRequestPiiAndExceptionMessagesBeforeSending() {
    var event = new SentryEvent();
    var request = new Request();
    request.setData("private request body");
    request.setHeaders(Map.of("Authorization", "Bearer private-token"));
    event.setRequest(request);
    var user = new User();
    user.setEmail("person@example.test");
    event.setUser(user);
    event.setExtra("credential", "private-value");
    var error = new SentryException();
    error.setValue("private exception text");
    event.setExceptions(new ArrayList<>(List.of(error)));
    ErrorReporting.sanitize(event);
    assertThat(event.getRequest()).isNull();
    assertThat(event.getUser()).isNull();
    assertThat(event.getExtras()).isEmpty();
    assertThat(event.getExceptions())
        .allSatisfy(e -> assertThat(e.getValue()).isEqualTo("服务器内部错误"));
  }
}
