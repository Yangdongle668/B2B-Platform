// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.extension;
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

/** 常用顺序值。 */
public final class ExtensionOrder {
    private ExtensionOrder() {}
    /** 平台内置实现（core） */
    public static final int PLATFORM = 100;
    /** 内容类型的所属模块（如 card 部分、主 JSON-LD 节点） */
    public static final int OWNER = 500;
    public static final int DEFAULT = 1000;
    /** 依赖其他实现结果的后置实现（如 seo 的发布贡献读取最终标题） */
    public static final int LATE = 5000;
}
