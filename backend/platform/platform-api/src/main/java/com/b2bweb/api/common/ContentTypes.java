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

/** V1 内置内容类型（系统设计 6.2）。内容类型是字符串，V2 模块可以通过 ContentTypeProvider 注册新类型。 */
public final class ContentTypes {
    private ContentTypes() {}
    public static final String PAGE = "PAGE";
    public static final String PRODUCT = "PRODUCT";
    public static final String ARTICLE = "ARTICLE";
    public static final String CASE_STUDY = "CASE_STUDY";
    public static final String FAQ = "FAQ";
    public static final String TERM_PAGE = "TERM_PAGE";
    public static final String GLOBAL_BLOCK = "GLOBAL_BLOCK";

    /** 内容类型格式：大写字母开头，大写字母、数字、下划线，最长 32。 */
    public static boolean isValidFormat(String type) {
        return type.matches("^[A-Z][A-Z0-9_]{1,31}$");
    }
}
