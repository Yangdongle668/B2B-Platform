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

public interface TranslationEngine {

    /**
     * 翻译一批单元：返回每个单元的译文与本次调用的 token 用量（系统设计 5.9）。
     * 请求：messages = [ { role: system, content: request.systemPrompt },
     *                    { role: user,   content: Jackson 序列化的 {"source","target","items":[{"id","text","maxLength","context"}]} } ]；
     * 待翻译文本只出现在 user 消息的 JSON 数据中，不做字符串拼接，不进入 system 消息（提示词注入防护，系统设计 26.3）。
     * 输出：{"detectedSource": string|null, "items":[{"id","text"}]}，见下方 JSON Schema。
     * 不得在持有数据库事务时调用。
     *
     * @throws ExternalServiceException INFRA_ENGINE_UNAVAILABLE（网络错误、超时、重试后仍为 429 / 5xx；retryable = true）、
     *                                  INFRA_ENGINE_AUTH_FAILED（401 / 403；retryable = false）、
     *                                  INFRA_ENGINE_BAD_REQUEST（其他 4xx，如上下文超长；retryable = false）、
     *                                  INFRA_ENGINE_OUTPUT_INVALID（输出不是合法 JSON、ID 缺失 / 多余 / 重复；retryable = false，
     *                                  translation 可缩小批次后重新请求）
     */
    TranslationResult translate(TranslationRequest request);

    /** 测试连接（后台“引擎配置 → 测试”）：翻译一个固定的短单元，返回结果、耗时与模型名；不抛出异常。 */
    EngineProbeResult probe(EngineConfig engine);
}
