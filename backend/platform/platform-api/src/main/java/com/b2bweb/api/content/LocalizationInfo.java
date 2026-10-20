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

/**
 * 本地化（cnt_localization）。
 *
 * @param pubPath               当前发布路径（投影，与 url_route 同步）；未发布为空
 * @param sourceRevisionNo      生成译文时依据的源语言修订号（目标语言）
 * @param hasUnpublishedChanges draft_hash ≠ pub_hash
 */
public record LocalizationInfo(
        long localizationId, long contentId, String contentType, String locale,
        @Nullable String slug, String draftTitle, DraftState draftState, PublishState publishState,
        @Nullable Long publishedRevisionId, @Nullable Integer publishedRevisionNo,
        @Nullable Instant firstPublishedAt, @Nullable Instant publishedAt,
        @Nullable Instant scheduledPublishAt, @Nullable Instant scheduledUnpublishAt,
        UnpublishStrategy unpublishStrategy, @Nullable Long unpublishRedirectContentId,
        TranslationMode translationMode, TranslationState translationState,
        @Nullable String sourceLocale, @Nullable Integer sourceRevisionNo,
        @Nullable String pubPath, boolean hasUnpublishedChanges, int version, Instant updatedAt,
        boolean translationReviewRequired) {

    public boolean isSource() { return translationMode == TranslationMode.SOURCE; }
    public boolean isPublished() { return publishState == PublishState.PUBLISHED; }
}
