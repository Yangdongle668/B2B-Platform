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

/** 内置词汇表 key（系统设计 7.2）。站点可以自定义词汇表。 */
public final class Vocabularies {
    private Vocabularies() {}
    public static final String PRODUCT_CATEGORY = "product_category";
    public static final String APPLICATION = "application";
    public static final String INDUSTRY = "industry";
    public static final String ARTICLE_CATEGORY = "article_category";
    public static final String TAG = "tag";
    public static final String CERTIFICATION = "certification";
}
