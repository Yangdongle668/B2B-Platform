// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.taxonomy;
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

public interface TaxonomyApi {

    Optional<VocabularyInfo> vocabulary(String vocabularyKey);

    /** 当前站点的全部词汇表（内置 + 自定义），按 sort。 */
    List<VocabularyInfo> vocabularies();

    /** 适用于某内容类型的词汇表（applicable_types 包含该类型）。 */
    List<VocabularyInfo> vocabulariesFor(String contentType);

    Optional<TermInfo> findTerm(long termId);

    /** 批量查询（≤ 500）。 */
    Map<Long, TermInfo> findTerms(Collection<Long> termIds);

    /** 分类项在指定语言的视图（名称、落地页）。Delivery 热路径。 */
    Map<Long, TermView> termViews(Collection<Long> termIds, String locale);

    /** 祖先链（从根到父，不含自身）；面包屑与结构化数据使用。 */
    List<TermInfo> ancestors(long termId);

    /** 批量取祖先 ID（从根到父）；属性集继承、分配规则、图谱“同属一个父分类”使用。 */
    Map<Long, List<Long>> ancestorIds(Collection<Long> termIds);

    /** 直接子项；parentTermId 为空时返回根项。按 sort。 */
    List<TermInfo> children(String vocabularyKey, @Nullable Long parentTermId);

    /** termId 是否等于 ancestorTermId 或是其后代（询盘分配规则“产品分类”条件，系统设计 17.7）。 */
    boolean isSameOrDescendant(long termId, long ancestorTermId);

    /** 按源语言名称路径查找（产品导入，如 ["电池", "锂电池"]）；名称精确匹配。 */
    Optional<TermInfo> findBySourceNamePath(String vocabularyKey, List<String> sourceNames);

    /** 由 TERM_PAGE 内容反查分类项（落地页的面包屑、SEO 变量）。 */
    Optional<TermInfo> findByLandingContent(long contentId);

    /** 内容的草稿态指派，按词汇表、sort。 */
    List<TermAssignment> assignments(long contentId);

    /** 批量查询草稿态指派；vocabularyKey 为空表示全部词汇表。 */
    Map<Long, List<TermAssignment>> assignmentsOf(Collection<Long> contentIds, @Nullable String vocabularyKey);

    /**
     * 替换内容在某词汇表中的草稿态指派（编辑页“分类”标签、导入、批量指派）。加入调用方事务。
     * 校验：词汇表适用于该内容类型、multiple = false 时最多一项、hierarchical 词汇表的主分类恰好一个（产品必须有主分类，系统设计 7.3）。
     *
     * @throws BusinessException TAXONOMY_VOCABULARY_NOT_APPLICABLE、TAXONOMY_MULTIPLE_NOT_ALLOWED、TAXONOMY_PRIMARY_INVALID、TAXONOMY_TERM_NOT_FOUND
     */
    void replaceAssignments(long contentId, String vocabularyKey, List<AssignmentInput> assignments);
}
