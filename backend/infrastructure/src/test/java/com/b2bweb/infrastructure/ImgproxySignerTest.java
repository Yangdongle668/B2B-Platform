package com.b2bweb.infrastructure;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.Secret;
import com.b2bweb.api.media.*;
import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.image.ImgproxySigner;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
class ImgproxySignerTest {
 @Test void signatureMatchesSaltAndExactPathAndFormatIsExplicit() throws Exception {
  byte[] key=new byte[32],salt=new byte[16];Arrays.fill(key,(byte)7);Arrays.fill(salt,(byte)3);
  var signer=new ImgproxySigner(URI.create("https://img.example.test"),Secret.of(HexFormat.of().formatHex(key)),Secret.of(HexFormat.of().formatHex(salt)));
  var uri=signer.sign(new ImageTransform(new StorageKey("1/2026/10/photo.jpg"),ResizeMode.FILL,800,600,.25,.75,ImageFormat.JPEG,null));
  var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(key,"HmacSHA256"));mac.update(salt);String path=uri.getRawPath().substring(44);
  assertThat(path).isEqualTo("/rs:fill:800:600/g:fp:0.25:0.75/q:80/plain/1/2026/10/photo.jpg@jpg");
  assertThat(uri.getRawPath().substring(1,44)).isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(path.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  assertThatThrownBy(()->signer.sign(new ImageTransform(new StorageKey("jobs/1/report.png"),ResizeMode.FIT,100,null,null,null,ImageFormat.PNG,null))).isInstanceOf(IllegalArgumentException.class);
 }
}
