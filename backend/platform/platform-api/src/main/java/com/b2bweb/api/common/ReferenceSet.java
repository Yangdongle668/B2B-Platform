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
 * 一组被引用的对象 ID。用于：Delivery 解析 refs 与生成缓存标签、媒体引用收集、SEO 链接索引。
 */
public record ReferenceSet(java.util.Set<Long> contentIds, java.util.Set<Long> mediaIds, java.util.Set<Long> termIds) {
    public ReferenceSet {
        contentIds = java.util.Set.copyOf(contentIds);
        mediaIds = java.util.Set.copyOf(mediaIds);
        termIds = java.util.Set.copyOf(termIds);
    }

    public static ReferenceSet empty() {
        return new ReferenceSet(java.util.Set.of(), java.util.Set.of(), java.util.Set.of());
    }

    public ReferenceSet merge(ReferenceSet other) {
        var c = new java.util.HashSet<>(contentIds); c.addAll(other.contentIds);
        var m = new java.util.HashSet<>(mediaIds); m.addAll(other.mediaIds);
        var t = new java.util.HashSet<>(termIds); t.addAll(other.termIds);
        return new ReferenceSet(c, m, t);
    }

    public boolean isEmpty() { return contentIds.isEmpty() && mediaIds.isEmpty() && termIds.isEmpty(); }

    /**
     * 扫描任意 JSON（快照、布局文档、ProseMirror 文档），收集全部引用：
     * 对象键 "$content" / "$media" / "$term" 的值；任意字符串值或 ProseMirror link 标记的 href 中以 content:// 开头的值。
     * 纯函数，所有模块使用同一实现，保证引用识别规则一致。
     */
    public static ReferenceSet scan(tools.jackson.databind.JsonNode json) {
        var content = new java.util.HashSet<Long>(); var media = new java.util.HashSet<Long>();
        var terms = new java.util.HashSet<Long>();
        var pending = new java.util.ArrayDeque<tools.jackson.databind.JsonNode>(); pending.push(json);
        while (!pending.isEmpty()) {
            var node = pending.pop();
            if (node.isObject()) {
                for (var entry : node.properties()) {
                    var target = switch (entry.getKey()) {
                        case "$content" -> content; case "$media" -> media; case "$term" -> terms;
                        default -> null;
                    };
                    var value = entry.getValue();
                    if (target != null && (value.isString() || value.isIntegralNumber())) {
                        try { long id = Long.parseLong(value.asString()); if (id > 0) target.add(id); }
                        catch (NumberFormatException ignored) { /* malformed refs are validated by the owner */ }
                    }
                    pending.push(value);
                }
            } else if (node.isArray()) node.forEach(pending::push);
            else if (node.isString()) ContentRef.fromUri(node.asString()).filter(ref -> ref.contentId() > 0)
                    .ifPresent(ref -> content.add(ref.contentId()));
        }
        return new ReferenceSet(content, media, terms);
    }
}
