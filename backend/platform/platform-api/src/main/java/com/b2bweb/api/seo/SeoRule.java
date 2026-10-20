// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.seo;
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

public interface SeoRule {

    /** 规则 ID：{模块 ID}.{点分小写}，如 "seo.title.length"；全局唯一。 */
    String id();

    /** 默认级别；单条问题可以给出不同级别（如标题重复：同类型为 ERROR，跨类型为 WARNING）。 */
    com.b2bweb.api.common.Severity severity();

    /** 适用的内容类型；空表示全部。 */
    Set<String> contentTypes();

    /** PAGE：发布后与夜间都执行；SITE：需要全站数据，只在夜间全站检查中执行（ctx.siteIndex() 可用）。 */
    default SeoRuleScope scope() { return SeoRuleScope.PAGE; }

    /** 检查一个本地化；无问题返回空列表。只读，不得调用远程服务。 */
    List<SeoIssue> check(SeoAuditContext ctx);
}
