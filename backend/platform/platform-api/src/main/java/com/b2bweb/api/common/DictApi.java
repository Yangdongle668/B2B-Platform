// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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

public interface DictApi {

    /** 启用的字典项，按 sortOrder。类型不存在时抛出 CORE_DICT_TYPE_NOT_FOUND。 */
    java.util.List<DictItem> items(String typeCode);

    /** 全部字典项（含停用，历史数据显示用）。 */
    java.util.List<DictItem> allItems(String typeCode);

    /** 字典值的标签（含停用项）；不存在时为空。 */
    java.util.Optional<String> label(String typeCode, String value);

    /** 字典值是否存在且启用（保存业务数据前校验）。 */
    boolean isValid(String typeCode, String value);

    /**
     * 幂等登记模块的内置字典类型与内置字典项（在 PlatformModule.onInstall / onUpgrade 中调用）：
     * 新增缺失的类型与项，更新内置项的标签与排序；不删除站点管理员添加的项。
     */
    void ensureBuiltin(DictTypeDefinition definition);
}
