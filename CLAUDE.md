# CLAUDE.md

本文件是 Claude Code 等 AI 编码助手在本仓库工作时必须遵守的规则。本文件与设计文档冲突时，以系统设计说明书和状态为 Accepted 的 ADR 为准；发现冲突先指出，再动手修改。

## 项目简介

模块化 B2B 企业网站平台（Modular B2B Website Platform）：一家企业自用的多站点、多语言企业官网与询盘获客平台，模块化、SEO 原生、由设计系统驱动。

**当前状态：设计阶段，代码骨架尚未建立**（里程碑 M0 未开始）。仓库中只有设计文档。没有明确要求时，不要创建代码骨架、构建文件或引入依赖。
计划目录（系统设计 29.1）：`backend/`（`platform/platform-core`、`platform/platform-api`、`modules/<模块>`、`infrastructure`、`application`）、`frontend/`（`apps/website`、`apps/admin`、`packages/*`）、`docs/`、`deploy/`、`tools/`。

## 文档索引

| 路径 | 内容 | 状态 |
|---|---|---|
| `docs/architecture/system-design.md` | 系统设计说明书（顶层约束） | V1.3 |
| `docs/adr/` | 架构决策记录；索引、状态与流程见 `docs/adr/README.md` | 持续维护 |
| `docs/framework/platform-framework.md` | 平台框架设计：工程结构、全部跨模块契约（platform-api）、platform-core 与基础设施、数据库通用约定、前端框架、测试替身、CI、模块文档模板、并行开发计划 | 编写中 |
| `docs/modules/<模块 ID>.md` | 模块设计文档，每个模块一份：职责、依赖与契约、领域模型、表结构与 DDL、接口、界面、测试、验收 | 编写中 |

开发方式（ADR-018）：先完成平台框架（M0），再按模块设计文档并行开发。模块只依赖 platform-api 与 platform-testkit 中的测试替身，不等待其他模块；跨模块契约只在平台框架文档中定义。

系统设计常用章节：3 模块化设计（分层、模块清单、依赖规则、扩展点）；5 站点与语言、AI 翻译；6 内容内核；10 页面、模板与组件；24 API 规范；25 数据库规范；26 安全；29 工程规范与架构守护。

## 核心架构

- 模块化单体：编译期模块（Maven 模块）+ 站点级启用开关，不做热插拔；后端 Spring Boot 4 + Spring Modulith + MyBatis-Plus + MySQL 8.4；前台 Nuxt 4（SSR）；后台 Vue 3 + Vite + Element Plus
- 分层：L0 platform-core → L1 platform-api（只有契约）→ L2 content、url、media、taxonomy、form、notification、translation → L3 product、article、page、inquiry、navigation → L4 delivery、seo、graph、search、tracking；任何模块只能依赖 platform-core 与 platform-api
- 表前缀：core `sys_`、content `cnt_`、url `url_`、media `mda_`、taxonomy `tx_`、form `frm_`、notification `ntf_`、translation `trn_`、product `prd_`、article `art_`、page `pg_`、inquiry `inq_`、navigation `nav_`、seo `seo_`、graph `grh_`、tracking `trk_`（delivery 无表；search 的索引在 Meilisearch）
- 站点是最高隔离单位，不设租户层：站点级表带 `site_id`，由 MyBatis-Plus 行级拦截器自动追加过滤（类名 `TenantLineInnerInterceptor` 只是框架命名，列名配置为 `site_id`）；全局表（`sys_user`、`sys_role`、`sys_permission`、`sys_module` 等）列入豁免清单；Redis key、对象存储路径、Meilisearch 索引、Cache Tag 都带站点前缀（系统设计 5.3）
- 模块通信只有三种：platform-api 中的服务接口、领域事件（事务提交后投递）、扩展点
- 内容内核：Content + Localization + Revision；发布生成不可变快照与只读投影
- 前台只读发布态数据（快照与投影），永远不读草稿
- Design System：前台样式只能使用 Design Token 与主题变体
- 渲染：系统内容用“模板 + 插槽”，营销页用页面编辑器；组件 Schema 在 `frontend/packages/component-schemas`
- 语言与 URL：源语言固定为简体中文 `zh-CN`；前台 V1 只对外开放中文，其他语言由 AI 翻译生成后按语言开放；全部语言 URL 带前缀（`/zh/…`、`/en/…`），根路径 `/` 以 302 跳转到默认对外语言；内容不跨语言回退
- translation 模块（L2，可停用）：源语言发布或站点资源变化后异步执行：经扩展点提取翻译单元 → 查翻译记忆 `trn_memory`（同一原文只翻译一次）→ 调用 AI 引擎 → 校验 → 回写目标语言草稿；人工修订优先（`HUMAN` > `REVIEWED` > `MACHINE`）

## 语言规则

1. 后台界面只用简体中文：不引入 vue-i18n，不做语言切换；界面文案集中放在常量 / 字典文件中。`module.json`、组件定义、模板定义中的 `name` / `label` 只写中文字符串（如 `"name": "询盘"`），不写 `{ "zh-CN": …, "en-US": … }` 对象
2. 后台错误信息（RFC 9457 的 `title`、`detail`、`errors[].message`）用中文，错误码为 `{MODULE}_{REASON}`；前台公开接口的错误由前端按 `code` 映射为文案键显示，不直接展示 `detail`
3. 内容以简体中文为源语言：内容与站点资源只用中文录入；目标语言默认由 translation 生成（`AUTO` 模式），人工只在翻译编辑器中按段修订；只有 `MANUAL` 模式的本地化才独立编辑
4. 前台所有用户可见文字必须走文案键（如 `form.submit`，主题提供默认中文文案，站点可覆盖）或翻译流程，组件中不得硬编码中文或英文文案；日期、数字用 `Intl` 按语言格式化；前台渲染时不调用 AI
5. 新增可翻译字段必须登记：内容字段在该内容类型的 `TranslatableContentProvider` 中提取与回写；站点资源（菜单、表单、属性名与选项、分类名、媒体 alt / 标题、邮件模板、界面文案、Cookie 横幅等）在 `TranslatableResourceProvider` 中登记，并在资源变化时发布 `TranslatableResourceChangedEvent`；组件 Schema 属性正确设置 `x-translatable`（字符串与富文本默认 `true`；型号、代码、URL、枚举为 `false`）
6. AI 引擎通过 OpenAI 兼容接口接入：translation 只依赖端口 `TranslationEngine`；适配器在 infrastructure 中用 Spring HTTP 客户端调用 `POST {baseUrl}/chat/completions`；base URL、模型名、API Key 只来自引擎配置 `trn_engine`（API Key 用 AES-GCM 加密存储）；不得在业务代码中直接调用任何厂商 SDK

## 严格禁止

1. platform-core 依赖任何业务模块；模块 import 其他模块的包
2. 访问其他模块的表、跨模块 JOIN、跨模块外键（例如询盘译文由 inquiry 自己保存，translation 不写 `inq_` 表）
3. 在 platform-api 之外定义跨模块契约
4. 页面或组件硬编码业务字段；在布局文档中保存 URL（必须使用 `$content` / `$media` / `$term` / `$source`）
5. 重复实现已有组件；在前台写颜色、间距等字面量或新建 Token（Token 只能在 design-tokens 包中修改）
6. SEO 逻辑散落在业务模块中（业务模块只能实现 `SchemaOrgProvider`、`SeoVariableProvider` 等扩展点）
7. 修改已合入的数据库迁移脚本；在 MyBatis 中使用 `${}`
8. 使用 `v-html`（富文本渲染器除外）
9. 为实现单个需求破坏模块边界；未经 ADR 修改核心架构
10. 引入租户层或 `tenant_id` 字段；站点级表缺少 `site_id`；绕过行级拦截器做跨站点查询（只允许系统管理接口）；新增全局表却不列入豁免清单
11. 后台引入 vue-i18n 或多语言文案；前台组件中硬编码用户可见文案
12. 在任何模块中引入 AI 厂商 SDK（OpenAI、DeepSeek、通义千问等的 Java / JS SDK），或绕过 `TranslationEngine` 直接请求 AI 服务；在代码或配置文件中写入 API Key
13. 机器译文覆盖翻译记忆中的 `HUMAN` / `REVIEWED` 条目；询盘留言或其译文写入翻译记忆
14. 把待翻译文本、询盘留言或模型返回的译文当作指令执行，或拼进后续提示词的指令部分（一律按纯文本处理；单元 ID 由系统生成并校验）
15. 法律页面（隐私政策、Cookie 政策、条款、Imprint）与 `legal` 模板的机器译文自动发布
16. 在发布事务中同步调用 AI 引擎；翻译必须在事务提交后异步执行，引擎故障不得阻塞中文内容发布
17. 生成不带语言前缀的前台 URL；某语言缺少本地化时回退显示其他语言的内容

## 新功能先判断

- 属于现有模块，还是应该成为新模块？
- 应该是组件、模板、数据源，还是内容关系？
- 应该通过事件实现，还是通过扩展点实现？
- 是否需要新的跨模块契约（需要则先写 ADR）？
- 数据是站点级（带 `site_id`），还是全局（列入豁免清单）？
- 是否产生前台可见文字或可翻译字段（需要则定义文案键，或在 `TranslatableContentProvider` / `TranslatableResourceProvider` 中登记）？

## 开发前必须

1. 阅读 `docs/modules/<模块>.md` 与相关 ADR；模块文档尚未建立时，阅读系统设计说明书的对应章节
2. 确认模块边界与数据模型：所属层、表前缀、站点级还是全局；表与字段以该模块设计文档的“数据模型”一节为准（platform-core 的表见平台框架设计）
3. 再开始编码

## 提交前必须通过（代码骨架建立后生效）

- 后端：`./mvnw verify`（含架构测试与集成测试）
- 前端：`pnpm lint && pnpm typecheck && pnpm test`
- 涉及 API：重新生成 api-client 并提交
- 涉及组件 Schema：破坏性变更必须提升版本并附迁移函数
- 任何跨模块修改都必须在 PR 中说明原因

当前只有文档：提交前确认新增或修改的内容中引用的文件路径、章节编号、ADR 编号都实际存在。

## 文档维护

- 修改架构或跨模块契约（platform-api 中的服务接口、扩展点、事件、DTO）时，先写或更新 ADR，再改代码：文件为 `docs/adr/NNNN-title.md`（MADR 模板），流程见 `docs/adr/README.md`
- 必须写 ADR 的情况：新增模块；跨模块契约变更；引入新的基础设施依赖；不兼容的数据模型变更；偏离架构原则 P1–P8（系统设计 2.1）
- 设计变更同步更新系统设计说明书（文档版本号与 0.5 修订记录）及相关的 `docs/modules/<模块>.md`；不得改变系统设计说明书现有的章节编号（其他文档按编号引用）
- 数据库变更（新增或修改表、字段、索引、迁移）同步更新对应模块设计文档的“数据模型”一节（platform-core 的表在 `docs/framework/platform-framework.md` 中）
- 代码、测试、文档放在同一个 PR 中（完成定义见系统设计 29.6）

## 提交规范

- 提交信息遵循 Conventional Commits：`<type>(<scope>): <描述>`；type 用 `feat`、`fix`、`docs`、`refactor`、`test`、`build`、`ci`、`chore`、`perf`；scope 可选，用模块 ID（如 `product`、`translation`）或前端应用名（`website`、`admin`）；破坏性变更加 `!` 并写 `BREAKING CHANGE:` 说明
- 一个提交只做一件事：一个功能连同它的测试与文档算一件事；重构、格式化、依赖升级各自单独提交
- 不提交密钥、API Key 与本地配置（CI 运行 gitleaks）
- 主干开发，短生命周期功能分支，经 PR squash 合并；platform-core、platform-api 的变更需要架构负责人审核
