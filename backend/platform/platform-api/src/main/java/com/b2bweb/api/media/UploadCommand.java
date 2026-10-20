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

/**
 * @param declaredContentType 客户端声明的 MIME（只作参考，以 Tika 识别结果为准）
 * @param allowedKinds        允许的类型；空表示按 source 的默认白名单
 * @param maxBytes            更严格的大小上限；为空表示按 source 的默认上限
 */
public record UploadCommand(@NotBlank @Size(max = 255) String fileName, @Nullable String declaredContentType,
                            @Positive long size, com.b2bweb.api.port.Visibility visibility, UploadSource source,
                            @Nullable Long folderId, boolean reuseDuplicate, Set<MediaKind> allowedKinds,
                            @Nullable Long maxBytes) {
    public UploadCommand { allowedKinds = Set.copyOf(allowedKinds); }
}
