// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.media;
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

public interface MediaApi {

    /**
     * 上传文件（流式，单文件 ≤ 50 MB；表单附件 ≤ 20 MB，由 command.maxBytes 收紧）。流程见系统设计 12.2：
     * 扩展名白名单 → Tika 识别真实 MIME（与扩展名不符则拒绝）→ sha256 → 图片尺寸、blurhash、主色；SVG 净化 →
     * 文档与压缩包异步病毒扫描（status = PROCESSING，扫描后 READY / QUARANTINED 并发布 MediaUpdatedEvent）→ 存储 → 发布 MediaUploadedEvent。
     * reuseDuplicate 为 true 且当前站点已有相同 sha256、相同可见性的媒体时，直接返回已有媒体（reused = true）。
     * 调用方负责关闭 content。
     *
     * @throws BusinessException MEDIA_TYPE_NOT_ALLOWED、MEDIA_TOO_LARGE、MEDIA_MIME_MISMATCH、MEDIA_SVG_FORBIDDEN（非管理员上传 SVG）
     */
    UploadResult upload(@Valid UploadCommand command, InputStream content);

    /**
     * 从远程 URL 导入（产品导入的图片 URL，系统设计 8.6）：经 RemoteFileFetcher（4.7.13）下载，域名必须在站点白名单中，
     * 其余同 upload。不得在持有数据库事务时调用（网络 I/O）。
     *
     * @throws BusinessException MEDIA_REMOTE_FETCH_FAILED（不在白名单、内网地址、超时、超限）
     */
    UploadResult uploadFromUrl(@Valid RemoteUploadCommand command);

    Optional<MediaInfo> find(long mediaId);

    /** 批量查询（≤ 500）；发布校验中检查引用的媒体存在、类型正确、READY。 */
    Map<Long, MediaInfo> findAll(Collection<Long> mediaIds);

    /** 媒体在指定语言的视图（含 alt、标题、说明；公开文件带下载 URL）。Delivery 热路径。 */
    Map<Long, MediaView> views(Collection<Long> mediaIds, String locale);

    /**
     * 图片的响应式来源：按预设与标准宽度（320、640、960、1280、1920，不超过原图宽度）生成 AVIF / WebP / 原格式的签名 URL
     * （系统设计 12.3）。非图片、私有或非 READY 的媒体不出现在结果中。Delivery 热路径。
     */
    Map<Long, ImageSources> imageSources(Collection<Long> mediaIds, ImagePreset preset, String locale);

    /** 单个图片 URL（JSON-LD、OG、邮件）。width 取不小于它的最近标准宽度。 */
    Optional<URI> imageUrl(long mediaId, ImagePreset preset, int width, ImageFormat format);

    /** 公开文件（PDF 等）的 CDN 下载 URL；私有文件为空。 */
    Optional<URI> publicFileUrl(long mediaId);

    /**
     * 私有文件的短期预签名下载链接（员工下载表单与询盘附件；调用方记录审计，系统设计 17.3）。ttl ≤ 1 小时。
     *
     * @throws BusinessException MEDIA_NOT_READY（扫描未完成）、MEDIA_QUARANTINED、MEDIA_NOT_PRIVATE
     */
    URI privateDownloadUrl(long mediaId, Duration ttl, @Nullable String downloadFileName);

    /**
     * 设置留资文档的门控表单（框架补充：product 保存文档的 gated_form_key 时同步到 media；为空表示取消门控）。
     * 只允许私有文件。
     */
    void setDownloadGate(long mediaId, @Nullable String formKey);

    /**
     * 签发留资文档票据：同站点、未软删、PRIVATE/READY、门控表单匹配，且mda_usage存在
     * 此contentId/locale的PUBLISHED文档引用。仅本地签名，无对象存储/SMTP网络调用。
     * 签名包括site/submission/media/content/locale/formKey/当前download_gate_version/固定expiresAt。
     * form把返回URL及expiresAt写入自身download_grant，数据库提交后才可返回/发送；详见4.10.6。
     */
    DownloadTicket issueGatedDownload(@Valid GatedDownloadCommand command);

    /** 媒体的引用位置（来自 mda_usage）。 */
    List<MediaUsageInfo> usages(long mediaId);

    /**
     * 普通资产检查引用后移入回收站，文件保留30天；到期由清理任务物理删除。
     * force只清理由调用模块管理且无受保护引用的私有附件，不能忽略mda_usage。
     * 排他媒体行锁保护状态变化；引用共享锁内必须确认deleted_at为空。幂等。
     */
    void delete(long mediaId, boolean force);
}
