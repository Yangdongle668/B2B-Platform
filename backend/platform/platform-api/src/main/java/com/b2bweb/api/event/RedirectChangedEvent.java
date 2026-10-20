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
 * 手工与导入的重定向变化（addRedirect、后台编辑与删除、CSV 导入）。applyRoute / retireRoute 自动维护的 AUTO 重定向
 * 不发布本事件（由 ContentPathChangedEvent 覆盖）。
 *
 * @param redirectId 单条变化时的重定向 ID；批量导入时为空
 * @param sourcePath 单条变化时的源路径（EXACT 为规范化路径，PREFIX 为前缀，REGEX 为表达式）；批量时为空
 * @param matchType  单条变化时的匹配方式；批量时为空
 * @param count      本次变化的条数（单条为 1）
 */
public record RedirectChangedEvent(EventMeta meta, ChangeKind change, @Nullable Long redirectId,
                                   @Nullable String sourcePath, @Nullable RedirectMatchType matchType,
                                   int count) implements DomainEvent {}
