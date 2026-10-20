// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.media;
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

public final class MediaErrors {
    private MediaErrors() {}
    public static final ErrorCode NOT_FOUND             = ErrorCode.of("MEDIA_NOT_FOUND", 404, "媒体不存在");
    public static final ErrorCode TYPE_NOT_ALLOWED      = ErrorCode.of("MEDIA_TYPE_NOT_ALLOWED", 422, "不允许的文件类型");
    public static final ErrorCode TOO_LARGE             = ErrorCode.of("MEDIA_TOO_LARGE", 413, "文件过大");
    public static final ErrorCode MIME_MISMATCH         = ErrorCode.of("MEDIA_MIME_MISMATCH", 422, "文件内容与扩展名不符");
    public static final ErrorCode SVG_FORBIDDEN         = ErrorCode.of("MEDIA_SVG_FORBIDDEN", 403, "当前角色不允许上传 SVG");
    public static final ErrorCode NOT_READY             = ErrorCode.of("MEDIA_NOT_READY", 409, "文件仍在处理中");
    public static final ErrorCode QUARANTINED           = ErrorCode.of("MEDIA_QUARANTINED", 409, "文件未通过病毒扫描");
    public static final ErrorCode NOT_PRIVATE           = ErrorCode.of("MEDIA_NOT_PRIVATE", 422, "该操作只适用于私有文件");
    public static final ErrorCode NOT_IMAGE             = ErrorCode.of("MEDIA_NOT_IMAGE", 422, "媒体不是图片");
    public static final ErrorCode IN_USE                = ErrorCode.of("MEDIA_IN_USE", 409, "媒体仍被引用");
    public static final ErrorCode DOWNLOAD_NOT_ALLOWED  = ErrorCode.of("MEDIA_DOWNLOAD_NOT_ALLOWED", 403, "不允许下载该文件");
    public static final ErrorCode REMOTE_FETCH_FAILED   = ErrorCode.of("MEDIA_REMOTE_FETCH_FAILED", 422, "远程文件下载失败");
}
