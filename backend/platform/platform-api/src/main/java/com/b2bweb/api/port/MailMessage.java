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
 * @param messageId RFC 5322 Message-ID，由 notification 生成（如 <{ntf_message.id}@{发件域名}>），用于重试去重与退信关联
 * @param htmlBody  模板引擎输出的已转义 HTML
 * @param textBody  纯文本版本（必填）
 * @param headers   额外头部；不得覆盖 From、To、Reply-To、Subject、Message-ID
 */
public record MailMessage(String messageId, MailAddress from, List<MailAddress> to, @Nullable MailAddress replyTo,
                          String subject, String htmlBody, String textBody, Map<String, String> headers) {
    public MailMessage {
        if (to.isEmpty()) throw new IllegalArgumentException("收件人不能为空");
        to = List.copyOf(to);
        headers = Map.copyOf(headers);
    }
}
