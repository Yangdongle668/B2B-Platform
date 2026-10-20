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
 * @param items                与请求的单元一一对应，顺序与请求一致
 * @param detectedSourceLocale 请求未指定源语言时模型识别出的语言（BCP 47）
 * @param model                响应中的模型名
 * @param attempts             HTTP 请求次数（含重试）
 */
public record TranslationResult(List<EngineTranslation> items, @Nullable String detectedSourceLocale,
                                EngineUsage usage, String model, Duration elapsed, int attempts) {
    public TranslationResult { items = List.copyOf(items); }
}
