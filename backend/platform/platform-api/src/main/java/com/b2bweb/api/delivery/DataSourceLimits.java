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

/** 数据源限制（系统设计 10.5）。 */
public final class DataSourceLimits {
    private DataSourceLimits() {}
    public static final int MAX_ITEMS = 24;
    public static final int MAX_SOURCES_PER_PAGE = 10;
    public static final Duration TIMEOUT = Duration.ofMillis(500);
    /** 参数中的上下文变量：当前内容 ID。 */
    public static final String CURRENT = "$current";
}
