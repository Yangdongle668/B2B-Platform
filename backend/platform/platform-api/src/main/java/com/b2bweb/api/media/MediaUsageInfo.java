// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.media;
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
 * 引用位置。
 *
 * @param contentId    引用方内容；资源引用为空
 * @param resourceType 引用方站点资源类型（如 "navigation.menu"、"core.site_profile"）；内容引用为空
 * @param part         快照部分或资源字段组，如 "product"、"layout"、"seo"
 * @param fieldPath    字段路径，如 "gallery[2]"、"sections[sec_01J9X4K2].props.image"
 */
public record MediaUsageInfo(long mediaId, @Nullable Long contentId, @Nullable String locale,
                             @Nullable String resourceType, @Nullable String resourceId,
                             String part, String fieldPath, boolean published) {}
