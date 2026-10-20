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
 * @param companyName   对外公司名称（可翻译）
 * @param tagline       一句话介绍（可翻译）
 * @param openingHours  营业时间说明（可翻译）
 */
public record SiteProfileView(
        String locale,
        @org.jspecify.annotations.Nullable String companyName,
        @org.jspecify.annotations.Nullable String tagline,
        @org.jspecify.annotations.Nullable Long logoMediaId,
        @org.jspecify.annotations.Nullable Long logoInverseMediaId,
        @org.jspecify.annotations.Nullable Long faviconMediaId,
        @org.jspecify.annotations.Nullable Long appIconMediaId,
        @org.jspecify.annotations.Nullable String email,
        @org.jspecify.annotations.Nullable String phone,
        @org.jspecify.annotations.Nullable String whatsapp,
        java.util.List<Address> addresses,
        java.util.List<SocialLink> socialLinks,
        @org.jspecify.annotations.Nullable String openingHours,
        @org.jspecify.annotations.Nullable Integer foundedYear,
        LegalInfo legal) {

    public SiteProfileView {
        addresses = java.util.List.copyOf(addresses);
        socialLinks = java.util.List.copyOf(socialLinks);
    }

    /** @param type HEADQUARTERS / OFFICE / FACTORY / WAREHOUSE / SHOWROOM / OTHER；@param label 可翻译 */
    public record Address(String type, @org.jspecify.annotations.Nullable String label, String street,
                          @org.jspecify.annotations.Nullable String postalCode, String city,
                          @org.jspecify.annotations.Nullable String region, String countryCode,
                          @org.jspecify.annotations.Nullable String phone) {}

    /** @param platform LINKEDIN / FACEBOOK / YOUTUBE / X / INSTAGRAM / TIKTOK / WECHAT / OTHER */
    public record SocialLink(String platform, String url) {}

    /** 法定信息（Imprint，系统设计 16.3）；不翻译，内容由业主确认（32 章 Q14）。 */
    public record LegalInfo(@org.jspecify.annotations.Nullable String legalName,
                            @org.jspecify.annotations.Nullable String registeredAddress,
                            java.util.List<String> representatives,
                            @org.jspecify.annotations.Nullable String registerAuthority,
                            @org.jspecify.annotations.Nullable String registerNumber,
                            @org.jspecify.annotations.Nullable String vatId,
                            @org.jspecify.annotations.Nullable String supervisoryAuthority,
                            @org.jspecify.annotations.Nullable String contentResponsible,
                            @org.jspecify.annotations.Nullable String legalEmail,
                            @org.jspecify.annotations.Nullable String legalPhone) {
        public LegalInfo { representatives = java.util.List.copyOf(representatives); }
    }
}
