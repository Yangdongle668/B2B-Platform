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

/** 站点上下文的来源。 */
public enum ContextOrigin {
    /** 后台请求（X-Site-Id，已校验用户在该站点有角色） */
    ADMIN,
    /** 前台公开请求或 Nuxt → Delivery 请求（按域名解析） */
    PUBLIC,
    /** 内部接口（/api/internal） */
    INTERNAL,
    /** 领域事件监听（取自 DomainEvent.siteId） */
    EVENT,
    /** 异步任务（取自 sys_job.site_id） */
    JOB,
    /** 定时任务（调度器按站点逐个建立） */
    SCHEDULED,
    /** 系统管理操作中显式切换（SiteContextProvider.callAs） */
    SYSTEM
}
