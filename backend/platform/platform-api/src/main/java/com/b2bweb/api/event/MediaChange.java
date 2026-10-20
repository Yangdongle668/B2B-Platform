// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.event;
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

/** 媒体属性变化。 */
public enum MediaChange {
    /** 源语言 alt、标题、说明或文件名（目标语言文本经翻译回写） */
    METADATA,
    /** 焦点 */
    FOCAL_POINT,
    /** 状态：病毒扫描完成（PROCESSING → READY / QUARANTINED） */
    STATUS,
    /** 所在文件夹 */
    FOLDER,
    /** 留资文档的门控表单（MediaApi.setDownloadGate） */
    DOWNLOAD_GATE
}
