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
| Spring Modulith 2.x | 模块边界验证 `verify()`；`@ApplicationModuleListener` 事务提交后异步投递；Event Publication Registry（`event_publication` 表）与重新投递；`@ApplicationModuleTest` 与 Scenario API（4.1、28） |
| MyBatis-Plus | 与 Spring Boot 4.x 集成；行级拦截器 `TenantLineInnerInterceptor`（列名 `site_id`）与豁免清单（25.7） |
| Flyway | 每个模块独立的位置与 history 表 `flyway_{module}_history`，由模块管理器按依赖顺序执行；独立的 migrate 任务（`--mode=migrate`）；在 MySQL 8.4 LTS 上运行（3.6、25.6） |
| Nuxt 4 + Vue 3.5 | SSR 与懒水合（21.4）；@nuxt/image 自定义 imgproxy provider（2.4）；主题在构建时打包、组件变体按需异步加载（11.2） |

3. **验证方式**：以 M0 的工程骨架为载体。M0 的验收（示例模块违反边界时 CI 失败；按模板新建一个模块并接入 ≤ 1 小时）与 CI 的全部门禁（编译、单元测试、架构测试、Testcontainers 集成测试、OpenAPI 生成、前端构建，27.3）都在锁定组合上通过，即视为验证通过（30）。

4. **版本表**（M0 填写）：

| 组件 | 选型方向（2.4） | 锁定版本 |
|---|---|---|
| JDK | 25 LTS（最低 21） | 待 M0 验证 |
| Spring Boot | 4.x | 待 M0 验证 |
| Spring Modulith | 2.x | 待 M0 验证 |
| MyBatis-Plus | — | 待 M0 验证 |
| Flyway | — | 待 M0 验证 |
| Nuxt / Vue | Nuxt 4 / Vue 3.5 | 待 M0 验证 |

   其他组件沿用 2.4 的选型（如 MySQL 8.4 LTS、Redis 7 或兼容的 Valkey、Element Plus、Tiptap），锁定时可以一并记入此表。

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
