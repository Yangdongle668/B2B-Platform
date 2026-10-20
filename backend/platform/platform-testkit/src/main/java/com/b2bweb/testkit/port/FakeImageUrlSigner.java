package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.net.*;

/** Deterministic URL fixture; never used as an imgproxy credential or a production signer. */
public final class FakeImageUrlSigner implements ImageUrlSigner {
  public URI sign(ImageTransform t) {
    String format = t.format().name().toLowerCase(java.util.Locale.ROOT);
    int quality =
        t.quality() == null
            ? (format.equals("avif") ? 60 : format.equals("webp") ? 75 : 80)
            : t.quality();
    return URI.create(
        "https://images.example.test/testkit/rs:"
            + t.mode().name().toLowerCase(java.util.Locale.ROOT)
            + ":"
            + t.width()
            + ":"
            + (t.height() == null ? 0 : t.height())
            + "/g:fp:"
            + (t.focalX() == null ? .5 : t.focalX())
            + ":"
            + (t.focalY() == null ? .5 : t.focalY())
            + "/q:"
            + quality
            + "/plain/"
            + t.source().value()
            + "@"
            + format);
  }
}
