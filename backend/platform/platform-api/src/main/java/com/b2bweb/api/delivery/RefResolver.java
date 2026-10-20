// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.delivery;
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

/** 引用解析器：只解析当前站点、当前语言的已发布对象（内容不跨语言回退）。 */
public interface RefResolver {

    /** 解析所用的语言。 */
    String locale();

    /** 内容在当前语言的已发布版本；未发布或不存在时为空。 */
    java.util.Optional<ResolvedContent> content(long contentId);

    /** 批量解析内容；结果只含已发布的内容。 */
    java.util.Map<Long, ResolvedContent> contents(java.util.Collection<Long> contentIds);

    /** 图片：按预设生成 URL（公开图片），带当前语言的 alt。 */
    java.util.Optional<ResolvedImage> image(long mediaId, com.b2bweb.api.media.ImagePreset preset);

    /** 公开文件（PDF 等）的下载 URL；私有文件返回空。 */
    java.util.Optional<ResolvedFile> file(long mediaId);

    /** 分类项在当前语言的名称与落地页路径。 */
    java.util.Optional<ResolvedTerm> term(long termId);

    /** 站内路径转为绝对 URL（主域名 https + 尾斜杠策略）。 */
    String absoluteUrl(String path);

    /** 至今解析过的全部引用（收集方据此生成缓存标签与 refs）。 */
    com.b2bweb.api.common.ReferenceSet touched();
}
