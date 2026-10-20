// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.event;
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
 * 翻译失败：引擎错误且重试耗尽、预算用尽、提取或回写异常（系统设计 5.7、5.10）。content 与 resource 恰好一个非空。
 *
 * @param subjectTitle 内容的源语言标题或资源名称（中文；notification 不能回查，用于邮件正文）
 * @param errorCode    如 TRANSLATION_ENGINE_UNAVAILABLE、TRANSLATION_OUTPUT_INVALID、TRANSLATION_BUDGET_EXCEEDED
 * @param errorMessage 中文说明；不得包含原文、译文与引擎返回的正文
 * @param attempts     已尝试次数
 */
public record TranslationFailedEvent(EventMeta meta, long jobId,
                                     TranslationSubject.@Nullable Content content,
                                     TranslationSubject.@Nullable Resource resource,
                                     String targetLocale, String subjectTitle, String errorCode,
                                     String errorMessage, int attempts) implements DomainEvent {

    public TranslationFailedEvent {
        if ((content == null) == (resource == null)) throw new IllegalArgumentException("content 与 resource 必须恰好一个非空");
    }

    public TranslationSubject subject() { return content != null ? content : resource; }
}
