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

/** 已下载的文件。调用方必须关闭（try-with-resources），关闭时删除临时文件。 */
public interface FetchedFile extends AutoCloseable {
    /** 跟随重定向后的最终 URL。 */
    URI finalUrl();
    @Nullable String contentType();
    long size();
    /** 文件名：取自 Content-Disposition，否则取 URL 路径最后一段；都没有时为空。 */
    @Nullable String fileName();
    /** 打开读取流（可多次打开，调用方关闭）。 */
    InputStream open();
    @Override
    void close();
}
