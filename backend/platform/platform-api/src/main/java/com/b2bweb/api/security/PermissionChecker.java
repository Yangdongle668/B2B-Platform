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

public interface PermissionChecker {

    /** 当前用户在当前站点是否拥有该权限；系统管理员恒为 true；没有登录用户时为 false。 */
    boolean has(String permission);

    /** 拥有任一权限。 */
    boolean hasAny(String... permissions);

    /** 没有权限时抛出 ForbiddenException（CORE_FORBIDDEN）。 */
    void check(String permission);

    /**
     * 当前用户对该权限的数据范围：多个角色取最大（ALL > GROUP > OWN）；
     * 权限不区分数据范围时返回 ALL；没有该权限时为空。
     */
    java.util.Optional<DataScope> dataScope(String permission);

    /** 语言范围：权限声明了 localeScoped 时按用户角色的语言范围返回；否则返回 LocaleScope.all()。 */
    LocaleScope localeScope(String permission);

    /** 当前用户是否可以对该语言执行该权限（翻译审校人员限定语言，系统设计 5.8）。 */
    boolean canAccessLocale(String permission, String locale);

    /** 指定用户在当前站点是否拥有该权限（筛选通知收件人等）。 */
    boolean userHas(long userId, String permission);
}
