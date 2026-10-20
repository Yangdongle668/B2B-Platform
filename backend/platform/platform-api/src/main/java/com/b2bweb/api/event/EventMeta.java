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
 * 事件公共元数据（系统设计 4.2）。只由 EventMetaFactory（4.4.14）创建；测试中用 testkit 的 TestEventMeta。
 *
 * @param eventId    TSID 的十进制字符串（IdGenerator.nextId）
 * @param occurredAt 发生时间（UTC，取自注入的 Clock）
 * @param siteId     站点 ID；系统级事件为空
 * @param actor      ActorRef.toEventActor()：用户 ID 的十进制字符串，或 "system"
 */
public record EventMeta(String eventId, Instant occurredAt, @Nullable Long siteId, String actor) {

    public EventMeta {
        if (!eventId.matches("^[0-9]{1,20}$")) throw new IllegalArgumentException("eventId 必须为 TSID 十进制字符串");
        if (!"system".equals(actor) && !actor.matches("^[1-9][0-9]{0,19}$")) {
            throw new IllegalArgumentException("actor 必须为用户 ID 或 system");
        }
    }

    /** 还原操作人（displayName 不可还原）。 */
    public com.b2bweb.api.common.ActorRef actorRef() { return com.b2bweb.api.common.ActorRef.fromEventActor(actor); }

    /** 站点级事件的站点 ID；系统级事件调用时抛出 IllegalStateException。 */
    public long requireSiteId() {
        if (siteId == null) throw new IllegalStateException("系统级事件没有 siteId");
        return siteId;
    }
}
