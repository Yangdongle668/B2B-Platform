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
 * 关系列表变化。graph 只在某内容的对外列表（前 N 名）实际变化时列入 contentIds。
 *
 * @param contentIds 关系列表变化的源内容，1–1000 个；更多时拆分为多条
 */
public record RelationChangedEvent(EventMeta meta, Set<Long> contentIds, RelationChangeSource source)
        implements DomainEvent {

    public static final int MAX_CONTENT_IDS = 1000;

    public RelationChangedEvent {
        if (contentIds.isEmpty() || contentIds.size() > MAX_CONTENT_IDS) {
            throw new IllegalArgumentException("contentIds 数量必须在 1–1000 之间");
        }
        contentIds = Set.copyOf(contentIds);
    }
}
