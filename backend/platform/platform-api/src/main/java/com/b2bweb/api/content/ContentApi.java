// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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

public interface ContentApi {

    // ───────────── 创建与草稿 ─────────────

    /**
     * 已有内容保存前调用：先锁cnt_content，再锁目标cnt_localization并核对expectedVersion。
     * 返回当前本地化；锁保持到调用方事务结束。必须先于所属模块业务表写入/贡献方apply。
     * MANDATORY，使用当前读；跨站/不存在拒绝。多内容操作按contentId排序；路由事务锁在本方法之前。
     */
    LocalizationInfo lockDraftForWrite(long localizationId, int expectedVersion);


    /**
     * 创建内容及其源语言本地化（系统设计 6.1）。业务模块在保存自己业务表的同一事务中调用。
     * <p>contentType 必须已由 ContentTypeProvider 注册且所属模块在当前站点启用；locale 必须为源语言 zh-CN。
     * slug 为空时：可路由类型由 title 经 UrlApi.generateSlug 生成（发布前可修改）；不可路由类型为空。
     * 内容的 owner_module 取自 ContentTypeDefinition.module。发布 ContentCreatedEvent、LocalizationCreatedEvent。
     *
     * @throws BusinessException   CONTENT_TYPE_UNKNOWN、CONTENT_LOCALE_NOT_SOURCE
     * @throws ValidationException CONTENT_SLUG_INVALID
     */
    ContentCreated create(@Valid CreateContentCommand command);

    /**
     * 为已有内容创建 MANUAL 模式的目标语言本地化（为某个市场独立编辑，系统设计 5.5）。
     * AUTO 模式的本地化只由 translation 经 ensureTargetLocalization 创建。发布 LocalizationCreatedEvent。
     *
     * @throws BusinessException CONTENT_LOCALE_EXISTS、CONTENT_LOCALE_NOT_ENABLED（站点未启用该语言）
     */
    LocalizationInfo createLocalization(@Valid CreateLocalizationCommand command);

    /**
     * 保存草稿后同步元数据：递增本地化 version，draft_state 置为 EDITING（IN_REVIEW / APPROVED 时退回 EDITING，系统设计 6.3），
     * 更新 draft_title / slug，重算 draft_hash；sharedDataChanged 为 true 时同时重算同一内容其他本地化的 draft_hash
     * （语言无关数据变化，如型号、图集；其他本地化的 version 同样递增）。发布 ContentDraftChangedEvent。
     *
     * @throws VersionConflictException CONTENT_VERSION_CONFLICT（properties.currentVersion）
     * @throws BusinessException        CONTENT_TRASHED
     * @throws ValidationException      CONTENT_SLUG_INVALID
     */
    DraftMetaUpdated updateDraftMeta(@Valid UpdateDraftMetaCommand command);

    /**
     * 语言无关的草稿数据被非编辑器途径修改后（分类指派、导入、批量操作），重算该内容全部本地化的 draft_hash 并递增 version。
     * 不做乐观锁检查；为每个本地化发布 ContentDraftChangedEvent。
     */
    void markDraftChanged(long contentId);

    // ───────────── 生命周期（系统设计 6.3） ─────────────

    /** 提交审核：EDITING → IN_REVIEW；站点对该类型未启用审核时抛出 CONTENT_REVIEW_NOT_ENABLED。发布 ReviewRequestedEvent。 */
    void submitForReview(long localizationId, int expectedVersion, @Nullable String comment);

    /** 审核通过：IN_REVIEW → APPROVED。发布 ReviewCompletedEvent。权限 review。 */
    void approve(long localizationId, int expectedVersion, @Nullable String comment);

    /** 驳回：IN_REVIEW → EDITING，comment 必填。发布 ReviewCompletedEvent。权限 review。 */
    void reject(long localizationId, int expectedVersion, String comment);

    /**
     * 发布（流程见本节开头）。前置条件：未进回收站；不需要审核或已 APPROVED（system 身份的译文自动发布按 6.7 的站点配置）。
     * WARNING / INFO 级问题不阻止发布，随结果返回。
     *
     * @throws ValidationFailedException CONTENT_PUBLISH_VALIDATION_FAILED（存在 ERROR 级问题）
     * @throws BusinessException         CONTENT_SLUG_CONFLICT（properties：conflictContentId、suggestedSlug）、
     *                                   CONTENT_REVIEW_REQUIRED、CONTENT_SNAPSHOT_TOO_LARGE、CONTENT_TRASHED
     * @throws VersionConflictException  CONTENT_VERSION_CONFLICT
     */
    PublishResult publish(@Valid PublishCommand command);

    /** 设置定时发布和 / 或定时下线（时间为 UTC，须晚于当前时间）；前置条件同 publish（定时发布到点时再执行一次完整校验）。 */
    LocalizationInfo schedule(@Valid ScheduleCommand command);

    /** 取消定时发布与定时下线。 */
    LocalizationInfo cancelSchedule(long localizationId, int expectedVersion);

    /**
     * 下线：PUBLISHED → UNPUBLISHED，经 UrlApi.retireRoute 按下线策略处理路由（系统设计 13.6）。
     * 源语言下线时，AUTO 模式的目标语言本地化在同一事务中跟随下线（策略相同）。发布 ContentUnpublishedEvent（每个本地化一条）。
     *
     * @throws BusinessException CONTENT_STATE_INVALID（未发布）、CONTENT_REDIRECT_TARGET_INVALID（REDIRECT_CONTENT 的目标不存在或是自身）
     */
    void unpublish(@Valid UnpublishCommand command);

    /** 下线内容的全部已发布本地化（删除可路由分类项、产品停产下线等），以 system 或当前用户身份执行；返回下线的本地化数量。 */
    int unpublishAll(long contentId, UnpublishStrategy strategy, @Nullable Long redirectContentId);

    /** 回滚：调用各 PublishContributor.restore 把历史修订恢复为草稿（需要再次发布）；递增 version。 */
    void rollback(long localizationId, int revisionNo, int expectedVersion);

    /**
     * 移入回收站（内容级）。前置条件：没有已发布的本地化（源语言下线后 AUTO 跟随下线；仍有已发布的 MANUAL 本地化时抛出
     * CONTENT_HAS_PUBLISHED_LOCALIZATION）。发布 ContentTrashedEvent。
     */
    void trash(long contentId);

    /** 从回收站恢复。发布 ContentRestoredEvent。 */
    void restore(long contentId);

    /**
     * 彻底删除（物理删除内核数据；回收站中 ≥ 30 天，或拥有 delete 权限的用户显式确认）。
     * 先经 UrlApi.releaseRoutes 释放路由，再发布 ContentPurgedEvent，各模块据此清理自己的表（系统设计 25.5）。
     */
    void purge(long contentId);

    /** 删除一个未发布的非源语言本地化。发布 LocalizationDeletedEvent。 */
    void deleteLocalization(long localizationId);

    /** 发布前校验（编辑器侧栏展示）：执行发布流程的校验部分，不写库。 */
    List<ValidationIssue> validateForPublish(long localizationId);

    /** 影响分析：收集 ContentImpactProvider（4.5.4）。locale 为空表示内容级操作（TRASH、PURGE）。 */
    ImpactReport analyzeImpact(long contentId, @Nullable String locale, ImpactAction action);

    // ───────────── 翻译（translation 调用，系统设计 5.7） ─────────────

    /**
     * 幂等确保目标语言本地化存在：不存在时创建（translation_mode = AUTO、translation_state = NONE、slug 为空——
     * 首次发布时由译文标题生成，系统设计 13.3），并发布 LocalizationCreatedEvent；已存在时原样返回（含 MANUAL）。
     *
     * @throws BusinessException CONTENT_LOCALE_NOT_ENABLED、CONTENT_LOCALE_IS_SOURCE、CONTENT_TRASHED
     */
    LocalizationInfo ensureTargetLocalization(long contentId, String targetLocale);

    /**
     * 更新翻译状态与所依据的源语言修订号。sourceRevisionNo 小于已记录值时忽略本次更新（过期写入）并返回 false。
     * 只允许目标语言本地化；不改变 version、不触发草稿事件。
     */
    boolean updateTranslationState(@Valid TranslationStateUpdate update);

    /** 切换翻译模式（AUTO ⇄ MANUAL，“脱离 / 恢复自动翻译”）。发布 TranslationModeChangedEvent。 */
    LocalizationInfo changeTranslationMode(long localizationId, TranslationMode mode, @Nullable Integer expectedVersion);

    // ───────────── 读取：内容与本地化 ─────────────

    Optional<ContentInfo> findContent(long contentId);

    /** 批量查询（≤ 500 个 ID，4.1.3）。 */
    Map<Long, ContentInfo> findContents(Collection<Long> contentIds);

    Optional<LocalizationInfo> findLocalization(long localizationId);

    Optional<LocalizationInfo> findLocalization(long contentId, String locale);

    Map<Long, LocalizationInfo> findLocalizations(Collection<Long> localizationIds);

    /** 内容的全部本地化，源语言在前，其余按站点语言 sortOrder。 */
    List<LocalizationInfo> localizations(long contentId);

    /** 当前站点可用的内容类型（所属模块已启用）。 */
    List<ContentTypeDefinition> contentTypes();

    Optional<ContentTypeDefinition> contentType(String contentType);

    // ───────────── 读取：快照与投影（前台只读发布态，系统设计 6.4） ─────────────

    /** 本地化当前发布的快照；未发布、已下线或内容在回收站中时为空。Delivery 热路径。 */
    Optional<PublishedSnapshot> findPublishedSnapshot(long localizationId);

    /** 同上，按内容与语言查询；该语言没有已发布的本地化时为空（不跨语言回退）。 */
    Optional<PublishedSnapshot> findPublishedSnapshot(long contentId, String locale);

    /** 指定修订的快照（不论当前状态；回滚预览、翻译依据的源语言修订、审计）。 */
    Optional<PublishedSnapshot> findRevision(long localizationId, int revisionNo);

    /** 修订列表；排序白名单：revisionNo（默认倒序）。 */
    PageResult<RevisionInfo> revisions(long localizationId, PageRequest page);

    /** 内容在指定语言的已发布卡片（投影）；未发布的 ID 不出现在结果中。Delivery 热路径。 */
    Map<Long, PublishedCard> findPublishedCards(Collection<Long> contentIds, String locale);

    /** 内容在所有语言的已发布版本（hreflang、语言切换器；是否对外公开由调用方按 SiteApi 过滤）。Delivery 热路径。 */
    List<PublishedCard> publishedVersions(long contentId);

    /** 按条件查询已发布内容（content-query 数据源的同一语义；search 不可用时的降级列表）。limit ≤ 100。 */
    List<PublishedCard> queryPublished(@Valid PublishedQuery query);

    /** 全量扫描已发布本地化（sitemap、搜索重建、图谱全量计算、批量翻译）；按 localizationId 升序，游标稳定。 */
    CursorPage<PublishedCard> scanPublished(@Valid PublishedScan scan, CursorRequest cursor);

    /** 发布态的分类指派（cnt_published_term）。 */
    Map<Long, List<PublishedTermRef>> publishedTerms(Collection<Long> contentIds, String locale);

    /**
     * 与给定分类项至少共享一个发布态指派的内容 ID（图谱候选集，系统设计 15.3）：同站点、可路由、未进回收站；
     * 按共享数量降序，最多 limit（≤ 2000）个。
     */
    List<Long> contentIdsByPublishedTerms(Collection<Long> termIds, String locale, Set<String> contentTypes, int limit);

    /** 各内容类型在当前站点已发布的本地化数量（停用影响分析）。 */
    Map<String, Long> countPublished(Set<String> contentTypes);

    // ───────────── 预览（系统设计 6.5） ─────────────

    /** 兼容入口：只签发PREVIEW（≤1小时）或SHARE（>1小时），绝不推断EDITOR。 */
    default PreviewToken createPreviewToken(long localizationId, PreviewMode mode, @Nullable Duration ttl) {
        return createPreviewToken(localizationId, mode,
                ttl != null && ttl.compareTo(Duration.ofHours(1)) > 0 ? PreviewPurpose.SHARE : PreviewPurpose.PREVIEW, ttl);
    }

    /**
     * 显式用途签发。PREVIEW/SHARE/EDITOR最长分别1小时/7天/12小时，空ttl默认1小时/1小时/12小时。
     * EDITOR只允许Draft，绑定当前用户并校验站点成员及edit权限/语言范围；其余后台入口同样自行校验编辑权限。
     */
    PreviewToken createPreviewToken(long localizationId, PreviewMode mode, PreviewPurpose purpose, @Nullable Duration ttl);

    /** 校验签名/签发行、撤销、有效期、站点和内容可见性；EDITOR另复核签发人当前编辑权限/语言范围。无效时为空。 */
    Optional<PreviewTarget> resolvePreviewToken(String token);

    /**
     * 从草稿（或指定修订）临时组装快照，与发布同一套代码（AssemblyPurpose.PREVIEW），不持久化、不发布事件。
     * partOverrides仅用于验证过用途的EDITOR/Draft；V1只接受声明的layout并经对应贡献方/Schema校验，AUTO目标只读。
     */
    PreviewSnapshot assemblePreview(@Valid PreviewRequest request);

    /** M0新增草案，ADR-022；content任务读取当前树，事务内提交，见4.10.5。 */
    JobInfo requestRouteRecalculation(Set<String> locales);

    /** 内容级策略：锁cnt_content并比较其version，变化时使各本地化旧审批/版本失效；见4.10.7。 */
    ContentInfo setTranslationReviewRequirement(long contentId, int expectedContentVersion, boolean required);
}
