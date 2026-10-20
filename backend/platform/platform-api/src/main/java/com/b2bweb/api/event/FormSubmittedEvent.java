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
 * 提交已保存（系统设计 17.2 第 7 步）。不含字段值与任何个人数据；inquiry 经 FormApi.findSubmission 读取。
 *
 * @param spamVerdict REJECT 同样发布（inquiry 不创建询盘；REJECT 记录的保留期清理由 form 负责）
 * @param test        测试提交（后台“测试表单”）
 */
public record FormSubmittedEvent(EventMeta meta, long submissionId, long formId, String formKey,
                                 FormPurpose purpose, String locale, SpamVerdict spamVerdict,
                                 boolean test) implements DomainEvent {}
