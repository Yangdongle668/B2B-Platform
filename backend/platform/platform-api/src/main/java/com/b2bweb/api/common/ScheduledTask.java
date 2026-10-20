// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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
 * 定时任务。用 @Extension(module = "…") 注册；key() 即 name()，也是 ShedLock 锁名。
 * 多实例部署时同一时刻只有一个实例执行（ShedLock，表 sys_lock）。
 */
public interface ScheduledTask extends com.b2bweb.api.extension.KeyedExtension {

    /** 任务名 {moduleId}.{名称}，全局唯一，如 "content.publish_due"、"graph.nightly_rebuild"。 */
    String name();

    @Override
    default String key() { return name(); }

    /** Spring cron 表达式（6 段，UTC），如 "0 * * * * *" 表示每分钟。 */
    String cron();

    /** SYSTEM：执行一次，没有站点上下文；PER_SITE：对每个未归档且启用了所属模块的站点各执行一次。 */
    ScheduleScope scope();

    /** 锁的最长持有时间（超过后视为实例失联，其他实例可以接手）。 */
    default java.time.Duration lockAtMostFor() { return java.time.Duration.ofMinutes(10); }

    void run(ScheduledTaskContext ctx);
}
