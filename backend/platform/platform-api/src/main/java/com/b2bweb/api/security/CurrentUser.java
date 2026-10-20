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
 * @param systemAdmin 系统管理员（拥有所有站点的权限）
 * @param roleCodes   在当前站点担任的角色代码
 * @param mfaVerified 本次会话是否已通过 2FA
 */
public record CurrentUser(long userId, String name, String email, boolean systemAdmin,
                          java.util.Set<String> roleCodes, boolean mfaVerified) {
    public CurrentUser { roleCodes = java.util.Set.copyOf(roleCodes); }

    public com.b2bweb.api.common.ActorRef toActor() { return com.b2bweb.api.common.ActorRef.user(userId, name); }
}
