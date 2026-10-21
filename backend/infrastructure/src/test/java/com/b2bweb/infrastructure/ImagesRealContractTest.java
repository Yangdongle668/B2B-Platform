package com.b2bweb.infrastructure;
import com.b2bweb.api.common.Secret;
import com.b2bweb.api.port.ImageUrlSigner;
import com.b2bweb.infrastructure.image.ImgproxySigner;
import com.b2bweb.testkit.contract.port.ImageUrlSignerContract;
import java.net.URI;
class ImagesRealContractTest extends ImageUrlSignerContract {
 protected ImageUrlSigner api(){return new ImgproxySigner(URI.create("https://images.example.test"),Secret.of("07".repeat(32)),Secret.of("03".repeat(16)));}
}
