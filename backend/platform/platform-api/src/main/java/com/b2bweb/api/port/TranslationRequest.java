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
 * @param systemPrompt 固定的系统提示词（角色、规则、输出格式）+ 风格指南 + 本批相关术语，由 translation 组装；
 *                     前缀保持稳定以利用服务商的前缀缓存（系统设计 5.9）；不得包含待翻译文本
 * @param sourceLocale 为空表示由模型识别（询盘留言），结果中返回 detectedSourceLocale
 * @param items        1–100 个单元，ID 在批内唯一（translation 按引擎配置的单次上限分批）
 * @param purpose      用量统计分类："content"、"resource"、"inquiry"、"probe"
 */
public record TranslationRequest(EngineConfig engine, String systemPrompt, @Nullable String sourceLocale,
                                 String targetLocale, List<EngineItem> items, String purpose) {
    public TranslationRequest {
        if (items.isEmpty() || items.size() > 100) throw new IllegalArgumentException("items 数量必须在 1–100 之间");
        if (items.stream().map(EngineItem::id).distinct().count() != items.size()) {
            throw new IllegalArgumentException("单元 ID 必须唯一");
        }
        items = List.copyOf(items);
    }
}
