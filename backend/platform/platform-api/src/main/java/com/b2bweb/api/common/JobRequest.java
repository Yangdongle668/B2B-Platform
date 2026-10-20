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

/**
 * @param jobType     任务类型 {moduleId}.{名称}，如 "product.import"；必须有对应的 JobHandler
 * @param title       任务名称（中文，任务中心列表显示）
 * @param params      任务参数（JSON，不得包含明文密钥）
 * @param scheduledAt 最早执行时间；为空表示立即
 * @param maxAttempts 最多执行次数（默认 1，即不自动重试）
 */
public record JobRequest(String moduleId, String jobType, String title,
                         tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode params,
                         @org.jspecify.annotations.Nullable String idempotencyKey,
                         @org.jspecify.annotations.Nullable Long parentJobId,
                         java.time.@org.jspecify.annotations.Nullable Instant scheduledAt,
                         int maxAttempts) {
    public static JobRequest of(String moduleId, String jobType, String title, tools.jackson.databind.JsonNode params) {
        return new JobRequest(moduleId, jobType, title, params, null, null, null, 1);
    }
}
