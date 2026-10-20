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

public interface SecretCipher {

    /** 用当前密钥加密为密文信封。 */
    String encrypt(com.b2bweb.api.common.Secret plaintext);

    /** 解密信封；格式错误、密钥不存在或认证失败时抛出 CORE_SECRET_DECRYPT_FAILED。 */
    com.b2bweb.api.common.Secret decrypt(String envelope);

    /** 是否为本平台的密文信封。 */
    boolean isEnvelope(String value);

    /** 信封是否使用了非当前密钥（密钥轮换任务据此重新加密）。 */
    boolean needsReEncryption(String envelope);

    /** 掩码显示：只保留末 4 位，如 "****abcd"；长度 ≤ 8 时全部掩码。 */
    String mask(com.b2bweb.api.common.Secret plaintext);
}
