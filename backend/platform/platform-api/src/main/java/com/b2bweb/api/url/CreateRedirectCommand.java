// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.url;
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

/** @param statusCode 301 / 302 / 308 / 410（GONE 时必须为 410） */
public record CreateRedirectCommand(@NotBlank @Size(max = 2048) String sourcePath, RedirectMatchType matchType,
                                    RedirectTargetKind targetKind, @Nullable Long targetLocalizationId,
                                    @Nullable @Size(max = 2048) String targetPath, int statusCode,
                                    RedirectOrigin origin, @Nullable @Size(max = 255) String note) {}
