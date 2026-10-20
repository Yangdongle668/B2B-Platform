// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.url;
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

public final class UrlErrors {
    private UrlErrors() {}
    public static final ErrorCode PATH_CONFLICT          = ErrorCode.of("URL_PATH_CONFLICT", 409, "路径已被占用");
    public static final ErrorCode PATH_RESERVED          = ErrorCode.of("URL_PATH_RESERVED", 422, "路径使用了保留字");
    public static final ErrorCode SLUG_INVALID           = ErrorCode.of("URL_SLUG_INVALID", 422, "slug 格式不正确");
    public static final ErrorCode PATTERN_INVALID        = ErrorCode.of("URL_PATTERN_INVALID", 422, "URL 规则无法展开");
    public static final ErrorCode REDIRECT_LOOP          = ErrorCode.of("URL_REDIRECT_LOOP", 422, "重定向形成环路");
    public static final ErrorCode REDIRECT_SOURCE_IS_ROUTE = ErrorCode.of("URL_REDIRECT_SOURCE_IS_ROUTE", 409, "源路径是现有内容的路由");
    public static final ErrorCode REDIRECT_LIMIT         = ErrorCode.of("URL_REDIRECT_LIMIT", 422, "前缀与正则重定向超过上限");
    public static final ErrorCode REDIRECT_INVALID       = ErrorCode.of("URL_REDIRECT_INVALID", 422, "重定向定义不合法");
}
