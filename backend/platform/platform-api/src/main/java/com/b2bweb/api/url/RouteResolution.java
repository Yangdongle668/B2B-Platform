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

/** 请求路径解析结果。 */
public sealed interface RouteResolution permits RouteResolution.Content, RouteResolution.Redirect,
        RouteResolution.Gone, RouteResolution.NotFound {

    record Content(long contentId, long localizationId, String locale, String path) implements RouteResolution {}

    /** @param status 301 / 302 / 308；location 为站内路径或外部绝对 URL */
    record Redirect(int status, String location, RedirectReason reason) implements RouteResolution {}

    record Gone(String path) implements RouteResolution {}

    record NotFound(String path) implements RouteResolution {}
}
