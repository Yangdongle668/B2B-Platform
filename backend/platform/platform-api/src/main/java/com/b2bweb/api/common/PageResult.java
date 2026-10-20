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

public record PageResult<T>(java.util.List<T> items, int page, int size, long total) {
    public PageResult { items = java.util.List.copyOf(items); }

    public static <T> PageResult<T> empty(PageRequest request) {
        return new PageResult<>(java.util.List.of(), request.page(), request.size(), 0L);
    }

    public <R> PageResult<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, total);
    }
}
