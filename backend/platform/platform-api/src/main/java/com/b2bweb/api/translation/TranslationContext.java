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
 * 翻译上下文：携带站点、翻译对象、源语言与目标语言。
 *
 * @param jobId translation 的任务 ID（trn_job.id），用于日志与幂等；extract 预估时为 0
 * @param expectedTargetVersion 内容任务在调用引擎前捕获并持久化的目标草稿version；内容apply必填，预估extract及资源上下文可空
 */
public record TranslationContext(long siteId, TranslationSubject subject, String sourceLocale,
                                 String targetLocale, long jobId, @Nullable Integer expectedTargetVersion) {

    public static TranslationContext forContent(long siteId, TranslationSubject.Content subject,
                                                String targetLocale, long jobId, @Nullable Integer expectedTargetVersion) {
        return new TranslationContext(siteId, subject, com.b2bweb.api.common.Locales.SOURCE, targetLocale, jobId, expectedTargetVersion);
    }

    public static TranslationContext forResource(long siteId, String resourceType, String resourceId,
                                                 String targetLocale, long jobId) {
        return new TranslationContext(siteId, new TranslationSubject.Resource(resourceType, resourceId),
                com.b2bweb.api.common.Locales.SOURCE, targetLocale, jobId, null);
    }

    /** 内容翻译时取内容对象；资源翻译时调用抛出 IllegalStateException。 */
    public TranslationSubject.Content content() {
        if (subject instanceof TranslationSubject.Content c) return c;
        throw new IllegalStateException("不是内容翻译上下文");
    }

    /** 资源翻译时取资源对象；内容翻译时调用抛出 IllegalStateException。 */
    public TranslationSubject.Resource resource() {
        if (subject instanceof TranslationSubject.Resource r) return r;
        throw new IllegalStateException("不是资源翻译上下文");
    }
}
