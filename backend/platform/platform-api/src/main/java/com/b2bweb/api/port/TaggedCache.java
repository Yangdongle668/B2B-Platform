// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
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

public interface TaggedCache {

    /** namespace 格式 {moduleId}.{名称}，如 "delivery.resolve"。 */
    <T> Optional<T> get(String namespace, String key, Class<T> type);

    /** ADR-020：公开交付链必须使用带绝对新鲜度的读取，不得将命中当成新回源。 */
    <T> Optional<CachedValue<T>> getWithFreshness(String namespace, String key, Class<T> type);

    /** 写入并登记标签。tags 必须属于当前站点；单条超过 CacheTag.MAX_TAGS_PER_RESPONSE 个时由调用方先合并（系统设计 23.2）。 */
    void put(String namespace, String key, Object value, Set<CacheTag> tags, Duration ttl);

    /** M0新增草案，ADR-020：开始回源前取得代次；Redis不可用时为空。 */
    Optional<CacheWriteFence> beginLoad();

    /** M0新增草案：与代次比较、数据及标签登记在一个原子操作中；不匹配时不写并返回false。 */
    boolean putIfUnchanged(String namespace, String key, Object value, Set<CacheTag> tags,
                           Duration ttl, CacheWriteFence fence);

    /**
     * 读取；未命中时合并并发回源（系统设计 23.3 防击穿）：同一键只有一个调用执行 loader，其他调用最多等待 waitTimeout
     * 后读取其结果（仍未得到时自行执行 loader）。loader 返回空表示结果不缓存（如 404）。
     */
    <T> Optional<T> getOrLoad(String namespace, String key, Class<T> type, Duration ttl, Duration waitTimeout,
                              Supplier<Optional<CacheEntry<T>>> loader);

    /** ADR-020：与getOrLoad同样代次保护，额外返回当前结果的绝对有效时刻。 */
    <T> Optional<CachedValue<T>> getOrLoadWithFreshness(String namespace, String key, Class<T> type,
            Duration ttl, Duration waitTimeout, Supplier<Optional<CacheEntry<T>>> loader);

    /** 删除带有任一标签的全部键并清理标签索引；返回删除的键数量。标签必须属于当前站点。 */
    long invalidate(Collection<CacheTag> tags);

    void evict(String namespace, String key);

    /** 删除当前站点某命名空间的全部键（如重定向批量变化时清除路由解析缓存）；返回删除数量。 */
    long evictNamespace(String namespace);
}
