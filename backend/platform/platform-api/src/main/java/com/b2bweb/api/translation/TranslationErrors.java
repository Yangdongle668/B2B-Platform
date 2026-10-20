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

public final class TranslationErrors {
    private TranslationErrors() {}
    public static final ErrorCode FEATURE_DISABLED      = ErrorCode.of("TRANSLATION_FEATURE_DISABLED", 422, "站点已关闭该翻译功能");
    public static final ErrorCode ENGINE_NOT_CONFIGURED = ErrorCode.of("TRANSLATION_ENGINE_NOT_CONFIGURED", 422, "未配置可用的翻译引擎");
    public static final ErrorCode ENGINE_UNAVAILABLE    = ErrorCode.of("TRANSLATION_ENGINE_UNAVAILABLE", 503, "翻译引擎暂时不可用");
    public static final ErrorCode OUTPUT_INVALID        = ErrorCode.of("TRANSLATION_OUTPUT_INVALID", 502, "翻译引擎返回的结果无效");
    public static final ErrorCode TEXT_TOO_LONG         = ErrorCode.of("TRANSLATION_TEXT_TOO_LONG", 422, "待翻译文本过长");
    public static final ErrorCode BUDGET_EXCEEDED       = ErrorCode.of("TRANSLATION_BUDGET_EXCEEDED", 429, "已达到本月翻译预算上限");
}
