// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.security;
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

/**
 * 声明应用服务方法（或类）需要的权限，由 core 的切面在方法执行前校验（系统设计 26.2）。
 * 前端隐藏按钮只是体验优化，不能代替本注解。
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface RequiresPermission {

    /** 权限码，如 "inquiry:inquiry:export"。 */
    String[] value();

    /** 多个权限码的关系：ALL（全部拥有）或 ANY（任一）。 */
    Logical logical() default Logical.ALL;

    /** 是否需要近期二次验证（修改密码、导出数据、修改脚本等敏感操作，系统设计 26.1）。 */
    boolean reauthenticate() default false;

    enum Logical { ALL, ANY }
}
