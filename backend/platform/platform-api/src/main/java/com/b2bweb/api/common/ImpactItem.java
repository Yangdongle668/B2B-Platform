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

/**
 * 一条影响。
 *
 * @param source     产生该条目的模块 ID
 * @param severity   ERROR：阻止操作；WARNING：需要用户确认；INFO：仅提示
 * @param code       {MODULE}_{REASON}
 * @param message    中文说明，如“该产品被 3 个页面引用，下线后对应卡片将被移除”
 * @param count      受影响对象的数量
 * @param contentIds 受影响的内容 ID（最多 50 个，用于后台跳转）
 * @param adminRoute 后台查看详情的路由（可空），如 "/seo/links?target=7201893453912345"
 */
public record ImpactItem(String source, Severity severity, String code, String message, int count,
                         java.util.List<Long> contentIds,
                         @org.jspecify.annotations.Nullable String adminRoute) {
    public ImpactItem { contentIds = java.util.List.copyOf(contentIds); }
}
