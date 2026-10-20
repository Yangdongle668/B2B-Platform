// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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

public record ContentRef(@com.fasterxml.jackson.annotation.JsonProperty("$content") long contentId) {
    public static final String URI_SCHEME = "content://";

    public String toUri() { return URI_SCHEME + contentId; }

    /** 解析 content://{id}；格式不符时返回 Optional.empty()。 */
    public static java.util.Optional<ContentRef> fromUri(String uri) {
        if (!uri.startsWith(URI_SCHEME)) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(new ContentRef(Long.parseLong(uri.substring(URI_SCHEME.length()))));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
