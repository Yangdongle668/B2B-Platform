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

public interface PersonalDataProvider extends KeyedExtension {

    /** 数据集 key：{模块 ID}.{名称}，如 "inquiry.inquiries"、"form.submissions"。 */
    @Override
    String key();

    /** 中文名称（数据主体请求报告中的分组标题）。 */
    String label();

    /** 查找：返回该站点中与邮箱相关的记录摘要；没有时 recordCount = 0。 */
    PersonalDataSummary find(PersonalDataRequest request);

    /** 导出（访问、可携带）：结构化 JSON，字段名为英文 camelCase，值为原文。 */
    JsonNode export(PersonalDataRequest request);

    /** 更正联系字段；返回更新的记录数。 */
    int rectify(PersonalDataRequest request, PersonalDataCorrection correction);

    /** 删除（匿名化）；返回处理的记录数。 */
    int erase(PersonalDataRequest request);

    /** 反对：撤销营销同意；返回处理的记录数。 */
    default int objectToMarketing(PersonalDataRequest request) { return 0; }

    /** 限制处理：标记或取消标记（被限制的记录不再用于跟进与通知）；返回处理的记录数。 */
    default int restrict(PersonalDataRequest request, boolean restricted) { return 0; }
}
