// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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
 * 内容类型定义（系统设计 6.2；label、translatable、sitemap、searchable、adminEditRoute 为框架补充）。
 *
 * @param type              ContentTypes.isValidFormat
 * @param module            所属模块 ID，必须等于实现的 @Extension.module
 * @param label             中文名称（后台显示）
 * @param defaultUrlPattern 默认 URL 规则，不含语言前缀，如 "/products/{slug}"；不可路由为空
 * @param templates         可用模板 key（第一个为默认）
 * @param vocabularies      适用的词汇表 key
 * @param layoutSlots       是否允许在模板插槽中放置组件
 * @param reviewable        是否参与审核流（站点再按类型配置是否启用审核）
 * @param translatable      是否由 AI 翻译（有对应的 TranslatableContentProvider）
 * @param sitemap           已发布的可路由本地化是否进入 sitemap
 * @param searchable        是否进入站内搜索索引
 * @param adminEditRoute    后台编辑页路由模板，如 "/product/products/{contentId}"（通知邮件、影响报告中的链接）
 */
public record ContentTypeDefinition(String type, String module, String label, boolean routable,
                                    @Nullable String defaultUrlPattern, List<String> templates,
                                    Set<String> vocabularies, boolean layoutSlots, boolean reviewable,
                                    boolean translatable, boolean sitemap, boolean searchable,
                                    String adminEditRoute) {
    public ContentTypeDefinition {
        templates = List.copyOf(templates);
        vocabularies = Set.copyOf(vocabularies);
        if (routable && (defaultUrlPattern == null || !defaultUrlPattern.startsWith("/"))) {
            throw new IllegalArgumentException("可路由类型必须声明以 / 开头的默认 URL 规则: " + type);
        }
    }
}
