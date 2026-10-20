// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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

public interface SettingsApi {

    /** 当前站点的设置值，映射为 type（通常是声明模块中的 record）。未声明的键抛出 CORE_SETTING_NOT_DECLARED。 */
    <T> T get(String key, Class<T> type);

    /** 当前站点的设置值（JSON，顶层带 schemaVersion）。 */
    tools.jackson.databind.JsonNode getJson(String key);

    /** 指定站点的设置值（系统级任务使用）。 */
    <T> T get(long siteId, String key, Class<T> type);

    /** 系统级设置（site_id = 0），如 core.smtp、core.dsr。 */
    <T> T getSystem(String key, Class<T> type);

    /**
     * 写入当前站点的设置（按 Schema 校验，敏感字段自动加密）。
     * expectedVersion 为空表示不做乐观锁检查（只用于 onInstall 写入初始值）。
     *
     * @return 新的版本号
     * @throws com.b2bweb.api.common.ValidationException CORE_SETTING_INVALID
     * @throws com.b2bweb.api.common.VersionConflictException CORE_SETTING_VERSION_CONFLICT
     */
    int put(String key, tools.jackson.databind.JsonNode value, @org.jspecify.annotations.Nullable Integer expectedVersion);

    /** 写入系统级设置；规则同 put。 */
    int putSystem(String key, tools.jackson.databind.JsonNode value, @org.jspecify.annotations.Nullable Integer expectedVersion);

    /** 恢复默认（删除当前站点的保存值）。 */
    void reset(String key);

    /** 当前站点某设置的元信息。 */
    SettingMeta meta(String key);
}
