// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.notification;
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
 * @param templateKey    {模块 ID}.{场景}.{接收方}，如 "inquiry.autoreply.customer"
 * @param locale         客户邮件的语言；员工通知忽略（固定 zh-CN）
 * @param fallbackLocale locale 没有模板时使用的语言；为空表示不回退
 * @param model          模板变量
 * @param replyTo        Reply-To（客户自动回复设为负责人邮箱）
 * @param dedupKey       幂等键，如 "inquiry:7201893453912345:autoreply"
 * @param notBefore      最早发送时间；为空表示立即
 * @param relatedType / relatedId 关联对象（如 "inquiry" / 询盘 ID），用于后台查询与数据主体请求
 */
public record NotificationRequest(@NotBlank String templateKey, @NotEmpty List<Recipient> recipients,
                                  @Nullable String locale, @Nullable String fallbackLocale,
                                  JsonNode model, @Nullable @Email String replyTo,
                                  @Nullable @Size(max = 128) String dedupKey, @Nullable Instant notBefore,
                                  @Nullable String relatedType, @Nullable String relatedId) {
    public NotificationRequest { recipients = List.copyOf(recipients); }
}
