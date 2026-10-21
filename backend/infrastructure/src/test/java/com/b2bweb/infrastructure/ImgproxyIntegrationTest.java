package com.b2bweb.infrastructure;

import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.infrastructure.image.ImgproxySigner;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;

/** Real signed HTTP -> imgproxy -> public S3 -> encoded image; no HTTP response fixtures. */
class ImgproxyIntegrationTest {
  @BeforeAll static void connect() { LocalAdaptersTest.connect(); }
  @AfterAll static void disconnect() { LocalAdaptersTest.close(); }

  @Test void actualPixelsFormatsAndSignatureBoundary() throws Exception {
    var api = new ImgproxySigner(URI.create(LocalAdaptersTest.required("B2BWEB_IMGPROXY_URL")),
        Secret.of(LocalAdaptersTest.required("B2BWEB_IMGPROXY_KEY")),
        Secret.of(LocalAdaptersTest.required("B2BWEB_IMGPROXY_SALT")));
    var key = new StorageKey("1/contract/" + UUID.randomUUID() + ".png");
    var source = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
    var pixels = source.createGraphics();
    pixels.setColor(java.awt.Color.RED); pixels.fillRect(0, 0, 160, 240);
    pixels.setColor(java.awt.Color.BLUE); pixels.fillRect(160, 0, 160, 240); pixels.dispose();
    var encoded = new ByteArrayOutputStream(); ImageIO.write(source, "png", encoded);
    byte[] bytes = encoded.toByteArray();
    try (var http = HttpClient.newHttpClient()) {
      LocalAdaptersTest.storage.put(key, new ByteArrayInputStream(bytes), bytes.length, "image/png", Visibility.PUBLIC);
      for (var format : ImageFormat.values()) {
        var url = api.sign(new ImageTransform(key, ResizeMode.FILL, 80, 60, .25, .5, format, null));
        var response = http.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).as("imgproxy %s", format).isEqualTo(200);
        String mime = format == ImageFormat.JPEG ? "image/jpeg" : "image/" + format.name().toLowerCase(Locale.ROOT);
        assertThat(response.headers().firstValue("Content-Type")).contains(mime);
        if (format == ImageFormat.PNG || format == ImageFormat.JPEG) {
          var image = ImageIO.read(new ByteArrayInputStream(response.body()));
          assertThat(image.getWidth()).isEqualTo(80); assertThat(image.getHeight()).isEqualTo(60);
        } else if (format == ImageFormat.WEBP) {
          assertThat(new String(response.body(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
          assertThat(new String(response.body(), 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WEBP");
        } else assertThat(new String(response.body(), 4, 8, StandardCharsets.US_ASCII)).contains("ftypavif");
        URI forged = URI.create(url.toString().replace("/rs:fill:80:60/", "/rs:fill:81:60/"));
        assertThat(http.send(HttpRequest.newBuilder(forged).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
      }
      var fit = api.sign(new ImageTransform(key, ResizeMode.FIT, 100, null, null, null, ImageFormat.PNG, null));
      var fitted = ImageIO.read(new ByteArrayInputStream(http.send(HttpRequest.newBuilder(fit).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body()));
      assertThat(fitted.getWidth()).isEqualTo(100); assertThat(fitted.getHeight()).isEqualTo(75);
      // A valid signature still cannot read an object from the private bucket.
      LocalAdaptersTest.storage.delete(key, Visibility.PUBLIC);
      LocalAdaptersTest.storage.put(key, new ByteArrayInputStream(bytes), bytes.length, "image/png", Visibility.PRIVATE);
      assertThat(http.send(HttpRequest.newBuilder(fit).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
    } finally {
      LocalAdaptersTest.storage.delete(key, Visibility.PUBLIC);
      LocalAdaptersTest.storage.delete(key, Visibility.PRIVATE);
    }
  }
}
