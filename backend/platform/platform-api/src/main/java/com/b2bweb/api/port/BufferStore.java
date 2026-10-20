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

public interface BufferStore {

    // ───── 计数器 ─────

    /** 累加计数（如 bucket = "url.not_found"，member = 路径）。member 最长 2048 个字符。 */
    void increment(String bucket, String member, long delta);

    /** 原子取出并清零一个桶中的计数，最多 maxMembers 个（其余留到下次）；定时任务据此批量写库。 */
    Map<String, Long> drainCounters(String bucket, int maxMembers);

    // ───── 延迟集合（合并触发） ─────

    /**
     * 登记一个待处理成员，到期时间 = 当前时间 + delay；成员已存在时保留较早的到期时间，窗口内的重复触发合并为一次。
     * 例：graph.recompute（contentId，30 秒）、seo.indexnow（URL，1 分钟）。
     */
    void schedule(String queue, String member, Duration delay);

    /** 原子取出已到期的成员（按到期时间升序，最多 limit 个）。处理失败时由调用方重新 schedule。 */
    List<String> pollDue(String queue, int limit);

    /** 队列中的成员数量（指标与积压告警）。 */
    long size(String queue);
}
