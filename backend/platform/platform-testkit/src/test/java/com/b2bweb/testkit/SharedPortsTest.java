package com.b2bweb.testkit;

import com.b2bweb.api.common.Secret;
import com.b2bweb.api.port.*;
import com.b2bweb.testkit.contract.port.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Nested;

/** Every nested provider is rerun from a fresh fixture for every inherited JUnit scenario. */
class SharedPortsTest {
 @Nested class Images extends ImageUrlSignerContract {
  final TestPlatform p=TestPlatform.standard(); protected ImageUrlSigner api(){return p.imageUrlSigner();}
 }
 @Nested class Buffer extends BufferStoreContract {
  final TestPlatform p=TestPlatform.standard();protected BufferStore api(){return p.bufferStore();}protected void site(long id){p.useSite(id);}protected void advance(Duration d){p.clock().advance(d);}
 }
 @Nested class Locks extends DistributedLockContract {
  final TestPlatform p=TestPlatform.standard();protected DistributedLock api(){return p.distributedLock();}protected void advance(Duration d){p.clock().advance(d);}
 }
 @Nested class Limits extends RateLimiterContract {
  final TestPlatform p=TestPlatform.standard();protected RateLimiter api(){return p.rateLimiter();}protected void site(long id){p.useSite(id);}protected void advance(Duration d){p.clock().advance(d);}protected void available(boolean value){p.rateLimiter().setAvailable(value);}
 }
 @Nested class Captcha extends CaptchaVerifierContract {
  final TestPlatform p=TestPlatform.standard();protected CaptchaVerifier api(){return p.implementation(CaptchaVerifier.class);}protected void allow(String t,CaptchaResult r){p.captchaVerifier().allow(t,r);}
 }
 @Nested class Scanner extends VirusScannerContract {
  final TestPlatform p=TestPlatform.standard();protected VirusScanner api(){return p.virusScanner();}protected void available(boolean v){p.virusScanner().setAvailable(v);}
 }
 @Nested class Geo extends GeoIpResolverContract {
  final TestPlatform p=TestPlatform.standard();protected GeoIpResolver api(){return p.geoIpResolver();}protected void seed(String ip,GeoLocation g){p.geoIpResolver().seed(ip,g);}
 }
 @Nested class Mx extends MxRecordCheckerContract {
  final TestPlatform p=TestPlatform.standard();protected MxRecordChecker api(){return p.mxRecordChecker();}protected void seed(String d,MxStatus s,List<String> h){p.mxRecordChecker().seed(d,s,h);}
 }
 @Nested class Cdn extends CdnPurgerContract {
  final TestPlatform p=TestPlatform.standard();protected CdnPurger api(){return p.cdnPurger();}protected void available(boolean v){p.cdnPurger().available(v);}protected int successfulPurges(){return p.cdnPurger().tagPurges().size();}
 }
 @Nested class IndexNow extends IndexNowClientContract {
  final TestPlatform p=TestPlatform.standard();protected IndexNowClient api(){return p.indexNowClient();}protected void status(int v){p.indexNowClient().status(v);}
 }
 @Nested class Mail extends MailTransportContract {
  final TestPlatform p=TestPlatform.standard();protected MailTransport api(){return p.mailTransport();}protected MailMessage received(){return p.mailTransport().sent().getLast().message();}
 }
 @Nested class Fetch extends RemoteFileFetcherContract {
  final TestPlatform p=TestPlatform.standard();protected RemoteFileFetcher api(){return p.remoteFileFetcher();}protected void seed(URI u,String t,byte[] b,String ip){p.remoteFileFetcher().seed(u,t,b,ip);}
 }
 @Nested class Webhooks extends WebhookTransportContract {
  final TestPlatform p=TestPlatform.standard();protected WebhookTransport api(){return p.webhookTransport();}protected void seed(URI u,String ip,int s){p.webhookTransport().seed(u,ip,s);}protected Map<String,String> receivedHeaders(){return p.webhookTransport().deliveries().getLast().headers();}protected String receivedBody(){return p.webhookTransport().deliveries().getLast().body();}
 }
 @Nested class Engine extends TranslationEngineContract {
  final TestPlatform p=TestPlatform.standard();protected TranslationEngine api(){return p.translationEngine();}protected void output(String id,String text){p.translationEngine().output(id,text);}protected EngineConfig config(){return new EngineConfig(1,"测试",URI.create("https://unused.example.test"),Secret.of("fixture"),"fixture",0,null,Duration.ofSeconds(1),StructuredOutputMode.JSON_OBJECT,Map.of());}
 }
}
