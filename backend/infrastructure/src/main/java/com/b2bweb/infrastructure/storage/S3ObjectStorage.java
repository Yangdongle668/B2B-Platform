package com.b2bweb.infrastructure.storage;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** AWS SDK v2 adapter. Bucket provisioning and public-read policy belong to deployment. */
public final class S3ObjectStorage implements ObjectStorage {
  private final S3Client client;
  private final S3Presigner signer;
  private final String publicBucket, privateBucket;
  private final URI cdn;

  public S3ObjectStorage(
      S3Client client, S3Presigner signer, String publicBucket, String privateBucket, URI cdn) {
    this.client = client;
    this.signer = signer;
    this.publicBucket = publicBucket;
    this.privateBucket = privateBucket;
    this.cdn = URI.create(cdn.toString().replaceAll("/+$", "") + "/");
    if (publicBucket.equals(privateBucket)) throw new IllegalArgumentException("公开与私有桶必须分离");
  }

  private String bucket(Visibility visibility) {
    return visibility == Visibility.PUBLIC ? publicBucket : privateBucket;
  }

  private ExternalServiceException unavailable(Exception cause) {
    return new ExternalServiceException(InfraErrors.STORAGE_UNAVAILABLE, "对象存储不可用", true, cause);
  }

  public void put(
      StorageKey key, InputStream input, long size, String contentType, Visibility visibility) {
    if (size < 0) throw new IllegalArgumentException("对象长度不能为负");
    Path spool = null;
    try {
      // Validate before overwriting an existing object. Do not buffer unbounded media in heap.
      spool = Files.createTempFile("b2b-upload-", ".bin");
      long count = 0;
      try (var output = Files.newOutputStream(spool)) {
        byte[] buffer = new byte[65536];
        int read;
        while ((read = input.read(buffer)) != -1) {
          count += read;
          if (count > size) throw new IllegalArgumentException("对象长度与声明不一致");
          output.write(buffer, 0, read);
        }
      }
      if (count != size) throw new IllegalArgumentException("对象长度与声明不一致");
      client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket(visibility))
              .key(key.value())
              .contentType(contentType)
              .build(),
          RequestBody.fromFile(spool));
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw unavailable(e);
    } finally {
      if (spool != null)
        try {
          Files.deleteIfExists(spool);
        } catch (IOException ignored) {
        }
    }
  }

  public InputStream get(StorageKey key, Visibility visibility) {
    try {
      return client.getObject(
          GetObjectRequest.builder().bucket(bucket(visibility)).key(key.value()).build());
    } catch (S3Exception e) {
      if (e.statusCode() == 404)
        throw new NotFoundException(InfraErrors.STORAGE_OBJECT_NOT_FOUND, "对象不存在");
      throw unavailable(e);
    } catch (Exception e) {
      throw unavailable(e);
    }
  }

  public Optional<StoredObjectInfo> stat(StorageKey key, Visibility visibility) {
    try {
      var info =
          client.headObject(
              HeadObjectRequest.builder().bucket(bucket(visibility)).key(key.value()).build());
      return Optional.of(
          new StoredObjectInfo(
              key,
              visibility,
              info.contentLength(),
              info.contentType(),
              info.eTag(),
              info.lastModified()));
    } catch (S3Exception e) {
      if (e.statusCode() == 404) return Optional.empty();
      throw unavailable(e);
    } catch (Exception e) {
      throw unavailable(e);
    }
  }

  public void delete(StorageKey key, Visibility visibility) {
    try {
      client.deleteObject(
          DeleteObjectRequest.builder().bucket(bucket(visibility)).key(key.value()).build());
    } catch (Exception e) {
      throw unavailable(e);
    }
  }

  public URI publicUrl(StorageKey key) {
    return cdn.resolve(key.value());
  }

  public URI presignedGet(StorageKey key, Duration ttl, String fileName) {
    if (ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofDays(7)) > 0)
      throw new IllegalArgumentException("预签名有效期超出范围");
    var request = GetObjectRequest.builder().bucket(privateBucket).key(key.value());
    if (fileName != null) {
      if (fileName.contains("\r") || fileName.contains("\n"))
        throw new IllegalArgumentException("文件名含非法换行");
      request.responseContentDisposition(
          "attachment; filename*=UTF-8''"
              + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return URI.create(
        signer
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(request.build())
                    .build())
            .url()
            .toExternalForm());
  }
}
