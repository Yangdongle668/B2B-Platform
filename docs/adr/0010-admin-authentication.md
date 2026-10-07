# ADR-010：后台认证：Session + HttpOnly Cookie + CSRF + TOTP

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-06 |
| 决策者 | 业主、架构负责人 |
| 相关章节 | 1.4、1.5、2.3、2.4、2.5、5.2、5.3、22.1、22.6、24.1、26.1–26.7、27.4、27.5、28、29.6 |
| 相关 ADR | ADR-009、ADR-011 |

## 背景

- 只有管理后台需要登录；前台网站没有登录，访客匿名浏览与提交询盘，客户门户不在 V1 范围（1.4、2.5）。
- 后台 SPA 与 Admin API 同源部署（`admin` 域名下 `/api/admin` 由反向代理转发），不需要 CORS，Cookie 可以使用 SameSite（2.3、22.6）。
- 后端多实例、无状态，会话保存在 Redis（2.3、27.1）。
- 安全目标为 OWASP ASVS Level 2；后台高权限账号强制 2FA（1.5）。
- 用户全局唯一，角色按站点分配；后台请求用 `X-Site-Id` 指定当前站点（5.2、ADR-009）。
- 后台可以执行高风险操作：发布与下线内容、导出询盘（含个人数据）、修改统计脚本与 `custom-html`（26.3、26.5）。
- 后台并发用户的目标规模为 50 个（1.5）；SSO 为【V2+】（26.1）。

## 决策

后台认证采用**服务端会话（Spring Session + Redis）+ HttpOnly Cookie + CSRF Token + TOTP 2FA**（26.1）：

| 项 | 设计 |
|---|---|
| 登录 | 邮箱 + 密码；Argon2id 哈希；密码 ≥ 12 位；【可选】对接泄露密码库检查 |
| 2FA | TOTP；拥有发布、管理、开发者权限的角色强制开启；提供恢复码；2FA 数据在全局表 `sys_user_mfa` 中 |
| 会话 | Spring Session（Redis）；Cookie `__Host-SESSION; Secure; HttpOnly; SameSite=Lax`；空闲 2 小时、绝对 12 小时过期 |
| CSRF | Spring Security CSRF，Cookie 转 Header（`XSRF-TOKEN` → `X-XSRF-TOKEN`）；Admin 的 HTTP 层统一附加（22.1） |
| 二次验证 | 修改密码、重置 2FA、导出数据、修改脚本等敏感操作需要重新验证 |
| 防暴力破解 | 同账号 + IP 15 分钟内失败 5 次后递增延迟，异常时告警；登录接口按“账号 + IP”限流 5 次 / 15 分钟（26.4） |
| 站点切换 | 会话绑定用户；切换站点时校验用户在该站点有角色（5.3） |
| 审计 | 登录、登出、登录失败、角色与权限变更写入 `sys_audit_log`（26.5） |
| 点击劫持 | 后台 `frame-ancestors 'none'`（26.3） |
| SSO | 【V2+】OIDC（Google Workspace、Microsoft Entra 等） |

范围：

- 所有 `/api/admin/v1/**` 接口都要求会话 + CSRF + 权限校验（24.1）；授权模型（权限码、数据范围、`@RequiresPermission`）见 26.2。
- 前台公开接口没有会话，表单提交使用 formToken + Turnstile（26.3）；Nuxt 调用后端使用内部令牌 `X-Internal-Token`（26.7）。二者不属于本 ADR。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| JWT 保存在 localStorage，以 Bearer 头发送 | 后端无状态；跨域方便 | 令牌可被 XSS 读取；难以主动吊销、难以实现空闲超时；需要刷新令牌机制 | 不采用 |
| JWT 保存在 HttpOnly Cookie | 脚本无法读取令牌 | 仍需要 CSRF 防护；吊销需要服务端黑名单，等于重新引入服务端状态 | 不采用 |
| 服务端会话（Redis）+ HttpOnly Cookie + CSRF | 可立即失效；空闲与绝对过期可控；同源部署下实现简单 | 依赖 Redis；需要处理 CSRF | 采用 |
| 短信或邮件验证码作为 2FA | 用户熟悉 | 依赖外部发送服务；送达与安全性不如 TOTP | 不采用 |
| V1 直接接入 SSO（OIDC） | 统一身份，无需管理密码 | 增加外部身份服务依赖；V1 用户规模小 | 不采用：【V2+】 |

## 后果

### 正面

- 会话凭据只在 HttpOnly Cookie 中，前端脚本无法读取；`__Host-` 前缀要求 Secure 且限定在当前主机。
- 服务端会话可以立即失效（登出、修改密码等），空闲与绝对过期由服务端控制。
- 同源部署 + SameSite Cookie + CSRF Token，CSRF 防护有两层。
- 会话在 Redis 中，后端实例保持无状态，可以水平扩展与滚动更新。
- TOTP 不依赖短信或邮件等外部发送服务。

### 负面与代价

- Redis 是后台登录的关键依赖；Redis 不备份，故障或数据丢失时会话丢失，用户需要重新登录（27.5）。
- 后台页面必须与 `/api/admin` 保持同源部署，限制了部署方式（22.6）。
- Admin 的 HTTP 层要统一处理 CSRF Token、会话过期后的重新登录与二次验证流程。
- 用户需要安装 TOTP 应用并妥善保管恢复码。
- V1 没有 SSO，员工需要单独维护一套后台账号密码。

### 需要遵守的规则

| 规则 | 检查方式 |
|---|---|
| 后台接口一律放在 `/api/admin/v1/{module}/…` 下，统一经过会话与 CSRF 校验；不得为后台接口关闭 CSRF 或开放匿名访问（登录相关接口除外） | PR 评审；安全扫描 OWASP ZAP baseline（28） |
| 每个后台写操作都有权限点（`@RequiresPermission`）与审计 | 完成定义（29.6）；PR 评审 |
| 修改密码、重置 2FA、导出数据、修改脚本等敏感操作必须二次验证；新增同类操作时在评审中确认 | PR 评审 |
| Admin 前端不得把会话凭据或令牌保存在 localStorage / sessionStorage；会话只由 HttpOnly Cookie 承载 | PR 评审 |
| 密码只用 Argon2id 哈希；密码、TOTP 密钥与恢复码不得写入日志或审计差异 | PR 评审；日志脱敏（27.4） |
| 自定义角色只要包含发布、管理或开发者权限，同样强制 2FA | 模块集成测试；PR 评审 |
| 后台 `frame-ancestors 'none'`；Cookie 属性与过期时间按本 ADR 配置 | 安全扫描（28）；PR 评审 |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-10-06 | 创建 |
