// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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
 * @param primaryDomain 主域名（小写，不含协议），如 "www.example.com"
 * @param version       站点乐观锁版本（域名、语言变化时同样递增），可用作缓存版本
 */
public record SiteInfo(long siteId, String code, String name, String themeKey, String sourceLocale,
                       String defaultLocale, UrlStrategy urlStrategy, TrailingSlash trailingSlash,
                       java.time.ZoneId timezone, SiteStatus status, String primaryDomain, int version) {}
