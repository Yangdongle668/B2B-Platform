// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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
 * 发布快照（cnt_revision）。payload 结构见系统设计 6.4 与 4.5.3 的 SnapshotParts。
 *
 * @param path              当前发布路径；findRevision 返回的历史修订为发布时的路径
 * @param contentModifiedAt 内容实际变化的时间（sitemap lastmod）
 */
public record PublishedSnapshot(long contentId, long localizationId, String contentType, String locale,
                                long revisionId, int revisionNo, JsonNode payload, String payloadHash,
                                @Nullable String path, Instant publishedAt, Instant contentModifiedAt,
                                long publishedBy, PublishTrigger trigger) {}
