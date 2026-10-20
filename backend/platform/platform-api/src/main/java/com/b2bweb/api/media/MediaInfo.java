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
 * 媒体元数据（语言无关）。
 *
 * @param focalX / focalY 焦点（0–1），FILL 裁剪时使用；未设置为空
 * @param downloadGateFormKey 留资文档的门控表单 key
 */
public record MediaInfo(long mediaId, MediaKind kind, com.b2bweb.api.port.Visibility visibility, MediaStatus status,
                        String originalName, String mime, long size, String sha256,
                        @Nullable Integer width, @Nullable Integer height, @Nullable Integer durationSeconds,
                        @Nullable Double focalX, @Nullable Double focalY,
                        @Nullable String blurhash, @Nullable String dominantColor,
                        @Nullable Long folderId, @Nullable String downloadGateFormKey,
                        UploadSource source, Instant createdAt) {

    public boolean isImage() { return kind == MediaKind.IMAGE; }
    public boolean isReady() { return status == MediaStatus.READY; }
}
