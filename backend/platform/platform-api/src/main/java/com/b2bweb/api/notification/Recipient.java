// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.notification;
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

/** 收件人。 */
public sealed interface Recipient permits Recipient.StaffUser, Recipient.Customer {
    /** 后台用户（邮箱取自用户目录） */
    record StaffUser(long userId) implements Recipient {}
    /** 客户（询盘联系人等） */
    record Customer(@Email @NotBlank String email, @Nullable String name) implements Recipient {}
}
