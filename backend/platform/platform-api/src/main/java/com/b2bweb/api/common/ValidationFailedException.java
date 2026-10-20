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

/** 存在 ERROR 时由收集方抛出（HTTP 422），problem+json 的 errors[] 来自 ERROR 级问题，properties.issues 含全部问题。 */
public class ValidationFailedException extends PlatformException {
    private final java.util.List<ValidationIssue> issues;

    public ValidationFailedException(ErrorCode code, String detail, java.util.List<ValidationIssue> issues) {
        super(code, detail,
              issues.stream().filter(i -> i.severity() == Severity.ERROR)
                    .map(i -> new FieldError(i.field() == null ? "" : i.field(), i.code(), i.message())).toList(),
              java.util.Map.of("issues", issues), null);
        this.issues = java.util.List.copyOf(issues);
    }

    public java.util.List<ValidationIssue> issues() { return issues; }
}
