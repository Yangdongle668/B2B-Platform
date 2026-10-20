// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
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

/** 平台异常基类。detail 为中文说明；properties 进入 problem+json 的扩展字段（如 conflictContentId）。 */
public abstract class PlatformException extends RuntimeException {
    private final ErrorCode errorCode;
    private final java.util.List<FieldError> errors;
    private final java.util.Map<String, Object> properties;

    protected PlatformException(ErrorCode errorCode, String detail,
                                java.util.List<FieldError> errors,
                                java.util.Map<String, Object> properties,
                                @org.jspecify.annotations.Nullable Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.errors = java.util.List.copyOf(errors);
        this.properties = java.util.Map.copyOf(properties);
    }

    public ErrorCode errorCode() { return errorCode; }
    public String detail() { return getMessage(); }
    public java.util.List<FieldError> errors() { return errors; }
    public java.util.Map<String, Object> properties() { return properties; }
}
