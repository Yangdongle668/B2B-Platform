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

public interface SchemaOrgContext {
    long siteId();
    String locale();
    com.b2bweb.api.delivery.DeliveryMode mode();
    /** 当前内容；站点级页面（如搜索页）为空。 */
    Optional<Long> contentId();
    Optional<String> contentType();
    Optional<JsonNode> snapshot();
    /** 当前页面的规范 URL。 */
    String pageUrl();
    /** {origin}/#organization */
    String organizationId();
    /** {origin}/#website */
    String websiteId();
    /** {pageUrl}#webpage */
    String webPageId();
    /** {pageUrl}#breadcrumb */
    String breadcrumbId();
    /** {pageUrl}#{fragment}，如 nodeId("product") */
    String nodeId(String fragment);
    List<com.b2bweb.api.delivery.BreadcrumbItem> breadcrumbs();
    com.b2bweb.api.delivery.RefResolver refs();
}
