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

public interface PlatformModule {

    /** 读取 module.json 得到的描述（由 core 提供的 ModuleDescriptor.load 解析 classpath 中的 module.json）。 */
    ModuleDescriptor descriptor();

    /** 首次安装：迁移已由平台执行；在此初始化默认数据（如默认表单、内置字典 DictApi.ensureBuiltin）。必须幂等。 */
    default void onInstall(ModuleContext ctx) {}

    /** 版本升级：数据修正（结构迁移由 Flyway 完成）。必须幂等。 */
    default void onUpgrade(ModuleContext ctx, com.b2bweb.api.common.SemVer fromVersion) {}

    /** 每次启动：安装/升级后按依赖拓扑顺序同步声明资源；必须幂等，不使用站点过滤。 */
    default void onStartup(ModuleContext ctx) {}

    /** 站点启用：初始化该站点的默认数据（首次启用时）。在站点上下文中调用。 */
    default void onEnable(SiteModuleContext ctx) {}

    /** 站点停用：停止该站点的后台处理（数据保留，系统设计 3.8）。在站点上下文中调用。 */
    default void onDisable(SiteModuleContext ctx) {}

    /** 停用前的影响分析，例如“该站点有 120 篇已发布文章将不可访问”。 */
    default com.b2bweb.api.common.ImpactReport analyzeDisableImpact(SiteModuleContext ctx) {
        return com.b2bweb.api.common.ImpactReport.none();
    }

    /** 彻底删除模块数据（仅 CLI / 系统管理员，所有站点均已停用，需二次确认且已备份）。 */
    default void onPurge(ModuleContext ctx) {}
}
