// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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

public record ModuleDisablePolicy(ModuleDisableAction action,
                                  @org.jspecify.annotations.Nullable Long redirectContentId) {
    public ModuleDisablePolicy {
        java.util.Objects.requireNonNull(action);
        if (action == ModuleDisableAction.REDIRECT && (redirectContentId == null || redirectContentId <= 0)
            || action == ModuleDisableAction.NOT_FOUND && redirectContentId != null)
            throw new IllegalArgumentException("模块停用路由策略不合法");
    }
    public static ModuleDisablePolicy notFound() { return new ModuleDisablePolicy(ModuleDisableAction.NOT_FOUND, null); }
}
