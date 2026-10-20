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

/** 排序：field 为提供方白名单中的字段名（camelCase）。 */
public record SortOrder(String field, boolean descending) {
    public static SortOrder asc(String field) { return new SortOrder(field, false); }
    public static SortOrder desc(String field) { return new SortOrder(field, true); }

    /** 解析 "-publishedAt" / "title" 形式（与 REST 查询参数和数据源参数一致）。 */
    public static SortOrder parse(String expr) {
        return expr.startsWith("-") ? desc(expr.substring(1)) : asc(expr);
    }
}
