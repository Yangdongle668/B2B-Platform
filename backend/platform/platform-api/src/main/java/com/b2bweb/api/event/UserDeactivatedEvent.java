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
 * 用户不再能处理某些站点的工作（框架补充，系统设计 17.7“跳过停用的成员”）。
 *
 * @param siteIds DEACTIVATED：用户此前拥有角色的全部站点；REMOVED_FROM_SITE：只含该站点
 */
public record UserDeactivatedEvent(EventMeta meta, long userId, UserDeactivationReason reason,
                                   Set<Long> siteIds) implements DomainEvent {
    public UserDeactivatedEvent { siteIds = Set.copyOf(siteIds); }
}
