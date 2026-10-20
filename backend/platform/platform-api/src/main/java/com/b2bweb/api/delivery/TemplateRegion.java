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
 * @param component 固定区域渲染的组件类型
 * @param allowed   插槽允许的组件类型
 * @param max       插槽最多放置的组件数
 * @param source    固定区域的数据源（读取时解析，修改不需要重新发布内容）
 * @param defaults  插槽的默认 Section 列表（JSON 数组，结构同布局文档 sections）
 */
public record TemplateRegion(String key, RegionKind kind, @Nullable String component, Set<String> allowed,
                             int max, @Nullable DataSourceSpec source, JsonNode defaults) {
    public TemplateRegion { allowed = Set.copyOf(allowed); }
}
