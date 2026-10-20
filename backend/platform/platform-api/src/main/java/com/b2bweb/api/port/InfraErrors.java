// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
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
 * 端口错误码。INFRA 是 infrastructure 的保留前缀（不是模块 ID，框架补充 4.1.7）；
 * 使用方通常把它们转换为自己的错误码再抛给前端（如 MEDIA_REMOTE_FETCH_FAILED、TRANSLATION_ENGINE_UNAVAILABLE）。
 */
public final class InfraErrors {
    private InfraErrors() {}
    public static final ErrorCode STORAGE_UNAVAILABLE       = ErrorCode.of("INFRA_STORAGE_UNAVAILABLE", 503, "对象存储暂时不可用");
    public static final ErrorCode STORAGE_OBJECT_NOT_FOUND  = ErrorCode.of("INFRA_STORAGE_OBJECT_NOT_FOUND", 404, "存储对象不存在");
    public static final ErrorCode VIRUS_SCANNER_UNAVAILABLE = ErrorCode.of("INFRA_VIRUS_SCANNER_UNAVAILABLE", 503, "病毒扫描服务暂时不可用");
    public static final ErrorCode MAIL_TEMPORARY_FAILURE    = ErrorCode.of("INFRA_MAIL_TEMPORARY_FAILURE", 503, "邮件服务暂时不可用");
    public static final ErrorCode MAIL_REJECTED             = ErrorCode.of("INFRA_MAIL_REJECTED", 502, "邮件被服务器拒绝");
    public static final ErrorCode WEBHOOK_URL_FORBIDDEN     = ErrorCode.of("INFRA_WEBHOOK_URL_FORBIDDEN", 422, "Webhook 地址不合法");
    public static final ErrorCode WEBHOOK_UNREACHABLE       = ErrorCode.of("INFRA_WEBHOOK_UNREACHABLE", 502, "Webhook 接收方无法访问");
    public static final ErrorCode SEARCH_UNAVAILABLE        = ErrorCode.of("INFRA_SEARCH_UNAVAILABLE", 503, "搜索服务暂时不可用");
    public static final ErrorCode SEARCH_TASK_FAILED        = ErrorCode.of("INFRA_SEARCH_TASK_FAILED", 502, "搜索索引任务失败");
    public static final ErrorCode CDN_PURGE_FAILED          = ErrorCode.of("INFRA_CDN_PURGE_FAILED", 502, "CDN 缓存清除失败");
    public static final ErrorCode INDEXNOW_FAILED           = ErrorCode.of("INFRA_INDEXNOW_FAILED", 502, "IndexNow 推送失败");
    public static final ErrorCode ENGINE_UNAVAILABLE        = ErrorCode.of("INFRA_ENGINE_UNAVAILABLE", 503, "翻译引擎暂时不可用");
    public static final ErrorCode ENGINE_AUTH_FAILED        = ErrorCode.of("INFRA_ENGINE_AUTH_FAILED", 502, "翻译引擎认证失败");
    public static final ErrorCode ENGINE_BAD_REQUEST        = ErrorCode.of("INFRA_ENGINE_BAD_REQUEST", 502, "翻译引擎拒绝了请求");
    public static final ErrorCode ENGINE_OUTPUT_INVALID     = ErrorCode.of("INFRA_ENGINE_OUTPUT_INVALID", 502, "翻译引擎返回的结果无法解析");
    public static final ErrorCode CAPTCHA_UNAVAILABLE       = ErrorCode.of("INFRA_CAPTCHA_UNAVAILABLE", 503, "人机验证服务暂时不可用");
    public static final ErrorCode FETCH_HOST_NOT_ALLOWED    = ErrorCode.of("INFRA_FETCH_HOST_NOT_ALLOWED", 422, "远程地址不在白名单中");
    public static final ErrorCode FETCH_ADDRESS_FORBIDDEN   = ErrorCode.of("INFRA_FETCH_ADDRESS_FORBIDDEN", 422, "远程地址指向内部网络");
    public static final ErrorCode FETCH_TOO_LARGE           = ErrorCode.of("INFRA_FETCH_TOO_LARGE", 413, "远程文件过大");
    public static final ErrorCode FETCH_TYPE_NOT_ALLOWED    = ErrorCode.of("INFRA_FETCH_TYPE_NOT_ALLOWED", 422, "远程文件类型不允许");
    public static final ErrorCode FETCH_TIMEOUT             = ErrorCode.of("INFRA_FETCH_TIMEOUT", 504, "远程文件下载超时");
    public static final ErrorCode FETCH_HTTP_ERROR          = ErrorCode.of("INFRA_FETCH_HTTP_ERROR", 502, "远程服务器返回错误");
    public static final ErrorCode REDIS_UNAVAILABLE         = ErrorCode.of("INFRA_REDIS_UNAVAILABLE", 503, "缓存服务暂时不可用");
}
