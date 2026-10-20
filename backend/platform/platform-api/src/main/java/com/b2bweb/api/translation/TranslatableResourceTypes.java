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

/** V1 可翻译资源类型：{模块 ID}.{名称}（与 4.3.10、4.4.4 一致），resourceId 的含义见注释。 */
public final class TranslatableResourceTypes {
    private TranslatableResourceTypes() {}
    /** 菜单（菜单项标签与可见性）；resourceId = menuId */
    public static final String NAVIGATION_MENU = "navigation.menu";
    /** 表单（标签、选项、提示语、成功提示、告知与营销同意文本）；resourceId = formId */
    public static final String FORM = "form.form";
    /** 媒体 alt、标题、说明；resourceId = mediaId */
    public static final String MEDIA = "media.media";
    /** 产品属性分组名；resourceId = groupId */
    public static final String PRODUCT_ATTRIBUTE_GROUP = "product.attribute_group";
    /** 产品属性名、单位说明与枚举选项名；resourceId = attributeId */
    public static final String PRODUCT_ATTRIBUTE = "product.attribute";
    /** 分类名（分类描述随 TERM_PAGE 作为内容翻译）；resourceId = termId */
    public static final String TAXONOMY_TERM = "taxonomy.term";
    /** 客户邮件模板（只限 CUSTOMER 受众）；resourceId = 模板 key */
    public static final String NOTIFICATION_TEMPLATE = "notification.template";
    /** 站点资料中的文本；resourceId = siteId */
    public static final String SITE_PROFILE = "core.site_profile";
    /** 前台界面文案；resourceId = 文案命名空间（如 "form"、"common"），一个命名空间一个资源 */
    public static final String UI_TEXT = "core.ui_text";
    /** 作者职务与简介；resourceId = authorId（4.10.2，Proposed） */
    public static final String ARTICLE_AUTHOR = "article.author";
    /** Cookie 横幅与同意设置文案；resourceId = siteId */
    public static final String CONSENT_BANNER = "tracking.consent_banner";
}
