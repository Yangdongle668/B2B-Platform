// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.extension;
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

/** 平台级生命周期上下文（安装、升级、彻底删除）；没有站点上下文。 */
public interface ModuleContext {
    String moduleId();
    /** 本次安装 / 升级的目标版本。 */
    com.b2bweb.api.common.SemVer version();
    /** 全部未归档站点的 ID。 */
    List<Long> siteIds();
    /** 依次以每个未归档站点为上下文执行（SiteContextProvider.runAs），用于为已有站点初始化数据。 */
    void forEachSite(java.util.function.LongConsumer action);
    com.b2bweb.api.common.ActorRef actor();
}
