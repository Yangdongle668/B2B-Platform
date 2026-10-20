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

public interface DistributedLock {

    /** 立即尝试获取；已被持有时返回空。 */
    Optional<LockHandle> tryAcquire(String name, Duration lease);

    /** 在 wait 内重试获取（50–200 ms 随机间隔）；仍未获取时返回空。 */
    Optional<LockHandle> tryAcquire(String name, Duration wait, Duration lease);

    /** 获取锁后执行 action 并释放；没有获取到锁时不执行，返回 false。 */
    default boolean runIfAcquired(String name, Duration wait, Duration lease, Runnable action) {
        Optional<LockHandle> handle = tryAcquire(name, wait, lease);
        if (handle.isEmpty()) return false;
        try (LockHandle ignored = handle.get()) {
            action.run();
            return true;
        }
    }
}
