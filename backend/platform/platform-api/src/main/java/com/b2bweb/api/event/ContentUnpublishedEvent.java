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
 * 下线（ContentApi.unpublish / unpublishAll、定时下线）：每个本地化一条。
 *
 * @param lastRevisionNo    下线前发布的修订号（订阅方据此判断过期）
 * @param previousPath      下线前的路径；不可路由类型为空
 * @param redirectContentId strategy = REDIRECT_CONTENT 时的目标内容
 * @param followedSource    AUTO 本地化随源语言下线（系统设计 5.7）
 */
public record ContentUnpublishedEvent(EventMeta meta, long contentId, String contentType, String locale,
                                      long localizationId, int lastRevisionNo, @Nullable String previousPath,
                                      UnpublishStrategy strategy, @Nullable Long redirectContentId,
                                      boolean followedSource) implements DomainEvent {}
