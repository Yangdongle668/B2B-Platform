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

/** 图片预设（系统设计 12.3；OG 为框架补充，用于 og:image 与 JSON-LD）。 */
public enum ImagePreset {
    THUMB(ResizeMode.FILL, 1.0, 320),
    CARD(ResizeMode.FILL, 4.0 / 3, 640),
    CONTENT(ResizeMode.FIT, null, 1280),
    HERO(ResizeMode.FILL, 16.0 / 9, 1920),
    OG(ResizeMode.FILL, 1200.0 / 630, 1200);

    private final ResizeMode mode;
    private final @Nullable Double aspectRatio;
    private final int maxWidth;

    ImagePreset(ResizeMode mode, @Nullable Double aspectRatio, int maxWidth) {
        this.mode = mode; this.aspectRatio = aspectRatio; this.maxWidth = maxWidth;
    }
    public ResizeMode mode() { return mode; }
    /** 宽高比；FIT 为空（保持原比例）。FILL 时按焦点裁剪。 */
    public @Nullable Double aspectRatio() { return aspectRatio; }
    public int maxWidth() { return maxWidth; }

    /** 响应式标准宽度（系统设计 12.3）。 */
    public static final List<Integer> STANDARD_WIDTHS = List.of(320, 640, 960, 1280, 1920);
}
