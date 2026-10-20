// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
import java.util.*;
import java.time.*;
import java.net.*;
import java.io.*;
import java.util.function.*;
import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.url.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.form.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.seo.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;

public interface ImageUrlSigner {

    /**
     * 生成 imgproxy 签名 URL（系统设计 12.3）：
     * {imgproxyBase}/{签名}/rs:{fit|fill}:{w}:{h}/g:fp:{x}:{y}/q:{quality}/plain/{对象键}@{avif|webp|jpg|png}。
     * 只用于公开桶中的图片；格式在 URL 中显式指定，不依赖 Accept 协商；EXIF 等元数据由 imgproxy 配置统一去除。
     * 纯计算、不访问网络，可在 Delivery 热路径中调用。
     */
    URI sign(ImageTransform transform);
}
