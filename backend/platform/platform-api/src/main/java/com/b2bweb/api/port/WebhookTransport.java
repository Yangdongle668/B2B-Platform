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

public interface WebhookTransport {

    /**
     * 发送一次 Webhook 请求（不重试；重试由 notification 的发件箱负责）。请求头：
     * Content-Type: application/json；X-B2B-Event: {eventType}；X-B2B-Delivery: {deliveryId}；X-B2B-Timestamp: {Unix 秒}；
     * X-B2B-Signature: v1={hex(HMAC-SHA256(signingSecret, timestamp + "." + body))}。
     * URL 必须是 https；解析后的地址不得是回环、私有、链路本地或元数据地址（与 RemoteFileFetcher 相同的 SSRF 防护，系统设计 26.3）；
     * 不跟随重定向。超时默认 10 秒。非 2xx 应答作为结果返回，不抛出异常。
     *
     * @throws BusinessException        INFRA_WEBHOOK_URL_FORBIDDEN
     * @throws ExternalServiceException INFRA_WEBHOOK_UNREACHABLE（连接失败、超时；retryable = true）
     */
    WebhookResponse post(WebhookDelivery delivery);
}
