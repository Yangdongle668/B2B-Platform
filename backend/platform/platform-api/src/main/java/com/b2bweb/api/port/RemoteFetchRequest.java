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

/**
 * @param allowedHosts        域名白名单（站点设置，小写）；为空时一律拒绝
 * @param allowedContentTypes 允许的响应 Content-Type（如 "image/*"）；为空表示不限（最终类型由 media 用 Tika 识别）
 */
public record RemoteFetchRequest(URI url, Set<String> allowedHosts, long maxBytes, Duration timeout,
                                 int maxRedirects, Set<String> allowedContentTypes) {
    public RemoteFetchRequest {
        allowedHosts = Set.copyOf(allowedHosts);
        allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    /** 导入图片的默认参数：≤ 50 MB、30 秒、最多 3 次重定向、image/*。 */
    public static RemoteFetchRequest image(URI url, Set<String> allowedHosts) {
        return new RemoteFetchRequest(url, allowedHosts, 50L * 1024 * 1024, Duration.ofSeconds(30), 3, Set.of("image/*"));
    }
}
