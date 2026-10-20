// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.translation;
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
 * @param detectedSourceLocale 识别出的源语言（BCP 47）；无法识别时为空
 * @param sameLanguage         原文已是目标语言（此时 translatedText 等于原文，不计费）
 * @param engineName           引擎配置名称（trn_engine.name）；@param model 模型名（取自响应）
 */
public record TextTranslation(String translatedText, @Nullable String detectedSourceLocale, boolean sameLanguage,
                              String engineName, String model, int inputTokens, int outputTokens,
                              Instant translatedAt) {}
