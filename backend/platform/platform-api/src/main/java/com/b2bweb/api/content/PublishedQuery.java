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
 * 已发布内容查询（基于投影，不读草稿）。
 *
 * @param anyTermIds 命中任一分类项（空表示不限）
 * @param contentIds 手选列表：非空时只返回这些内容，并按列表顺序（忽略 sort）
 * @param sort       白名单：publishedAt、firstPublishedAt、contentModifiedAt、title；默认 -publishedAt
 */
public record PublishedQuery(String locale, Set<String> contentTypes, Set<Long> anyTermIds,
                             List<Long> contentIds, Set<Long> excludeContentIds,
                             List<SortOrder> sort, @Positive @Max(100) int limit) {
    public PublishedQuery {
        contentTypes = Set.copyOf(contentTypes);
        anyTermIds = Set.copyOf(anyTermIds);
        contentIds = List.copyOf(contentIds);
        excludeContentIds = Set.copyOf(excludeContentIds);
        sort = List.copyOf(sort);
    }
}
