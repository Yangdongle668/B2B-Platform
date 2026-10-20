// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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

public interface SiteApi {

    /** 当前站点（取自站点上下文）。没有站点上下文时抛出 NoSiteContextException。 */
    SiteInfo current();

    /** 按 ID 查询站点；不存在时为空。 */
    java.util.Optional<SiteInfo> find(long siteId);

    /** 全部未归档的站点（SETUP、ACTIVE），按 ID 升序；系统级任务逐站点执行时使用。 */
    java.util.List<SiteInfo> listOperationalSites();

    /**
     * 按请求的 Host 解析站点（系统设计 5.2）。host 为小写、不含端口（国际化域名为 Punycode）。
     * 未登记的域名、归档站点的域名返回空。Delivery 热路径。
     */
    java.util.Optional<HostResolution> resolveHost(String host);

    /** 当前站点的全部语言（含源语言），按 sortOrder 排序。 */
    java.util.List<SiteLocaleInfo> locales();

    /** 当前站点的某个语言；站点未启用该语言时为空。 */
    java.util.Optional<SiteLocaleInfo> locale(String locale);

    /** 按 URL 前缀（不含斜杠，如 "de"、"pt-br"）查找语言；前台路径解析使用。Delivery 热路径。 */
    java.util.Optional<SiteLocaleInfo> localeByUrlPrefix(String urlPrefix);

    /** 对外公开的语言代码，按 sortOrder（hreflang、sitemap、语言切换器、IndexNow）。 */
    java.util.List<String> publicLocales();

    /** 启用自动翻译的目标语言代码（不含源语言）。 */
    java.util.List<String> autoTranslateLocales();

    /** 该语言是否已启用且对外公开（前台可访问）。 */
    boolean isPublicLocale(String locale);

    /** 模块在当前站点是否启用：核心模块恒为 true；不在构建中（MISSING）或未安装的模块为 false。 */
    boolean isModuleEnabled(String moduleId);

    /** 模块在指定站点是否启用（系统级任务使用）。 */
    boolean isModuleEnabled(long siteId, String moduleId);

    /** 当前站点某已安装模块的停用路由策略；空配置按NOT_FOUND。供url/delivery读取，不访问sys_表。 */
    ModuleDisablePolicy moduleDisablePolicy(String moduleId);

    /** 当前站点启用的模块 ID。 */
    java.util.Set<String> enabledModules();

    /** 当前站点主域名的 origin，如 "https://www.example.com"（canonical、sitemap、邮件与 Webhook 中的链接）。 */
    String primaryOrigin();

    /** 后台的 origin，如 "https://admin.example.com"（通知邮件中的后台链接）。系统级配置。 */
    String adminOrigin();

    /** 构建中全部模块的平台级信息。 */
    java.util.List<ModuleInfo> modules();
}
