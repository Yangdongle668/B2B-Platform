// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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

/** 缓存标签（CDN Cache-Tag 与 Redis 标签索引共用），全部带站点前缀 s{siteId}。 */
public record CacheTag(String value) {
    /** 单个响应的标签上限；超过时把卡片标签合并为 list:{type}（系统设计 23.2）。 */
    public static final int MAX_TAGS_PER_RESPONSE = 200;

    public CacheTag {
        if (!value.matches("^s\\d+(:[a-z]+(:[A-Za-z0-9_\\-]+)?)?$")) {
            throw new IllegalArgumentException("非法的缓存标签: " + value);
        }
    }

    /** s{site}：站点全部 */
    public static CacheTag site(long siteId) { return new CacheTag("s" + siteId); }
    /** s{site}:c:{contentId}：某内容（页面主体、卡片或链接） */
    public static CacheTag content(long siteId, long contentId) { return new CacheTag("s" + siteId + ":c:" + contentId); }
    /** s{site}:t:{termId}：分类项（名称、列表） */
    public static CacheTag term(long siteId, long termId) { return new CacheTag("s" + siteId + ":t:" + termId); }
    /** s{site}:m:{mediaId}：媒体（alt、尺寸） */
    public static CacheTag media(long siteId, long mediaId) { return new CacheTag("s" + siteId + ":m:" + mediaId); }
    /** s{site}:g:{contentId}：某内容的关系列表 */
    public static CacheTag graph(long siteId, long contentId) { return new CacheTag("s" + siteId + ":g:" + contentId); }
    /** s{site}:list:{type}：某类型的列表 */
    public static CacheTag list(long siteId, String contentType) { return new CacheTag("s" + siteId + ":list:" + contentType); }
    /** s{site}:f:{formId}：表单定义（框架补充：内嵌表单定义的页面随表单变化失效） */
    public static CacheTag form(long siteId, long formId) { return new CacheTag("s" + siteId + ":f:" + formId); }
    /** s{site}:nav：导航 */
    public static CacheTag nav(long siteId) { return new CacheTag("s" + siteId + ":nav"); }
    /** s{site}:settings：站点设置、主题、站点资料、界面文案 */
    public static CacheTag settings(long siteId) { return new CacheTag("s" + siteId + ":settings"); }
    /** s{site}:sitemap：sitemap */
    public static CacheTag sitemap(long siteId) { return new CacheTag("s" + siteId + ":sitemap"); }
}
