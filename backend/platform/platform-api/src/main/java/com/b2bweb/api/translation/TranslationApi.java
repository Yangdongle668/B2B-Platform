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

public interface TranslationApi {

    /** translation 已启用、站点开启了询盘翻译功能、且配置了可用的主引擎。 */
    boolean isTextTranslationAvailable();

    /**
     * 同步翻译一段纯文本（不经翻译记忆、术语表只用于提示词、不写任何业务数据）。
     * 主引擎失败时按站点配置切换备用引擎；429 / 5xx 的重试由引擎适配器完成（4.7.9）。
     *
     * @throws ModuleDisabledException     translation 在当前站点停用
     * @throws BusinessException           TRANSLATION_FEATURE_DISABLED、TRANSLATION_ENGINE_NOT_CONFIGURED、
     *                                     TRANSLATION_TEXT_TOO_LONG、TRANSLATION_BUDGET_EXCEEDED
     * @throws ExternalServiceException    TRANSLATION_ENGINE_UNAVAILABLE（retryable = true）、TRANSLATION_OUTPUT_INVALID
     */
    TextTranslation translateText(@Valid TextTranslationRequest request);

    /** 本地化当前草稿各翻译单元的译文来源统计（来自 trn_job_item 与翻译记忆）；没有翻译记录的本地化不出现在结果中。 */
    Map<Long, SegmentOriginStats> segmentOrigins(Collection<Long> localizationIds);
}
