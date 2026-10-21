package com.b2bweb.infrastructure.http;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.util.Timeout;

/** One request, no retries/redirects/decompression; TLS validates the original hostname. */
final class PinnedHttp {
  interface Reader<T> {
    T read(int status, Map<String, String> headers, InputStream body) throws IOException;
  }

  <T> T exchange(
      SsrfGuard.Target target,
      String method,
      Map<String, String> headers,
      byte[] body,
      Duration timeout,
      Reader<T> reader)
      throws IOException {
    long millis = timeout.toMillis();
    if (millis < 1) throw new IllegalArgumentException("超时必须为正");
    var manager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDnsResolver(
                new DnsResolver() {
                  public InetAddress[] resolve(String host) throws UnknownHostException {
                    if (!host.equalsIgnoreCase(target.uri().getHost()))
                      throw new UnknownHostException();
                    return target.addresses().toArray(InetAddress[]::new);
                  }

                  public String resolveCanonicalHostname(String host) {
                    return host;
                  }
                })
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(millis))
                    .setSocketTimeout(Timeout.ofMilliseconds(millis))
                    .build())
            .build();
    try (var client =
        HttpClients.custom()
            .setConnectionManager(manager)
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .disableContentCompression()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(millis))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(millis))
                    .build())
            .build()) {
      var request = new HttpUriRequestBase(method, target.uri());
      headers.forEach(request::setHeader);
      if (body != null) request.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_JSON));
      // The deadline also bounds trickle responses; socket timeouts alone only bound idle time.
      try (var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()) {
        var deadline =
            scheduler.schedule(
                () -> request.cancel(), millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        try {
          var response = client.executeOpen(null, request, null);
          try {
            Map<String, String> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (var header : response.getHeaders()) out.put(header.getName(), header.getValue());
            InputStream in =
                response.getEntity() == null
                    ? InputStream.nullInputStream()
                    : response.getEntity().getContent();
            return reader.read(response.getCode(), out, in);
          } finally {
            // A one-shot connection must not drain an ignored/hostile response after the reader
            // stops.
            if (response instanceof org.apache.hc.core5.io.ModalCloseable closeable)
              closeable.close(org.apache.hc.core5.io.CloseMode.IMMEDIATE);
            else {
              request.cancel();
              response.close();
            }
          }
        } finally {
          deadline.cancel(false);
          scheduler.shutdownNow();
        }
      }
    }
  }
}
