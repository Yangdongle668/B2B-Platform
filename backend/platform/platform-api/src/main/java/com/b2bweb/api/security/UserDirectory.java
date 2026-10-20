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

public interface UserDirectory {

    /** 按 ID 查询用户（含已停用；已软删除的用户 name 为匿名化后的值）。 */
    java.util.Optional<UserSummary> find(long userId);

    /** 批量查询；不存在的 ID 不出现在结果中。 */
    java.util.Map<Long, UserSummary> findAll(java.util.Collection<Long> userIds);

    /**
     * 当前站点内拥有某权限的启用用户（含系统管理员）。
     * locale 非空时，对 localeScoped 权限只返回语言范围包含该语言的用户。
     */
    java.util.List<UserSummary> usersWithPermission(String permission, @org.jspecify.annotations.Nullable String locale);

    /** 用户是否启用且在当前站点有角色（分配询盘、选择负责人前校验）。 */
    boolean isActiveMember(long userId);
}
