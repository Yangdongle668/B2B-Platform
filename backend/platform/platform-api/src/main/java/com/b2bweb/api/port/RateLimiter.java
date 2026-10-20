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

public interface RateLimiter {

    /**
     * 尝试消耗 tokens 个令牌。subject 为限流维度的值：IP 用规范化后的地址；邮箱等个人数据由调用方先做 SHA-256，
     * Redis 中不保存明文。Redis 不可用时：rule.failClosed = true 返回拒绝，否则放行；两种情况都记录指标并告警。
     */
    RateLimitDecision tryConsume(RateLimitRule rule, String subject, int tokens);

    default RateLimitDecision tryConsume(RateLimitRule rule, String subject) { return tryConsume(rule, subject, 1); }
}
