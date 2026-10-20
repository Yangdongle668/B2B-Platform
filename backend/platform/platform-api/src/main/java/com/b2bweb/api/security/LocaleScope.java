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

/** 语言范围：unrestricted 为 true 时不限语言。 */
public record LocaleScope(boolean unrestricted, java.util.Set<String> locales) {
    public LocaleScope { locales = java.util.Set.copyOf(locales); }

    public static LocaleScope all() { return new LocaleScope(true, java.util.Set.of()); }

    public static LocaleScope of(java.util.Set<String> locales) { return new LocaleScope(false, locales); }

    public boolean allows(String locale) { return unrestricted || locales.contains(locale); }
}
