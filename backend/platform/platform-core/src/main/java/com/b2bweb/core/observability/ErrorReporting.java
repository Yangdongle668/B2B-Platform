package com.b2bweb.core.observability;

import io.sentry.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/** Explicit 5xx-only reporting; no automatic HTTP/request-body instrumentation. */
@Configuration(proxyBeanMethods = false)
public class ErrorReporting {
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "B2BWEB_SENTRY_DSN")
  AutoCloseable sentry(@Value("${B2BWEB_SENTRY_DSN}") String dsn) {
    var uri = java.net.URI.create(dsn);
    if (!"https".equals(uri.getScheme())
        || uri.getHost() == null
        || !uri.getHost().endsWith(".ingest.de.sentry.io"))
      throw new IllegalArgumentException("Sentry必须使用欧盟数据区域");
    Sentry.init(
        options -> {
          options.setDsn(dsn);
          options.setSendDefaultPii(false);
          options.setBeforeSend((event, hint) -> sanitize(event));
        });
    return Sentry::close;
  }

  public static SentryEvent sanitize(SentryEvent event) {
    event.setRequest(null);
    event.setUser(null);
    event.setMessage(null);
    event.setBreadcrumbs(null);
    if (event.getExtras() != null) event.getExtras().clear();
    if (event.getTags() != null) event.getTags().clear();
    event.getContexts().clear();
    if (event.getExceptions() != null)
      for (var error : event.getExceptions()) error.setValue("服务器内部错误");
    String trace = org.slf4j.MDC.get("traceId");
    if (trace != null && trace.matches("[a-f0-9]{32}")) event.setTag("traceId", trace);
    return event;
  }

  public static void capture(Throwable error) {
    if (Sentry.isEnabled()) Sentry.captureException(error);
  }
}
