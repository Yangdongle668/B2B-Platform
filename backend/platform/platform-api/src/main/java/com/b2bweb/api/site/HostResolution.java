// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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
 * Host 解析结果。
 *
 * @param locale V2 语言独立域名（domainType = LOCALE）对应的语言；V1 为空
 */
public record HostResolution(long siteId, String host, DomainType domainType, String primaryDomain,
                             @org.jspecify.annotations.Nullable String locale, SiteStatus status) {
    /** 别名域名需要 301 到主域名 https（系统设计 13.2 第 1 步）。 */
    public boolean redirectToPrimary() { return domainType == DomainType.ALIAS; }
}
