// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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

/** 快照结构常量（系统设计 6.4）。 */
public final class SnapshotParts {
    private SnapshotParts() {}
    /** 快照结构版本（payload.schemaVersion）。 */
    public static final int SCHEMA_VERSION = 1;
    /** 快照大小上限（序列化后的字节数）。 */
    public static final int MAX_BYTES = 1_048_576;
    /** 顶层：卡片 { title, summary, coverMediaId }，由内容类型的所属模块提供；投影到 pub_title / pub_summary / pub_cover_media_id。 */
    public static final String CARD = "card";
    /** 顶层：发布态分类指派 [{ vocabulary, termId, primary, sort }]，由 taxonomy 提供；投影到 cnt_published_term。 */
    public static final String TERMS = "terms";
    /** parts.layout：模板与插槽或完整布局文档（page）。 */
    public static final String LAYOUT = "layout";
    /** parts.seo：最终值、自动值、被覆盖字段列表（seo，系统设计 14.1）。 */
    public static final String SEO = "seo";
}
