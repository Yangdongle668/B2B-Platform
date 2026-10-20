// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.extension;
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
 * @param key                 KeyedExtension 的 key；其他为空
 * @param enabledInCurrentSite 所属模块在当前站点是否启用（没有站点上下文时为 false）
 */
public record ExtensionInfo(String point, String implementationClass, String moduleId,
                            java.util.Set<String> contentTypes, int order,
                            @org.jspecify.annotations.Nullable String key,
                            boolean enabledInCurrentSite) {
    public ExtensionInfo { contentTypes = java.util.Set.copyOf(contentTypes); }
}
