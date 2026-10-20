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

/** 错误码：{MODULE}_{REASON}，带默认 HTTP 状态与中文标题。 */
public record ErrorCode(String code, int status, String title) {
    private static final java.util.regex.Pattern FORMAT =
            java.util.regex.Pattern.compile("^[A-Z][A-Z0-9]*_[A-Z0-9_]+$");

    public ErrorCode {
        if (!FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("错误码格式必须为 {MODULE}_{REASON}: " + code);
        }
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("错误码的 HTTP 状态必须为 4xx 或 5xx: " + status);
        }
    }

    public static ErrorCode of(String code, int status, String title) {
        return new ErrorCode(code, status, title);
    }
}
