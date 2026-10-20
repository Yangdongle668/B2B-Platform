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
 * 对象键（不含桶名）。格式：
 * 媒体    {siteId}/{yyyy}/{mm}/{mediaId}-{slug 化的原文件名}.{ext}（系统设计 12.2）；
 * 任务文件 jobs/{siteId}/{yyyy}/{mm}/{fileId}-{slug 化的文件名}.{ext}（框架补充；系统级任务 siteId 为 0）。
 * 只允许小写字母、数字、"."、"_"、"-" 与 "/"；不得以 "/" 开头，不得包含 ".." 或 "//"；最长 512。
 */
public record StorageKey(String value) {
    private static final java.util.regex.Pattern FORMAT = java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9._\\-/]{0,511}$");

    public StorageKey {
        if (!FORMAT.matcher(value).matches() || value.contains("..") || value.contains("//")) {
            throw new IllegalArgumentException("非法的对象键: " + value);
        }
    }

    /** 媒体对象键。safeName 为 slug 化后的文件名主体（空时用 "file"），ext 为小写扩展名（不含点）。 */
    public static StorageKey media(long siteId, java.time.YearMonth month, long mediaId, String safeName, String ext) {
        return new StorageKey(siteId + "/" + month.getYear() + "/" + String.format("%02d", month.getMonthValue())
                + "/" + mediaId + "-" + (safeName.isBlank() ? "file" : safeName) + "." + ext);
    }

    /** 任务文件对象键。 */
    public static StorageKey jobFile(long siteId, java.time.YearMonth month, long fileId, String safeName, String ext) {
        return new StorageKey("jobs/" + media(siteId, month, fileId, safeName, ext).value());
    }

    /** 是否属于该站点（媒体键以 {siteId}/ 开头，任务文件键以 jobs/{siteId}/ 开头）。 */
    public boolean belongsToSite(long siteId) {
        return value.startsWith(siteId + "/") || value.startsWith("jobs/" + siteId + "/");
    }
}
