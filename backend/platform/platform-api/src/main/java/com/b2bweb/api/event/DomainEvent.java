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

/**
 * 领域事件（系统设计 4.2）。全部事件都是本包中的 record，第一个组件为 meta。
 * JSON 形式：{ "meta": { "eventId", "occurredAt", "siteId", "actor" }, "schemaVersion": 1, …事件字段 }；
 * ID 一律序列化为字符串（4.1.5）。
 */
public interface DomainEvent {

    /** 公共元数据，由 EventMetaFactory 生成。 */
    EventMeta meta();

    /**
     * 事件结构版本：默认 1。发生不兼容变更时由事件类覆盖本方法返回新版本，并按 4.1.8 过渡。
     * 序列化时作为只读属性输出，反序列化时忽略（以类为准）。
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "schemaVersion",
            access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    default int schemaVersion() { return 1; }

    /** TSID 的十进制字符串。 */
    default String eventId() { return meta().eventId(); }

    default Instant occurredAt() { return meta().occurredAt(); }

    /** 站点 ID；系统级事件为空。 */
    default @Nullable Long siteId() { return meta().siteId(); }

    /** 用户 ID 的十进制字符串，或 "system"。 */
    default String actor() { return meta().actor(); }

    /** 事件类型名（类的简单名，如 "ContentPublishedEvent"），用于日志、指标与监控。 */
    default String eventType() { return getClass().getSimpleName(); }

    /**
     * 订阅方模块在该站点停用时是否仍然投递（框架补充）。默认 false：监听器包装跳过执行（系统设计 3.8）；
     * 清理类事件（ContentPurgedEvent）覆盖为 true，避免停用模块中留下孤儿数据。
     */
    default boolean deliverWhenModuleDisabled() { return false; }
}
