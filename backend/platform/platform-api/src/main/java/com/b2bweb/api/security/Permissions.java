// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.security;
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

/** 权限码工具。 */
public final class Permissions {
    private Permissions() {}

    /** 内容生命周期动作（系统设计 6.3）。 */
    public enum ContentAction { EDIT, REVIEW, PUBLISH, DELETE }

    /**
     * 内容生命周期权限码：content:{type 小写}:{action 小写}，
     * 如 forContent("PRODUCT", PUBLISH) = "content:product:publish"，forContent("CASE_STUDY", EDIT) = "content:case_study:edit"。
     */
    public static String forContent(String contentType, ContentAction action) {
        return "content:" + contentType.toLowerCase(java.util.Locale.ROOT) + ":" + action.name().toLowerCase(java.util.Locale.ROOT);
    }
}
