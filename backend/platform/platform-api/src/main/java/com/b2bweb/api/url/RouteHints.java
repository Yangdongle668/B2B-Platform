// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.url;
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
 * 内容类型的所属模块为某本地化提供的路由提示（ContentTypeProvider.routeHints，4.5.2）。
 *
 * @param patternKey             url_pattern 的匹配键：为空时按内容类型；TERM_PAGE 为词汇表 key（如 "product_category"）
 * @param defaultPatternOverride 覆盖 ContentTypeDefinition.defaultUrlPattern（如各词汇表的落地页规则）；为空表示不覆盖
 * @param parentContentId        {parentPath} 的来源：父页面或上级分类落地页
 * @param fallbackParentContentId UnpublishStrategy.REDIRECT_PARENT 的目标：主分类落地页或父页面
 * @param localeRoot             是否为语言首页（路径 = 语言前缀）
 * @param variables              其他规则变量
 */
public record RouteHints(@Nullable String patternKey, @Nullable String defaultPatternOverride,
                         @Nullable Long parentContentId, @Nullable Long fallbackParentContentId,
                         boolean localeRoot, Map<String, String> variables) {
    public RouteHints { variables = Map.copyOf(variables); }

    public static RouteHints none() { return new RouteHints(null, null, null, null, false, Map.of()); }
}
