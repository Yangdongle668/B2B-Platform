// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.notification;
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
 * @param key       {模块 ID}.{场景}.{接收方}，前缀必须是实现所属的模块 ID
 * @param name      中文名称（后台模板列表）
 * @param subject   主题模板（中文）
 * @param body      正文模板（中文，HTML；变量引用语法与转义规则见 notification 模块设计文档）
 * @param version   默认正文版本（变化时提示站点“有新版默认模板”）
 */
public record NotificationTemplateDefinition(String key, String name, NotificationAudience audience,
                                             NotificationChannel channel, String subject, String body,
                                             List<TemplateVariable> variables, int version) {
    public NotificationTemplateDefinition { variables = List.copyOf(variables); }
}
