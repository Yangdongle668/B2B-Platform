// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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

public interface UiTextApi {

    /**
     * 文案键在指定语言的文本。zh-CN：站点覆盖 → 主题默认；其他语言：该语言的译文，没有译文时为空（不回退到中文）。
     */
    java.util.Optional<String> text(String key, String locale);

    /** 批量读取；结果只含有文本的键。 */
    java.util.Map<String, String> texts(java.util.Collection<String> keys, String locale);

    /** 某语言的全部文案（站点引导数据使用）。Delivery 热路径。 */
    java.util.Map<String, String> allTexts(String locale);

    /** 读取并替换 {name} 形式的插值占位符；文案不存在时为空。 */
    java.util.Optional<String> format(String key, String locale, java.util.Map<String, String> args);
}
