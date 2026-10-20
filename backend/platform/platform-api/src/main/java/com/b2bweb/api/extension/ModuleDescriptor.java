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
 * module.json 的描述（字段与附录 A 的 Schema 一致；未列出的字段保留在 raw 中）。
 *
 * @param layer    "L0" / "L2" / "L3" / "L4"
 * @param platform 兼容的平台版本范围，如 ">=1.0.0 <2.0.0"
 * @param requires 必需依赖：模块 ID → 版本范围
 * @param optional 可选依赖：模块 ID → 版本范围
 */
public record ModuleDescriptor(String id, String name, com.b2bweb.api.common.SemVer version, String layer,
                               String platform, Map<String, String> requires, Map<String, String> optional,
                               boolean disableable, @Nullable String tablePrefix, @Nullable String apiPrefix,
                               List<String> permissions, Set<String> publishes, Set<String> subscribes,
                               JsonNode raw) {
    public ModuleDescriptor {
        requires = Map.copyOf(requires);
        optional = Map.copyOf(optional);
        permissions = List.copyOf(permissions);
        publishes = Set.copyOf(publishes);
        subscribes = Set.copyOf(subscribes);
    }
}
