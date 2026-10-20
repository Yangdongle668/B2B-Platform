// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.media;
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

public interface MediaReferenceCollector extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 内容某本地化的草稿（含语言无关数据）引用的媒体。只收集站点资源的实现覆盖 supports 返回 false。 */
    default List<MediaReference> collectDraft(MediaReferenceContext ctx) { return List.of(); }

    /** 本实现负责的站点资源类型（TranslatableResourceTypes 中的值）。 */
    default Set<String> resourceTypes() { return Set.of(); }

    /** 某站点资源引用的媒体。 */
    default List<MediaReference> collectResource(String resourceType, String resourceId) { return List.of(); }
}
