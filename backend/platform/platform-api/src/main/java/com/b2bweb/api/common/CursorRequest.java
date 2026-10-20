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

/** 游标分页：cursor 为提供方生成的不透明字符串，首页为 null；size 1–1000。 */
public record CursorRequest(@org.jspecify.annotations.Nullable String cursor, int size) {
    public static final int MAX_SIZE = 1000;

    public CursorRequest {
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size 必须在 1–1000 之间");
    }

    public static CursorRequest first(int size) { return new CursorRequest(null, size); }
}
