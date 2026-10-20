// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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

public final class CoreErrors {
    private CoreErrors() {}
    public static final ErrorCode VALIDATION_FAILED     = ErrorCode.of("CORE_VALIDATION_FAILED", 400, "输入校验失败");
    public static final ErrorCode NO_SITE_CONTEXT       = ErrorCode.of("CORE_NO_SITE_CONTEXT", 500, "缺少站点上下文");
    public static final ErrorCode SITE_NOT_FOUND        = ErrorCode.of("CORE_SITE_NOT_FOUND", 404, "站点不存在");
    public static final ErrorCode LOCALE_NOT_ENABLED    = ErrorCode.of("CORE_LOCALE_NOT_ENABLED", 422, "站点未启用该语言");
    public static final ErrorCode MODULE_DISABLED       = ErrorCode.of("CORE_MODULE_DISABLED", 404, "模块已停用");
    public static final ErrorCode FORBIDDEN             = ErrorCode.of("CORE_FORBIDDEN", 403, "没有权限");
    public static final ErrorCode NOT_AUTHENTICATED     = ErrorCode.of("CORE_NOT_AUTHENTICATED", 401, "未登录");
    public static final ErrorCode SETTING_NOT_DECLARED  = ErrorCode.of("CORE_SETTING_NOT_DECLARED", 500, "设置键未声明");
    public static final ErrorCode SETTING_VERSION_CONFLICT = ErrorCode.of("CORE_SETTING_VERSION_CONFLICT", 409, "设置已被他人修改");
    public static final ErrorCode SETTING_INVALID       = ErrorCode.of("CORE_SETTING_INVALID", 422, "设置值不符合 Schema");
    public static final ErrorCode JOB_NOT_FOUND         = ErrorCode.of("CORE_JOB_NOT_FOUND", 404, "任务不存在");
    public static final ErrorCode JOB_TYPE_UNKNOWN      = ErrorCode.of("CORE_JOB_TYPE_UNKNOWN", 500, "未注册的任务类型");
    public static final ErrorCode JOB_STATE_INVALID     = ErrorCode.of("CORE_JOB_STATE_INVALID", 409, "任务状态不允许该操作");
    public static final ErrorCode DICT_TYPE_NOT_FOUND   = ErrorCode.of("CORE_DICT_TYPE_NOT_FOUND", 404, "字典类型不存在");
    public static final ErrorCode SECRET_DECRYPT_FAILED = ErrorCode.of("CORE_SECRET_DECRYPT_FAILED", 500, "密文无法解密");
    public static final ErrorCode EXTENSION_KEY_UNKNOWN = ErrorCode.of("CORE_EXTENSION_KEY_UNKNOWN", 404, "扩展不存在");
}
