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

public interface CdnPurger {

    /**
     * 按缓存标签清除（标签必须属于同一站点）。V1可靠路径直接调用，不经BufferStore去抖；
     * 适配器按服务商上限拆分批次（如每次 30 个标签）。不支持标签清除的 CDN：适配器把请求转为该站点全部域名的整站清除。
     *
     * @throws ExternalServiceException INFRA_CDN_PURGE_FAILED（retryable = true）
     */
    void purgeTags(Collection<CacheTag> tags);

    /** 按绝对 URL 清除（重定向响应、sitemap 文件等）；单批 ≤ 500 个。 */
    void purgeUrls(Collection<URI> urls);

    /** 是否支持按标签清除（后台状态页显示；为 false 时缓存失效粒度退化为整站）。 */
    boolean supportsTagPurge();
}
