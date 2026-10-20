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

/**
 * @param expectedVersion 翻译/交互保存必填；仅持有父锁的受控导入可空，禁止用null绕过翻译冲突
 * @param title             新的草稿标题；为空表示不变
 * @param slug              新的 slug；为空表示不变（已发布的本地化修改 slug 时，下次发布自动生成 301）
 * @param sharedDataChanged 语言无关的数据是否也变化了（见 updateDraftMeta）
 */
public record UpdateDraftMetaCommand(long localizationId, @Nullable Integer expectedVersion,
                                     @Nullable @Size(max = 255) String title,
                                     @Nullable @Size(max = 80) String slug,
                                     boolean sharedDataChanged) {}
