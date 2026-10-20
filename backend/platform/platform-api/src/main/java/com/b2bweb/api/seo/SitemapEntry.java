// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.seo;
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
 * @param path       站内路径（含语言前缀）
 * @param alternates hreflang → 站内路径（xhtml:link）
 * @param imageMediaIds image:image（公开图片）
 */
public record SitemapEntry(String path, @Nullable Instant lastModified, Map<String, String> alternates,
                           List<Long> imageMediaIds) {
    public SitemapEntry {
        alternates = Map.copyOf(alternates);
        imageMediaIds = List.copyOf(imageMediaIds);
    }
}
