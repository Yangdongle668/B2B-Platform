package com.b2bweb.infrastructure;

import com.b2bweb.api.common.Secret;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.infrastructure.dns.*;
import com.b2bweb.infrastructure.http.*;
import com.b2bweb.infrastructure.redis.*;
import com.b2bweb.infrastructure.virus.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class RuntimePorts {
  @Bean
  RateLimiter rateLimiter(StringRedisTemplate redis, SiteContextProvider sites) {
    return new RedisRateLimiter(redis, sites);
  }

  @Bean
  DistributedLock distributedLock(StringRedisTemplate redis, SiteContextProvider sites) {
    return new RedisDistributedLock(redis, sites);
  }

  @Bean
  BufferStore bufferStore(StringRedisTemplate redis, SiteContextProvider sites) {
    return new RedisBufferStore(redis, sites);
  }

  @Bean
  WebhookTransport webhookTransport(Clock clock) {
    return new HttpWebhookTransport(clock);
  }

  @Bean
  RemoteFileFetcher remoteFileFetcher(
      @Value("${b2bweb.infra.remote-fetch.temp-dir:${java.io.tmpdir}/b2b-fetch}") Path path) {
    return new SafeHttpRemoteFileFetcher(path);
  }

  @Bean
  MxRecordChecker mxRecordChecker(
      Clock clock,
      @Value("${b2bweb.infra.mx.timeout:3s}") Duration timeout,
      @Value("${b2bweb.infra.mx.cache-ttl:24h}") Duration ttl) {
    return new DnsMxRecordChecker(clock, timeout, ttl);
  }

  @Bean
  CaptchaVerifier captchaVerifier(
      @Value(
              "${b2bweb.infra.captcha.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
          URI endpoint) {
    return new TurnstileCaptchaVerifier(endpoint);
  }

  @Bean
  @ConditionalOnProperty(name = "b2bweb.infra.index-now.type", havingValue = "http")
  IndexNowClient indexNowClient(
      @Value("${b2bweb.infra.index-now.endpoint:https://api.indexnow.org/indexnow}") URI endpoint) {
    return new HttpIndexNowClient(endpoint);
  }

  @Bean
  @ConditionalOnProperty(name = "b2bweb.infra.cdn.type", havingValue = "cloudflare")
  CdnPurger cdnPurger(
      @Value("${b2bweb.infra.cdn.zone-id}") String zone,
      @Value("${B2BWEB_CDN_API_TOKEN}") String token,
      @Value("${b2bweb.infra.cdn.max-tags-per-request:30}") int batch,
      @Value("${b2bweb.infra.cdn.tag-purge:true}") boolean tags) {
    if (!zone.matches("[a-fA-F0-9]{32}")) throw new IllegalArgumentException("CDN区域标识无效");
    return new CloudflareCdnPurger(
        URI.create("https://api.cloudflare.com/client/v4/zones/" + zone + "/purge_cache"),
        Secret.of(token),
        batch,
        tags);
  }

  @Bean
  @ConditionalOnProperty(name = "b2bweb.infra.virus-scanner.type", havingValue = "clamav")
  VirusScanner virusScanner(
      Clock clock,
      @Value("${b2bweb.infra.virus-scanner.host}") String host,
      @Value("${b2bweb.infra.virus-scanner.port:3310}") int port,
      @Value("${b2bweb.infra.virus-scanner.timeout:60s}") Duration timeout,
      @Value("${b2bweb.infra.virus-scanner.max-stream-size:52428800}") long maximum) {
    return new ClamAvVirusScanner(host, port, timeout, maximum, clock);
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "b2bweb.infra.geoip.database-path")
  GeoIpResolver geoIpResolver(@Value("${b2bweb.infra.geoip.database-path}") Path path)
      throws java.io.IOException {
    return new MaxMindGeoIpResolver(path);
  }

  @Bean
  static com.b2bweb.infrastructure.observability.PortObservations portObservations(
      org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>
          metrics) {
    return new com.b2bweb.infrastructure.observability.PortObservations(metrics);
  }
}
