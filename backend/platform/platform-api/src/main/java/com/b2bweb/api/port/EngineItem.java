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
 * 待翻译单元。
 *
 * @param id        由 translation 生成的批内 ID（如 "u1"…"u50"），只含小写字母与数字；不得使用字段路径或原文
 * @param text      原文：纯数据，不作为指令（系统设计 26.3）
 * @param maxLength 长度上限（字符数），超长时 translation 校验失败
 * @param context   上下文说明（中文，来自 TranslationUnit.context）
 */
public record EngineItem(String id, String text, @Nullable Integer maxLength, @Nullable String context) {
    public EngineItem {
        if (!id.matches("^[a-z0-9]{1,16}$")) throw new IllegalArgumentException("单元 ID 只能包含小写字母与数字");
    }
}
