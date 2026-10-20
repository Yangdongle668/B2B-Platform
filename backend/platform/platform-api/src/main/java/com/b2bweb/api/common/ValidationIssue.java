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

/**
 * 一条校验问题。
 *
 * @param code     错误码，格式 {MODULE}_{REASON}，前端据此映射说明或跳转
 * @param message  中文说明（后台展示）
 * @param field    字段路径（如 "slug"、"parts.product.modelNo"、"layout.sections[2].props.title"），无则为空
 * @param source   产生问题的模块 ID（由收集方填写）
 */
public record ValidationIssue(Severity severity, String code, String message,
                              @org.jspecify.annotations.Nullable String field,
                              @org.jspecify.annotations.Nullable String source) {}
