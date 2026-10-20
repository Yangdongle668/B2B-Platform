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

/** 页码分页：page 从 1 开始，size 1–100（系统设计 24.2）。 */
public record PageRequest(int page, int size, java.util.List<SortOrder> sort) {
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 1) throw new IllegalArgumentException("page 从 1 开始");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size 必须在 1–100 之间");
        sort = java.util.List.copyOf(sort);
    }

    public static PageRequest of(int page, int size) { return new PageRequest(page, size, java.util.List.of()); }

    public long offset() { return (long) (page - 1) * size; }
}
