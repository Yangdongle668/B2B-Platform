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

public interface PublishContributor extends com.b2bweb.api.extension.ContentTypeScoped {

    /**
     * 快照部分的 key：SnapshotParts.CARD / TERMS 为顶层保留部分，其他进入 payload.parts.{partKey}。
     * 同一内容类型内唯一。
     */
    String partKey();

    /** 校验草稿（必填、引用存在、布局 Schema 等）。只追加问题，不抛出业务异常。 */
    void validate(PublishContext ctx, ValidationResult result);

    /**
     * 产出本部分（只包含本内容自身的数据；其他内容、媒体、分类只保存 $content / $media / $term 引用）。
     * 返回 null 表示没有该部分。结果必须是确定性的：同一草稿多次调用得到相同的 JSON（数组顺序稳定）。
     */
    @Nullable JsonNode contribute(PublishContext ctx);

    /**
     * 回滚：把历史快照中的本部分恢复为本模块的草稿（ctx.purpose() = RESTORE）。
     * 历史快照中没有本部分时不调用。修订的 schemaVersion 较旧时由实现方迁移。
     */
    void restore(PublishContext ctx, JsonNode part);
}
