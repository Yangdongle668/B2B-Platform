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
 * 引擎配置（trn_engine）。
 *
 * @param engineId        trn_engine.id（指标标签、日志）
 * @param name            配置名称（后台显示）
 * @param baseUrl         如 https://api.openai.com/v1、https://api.deepseek.com/v1 或自部署地址；适配器拼接 /chat/completions
 * @param apiKey          以 Authorization: Bearer 发送；自部署服务不需要时可为空串
 * @param temperature     默认 0.2
 * @param maxOutputTokens 请求的 max_tokens；为空表示不设置
 * @param timeout         单次 HTTP 请求的超时
 * @param extraHeaders    额外请求头（如自部署网关所需）；不得包含 Authorization
 */
public record EngineConfig(long engineId, String name, URI baseUrl, Secret apiKey, String model, double temperature,
                           @Nullable Integer maxOutputTokens, Duration timeout, StructuredOutputMode structuredOutput,
                           Map<String, String> extraHeaders) {
    public EngineConfig {
        if (!"https".equals(baseUrl.getScheme()) && !"http".equals(baseUrl.getScheme())) {
            throw new IllegalArgumentException("baseUrl 必须是 http(s) 地址");
        }
        if (temperature < 0 || temperature > 2) throw new IllegalArgumentException("temperature 必须在 0–2 之间");
        if (extraHeaders.keySet().stream().anyMatch(h -> h.equalsIgnoreCase("Authorization"))) {
            throw new IllegalArgumentException("extraHeaders 不得包含 Authorization");
        }
        extraHeaders = Map.copyOf(extraHeaders);
    }
}
