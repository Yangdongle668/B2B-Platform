// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.form;
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

/** 标准字段 key（映射到询盘，系统设计 17.1）。 */
public final class FormFieldKeys {
    private FormFieldKeys() {}
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String COMPANY = "company";
    public static final String COUNTRY = "country";
    public static final String JOB_TITLE = "job_title";
    public static final String MESSAGE = "message";
    public static final String QUANTITY = "quantity";
    public static final String WEBSITE = "website";
}
