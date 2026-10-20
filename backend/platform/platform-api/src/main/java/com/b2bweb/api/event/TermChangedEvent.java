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
 * @param parentId          变化后的父项；根项为空
 * @param previousParentId  MOVED 时变化前的父项；其他为空
 * @param landingContentId  可路由分类项的 TERM_PAGE 内容
 * @param descendantTermIds MOVED / DELETED 时受影响的后代分类项（不含自身）；其他为空列表
 */
public record TermChangedEvent(EventMeta meta, long termId, String vocabularyKey, TermChange change,
                               @Nullable Long parentId, @Nullable Long previousParentId,
                               @Nullable Long landingContentId, List<Long> descendantTermIds) implements DomainEvent {
    public TermChangedEvent { descendantTermIds = List.copyOf(descendantTermIds); }
}
