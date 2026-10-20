package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class InMemoryObjectStorage implements ObjectStorage {
  private record Entry(byte[] bytes, StoredObjectInfo info) {}

  private final Map<String, Entry> rows = new ConcurrentHashMap<>();
  private final Clock clock;

  public InMemoryObjectStorage(Clock clock) {
    this.clock = clock;
  }

  private String key(StorageKey key, Visibility visibility) {
    return visibility + ":" + key.value();
  }

  public void put(
      StorageKey key, InputStream input, long size, String contentType, Visibility visibility) {
    if (size < 0 || size > Integer.MAX_VALUE) throw new IllegalArgumentException("内存存储长度超限");
    try {
      var output = new ByteArrayOutputStream();
      long total = 0;
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        total += read;
        if (total > size) throw new IllegalArgumentException("对象长度不匹配");
        output.write(buffer, 0, read);
      }
      if (total != size) throw new IllegalArgumentException("对象长度不匹配");
      byte[] bytes = output.toByteArray();
      String etag =
          HexFormat.of()
              .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
      rows.put(
          key(key, visibility),
          new Entry(
              bytes,
              new StoredObjectInfo(key, visibility, size, contentType, etag, clock.instant())));
    } catch (IOException e) {
      throw new ExternalServiceException(InfraErrors.STORAGE_UNAVAILABLE, "读取测试流失败", true, e);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  public InputStream get(StorageKey key, Visibility visibility) {
    var row = rows.get(key(key, visibility));
    if (row == null) throw new NotFoundException(InfraErrors.STORAGE_OBJECT_NOT_FOUND, "对象不存在");
    return new ByteArrayInputStream(row.bytes());
  }

  public Optional<StoredObjectInfo> stat(StorageKey key, Visibility visibility) {
    return Optional.ofNullable(rows.get(key(key, visibility))).map(Entry::info);
  }

  public void delete(StorageKey key, Visibility visibility) {
    rows.remove(key(key, visibility));
  }

  public URI publicUrl(StorageKey key) {
    return URI.create("https://public.storage.example.test/" + key.value());
  }

  public URI presignedGet(StorageKey key, Duration ttl, String name) {
    if (ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofDays(7)) > 0)
      throw new IllegalArgumentException("预签名有效期超限");
    if (name != null && (name.contains("\r") || name.contains("\n")))
      throw new IllegalArgumentException("文件名非法");
    return URI.create(
        "https://private.storage.example.test/"
            + key.value()
            + "?expires="
            + clock.instant().plus(ttl).getEpochSecond()
            + (name == null
                ? ""
                : "&response-content-disposition="
                    + URLEncoder.encode(
                        "attachment; filename*=UTF-8''"
                            + URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20"),
                        java.nio.charset.StandardCharsets.UTF_8)));
  }

  public void reset() {
    rows.clear();
  }
}
