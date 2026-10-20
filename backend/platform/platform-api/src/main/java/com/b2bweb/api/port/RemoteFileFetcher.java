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

public interface RemoteFileFetcher {

    /**
     * 下载远程文件到临时文件（SSRF 防护，系统设计 26.3）：
     * 只允许 http / https；主机必须匹配 allowedHosts（支持 "*.example.com"）；DNS 解析后拒绝回环、私有、链路本地、组播、
     * 保留地址与云元数据地址，并直接连接已校验的 IP（防 DNS 重绑定）；每次重定向重新校验（最多 maxRedirects 次）；
     * 流式计数超过 maxBytes 立即中止；总耗时超过 timeout 中止。不得在持有数据库事务时调用。
     *
     * @throws BusinessException        INFRA_FETCH_HOST_NOT_ALLOWED、INFRA_FETCH_ADDRESS_FORBIDDEN、INFRA_FETCH_TOO_LARGE、
     *                                  INFRA_FETCH_TYPE_NOT_ALLOWED
     * @throws ExternalServiceException INFRA_FETCH_TIMEOUT（retryable = true）、
     *                                  INFRA_FETCH_HTTP_ERROR（5xx 与网络错误 retryable = true；4xx retryable = false）
     */
    FetchedFile fetch(RemoteFetchRequest request);
}
