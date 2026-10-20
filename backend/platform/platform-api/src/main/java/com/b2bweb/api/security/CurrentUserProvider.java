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

public interface CurrentUserProvider {

    /** 当前登录的后台用户；前台请求、事件监听、任务中为空。 */
    java.util.Optional<CurrentUser> current();

    /** 当前登录的后台用户；没有时抛出 CORE_NOT_AUTHENTICATED（401）。 */
    CurrentUser require();

    /** 当前操作人：有登录用户时为该用户；否则为 ActorRef.SYSTEM（任务执行器以任务发起人身份执行时返回发起人）。 */
    com.b2bweb.api.common.ActorRef actor();
}
