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

/**
 * @param filter 过滤表达式：由 search 用白名单字段构造，值必须经 SearchFilters 转义，不得拼接用户输入
 * @param sort   如 "published_at:desc"；字段必须在 sortableAttributes 中
 * @param limit  1–100
 */
public record SearchRequest(String query, @Nullable String filter, List<String> facets, List<String> sort,
                            int offset, int limit, List<String> attributesToRetrieve,
                            List<String> attributesToHighlight) {
    public SearchRequest {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit 必须在 1–100 之间");
        if (offset < 0) throw new IllegalArgumentException("offset 不能为负数");
        facets = List.copyOf(facets);
        sort = List.copyOf(sort);
        attributesToRetrieve = List.copyOf(attributesToRetrieve);
        attributesToHighlight = List.copyOf(attributesToHighlight);
    }
}
