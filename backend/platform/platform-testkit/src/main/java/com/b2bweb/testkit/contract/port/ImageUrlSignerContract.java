package com.b2bweb.testkit.contract.port;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class ImageUrlSignerContract {
 protected abstract ImageUrlSigner api();
 @Test public void deterministicExplicitFormatAndDimensions() {
  var source=new StorageKey("1/2026/10/101-photo.jpg");
  for(var format:ImageFormat.values()) {
   var t=new ImageTransform(source,ResizeMode.FILL,800,600,.25,.75,format,null);
   var uri=api().sign(t); assertThat(api().sign(t)).isEqualTo(uri);
   assertThat(uri.isAbsolute()).isTrue(); assertThat(uri.getScheme()).isIn("https","http");
   assertThat(uri.getPath()).contains("/rs:fill:800:600/","/g:fp:0.25:0.75/");
   assertThat(uri.getPath()).endsWith("@"+(format==ImageFormat.JPEG?"jpg":format.name().toLowerCase(Locale.ROOT)));
  }
 }
 @Test public void rejectsPrivateJobKeysAndInvalidTransform() {
  for(String key:List.of("jobs/1/2026/report.png","0/image.jpg"))
   assertThatThrownBy(()->api().sign(new ImageTransform(new StorageKey(key),ResizeMode.FIT,100,null,null,null,ImageFormat.PNG,null))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->api().sign(new ImageTransform(new StorageKey("1/a.jpg"),ResizeMode.FILL,100,0,null,null,ImageFormat.PNG,null))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->api().sign(new ImageTransform(new StorageKey("1/a.jpg"),ResizeMode.FIT,100,null,Double.NaN,null,ImageFormat.PNG,null))).isInstanceOf(IllegalArgumentException.class);
 }

}
