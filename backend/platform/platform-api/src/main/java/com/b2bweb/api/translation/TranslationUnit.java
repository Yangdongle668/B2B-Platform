// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.translation;
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
 * 翻译单元（Segment）。
 *
 * @param key        字段路径，在同一个提供方、同一个对象内唯一且稳定，如 "title"、"seo.title"、"body[3]"、
 *                   "layout.sec_01J9X4K2.props.title"。translation 存储时自动加提供方模块前缀，提供方不必全局唯一
 * @param text       原文（源语言）。RICH_INLINE 格式中行内标记已转为编号占位标签 <1>…</1>
 * @param format     TEXT / RICH_INLINE
 * @param maxLength  可选的长度上限（字符数），如 SEO 标题 60；超长时 translation 生成 max{N} 变体
 * @param context    上下文说明（中文），如“产品名称”“按钮文字”，进入提示词帮助模型理解
 */
public record TranslationUnit(String key, String text, TranslationFormat format,
                              @org.jspecify.annotations.Nullable Integer maxLength,
                              @org.jspecify.annotations.Nullable String context) {
    public TranslationUnit {
        if (key.isBlank()) throw new IllegalArgumentException("key 不能为空");
        if (maxLength != null && maxLength <= 0) throw new IllegalArgumentException("maxLength 必须为正数");
    }

    public static TranslationUnit text(String key, String text, @org.jspecify.annotations.Nullable String context) {
        return new TranslationUnit(key, text, TranslationFormat.TEXT, null, context);
    }
}
