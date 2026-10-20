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

/**
 * 提交审核（系统设计 6.7）。
 *
 * @param title       该本地化的草稿标题（notification 用于邮件正文）
 * @param version     提交时的本地化 version（通知去重）
 * @param submittedBy 提交人用户 ID
 * @param comment     提交说明（员工填写的内部文本）
 */
public record ReviewRequestedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                   String locale, String title, int version, long submittedBy,
                                   @Nullable String comment) implements DomainEvent {}
