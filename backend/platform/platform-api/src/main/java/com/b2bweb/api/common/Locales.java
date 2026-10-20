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

/** 语言代码工具。语言代码为 BCP 47 字符串；源语言固定为 zh-CN（系统设计 5.4）。 */
public final class Locales {
    private Locales() {}

    /** 源语言：内容与站点资源的录入语言。 */
    public static final String SOURCE = "zh-CN";

    /** 语言代码最大长度（与数据库 VARCHAR(16) 一致）。 */
    public static final int MAX_LENGTH = 16;

    public static boolean isSource(String locale) {
        return SOURCE.equals(locale);
    }

    /** 是否为合法的 BCP 47 语言代码（只允许 语言[-文字][-地区]，如 zh-CN、en、pt-BR、zh-Hans）。 */
    public static boolean isValid(String locale) {
        return locale.length() <= MAX_LENGTH
                && locale.matches("^[a-z]{2,3}(-[A-Z][a-z]{3})?(-([A-Z]{2}|[0-9]{3}))?$");
    }

    /** 规范大小写：语言小写、文字首字母大写、地区大写，如 "ZH-cn" → "zh-CN"。非法时抛出 IllegalArgumentException。 */
    public static String normalize(String locale) {
        String tag = java.util.Locale.forLanguageTag(locale.replace('_', '-')).toLanguageTag();
        if (!isValid(tag)) {
            throw new IllegalArgumentException("非法的语言代码: " + locale);
        }
        return tag;
    }
}
