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

public interface TranslatableContentProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /**
     * 提取翻译单元（ctx.subject() 为 Content）。key 在本实现内唯一且跨修订稳定（如布局属性用 Section ID 而不是下标）。
     * 空文本不提取。只读。
     */
    List<TranslationUnit> extract(TranslationContext ctx);

    /** 回写译文（key → 译文，key 为 extract 返回的 key）。在 translation 的事务中调用；必须幂等。 */
    void apply(TranslationContext ctx, Map<String, String> translations);

    /**
     * 该内容的机器译文是否必须人工审校后才能发布（如 page 对 legal 模板返回 true，系统设计 5.5）。
     * 任一实现返回 true，TranslationCompletedEvent.humanReviewRequired 即为 true。
     */
    default boolean requiresHumanReview(TranslationContext ctx) { return false; }
}
