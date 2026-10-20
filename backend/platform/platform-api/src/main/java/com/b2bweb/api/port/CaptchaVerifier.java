// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
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

public interface CaptchaVerifier {

    /**
     * 服务端校验令牌。超时默认 5 秒。令牌只能校验一次（重复校验返回 success = false）。
     * form 还需校验 result.hostname 属于本站域名、result.action 与表单 key 一致。
     *
     * @throws ExternalServiceException INFRA_CAPTCHA_UNAVAILABLE（retryable = true）；服务不可用时是否放行由 form 决定
     */
    CaptchaResult verify(CaptchaVerification verification);
}
