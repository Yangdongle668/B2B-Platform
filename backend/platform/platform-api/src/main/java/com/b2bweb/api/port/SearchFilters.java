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

/** 过滤表达式构造（纯函数），防止表达式注入。 */
public final class SearchFilters {
    private SearchFilters() {}
    private static final java.util.regex.Pattern FIELD = java.util.regex.Pattern.compile("^[a-z_][a-z0-9_.]*$");

    /** 转义为双引号字符串字面量（转义 \ 与 "）。 */
    public static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** field = "value"；field 必须是白名单形式的字段名。 */
    public static String eq(String field, String value) { return checked(field) + " = " + quote(value); }

    /** field IN ["a", "b"]。 */
    public static String in(String field, Collection<String> values) {
        return checked(field) + " IN [" + String.join(", ", values.stream().map(SearchFilters::quote).toList()) + "]";
    }

    /** 用 AND 连接（每个子句加括号）。 */
    public static String and(List<String> clauses) {
        return String.join(" AND ", clauses.stream().map(c -> "(" + c + ")").toList());
    }

    private static String checked(String field) {
        if (!FIELD.matcher(field).matches()) throw new IllegalArgumentException("非法的字段名: " + field);
        return field;
    }
}
