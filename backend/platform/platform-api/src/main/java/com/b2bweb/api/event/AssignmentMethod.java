// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.event;
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

/** 分配方式（系统设计 17.7）。 */
public enum AssignmentMethod {
    /** 命中分配规则（指定用户） */
    RULE,
    /** 命中分配规则（销售组轮询） */
    ROUND_ROBIN,
    /** 无规则匹配，使用站点默认负责人 */
    DEFAULT_OWNER,
    /** 7 天内同一邮箱的重复询盘，沿用原负责人 */
    DUPLICATE_OWNER,
    /** 手动改派 */
    MANUAL
}
