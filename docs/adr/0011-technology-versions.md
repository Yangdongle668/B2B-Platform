# ADR-011：技术版本锁定

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-06 |
| 决策者 | 业主、架构负责人 |
| 相关章节 | 2.4、3.6、4.1、5.9、10.5、11.2、21.4、25.6、25.7、26.1、26.3、27.3、28、29.3、29.4、30（M0）、31 |
| 相关 ADR | ADR-001、ADR-003、ADR-009、ADR-010、ADR-017 |

## 背景

- 2.4 给出的是**选型方向**，其中多项是较新的主版本：JDK 25 LTS（最低 21）、Spring Boot 4.x + Spring Modulith 2.x、Nuxt 4 + Vue 3.5；持久层为 MyBatis-Plus，迁移为 Flyway。2.4 的版本说明写明：“M0 阶段需要对 Spring Boot、Spring Modulith、MyBatis-Plus、Flyway、Nuxt 的具体版本做一次兼容性验证，锁定组合后写入 ADR-011。”
- 架构依赖这些组件的特定能力（见决策第 2 条），任何一项不可用都会影响架构本身，而不只是实现细节。
- “第三方依赖版本不兼容”被列为可能性中、影响中的风险，对策是“M0 做版本验证并锁定；Renovate 渐进升级”（31）。
- M0 的主要交付包含“版本兼容验证（ADR-011）”（30）。

## 决策

1. **M0 在工程骨架上做兼容性验证，锁定一组通过验证的版本组合**，记录在本 ADR 的版本表中。版本表填写完成后提交评审，通过后本 ADR 改为 Accepted。

2. **验证范围**：以下能力必须在锁定组合上实际跑通（能力来源见各章节）：

| 组件 | 必须验证的能力 |
|---|---|
| JDK + Spring Boot 4.x | 虚拟线程（数据源并行解析、通知发送，2.4、10.5）；Spring Security 与 Spring Session（Redis）（26.1）；Spring HTTP 客户端（AI 翻译引擎适配器，5.9） |
| Spring Modulith 2.x | 模块边界验证 `verify()`；`@ApplicationModuleListener` 事务提交后异步投递；Event Publication Registry（`EVENT_PUBLICATION` 表）与重新投递；`@ApplicationModuleTest` 与 Scenario API（4.1、28） |
| MyBatis-Plus | 与 Spring Boot 4.x 集成；行级拦截器 `TenantLineInnerInterceptor`（列名 `site_id`）与豁免清单（25.7） |
| Flyway | 每个模块独立的位置与 history 表 `flyway_{module}_history`，由模块管理器按依赖顺序执行；独立的 migrate 任务（`--mode=migrate`）；在 MySQL 8.4 LTS 上运行（3.6、25.6） |
| Nuxt 4 + Vue 3.5 | SSR 与懒水合（21.4）；@nuxt/image 自定义 imgproxy provider（2.4）；主题在构建时打包、组件变体按需异步加载（11.2） |

3. **验证方式**：以 M0 的工程骨架为载体。M0 的验收（示例模块违反边界时 CI 失败；按模板新建一个模块并接入 ≤ 1 小时）与 CI 的全部门禁（编译、单元测试、架构测试、Testcontainers 集成测试、OpenAPI 生成、前端构建，27.3）都在锁定组合上通过，即视为验证通过（30）。

4. **版本表**（M0正式冻结前填写；下列为正式工程及隔离验证候选，尚未完成所有锁定门禁）：

| 组件 | 选型方向（2.4） | 工程验证候选 / 冻结状态 |
|---|---|---|
| JDK | 25 LTS（最低 21） | 25.0.4.1（工程与原型）；未冻结 |
| Spring Boot | 4.x | 4.0.8（工程与原型）；未冻结 |
| Spring Modulith | 2.x | 2.0.8（工程与原型）；未冻结 |
| MyBatis-Plus | — | 3.5.17 Boot4 starter + jsqlparser（工程与原型）；未冻结 |
| Flyway | — | 11.14.1（Boot管理）；未冻结 |
| Nuxt / Vue | Nuxt 4 / Vue 3.5 | 4.6.2 / 3.5.44；@nuxt/image 2.2.0（工程与原型）；未冻结 |

| Node / pnpm | 前端构建 | 24.19.0 / 11.25.0；packageManager与CI明确固定 |
| Element Plus / Vite / TypeScript | 后台与类型检查 | 2.14.7 / 7.3.7 / 5.9.3；未冻结 |
| Vitest / ESLint / Stylelint | 前端门禁 | 3.2.4 / 10.13.0 / 17.16.0；未冻结 |
| AWS SDK v2 | S3端口 | 2.57.0；真实S3Mock验证，不等于MinIO认证 |
| JSON Schema / ShedLock | 设置校验与调度 | networknt 3.0.8 / 7.10.1；未冻结 |
| MySQL / Redis / Meilisearch | 本地真实服务 | 8.4 / 7.4 / 1.15.2；镜像摘要见验证脚本 |
| imgproxy | 图片处理与签名 | 3.30.1，真实S3取图/四格式/裁切与签名拒绝验证；摘要见验证脚本，未冻结 |
| TSID正数编码 | ID生成候选 | 自有41/10/12位实现，纪元2020-01-01 UTC，数据库节点租约；ADR-032待评审 |
| Actuator / Prometheus | Core观测 | Spring Boot 4.0.8 / Micrometer 1.16.7（BOM管理），未冻结 |
| S3Mock / Mailpit | 本地开发候选 | 4.11.0 / 1.27.9；MinIO下载受限，ADR-028待评审 |

   其他组件沿用 2.4 的选型（如 MySQL 8.4 LTS、Redis 7 或兼容的 Valkey、Element Plus、Tiptap），锁定时可以一并记入此表。

2026-10-20原型已覆盖与未覆盖的能力逐项见[M0复审](../reviews/m0-remediation-2026-10-20.md)及[可复现验证](../../tools/verification/m0-compatibility/README.md)。正式工程已补独立模块history、Security/Session、任务/调度、testkit和本地开发栈；Scenario API、全部安全/依赖门禁及签审仍须逐项核对，见[M0工程记录](../reviews/m0-engineering-review-2026-10-20.md)。pnpm构建链两个高危通告尚无发布修复；后端漏洞数据库在当前托管环境被代理策略拒绝，不能填写“扫描通过”。

5. **锁定之后的升级**：补丁与次版本通过 Renovate / Dependabot 渐进升级，必须通过全部 CI 门禁（26.3、31）；锁定组合中任一组件的主版本升级或替换，视为修改本决策：重新执行第 2、3 条的验证，并更新版本表与变更记录。

6. AI 翻译引擎用 Spring HTTP 客户端直接调用 OpenAI 兼容接口，不引入任何厂商 SDK（5.9、ADR-017），因此版本表中没有 AI SDK。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 现在直接写定各组件的最新版本，不做验证 | 省去 M0 的验证时间 | Spring Boot 4.x 与 Spring Modulith 2.x、MyBatis-Plus、Flyway 的组合，以及 Nuxt 4 与所需模块（如 @nuxt/image）的兼容性未经确认，问题可能在 M1 之后才暴露，返工成本高 | 不采用 |
| 不锁定，始终跟随最新版本 | 始终使用最新特性与补丁 | 构建不可重复；升级风险不可控 | 不采用 |
| 改用上一代成熟组合 | 生态成熟，资料多 | 偏离 2.4 已定的选型方向 | 不采用：如 M0 验证发现阻断问题，再通过修订本 ADR 与 2.4 处理 |
| M0 验证兼容组合后锁定，之后渐进升级 | 问题在 M0 暴露；构建可重复；升级节奏可控 | M0 需要投入验证时间 | 采用 |

## 后果

### 正面

- 兼容性问题在 M0 暴露，不会在业务开发中途返工。
- 构建可重复；开发者与 AI 编码助手对版本有唯一的依据。
- 升级有固定节奏：补丁与次版本持续跟进，主版本升级有明确的验证要求。

### 负面与代价

- M0 需要投入时间做验证。
- 较新的主版本周边生态（第三方 starter、插件）可能滞后；验证不通过时需要调整选型，并同步修订 2.4。
- 锁定后需要持续跟进安全补丁；主版本升级需要重新验证，成本较高。

### 需要遵守的规则

| 规则 | 检查方式 |
|---|---|
| M1 开始前完成验证并填写版本表 | M0 里程碑验收（30） |
| 构建文件（Maven、pnpm workspace）中声明的版本与本 ADR 的版本表一致 | PR 评审（依赖变更时核对） |
| 依赖升级单独提交（Conventional Commits，如 `build`），并通过全部 CI 门禁 | CI（27.3）；PR 评审（29.4、CLAUDE.md 提交规范） |
| 锁定组合中任一组件的主版本升级或替换，先修订本 ADR | PR 评审（P8、29.5） |
| 依赖漏洞持续扫描，高危阻断 | Renovate / Dependabot、OWASP Dependency-Check、`pnpm audit`、Trivy（26.3、28） |
| 不引入 AI 厂商 SDK | PR 评审（5.9、ADR-017） |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-10-06 | 创建 |
| 2026-10-20 | 登记隔离验证候选，保留完整M0门禁及Proposed状态 |
| 2026-10-20 | 补正式工程候选版本及实际门禁状态，未自动Accepted；保留依赖高危阻断与完整G0审核 |
