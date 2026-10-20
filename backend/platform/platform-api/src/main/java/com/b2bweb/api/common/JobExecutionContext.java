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

public interface JobExecutionContext {

    long jobId();

    /** 任务所属站点；系统级任务为 0。 */
    long siteId();

    /** 发起人。 */
    ActorRef initiator();

    /** 第几次执行（从 1 开始）。 */
    int attempt();

    tools.jackson.databind.JsonNode params();

    <T> T params(Class<T> type);

    /** 上报进度（同时作为心跳）；节流写库（每秒最多一次）。 */
    void progress(@org.jspecify.annotations.Nullable Integer total, int done, int failed,
                  @org.jspecify.annotations.Nullable String message);

    /** 检查点：已请求取消时抛出 JobCanceledException（任务置为 CANCELED），否则刷新心跳。 */
    void checkpoint();

    boolean isCancelRequested();

    /** 读取 JobApi.storeInputFile 保存的输入文件；调用方负责关闭流。 */
    java.io.InputStream openInputFile(String fileKey);

    /** 写入结果文件（如错误报告 xlsx、导出文件）；同一任务只保留最后一个结果文件。 */
    void writeResultFile(String fileName, String contentType,
                         ThrowingConsumer<java.io.OutputStream> writer) throws java.io.IOException;

    @FunctionalInterface
    interface ThrowingConsumer<T> { void accept(T t) throws java.io.IOException; }
}
