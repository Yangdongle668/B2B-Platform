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

public interface VirusScanner {

    /**
     * 扫描文件内容。media 在异步任务中调用（不在请求线程中）；调用方负责关闭 content。超时默认 60 秒。
     *
     * @throws ExternalServiceException INFRA_VIRUS_SCANNER_UNAVAILABLE（retryable = true；media 保持 PROCESSING 并重试）
     */
    ScanResult scan(InputStream content, long size, String fileName);

    /** 扫描服务是否可用（健康检查、后台状态）。 */
    boolean available();
}
