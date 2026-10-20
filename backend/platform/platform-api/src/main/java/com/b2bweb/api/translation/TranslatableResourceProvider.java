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

public interface TranslatableResourceProvider {

    /** 本实现负责的资源类型（TranslatableResourceTypes 中的值）。 */
    Set<String> resourceTypes();

    default boolean supports(String resourceType) { return resourceTypes().contains(resourceType); }

    /** 提取资源的翻译单元（ctx.subject() 为 Resource）；资源不存在时返回空列表。 */
    List<TranslationUnit> extract(TranslationContext ctx);

    /** 写回目标语言（ctx.targetLocale()）的资源文本；资源已删除时忽略。必须幂等。 */
    void apply(TranslationContext ctx, Map<String, String> translations);

    /** 当前站点该类型的全部资源 ID（开放新语言、术语表变化后的批量翻译）；按 ID 升序，游标稳定。 */
    CursorPage<String> resourceIds(String resourceType, CursorRequest cursor);
}
