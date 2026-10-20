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

public interface ObjectStorage {

    /**
     * 写入对象（同名覆盖）。size 必须等于流的实际长度，否则删除不完整的对象并抛出 IllegalArgumentException。
     * 调用方负责关闭 in。超时默认 120 秒。
     *
     * @throws ExternalServiceException INFRA_STORAGE_UNAVAILABLE（retryable = true）
     */
    void put(StorageKey key, InputStream in, long size, String contentType, Visibility visibility);

    /**
     * 读取对象；调用方负责关闭返回的流。
     *
     * @throws NotFoundException        INFRA_STORAGE_OBJECT_NOT_FOUND
     * @throws ExternalServiceException INFRA_STORAGE_UNAVAILABLE
     */
    InputStream get(StorageKey key, Visibility visibility);

    /** 对象元数据；不存在时为空。 */
    Optional<StoredObjectInfo> stat(StorageKey key, Visibility visibility);

    /** 删除；不存在时静默返回（幂等）。 */
    void delete(StorageKey key, Visibility visibility);

    /** 公开对象的 CDN URL（不访问网络；只用于 PUBLIC 桶中的对象）。 */
    URI publicUrl(StorageKey key);

    /**
     * 私有对象的预签名 GET 链接（不访问网络；只用于 PRIVATE 桶）。ttl 为 1 分钟–7 天。
     * downloadFileName 非空时设置 Content-Disposition: attachment; filename*=UTF-8''{编码后的文件名}。
     */
    URI presignedGet(StorageKey key, Duration ttl, @Nullable String downloadFileName);
}
