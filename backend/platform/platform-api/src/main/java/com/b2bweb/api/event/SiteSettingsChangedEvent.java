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
 * 站点配置变化（系统设计 4.3）。一次保存一条，scopes 至少一项。
 *
 * @param settingKeys      SETTINGS：变化的设置键（{moduleId}.{名称}）；否则为空
 * @param localeChanges    LOCALES：逐项变化；否则为空
 * @param uiTextNamespaces UI_TEXT：变化的文案命名空间（如 "form"、"common"）；否则为空
 */
public record SiteSettingsChangedEvent(EventMeta meta, Set<SiteChangeScope> scopes, Set<String> settingKeys,
                                       List<LocaleChange> localeChanges, Set<String> uiTextNamespaces)
        implements DomainEvent {

    public SiteSettingsChangedEvent {
        if (scopes.isEmpty()) throw new IllegalArgumentException("scopes 不能为空");
        scopes = Set.copyOf(scopes);
        settingKeys = Set.copyOf(settingKeys);
        localeChanges = List.copyOf(localeChanges);
        uiTextNamespaces = Set.copyOf(uiTextNamespaces);
    }

    public boolean has(SiteChangeScope scope) { return scopes.contains(scope); }

    /** 某模块的设置是否变化（键以 "{moduleId}." 开头）。 */
    public boolean settingsOf(String moduleId) {
        return settingKeys.stream().anyMatch(k -> k.startsWith(moduleId + "."));
    }

    /** 某类语言变化涉及的语言代码。 */
    public Set<String> locales(LocaleChangeType type) {
        return localeChanges.stream().filter(c -> c.type() == type).map(LocaleChange::locale)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
