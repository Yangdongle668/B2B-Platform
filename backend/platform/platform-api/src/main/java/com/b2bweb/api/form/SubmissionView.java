// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.form;
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
 * 提交记录（frm_submission）。
 *
 * @param fields            字段值：key → 值（文本为字符串；多选为数组；电话为 E.164；国家为 ISO 3166-1 alpha-2）
 * @param items             产品选择字段（询价篮或当前产品）
 * @param attachmentMediaIds 附件（私有媒体）
 * @param downloadMediaId   DOWNLOAD 表单对应的留资文档
 * @param ip                原始 IP（保留 30 天后置空，系统设计 17.9）
 * @param test              测试提交（后台“测试表单”或测试标记）
 */
public record SubmissionView(long submissionId, long formId, String formKey, FormPurpose purpose, String locale,
                             JsonNode fields, List<SubmittedItem> items, List<Long> attachmentMediaIds,
                             @Nullable Long downloadMediaId, SubmissionAttribution attribution, PrivacyRecord privacy,
                             int spamScore, SpamVerdict spamVerdict, @Nullable String ip, @Nullable String ipCountry,
                             @Nullable String userAgent, boolean test, Instant createdAt) {
    public SubmissionView {
        items = List.copyOf(items);
        attachmentMediaIds = List.copyOf(attachmentMediaIds);
    }

    /** 标准字段的文本值；不存在或为空时为空。 */
    public Optional<String> text(String fieldKey) {
        JsonNode v = fields.get(fieldKey);
        return v == null || v.isNull() || v.asString().isBlank() ? Optional.empty() : Optional.of(v.asString());
    }
}
