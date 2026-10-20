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
 * @param path         发布后的路径（含语言前缀）；不可路由类型为空
 * @param previousPath 上一次发布的路径；首次发布或路径未变时与 path 相同或为空
 * @param contentChanged payload_hash 是否变化（为 false 时 content_modified_at 不更新）
 * @param warnings     WARNING / INFO 级校验问题
 */
public record PublishResult(long localizationId, long revisionId, int revisionNo,
                            @Nullable String path, @Nullable String previousPath,
                            boolean firstPublish, boolean contentChanged,
                            List<ValidationIssue> warnings) {
    public PublishResult { warnings = List.copyOf(warnings); }
}
