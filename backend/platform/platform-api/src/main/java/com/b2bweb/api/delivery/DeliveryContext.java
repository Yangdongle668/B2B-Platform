// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.delivery;
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

/** 交付上下文（delivery 实现）。 */
public interface DeliveryContext {
    long siteId();
    String locale();
    DeliveryMode mode();
    long contentId();
    long localizationId();
    String contentType();
    /** LIVE 为当前修订号；PREVIEW 草稿与 EDITOR 为 0。 */
    int revisionNo();
    /** 快照（结构见 4.5.3）。 */
    JsonNode snapshot();
    /** 当前路径（含语言前缀）；PREVIEW 中未发布的本地化为空。 */
    @Nullable String path();
    /** 规范地址：主域名 https + 路径，去除查询参数（分页参数除外，系统设计 14.2、14.3）。 */
    String canonicalUrl();
    /** 请求的查询参数（seo 按分页与筛选参数决定 robots 与 canonical）。 */
    Map<String, List<String>> queryParameters();
    /**
     * 布局中引用的 key：delivery 按组件 Schema 中标注 "x-ref": "<kind>" 的属性收集（框架补充，同步系统设计 10.4）。
     * referencedKeys("form") → 表单 key；referencedKeys("global-block") → 全局区块 key。
     */
    Set<String> referencedKeys(String refKind);
    /** 面包屑（已加首页与当前页，4.5.9）。 */
    List<BreadcrumbItem> breadcrumbs();
    RefResolver refs();
}
