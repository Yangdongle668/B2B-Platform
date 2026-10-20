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

/**
 * 任务执行逻辑。用 @Extension(module = "…") 注册；key() 即 jobType。
 * 执行器在任务所属站点的上下文中、以发起人身份调用；不在事务中调用，实现方按需开启短事务（如每个产品一个事务，系统设计 8.6）。
 * 抛出 PlatformException 时任务 FAILED，errorCode / errorMessage 取自异常；其他异常记为 CORE_JOB_FAILED。
 */
public interface JobHandler extends com.b2bweb.api.extension.KeyedExtension {

    /** 任务类型 {moduleId}.{名称}。 */
    String jobType();

    @Override
    default String key() { return jobType(); }

    JobOutcome execute(JobExecutionContext ctx) throws Exception;

    /** 心跳超时：超过该时间没有 progress / checkpoint 调用，视为实例失联。 */
    default java.time.Duration heartbeatTimeout() { return java.time.Duration.ofMinutes(5); }
}
