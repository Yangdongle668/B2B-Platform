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
 * 图片的响应式来源（前台输出 picture / srcset）。
 *
 * @param width / height 按预设裁剪后的最大输出尺寸（用于 width / height 属性，防止 CLS）
 * @param fallbackUrl    原格式（JPEG / PNG）在默认宽度的 URL
 * @param srcsets        AVIF、WEBP 与原格式各一组
 */
public record ImageSources(long mediaId, ImagePreset preset, int width, int height,
                           @Nullable String alt, @Nullable String blurhash, @Nullable String dominantColor,
                           URI fallbackUrl, Map<ImageFormat, List<ImageCandidate>> srcsets) {
    public ImageSources {
        var copy = new java.util.EnumMap<ImageFormat, List<ImageCandidate>>(ImageFormat.class);
        srcsets.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        srcsets = java.util.Collections.unmodifiableMap(copy);
    }
}
