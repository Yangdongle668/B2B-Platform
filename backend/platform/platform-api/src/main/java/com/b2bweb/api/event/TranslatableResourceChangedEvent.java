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
 * 站点资源的源语言（中文）可翻译文本变化（系统设计 4.3、5.7；CLAUDE.md 语言规则 5）。
 * 所属模块在保存资源的同一事务中发布；只在可翻译文本实际变化（新建、修改）或资源删除时发布，
 * 只改非文本属性（排序、链接、图片、开关）时不发布。
 *
 * @param resourceType TranslatableResourceTypes 中的值（4.5.21）
 * @param resourceId   含义见 TranslatableResourceTypes 的注释
 */
public record TranslatableResourceChangedEvent(EventMeta meta, String resourceType, String resourceId,
                                               ResourceChange change) implements DomainEvent {}
