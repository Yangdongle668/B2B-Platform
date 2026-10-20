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
 * 当前站点上下文。不可变；站点配置变化后在下一个请求 / 任务中生效。
 *
 * @param siteId         站点 ID
 * @param siteCode       站点代码（日志、Redis 键前缀之外的可读标识）
 * @param sourceLocale   源语言，恒为 {@code zh-CN}
 * @param defaultLocale  默认对外语言（根路径 302 与 x-default 的目标）
 * @param timezone       站点时区（定时发布的输入与展示、按天统计）
 * @param requestLocale  当前请求的内容语言：前台请求由路径前缀解析；后台请求、任务、事件为空
 * @param origin         上下文来源
 */
public record SiteContext(
        long siteId,
        String siteCode,
        String sourceLocale,
        String defaultLocale,
        java.time.ZoneId timezone,
        @org.jspecify.annotations.Nullable String requestLocale,
        ContextOrigin origin) {
}
