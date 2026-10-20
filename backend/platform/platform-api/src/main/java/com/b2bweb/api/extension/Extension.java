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

/**
 * 声明扩展点实现（系统设计 3.7）。以 @Component 为元注解，实现类自动成为 Spring Bean，并由 ExtensionRegistry 收集。
 * 一个类可以实现多个扩展点接口。
 */
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
@org.springframework.stereotype.Component
public @interface Extension {

    /** 实现所属模块 ID（与 module.json 的 id 一致）；注册表据此按站点启用状态过滤。 */
    String module();

    /** 适用的内容类型；空表示全部。只影响 ExtensionRegistry.forContentType 的结果。 */
    String[] contentTypes() default {};

    /** 调用顺序，升序（相同时按模块 ID、类名）。 */
    int order() default ExtensionOrder.DEFAULT;
}
