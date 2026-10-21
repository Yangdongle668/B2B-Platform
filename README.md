# B2B Platform

多站点、多语言企业网站平台。前台参考 Tesla 的黑白灰、简洁导航与大幅视觉方向，后台使用 Element Plus。

当前为 **M0 工程候选版，完整 G0 未通过**。17 个业务模块已生成骨架，media/search 包含 M0 事务协议接收器；完整业务功能按 M1～M4 交付。契约、替身和原型的存在不等于业务提供方已实现。

需要 Docker（本地 Unix socket）、Python 3.12+、Node 24.19 和 pnpm 11.25。后端使用固定摘要的 Maven/JDK25 容器构建，API 按 Java21 语言级别编译。

```bash
python -m pip install -r tools/verification/m0-compatibility/requirements.txt
python tools/dev/stack.py up
```

启动后访问 `https://localhost:8443/`、`https://localhost:8443/admin/`。开发证书为本机自签名证书，位于忽略的 `.dev/stack/`。初始账号 `admin@local.test`，随机密码在 `.dev/stack/state.json` 的 `bootstrapPassword`，文件权限为 0600；首次登录必须绑定验证器并修改密码。Mailpit 在 `http://localhost:8025`。所有主机端口只绑定回环地址。

`python tools/dev/stack.py down` 只停止本仓库创建的服务，保留数据卷。再次 `up` 复用数据与密钥。`--no-build` 仅用于已有本地构建；本地 HTTPS 进程日志在 `.dev/stack/web.log`。首次运行会拉取镜像和依赖。

开发栈显式使用 Maven `development` 构建配置，把尚无业务提供方的服务由 testkit 补位，并桥接真实站点、操作人与时钟。替身数据在进程内，重启丢失；MySQL/Redis/S3兼容服务的数据卷保留。默认生产构建不会加入这段开发装配；生产和预发布保护拒绝开发构件。当前本地对象存储为固定摘要的 **S3Mock**，MinIO 尚未验证，见 [ADR-028](docs/adr/0028-local-s3-provider.md)。

```bash
./mvnw verify
python tools/generators/contracts.py --check
python tools/generators/catalog.py --check
python tools/generators/modules.py --check
python tools/checks/architecture.py
python tools/checks/migrations.py
python tools/verification/m0-compatibility/verify_design.py
python tools/verification/m0-compatibility/verify_backend.py
python tools/frontend/verify.py
python tools/frontend/browser.py
python tools/frontend/auth_browser.py
python tools/frontend/permissions_browser.py
python tools/checks/new_module_exercise.py
python tools/checks/security.py
```

认证浏览器测试需要开发栈，使用真实 HTTPS、MySQL、Redis、MFA 和会话，不注入接口响应；测试账号的后续密码和恢复码只保存在忽略的本地状态文件。权限浏览器套件临时创建无管理权限的站点成员，验证真实菜单、403路由及后端授权，结束删除夹具。普通浏览器套件验证 SSR、画廊、四种屏宽和 axe，无会话失败提示使用明确的 HTTP 故障夹具。

后端验证为每次运行创建临时 MySQL/Redis/Meilisearch/S3Mock/Mailpit，不开放主机端口；使用独立 DML 与 DDL 账号，结束只删除本次容器、卷和网络。缺少服务时失败，不跳过集成测试。部分 reactor 运行只统计本次生成的测试 XML，不合并旧结果。摘要在忽略的 `.reports/`。

前端执行 ESLint、Stylelint、文案/Token 守护、类型检查、Vitest 和前后台构建。用户目录不可写时脚本会为 pnpm 使用 `.dev/pnpm-home`，不修改 HOME。托管环境保留代理及 CA 信任，不把凭据写入仓库。

契约由框架第4章生成，禁止直接修改生成文件。真实应用测试导出 OpenAPI 后执行 `python tools/generators/client.py --write && pnpm generate:client`，提交 JSON 和类型文件；CI 比较真实导出，防止旧 JSON 掩盖后端漂移。Java Long 在 JSON 和 OpenAPI 中均使用十进制字符串。

模块各有独立 Flyway history。既有迁移不可改写，数据库变更追加版本。部署时先以独立迁移凭据运行 `java -jar application.jar --mode=migrate`（变量见应用配置），该命令不启动 HTTP、会话或 worker；正常生产启动只验证迁移。新模块先按 ADR 流程评审，再用 `python tools/generators/new_module.py --id example --name 示例模块 --layer L3` 接入。

安全检查真实阻断失败：当前 pnpm 构建依赖链有两个尚无发布修复的高危通告；密钥扫描已通过，后端漏洞数据库访问受当前环境网络策略阻断。没有自动忽略高危问题或把 G0 标成通过。详细证据与后续关卡见 [M0 工程记录](docs/reviews/m0-engineering-review-2026-10-20.md)。

文档入口：[系统设计](docs/architecture/system-design.md)、[平台框架](docs/framework/platform-framework.md)、[开发计划](docs/plans/development-plan.md)、[正式评审](docs/reviews/formal-design-review-2026-10-20.md)。发布标签、主干合并与 ADR 采纳继续按人工评审规则执行。
