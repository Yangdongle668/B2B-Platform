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
 * @param data      写入 resolve 响应的 dataSources[sectionId]，如 { "items": [ { "$content": "…" } ] }
 * @param itemCount 结果条数（指标与日志）
 */
public record DataSourceResult(JsonNode data, int itemCount, Set<CacheTag> cacheTags) {
    public DataSourceResult { cacheTags = Set.copyOf(cacheTags); }

    public static DataSourceResult empty() {
        return new DataSourceResult(tools.jackson.databind.node.JsonNodeFactory.instance.objectNode(), 0, Set.of());
    }
}
