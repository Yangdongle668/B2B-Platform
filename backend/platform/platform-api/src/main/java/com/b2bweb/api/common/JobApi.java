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

public interface JobApi {

    /**
     * 提交任务（当前站点；系统级任务在没有站点上下文时提交，site_id = 0）。
     * idempotencyKey 非空时，同一站点、同一 jobType、同一 key 在任务保留期内只创建一次，重复提交返回已有任务。
     * 加入调用方事务：事务回滚则任务不存在；事务提交后执行器才可见。
     *
     * @throws com.b2bweb.api.common.BusinessException CORE_JOB_TYPE_UNKNOWN（没有对应的 JobHandler）
     */
    JobInfo submit(JobRequest request);

    /** 查询任务（当前站点）。 */
    java.util.Optional<JobInfo> find(long jobId);

    /** 当前站点的任务列表；排序字段白名单：createdAt（默认倒序）、finishedAt。 */
    PageResult<JobInfo> list(JobQuery query, PageRequest page);

    /** 请求取消：PENDING 直接取消；RUNNING 设置取消标记，由执行逻辑在检查点退出。其他状态抛出 CORE_JOB_STATE_INVALID。 */
    void requestCancel(long jobId);

    /** 重试失败的任务（FAILED → PENDING，attempts 不清零）。其他状态抛出 CORE_JOB_STATE_INVALID。 */
    JobInfo retry(long jobId);

    /**
     * 保存任务的输入文件（如导入用的 xlsx、图片 zip）到私有存储，7 天后自动删除。
     * 返回的 fileKey 放入 JobRequest.params，执行时用 JobExecutionContext.openInputFile 读取。
     */
    StoredFile storeInputFile(String fileName, String contentType, long size, java.io.InputStream content);

    /** 任务结果文件的短期签名下载链接；没有结果文件或已过期时抛出 CORE_JOB_NOT_FOUND。 */
    java.net.URI resultDownloadUrl(long jobId, java.time.Duration ttl);
}
