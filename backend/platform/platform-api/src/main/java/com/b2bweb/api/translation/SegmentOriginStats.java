// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.translation;
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
 * 译文来源统计（系统设计 5.6、5.8）。
 *
 * @param memoryHits 本次生成时命中翻译记忆的单元数（含 HUMAN / REVIEWED / MACHINE 条目）
 */
public record SegmentOriginStats(long localizationId, int total, int human, int reviewed, int machine,
                                 int memoryHits, int needsReview) {
    /** 是否仍有未经人工修订或确认的机器译文。 */
    public boolean hasUnreviewedMachine() { return machine > 0; }
}
