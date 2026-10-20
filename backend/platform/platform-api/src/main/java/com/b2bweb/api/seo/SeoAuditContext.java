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

public interface SeoAuditContext {
    long siteId();
    long contentId();
    long localizationId();
    String contentType();
    String locale();
    /** 当前发布的快照。 */
    JsonNode snapshot();
    /** 该页面的头部输出（与 DeliveryContributor 的 seo 部分同源）。 */
    SeoHead head();
    /** 入站链接（seo_link，不计 sitemap）。 */
    List<SeoLinkRef> inboundLinks();
    /** 出站链接。 */
    List<SeoLinkRef> outboundLinks();
    /** 全站索引；只在 SITE 范围的全站检查中可用，PAGE 检查中调用抛出 IllegalStateException。 */
    SeoSiteIndex siteIndex();
    com.b2bweb.api.delivery.RefResolver refs();
}
