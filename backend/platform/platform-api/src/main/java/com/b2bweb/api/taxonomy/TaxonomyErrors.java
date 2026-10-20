// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.taxonomy;
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

public final class TaxonomyErrors {
    private TaxonomyErrors() {}
    public static final ErrorCode VOCABULARY_NOT_FOUND      = ErrorCode.of("TAXONOMY_VOCABULARY_NOT_FOUND", 404, "词汇表不存在");
    public static final ErrorCode TERM_NOT_FOUND            = ErrorCode.of("TAXONOMY_TERM_NOT_FOUND", 404, "分类项不存在");
    public static final ErrorCode VOCABULARY_NOT_APPLICABLE = ErrorCode.of("TAXONOMY_VOCABULARY_NOT_APPLICABLE", 422, "该词汇表不适用于此内容类型");
    public static final ErrorCode MULTIPLE_NOT_ALLOWED      = ErrorCode.of("TAXONOMY_MULTIPLE_NOT_ALLOWED", 422, "该词汇表只允许选择一项");
    public static final ErrorCode PRIMARY_INVALID           = ErrorCode.of("TAXONOMY_PRIMARY_INVALID", 422, "必须且只能有一个主分类");
    public static final ErrorCode TERM_HAS_ASSIGNMENTS      = ErrorCode.of("TAXONOMY_TERM_HAS_ASSIGNMENTS", 409, "分类项仍有内容指派，请先转移");
}
