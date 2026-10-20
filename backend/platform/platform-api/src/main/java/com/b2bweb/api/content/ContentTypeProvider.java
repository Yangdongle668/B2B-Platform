// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.content;
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

public interface ContentTypeProvider {

    /** 本模块注册的内容类型。 */
    List<ContentTypeDefinition> contentTypes();

    /**
     * 为某本地化提供路由提示（父页面、主分类、首页标记、规则变量）。只对 routable 类型调用；
     * 在发布事务中调用（4.1.3 回调约束），只读本模块的草稿数据。
     */
    default com.b2bweb.api.url.RouteHints routeHints(RouteHintsContext ctx) {
        return com.b2bweb.api.url.RouteHints.none();
    }
}
