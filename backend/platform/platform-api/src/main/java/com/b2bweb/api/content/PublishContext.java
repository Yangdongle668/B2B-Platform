// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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

/** 发布上下文。由 content 创建，同一次组装中所有贡献方共享（只读）。 */
public interface PublishContext {
    long siteId();
    long contentId();
    long localizationId();
    String contentType();
    String locale();
    TranslationMode translationMode();
    /** 源语言本地化 ID（源语言时为自身）。 */
    long sourceLocalizationId();
    AssemblyPurpose purpose();
    /** PUBLISH：将生成的修订号；RESTORE：被恢复的修订号；其他为 0。 */
    int revisionNo();
    /** PUBLISH 且该本地化从未发布过。 */
    boolean firstPublish();
    com.b2bweb.api.common.ActorRef actor();
    Instant now();
    /** 当前已发布快照中的某部分（seo 比较覆盖值、增量判断）；未发布为空。 */
    Optional<JsonNode> publishedPart(String partKey);
    /** 编辑器传入的未保存部分（PREVIEW 且 PreviewRequest.partOverrides 含该 key）；贡献方应以它代替草稿。 */
    Optional<JsonNode> overridePart(String partKey);
}
