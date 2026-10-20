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
 * 发布（系统设计 4.2、4.4）：人工发布、定时发布、译文自动发布都发布本事件。
 *
 * @param path           发布后的路径（含语言前缀）；不可路由类型为空
 * @param previousPath   上一次发布的路径；首次发布或路径未变时为空或等于 path
 * @param firstPublish   该本地化第一次发布
 * @param contentChanged 快照的 payload_hash 是否变化（为 false 时只是重新发布，订阅方可跳过重算）
 * @param trigger        发布方式（框架补充）：MANUAL、SCHEDULED、TRANSLATION
 */
public record ContentPublishedEvent(EventMeta meta, long contentId, String contentType, String locale,
                                    long localizationId, long revisionId, int revisionNo,
                                    @Nullable String path, @Nullable String previousPath,
                                    boolean firstPublish, boolean contentChanged,
                                    PublishTrigger trigger) implements DomainEvent {

    public boolean isSourceLocale() { return com.b2bweb.api.common.Locales.isSource(locale); }

    public boolean pathChanged() { return previousPath != null && path != null && !previousPath.equals(path); }
}
