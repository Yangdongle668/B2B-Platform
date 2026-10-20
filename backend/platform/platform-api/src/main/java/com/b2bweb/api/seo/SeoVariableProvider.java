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

public interface SeoVariableProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 本实现提供的变量（后台模板编辑器的变量提示）。 */
    List<SeoVariableDefinition> variables();

    /** 变量值（变量名不含花括号）；值为该语言的纯文本，没有值的变量不放入结果（模板中替换为空串）。在发布事务中调用。 */
    Map<String, String> resolve(com.b2bweb.api.content.PublishContext ctx);
}
