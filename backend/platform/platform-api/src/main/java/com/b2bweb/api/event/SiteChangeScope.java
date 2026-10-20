// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.event;
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

/** 站点配置变化的范围。 */
public enum SiteChangeScope {
    /** 基本信息：名称、时区、默认对外语言、尾斜杠策略、主题、站点状态 */
    SITE,
    /** 域名：主域名、别名 */
    DOMAINS,
    /** 站点语言：增删、对外公开、自动翻译、URL 前缀、hreflang、排序 */
    LOCALES,
    /** 模块设置（SettingsApi）；settingKeys 列出变化的键 */
    SETTINGS,
    /** 站点资料（SiteProfileApi） */
    PROFILE,
    /** 前台界面文案（UiTextApi）；uiTextNamespaces 列出变化的命名空间 */
    UI_TEXT
}
