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

public sealed interface PathReservation
        permits PathReservation.Available, PathReservation.Conflict, PathReservation.Invalid {

    /** @param previousOwnerLocalizationId 该路径以前属于的本地化（历史路径被占用时） */
    record Available(String path, @Nullable Long previousOwnerLocalizationId) implements PathReservation {}

    /** 路径已被其他本地化的路由占用。 */
    record Conflict(String path, long conflictContentId, long conflictLocalizationId,
                    String suggestedSlug) implements PathReservation {}

    /** 路径不合法：保留字（URL_PATH_RESERVED）、规则变量缺失（URL_PATTERN_INVALID）、长度超限等。 */
    record Invalid(String path, String code, String message) implements PathReservation {}
}
