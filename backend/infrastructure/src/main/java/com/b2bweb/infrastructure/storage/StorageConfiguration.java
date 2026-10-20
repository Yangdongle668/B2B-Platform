package com.b2bweb.infrastructure.storage;

import com.b2bweb.api.port.ObjectStorage;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "B2BWEB_S3_ENDPOINT")
public class StorageConfiguration {
  private StaticCredentialsProvider credentials(Environment env) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            env.getRequiredProperty("B2BWEB_S3_ACCESS_KEY"),
            env.getRequiredProperty("B2BWEB_S3_SECRET_KEY")));
  }

  @Bean(destroyMethod = "close")
  S3Client s3Client(Environment env) {
    return S3Client.builder()
        .endpointOverride(URI.create(env.getRequiredProperty("B2BWEB_S3_ENDPOINT")))
        .region(Region.of(env.getProperty("B2BWEB_S3_REGION", "us-east-1")))
        .credentialsProvider(credentials(env))
        .forcePathStyle(true)
        .httpClientBuilder(
            UrlConnectionHttpClient.builder()
                .proxyConfiguration(
                    software.amazon.awssdk.http.urlconnection.ProxyConfiguration.builder()
                        .addNonProxyHost("storage")
                        .build()))
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(120))
                .retryPolicy(b -> b.numRetries(0))
                .build())
        .build();
  }

  @Bean(destroyMethod = "close")
  S3Presigner s3Presigner(Environment env) {
    return S3Presigner.builder()
        .endpointOverride(
            URI.create(
                env.getProperty(
                    "B2BWEB_S3_SIGNING_ENDPOINT", env.getRequiredProperty("B2BWEB_S3_ENDPOINT"))))
        .region(Region.of(env.getProperty("B2BWEB_S3_REGION", "us-east-1")))
        .credentialsProvider(credentials(env))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  @Bean
  ObjectStorage objectStorage(S3Client client, S3Presigner signer, Environment env) {
    return new S3ObjectStorage(
        client,
        signer,
        env.getProperty("B2BWEB_S3_PUBLIC_BUCKET", "public-assets"),
        env.getProperty("B2BWEB_S3_PRIVATE_BUCKET", "private-assets"),
        URI.create(env.getProperty("B2BWEB_CDN_ORIGIN", "https://cdn.example.invalid/assets/")));
  }
}
