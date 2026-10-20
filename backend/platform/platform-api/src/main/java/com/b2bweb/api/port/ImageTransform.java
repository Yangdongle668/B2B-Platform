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

/**
 * @param height  FILL 时必填（由宽度 × 预设宽高比计算）；FIT 时为空（保持原比例）
 * @param focalX  焦点 0–1；为空时居中
 * @param quality 1–100；为空时用适配器默认值（AVIF 60、WebP 75、JPEG 80）
 */
public record ImageTransform(StorageKey source, ResizeMode mode, int width, @Nullable Integer height,
                             @Nullable Double focalX, @Nullable Double focalY, ImageFormat format,
                             @Nullable Integer quality) {
    public ImageTransform {
        if (width < 1 || width > 4096) throw new IllegalArgumentException("width 必须在 1–4096 之间");
        if (mode == ResizeMode.FILL && height == null) throw new IllegalArgumentException("FILL 必须指定 height");
        if (quality != null && (quality < 1 || quality > 100)) throw new IllegalArgumentException("quality 必须在 1–100 之间");
    }
}
