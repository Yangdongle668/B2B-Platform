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
 * @param bodyText   去除标记的正文（≤ 10,000 个字符，超出由 search 截断）
 * @param termIds    参与筛选的分类项（发布态）
 * @param attributes 分面字段：key 为不带 "attr." 前缀的属性 key，值为字符串、数值、布尔或其数组
 * @param modelNos   型号（关闭拼写容错的字段）
 * @param sort       业务排序值（可排序字段 sort）
 */
public record SearchDocumentFields(String title, @Nullable String summary, String bodyText, Set<Long> termIds,
                                   JsonNode attributes, List<String> modelNos, @Nullable Long coverMediaId,
                                   int sort) {
    public SearchDocumentFields {
        termIds = Set.copyOf(termIds);
        modelNos = List.copyOf(modelNos);
    }
}
