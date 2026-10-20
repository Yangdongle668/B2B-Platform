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
 * 一个对象（内容本地化或站点资源）在一个目标语言的译文已回写（系统设计 5.7 第 8 步）。content 与 resource 恰好一个非空。
 *
 * @param jobId               trn_job.id
 * @param content             内容翻译（含目标本地化 ID 与依据的源语言修订号）
 * @param resource            站点资源翻译
 * @param state               TRANSLATED 或 NEEDS_REVIEW
 * @param humanReviewRequired 任一 TranslatableContentProvider.requiresHumanReview 返回 true（法律页面、legal 模板，4.5.20）
 * @param autoPublish         translation 的结论：内容翻译、state = TRANSLATED、humanReviewRequired = false，
 *                            且站点开启了译文自动发布（默认开启，系统设计 5.7 第 7 步、6.7）
 */
public record TranslationCompletedEvent(EventMeta meta, long jobId,
                                        TranslationSubject.@Nullable Content content,
                                        TranslationSubject.@Nullable Resource resource,
                                        String targetLocale, TranslationState state,
                                        boolean humanReviewRequired, boolean autoPublish,
                                        TranslationStats stats, @Nullable Integer targetVersion) implements DomainEvent {

    public TranslationCompletedEvent {
        // ADR-024（M0草案）：内容事件必须带本次回写后共同版本；资源事件不带版本。
        if ((content != null && targetVersion == null) || (resource != null && targetVersion != null)) {
            throw new IllegalArgumentException("targetVersion只用于内容翻译且必填");
        }
        if ((content == null) == (resource == null)) throw new IllegalArgumentException("content 与 resource 必须恰好一个非空");
        if (state != TranslationState.TRANSLATED && state != TranslationState.NEEDS_REVIEW) {
            throw new IllegalArgumentException("state 只能是 TRANSLATED 或 NEEDS_REVIEW");
        }
        if (autoPublish && (content == null || state != TranslationState.TRANSLATED || humanReviewRequired)) {
            throw new IllegalArgumentException("只有无需人工审校、已通过校验的内容译文可以自动发布");
        }
    }

    public TranslationSubject subject() { return content != null ? content : resource; }
}
