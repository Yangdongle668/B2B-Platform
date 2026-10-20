// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
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

public interface SearchEngineClient {

    /** 确保索引存在并应用设置（幂等；设置未变化时不触发重新索引）。返回引擎任务 ID；无需变更时为空。 */
    Optional<Long> ensureIndex(String indexName, SearchIndexSettings settings);

    /** 删除索引；不存在时静默返回。 */
    void deleteIndex(String indexName);

    /** 以 prefix 开头的现有索引名（如 "s1_"）。 */
    List<String> listIndexes(String prefix);

    /** 新增或替换文档（按主键），异步生效，返回任务 ID。文档为 JSON 对象且带字符串主键 id；单批 ≤ 1000 个。 */
    long upsert(String indexName, List<JsonNode> documents);

    /** 按 ID 删除文档（不存在的 ID 忽略）；单批 ≤ 1000 个。返回任务 ID。 */
    long delete(String indexName, Collection<String> documentIds);

    /** 按过滤表达式删除（如模块停用时删除某类型的全部文档）。返回任务 ID。 */
    long deleteByFilter(String indexName, String filter);

    /** 底层引擎的交换能力；ADR-021的V1搜索重建禁止调用，必须发布私有不可变代次并CAS数据库指针。返回任务 ID。 */
    long swap(String indexName, String otherIndexName);

    /** 等待任务结束；超过 timeout 时返回当前状态。 */
    SearchTaskStatus await(long taskId, Duration timeout);

    /** 查询；超时默认 2 秒。 */
    SearchResponse search(String indexName, SearchRequest request);

    /** 文档数量（每晚的数量比对，系统设计 19）。 */
    long count(String indexName);

    /** M0新增草案，ADR-021：仅浏览不可变代次，游标稳定、每批≤1000；见4.10.4。 */
    CursorPage<JsonNode> scanDocuments(String indexName, CursorRequest cursor);
}
