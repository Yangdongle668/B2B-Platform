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
 * @param name       规则名 {moduleId}.{名称}，如 "form.submit_ip"
 * @param capacity   一个周期内允许的次数
 * @param period     周期（令牌按 capacity / period 匀速补充）
 * @param siteScoped 是否按站点隔离
 * @param failClosed Redis 不可用时是否拒绝
 */
public record RateLimitRule(String name, long capacity, Duration period, boolean siteScoped, boolean failClosed) {
    public RateLimitRule {
        if (!name.matches("^[a-z]+\\.[a-z0-9_]+$")) throw new IllegalArgumentException("规则名格式为 {moduleId}.{名称}");
        if (capacity < 1) throw new IllegalArgumentException("capacity 必须为正数");
    }
}
