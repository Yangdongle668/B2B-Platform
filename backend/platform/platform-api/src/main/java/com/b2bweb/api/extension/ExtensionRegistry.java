// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.extension;
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

public interface ExtensionRegistry {

    /** 当前站点可用的某扩展点的全部实现。没有站点上下文时抛出 NoSiteContextException。 */
    <T> java.util.List<T> all(Class<T> point);

    /** 当前站点可用、且适用于该内容类型的实现。 */
    <T> java.util.List<T> forContentType(Class<T> point, String contentType);

    /** 按 key 查找当前站点可用的实现（DataSourceProvider、JobHandler 等 KeyedExtension）。 */
    <T extends KeyedExtension> java.util.Optional<T> byKey(Class<T> point, String key);

    /** 按 key 查找；不存在或所属模块停用时抛出 NotFoundException（CORE_EXTENSION_KEY_UNKNOWN）。 */
    <T extends KeyedExtension> T requireByKey(Class<T> point, String key);

    /** 指定站点可用的实现（系统级任务逐站点处理时使用）。 */
    <T> java.util.List<T> allForSite(long siteId, Class<T> point);

    /** 指定站点可用、且适用于该内容类型的实现。 */
    <T> java.util.List<T> forContentTypeForSite(long siteId, Class<T> point, String contentType);

    /**
     * 不按站点过滤的全部实现：只用于平台级用途（启动时同步声明式资源、模块安装、诊断）。业务调用不得使用。
     */
    <T> java.util.List<T> allRegardlessOfSite(Class<T> point);

    /** 实现所属的模块 ID（取自 @Extension.module）。 */
    String moduleOf(Object extension);

    /** 某扩展点的全部实现的注册信息（后台诊断页、契约测试）。 */
    java.util.List<ExtensionInfo> describe(Class<?> point);
}
