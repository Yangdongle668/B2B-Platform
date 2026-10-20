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

/**
 * @param key                大写下划线，全局唯一
 * @param label              中文名称
 * @param inverseKey         互逆关系的反向类型（同时注册，graph 在同一事务中写入两个方向）；对称或单向时为空
 * @param allowedSourceTypes 允许的源内容类型；空表示全部
 * @param allowedTargetTypes 允许的目标内容类型；空表示全部
 * @param autoComputable     是否参与自动评分（RELATED）；人工关系类型为 false
 */
public record RelationTypeDefinition(String key, String label, boolean symmetric, @Nullable String inverseKey,
                                     Set<String> allowedSourceTypes, Set<String> allowedTargetTypes,
                                     boolean autoComputable) {
    public RelationTypeDefinition {
        allowedSourceTypes = Set.copyOf(allowedSourceTypes);
        allowedTargetTypes = Set.copyOf(allowedTargetTypes);
        if (symmetric && inverseKey != null) throw new IllegalArgumentException("对称关系不能有反向类型: " + key);
    }
}
