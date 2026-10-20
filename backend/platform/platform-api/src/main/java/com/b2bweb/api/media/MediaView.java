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
 * 媒体在某语言的视图。
 *
 * @param alt / title / caption 该语言的文本；没有译文时为空（不回退）
 * @param publicUrl             公开文件的下载 URL；图片请用 imageSources；私有文件为空
 */
public record MediaView(long mediaId, String locale, MediaKind kind, com.b2bweb.api.port.Visibility visibility,
                        MediaStatus status, String fileName, String mime, long size,
                        @Nullable Integer width, @Nullable Integer height,
                        @Nullable String alt, @Nullable String title, @Nullable String caption,
                        @Nullable String blurhash, @Nullable String dominantColor,
                        @Nullable URI publicUrl) {}
