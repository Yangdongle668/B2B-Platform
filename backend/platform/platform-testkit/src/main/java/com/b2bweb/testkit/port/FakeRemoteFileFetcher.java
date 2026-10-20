package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.net.URI;
import java.util.*;

public final class FakeRemoteFileFetcher implements RemoteFileFetcher {
  private record Fixture(byte[] bytes, String contentType, String ip) {}

  private final Map<URI, Fixture> fixtures = new HashMap<>();

  public void seed(URI url, String contentType, byte[] value, String resolvedIp) {
    fixtures.put(url, new Fixture(value.clone(), contentType, resolvedIp));
  }

  public FetchedFile fetch(RemoteFetchRequest request) {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) throw new IllegalStateException("远程下载不能在事务中执行");
    URI url = request.url();
    String host = url.getHost();
    if (host == null
        || url.getUserInfo() != null
        || !Set.of("http", "https").contains(url.getScheme())
        || request.allowedHosts().stream()
            .noneMatch(
                allowed ->
                    host.equals(allowed)
                        || allowed.startsWith("*.") && host.endsWith(allowed.substring(1))))
      throw new BusinessException(InfraErrors.FETCH_HOST_NOT_ALLOWED, "下载域名不在白名单");
    var fixture = fixtures.get(url);
    if (fixture == null)
      throw new ExternalServiceException(InfraErrors.FETCH_HTTP_ERROR, "未配置远程文件夹具", false, null);
    if (!PublicAddresses.isPublicLiteral(fixture.ip()))
      throw new BusinessException(InfraErrors.FETCH_ADDRESS_FORBIDDEN, "下载地址不允许");
    if (fixture.bytes().length > request.maxBytes())
      throw new BusinessException(InfraErrors.FETCH_TOO_LARGE, "文件超过上限");
    if (!request.allowedContentTypes().isEmpty()
        && request.allowedContentTypes().stream()
            .noneMatch(
                t ->
                    t.equals(fixture.contentType())
                        || t.endsWith("/*")
                            && fixture.contentType().startsWith(t.substring(0, t.length() - 1))))
      throw new BusinessException(InfraErrors.FETCH_TYPE_NOT_ALLOWED, "文件类型不允许");
    return new FetchedFile() {
      boolean closed;

      public URI finalUrl() {
        return url;
      }

      public String contentType() {
        return fixture.contentType();
      }

      public long size() {
        return fixture.bytes().length;
      }

      public String fileName() {
        String path = url.getPath();
        return path == null || path.endsWith("/")
            ? null
            : path.substring(path.lastIndexOf('/') + 1);
      }

      public InputStream open() {
        if (closed) throw new IllegalStateException("文件已关闭");
        return new ByteArrayInputStream(fixture.bytes());
      }

      public void close() {
        closed = true;
      }
    };
  }
}
