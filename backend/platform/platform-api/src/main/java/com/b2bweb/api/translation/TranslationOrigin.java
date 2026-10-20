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

/** 翻译记忆条目来源，优先级 HUMAN > REVIEWED > MACHINE（系统设计 5.6）。 */
public enum TranslationOrigin {
    MACHINE(1), REVIEWED(2), HUMAN(3);
    private final int priority;
    TranslationOrigin(int priority) { this.priority = priority; }
    public int priority() { return priority; }
    /** 机器译文永远不能覆盖 HUMAN / REVIEWED。 */
    public boolean canBeOverwrittenBy(TranslationOrigin other) { return other.priority >= this.priority; }
}
