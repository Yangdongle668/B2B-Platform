// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.delivery;
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

public interface DataSourceProvider extends com.b2bweb.api.extension.KeyedExtension {

    /** 数据源 key，小写连字符，如 "content-query"。 */
    @Override
    String key();

    /** 中文名称（后台参数表单标题）。 */
    String label();

    /** 参数的 JSON Schema（draft 2020-12）；后台据此生成表单，delivery 据此校验参数。 */
    JsonNode paramsSchema();

    /** Schema 之外的业务校验（如分类项存在）；在保存布局与发布校验时调用。 */
    default void validateParams(JsonNode params, ValidationResult result) {}

    /**
     * 解析数据。结果中的对象引用一律使用 $content / $media / $term，由 delivery 统一解析为 refs 并生成 c / m / t 缓存标签；
     * 列表类缓存标签（list:{type}、g:{contentId}）由实现方放入 cacheTags。
     * 必须在 500 ms 内返回，最多 request.limit() 条（≤ 24）；只返回 request.locale() 已发布的内容（不跨语言回退）。
     */
    DataSourceResult resolve(DataSourceRequest request);
}
