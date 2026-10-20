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
 * @param host        站点主域名（全部 URL 必须属于该域名）
 * @param key         IndexNow 密钥（公开值，Nuxt 在 /{key}.txt 提供）
 * @param keyLocation 密钥文件的绝对 URL
 * @param urls        1–10,000 个绝对 URL
 */
public record IndexNowSubmission(String host, String key, URI keyLocation, List<URI> urls) {
    public IndexNowSubmission {
        if (urls.isEmpty() || urls.size() > 10_000) throw new IllegalArgumentException("urls 数量必须在 1–10000 之间");
        urls = List.copyOf(urls);
    }
}
