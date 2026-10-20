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

public record JobInfo(long jobId, String moduleId, String jobType, String title, JobStatus status,
                      @org.jspecify.annotations.Nullable Long parentJobId,
                      JobProgress progress,
                      tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode result,
                      @org.jspecify.annotations.Nullable JobFile resultFile,
                      @org.jspecify.annotations.Nullable String errorCode,
                      @org.jspecify.annotations.Nullable String errorMessage,
                      int attempts, int maxAttempts, boolean cancelRequested,
                      long createdBy, java.time.Instant createdAt,
                      java.time.@org.jspecify.annotations.Nullable Instant startedAt,
                      java.time.@org.jspecify.annotations.Nullable Instant finishedAt) {}
