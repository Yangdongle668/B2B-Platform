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
 * 操作人。系统操作的 userId 为 0（与数据库 created_by = 0 一致），事件中记为 "system"。
 *
 * @param userId       用户 ID；0 表示系统
 * @param displayName  显示名（审计快照用）；系统为 "system"
 */
public record ActorRef(long userId, String displayName) {
    public static final ActorRef SYSTEM = new ActorRef(0L, "system");

    public static ActorRef user(long userId, String displayName) {
        if (userId <= 0) throw new IllegalArgumentException("用户 ID 必须为正数");
        return new ActorRef(userId, displayName);
    }

    public boolean isSystem() { return userId == 0L; }

    /** 事件中的 actor 字段：用户 ID 的十进制字符串，或 "system"（系统设计 4.2）。 */
    public String toEventActor() { return isSystem() ? "system" : Long.toString(userId); }

    /** 从事件的 actor 字段还原（displayName 不可还原，置为 actor 原值）。 */
    public static ActorRef fromEventActor(String actor) {
        return "system".equals(actor) ? SYSTEM : new ActorRef(Long.parseLong(actor), actor);
    }
}
