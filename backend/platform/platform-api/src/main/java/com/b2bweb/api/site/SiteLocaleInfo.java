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
 * 站点语言。
 *
 * @param urlPrefix        URL 前缀（不含斜杠），全部语言都有前缀
 * @param hreflang         hreflang 值（默认只用语言代码：zh 或 zh-Hans、en、de）
 * @param publiclyVisible  对外公开（数据库列 public）：前台可访问，出现在语言切换器、hreflang、sitemap 中
 * @param autoTranslate    源语言发布、站点资源变化时自动翻译到该语言；源语言恒为 false
 * @param source           是否为源语言（zh-CN）
 */
public record SiteLocaleInfo(String locale, String urlPrefix, String hreflang, boolean publiclyVisible,
                             boolean autoTranslate, int sortOrder, boolean source) {}
