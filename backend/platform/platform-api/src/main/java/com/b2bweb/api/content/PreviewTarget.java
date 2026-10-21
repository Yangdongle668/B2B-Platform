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

public record PreviewTarget(long siteId, long contentId, long localizationId, String locale, PreviewMode mode,
                            Instant expiresAt, PreviewPurpose purpose, @Nullable Long issuedBy) {
    public PreviewTarget {
        Objects.requireNonNull(mode); Objects.requireNonNull(expiresAt); Objects.requireNonNull(purpose);
        if (purpose == PreviewPurpose.EDITOR && (!(mode instanceof PreviewMode.Draft) || issuedBy == null || issuedBy <= 0))
            throw new IllegalArgumentException("编辑器令牌必须绑定用户与草稿模式");
    }
}
