// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.form;
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

/** 来源归因（系统设计 17.9），服务端已校验并截断。 */
public record SubmissionAttribution(@Nullable String landingUrl, @Nullable String referrer,
                                    @Nullable String utmSource, @Nullable String utmMedium,
                                    @Nullable String utmCampaign, @Nullable String utmTerm,
                                    @Nullable String utmContent, @Nullable String gclid,
                                    @Nullable String msclkid, @Nullable String fbclid,
                                    @Nullable JsonNode firstTouch, @Nullable JsonNode lastTouch,
                                    List<Long> viewedContentIds, @Nullable String device) {
    public SubmissionAttribution { viewedContentIds = List.copyOf(viewedContentIds); }
}
