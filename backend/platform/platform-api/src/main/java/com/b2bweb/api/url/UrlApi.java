// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.url;
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

public interface UrlApi {

    /**
     * 计算路径并检查是否可用（发布事务第 2 步，系统设计 4.4）；不写库。
     * 历史路径（某条 AUTO 重定向的源）可被新内容占用：返回 Available.previousOwnerLocalizationId，
     * 由后台提示“此 URL 以前属于 X”；applyRoute 时删除该重定向（路由优先）。
     */
    PathReservation reservePath(RouteRequest request);

    /**
     * 写入或更新 url_route（发布事务第 6 步）：路径变化时写入 AUTO 301（源 = 旧路径，目标 = 本地化）、删除源为新路径的重定向、
     * 发布 ContentPathChangedEvent。applyRoute 内部再次检查唯一性（并发发布），冲突时抛出 URL_PATH_CONFLICT。
     */
    RouteChange applyRoute(ApplyRouteCommand command);

    /**
     * 下线时退役路由（系统设计 13.4、13.6）：删除 url_route；当前路径与所有指向该本地化的历史重定向，
     * 按 mode 返回 410 或 301 到 targetLocalizationId 的当前路径。再次发布时由 applyRoute 恢复。发布 ContentPathChangedEvent。
     */
    RouteChange retireRoute(RetireRouteCommand command);

    /** 彻底删除内容时释放其全部路由；指向它的重定向改为 410。幂等。 */
    void releaseRoutes(long contentId);

    /** 按语言的音译规则生成 slug（系统设计 13.3）：只含 [a-z0-9-]，最长 80；保留本地文字的语言以 NFC 输出。结果可能为空串（全为符号）。 */
    String generateSlug(String text, String locale);

    /** 校验 slug 格式与保留字；合法时返回空列表。 */
    List<ValidationIssue> validateSlug(String slug, String locale);

    /** 在 slug 后追加 -2、-3… 直到路径可用（最多尝试 50 次，失败抛出 URL_PATH_CONFLICT）。 */
    String suggestAvailableSlug(RouteRequest request);

    /**
     * 解析请求路径（系统设计 13.2 的第 1b–6 步；第 1a 步主机名规范化由 delivery 用 SiteApi.resolveHost 完成）。
     * path 为原始请求路径（已 URL 解码、不含查询串）。路由所属语言未对外公开时按 NotFound 处理。Delivery 热路径。
     */
    RouteResolution resolve(String path);

    /** 本地化的当前路径（含语言前缀）；未发布为空。Delivery 热路径。 */
    Optional<String> pathOf(long localizationId);

    /** 批量查询当前路径（≤ 500）。Delivery 热路径。 */
    Map<Long, String> pathsOf(Collection<Long> localizationIds);

    /** 语言首页路径，如 "/zh"（按尾斜杠策略）。 */
    String localeHomePath(String locale);

    /** 站内路径转为主域名 https 绝对 URL。 */
    String absoluteUrl(String path);

    /** 计入 404（Redis 计数，定期批量写入 url_not_found）。由 delivery 在确认没有系统路由匹配后调用。 */
    void recordNotFound(String path, @Nullable String referrer);

    /**
     * 创建重定向（导入的旧站 URL 等）。目标路径本身是重定向源时压缩为最终目标；目标路径是某本地化的路由时转存为 LOCALIZATION；
     * 环路检测（系统设计 13.4）。PREFIX / REGEX 每站最多 200 条。发布 RedirectChangedEvent。
     *
     * @throws BusinessException URL_REDIRECT_LOOP、URL_REDIRECT_SOURCE_IS_ROUTE、URL_REDIRECT_LIMIT
     */
    RedirectInfo addRedirect(@Valid CreateRedirectCommand command);
}
