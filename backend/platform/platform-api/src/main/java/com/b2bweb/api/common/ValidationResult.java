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

/** 校验收集器。非线程安全：每次校验创建一个实例。 */
public final class ValidationResult {
    private final java.util.List<ValidationIssue> issues;
    private final @org.jspecify.annotations.Nullable String source;

    public ValidationResult() { this(null); }
    public ValidationResult(@org.jspecify.annotations.Nullable String source) { this(source, new java.util.ArrayList<>()); }
    private ValidationResult(@org.jspecify.annotations.Nullable String source, java.util.List<ValidationIssue> issues) {
        this.source = source; this.issues = issues;
    }

    public ValidationResult error(String code, String message) { return add(Severity.ERROR, code, null, message); }
    public ValidationResult error(String code, String field, String message) { return add(Severity.ERROR, code, field, message); }
    public ValidationResult warning(String code, String message) { return add(Severity.WARNING, code, null, message); }
    public ValidationResult warning(String code, String field, String message) { return add(Severity.WARNING, code, field, message); }
    public ValidationResult info(String code, String message) { return add(Severity.INFO, code, null, message); }

    public ValidationResult add(Severity severity, String code,
                                @org.jspecify.annotations.Nullable String field, String message) {
        issues.add(new ValidationIssue(severity, code, message, field, source));
        return this;
    }

    public ValidationResult addAll(java.util.Collection<ValidationIssue> more) { issues.addAll(more); return this; }

    /** 返回一个写入同一列表、但 source 为指定模块的视图（收集方为每个扩展点实现创建）。 */
    public ValidationResult forSource(String moduleId) { return new ValidationResult(moduleId, issues); }

    public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR); }
    public java.util.List<ValidationIssue> issues() { return java.util.List.copyOf(issues); }
    public java.util.List<ValidationIssue> errors() {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
    }
}
