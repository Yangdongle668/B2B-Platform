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

/** 翻译对象：内容或站点资源。 */
public sealed interface TranslationSubject permits TranslationSubject.Content, TranslationSubject.Resource {

    /**
     * 内容：源语言本地化 → 目标语言本地化。
     *
     * @param sourceRevisionNo 本次翻译依据的源语言修订号
     */
    record Content(long contentId, String contentType, long sourceLocalizationId,
                   long targetLocalizationId, int sourceRevisionNo) implements TranslationSubject {}

    /**
     * 站点资源。
     *
     * @param resourceType 资源类型，见 TranslatableResourceTypes（如 "navigation.menu"）
     * @param resourceId   资源 ID（TSID 的十进制字符串；界面文案为文案键）
     */
    record Resource(String resourceType, String resourceId) implements TranslationSubject {}
}
