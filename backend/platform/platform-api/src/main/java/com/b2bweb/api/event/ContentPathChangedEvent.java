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
 * 内容路由变化（系统设计 4.3、13.4）。路径未变化的 applyRoute 不发布；releaseRoutes（彻底删除）由 ContentPurgedEvent 覆盖。
 *
 * @param previousPath 变化前的路径（含语言前缀）；首次发布为空
 * @param currentPath  变化后的路径；RETIRED 为空
 * @param retireMode   RETIRED 时为 GONE（410）或 REDIRECT（301）；其他为空
 */
public record ContentPathChangedEvent(EventMeta meta, long contentId, long localizationId, String locale,
                                      @Nullable String previousPath, @Nullable String currentPath,
                                      PathChangeReason reason, @Nullable RetireMode retireMode) implements DomainEvent {}
