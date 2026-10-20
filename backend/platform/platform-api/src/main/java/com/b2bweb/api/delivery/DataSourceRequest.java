// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.delivery;
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
 * @param params           已用 paramsSchema 校验、并已把 "$current" 替换为当前内容 ID 的参数
 * @param currentContentId 当前页面的内容（模板固定区域、插槽）；全局区块与独立页面为空
 * @param sectionId        发起解析的 Section ID（模板区域为区域 key）
 * @param limit            min(参数中的 limit, 24)
 */
public record DataSourceRequest(long siteId, String locale, DeliveryMode mode, JsonNode params,
                                @Nullable Long currentContentId, @Nullable String currentContentType,
                                String sectionId, int limit, RefResolver refs) {}
