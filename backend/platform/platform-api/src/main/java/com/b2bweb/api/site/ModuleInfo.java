// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.site;
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
 * 模块的平台级信息。
 *
 * @param layer   "L0" / "L2" / "L3" / "L4"
 * @param version 已安装版本；未安装成功时为构建中的版本
 */
public record ModuleInfo(String moduleId, String name, String layer, String version, boolean disableable,
                         ModuleStatus status, @org.jspecify.annotations.Nullable String apiPrefix,
                         @org.jspecify.annotations.Nullable String tablePrefix) {}
