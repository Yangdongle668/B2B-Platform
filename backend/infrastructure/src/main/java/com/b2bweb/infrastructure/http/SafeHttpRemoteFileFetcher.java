package com.b2bweb.infrastructure.http;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class SafeHttpRemoteFileFetcher implements RemoteFileFetcher {
  private final Path directory;
  private final SsrfGuard guard = new SsrfGuard();
  private final PinnedHttp http = new PinnedHttp();

  public SafeHttpRemoteFileFetcher(Path directory) {
    this.directory = directory;
  }

  public FetchedFile fetch(RemoteFetchRequest request) {
    RemoteCalls.outsideTransaction();
    if (request.maxBytes() < 1
        || request.timeout().isNegative()
        || request.timeout().isZero()
        || request.maxRedirects() < 0
        || request.maxRedirects() > 10) throw new IllegalArgumentException("下载限制无效");
    long started = System.nanoTime();
    URI current = request.url();
    try {
      for (int redirects = 0; ; redirects++) {
        var target =
            guard.validate(
                current,
                request.allowedHosts(),
                false,
                Duration.ofNanos(
                    Math.max(1, request.timeout().toNanos() - (System.nanoTime() - started))));
        long left = request.timeout().toNanos() - (System.nanoTime() - started);
        if (left <= 0) throw new java.net.SocketTimeoutException();
        record Result(URI redirect, FetchedFile file) {}
        final URI finalUri = current;
        Result result =
            http.exchange(
                target,
                "GET",
                Map.of("User-Agent", "B2BWeb-RemoteFetcher/1", "Accept-Encoding", "identity"),
                null,
                Duration.ofNanos(left),
                (status, headers, in) -> {
                  if (Set.of(301, 302, 303, 307, 308).contains(status)) {
                    if (headers.get("Location") == null) throw failure(status);
                    return new Result(finalUri.resolve(headers.get("Location")), null);
                  }
                  if (status < 200 || status >= 300) throw failure(status);
                  String contentType = headers.get("Content-Type");
                  String mime =
                      contentType == null
                          ? ""
                          : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
                  if (!request.allowedContentTypes().isEmpty()
                      && request.allowedContentTypes().stream()
                          .noneMatch(
                              pattern ->
                                  pattern.equals(mime)
                                      || pattern.endsWith("/*")
                                          && mime.startsWith(
                                              pattern.substring(0, pattern.length() - 1))))
                    throw new BusinessException(InfraErrors.FETCH_TYPE_NOT_ALLOWED, "远程文件类型不允许");
                  String length = headers.get("Content-Length");
                  if (length != null && Long.parseLong(length) > request.maxBytes())
                    throw tooLarge();
                  Files.createDirectories(directory);
                  Path path = Files.createTempFile(directory, "fetch-", ".part");
                  long count = 0;
                  boolean success = false;
                  try (OutputStream out = Files.newOutputStream(path)) {
                    byte[] buffer = new byte[8192];
                    for (int n; (n = in.read(buffer)) != -1; ) {
                      if (n > request.maxBytes() - count) throw tooLarge();
                      if (System.nanoTime() - started > request.timeout().toNanos())
                        throw new SocketTimeoutException();
                      out.write(buffer, 0, n);
                      count += n;
                    }
                    success = true;
                  } finally {
                    if (!success) Files.deleteIfExists(path);
                  }
                  long size = count;
                  String name = fileName(headers.get("Content-Disposition"), finalUri);
                  return new Result(
                      null,
                      new FetchedFile() {
                        private boolean closed;

                        public URI finalUrl() {
                          return finalUri;
                        }

                        public String contentType() {
                          return contentType;
                        }

                        public long size() {
                          return size;
                        }

                        public String fileName() {
                          return name;
                        }

                        public InputStream open() {
                          if (closed) throw new IllegalStateException("下载文件已关闭");
                          try {
                            return Files.newInputStream(path);
                          } catch (IOException e) {
                            throw failure(503);
                          }
                        }

                        public void close() {
                          try {
                            Files.deleteIfExists(path);
                            closed = true;
                          } catch (IOException e) {
                            throw failure(503);
                          }
                        }
                      });
                });
        if (result.file() != null) return result.file();
        if (redirects >= request.maxRedirects()) throw failure(400);
        current = result.redirect();
      }
    } catch (SsrfGuard.HostNotAllowed e) {
      throw new BusinessException(InfraErrors.FETCH_HOST_NOT_ALLOWED, "远程主机不在白名单中");
    } catch (SsrfGuard.AddressForbidden e) {
      throw new BusinessException(InfraErrors.FETCH_ADDRESS_FORBIDDEN, "远程主机地址不允许");
    } catch (IllegalArgumentException e) {
      throw new BusinessException(InfraErrors.FETCH_ADDRESS_FORBIDDEN, "远程地址格式无效");
    } catch (SocketTimeoutException e) {
      throw new ExternalServiceException(InfraErrors.FETCH_TIMEOUT, "远程下载超时", true, null);
    } catch (IOException e) {
      if (System.nanoTime() - started >= request.timeout().toNanos())
        throw new ExternalServiceException(InfraErrors.FETCH_TIMEOUT, "远程下载超时", true, null);
      throw new ExternalServiceException(InfraErrors.FETCH_HTTP_ERROR, "远程下载失败", true, null);
    }
  }

  static String fileName(String disposition, URI uri) {
    String name = null;
    if (disposition != null) {
      var m =
          java.util.regex.Pattern.compile("(?i)(?:^|;)\\s*filename=\"?([^;\"]+)")
              .matcher(disposition);
      if (m.find()) name = m.group(1);
    }
    if (name == null) {
      String path = uri.getPath();
      if (path != null && !path.endsWith("/")) name = path.substring(path.lastIndexOf('/') + 1);
    }
    if (name == null) return null;
    name = name.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
    return name.isBlank() ? null : name.substring(0, Math.min(255, name.length()));
  }

  private static BusinessException tooLarge() {
    return new BusinessException(InfraErrors.FETCH_TOO_LARGE, "远程文件超过大小限制");
  }

  private static ExternalServiceException failure(int status) {
    return new ExternalServiceException(
        InfraErrors.FETCH_HTTP_ERROR, "远程下载失败", status >= 500 || status == 429, null);
  }
}
