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

public interface NotificationApi {

    /**
     * 为每个收件人写入一条发件箱消息（加入调用方事务），返回消息 ID（与 recipients 顺序一致；被 dedupKey 去重的返回已有 ID）。
     * 员工收件人的邮箱经 UserDirectory 解析，已停用的用户被跳过（不出现在结果中）。
     *
     * @throws BusinessException NOTIFICATION_TEMPLATE_NOT_FOUND（模板 key 未注册）、NOTIFICATION_RECIPIENT_INVALID
     */
    List<Long> send(@Valid NotificationRequest request);

    /**
     * 向当前站点订阅了该事件类型的 Webhook 投递事件（发件箱 + HMAC 签名 + 重试，系统设计 18）。加入调用方事务。
     * 没有订阅时不产生消息，返回空列表。
     */
    List<Long> sendWebhookEvent(@Valid WebhookEventRequest request);

    /** 消息状态（后台时间线展示“自动回复已发送”等）。 */
    Optional<MessageStatusView> status(long messageId);

    /** 该模板在该语言是否可用（有启用的模板正文）。 */
    boolean templateAvailable(String templateKey, String locale);
}
