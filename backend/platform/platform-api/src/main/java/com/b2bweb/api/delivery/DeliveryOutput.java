// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.delivery;
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
 * 交付输出收集器（4.1.1 允许的可变容器）。delivery 为每个贡献方创建一个实例，非线程安全。
 */
public final class DeliveryOutput {
    private final Map<String, JsonNode> sections = new java.util.LinkedHashMap<>();
    private final Set<CacheTag> cacheTags = new java.util.LinkedHashSet<>();
    private ReferenceSet references = ReferenceSet.empty();
    private @Nullable Duration maxAge;

    /** 写入一个顶层部分（同 key 覆盖）。DeliveryContributor 只能写入自己的 sectionKey()。 */
    public DeliveryOutput put(String section, JsonNode value) { sections.put(section, value); return this; }

    public DeliveryOutput addCacheTag(CacheTag tag) { cacheTags.add(tag); return this; }

    public DeliveryOutput addCacheTags(Collection<CacheTag> tags) { cacheTags.addAll(tags); return this; }

    /** 额外的引用（输出 JSON 中的 $content / $media / $term 会被自动扫描，不必重复登记）。 */
    public DeliveryOutput addReferences(ReferenceSet refs) { references = references.merge(refs); return this; }

    /** 收紧响应的缓存时长（多个贡献方取最小值）。 */
    public DeliveryOutput capMaxAge(Duration value) {
        maxAge = (maxAge == null || value.compareTo(maxAge) < 0) ? value : maxAge;
        return this;
    }

    public Map<String, JsonNode> sections() { return Map.copyOf(sections); }
    public Set<CacheTag> cacheTags() { return Set.copyOf(cacheTags); }
    public ReferenceSet references() { return references; }
    public Optional<Duration> maxAge() { return Optional.ofNullable(maxAge); }
}
