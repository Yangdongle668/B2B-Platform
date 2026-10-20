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
 * 彻底删除（系统设计 25.5）：内核数据已物理删除、路由已释放；各模块据此删除自己以 content_id 关联的数据。
 *
 * @param ownerModule 内容类型的所属模块（删除业务表的责任方）
 */
public record ContentPurgedEvent(EventMeta meta, long contentId, String contentType, String ownerModule,
                                 List<Long> localizationIds) implements DomainEvent {
    public ContentPurgedEvent { localizationIds = List.copyOf(localizationIds); }

    /** 停用模块同样必须清理自己的数据。 */
    @Override
    public boolean deliverWhenModuleDisabled() { return true; }
}
