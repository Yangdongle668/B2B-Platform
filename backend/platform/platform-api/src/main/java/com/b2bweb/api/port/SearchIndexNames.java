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

/** 索引命名（系统设计 5.3、19）。 */
public final class SearchIndexNames {
    private SearchIndexNames() {}

    /** s{site}_{locale}_content，如 s1_de_content、s1_zh-CN_content。 */
    public static String content(long siteId, String locale) { return "s" + siteId + "_" + locale + "_content"; }

    /** 全量重建用的临时索引名。 */
    public static String rebuilding(String indexName) { return indexName + "_rebuild"; }

    /** 某站点全部索引的名称前缀。 */
    public static String sitePrefix(long siteId) { return "s" + siteId + "_"; }
}
