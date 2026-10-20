// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.taxonomy;
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
 * 分类项（语言无关）。
 *
 * @param path             物化路径，如 "/1/5/9/"
 * @param landingContentId 可路由分类项的 TERM_PAGE 内容 ID
 * @param sourceName       源语言（中文）名称
 */
public record TermInfo(long termId, String vocabularyKey, @Nullable Long parentId, String path, int depth,
                       int sort, @Nullable Long landingContentId, String sourceName) {}
