// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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

public final class ContentErrors {
    private ContentErrors() {}
    public static final ErrorCode NOT_FOUND                  = ErrorCode.of("CONTENT_NOT_FOUND", 404, "内容不存在");
    public static final ErrorCode LOCALIZATION_NOT_FOUND     = ErrorCode.of("CONTENT_LOCALIZATION_NOT_FOUND", 404, "本地化不存在");
    public static final ErrorCode REVISION_NOT_FOUND         = ErrorCode.of("CONTENT_REVISION_NOT_FOUND", 404, "修订不存在");
    public static final ErrorCode TYPE_UNKNOWN               = ErrorCode.of("CONTENT_TYPE_UNKNOWN", 422, "未注册或已停用的内容类型");
    public static final ErrorCode LOCALE_NOT_SOURCE          = ErrorCode.of("CONTENT_LOCALE_NOT_SOURCE", 422, "新建内容必须使用源语言");
    public static final ErrorCode LOCALE_IS_SOURCE           = ErrorCode.of("CONTENT_LOCALE_IS_SOURCE", 422, "目标语言不能是源语言");
    public static final ErrorCode LOCALE_NOT_ENABLED         = ErrorCode.of("CONTENT_LOCALE_NOT_ENABLED", 422, "站点未启用该语言");
    public static final ErrorCode LOCALE_EXISTS              = ErrorCode.of("CONTENT_LOCALE_EXISTS", 409, "该语言的本地化已存在");
    public static final ErrorCode VERSION_CONFLICT           = ErrorCode.of("CONTENT_VERSION_CONFLICT", 409, "内容已被他人修改");
    public static final ErrorCode STATE_INVALID              = ErrorCode.of("CONTENT_STATE_INVALID", 409, "当前状态不允许该操作");
    public static final ErrorCode REVIEW_NOT_ENABLED         = ErrorCode.of("CONTENT_REVIEW_NOT_ENABLED", 409, "该内容类型未启用审核");
    public static final ErrorCode REVIEW_REQUIRED            = ErrorCode.of("CONTENT_REVIEW_REQUIRED", 409, "需要审核通过后才能发布");
    public static final ErrorCode PUBLISH_VALIDATION_FAILED  = ErrorCode.of("CONTENT_PUBLISH_VALIDATION_FAILED", 422, "发布校验未通过");
    public static final ErrorCode SLUG_INVALID               = ErrorCode.of("CONTENT_SLUG_INVALID", 422, "slug 格式不正确");
    public static final ErrorCode SLUG_CONFLICT              = ErrorCode.of("CONTENT_SLUG_CONFLICT", 409, "路径已被其他内容占用");
    public static final ErrorCode CARD_MISSING               = ErrorCode.of("CONTENT_CARD_MISSING", 422, "发布快照缺少卡片信息");
    public static final ErrorCode SNAPSHOT_TOO_LARGE         = ErrorCode.of("CONTENT_SNAPSHOT_TOO_LARGE", 422, "快照超过 1 MB");
    public static final ErrorCode REDIRECT_TARGET_INVALID    = ErrorCode.of("CONTENT_REDIRECT_TARGET_INVALID", 422, "下线跳转目标无效");
    public static final ErrorCode TRASHED                    = ErrorCode.of("CONTENT_TRASHED", 409, "内容在回收站中");
    public static final ErrorCode NOT_TRASHED                = ErrorCode.of("CONTENT_NOT_TRASHED", 409, "内容不在回收站中");
    public static final ErrorCode HAS_PUBLISHED_LOCALIZATION = ErrorCode.of("CONTENT_HAS_PUBLISHED_LOCALIZATION", 409, "仍有已发布的语言版本，请先下线");
    public static final ErrorCode IMPACT_BLOCKED             = ErrorCode.of("CONTENT_IMPACT_BLOCKED", 409, "影响分析存在阻止项");
    public static final ErrorCode PREVIEW_TTL_INVALID        = ErrorCode.of("CONTENT_PREVIEW_TTL_INVALID", 422, "预览链接有效期超出上限");
    public static final ErrorCode TRANSLATION_MODE_INVALID   = ErrorCode.of("CONTENT_TRANSLATION_MODE_INVALID", 422, "不允许的翻译模式切换");
}
