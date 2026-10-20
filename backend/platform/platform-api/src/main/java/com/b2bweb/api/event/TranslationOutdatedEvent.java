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
 * 源语言发布后，MANUAL 本地化被标记为 OUTDATED（系统设计 5.5）。
 *
 * @param sourceTitle             源语言标题（中文；notification 不能回查 content，用于邮件正文）
 * @param sourceRevisionNo        新发布的源语言修订号
 * @param basedOnSourceRevisionNo 该本地化依据的源语言修订号；从未记录时为空
 */
public record TranslationOutdatedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                       String locale, String sourceTitle, int sourceRevisionNo,
                                       @Nullable Integer basedOnSourceRevisionNo) implements DomainEvent {}
