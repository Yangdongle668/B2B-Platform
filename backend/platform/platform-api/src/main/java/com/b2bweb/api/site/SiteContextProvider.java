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

public interface SiteContextProvider {

    /** 当前站点上下文；没有时为空（如应用启动、系统级任务）。 */
    java.util.Optional<SiteContext> current();

    /** 当前站点上下文；没有时抛出 NoSiteContextException。 */
    SiteContext require();

    /** 当前站点 ID；没有时抛出 NoSiteContextException。 */
    long requireSiteId();

    /**
     * 以指定站点为上下文执行 action，结束后恢复原上下文（origin = SYSTEM）。
     * 只允许系统级代码使用：系统级定时任务逐站点处理、模块安装时为各站点初始化数据、数据主体请求的逐站点处理。
     * 站点不存在时抛出 NotFoundException（CORE_SITE_NOT_FOUND）。
     */
    <T> T callAs(long siteId, java.util.function.Supplier<T> action);

    /** 同 callAs，无返回值。 */
    void runAs(long siteId, Runnable action);
}
