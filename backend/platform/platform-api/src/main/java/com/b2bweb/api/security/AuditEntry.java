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
 * @param moduleId    模块 ID
 * @param action      动作 {资源}.{动作}，如 "localization.publish"、"inquiry.export"
 * @param targetType  目标类型，如 "PRODUCT"、"setting"、"sys_user"
 * @param targetId    目标 ID（TSID 十进制字符串、设置键、模块 ID 等）
 * @param summary     中文摘要
 * @param diff        字段变更；密码、密钥、恢复码等必须用 AuditChange.masked() 脱敏
 * @param systemLevel 系统级操作（站点、模块、数据主体请求等），写入时 site_id 为空
 */
public record AuditEntry(String moduleId, String action,
                         @org.jspecify.annotations.Nullable String targetType,
                         @org.jspecify.annotations.Nullable String targetId,
                         String summary,
                         java.util.Map<String, AuditChange> diff,
                         AuditResult result,
                         boolean sensitive,
                         boolean systemLevel) {
    public AuditEntry { diff = java.util.Map.copyOf(diff); }

    public static AuditEntry success(String moduleId, String action, String targetType, String targetId, String summary) {
        return new AuditEntry(moduleId, action, targetType, targetId, summary, java.util.Map.of(), AuditResult.SUCCESS, false, false);
    }
}
