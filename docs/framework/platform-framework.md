# 平台框架设计

| 项 | 内容 |
|---|---|
| 文档版本 | V1.10 |
| 状态 | 草案（Draft），待评审 |
| 更新日期 | 2026-10-21 |
| 项目代号 | B2B Web Platform |
| 依据 | 《系统设计说明书》V1.12（`docs/architecture/system-design.md`）；架构决策记录 ADR-000 至 ADR-032（`docs/adr/`），重点为 ADR-001、ADR-002、ADR-003、ADR-004、ADR-009、ADR-012、ADR-014、ADR-017、ADR-018 |
| 定位 | 里程碑 M0 的交付物设计；全部跨模块契约的唯一定义处；各模块并行开发的前提 |
| 文档负责人 | 架构负责人 |

> 阅读提示：
> - 本文是三层设计文档中的中间一层（见 1.2）：向上服从系统设计说明书与已确认（Accepted）的 ADR，向下约束每一份模块设计文档。
> - 第 4 章的代码是**规范性契约**，必须按签名实现；其他章节的代码与配置除特别说明外为示意。
> - 本文引用系统设计说明书时直接写章节号（如“系统设计 3.7”），引用本文时写“本文第 N 章”或“N.M 节”。

## 目录

- [1. 文档说明](#1-文档说明)
- [2. 并行开发模型](#2-并行开发模型)
- [3. 仓库与工程结构](#3-仓库与工程结构)
- [4. platform-api 契约](#4-platform-api-契约)
- [5. 后端框架：platform-core 与基础设施](#5-后端框架platform-core-与基础设施)
- [6. platform-core 数据模型](#6-platform-core-数据模型)
- [7. 数据库通用约定与框架表](#7-数据库通用约定与框架表)
- [8. 前端框架](#8-前端框架)
- [9. 测试框架与测试替身](#9-测试框架与测试替身)
- [10. 质量门禁与 CI](#10-质量门禁与-ci)
- [11. 本地开发环境](#11-本地开发环境)
- [12. 模块设计文档模板与开发流程](#12-模块设计文档模板与开发流程)
- [13. 并行开发计划](#13-并行开发计划)
- [附录 A：module.json Schema](#附录-amodulejson-schema)

---

## 1. 文档说明

### 1.1 目的

业主确认的开发方式是“先写好系统框架，再为每个模块单独写设计文档，模块彼此独立、可以并行开发”（ADR-018）。本文是这一方式中“系统框架”部分的设计，用作：

1. **M0 交付物的设计依据**。系统设计 30 章把 M0（工程底座）定义为“Monorepo；Maven 多模块与 platform-api 骨架；模块管理器；扩展点注册表；事件；站点上下文；OpenAPI → TS；Nuxt 与 Admin 骨架；Docker Compose；CI 全部门禁；版本兼容验证”。本文把这些交付物细化到可以直接编码的程度，并补充 ADR-018 新增的交付物：契约冻结的 `platform-api`、`platform-testkit`（测试替身与契约测试）、模块设计文档模板与并行开发计划。
2. **模块并行开发的前提**。17 个业务模块（L2 7 个、L3 5 个、L4 5 个，见系统设计 3.2）之间的全部协作——服务接口、扩展点、领域事件、端口、共享类型——都在本文第 4 章给出完整签名。模块设计文档只引用这些契约，不再各自定义；每个服务接口在 `platform-testkit` 中都有内存实现，因此任何模块都可以在其他模块完成之前独立开发与测试（第 2 章）。
3. **统一的工程约定**。仓库结构、构建方式、包与类的命名、REST 路径、表前缀、权限码、事件名、文案键（第 3 章），以及数据库通用约定（第 7 章）、测试方式（第 9 章）、质量门禁（第 10 章），在所有模块中保持一致。

M0 完成的判定见 13.1；系统设计 30 章的 M0 验收标准（“示例模块违反边界时 CI 失败；按模板新建一个模块并接入 ≤ 1 小时”）保持不变。

### 1.2 文档体系

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 系统设计说明书  docs/architecture/system-design.md                               │
│ 做什么、为什么：需求范围、架构原则 P1–P8、分层与模块清单、关键机制、非功能指标       │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ 细化为可编码的框架（不改变结论）
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ 平台框架设计（本文）  docs/framework/platform-framework.md                        │
│ 怎么搭、模块之间怎么接：工程结构、全部跨模块契约的完整签名、platform-core 与基础设施、 │
│ platform-core 数据模型、数据库通用约定、前端框架、测试替身、CI、模板、并行计划       │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ 只引用契约；按第 12 章的模板编写
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ 模块设计文档  docs/modules/<模块ID>.md（每个模块一份）                             │
│ 每个模块怎么做：职责与边界、依赖与契约、领域模型、表结构与 DDL、流程、接口、界面、     │
│ 组件、配置、任务、测试、独立开发说明、验收标准                                     │
└──────────────────────────────────────────────────────────────────────────────┘

横向：ADR（docs/adr/）记录每一项架构决策的背景、取舍与必须遵守的规则
```

| 层 | 回答的问题 | 由谁维护 | 变更方式 |
|---|---|---|---|
| 系统设计说明书 | 做什么、为什么；架构约束 | 架构负责人 | 修订版本号与 0.5 修订记录；改变架构结论先写 ADR（系统设计 29.5） |
| ADR | 某项决策为什么这样做、必须遵守哪些规则 | 架构负责人 | `docs/adr/README.md` 的流程；Accepted 后不改写决策，改变决策写新的 ADR |
| 平台框架设计（本文） | 工程如何搭建；模块之间如何协作（契约） | 架构负责人 + 框架组 | 见 1.7；契约变更走 2.3 的流程 |
| 模块设计文档 | 某个模块如何实现 | 模块负责人 | 见 12.3、12.5；与代码同一个 PR 更新 |

**优先级与冲突处理**：

1. 系统设计说明书与状态为 Accepted 的 ADR 优先于本文，本文优先于模块设计文档（与 `CLAUDE.md` 的规定一致）。
2. 本文只做细化，不改变系统设计的结论。为了落地而做的细化（如 Maven 依赖方向、包结构、命名）在本文中写明理由；若细化与系统设计的字面表述不一致，在“待确认问题”中列出，评审确认后同步修订系统设计。
3. 发现冲突时先指出、再修改：在 PR 中说明冲突点，修订上层文档（必要时写 ADR）或修正下层文档，不允许下层文档“就地覆盖”上层规定。
4. 原计划的单一《领域模型与数据库设计》不再作为独立文档（ADR-018）：数据库通用约定与 platform-core 的表并入本文第 6、7 章，各模块的表结构与 DDL 并入对应模块设计文档的“数据模型”一节。

### 1.3 读者与阅读路径

| 读者 | 必读 | 按需 |
|---|---|---|
| 架构负责人 | 全文 | — |
| 框架组（M0 实施者） | 第 2–11 章、附录 A | 第 12、13 章 |
| 模块负责人、后端开发 | 第 2、3、4、7、9、12 章；本模块的设计文档 | 第 5、6、10、13 章 |
| 前端开发 | 第 3、8、9、12 章；本模块设计文档的第 7、8 节 | 第 4 章（Delivery 与组件相关契约） |
| 测试 | 第 2.7–2.8、9、10、13.6 节 | 各模块设计文档的第 11、13 节 |
| 运维 | 第 10、11 章 | 系统设计 27 章 |
| AI 编码助手（Claude Code） | `CLAUDE.md`；本文第 3、4、12 章；本模块设计文档 | 其他章节按任务需要 |
| 业主、产品负责人 | 第 1、2、13 章 | 各模块设计文档的第 13 节（验收标准） |

推荐的阅读顺序：开发某个模块之前，先读本文第 2 章（如何独立开发）、第 3 章（代码放在哪里、如何命名）、第 4 章中本模块用到的契约，再读本模块的设计文档（系统设计 29.7 与 `CLAUDE.md`“开发前必须”）。

### 1.4 范围

**本文包含**：

- 并行开发模型与流程：契约生命周期、集成节奏、负责人与 CODEOWNERS、就绪定义与完成定义（第 2 章）；
- 仓库、构建与工程结构，模块模板与命名规范（第 3 章）；
- `platform-api` 中全部跨模块契约的完整签名：服务接口、扩展点、领域事件、端口、共享类型（第 4 章）；
- platform-core 的运行机制（模块管理器、扩展点注册表、事件、站点上下文、安全、任务等）与 infrastructure 适配器（第 5 章）；
- platform-core 的表结构与 DDL（第 6 章），数据库通用约定与框架表（第 7 章）；
- 前端工程框架：Admin 外壳、前台渲染框架、共享包（第 8 章）；
- 测试框架、测试替身与契约测试（第 9 章），质量门禁与 CI（第 10 章），本地开发环境（第 11 章）；
- 模块设计文档模板（第 12 章）与并行开发计划（第 13 章），`module.json` 的 JSON Schema（附录 A）。

**本文不包含**：

- 各业务模块的领域模型、表结构、接口与界面：在各自的模块设计文档中；
- 页面、组件、模板 JSON Schema 的完整规范与 Design System 规范：见系统设计附录 C 第 5、6 项（本文只规定其工程位置与校验方式）；
- 生产部署：云厂商、CDN 与部署由业主自行负责（系统设计 27 章、32 章 Q3），本文只给出本地与 CI 环境；
- 具体的第三方组件版本：由 ADR-011 在 M0 验证后锁定。

### 1.5 规范用语与约定

| 用语 | 含义 |
|---|---|
| 必须 / 不得 | 强制要求，违反即不能合入；多数配有自动化检查（第 10 章） |
| 应 / 不应 | 默认要求；偏离时在 PR 中说明理由 |
| 可以 | 允许但不要求 |
| 【V2+】 | 预留设计，V1 只保证不阻碍其实现（与系统设计一致） |

其他约定：

- 模块用**模块 ID** 指称（`content`、`inquiry` 等，见系统设计 3.2）；platform-core 在 `module.json` 与数据库中的模块 ID 为 `core`，platform-api 只有契约，没有模块 ID。
- Java 基础包名为 `com.b2bweb`，Maven groupId 为 `com.b2bweb`，前端包作用域为 `@b2bweb/*`（M0 开工前可以统一替换为正式名称，替换时同步修改本文）。
- 文中的 GitHub 账号与团队（如 `@arch-lead`）是占位符，M0 时替换为真实账号。
- 表格中的“系统设计 N.M”指《系统设计说明书》V1.9 的章节。

### 1.6 术语

在系统设计 0.3 术语表之外，本文使用以下术语：

| 术语 | 含义 |
|---|---|
| 契约（Contract） | 定义在 `platform-api` 中、供多个模块共同遵守的类型：服务接口、扩展点接口、领域事件、端口接口、共享值对象与 DTO（第 4 章） |
| 服务接口（Service API） | 由某个模块（提供方）实现、供其他模块同步调用的接口，命名为 `XxxApi`（如 `ContentApi`） |
| 提供方 / 消费方 | 实现某个服务接口的模块 / 调用它的模块；对扩展点而言为“实现方 / 收集方” |
| 端口（Port） | `platform-api` 中定义、由 infrastructure 中的适配器实现的技术能力接口（如 `ObjectStorage`、`TranslationEngine`） |
| platform-testkit | 框架组在 M0 提供的测试库（Maven 模块）：每个服务接口的内存实现、每个端口的假实现、扩展点示例实现、契约测试套件与测试夹具（第 9 章） |
| 测试替身（Test Double） | testkit 中的内存实现（`InMemoryXxxApi`）或假实现（`FakeXxx`），使模块不依赖其他模块的实现即可开发与测试 |
| 契约测试（Contract Test） | testkit 中针对一个契约的抽象测试套件；提供方的真实实现与 testkit 中的替身必须通过同一套契约测试，保证替身与真实实现行为一致 |
| 契约冻结 | M0 结束时发布 `platform-api` 1.0.0；此后任何契约变更都按 2.3 的流程评审 |
| 平台版本 | `platform-api` 的语义化版本；`module.json` 的 `platform` 字段按它校验（附录 A） |
| 框架组 | 负责 M0 交付与之后框架维护的成员（含架构负责人），见 2.6 |
| 模块负责人 | 对一个模块的四个位置（3.6）与模块设计文档负责的成员，在 CODEOWNERS 中登记 |
| 就绪定义（DoR） | 一个模块可以开始编码的条件（2.7） |
| 完成定义（DoD） | 一个 PR 或一个模块的里程碑切片可以视为完成的条件（2.8） |
| 里程碑切片 | 某个模块在某个里程碑（M1–M4）需要交付的功能子集（13.5） |
| 替身退场 | 提供方的真实实现合入后，`application` 中改为装配真实实现；替身继续保留在 testkit 中供其他模块的测试使用（2.5） |

### 1.7 文档维护规则

| 规则 | 说明 |
|---|---|
| 位置与负责人 | 本文位于 `docs/framework/platform-framework.md`，CODEOWNERS 指定架构负责人为必审人（2.6）；框架组成员维护各自实现部分对应的章节 |
| 修改方式 | 一律通过 PR；与代码相关的修改（契约、机制、命名、门禁）与代码、测试放在同一个 PR 中（系统设计 29.6） |
| 契约变更 | 第 4 章的任何变更先按 2.3 的流程评审（ADR-018），并在同一个 PR 中同步修改 `platform-api`、`platform-testkit` 中的替身与契约测试 |
| 版本号 | 文档版本为 `V{主}.{次}`：主版本与 `platform-api` 的主版本一致（V1.x 对应 `platform-api` 1.x）；新增契约或新增规定时次版本加 1；只做文字澄清时不改版本号，但记入修订记录 |
| 章节编号 | 第 1–13 章与附录 A 的编号固定，模块设计文档按编号引用本文；新增内容追加为新的小节，不重排已有编号；删除的小节保留标题并注明“已废弃（原因、日期）” |
| 与系统设计同步 | 本文不得改变系统设计的结论；需要改变时先写 ADR、修订系统设计，再修订本文（1.2） |
| 与模块文档同步 | 契约变更合入后，框架组在同一个 PR 或紧随的 PR 中通知并协助受影响模块更新其设计文档第 2 节 |
| 引用检查 | CI 中的文档检查脚本（第 10 章）检查：模块设计文档中出现的服务接口、扩展点、事件、端口名称必须在本文第 4 章中存在；引用的本文章节号、系统设计章节号与 ADR 编号必须存在（ADR-018） |
| 语言与格式 | 中文书写；代码中的标识符用英文，Javadoc 与注释可以用中文；表格与代码块使用标准 Markdown，Mermaid 图只表达结构，不承载唯一的规范信息 |

### 1.8 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| V1.0 | 2026-10-07 | 初稿：依据系统设计 V1.4 与 ADR-018 编写；数据库通用约定与 platform-core 数据模型由原《领域模型与数据库设计》并入（第 6、7 章） |
| V1.1 | 2026-10-20 | 补充 8.6.3 前台视觉基线与设计稿交付边界，依据系统设计 V1.5；现有 platform-api 契约不变 |
| V1.2 | 2026-10-20 | 为11项实现前设计问题补齐4.10统一契约草案、事务/恢复协议和新增表；ADR-019～025待评审，未冻结或实现 |
| V1.3 | 2026-10-20 | 整改DR-01～04：内容级审校策略与明确写锁、翻译目标版本、媒体三阶段删除、原子下载提交及持久幂等结果；统一事件/缓存规则。ADR仍待评审，运行验证另记 |
| V1.4 | 2026-10-20 | M0-07原型反馈：明确RR当前读、锁内保留期判断、下载唯一冲突后新事务重放及恢复保留已捕获版本；业务服务验收仍待实现 |
| V1.5 | 2026-10-20 | M0-07B反馈：排他初始化互斥行、事务要求、租约复核、拓扑检查点与语言范围去重；65项原型回归，正式服务门禁保留 |
| V1.6 | 2026-10-20 | M0-07C反馈：恢复门闩覆盖读写与旧RDB代次，可靠失效不经BufferStore去抖，统一freshUntil示例；保持既有DDL和跨模块签名 |
| V1.7 | 2026-10-20 | 正式工程RC：完整契约编译、设置对象与启动回调统一、模块停用策略读取、Core技术表V002/V003、实际任务/调度与事务协调器；ADR-026～029待评审。保留G0完整门槛，不以目录覆盖代替全部契约语义验收 |
| V1.8 | 2026-10-21 | ADR-030：补齐预览用途、签发人及编辑授权契约，统一内容权限码；保持待评审 |
| V1.10 | 2026-10-21 | 42接口共享基础行为套件、34事件样例及目录门禁；ADR-032节点租约/审计维护/观测候选；真实imgproxy链和SSR引用预设装配，未替代完整G0与人工签审 |
| V1.9 | 2026-10-21 | 正式HTTP预览、画布及Schema底座；ADR-031持久恢复边界和Core V004追加迁移；搜索空投影契约说明统一；G0仍待完整行为、安全和人工评审 |

---

## 2. 并行开发模型

### 2.1 目标与原则

**目标**：M0 之后，17 个业务模块可以由不同的人或不同的 AI 编码会话同时开发；任何模块都不需要等待另一个模块完成才能编码、测试与合入；集成问题前移到契约设计阶段解决，而不是在里程碑末尾集中暴露。

| 原则 | 含义 | 依据 |
|---|---|---|
| 契约优先 | 模块之间的每一种交互（服务接口、扩展点、事件、端口、共享类型）在编码之前以完整签名定义在 `platform-api` 中，并写入本文第 4 章；模块设计文档只引用，不定义 | 系统设计 3.3 R3；ADR-002；ADR-018 |
| 框架先行 | M0 一次性交付平台框架：platform-core、契约冻结的 platform-api、platform-testkit、infrastructure 适配器、application 装配、Admin 外壳、前台渲染框架、共享前端包、CI 门禁与本地环境 | 系统设计 30（M0）；ADR-018 |
| 替身开发 | 模块在编译期只依赖 `platform-api`（以及 platform-core 的开发支撑包，见 3.3），测试时依赖 `platform-testkit`；其他模块的能力一律通过 testkit 中的测试替身获得 | ADR-018 |
| 替身保真 | 提供方的真实实现与 testkit 中的替身通过同一套契约测试；替身行为与真实实现不一致视为缺陷，由框架组修复 | ADR-018 |
| 不互相等待 | 消费方不等提供方：用替身先行开发；提供方不等消费方：用契约测试验证实现；扩展点的收集方不等实现方：用 testkit 中的扩展点示例实现开发 | 本节 |
| 主干集成 | 主干开发、短生命周期分支、squash 合并；每个 PR 都对全部模块完整构建，主干随时可构建、可部署到 dev | 系统设计 29.4、27.3 |
| 场景验收 | 开发按模块并行，验收仍按系统设计 30 章的里程碑与 1.3 的场景（S1–S13）进行 | 系统设计 30；ADR-018 |

### 2.2 开发阶段总览

```text
阶段 0  M0 框架
        ├─ platform-api 1.0.0（契约冻结）+ platform-testkit（替身、契约测试、夹具）
        ├─ platform-core、infrastructure、application、CI、本地环境
        ├─ Admin 外壳、前台渲染框架、共享前端包、组件 Schema 校验
        └─ 与此同时：各模块负责人编写模块设计文档（第 12 章模板），在 M0 结束前完成评审
                │
                ▼  M0 验收（13.1）：契约冻结 + testkit 就绪 + 模块文档评审通过（DoR）
阶段 1  模块并行开发
        ├─ 每个模块：编码 → PR（满足 PR 级 DoD）→ squash 合入主干
        ├─ 每个 PR：全量构建 + 架构测试 + 契约测试（主干持续集成）
        ├─ 每天：main 自动部署 dev，E2E 冒烟
        └─ 每周：集成演示；替身退场进度；契约变更请求的评审
                │
                ▼
阶段 2  里程碑集成验收 M1 → M2 → M3 → M4
        └─ 按 13.5 的里程碑切片集成，按 13.6 的场景验收；每个里程碑结束时演示与复盘
```

模块设计文档的编写可以与 M0 的后半段重叠：本文第 4 章的契约草案稳定（M0 中期评审）后，模块负责人即可开始编写，引用的契约若在评审中变化，随契约变更同步更新。

### 2.3 契约的生命周期

**契约的组成**：一个契约由四部分组成，四部分必须在同一个 PR 中一起变更：

| 组成 | 位置 |
|---|---|
| 规范文本（签名、语义、异常、幂等性、调用方向） | 本文第 4 章 |
| 代码（接口、记录类型、注解、事件） | `backend/platform/platform-api/` |
| 测试替身（服务接口的内存实现、端口的假实现、扩展点示例实现） | `backend/platform/platform-testkit/` |
| 契约测试（提供方与替身共同通过的抽象测试套件） | `backend/platform/platform-testkit/` |

**冻结**：M0 结束时发布 `platform-api` 1.0.0，即契约冻结。冻结之后：

| 变更类型 | 例子 | 流程 | 版本 |
|---|---|---|---|
| 兼容新增 | 新增服务接口或扩展点；在接口上新增带默认实现的方法；事件新增可空字段；新增端口 | 契约变更请求 → ADR（可以简写：背景、签名、受影响模块）→ 架构负责人批准 → 四部分同一个 PR 合入 | 次版本 +1（1.1.0） |
| 不兼容变更 | 修改或删除方法签名；改变语义；事件不兼容（系统设计 4.2） | 完整 ADR（含备选方案）→ 过渡期：旧签名标记 `@Deprecated(forRemoval = true)` 并至少保留到下一个里程碑结束；事件提升 `schemaVersion`，过渡期内同时支持新旧版本 → 所有消费方迁移后删除 | V1 期间不提升主版本：以“新增 + 废弃”代替；真正删除时次版本 +1，并在修订记录中列出 |
| 澄清 | Javadoc、规范文本措辞，不改变行为 | 普通 PR，架构负责人审核 | 修订号 +1（1.0.1） |

**契约变更请求**（CCR，Contract Change Request）：模块负责人在编写模块文档或开发中发现缺少契约时提出。

| 项 | 内容 |
|---|---|
| 提出方式 | 仓库 Issue，标签 `contract-change`，模板字段：需要的能力、调用方向（是否符合系统设计 3.3 R2）、选择服务接口 / 事件 / 扩展点的理由（P3）、提议的签名、受影响的模块、替身与契约测试的变化 |
| 答复时限 | 框架组 2 个工作日内给出结论（接受 / 修改后接受 / 拒绝并给出替代方案），避免阻塞并行开发 |
| 实施 | 接受后由框架组（或在框架组审核下由提出者）提交四部分同一个 PR；合入后在周集成会上通报 |
| 等待期间 | 消费方可以在**本模块内部**定义一个防腐接口（只在模块内可见），用本地替身先行开发；契约合入后改为调用 `platform-api`。该内部接口不得被其他模块使用，也不得放进 `platform-api` 以外的任何共享位置（系统设计 3.3 R3） |

**平台版本与模块描述符**：`platform-api` 的版本即平台版本。`module.json` 的 `platform` 字段声明模块兼容的平台版本范围（如 `>=1.0.0 <2.0.0`），启动时由模块管理器校验（系统设计 3.6，附录 A）。

### 2.4 模块的独立开发与测试

**依赖**：

| 范围 | 模块可以依赖 | 不得依赖 |
|---|---|---|
| 编译（`compile`） | `platform-api`；platform-core 的开发支撑包（持久化基类、Web 基础设施等，见 3.3 与第 5 章）；BOM 中管理的第三方库 | 其他业务模块、`infrastructure`、`application`；AI 厂商 SDK |
| 测试（`test`） | `platform-testkit`；测试库（JUnit 5、AssertJ、Testcontainers 等） | 其他业务模块的测试代码 |

这些规则由 Maven Enforcer（3.2）、ArchUnit 与 Spring Modulith `verify()`（第 10 章）自动检查。

**如何在不依赖其他模块的情况下开发**：

| 需要 | 做法 |
|---|---|
| 调用其他模块的服务接口 | 测试中注入 testkit 的内存实现（如 `InMemoryContentApi`），用夹具预置数据；断言调用结果与副作用（如 `InMemoryNotificationApi` 记录的发送请求） |
| 订阅其他模块的事件 | 在测试中直接构造 `platform-api` 中的事件记录并发布，用 Spring Modulith 的 Scenario API 验证监听器的效果；同一事件重复投递两次验证幂等（ADR-003） |
| 发布事件 | 用 Scenario API 或事件捕获夹具断言事件已在业务事务内发布、字段正确 |
| 实现扩展点 | 直接对实现类做单元测试；需要时用 testkit 提供的上下文夹具（如发布上下文、翻译上下文）构造入参 |
| 收集扩展点（如 content 收集 `PublishContributor`、translation 收集 `TranslatableContentProvider`） | 使用 testkit 中的扩展点示例实现（示例内容类型等）验证收集、过滤与编排逻辑 |
| 使用技术能力（存储、邮件、AI 引擎等） | 测试中使用端口的假实现（如 `InMemoryObjectStorage`、`FakeTranslationEngine`），不访问外部服务（系统设计 28 “AI 翻译”一行） |
| 访问本模块的表 | Testcontainers（MySQL 8.4）+ 本模块的 Flyway 迁移，只建本模块与框架需要的表 |
| 提供服务接口 | 提供方的测试类继承 testkit 中对应的契约测试套件，用真实实现运行（`XxxApiContractTest`） |
| 本地运行整个应用 | `application` 的本地配置中，尚未实现的服务接口由 testkit 的内存实现补位，使部分模块完成时也能启动与联调（机制见第 9、11 章） |
| 前端 | Admin 模块与主题组件基于 `@b2bweb/api-client` 的生成类型开发，用接口 Mock 与组件画廊（`/__gallery`）独立调试（第 8、9 章） |

测试的层级、替身清单与契约测试的写法见第 9 章。

### 2.5 集成节奏

| 频率 | 活动 | 产出 |
|---|---|---|
| 每个 PR | 全量构建全部模块；单元测试；架构测试；本模块的集成测试；全部契约测试；OpenAPI 生成与破坏性变更检查；前端检查（第 10 章） | 主干始终可构建 |
| 每次合入 main | 构建镜像 → 部署 dev → E2E 冒烟（系统设计 27.3） | dev 环境保持最新 |
| 每周 | 集成例会（30–60 分钟）：各模块进度与替身退场情况；待评审的契约变更请求；跨模块阻塞项；在 dev 上演示本周新打通的链路 | 集成看板更新（13.5） |
| 每个里程碑 | 集成验收：场景 E2E、Lighthouse、SEO 爬取检查、安全扫描等（系统设计 27.3、28、30）；演示与复盘 | 验收报告；必要时调整后续范围 |

**替身退场**：提供方的真实实现合入并通过契约测试后，`application` 装配真实实现，替身不再参与运行；替身继续保留在 testkit 中，供其他模块的单元与模块集成测试使用。真实实现参与的跨模块行为由 `application` 中的跨模块集成测试与里程碑场景 E2E 覆盖（第 9 章）。

### 2.6 模块负责人与 CODEOWNERS

**角色**：

| 角色 | 职责 | 人数 |
|---|---|---|
| 架构负责人 | 本文、系统设计说明书与 ADR 的维护；`platform-api`、`platform-core` 的必审人；契约变更评审；模块文档评审；里程碑集成验收的主持 | 1 |
| 框架组 | M0 交付；之后维护 platform-*、infrastructure、application、Admin 外壳与前台渲染框架、共享前端包、CI；落地契约变更（契约四部分）；处理替身与真实实现不一致的缺陷 | 1–2（含架构负责人） |
| 模块负责人 | 模块设计文档；模块四个位置（3.6）的代码；就绪与完成的自检；本模块提供的服务接口通过契约测试；本模块需要的契约变更请求 | 每个模块 1 人；一人可以负责多个模块 |
| 业主 / 产品负责人 | 确认里程碑范围与验收场景；关闭模块文档中需要业务确认的待定问题 | 1 |

团队规模按 2–6 人设计（系统设计 0.4 A6），分组建议见 13.4。

**CODEOWNERS**（`.github/CODEOWNERS`，账号为占位符）：

```text
# GitHub 按“最后一条匹配的规则”确定负责人：通用规则在前，具体规则在后。

# 默认
*                                                         @arch-lead

# 设计文档
/docs/architecture/                                       @arch-lead
/docs/adr/                                                @arch-lead
/docs/framework/                                          @arch-lead
/docs/modules/                                            @arch-lead

# 框架：platform-api、platform-core 只由架构负责人担任必审人
/backend/platform/platform-api/                           @arch-lead
/backend/platform/platform-core/                          @arch-lead
/backend/platform/platform-testkit/                       @fw-team
/backend/build/                                           @fw-team
/backend/pom.xml                                          @fw-team
/backend/modules/pom.xml                                  @fw-team
/backend/infrastructure/                                  @fw-team
/backend/application/                                     @fw-team
/frontend/                                                @fw-team
/deploy/                                                  @fw-team
/tools/                                                   @fw-team
/.github/                                                 @arch-lead

# 模块：每个模块一组，覆盖模块文档与四个位置（以 inquiry 为例，其他模块同理）
/docs/modules/inquiry.md                                  @owner-inquiry @arch-lead
/backend/modules/inquiry/                                 @owner-inquiry
/frontend/apps/admin/src/modules/inquiry/                 @owner-inquiry
/frontend/packages/component-schemas/components/inquiry/  @owner-inquiry
/frontend/packages/component-schemas/templates/inquiry/   @owner-inquiry
/frontend/packages/themes/industrial/components/inquiry/  @owner-inquiry
```

- `platform-core`、`platform-api` 的 PR 需要 2 人批准，其中包括架构负责人（系统设计 29.4）：CODEOWNERS 保证架构负责人必审，第二人的批准由仓库规则或 CI 检查保证（第 10 章）。
- 模块文档的负责人写为“模块负责人 + 架构负责人”：GitHub 只要求其中任一人批准，架构负责人对模块文档的评审由 12.3 的流程保证。
- 跨模块修改（同一个 PR 触及两个及以上模块的目录）必须在 PR 描述中说明原因（`CLAUDE.md` 提交规范），并由各相关模块负责人批准。

### 2.7 就绪定义（DoR）

模块（或模块的某个里程碑切片）满足以下全部条件后才能开始编码：

| # | 条件 | 检查方式 |
|---|---|---|
| 1 | 模块设计文档按第 12 章模板完成，评审通过并合入主干，状态为“已评审” | PR 评审（12.3） |
| 2 | 文档第 2 节引用的每一个服务接口、扩展点、事件、端口都已存在于已发布的 `platform-api` 中，或对应的契约变更已合入 | CI 文档检查脚本（1.7） |
| 3 | 需要的测试替身在 `platform-testkit` 中可用（服务接口的内存实现、端口的假实现、扩展点示例实现） | 框架组确认 |
| 4 | `module.json` 草案通过 Schema 校验（附录 A）；`requires` 符合系统设计 3.3 R2，无循环依赖 | 校验脚本 |
| 5 | 表结构与 DDL 已在 MySQL 8.4 上执行通过；每张表标明站点级或全局（全局表列入豁免清单，第 7 章）；表前缀正确 | DDL 执行记录；评审 |
| 6 | 接口路径、权限码、错误码、事件、组件类型、文案键等命名符合 3.7 | 评审 |
| 7 | 本切片的验收标准可测试，并对应到系统设计 1.3 的场景（S1–S13）或模块自身的验收项 | 评审 |
| 8 | 阻塞开发的待定问题已关闭；非阻塞问题留在文档第 14 节，并写明最晚解决的里程碑 | 评审 |
| 9 | 模块负责人已登记到 CODEOWNERS；新增模块的 ADR 已为 Accepted（系统设计 29.5） | CODEOWNERS；ADR 索引 |

### 2.8 完成定义（DoD）

**PR 级**（每个 PR 都必须满足）：在系统设计 29.6 的基础上：

| # | 条件 |
|---|---|
| 1 | 代码、测试、文档在同一个 PR 中；模块设计文档与实现保持一致（接口、表、配置、事件） |
| 2 | 第 10 章的全部 CI 门禁通过 |
| 3 | 新的后台功能有对应的权限点（已在 `module.json` 声明）与审计 |
| 4 | 新的前台组件有 Schema、变体截图、无障碍检查；固定文案使用文案键，不硬编码（系统设计 21.7） |
| 5 | 新增的可翻译字段已在 `TranslatableContentProvider` / `TranslatableResourceProvider` 中登记；组件属性正确标注 `x-translatable` |
| 6 | 新的内容类型实现了系统设计 29.2 第 3 步列出的全部扩展点 |
| 7 | 数据库迁移遵循 expand / contract；新表的站点级 / 全局归属已确认 |
| 8 | 涉及 API 时已重新生成 `@b2bweb/api-client` 并提交 |
| 9 | 涉及组件 Schema 的破坏性变更已提升版本并附迁移函数 |
| 10 | 只依赖 `platform-api`、platform-core 开发支撑包与 testkit；没有跨模块 import、跨模块表访问 |

**模块里程碑切片级**（某模块在某里程碑的切片全部完成时）：

| # | 条件 |
|---|---|
| 1 | 切片范围内的功能全部合入主干，文档第 13 节对应的验收项全部通过 |
| 2 | 本模块提供的每个服务接口：真实实现通过 testkit 契约测试；替身行为与之一致 |
| 3 | `module.json` 与代码一致：事件（CI 检查）、权限、菜单、组件、设置 Schema |
| 4 | 可停用模块：系统设计 3.8 的停用行为有测试；`analyzeDisableImpact` 已实现 |
| 5 | 站点隔离测试覆盖本切片新增的全部后台接口（系统设计 5.3、28） |
| 6 | 演示数据集（系统设计 28）已补充本切片需要的数据 |
| 7 | 在 `application` 中以真实实现装配后，本切片参与的场景 E2E 在 dev 环境通过 |
| 8 | 模块设计文档状态更新为“已实现（M?）”，第 14 节的待定问题已处理或移交下一切片 |

### 2.9 与里程碑的关系

系统设计 30 章的垂直切片里程碑保留，作为**集成与验收的节奏**；本章的并行模型规定**开发如何组织**。两者的关系：

```text
                     M0                 M1          M2          M3          M4
框架组        ████ 框架交付 ████▶  契约维护、替身维护、集成支持、框架缺陷修复 ──────────────▶
模块文档            ██ 编写与评审 ██▶  按切片补充与修订 ───────────────────────────────▶
模块开发                           ████ 全部模块按各自的里程碑切片并行开发 ███████████████▶
集成验收                                     ▲ M1 验收   ▲ M2 验收   ▲ M3 验收   ▲ M4 验收
```

| 里程碑 | 系统设计 30 章的目标 | 并行模型中的含义 |
|---|---|---|
| M0 工程底座 | 可持续开发的骨架 | 框架交付；`platform-api` 1.0.0 冻结；testkit 就绪；全部 M1 涉及模块的设计文档评审通过（13.1） |
| M1 中文单语闭环 | 内容 → SEO → 询盘 | M1 切片涉及的模块完成真实实现并替身退场；场景 S1（不含英语）、S2、S4（不含 GA4）验收 |
| M2 页面与设计系统 | 运营自主搭建页面 | M2 切片集成；S3、S10 验收 |
| M3 SEO 与获客完善 | SEO 完整、转化可追踪、AI 翻译上线 | M3 切片集成；S5、S7、S8、S9、S11、S12、S13 验收 |
| M4 规模化运营 | 支撑大量内容与上线 | M4 切片集成；S6、安全扫描、恢复演练 |

模块不必等到“轮到它的里程碑”才开工：只要满足 DoR，就可以提前开发后续里程碑的切片（13.3）；但集成与验收按里程碑顺序进行，未到验收的切片不影响当前里程碑的发布。

---

## 3. 仓库与工程结构

### 3.1 Monorepo 目录

在系统设计 29.1 的基础上细化（增加 `platform-testkit`、构建工程、前端套件包、脚手架与检查脚本）：

```text
b2b-platform/
├── CLAUDE.md                                # AI 编码助手的架构边界
├── README.md                                # 仓库入口：环境要求、常用命令、文档索引
├── .editorconfig  .gitattributes  .gitignore
├── .sdkmanrc                                # JDK 版本（ADR-011，11.1）
├── .run/                                    # IntelliJ IDEA 运行配置（11.4）
├── .github/
│   ├── CODEOWNERS                           # 2.6
│   ├── pull_request_template.md             # PR 级 DoD 检查项（2.8）
│   ├── ISSUE_TEMPLATE/contract-change.md    # 契约变更请求（2.3）
│   └── workflows/                           # CI（第 10 章）
├── backend/                                 # Maven 多模块（3.2）
│   ├── pom.xml                              # 聚合 POM
│   ├── mvnw  mvnw.cmd  .mvn/                # Maven Wrapper（版本锁定）
│   ├── build/
│   │   ├── bom/pom.xml                      # b2bweb-bom：依赖版本清单
│   │   └── parent/pom.xml                   # b2bweb-parent：插件管理、编译参数、通用 Enforcer 规则
│   ├── platform/
│   │   ├── platform-api/                    # L1 契约（第 4 章）
│   │   ├── platform-core/                   # L0 内核（第 5、6 章）
│   │   └── platform-testkit/                # 测试替身、契约测试、夹具（第 9 章）
│   ├── modules/
│   │   ├── pom.xml                          # b2bweb-modules：业务模块的父 POM（统一依赖与边界规则）
│   │   ├── content/  url/  media/  taxonomy/  form/  notification/  translation/   # L2
│   │   ├── product/  article/  page/  inquiry/  navigation/                        # L3
│   │   └── delivery/  seo/  graph/  search/  tracking/                             # L4
│   ├── infrastructure/                      # 端口适配器（第 5 章）
│   └── application/                         # Spring Boot 启动与装配、跨模块集成测试
├── frontend/                                # pnpm workspace（3.5、第 8 章）
│   ├── package.json  pnpm-workspace.yaml  pnpm-lock.yaml  tsconfig.base.json
│   ├── apps/
│   │   ├── website/                         # Nuxt 4 前台
│   │   └── admin/                           # Vue 3 + Vite 后台
│   └── packages/
│       ├── design-tokens/  component-schemas/  api-client/
│       ├── admin-kit/  website-kit/  shared/
│       └── themes/industrial/               # V1 主题
├── docs/
│   ├── architecture/system-design.md        # 系统设计说明书
│   ├── framework/platform-framework.md      # 本文
│   ├── adr/                                 # 架构决策记录
│   ├── modules/<模块ID>.md                  # 模块设计文档（第 12 章）
│   ├── api/                                 # OpenAPI 文档快照（由 CI 生成，供 oasdiff 比对，第 10 章）
│   └── runbooks/                            # 运维手册（系统设计 27.5）
├── deploy/
│   ├── docker/                              # Dockerfile：backend、website、admin
│   └── compose/                             # 本地环境（第 11 章，目录结构见 11.1）与参考部署
└── tools/
    ├── scaffold/                            # 新模块脚手架（3.8）
    ├── checks/                              # 自定义检查脚本：${} 扫描、表前缀扫描、module.json 一致性、迁移哈希、文档引用（第 10 章）
    ├── ci/                                  # 门禁范围计算、门禁自检、结果汇总（10.1）
    ├── dev/b2b.mjs                          # 本地命令入口（11.4）
    ├── mock-openai/                         # 本地 OpenAI 兼容模拟服务（11.7）
    ├── perf/                                # k6 脚本与 Lighthouse CI 配置（10.5）
    ├── security/                            # OWASP ZAP 规则（10.5）
    ├── seo-check/                           # SEO 爬取检查（系统设计 28）
    └── demo-data/                           # 演示数据集（系统设计 28）
```

顶层规则：

- `backend/` 与 `frontend/` 是两个独立的构建根，不互相调用构建工具；唯一的跨栈构建关系是 `application` 在打包时从 `frontend/packages/component-schemas` 复制组件与模板定义到 classpath（系统设计 10.4、29.1）。
- 生成物（`@b2bweb/api-client` 的生成代码、Token 构建产物）按第 8、10 章的规定决定是否提交；CI 检查“生成结果与源一致”（系统设计 24.3）。
- 任何密钥、API Key、本地配置都不进入仓库（`.gitignore` + gitleaks，系统设计 26.6）。

### 3.2 后端 Maven 工程

**构件清单**（groupId 均为 `com.b2bweb`）：

| 目录 | artifactId | 打包 | 父 POM | 说明 |
|---|---|---|---|---|
| `backend/` | `b2bweb-backend` | pom | — | 聚合 POM：只列出子工程，不承载配置 |
| `backend/build/bom/` | `b2bweb-bom` | pom | — | 依赖版本清单：内部构件与第三方库 |
| `backend/build/parent/` | `b2bweb-parent` | pom | — | 父 POM：导入 `b2bweb-bom`；插件管理；编译参数；通用 Enforcer 规则 |
| `backend/platform/platform-api/` | `platform-api` | jar | `b2bweb-parent` | 契约 |
| `backend/platform/platform-core/` | `platform-core` | jar | `b2bweb-parent` | L0 内核 |
| `backend/platform/platform-testkit/` | `platform-testkit` | jar | `b2bweb-parent` | 测试库（其他工程以 `test` 范围依赖） |
| `backend/modules/` | `b2bweb-modules` | pom | `b2bweb-parent` | 业务模块的父 POM 兼聚合 POM |
| `backend/modules/<id>/` | `module-<id>` | jar | `b2bweb-modules` | 业务模块，如 `module-inquiry` |
| `backend/infrastructure/` | `infrastructure` | jar | `b2bweb-parent` | 端口适配器 |
| `backend/application/` | `application` | jar（Spring Boot 可执行包） | `b2bweb-parent` | 装配与启动 |

**版本**：

- 全部内部构件共用一个版本，使用 CI 友好版本 `${revision}`（默认 `1.0.0-SNAPSHOT`，发布标签时由 CI 传入），配合 flatten 插件生成发布用 POM。
- `platform-api` 的版本即平台版本（2.3）；`module.json` 中的 `version` 是模块自身的语义化版本，与 Maven 版本相互独立（附录 A）：前者描述“构建”，后者描述“模块的数据与行为”，驱动 `onInstall` / `onUpgrade`（系统设计 3.6）。
- 第三方库的版本只在 `b2bweb-bom` 中声明，取值以 ADR-011 的版本表为准；子工程中不得写版本号（Enforcer 检查）。

**`b2bweb-bom` 的内容**：

| 类别 | 内容 |
|---|---|
| 导入的 BOM | Spring Boot（依赖管理）、Spring Modulith、Testcontainers |
| 显式管理的第三方库 | MyBatis-Plus（与 Spring Boot 4 的集成及行级拦截器）、TSID 生成器（ADR-014）、Apache Tika、jsoup、Bucket4j、ShedLock、springdoc-openapi、libphonenumber、ICU4J（slug 音译，系统设计 13.3）、Apache POI（Excel 导入导出）、语义化版本解析库、JSON Schema 校验库（`module.json`、组件 Schema、设置 Schema）、ArchUnit、以及 infrastructure 使用的客户端库（S3 兼容存储、邮件、Meilisearch、GeoIP 等） |
| 内部构件 | `platform-api`、`platform-core`、`platform-testkit`、`infrastructure`、全部 `module-<id>`，版本为 `${revision}` |

**`b2bweb-parent` 的内容**：

| 项 | 规定 |
|---|---|
| 编译 | `maven.compiler.release` 取 ADR-011 锁定的 JDK 版本（选型方向为 25，最低 21）；开启 `-parameters`；源码编码 UTF-8 |
| 测试插件 | Surefire 运行单元测试（`*Test`）；Failsafe 运行集成测试（`*IT`）；JaCoCo 统计覆盖率（领域层 ≥ 80%，系统设计 28） |
| Enforcer 通用规则 | Maven 与 JDK 版本下限；依赖版本收敛；子工程不写版本号；**禁止 AI 厂商 SDK**（`bannedDependencies`，清单在父 POM 中维护，如 `com.openai:*`、`com.theokanning.openai-gpt3-java:*`、`com.azure:azure-ai-openai`、`com.alibaba:dashscope-sdk-java`、`dev.langchain4j:*`、`org.springframework.ai:*`；系统设计 5.9、ADR-017、`CLAUDE.md` 禁止 12） |
| 其他 | flatten 插件（`${revision}`）；Spring Boot 插件只在 `application` 中启用 |

**`b2bweb-modules` 的内容**（所有业务模块的父 POM，使模块的 `pom.xml` 只需写 artifactId 与模块特有的第三方库）：

- 统一依赖：`platform-api`、`platform-core`（compile）；`platform-testkit`、Spring Boot 测试、Spring Modulith 测试、Testcontainers MySQL（test）。
- **模块边界规则**（Enforcer `bannedDependencies`）：排除 `com.b2bweb:*`，再放行 `com.b2bweb:platform-api`、`com.b2bweb:platform-core`、`com.b2bweb:platform-testkit`。任何模块依赖其他模块、`infrastructure` 或 `application` 时构建直接失败（系统设计 3.3 R1）。

业务模块的 `pom.xml` 示例：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.b2bweb</groupId>
    <artifactId>b2bweb-modules</artifactId>
    <version>${revision}</version>
  </parent>
  <artifactId>module-inquiry</artifactId>
  <name>B2B Web :: Module :: inquiry（询盘）</name>
  <!-- platform-api、platform-core、platform-testkit 已由父 POM 声明；这里只添加本模块特有的第三方库（版本由 BOM 管理） -->
  <dependencies>
    <dependency>
      <groupId>com.googlecode.libphonenumber</groupId>
      <artifactId>libphonenumber</artifactId>
    </dependency>
  </dependencies>
</project>
```

**常用构建命令**（在 `backend/` 下执行）：

| 命令 | 用途 |
|---|---|
| `./mvnw verify` | 提交前必须通过：全部模块的编译、单元测试、架构测试、集成测试（`CLAUDE.md`） |
| `./mvnw -pl modules/inquiry -am verify` | 只构建与测试一个模块（`-am` 同时构建它依赖的 platform-api、platform-core、platform-testkit） |
| `./mvnw -pl modules/inquiry -am verify -DskipITs` | 只运行单元测试与架构测试，用于快速反馈 |

CI 中的完整门禁见第 10 章。

### 3.3 平台工程的职责

| 工程 | 包 | 职责 | 可以依赖 | 不得包含 |
|---|---|---|---|---|
| `platform-api` | `com.b2bweb.api.*`，按领域分包：`common`、`site`、`security`、`content`、`url`、`media`、`taxonomy`、`form`、`notification`、`translation`、`seo`、`delivery`、`extension`、`event`、`port` | 全部跨模块契约：服务接口、扩展点接口及其注解、领域事件、端口接口、共享值对象与 DTO；`module.json` 的 JSON Schema（附录 A） | JDK；第 4 章列出的少量注解与数据类型库（如 Jackson 的 `JsonNode`） | 任何实现、Spring Bean、持久化代码、对其他内部构件的依赖 |
| `platform-core` | `com.b2bweb.core.*` | L0 内核（系统设计 3.2）：实现 `platform-api` 中由 core 提供的契约（站点与语言、身份与权限、设置、任务、审计、字典、界面文案、站点资料、数据主体请求）；运行机制（模块管理器、扩展点注册表、事件基础、站点上下文与行级拦截、`ModuleGuardFilter`、统一错误处理、ID 生成等）；供模块编译期使用的开发支撑包 | `platform-api`；Spring Boot、Spring Modulith、MyBatis-Plus 等 | 业务模块的任何代码（P1）；具体技术适配器 |
| `platform-testkit` | `com.b2bweb.testkit.*` | 每个服务接口的内存实现（`InMemoryXxxApi` / `FakeXxxApi`）、每个端口的假实现、扩展点示例实现、契约测试套件、测试夹具、ArchUnit 规则集 | `platform-api`；测试库 | `platform-core`（见下文）；业务模块 |
| `infrastructure` | `com.b2bweb.infrastructure.<技术>` | `platform-api` 中端口的适配器：对象存储、邮件传输、搜索客户端、缓存、CDN 清除、AI 翻译引擎（OpenAI 兼容接口）、GeoIP、人机验证、病毒扫描、图片 URL 签名等（清单见第 5 章） | `platform-api`、`platform-core`；第三方客户端库 | 业务逻辑；业务模块；AI 厂商 SDK |
| `application` | 启动类 `com.b2bweb.B2bWebApplication`；装配配置在 `com.b2bweb.app` | 装配 platform-core、infrastructure 与全部业务模块；配置文件与 Profile；迁移模式（`--mode=migrate`，系统设计 3.6）；跨模块集成测试与场景测试；Spring Modulith `verify()` 与模块文档生成 | 全部内部构件 | 业务逻辑 |

**依赖关系**（箭头表示 Maven 依赖）：

```text
application ──────▶ module-<id>（×17）、infrastructure、platform-core
module-<id> ──────▶ platform-core（仅开发支撑包）、platform-api        test：platform-testkit
infrastructure ───▶ platform-core、platform-api                        test：platform-testkit
platform-core ────▶ platform-api                                       test：platform-testkit
platform-testkit ─▶ platform-api
platform-api ─────▶ （无内部依赖）
```

说明：

1. **层号与 Maven 依赖方向**。系统设计 3.1 的层号描述运行时职责（L0 内核最底层），3.3 规定“任何模块只能依赖 platform-core 与 platform-api”。在 Maven 中，`platform-api` 是不含任何实现的纯契约构件，位于依赖链的最底端；`platform-core` 实现其中由 core 提供的契约，因此依赖 `platform-api`。这样做使业务模块对 core 能力（站点、权限、设置、任务等）的调用同样通过 `platform-api` 中的接口进行，可以用 testkit 的替身替换，core 未完成时模块也能独立开发。P1（core 不依赖业务模块）与 R1 不受影响。
2. **模块对 platform-core 的编译依赖只限开发支撑包**（持久化基类、Web 基础设施、配置属性等，清单见第 5 章）；core 的其他包是内部实现，模块不得引用（ArchUnit 检查）。对 core 业务能力的调用一律通过 `platform-api` 中的服务接口。
3. **testkit 不依赖 platform-core**：platform-core 自己的测试也要使用 testkit（契约测试、端口假实现），若 testkit 依赖 platform-core 会形成 Maven 反应堆循环。需要 platform-core 运行时的测试支撑（如带站点行级拦截的持久化测试切片）由 platform-core 自己提供，见第 5、9 章。
4. **端口的位置**：系统设计 3.3 R1 中“infrastructure 定义的端口接口”在本框架中统一放在 `com.b2bweb.api.port`，由 infrastructure 实现。业务模块因此不依赖 infrastructure 构件。
5. **Spring Modulith 的模块识别**：启动类位于 `com.b2bweb`，其直接子包被识别为应用模块：`api`、`core`、`infrastructure`、`app` 与 17 个业务模块（`content`、`product` …）。`api` 声明为开放模块，供所有模块引用；业务模块的 `allowedDependencies` 为 `api` 与 `core :: *`（仅命名技术支撑接口，3.4）。具体配置见第 5、10 章。

> 评审确认事项：说明 1 的 Maven 依赖方向（platform-core → platform-api）是对系统设计 3.3 图示（“L1 platform-api ──▶ L0 platform-core”）的细化，评审确认后在系统设计 3.3 中补充说明。

### 3.4 后端模块模板

在系统设计 3.4 的基础上细化。以 inquiry 为例：

```text
backend/modules/inquiry/
├── pom.xml                                   # 父 POM b2bweb-modules；artifactId module-inquiry（3.2）
└── src/
    ├── main/
    │   ├── java/com/b2bweb/inquiry/
    │   │   ├── package-info.java             # @ApplicationModule：显示名与允许的依赖
    │   │   ├── InquiryModule.java            # PlatformModule 实现；模块根包中只有这两个文件
    │   │   ├── domain/                       # 聚合、实体、值对象、领域服务、仓储接口；不依赖 Spring 与 MyBatis
    │   │   ├── application/                  # 应用服务：事务边界、@RequiresPermission、调用 platform-api 服务、发布事件
    │   │   ├── infrastructure/persistence/   # MyBatis-Plus Mapper、持久化对象（PO）、仓储实现
    │   │   ├── web/admin/                    # /api/admin/v1/inquiry/**：控制器与请求 / 响应 DTO
    │   │   ├── web/publicapi/                # /api/public/v1/inquiry/**（如有）
    │   │   ├── web/internal/                 # /api/internal/v1/inquiry/**（如有）
    │   │   ├── facade/                       # 本模块提供的 platform-api 服务接口的实现（如有）
    │   │   ├── extension/                    # 扩展点实现（@Extension）
    │   │   ├── listener/                     # 领域事件监听器（@ApplicationModuleListener）
    │   │   ├── job/                          # 定时任务、sys_job 任务处理器
    │   │   └── config/                       # 配置属性类（b2bweb.inquiry.*）与模块内的 Spring 配置
    │   └── resources/
    │       ├── module.json                   # 模块描述符（附录 A）
    │       ├── settings.schema.json          # 站点设置的 JSON Schema（如有；module.json 的 settings 指向它）
    │       ├── db/migration/inquiry/         # Flyway 迁移：V001__init.sql …
    │       └── messages/inquiry.properties   # 后台错误信息（只有中文），键为错误码
    └── test/
        ├── java/com/b2bweb/inquiry/
        │   ├── domain/                       # 领域单元测试（*Test）
        │   ├── application/                  # 应用服务测试（注入 testkit 替身）
        │   ├── InquiryModuleIT.java          # 模块集成测试（@ApplicationModuleTest + Testcontainers）
        │   └── ArchitectureTest.java         # 运行 testkit 中的 ArchUnit 规则集
        └── resources/
```

`package-info.java` 示例：

```java
/**
 * 询盘模块（L3）：线索捕获、分配、跟进、询价篮与报表。设计文档：docs/modules/inquiry.md。
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "询盘",
        allowedDependencies = { "api", "core :: *" })   // 只允许依赖 platform-api 与 platform-core 的开发支撑包
package com.b2bweb.inquiry;
```

**包内的依赖方向**（ArchUnit 检查，规则集由 testkit 提供，第 9、10 章）：

| 包 | 可以依赖 | 不得依赖 |
|---|---|---|
| `domain` | JDK；`platform-api` 中的值对象 | Spring、MyBatis；本模块的其他包 |
| `application` | `domain`；`platform-api`；core 开发支撑包 | `web`；持久化的具体类（通过 `domain` 中的仓储接口访问） |
| `infrastructure.persistence` | `domain`；core 开发支撑包；MyBatis-Plus | `application`、`web` |
| `web.*` | `application` | Mapper 与 PO；把领域对象直接作为响应体 |
| `facade`、`extension`、`listener`、`job` | `application`；`platform-api` | Mapper 与 PO（必须经过应用服务） |
| `config` | 本模块的其他包；core 开发支撑包 | 其他模块 |

**放置规则**：

| 规则 | 说明 |
|---|---|
| 根包只有两个文件 | `package-info.java` 与 `<Id>Module.java`；其余代码都在子包中 |
| 事务与权限在应用服务 | `@Transactional` 与 `@RequiresPermission` 放在 `application` 层的方法上（系统设计 26.2）；控制器只做参数绑定与 DTO 转换 |
| 事件在事务内发布 | 应用服务在业务事务内发布 `platform-api` 中的领域事件（ADR-003）；监听器使用 `@ApplicationModuleListener`，必须幂等并丢弃过期事件 |
| Mapper 只在本模块的持久化包中使用 | 只访问本模块前缀的表；不跨模块 JOIN；MyBatis 只用 `#{}`（系统设计 25.5、26.3） |
| 本模块的类不对外 | 模块之间在编译期不可见；对外能力只通过 `facade` 实现的 `platform-api` 接口、发布的事件与扩展点实现提供 |
| 资源文件名带模块 ID | 多个 jar 中同名的资源在 classpath 上会互相遮蔽（Spring 的 `MessageSource` 只读取第一个），因此错误信息文件为 `messages/<id>.properties`，迁移目录为 `db/migration/<id>/`（系统设计 3.4 中的 `messages.properties` 据此细化）。`module.json` 由模块管理器以 `classpath*:module.json` 扫描全部 jar，`settings` 等相对路径相对于该 `module.json` 解析 |
| 配置属性 | 部署期配置使用前缀 `b2bweb.<id>.*`（`application.yml`）；站点级运行期设置放在 `sys_setting`，由 `settings.schema.json` 描述（第 5、6 章） |

### 3.5 前端工程（pnpm workspace）

```text
frontend/
├── package.json             # 根脚本：lint、typecheck、test、build（提交前：pnpm lint && pnpm typecheck && pnpm test）
├── pnpm-workspace.yaml      # apps/*、packages/*、packages/themes/*
├── pnpm-lock.yaml
├── tsconfig.base.json
├── apps/
│   ├── website/             # @b2bweb/website
│   └── admin/               # @b2bweb/admin
└── packages/
    ├── design-tokens/       # @b2bweb/design-tokens
    ├── component-schemas/   # @b2bweb/component-schemas
    ├── api-client/          # @b2bweb/api-client
    ├── shared/              # @b2bweb/shared
    ├── admin-kit/           # @b2bweb/admin-kit
    ├── website-kit/         # @b2bweb/website-kit
    └── themes/
        └── industrial/      # @b2bweb/theme-industrial
```

| 包 | 职责 | 可以依赖（workspace 内） |
|---|---|---|
| `@b2bweb/design-tokens` | DTCG 格式的 Token 源文件与 Style Dictionary 产物（CSS 变量、TS 常量）；唯一允许出现颜色、间距等字面量的位置（系统设计 11.1、11.4） | — |
| `@b2bweb/component-schemas` | 组件与模板定义、公共 `$defs`、迁移函数、校验工具；`application` 打包时复制其中的 JSON 到 classpath（系统设计 10.2、10.4、10.9） | `design-tokens`（样式选项枚举） |
| `@b2bweb/api-client` | 由 OpenAPI 生成的类型与客户端（admin、public 两组，系统设计 24.3）；只允许生成，不得手改 | — |
| `@b2bweb/shared` | 前后台通用：富文本（ProseMirror JSON）模型与渲染、ID 与语言工具、文案键类型（系统设计 29.1 中的 `shared/`） | `component-schemas`（类型） |
| `@b2bweb/admin-kit` | 后台模块开发套件：`defineAdminModule`、`registerContentTab`、SchemaForm、媒体 / 内容 / 分类选择器、富文本编辑器、LocaleTabs、RevisionHistory 等（系统设计 22.1–22.4） | `api-client`、`component-schemas`、`shared` |
| `@b2bweb/website-kit` | 前台渲染套件：`defineTheme`、布局 / Section / 模板渲染器、Delivery 组合式函数、文案键与 `Intl` 格式化（系统设计 21.1、21.7） | `api-client`、`component-schemas`、`design-tokens`、`shared` |
| `@b2bweb/theme-industrial` | V1 主题：Token 覆盖、布局、Header / Footer、模板、组件变体、主题默认文案（系统设计 11.2） | `website-kit`、`design-tokens`、`component-schemas`、`shared` |
| `@b2bweb/website` | Nuxt 4 应用：内容路由、预览与编辑器画布、服务端路由、插件、主题装配（系统设计 21.1） | 以上除 `admin-kit` 外的全部包 |
| `@b2bweb/admin` | Admin 应用：外壳（`src/core/`）、模块目录（`src/modules/`）、页面编辑器（`src/editor/`）（系统设计 22.1） | `api-client`、`admin-kit`、`component-schemas`、`shared`；`design-tokens` 只用于主题设置预览（系统设计 11.1） |

依赖方向规则（ESLint `no-restricted-imports` / dependency-cruiser 检查，系统设计 29.3）：

- 包不得依赖应用；两个应用之间不得互相 import；
- Admin 的 `src/modules/<a>/` 不得 import `src/modules/<b>/`，模块之间只通过 `admin-kit` 提供的注册机制协作（如内容编辑页标签）；
- 主题不得依赖 Admin 与 `admin-kit`；主题组件只使用 Token 与主题变体（P7）。

各包的内部结构与 API 见第 8 章。系统设计 22.1 中 Admin `src/shared/` 下的通用组件在本框架中放入 `@b2bweb/admin-kit`，便于独立测试与约束依赖方向。

### 3.6 模块的四个位置

一个模块是一个纵向切片，横跨四个位置，使用同一个模块 ID（系统设计 3.4、ADR-001）：

| 位置 | 路径 | 内容 | 与模块 ID 的关联 |
|---|---|---|---|
| ① 后端模块 | `backend/modules/<id>/` | Maven 工程 `module-<id>`；Java 包 `com.b2bweb.<id>`；`module.json`；迁移脚本 | `module.json` 的 `id` |
| ② Admin 模块 | `frontend/apps/admin/src/modules/<apiPrefix>/` | `index.ts`（`defineAdminModule({ id: '<id>', … })`）、`views/`、`components/`、`api/`、`stores/`、`texts.ts`（本模块的中文界面文案常量） | `defineAdminModule` 的 `id`；路由前缀 `/<apiPrefix>/`；菜单来自 `module.json` 的 `menus` |
| ③ 组件 Schema | `frontend/packages/component-schemas/components/<id>/`、`…/templates/<id>/` | 组件定义 JSON（其 `module` 字段等于模块 ID）及迁移函数；模板定义 JSON | 目录名与 `module` 字段一致；`module.json` 的 `components` 列出全部组件类型 |
| ④ 主题组件 | `frontend/packages/themes/industrial/components/<id>/<组件类型>/` | 该组件各变体的 Vue 实现；模板组件放在主题的 `templates/` 下 | 组件类型与 ③ 中的定义一一对应 |

- 只有 core 的 `apiPrefix` 与模块 ID 不同（`system` 与 `core`，见附录 A.3），因此 core 的 Admin 目录为 `modules/system/`（系统设计 22.1）；其他模块的 Admin 目录名即模块 ID。
- 不是每个模块都有四个位置：没有后台界面的模块没有 ②（如 delivery），没有前台组件的模块没有 ③ ④（如 notification）。模块设计文档第 7、8 节写明本模块涉及哪些位置。
- ③ 与 ④ 按模块 ID 分目录，是对系统设计 11.2 示例路径（`components/hero/HeroSplit.vue`）的细化，目的是让 CODEOWNERS 能按模块划分负责人（2.6）；例如 `hero` 属于 page 模块，路径为 `components/page/hero/HeroSplit.vue`。
- 模块停用时，四个位置由平台统一处理（系统设计 3.8）：②的菜单与路由按启用状态过滤，③的组件在编辑器面板中隐藏，④的已放置实例在前台不渲染。模块本身不需要写判断代码，但必须在 `module.json` 中如实声明 `apiPrefix`、`menus`、`components`（ADR-001）。

### 3.7 命名规范

**标识符**：

| 对象 | 规则 | 示例 |
|---|---|---|
| 模块 ID | 小写字母开头，只含小写字母与数字，2–32 个字符（同时用作 Java 包名、目录名、Flyway 位置） | `content`、`inquiry`、`core` |
| API 前缀（`apiPrefix`） | 等于模块 ID；唯一例外是 core 为 `system` | `inquiry`、`system` |
| 内容类型 | 大写下划线 | `PRODUCT`、`CASE_STUDY`、`TERM_PAGE` |
| 词汇表 key | 小写下划线 | `product_category`、`article_category` |
| 关系类型 | 大写下划线 | `RELATED`、`USED_IN` |
| 可翻译资源类型 | 大写下划线，全局唯一，清单见第 4 章 | `SITE_PROFILE`、`UI_TEXT` |
| 数据源 key | 小写连字符 | `content-query`、`term-children` |
| 组件类型、模板 key、变体 key | 小写连字符 | `inquiry-form`、`product-detail`、`hero-split` |
| SEO 规则 ID | `<模块 ID>.<点分小写>` | `seo.title.length` |

**Java**（模块内部的包结构见 3.4）：

| 对象 | 命名 | 示例 | 位置 |
|---|---|---|---|
| 包 | `com.b2bweb.<模块 ID>`；platform-core 为 `com.b2bweb.core`；platform-api 为 `com.b2bweb.api.<领域>` | `com.b2bweb.inquiry.application` | — |
| 模块描述类 | `<Id>Module`（模块 ID 首字母大写） | `InquiryModule`、`SeoModule` | 模块根包 |
| 服务接口 / 实现 | `<领域>Api` / `<接口名>Impl` | `ContentApi` / `ContentApiImpl` | `platform-api` / 提供方 `facade` |
| 扩展点 / 实现 | 沿用系统设计 3.7 的名称（`…Provider`、`…Contributor`、`…Collector`、`…Rule`）/ `<模块或实体><扩展点名>` | `SchemaOrgProvider` / `ProductSchemaOrgProvider` | `platform-api` / `extension` |
| 端口 / 适配器 | `<能力>`（名词）/ `<技术><端口名>` | `ObjectStorage` / `S3CompatibleStorage` | `platform-api` / `infrastructure` |
| 领域事件 | `<聚合><过去分词>Event`（系统设计 4.2） | `InquiryAssignedEvent` | `com.b2bweb.api.event` |
| 事件监听器 | `<用途>Listener`；方法名 `on<事件名去掉 Event>` | `FormSubmissionListener#onFormSubmitted` | `listener` |
| 应用服务 | `<聚合>Service`（命令）、`<聚合>QueryService`（查询） | `InquiryService` | `application` |
| 领域对象 | 不加后缀 | `Inquiry`、`AssignmentRule` | `domain` |
| 仓储 | `<聚合>Repository`（接口）/ `MyBatis<聚合>Repository`（实现） | `InquiryRepository` | `domain` / `infrastructure.persistence` |
| 持久化对象 / Mapper | `<表名去前缀的 Pascal 形式>PO` / `…Mapper` | `InquiryPO`、`InquiryMapper`（表 `inq_inquiry`） | `infrastructure.persistence` |
| 控制器 | `<资源>AdminController` / `<资源>PublicController` / `<资源>InternalController` | `InquiryAdminController` | `web.*` |
| Web DTO | `<动作><资源>Request`、`<资源>Response`、列表项 `<资源>ListItem` | `AssignInquiryRequest` | `web.*` |
| 定时任务 / 任务处理器 | `<用途>Job` / `<用途>JobHandler` | `SlaReminderJob`、`ProductImportJobHandler` | `job` |
| 配置属性类 | `<Id>Properties`，前缀 `b2bweb.<id>` | `InquiryProperties` | `config` |
| 测试 | 单元 `<被测类>Test`；集成 `<场景>IT`；契约 `<接口名>ContractTest`（继承 testkit 中的契约套件） | `ContentApiContractTest` | `src/test` |
| 测试替身 | 服务接口 `InMemory<接口名>` 或 `Fake<接口名>`；端口 `InMemory<端口名>` 或 `Fake<端口名>` | `InMemoryContentApi`、`FakeTranslationEngine` | `platform-testkit` |

**REST 与错误**（系统设计 24.1、24.2）：

| 对象 | 规则 | 示例 |
|---|---|---|
| 路径 | `/api/{admin\|public\|internal}/v1/{apiPrefix}/{资源}`；资源用复数名词、kebab-case | `/api/admin/v1/inquiry/assignment-rules` |
| 子资源与动作 | 子资源 `/{资源}/{id}/{子资源}`；状态变化用 `POST /{资源}/{id}/{动词}` | `POST /api/admin/v1/content/localizations/{id}/publish` |
| JSON 字段与查询参数 | camelCase；ID 为字符串（ADR-014）；时间为 ISO-8601 UTC | `ownerUserId`、`"id": "7201893453912345"` |
| 错误码 | `{MODULE}_{REASON}`，大写下划线；`MODULE` 为 `apiPrefix` 的大写形式 | `CONTENT_SLUG_CONFLICT`、`INQUIRY_NOT_FOUND` |
| 错误信息 | 后台的 `title`、`detail`、`errors[].message` 用中文，放在 `messages/<id>.properties`，键为错误码；前台公开接口的错误由前端按 `code` 映射为文案键 | `INQUIRY_NOT_FOUND=询盘不存在或无权访问` |

**权限与菜单**（系统设计 3.5、26.2）：

| 对象 | 规则 | 示例 |
|---|---|---|
| 权限码 | `{apiPrefix}:{资源}:{动作}`，三段均为小写，多词用连字符；动作优先从 `view`、`create`、`edit`、`delete`、`publish`、`review`、`assign`、`import`、`export`、`manage` 中选 | `inquiry:inquiry:export`、`system:dsr:manage` |
| 内容生命周期权限 | `content:{内容类型的小写连字符形式}:{edit\|review\|publish\|delete}`（系统设计 6.3），由内容类型的所属模块在其 `module.json` 中声明（附录 A.3） | `content:product:publish`、`content:case-study:edit` |
| 菜单 key | 小写连字符，全局唯一；模块的主入口用模块 ID，其他以模块 ID 开头 | `inquiry`、`inquiry-rules` |
| Admin 路由 | `/{apiPrefix}/{页面}`，页面名小写连字符；路由名 `{apiPrefix}.{页面}` | `/inquiry/inquiries`、`inquiry.inquiries` |

**事件、任务与消息**：

| 对象 | 规则 | 示例 |
|---|---|---|
| 领域事件 | 见上表；`module.json` 的 `events` 中写简单类名 | `ContentPublishedEvent` |
| 通知模板 key | `{模块 ID}.{场景}.{接收方}`（系统设计 18） | `inquiry.assigned.staff`、`inquiry.autoreply.customer` |
| 定时任务锁名（ShedLock） | `{模块 ID}.{任务名 camelCase}` | `inquiry.slaReminder` |
| 通用任务类型（`sys_job`） | `{模块 ID}.{任务名 camelCase}` | `product.import`、`url.redirectImport` |
| 设置键（`sys_setting`） | `{模块 ID}.{名称 camelCase}` | `core.smtp`、`inquiry.retention` |
| 指标名（Micrometer） | `b2bweb.{模块 ID}.{点分小写}` | `b2bweb.translation.memory.hit` |

**数据与存储**（详见第 7 章）：

| 对象 | 规则 | 示例 |
|---|---|---|
| 表前缀 | 按系统设计 3.2：core `sys_`、content `cnt_`、url `url_`、media `mda_`、taxonomy `tx_`、form `frm_`、notification `ntf_`、translation `trn_`、product `prd_`、article `art_`、page `pg_`、inquiry `inq_`、navigation `nav_`、seo `seo_`、graph `grh_`、tracking `trk_`；delivery无表；search按ADR-021提案增加 `sch_` 控制表 | `inq_inquiry` |
| 表、字段、索引、外键 | 第 7 章 | `uk_route_path` |
| Flyway | 位置 `db/migration/{模块 ID}`；文件 `V{三位序号}__{描述}.sql`；history 表 `flyway_{模块 ID}_history`（系统设计 25.6） | `V001__init.sql`、`flyway_core_history` |
| Redis key | 站点级 `s{siteId}:{模块 ID}:{用途}:…`；系统级 `sys:{模块 ID}:…`（系统设计 5.3） | `s1:delivery:resolve:…` |
| Cache Tag | 系统设计 23.2，由 delivery 统一生成 | `s1:c:7201893453912345` |
| Meilisearch 索引 | `s{site}_{locale}_content`（系统设计 19） | `s1_de_content` |
| 对象存储键 | `{siteId}/{yyyy}/{mm}/{id}-{slug 化的原文件名}.{ext}`（系统设计 12.2） | — |

**前端与文案**：

| 对象 | 规则 | 示例 |
|---|---|---|
| 前端包 | `@b2bweb/<名称>`，小写连字符 | `@b2bweb/admin-kit` |
| Vue 组件文件 | PascalCase；主题变体为 `<组件类型的 Pascal 形式><变体>.vue` | `HeroSplit.vue` |
| 组合式函数 | `use<名称>` | `useDelivery`、`useConsent` |
| 前台文案键 | `{命名空间}.{键}`，各段 lowerCamelCase；命名空间为组件类型的 camelCase 形式或通用域（`common`、`form`、`consent`）；由主题提供默认中文文案，站点可以覆盖（系统设计 5.4、21.7） | `form.submit`、`inquiryCart.empty`、`common.readMore` |
| 前台错误文案键 | `error.{错误码}`，错误码原样 | `error.FORM_RATE_LIMITED` |
| 后台界面文案 | 只用简体中文，集中在各模块的 `texts.ts` 与 `admin-kit` 的常量文件中；不引入 vue-i18n（`CLAUDE.md` 语言规则 1） | `texts.inquiry.assign = '分配'` |

**Git**（`CLAUDE.md` 提交规范）：分支 `feat/<模块 ID>-<主题>`、`fix/<模块 ID>-<主题>`、`docs/<模块 ID>-design`；提交信息 `<type>(<模块 ID>): <描述>`。

### 3.8 新模块脚手架

M0 的验收要求“按模板新建一个模块并接入 ≤ 1 小时”（系统设计 30）。框架组在 `tools/scaffold/` 中提供脚手架：

```bash
node tools/scaffold/new-module.mjs --id inquiry --name 询盘 --layer L3 --table-prefix inq_ --disableable
```

脚手架生成并登记以下内容，生成结果直接满足 `./mvnw verify` 与前端检查：

| # | 生成或修改 | 内容 |
|---|---|---|
| 1 | `backend/modules/<id>/` | `pom.xml`、`package-info.java`、`<Id>Module.java`、通过 Schema 校验的 `module.json`、空的 `V001__init.sql`（只含注释）、`messages/<id>.properties`、`ArchitectureTest`、空的模块集成测试 |
| 2 | 构建登记 | `backend/modules/pom.xml` 的 `<modules>`；`application` 的依赖 |
| 3 | `frontend/apps/admin/src/modules/<id>/` | `index.ts`（`defineAdminModule`）、`texts.ts`、空的 `views/` |
| 4 | `component-schemas`、主题 | `components/<id>/`、`templates/<id>/`、主题 `components/<id>/` 空目录 |
| 5 | `docs/modules/<id>.md` | 第 12 章模板，已填入基本信息表 |
| 6 | `.github/CODEOWNERS` | 本模块文档与四个位置的负责人占位行（2.6） |

- 脚手架只生成骨架，不生成业务代码；新增 V1 范围以外的模块（如 V2 的 crm）之前必须先写 ADR（系统设计 29.5）。
- 为避免并行开发时多人同时修改共享文件（聚合 POM、`application` 的依赖、CODEOWNERS、Admin 的模块登记），M0 末尾用脚手架一次性生成全部 17 个业务模块的骨架并合入主干（13.1）。此后各模块只在自己的目录中工作。

---

## 4. platform-api 契约

本章定义 `platform-api`（L1）中的全部跨模块契约：服务接口、扩展点、领域事件、基础设施端口、平台 SPI、共享类型与异常。它是模块并行开发的唯一依据（ADR-002、ADR-018）：

- 模块设计文档（`docs/modules/<模块ID>.md`）只引用本章的契约，不得自行定义跨模块接口、事件或 DTO；
- 本章与系统设计说明书的示意代码（3.6、3.7、4.2、5.9、6.2、6.4、10.5、12.5、14.9、15.5）不一致时，以本章为准；本章对示意代码所做的细化与补充在对应小节注明“框架补充”；
- 本章中的代码就是 M0 中 `platform-api` 的源代码草稿：类型名、方法名、参数与返回类型按本章编码，Javadoc 可以补充但不得改变语义。

### 4.1 契约原则

#### 4.1.1 platform-api 中放什么

| 允许 | 说明 |
|---|---|
| 接口 | 服务接口（由某个模块实现，供其他模块调用）、扩展点（由多个模块实现，由某个模块收集调用）、基础设施端口（由 infrastructure 实现）、平台 SPI（`PlatformModule`、`JobHandler`、`ScheduledTask`） |
| record | 命令、查询、结果、上下文、事件；不可变，集合组件在紧凑构造器中用 `List.copyOf` / `Set.copyOf` / `Map.copyOf` 复制 |
| 枚举 | 跨模块共享的取值（与《领域模型与数据库设计》第 2 章的共享枚举一致） |
| 常量与纯函数 | `final` 类 + 私有构造器，只含常量与无副作用的静态方法（如 `Locales`、`ContentTypes`、`CacheTag` 的工厂方法）；不得访问数据库、Redis、网络、文件或 Spring 容器 |
| 异常与错误码 | `PlatformException` 体系与各领域的 `XxxErrors` 错误码常量（见 4.1.7） |
| 注解 | `@Extension`、`@RequiresPermission` |
| `package-info.java` | 每个包标注 `@NullMarked`（见 4.1.4） |

| 不允许 | 原因 |
|---|---|
| 任何实现：Spring Bean、Mapper、持久化对象、Controller、监听器、切面 | 契约与实现分离；实现放在提供方模块或 platform-core |
| 可变状态 | 例外：`ValidationResult`、`DeliveryOutput` 等作为扩展点“收集器”参数的容器类，不持有外部资源 |
| 只属于某个模块内部的模型（MyBatis 实体、领域实体、内部枚举） | 跨模块只交换 record DTO，内部模型可以自由重构 |
| 后台界面或前台展示用的文案常量 | 后台文案在 Admin 字典文件中；前台文案走文案键（系统设计 21.7） |

> 本章的代码块中，一个代码块可能包含同一包下的多个顶层类型；落地时每个公开类型一个文件。代码块中省略 `import` 语句。

#### 4.1.2 依赖与依赖方向

`platform-api` 的编译依赖只允许下表所列（版本由 ADR-011 锁定的 BOM 管理），由 ArchUnit 规则检查：`com.b2bweb.api..` 中的类只能依赖 `java..`、`jakarta.validation..`、`org.jspecify..`、`org.springframework.stereotype..`、`tools.jackson.databind..`、`com.fasterxml.jackson.annotation..` 与 `com.b2bweb.api..`。

| 依赖 | 用途 |
|---|---|
| JDK（语言级别 21，运行 JDK 25） | record、sealed 接口、模式匹配 |
| `spring-context`（只使用 `org.springframework.stereotype.Component`） | `@Extension` 以 `@Component` 为元注解，扩展点实现自动注册为 Bean |
| `jakarta.validation-api` | 命令 record 的约束注解（`@NotBlank`、`@Size`、`@Positive` 等） |
| `org.jspecify:jspecify` | `@NullMarked`、`@Nullable`（与 Spring Framework 7 一致） |
| Jackson 3 `jackson-databind` 与 `jackson-annotations` | `JsonNode`（快照部分、布局文档、数据源参数、JSON Schema 等结构由提供方定义的 JSON），以及 ID 字符串序列化注解 |

模块之间的 Maven 依赖方向：

```text
platform-api（无内部依赖）
   ▲          ▲                ▲
   │          │                │
platform-core  infrastructure   platform-testkit（测试替身与契约测试套件，见 4.8、第 9 章）
   ▲   ▲          ▲                ▲（test scope）
   │   └──────────┤                │
   │         application ──装配──▶ 全部模块
   │
modules/*（compile：platform-api、platform-core；test：platform-testkit）
```

- 系统设计 3.3 图中“platform-api ──▶ platform-core”表示模块只能依赖这两层；Maven 层面 platform-core **实现** platform-api 中的平台服务接口（`SiteApi`、`SettingsApi` 等），因此依赖方向是 platform-core → platform-api。
- 模块对 platform-core 的编译依赖只用于技术基类与自动配置（持久化基类、Web 异常处理、请求上下文实现等，见第 5 章）。**跨模块的业务能力，包括 platform-core 提供的站点、权限、设置、任务等，一律通过本章的接口调用**，不得调用 platform-core 中的实现类。
- infrastructure 只依赖 platform-api（以及 platform-core 的配置读取能力），实现 4.7 的端口；业务模块不依赖 infrastructure。

#### 4.1.3 调用规则

**层间调用**（系统设计 3.3 R2 的落地）：

| 调用方 | 可以同步调用的服务接口 | 说明 |
|---|---|---|
| platform-core（L0） | 无（只调用端口与扩展点） | Core 需要上层能力时只能收集扩展点（如 `PersonalDataProvider`、`SchemaOrgProvider` 上下文中的 `RefResolver`） |
| L2 模块 | Core 的平台服务；下表列出的 L2 → L2 方向 | L2 不得调用 L3 / L4，只能通过扩展点回调 |
| L3 模块 | Core 的平台服务、全部 L2 服务接口 | 同为 L3 的模块之间不直接调用 |
| L4 模块 | Core 的平台服务、全部 L2 服务接口 | 不得调用 L3；需要 L3 的能力时使用扩展点（如 page 的模板经 `TemplateProvider`） |

**L2 之间允许的同步调用方向**（有向无环；新增方向需要 ADR，并在 `module.json` 的 `requires` 中声明）：

| 调用方 → 提供方 | 用途 |
|---|---|
| content → url | 发布事务中预留路径、应用路由、下线时退役路由、生成 slug |
| taxonomy → content | 为可路由分类项创建与维护 `TERM_PAGE` 内容 |
| translation → content | 创建目标语言本地化、读写翻译状态、读取源语言发布状态 |
| form → media | 表单附件上传为私有媒体；留资文档的下载票据校验与签名链接 |
| form → notification（ADR-023，Proposed） | 原DOWNLOAD提交事务调用NotificationApi.send入发件箱；完整方案4.10.6 |

由此禁止的方向（会形成环）：url → content、url → taxonomy、content → taxonomy、content → translation、media → 任何 L2。

**可选依赖与模块缺失**：

- `module.json` 的 `optional` 中声明的模块，其服务接口通过 `ObjectProvider<T>` 注入（`getIfAvailable()`），模块不在构建中（`MISSING`）时为空，调用方降级。
- 可停用模块（form、translation、product、article、inquiry、search、tracking）的服务接口：当前站点停用该模块时，写操作与面向前台的操作抛出 `ModuleDisabledException`（错误码 `CORE_MODULE_DISABLED`，HTTP 404）；只读查询照常返回已有数据（停用不删除数据，系统设计 3.8）。调用方应先用 `SiteApi.isModuleEnabled(moduleId)` 判断，以便给出友好的降级。
- 扩展点由 `ExtensionRegistry` 按当前站点的启用状态过滤（4.4.15），调用方无需判断。

**站点上下文**：

- L2 及以上模块的服务接口**隐式使用当前站点上下文**（`SiteContextProvider`，4.4.2），方法参数中不传 `siteId`；没有站点上下文时抛出 `NoSiteContextException`（`CORE_NO_SITE_CONTEXT`，HTTP 500，属于编程错误）。请求、事件监听、异步任务、定时任务中的站点上下文由 platform-core 设置（系统设计 5.2）。
- 跨站点操作只出现在系统管理接口中，契约方法名以 `System` 或 `forSite` 标明并显式接收 `siteId`（如 `ExtensionRegistry.allForSite`、`PersonalDataProvider`）。
- 扩展点的上下文 record 中带 `siteId` 只用于日志与缓存键，实现方仍依赖站点上下文访问自己的表。

**事务语义**：

- 服务接口的写方法以 `Propagation.REQUIRED` 加入调用方事务（同一进程、同一数据源），例如业务模块在自己的事务中调用 `ContentApi.create`（系统设计 6.1）；调用方负责开启事务。
- 例外在 Javadoc 中注明，如 `AuditApi.recordIndependently`（独立事务）、`TranslationApi.translateText`（不得在持有数据库事务时调用）。
- 写方法中发布的领域事件随调用方事务提交后投递（ADR-003）。

**回调约束**（扩展点实现方必须遵守）：

- 在调用方的事务中被调用的扩展点（如 `PublishContributor`）不得调用调用方模块的写方法（禁止重入），不得执行远程调用（AI、邮件、CDN、搜索）或耗时超过 100 ms 的操作。
- 扩展点实现必须线程安全、无会话状态；同一输入多次调用结果一致（便于重试与预览）。

**权限**：

- 服务接口默认**不做后台权限校验**：调用方在自己的应用服务方法上用 `@RequiresPermission` 校验（系统设计 26.2）。
- 例外：`ContentApi` 的生命周期写操作在存在当前用户时，按内容类型校验 `content:<type>:<action>`（内容生命周期权限归内容内核，系统设计 26.2）；以 `system` 身份执行（定时任务、译文自动发布）时跳过。
- 数据范围（`DataScope`）由拥有数据的模块在查询层过滤；platform-core 只提供当前用户的数据范围（`PermissionChecker.dataScope`）。

**批量与性能**：

- 按 ID 批量查询的方法接收 `Collection<Long>`，单次最多 500 个 ID，返回 `Map<Long, T>`（不存在的 ID 不出现在结果中）；调用方不得在循环中逐个调用单条查询（避免 N+1）。
- Delivery 路径上的读方法（标注“Delivery 热路径”）P95 ≤ 20 ms（缓存未命中），实现方负责缓存与失效。

提供方模块启用检查注解（M0 RC补齐，ADR-026）：

```java
package com.b2bweb.api.common;

/** 提供方的写方法与公开读取入口必须检查当前站点的模块启用状态；系统级读取不标注。 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Inherited
@java.lang.annotation.Documented
public @interface RequiresModuleEnabled {
    String value();
}
```

#### 4.1.4 空值约定

- 每个包的 `package-info.java` 标注 `@org.jspecify.annotations.NullMarked`：未标注的参数、返回值、record 组件一律非空。
- 可空的参数与 record 组件标注 `@Nullable`；查询“可能不存在的单个对象”的方法返回 `Optional<T>`，不返回 `null`。
- `Optional` 只用作返回类型，不用作参数、字段或 record 组件。
- 集合、`Map` 永远不为 `null`，没有数据时返回空集合；record 的集合组件在紧凑构造器中复制为不可变集合。
- 基本类型 `long` / `int` / `boolean` 表示必填；可空的数值使用包装类型并标注 `@Nullable`。

#### 4.1.5 ID、时间与语言

| 项 | 约定 |
|---|---|
| ID | 所有实体 ID 在 Java 中为 `long`（可空时为 `@Nullable Long`），即 64 位 TSID（ADR-014）；不定义 `SiteId`、`ContentId` 等包装类型（与 MyBatis 映射、事件 JSON、系统设计中的签名保持一致）。参数名带实体前缀以避免混淆：`contentId`、`localizationId`、`mediaId`、`termId` |
| ID 的 JSON 序列化 | 一律为字符串：platform-core 注册全局 Jackson 模块，把 `long` / `Long` 序列化为字符串并接受字符串反序列化；快照、布局文档、事件载荷、API 响应同样适用 |
| ID 生成 | 只通过 `IdGenerator.nextId()`（4.3.1），不使用数据库自增，不各自实现 |
| 事件 ID | `DomainEvent.eventId()` 为 TSID 的十进制字符串（系统设计 4.2） |
| 时间 | `java.time.Instant`（UTC）；需要“当前时间”时注入 `java.time.Clock`（platform-core 提供 UTC 时钟 Bean，testkit 提供可拨动的时钟），不直接调用 `Instant.now()` |
| 时长 | `java.time.Duration` |
| 语言 | `String`，BCP 47 语言代码（如 `zh-CN`、`en`、`de`、`pt-BR`），参数名为 `locale`、`sourceLocale`、`targetLocale`；源语言常量 `Locales.SOURCE = "zh-CN"`（4.3.2）；不使用 `java.util.Locale` 作为契约类型 |
| 用户 | `long userId`；系统操作的用户 ID 为 0，事件中的 `actor` 为 `"system"`（4.3.4） |
| 内容类型、模块 ID、权限码、设置键 | `String`；内置值见 `ContentTypes`（4.3.3），模块 ID 与 `module.json` 的 `id` 一致（platform-core 的模块 ID 为 `core`） |

#### 4.1.6 分页

| 类型 | 用途 |
|---|---|
| `PageRequest` / `PageResult<T>` | 后台列表等需要总数的场景：页码从 1 开始，`size` ≤ 100，与 REST 响应 `{ items, page, size, total }` 一一对应（系统设计 24.2） |
| `CursorRequest` / `CursorPage<T>` | 全量扫描、导出与同步（sitemap 生成、搜索重建、图谱全量计算、批量翻译）：不透明游标，`size` ≤ 1000，不计算总数 |
| `List<T>` + `limit` | 结果数量有硬上限的查询（如数据源最多 24 条，系统设计 10.5） |

类型定义见 4.3.6。排序字段由提供方以白名单声明（在 Javadoc 中列出），不接受任意字段名（防止 `${}` 注入，系统设计 26.3）。

#### 4.1.7 异常体系与错误码

跨模块抛出的异常都是 `PlatformException` 的子类（非受检）。platform-core 的全局异常处理器把它们转换为 RFC 9457 响应（系统设计 24.2）：`status` 取自错误码，`code` 即错误码，`title` 为错误码的中文标题，`detail` 为异常的中文说明，`errors[]` 来自字段错误。

- 错误码格式 `{MODULE}_{REASON}`：`MODULE` 为大写模块 ID（platform-core 为 `CORE`），`REASON` 为大写下划线短语，如 `CONTENT_SLUG_CONFLICT`。
- 跨模块可能抛出的错误码定义在对应 api 包的 `XxxErrors` 常量类中（4.4 各接口给出）；模块内部才用到的错误码定义在模块自己的包中。
- `title`、`detail`、`errors[].message` 只用中文（系统设计 24.2、CLAUDE.md 语言规则 2）；前台公开接口由前端按 `code` 映射文案键，不展示 `detail`。

```java
package com.b2bweb.api.common;

/** 错误码：{MODULE}_{REASON}，带默认 HTTP 状态与中文标题。 */
public record ErrorCode(String code, int status, String title) {
    private static final java.util.regex.Pattern FORMAT =
            java.util.regex.Pattern.compile("^[A-Z][A-Z0-9]*_[A-Z0-9_]+$");

    public ErrorCode {
        if (!FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("错误码格式必须为 {MODULE}_{REASON}: " + code);
        }
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("错误码的 HTTP 状态必须为 4xx 或 5xx: " + status);
        }
    }

    public static ErrorCode of(String code, int status, String title) {
        return new ErrorCode(code, status, title);
    }
}

/** 字段级错误，对应 RFC 9457 响应的 errors[]。 */
public record FieldError(String field, String code, String message) {}

/** 平台异常基类。detail 为中文说明；properties 进入 problem+json 的扩展字段（如 conflictContentId）。 */
public abstract class PlatformException extends RuntimeException {
    private final ErrorCode errorCode;
    private final java.util.List<FieldError> errors;
    private final java.util.Map<String, Object> properties;

    protected PlatformException(ErrorCode errorCode, String detail,
                                java.util.List<FieldError> errors,
                                java.util.Map<String, Object> properties,
                                @org.jspecify.annotations.Nullable Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.errors = java.util.List.copyOf(errors);
        this.properties = java.util.Map.copyOf(properties);
    }

    public ErrorCode errorCode() { return errorCode; }
    public String detail() { return getMessage(); }
    public java.util.List<FieldError> errors() { return errors; }
    public java.util.Map<String, Object> properties() { return properties; }
}

/** 资源不存在（404）。 */
public class NotFoundException extends PlatformException {
    public NotFoundException(ErrorCode code, String detail) {
        super(code, detail, java.util.List.of(), java.util.Map.of(), null);
    }
}

/** 违反业务规则（默认 409 或 422，由错误码决定）。 */
public class BusinessException extends PlatformException {
    public BusinessException(ErrorCode code, String detail) {
        this(code, detail, java.util.Map.of());
    }
    public BusinessException(ErrorCode code, String detail, java.util.Map<String, Object> properties) {
        super(code, detail, java.util.List.of(), properties, null);
    }
}

/** 乐观锁冲突（409）；properties 中带 currentVersion。 */
public class VersionConflictException extends BusinessException {
    public VersionConflictException(ErrorCode code, String detail, int currentVersion) {
        super(code, detail, java.util.Map.of("currentVersion", currentVersion));
    }
}

/** 输入校验失败（400），带字段错误列表。 */
public class ValidationException extends PlatformException {
    public ValidationException(ErrorCode code, String detail, java.util.List<FieldError> errors) {
        super(code, detail, errors, java.util.Map.of(), null);
    }
}

/** 无权限（403）。 */
public class ForbiddenException extends PlatformException {
    public ForbiddenException(ErrorCode code, String detail) {
        super(code, detail, java.util.List.of(), java.util.Map.of(), null);
    }
}

/** 当前站点停用了提供该能力的模块（404，与 ModuleGuardFilter 一致）。 */
public class ModuleDisabledException extends PlatformException {
    public ModuleDisabledException(String moduleId) {
        super(CoreErrors.MODULE_DISABLED, "模块 " + moduleId + " 在当前站点已停用",
              java.util.List.of(), java.util.Map.of("moduleId", moduleId), null);
    }
}

/** 没有站点上下文（编程错误，500）。 */
public class NoSiteContextException extends PlatformException {
    public NoSiteContextException() {
        super(CoreErrors.NO_SITE_CONTEXT, "当前线程没有站点上下文", java.util.List.of(), java.util.Map.of(), null);
    }
}

/** 外部服务不可用（502 / 503），retryable 表示调用方可以稍后重试。 */
public class ExternalServiceException extends PlatformException {
    private final boolean retryable;
    public ExternalServiceException(ErrorCode code, String detail, boolean retryable,
                                    @org.jspecify.annotations.Nullable Throwable cause) {
        super(code, detail, java.util.List.of(), java.util.Map.of(), cause);
        this.retryable = retryable;
    }
    public boolean retryable() { return retryable; }
}
```

platform-core 的通用错误码（`com.b2bweb.api.common.CoreErrors`）：

```java
package com.b2bweb.api.common;

public final class CoreErrors {
    private CoreErrors() {}
    public static final ErrorCode VALIDATION_FAILED     = ErrorCode.of("CORE_VALIDATION_FAILED", 400, "输入校验失败");
    public static final ErrorCode NO_SITE_CONTEXT       = ErrorCode.of("CORE_NO_SITE_CONTEXT", 500, "缺少站点上下文");
    public static final ErrorCode SITE_NOT_FOUND        = ErrorCode.of("CORE_SITE_NOT_FOUND", 404, "站点不存在");
    public static final ErrorCode LOCALE_NOT_ENABLED    = ErrorCode.of("CORE_LOCALE_NOT_ENABLED", 422, "站点未启用该语言");
    public static final ErrorCode MODULE_DISABLED       = ErrorCode.of("CORE_MODULE_DISABLED", 404, "模块已停用");
    public static final ErrorCode FORBIDDEN             = ErrorCode.of("CORE_FORBIDDEN", 403, "没有权限");
    public static final ErrorCode NOT_AUTHENTICATED     = ErrorCode.of("CORE_NOT_AUTHENTICATED", 401, "未登录");
    public static final ErrorCode SETTING_NOT_DECLARED  = ErrorCode.of("CORE_SETTING_NOT_DECLARED", 500, "设置键未声明");
    public static final ErrorCode SETTING_VERSION_CONFLICT = ErrorCode.of("CORE_SETTING_VERSION_CONFLICT", 409, "设置已被他人修改");
    public static final ErrorCode SETTING_INVALID       = ErrorCode.of("CORE_SETTING_INVALID", 422, "设置值不符合 Schema");
    public static final ErrorCode JOB_NOT_FOUND         = ErrorCode.of("CORE_JOB_NOT_FOUND", 404, "任务不存在");
    public static final ErrorCode JOB_TYPE_UNKNOWN      = ErrorCode.of("CORE_JOB_TYPE_UNKNOWN", 500, "未注册的任务类型");
    public static final ErrorCode JOB_STATE_INVALID     = ErrorCode.of("CORE_JOB_STATE_INVALID", 409, "任务状态不允许该操作");
    public static final ErrorCode DICT_TYPE_NOT_FOUND   = ErrorCode.of("CORE_DICT_TYPE_NOT_FOUND", 404, "字典类型不存在");
    public static final ErrorCode SECRET_DECRYPT_FAILED = ErrorCode.of("CORE_SECRET_DECRYPT_FAILED", 500, "密文无法解密");
    public static final ErrorCode EXTENSION_KEY_UNKNOWN = ErrorCode.of("CORE_EXTENSION_KEY_UNKNOWN", 404, "扩展不存在");
}
```

#### 4.1.8 兼容规则

契约只增不改。`platform-api` 的版本即平台版本（`module.json` 的 `platform` 范围，如 `">=1.0.0 <2.0.0"`）：

| 变更 | 是否兼容 | 规则 |
|---|---|---|
| 新增服务接口、扩展点、事件、端口、枚举常量类 | 兼容（次版本） | 同一个 PR 中提供 testkit 替身与契约测试（4.8） |
| 服务接口新增方法 | 兼容（次版本） | 提供方实现与 testkit 替身在同一个 PR 中更新；调用方不受影响 |
| 扩展点新增方法 | 兼容（次版本） | **必须是 `default` 方法**，默认行为等价于“不参与” |
| 端口新增方法 | 兼容（次版本） | 必须是 `default` 方法，或在同一个 PR 中更新全部 infrastructure 适配器与 testkit 替身 |
| record 新增组件 | 兼容（次版本） | 追加在末尾；保留旧的组件列表作为附加构造器（委托新构造器并给出默认值）；模块代码不得对 platform-api 的 record 使用解构模式（record pattern） |
| 枚举新增取值 | 兼容（次版本） | 调用方的 `switch` 必须有 `default` 分支（未知取值按“忽略”或“不支持”处理）；数据库中的取值同步写入《领域模型与数据库设计》/ 模块文档 |
| 事件新增字段 | 兼容（次版本） | 同 record；`schemaVersion` 不变 |
| 新增错误码 | 兼容（次版本） | — |
| 删除或改名方法、组件、取值；改变参数或返回类型；改变语义（包括空值约定、事务语义、异常） | **不兼容（主版本）** | 必须写 ADR；先以 `@Deprecated(since = "x.y", forRemoval = true)` 标记并提供替代，至少保留一个次版本；事件的不兼容变更提升 `schemaVersion`，过渡期内同时发布新旧两个版本或监听方同时支持两个版本（系统设计 4.2） |

变更流程见 4.9。

#### 4.1.9 提供方与使用方

每个契约在 4.2 的总表与各小节标题下注明：

- **提供方**：实现该接口的模块（服务接口、端口）或收集调用的模块（扩展点）；
- **实现方**（扩展点）：系统设计 3.7 中的 V1 实现模块；
- **使用方**：调用该接口、订阅该事件的模块。

提供方对契约的行为负责，并保证通过 4.8 的契约测试；使用方只依赖契约中写明的行为，不依赖提供方的实现细节（如缓存时长、内部排序）。

### 4.2 包结构与契约总表

> **补充契约草案登记**：ReferenceWriteApi、ProjectionChangeApi、TransactionMutexApi、MediaReferenceSink、SearchChangeCapture、SiteResourceChangedEvent 及端口/ContentApi 增量统一见 4.10.9。当前为 Proposed；原目录中冲突的 media 快照读取、异步引用写入、form 通知方向及搜索 swap 描述按 4.10 记录替代方案，正式冻结前必须合并到完整目录和代码。

#### 4.2.1 包结构

```text
backend/platform/platform-api/src/main/java/com/b2bweb/api/
├── common/        错误码与异常、ID 生成、语言与内容类型常量、操作人、分页、校验结果、影响报告、引用、缓存标签、
│                  通用任务（JobApi、JobHandler、ScheduledTask）、字典（DictApi）
├── site/          站点、站点上下文、设置、站点资料、前台界面文案
├── security/      当前用户、权限与数据范围、用户目录、审计、密文加解密
├── content/       内容内核服务（ContentApi）、内容类型与发布相关扩展点、内容状态枚举
├── url/           URL 引擎服务（UrlApi）、路由解析结果
├── media/         媒体服务（MediaApi）、媒体引用收集扩展点
├── taxonomy/      分类服务（TaxonomyApi）
├── form/          表单服务（FormApi）
├── notification/  通知服务（NotificationApi）、通知模板扩展点
├── translation/   翻译服务（TranslationApi）、翻译单元与上下文、可翻译内容 / 资源扩展点、翻译枚举
├── seo/           SEO 相关扩展点（SchemaOrgProvider、SeoVariableProvider、SeoRule、SitemapProvider）
├── delivery/      前台交付相关扩展点（模板、数据源、交付贡献、站点引导、面包屑）与引用解析器
├── extension/     @Extension、ExtensionRegistry、平台 SPI（PlatformModule）、无专属领域包的扩展点
│                  （SearchDocumentProvider、RelationTypeProvider、PersonalDataProvider）
├── event/         DomainEvent、事件元数据、全部领域事件
└── port/          基础设施端口（由 infrastructure 实现）
```

- 包按“契约所属的领域”划分，而不是按提供方模块划分：例如 `TranslationMode`、`TranslationState` 同时用于内容与站点资源，放在 `translation` 包；`BreadcrumbProvider` 由 delivery 与 seo 共同调用，放在 `delivery` 包。
- L3、L4 模块（product、article、page、inquiry、navigation、delivery、seo、graph、search、tracking）不对外提供服务接口（系统设计 3.3 R2：同层不互调，下层不调上层），因此没有自己的服务包；它们只实现扩展点、发布与订阅事件。

#### 4.2.2 服务接口

| 契约 | 包 | 提供方 | 主要使用方 | 小节 | 系统设计 |
|---|---|---|---|---|---|
| `SiteApi` | `site` | core | 全部模块 | 4.4.1 | 5.1–5.4、3.6 |
| `SiteContextProvider` | `site` | core | 全部模块 | 4.4.2 | 5.2 |
| `SettingsApi` | `site` | core | 全部模块 | 4.4.3 | 3.5、11.3、25.8 |
| `SiteProfileApi` | `site` | core | notification、seo | 4.4.4 | 16.3 |
| `UiTextApi` | `site` | core | delivery、seo、notification | 4.4.5 | 5.4、21.7 |
| `CurrentUserProvider` | `security` | core | 全部模块 | 4.4.6 | 26.1、26.2 |
| `PermissionChecker`、`DataScope`、`@RequiresPermission` | `security` | core | 全部模块 | 4.4.7 | 26.2 |
| `UserDirectory` | `security` | core | content、inquiry、notification、translation | 4.4.8 | 1.2、17.7、18 |
| `AuditApi` | `security` | core | 全部模块 | 4.4.9 | 26.5 |
| `SecretCipher` | `security` | core | translation、notification、form、tracking、infrastructure | 4.4.10 | 26.6 |
| `IdGenerator` | `common` | core | 全部模块 | 4.4.11 | 25.3、ADR-014 |
| `JobApi`（含 `JobHandler`、`ScheduledTask`） | `common` | core | product、url、inquiry、graph、seo、search、media、form、tracking、translation | 4.4.12 | 2.4、3.8、8.6、13.5、17.11 |
| `DictApi` | `common` | core | inquiry 等使用后台字典的模块 | 4.4.13 | 3.2、25.8 |
| `EventMetaFactory` | `event` | core | 全部发布事件的模块 | 4.4.14 | 4.2 |
| `ExtensionRegistry` | `extension` | core | content、url、delivery、seo、search、graph、media、translation、notification、inquiry、core | 4.4.15 | 3.7、3.8 |
| `ContentApi` | `content` | content | product、article、page、taxonomy、translation、navigation、inquiry、delivery、seo、graph、search（media资格取消，4.10.1） | 4.4.16 | 4.4、5.7、6 |
| `UrlApi` | `url` | url | content、delivery、seo、product、navigation | 4.4.17 | 4.4、13 |
| `MediaApi` | `media` | media | form、product、article、page、inquiry、delivery、seo、search | 4.4.18 | 8.4、12、17.3 |
| `TaxonomyApi` | `taxonomy` | taxonomy | product、article、page、inquiry、navigation、delivery、seo、search、graph | 4.4.19 | 7、16.2、17.7 |
| `FormApi` | `form` | form | inquiry、product | 4.4.20 | 17.1、17.2 |
| `NotificationApi` | `notification` | notification | inquiry、form | 4.4.21 | 18 |
| `TranslationApi` | `translation` | translation | inquiry、seo | 4.4.22 | 5.7、17.10 |
| `ReferenceWriteApi`（Proposed） | `common` | core | content、资源拥有者/core | 4.10.1 | 12.6 |
| `ProjectionChangeApi`（Proposed） | `common` | core | 公开事实/配置写入方 | 4.10.4 | 19 |
| `TransactionMutexApi`（Proposed） | `common` | core | content、page、url、core路由输入写入方 | 4.10.5 | 13 |

#### 4.2.3 扩展点与平台 SPI

| 扩展点 | 包 | 收集调用方 | V1 实现方 | 小节 | 系统设计 |
|---|---|---|---|---|---|
| `@Extension`、`ContentTypeScoped`、`KeyedExtension` | `extension` | core（`ExtensionRegistry`） | 全部扩展点实现 | 4.5.1 | 3.7 |
| `ContentTypeProvider` | `content` | content、url（经 content）、Admin | page、product、article、taxonomy | 4.5.2 | 6.2 |
| `PublishContributor` | `content` | content | product、article、page、taxonomy、seo | 4.5.3 | 4.4、6.3、6.4 |
| `ContentImpactProvider` | `content` | content、core | seo、navigation、graph | 4.5.4 | 3.6、13.6 |
| `TemplateProvider` | `delivery` | delivery、Admin | page | 4.5.5 | 10.2 |
| `DataSourceProvider` | `delivery` | delivery | content、graph、taxonomy、article、search | 4.5.6 | 10.5 |
| `DeliveryContributor` | `delivery` | delivery | seo；框架补充：page（全局区块）、form（内嵌表单定义） | 4.5.7 | 14.2、24.4 |
| `SiteBootstrapContributor` | `delivery` | delivery | navigation、tracking、core | 4.5.8 | 21.2、24.4 |
| `BreadcrumbProvider` | `delivery` | delivery、seo | page、product、article、taxonomy | 4.5.9 | 16.2 |
| `SchemaOrgProvider` | `seo` | seo | product、article、page、core | 4.5.10 | 14.5 |
| `SeoVariableProvider` | `seo` | seo | product、article、taxonomy | 4.5.11 | 14.1 |
| `SeoRule` | `seo` | seo | seo、product | 4.5.12 | 14.9 |
| `SitemapProvider` | `seo` | seo | （预留） | 4.5.13 | 14.6 |
| `SearchDocumentProvider` | `extension` | search | product、article、page | 4.5.14 | 8.2、19 |
| `RelationTypeProvider` | `extension` | graph | graph、product | 4.5.15 | 15.2 |
| `MediaReferenceCollector` | `media` | content、资源拥有者/core（4.10.1） | product、article、page、seo、core；框架补充：navigation（菜单推荐图片） | 4.5.16 | 12.1、12.6 |
| `ContentVariantProvider` | `content` | inquiry | product | 4.5.17 | 3.5、8.3、17.5 |
| `PersonalDataProvider` | `extension` | core（数据主体请求工具） | inquiry、form、notification | 4.5.18 | 17.11 |
| `NotificationTemplateProvider` | `notification` | notification | inquiry、content；框架补充：form（下载链接邮件）、translation（翻译失败通知） | 4.5.19 | 18 |
| `TranslatableContentProvider` | `translation` | translation | product、article、page、taxonomy、seo | 4.5.20 | 5.5–5.7 |
| `TranslatableResourceProvider` | `translation` | translation | navigation、form、media、product、taxonomy、notification、core、tracking、article（4.10.2） | 4.5.21 | 5.5–5.7 |
| `PlatformModule`（模块生命周期 SPI） | `extension` | core（模块管理器） | 每个模块 | 4.5.22 | 3.6 |
| `JobHandler`、`ScheduledTask`（任务 SPI） | `common` | core（任务执行器、调度器） | 需要异步任务或定时任务的模块 | 4.4.12 | 2.4、3.8 |
| `MediaReferenceSink`（Proposed） | `media` | core | media | 4.10.1 | 12.6 |
| `SearchChangeCapture`（Proposed） | `extension` | core | search | 4.10.4 | 19 |

#### 4.2.4 领域事件

全部在 `com.b2bweb.api.event` 包中，定义见 4.6。

| 事件 | 发布方 | 订阅方 | 小节 | 系统设计 |
|---|---|---|---|---|
| `ContentCreatedEvent` | content | graph | 4.6.2 | 4.3 |
| `ContentDraftChangedEvent`（框架补充） | content | 诊断/源修复提示，引用已同步登记（4.10.1） | 4.6.2 | 12.1 |
| `ContentPublishedEvent` | content | seo、graph、search、delivery、translation | 4.6.2 | 4.3、4.4、5.7 |
| `ContentUnpublishedEvent` | content | seo、graph、search、delivery | 4.6.2 | 4.3 |
| `ContentTrashedEvent` / `ContentRestoredEvent` | content | graph、search、delivery | 4.6.2 | 4.3 |
| `ContentPurgedEvent` | content | 所有以 `content_id` 保存数据的模块 | 4.6.2 | 4.3、25.5 |
| `LocalizationCreatedEvent` / `LocalizationDeletedEvent` | content | seo、delivery、translation | 4.6.2 | 4.3 |
| `TranslationModeChangedEvent`（框架补充） | content | translation | 4.6.2 | 5.5 |
| `TranslationOutdatedEvent` | content | notification | 4.6.2 | 4.3、5.5 |
| `ReviewRequestedEvent` / `ReviewCompletedEvent` | content | notification | 4.6.2 | 4.3、6.7 |
| `ContentPathChangedEvent` | url | seo、delivery、content（同步投影） | 4.6.3 | 4.3、13.4 |
| `RedirectChangedEvent` | url | delivery | 4.6.3 | 4.3 |
| `TermChangedEvent` | taxonomy | graph、search、delivery、content、product、navigation | 4.6.4 | 4.3、7.3 |
| `MediaUploadedEvent` / `MediaUpdatedEvent` / `MediaDeletedEvent` | media | delivery、seo | 4.6.5 | 4.3 |
| `NavigationChangedEvent` | navigation | delivery | 4.6.6 | 4.3、16.1 |
| `SiteSettingsChangedEvent` | core | delivery、seo、url、search、translation、media | 4.6.7 | 4.3 |
| `ModuleEnabledEvent` / `ModuleDisabledEvent` | core | delivery、seo、search、graph | 4.6.7 | 4.3、3.8 |
| `UserDeactivatedEvent`（框架补充） | core | inquiry | 4.6.7 | 17.7 |
| `RelationChangedEvent` | graph | delivery | 4.6.8 | 4.3 |
| `FormSubmittedEvent` | form | inquiry | 4.6.9 | 17.2 |
| `FormChangedEvent`（框架补充） | form | delivery | 4.6.9 | 17.1 |
| `SiteResourceChangedEvent`（Proposed） | article、tracking、url | delivery、content（url.pattern） | 4.10.2 | 4.3、20、23.3 |
| `InquiryCreatedEvent` / `InquiryAssignedEvent` / `InquiryStatusChangedEvent` | inquiry | V1 无跨模块订阅方；V2：crm、analytics | 4.6.10 | 4.3 |
| `TranslationCompletedEvent` | translation | content、delivery | 4.6.11 | 4.3、5.7 |
| `TranslationFailedEvent` | translation | notification | 4.6.11 | 4.3 |
| `TranslatableResourceChangedEvent` | navigation、form、media、product、taxonomy、notification、core、tracking | translation | 4.6.11 | 4.3、5.7 |

#### 4.2.5 基础设施端口

全部在 `com.b2bweb.api.port` 包中，由 `backend/infrastructure` 实现，定义见 4.7。

| 端口 | 默认适配器 | 使用方 | 小节 | 系统设计 |
|---|---|---|---|---|
| `ObjectStorage` | S3 兼容存储（MinIO、R2、S3、OSS）；本地文件（仅开发） | media、core（任务文件） | 4.7.1 | 12.5 |
| `ImageUrlSigner` | imgproxy 签名 URL | media | 4.7.2 | 12.3 |
| `VirusScanner` | ClamAV | media | 4.7.3 | 12.2、17.3 |
| `MailTransport` | SMTP / 邮件服务商 API；开发环境 Mailpit | notification、core（数据主体请求核验邮件） | 4.7.4 | 18 |
| `WebhookTransport` | Spring HTTP 客户端 | notification | 4.7.5 | 18 |
| `SearchEngineClient` | Meilisearch | search | 4.7.6 | 19 |
| `CdnPurger` | 支持 Cache-Tag 清除的 CDN（如 Cloudflare、Fastly）；开发环境为空实现 | delivery、seo | 4.7.7 | 23.3 |
| `IndexNowClient` | IndexNow HTTP 接口 | seo | 4.7.8 | 14.7 |
| `TranslationEngine` | OpenAI 兼容 Chat Completions（Spring HTTP 客户端） | translation | 4.7.9 | 5.9 |
| `CaptchaVerifier` | Cloudflare Turnstile siteverify | form | 4.7.10 | 17.2 |
| `GeoIpResolver` | 本地 GeoIP 数据库 | form、tracking | 4.7.11 | 17.9 |
| `MxRecordChecker` | DNS 查询 | form | 4.7.12 | 17.4 |
| `RemoteFileFetcher` | 带 SSRF 防护的 HTTP 下载 | product（导入图片 URL）、media | 4.7.13 | 8.6、26.3 |
| `TaggedCache` | Redis（标签索引） | delivery、seo、url | 4.7.14 | 23.1–23.3 |
| `RateLimiter` | Bucket4j + Redis | form、search、tracking、core | 4.7.15 | 26.4 |
| `DistributedLock` | Redis | delivery、graph、translation、url | 4.7.16 | 23.3 |
| `BufferStore` | Redis（计数器与延迟集合） | url、graph、delivery、seo | 4.7.17 | 13.2、15.4、23.3 |

### 4.3 公共类型

本节定义被多个契约共用的类型。服务接口、扩展点专用的 DTO 在对应小节定义。

#### 4.3.1 站点上下文

`SiteContext` 是当前线程的站点信息快照，由 platform-core 在请求入口、事件监听器包装器、任务执行器中建立（系统设计 5.2），通过 `SiteContextProvider`（4.4.2）读取。

```java
package com.b2bweb.api.site;

/**
 * 当前站点上下文。不可变；站点配置变化后在下一个请求 / 任务中生效。
 *
 * @param siteId         站点 ID
 * @param siteCode       站点代码（日志、Redis 键前缀之外的可读标识）
 * @param sourceLocale   源语言，恒为 {@code zh-CN}
 * @param defaultLocale  默认对外语言（根路径 302 与 x-default 的目标）
 * @param timezone       站点时区（定时发布的输入与展示、按天统计）
 * @param requestLocale  当前请求的内容语言：前台请求由路径前缀解析；后台请求、任务、事件为空
 * @param origin         上下文来源
 */
public record SiteContext(
        long siteId,
        String siteCode,
        String sourceLocale,
        String defaultLocale,
        java.time.ZoneId timezone,
        @org.jspecify.annotations.Nullable String requestLocale,
        ContextOrigin origin) {
}

/** 站点上下文的来源。 */
public enum ContextOrigin {
    /** 后台请求（X-Site-Id，已校验用户在该站点有角色） */
    ADMIN,
    /** 前台公开请求或 Nuxt → Delivery 请求（按域名解析） */
    PUBLIC,
    /** 内部接口（/api/internal） */
    INTERNAL,
    /** 领域事件监听（取自 DomainEvent.siteId） */
    EVENT,
    /** 异步任务（取自 sys_job.site_id） */
    JOB,
    /** 定时任务（调度器按站点逐个建立） */
    SCHEDULED,
    /** 系统管理操作中显式切换（SiteContextProvider.callAs） */
    SYSTEM
}
```

#### 4.3.2 语言

```java
package com.b2bweb.api.common;

/** 语言代码工具。语言代码为 BCP 47 字符串；源语言固定为 zh-CN（系统设计 5.4）。 */
public final class Locales {
    private Locales() {}

    /** 源语言：内容与站点资源的录入语言。 */
    public static final String SOURCE = "zh-CN";

    /** 语言代码最大长度（与数据库 VARCHAR(16) 一致）。 */
    public static final int MAX_LENGTH = 16;

    public static boolean isSource(String locale) {
        return SOURCE.equals(locale);
    }

    /** 是否为合法的 BCP 47 语言代码（只允许 语言[-文字][-地区]，如 zh-CN、en、pt-BR、zh-Hans）。 */
    public static boolean isValid(String locale) {
        return locale.length() <= MAX_LENGTH
                && locale.matches("^[a-z]{2,3}(-[A-Z][a-z]{3})?(-([A-Z]{2}|[0-9]{3}))?$");
    }

    /** 规范大小写：语言小写、文字首字母大写、地区大写，如 "ZH-cn" → "zh-CN"。非法时抛出 IllegalArgumentException。 */
    public static String normalize(String locale) {
        String tag = java.util.Locale.forLanguageTag(locale.replace('_', '-')).toLanguageTag();
        if (!isValid(tag)) {
            throw new IllegalArgumentException("非法的语言代码: " + locale);
        }
        return tag;
    }
}
```

- 站点启用了哪些语言、URL 前缀、hreflang、是否对外公开，只能通过 `SiteApi`（4.4.1）读取，不在代码中写死。
- 内容不跨语言回退（系统设计 5.4）：所有按 `locale` 查询的方法只返回该语言的数据，不存在即为空。

#### 4.3.3 内容类型与状态枚举

```java
package com.b2bweb.api.common;

/** V1 内置内容类型（系统设计 6.2）。内容类型是字符串，V2 模块可以通过 ContentTypeProvider 注册新类型。 */
public final class ContentTypes {
    private ContentTypes() {}
    public static final String PAGE = "PAGE";
    public static final String PRODUCT = "PRODUCT";
    public static final String ARTICLE = "ARTICLE";
    public static final String CASE_STUDY = "CASE_STUDY";
    public static final String FAQ = "FAQ";
    public static final String TERM_PAGE = "TERM_PAGE";
    public static final String GLOBAL_BLOCK = "GLOBAL_BLOCK";

    /** 内容类型格式：大写字母开头，大写字母、数字、下划线，最长 32。 */
    public static boolean isValidFormat(String type) {
        return type.matches("^[A-Z][A-Z0-9_]{1,31}$");
    }
}
```

```java
package com.b2bweb.api.content;

/** 草稿状态（系统设计 6.3）。 */
public enum DraftState { EDITING, IN_REVIEW, APPROVED }

/** 发布状态（系统设计 6.3）。 */
public enum PublishState { NEVER, SCHEDULED, PUBLISHED, UNPUBLISHED }

/** 下线策略（系统设计 13.6）。 */
public enum UnpublishStrategy {
    /** 410（默认） */
    GONE,
    /** 301 到指定内容的同语言版本 */
    REDIRECT_CONTENT,
    /** 301 到主分类页或父页面（由所属模块的 RouteHints.fallbackParentContentId 决定） */
    REDIRECT_PARENT
}

/** 修订的发布方式。 */
public enum PublishTrigger { MANUAL, SCHEDULED, TRANSLATION }
```

```java
package com.b2bweb.api.translation;

/** 本地化的翻译模式（系统设计 5.5）。 */
public enum TranslationMode {
    /** 源语言本地化本身 */
    SOURCE,
    /** 由源语言 + 翻译记忆生成（目标语言默认） */
    AUTO,
    /** 独立编辑；源语言更新后只标记 OUTDATED */
    MANUAL
}

/** 翻译状态：内容本地化与站点资源本地化通用（系统设计 5.5）。 */
public enum TranslationState { NONE, QUEUED, TRANSLATED, NEEDS_REVIEW, FAILED, OUTDATED }

/** 翻译记忆条目来源，优先级 HUMAN > REVIEWED > MACHINE（系统设计 5.6）。 */
public enum TranslationOrigin {
    MACHINE(1), REVIEWED(2), HUMAN(3);
    private final int priority;
    TranslationOrigin(int priority) { this.priority = priority; }
    public int priority() { return priority; }
    /** 机器译文永远不能覆盖 HUMAN / REVIEWED。 */
    public boolean canBeOverwrittenBy(TranslationOrigin other) { return other.priority >= this.priority; }
}

/** 翻译单元格式。 */
public enum TranslationFormat {
    /** 纯文本 */
    TEXT,
    /** 富文本块级节点：行内标记已转为编号占位标签 <1>…</1>（系统设计 5.6） */
    RICH_INLINE
}
```

#### 4.3.4 操作人

```java
package com.b2bweb.api.common;

/**
 * 操作人。系统操作的 userId 为 0（与数据库 created_by = 0 一致），事件中记为 "system"。
 *
 * @param userId       用户 ID；0 表示系统
 * @param displayName  显示名（审计快照用）；系统为 "system"
 */
public record ActorRef(long userId, String displayName) {
    public static final ActorRef SYSTEM = new ActorRef(0L, "system");

    public static ActorRef user(long userId, String displayName) {
        if (userId <= 0) throw new IllegalArgumentException("用户 ID 必须为正数");
        return new ActorRef(userId, displayName);
    }

    public boolean isSystem() { return userId == 0L; }

    /** 事件中的 actor 字段：用户 ID 的十进制字符串，或 "system"（系统设计 4.2）。 */
    public String toEventActor() { return isSystem() ? "system" : Long.toString(userId); }

    /** 从事件的 actor 字段还原（displayName 不可还原，置为 actor 原值）。 */
    public static ActorRef fromEventActor(String actor) {
        return "system".equals(actor) ? SYSTEM : new ActorRef(Long.parseLong(actor), actor);
    }
}
```

#### 4.3.5 对象引用

快照、布局文档与富文本只保存对象 ID，不保存 URL（系统设计 6.4、10.3；CLAUDE.md 禁止 4）。以下类型与 JSON 中的引用对象一一对应：

| JSON 形式 | Java 类型 |
|---|---|
| `{ "$content": "7201893453912345" }` | `ContentRef` |
| `{ "$media": "7201893453918208" }` | `MediaRef` |
| `{ "$term": "7201893453900001" }` | `TermRef` |
| `{ "$source": { "provider": "content-query", "params": { … } } }` | `SourceRef` / `DataSourceSpec` |
| 富文本链接 `content://7201893453912345` | `ContentRef.fromUri` / `ContentRef.toUri` |

```java
package com.b2bweb.api.common;

public record ContentRef(@com.fasterxml.jackson.annotation.JsonProperty("$content") long contentId) {
    public static final String URI_SCHEME = "content://";

    public String toUri() { return URI_SCHEME + contentId; }

    /** 解析 content://{id}；格式不符时返回 Optional.empty()。 */
    public static java.util.Optional<ContentRef> fromUri(String uri) {
        if (!uri.startsWith(URI_SCHEME)) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(new ContentRef(Long.parseLong(uri.substring(URI_SCHEME.length()))));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}

public record MediaRef(@com.fasterxml.jackson.annotation.JsonProperty("$media") long mediaId) {}

public record TermRef(@com.fasterxml.jackson.annotation.JsonProperty("$term") long termId) {}

/** 数据源绑定：provider 为 DataSourceProvider.key()，params 由该数据源的 paramsSchema 校验。 */
public record DataSourceSpec(String provider, tools.jackson.databind.JsonNode params) {}

public record SourceRef(@com.fasterxml.jackson.annotation.JsonProperty("$source") DataSourceSpec source) {}

/**
 * 一组被引用的对象 ID。用于：Delivery 解析 refs 与生成缓存标签、媒体引用收集、SEO 链接索引。
 */
public record ReferenceSet(java.util.Set<Long> contentIds, java.util.Set<Long> mediaIds, java.util.Set<Long> termIds) {
    public ReferenceSet {
        contentIds = java.util.Set.copyOf(contentIds);
        mediaIds = java.util.Set.copyOf(mediaIds);
        termIds = java.util.Set.copyOf(termIds);
    }

    public static ReferenceSet empty() {
        return new ReferenceSet(java.util.Set.of(), java.util.Set.of(), java.util.Set.of());
    }

    public ReferenceSet merge(ReferenceSet other) {
        var c = new java.util.HashSet<>(contentIds); c.addAll(other.contentIds);
        var m = new java.util.HashSet<>(mediaIds); m.addAll(other.mediaIds);
        var t = new java.util.HashSet<>(termIds); t.addAll(other.termIds);
        return new ReferenceSet(c, m, t);
    }

    public boolean isEmpty() { return contentIds.isEmpty() && mediaIds.isEmpty() && termIds.isEmpty(); }

    /**
     * 扫描任意 JSON（快照、布局文档、ProseMirror 文档），收集全部引用：
     * 对象键 "$content" / "$media" / "$term" 的值；任意字符串值或 ProseMirror link 标记的 href 中以 content:// 开头的值。
     * 纯函数，所有模块使用同一实现，保证引用识别规则一致。
     */
    public static ReferenceSet scan(tools.jackson.databind.JsonNode json) {
        var content = new java.util.HashSet<Long>(); var media = new java.util.HashSet<Long>();
        var terms = new java.util.HashSet<Long>();
        var pending = new java.util.ArrayDeque<tools.jackson.databind.JsonNode>(); pending.push(json);
        while (!pending.isEmpty()) {
            var node = pending.pop();
            if (node.isObject()) {
                for (var entry : node.properties()) {
                    var target = switch (entry.getKey()) {
                        case "$content" -> content; case "$media" -> media; case "$term" -> terms;
                        default -> null;
                    };
                    var value = entry.getValue();
                    if (target != null && (value.isString() || value.isIntegralNumber())) {
                        try { long id = Long.parseLong(value.asString()); if (id > 0) target.add(id); }
                        catch (NumberFormatException ignored) { /* malformed refs are validated by the owner */ }
                    }
                    pending.push(value);
                }
            } else if (node.isArray()) node.forEach(pending::push);
            else if (node.isString()) ContentRef.fromUri(node.asString()).filter(ref -> ref.contentId() > 0)
                    .ifPresent(ref -> content.add(ref.contentId()));
        }
        return new ReferenceSet(content, media, terms);
    }
}
```

> 代码块中标注“示意”的方法体在 M0 中按 Javadoc 实现（纯函数，无外部依赖），契约以方法签名与 Javadoc 为准。

#### 4.3.6 分页

```java
package com.b2bweb.api.common;

/** 排序：field 为提供方白名单中的字段名（camelCase）。 */
public record SortOrder(String field, boolean descending) {
    public static SortOrder asc(String field) { return new SortOrder(field, false); }
    public static SortOrder desc(String field) { return new SortOrder(field, true); }

    /** 解析 "-publishedAt" / "title" 形式（与 REST 查询参数和数据源参数一致）。 */
    public static SortOrder parse(String expr) {
        return expr.startsWith("-") ? desc(expr.substring(1)) : asc(expr);
    }
}

/** 页码分页：page 从 1 开始，size 1–100（系统设计 24.2）。 */
public record PageRequest(int page, int size, java.util.List<SortOrder> sort) {
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 1) throw new IllegalArgumentException("page 从 1 开始");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size 必须在 1–100 之间");
        sort = java.util.List.copyOf(sort);
    }

    public static PageRequest of(int page, int size) { return new PageRequest(page, size, java.util.List.of()); }

    public long offset() { return (long) (page - 1) * size; }
}

public record PageResult<T>(java.util.List<T> items, int page, int size, long total) {
    public PageResult { items = java.util.List.copyOf(items); }

    public static <T> PageResult<T> empty(PageRequest request) {
        return new PageResult<>(java.util.List.of(), request.page(), request.size(), 0L);
    }

    public <R> PageResult<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, total);
    }
}

/** 游标分页：cursor 为提供方生成的不透明字符串，首页为 null；size 1–1000。 */
public record CursorRequest(@org.jspecify.annotations.Nullable String cursor, int size) {
    public static final int MAX_SIZE = 1000;

    public CursorRequest {
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size 必须在 1–1000 之间");
    }

    public static CursorRequest first(int size) { return new CursorRequest(null, size); }
}

/** nextCursor 为空表示没有更多数据。 */
public record CursorPage<T>(java.util.List<T> items, @org.jspecify.annotations.Nullable String nextCursor) {
    public CursorPage { items = java.util.List.copyOf(items); }

    public boolean hasMore() { return nextCursor != null; }
}
```

#### 4.3.7 缓存标签

缓存标签的格式由系统设计 23.2 规定，所有模块只通过 `CacheTag` 的工厂方法生成，不手工拼接字符串。

```java
package com.b2bweb.api.common;

/** 缓存标签（CDN Cache-Tag 与 Redis 标签索引共用），全部带站点前缀 s{siteId}。 */
public record CacheTag(String value) {
    /** 单个响应的标签上限；超过时把卡片标签合并为 list:{type}（系统设计 23.2）。 */
    public static final int MAX_TAGS_PER_RESPONSE = 200;

    public CacheTag {
        if (!value.matches("^s\\d+(:[a-z]+(:[A-Za-z0-9_\\-]+)?)?$")) {
            throw new IllegalArgumentException("非法的缓存标签: " + value);
        }
    }

    /** s{site}：站点全部 */
    public static CacheTag site(long siteId) { return new CacheTag("s" + siteId); }
    /** s{site}:c:{contentId}：某内容（页面主体、卡片或链接） */
    public static CacheTag content(long siteId, long contentId) { return new CacheTag("s" + siteId + ":c:" + contentId); }
    /** s{site}:t:{termId}：分类项（名称、列表） */
    public static CacheTag term(long siteId, long termId) { return new CacheTag("s" + siteId + ":t:" + termId); }
    /** s{site}:m:{mediaId}：媒体（alt、尺寸） */
    public static CacheTag media(long siteId, long mediaId) { return new CacheTag("s" + siteId + ":m:" + mediaId); }
    /** s{site}:g:{contentId}：某内容的关系列表 */
    public static CacheTag graph(long siteId, long contentId) { return new CacheTag("s" + siteId + ":g:" + contentId); }
    /** s{site}:list:{type}：某类型的列表 */
    public static CacheTag list(long siteId, String contentType) { return new CacheTag("s" + siteId + ":list:" + contentType); }
    /** s{site}:f:{formId}：表单定义（框架补充：内嵌表单定义的页面随表单变化失效） */
    public static CacheTag form(long siteId, long formId) { return new CacheTag("s" + siteId + ":f:" + formId); }
    /** s{site}:nav：导航 */
    public static CacheTag nav(long siteId) { return new CacheTag("s" + siteId + ":nav"); }
    /** s{site}:settings：站点设置、主题、站点资料、界面文案 */
    public static CacheTag settings(long siteId) { return new CacheTag("s" + siteId + ":settings"); }
    /** s{site}:sitemap：sitemap */
    public static CacheTag sitemap(long siteId) { return new CacheTag("s" + siteId + ":sitemap"); }
}
```

- `s{site}:f:{formId}` 是对系统设计 23.2 的补充（表单定义内嵌在 resolve 结果中，系统设计 24.4），需同步更新 23.2。
- 新增标签类型必须在本类中增加工厂方法，并同步更新系统设计 23.2（ADR-012 的规则）。

#### 4.3.8 校验结果

`ValidationResult` 是发布校验（`PublishContributor.validate`）、导入校验等场景的收集器；`Severity` 同时用于 SEO 规则与影响报告。

```java
package com.b2bweb.api.common;

public enum Severity { ERROR, WARNING, INFO }

/**
 * 一条校验问题。
 *
 * @param code     错误码，格式 {MODULE}_{REASON}，前端据此映射说明或跳转
 * @param message  中文说明（后台展示）
 * @param field    字段路径（如 "slug"、"parts.product.modelNo"、"layout.sections[2].props.title"），无则为空
 * @param source   产生问题的模块 ID（由收集方填写）
 */
public record ValidationIssue(Severity severity, String code, String message,
                              @org.jspecify.annotations.Nullable String field,
                              @org.jspecify.annotations.Nullable String source) {}

/** 校验收集器。非线程安全：每次校验创建一个实例。 */
public final class ValidationResult {
    private final java.util.List<ValidationIssue> issues;
    private final @org.jspecify.annotations.Nullable String source;

    public ValidationResult() { this(null); }
    public ValidationResult(@org.jspecify.annotations.Nullable String source) { this(source, new java.util.ArrayList<>()); }
    private ValidationResult(@org.jspecify.annotations.Nullable String source, java.util.List<ValidationIssue> issues) {
        this.source = source; this.issues = issues;
    }

    public ValidationResult error(String code, String message) { return add(Severity.ERROR, code, null, message); }
    public ValidationResult error(String code, String field, String message) { return add(Severity.ERROR, code, field, message); }
    public ValidationResult warning(String code, String message) { return add(Severity.WARNING, code, null, message); }
    public ValidationResult warning(String code, String field, String message) { return add(Severity.WARNING, code, field, message); }
    public ValidationResult info(String code, String message) { return add(Severity.INFO, code, null, message); }

    public ValidationResult add(Severity severity, String code,
                                @org.jspecify.annotations.Nullable String field, String message) {
        issues.add(new ValidationIssue(severity, code, message, field, source));
        return this;
    }

    public ValidationResult addAll(java.util.Collection<ValidationIssue> more) { issues.addAll(more); return this; }

    /** 返回一个写入同一列表、但 source 为指定模块的视图（收集方为每个扩展点实现创建）。 */
    public ValidationResult forSource(String moduleId) { return new ValidationResult(moduleId, issues); }

    public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR); }
    public java.util.List<ValidationIssue> issues() { return java.util.List.copyOf(issues); }
    public java.util.List<ValidationIssue> errors() {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
    }
}

/** 存在 ERROR 时由收集方抛出（HTTP 422），problem+json 的 errors[] 来自 ERROR 级问题，properties.issues 含全部问题。 */
public class ValidationFailedException extends PlatformException {
    private final java.util.List<ValidationIssue> issues;

    public ValidationFailedException(ErrorCode code, String detail, java.util.List<ValidationIssue> issues) {
        super(code, detail,
              issues.stream().filter(i -> i.severity() == Severity.ERROR)
                    .map(i -> new FieldError(i.field() == null ? "" : i.field(), i.code(), i.message())).toList(),
              java.util.Map.of("issues", issues), null);
        this.issues = java.util.List.copyOf(issues);
    }

    public java.util.List<ValidationIssue> issues() { return issues; }
}
```

#### 4.3.9 影响报告

用于下线、移入回收站、彻底删除、停用模块、删除分类项与媒体前的影响分析（`ContentImpactProvider`、`PlatformModule.analyzeDisableImpact`）。

```java
package com.b2bweb.api.common;

/** 被分析的操作。 */
public enum ImpactAction { UNPUBLISH, TRASH, PURGE, DISABLE_MODULE }

/**
 * 一条影响。
 *
 * @param source     产生该条目的模块 ID
 * @param severity   ERROR：阻止操作；WARNING：需要用户确认；INFO：仅提示
 * @param code       {MODULE}_{REASON}
 * @param message    中文说明，如“该产品被 3 个页面引用，下线后对应卡片将被移除”
 * @param count      受影响对象的数量
 * @param contentIds 受影响的内容 ID（最多 50 个，用于后台跳转）
 * @param adminRoute 后台查看详情的路由（可空），如 "/seo/links?target=7201893453912345"
 */
public record ImpactItem(String source, Severity severity, String code, String message, int count,
                         java.util.List<Long> contentIds,
                         @org.jspecify.annotations.Nullable String adminRoute) {
    public ImpactItem { contentIds = java.util.List.copyOf(contentIds); }
}

public record ImpactReport(java.util.List<ImpactItem> items) {
    public ImpactReport { items = java.util.List.copyOf(items); }

    public static ImpactReport none() { return new ImpactReport(java.util.List.of()); }

    public boolean blocking() { return items.stream().anyMatch(i -> i.severity() == Severity.ERROR); }

    public boolean requiresConfirmation() { return items.stream().anyMatch(i -> i.severity() == Severity.WARNING); }

    public ImpactReport merge(ImpactReport other) {
        var all = new java.util.ArrayList<>(items);
        all.addAll(other.items());
        return new ImpactReport(all);
    }
}
```

#### 4.3.10 翻译单元与翻译上下文

`TranslatableContentProvider` / `TranslatableResourceProvider`（4.5.20、4.5.21）与 translation 之间交换的数据（系统设计 3.7、5.6）。

```java
package com.b2bweb.api.translation;

/**
 * 翻译单元（Segment）。
 *
 * @param key        字段路径，在同一个提供方、同一个对象内唯一且稳定，如 "title"、"seo.title"、"body[3]"、
 *                   "layout.sec_01J9X4K2.props.title"。translation 存储时自动加提供方模块前缀，提供方不必全局唯一
 * @param text       原文（源语言）。RICH_INLINE 格式中行内标记已转为编号占位标签 <1>…</1>
 * @param format     TEXT / RICH_INLINE
 * @param maxLength  可选的长度上限（字符数），如 SEO 标题 60；超长时 translation 生成 max{N} 变体
 * @param context    上下文说明（中文），如“产品名称”“按钮文字”，进入提示词帮助模型理解
 */
public record TranslationUnit(String key, String text, TranslationFormat format,
                              @org.jspecify.annotations.Nullable Integer maxLength,
                              @org.jspecify.annotations.Nullable String context) {
    public TranslationUnit {
        if (key.isBlank()) throw new IllegalArgumentException("key 不能为空");
        if (maxLength != null && maxLength <= 0) throw new IllegalArgumentException("maxLength 必须为正数");
    }

    public static TranslationUnit text(String key, String text, @org.jspecify.annotations.Nullable String context) {
        return new TranslationUnit(key, text, TranslationFormat.TEXT, null, context);
    }
}

/** 翻译对象：内容或站点资源。 */
public sealed interface TranslationSubject permits TranslationSubject.Content, TranslationSubject.Resource {

    /**
     * 内容：源语言本地化 → 目标语言本地化。
     *
     * @param sourceRevisionNo 本次翻译依据的源语言修订号
     */
    record Content(long contentId, String contentType, long sourceLocalizationId,
                   long targetLocalizationId, int sourceRevisionNo) implements TranslationSubject {}

    /**
     * 站点资源。
     *
     * @param resourceType 资源类型，见 TranslatableResourceTypes（如 "navigation.menu"）
     * @param resourceId   资源 ID（TSID 的十进制字符串；界面文案为文案键）
     */
    record Resource(String resourceType, String resourceId) implements TranslationSubject {}
}

/**
 * 翻译上下文：携带站点、翻译对象、源语言与目标语言。
 *
 * @param jobId translation 的任务 ID（trn_job.id），用于日志与幂等；extract 预估时为 0
 * @param expectedTargetVersion 内容任务在调用引擎前捕获并持久化的目标草稿version；内容apply必填，预估extract及资源上下文可空
 */
public record TranslationContext(long siteId, TranslationSubject subject, String sourceLocale,
                                 String targetLocale, long jobId, @Nullable Integer expectedTargetVersion) {

    public static TranslationContext forContent(long siteId, TranslationSubject.Content subject,
                                                String targetLocale, long jobId, @Nullable Integer expectedTargetVersion) {
        return new TranslationContext(siteId, subject, com.b2bweb.api.common.Locales.SOURCE, targetLocale, jobId, expectedTargetVersion);
    }

    public static TranslationContext forResource(long siteId, String resourceType, String resourceId,
                                                 String targetLocale, long jobId) {
        return new TranslationContext(siteId, new TranslationSubject.Resource(resourceType, resourceId),
                com.b2bweb.api.common.Locales.SOURCE, targetLocale, jobId, null);
    }

    /** 内容翻译时取内容对象；资源翻译时调用抛出 IllegalStateException。 */
    public TranslationSubject.Content content() {
        if (subject instanceof TranslationSubject.Content c) return c;
        throw new IllegalStateException("不是内容翻译上下文");
    }

    /** 资源翻译时取资源对象；内容翻译时调用抛出 IllegalStateException。 */
    public TranslationSubject.Resource resource() {
        if (subject instanceof TranslationSubject.Resource r) return r;
        throw new IllegalStateException("不是资源翻译上下文");
    }
}
```

#### 4.3.11 引用解析器

扩展点实现（如 core 的 `SchemaOrgProvider`、`SiteBootstrapContributor`）需要把媒体、内容、分类 ID 解析为 URL 与名称，但 platform-core 不能调用 L2 的服务接口（4.1.3）。因此由收集方（delivery、seo、search）在上下文中提供 `RefResolver`，实现方只通过它解析引用；解析过的对象自动计入缓存标签。

```java
package com.b2bweb.api.delivery;

/** 引用解析器：只解析当前站点、当前语言的已发布对象（内容不跨语言回退）。 */
public interface RefResolver {

    /** 解析所用的语言。 */
    String locale();

    /** 内容在当前语言的已发布版本；未发布或不存在时为空。 */
    java.util.Optional<ResolvedContent> content(long contentId);

    /** 批量解析内容；结果只含已发布的内容。 */
    java.util.Map<Long, ResolvedContent> contents(java.util.Collection<Long> contentIds);

    /** 图片：按预设生成 URL（公开图片），带当前语言的 alt。 */
    java.util.Optional<ResolvedImage> image(long mediaId, com.b2bweb.api.media.ImagePreset preset);

    /** 公开文件（PDF 等）的下载 URL；私有文件返回空。 */
    java.util.Optional<ResolvedFile> file(long mediaId);

    /** 分类项在当前语言的名称与落地页路径。 */
    java.util.Optional<ResolvedTerm> term(long termId);

    /** 站内路径转为绝对 URL（主域名 https + 尾斜杠策略）。 */
    String absoluteUrl(String path);

    /** 至今解析过的全部引用（收集方据此生成缓存标签与 refs）。 */
    com.b2bweb.api.common.ReferenceSet touched();
}

/** @param path 站内路径（含语言前缀，已按尾斜杠策略输出）；@param url 绝对 URL */
public record ResolvedContent(long contentId, String contentType, String title,
                              @org.jspecify.annotations.Nullable String summary,
                              @org.jspecify.annotations.Nullable Long coverMediaId,
                              @org.jspecify.annotations.Nullable String path,
                              @org.jspecify.annotations.Nullable String url,
                              java.time.Instant publishedAt) {}

public record ResolvedImage(long mediaId, String url, int width, int height,
                            @org.jspecify.annotations.Nullable String alt,
                            @org.jspecify.annotations.Nullable String blurhash,
                            @org.jspecify.annotations.Nullable String dominantColor) {}

public record ResolvedFile(long mediaId, String url, String fileName, String mime, long size,
                           @org.jspecify.annotations.Nullable String title) {}

public record ResolvedTerm(long termId, String vocabularyKey, String name,
                           @org.jspecify.annotations.Nullable String path,
                           @org.jspecify.annotations.Nullable String url) {}

/** 交付模式。 */
public enum DeliveryMode {
    /** 线上：只读快照与投影 */
    LIVE,
    /** 预览：草稿或指定修订（noindex、no-store） */
    PREVIEW,
    /** 页面编辑器画布：未保存的布局文档 */
    EDITOR
}
```

#### 4.3.12 其他共享类型

```java
package com.b2bweb.api.common;

/** 敏感值包装：toString 永远输出掩码，防止 API Key、密码进入日志（系统设计 26.6）。 */
public record Secret(String value) {
    @Override
    public String toString() { return "Secret[****]"; }

    public static Secret of(String value) { return new Secret(value); }
}

/** 语义化版本（模块版本、平台版本）。 */
public record SemVer(int major, int minor, int patch,
                     @org.jspecify.annotations.Nullable String preRelease) implements Comparable<SemVer> {

    /** 解析 "1.2.3" 或 "1.2.3-rc.1"；格式错误抛出 IllegalArgumentException。 */
    public static SemVer parse(String text) {
        var match = java.util.regex.Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$").matcher(text);
        if (!match.matches()) throw new IllegalArgumentException("非法语义版本: " + text);
        String pre = match.group(4);
        if (pre != null) for (String id : pre.split("\\."))
            if (id.matches("[0-9]+") && id.length() > 1 && id.startsWith("0")) throw new IllegalArgumentException("版本数字不得有前导零");
        return new SemVer(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)), pre);
    }

    @Override
    public int compareTo(SemVer o) {
        int result = Integer.compare(major, o.major); if (result != 0) return result;
        result = Integer.compare(minor, o.minor); if (result != 0) return result;
        result = Integer.compare(patch, o.patch); if (result != 0) return result;
        if (preRelease == null) return o.preRelease == null ? 0 : 1;
        if (o.preRelease == null) return -1;
        String[] left = preRelease.split("\\."), right = o.preRelease.split("\\.");
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            boolean a = left[i].matches("[0-9]+"), b = right[i].matches("[0-9]+");
            result = a && b ? new java.math.BigInteger(left[i]).compareTo(new java.math.BigInteger(right[i]))
                    : a != b ? (a ? -1 : 1) : left[i].compareTo(right[i]);
            if (result != 0) return result;
        }
        return Integer.compare(left.length, right.length);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (preRelease == null ? "" : "-" + preRelease);
    }
}
```

### 4.4 服务接口

本节逐个给出服务接口的完整签名。每个接口先说明用途，再给出提供方、使用方与代码；接口专用的 DTO 与错误码紧随其后。4.4.1–4.4.15 是 platform-core 提供的平台服务，4.4.16–4.4.22 是 L2 模块提供的服务。

#### 4.4.1 SiteApi：站点、域名、语言与模块启用状态

站点信息的只读查询：站点、域名解析、站点语言（URL 前缀、hreflang、对外公开、自动翻译）、模块启用状态。站点的增删改是 core 的系统管理功能，不进入契约。

- 提供方：core。使用方：全部模块。
- 结果在应用内缓存，站点配置变化时（`SiteSettingsChangedEvent`）经 Redis Pub/Sub 广播失效（系统设计 23.1）。

```java
package com.b2bweb.api.site;

public interface SiteApi {

    /** 当前站点（取自站点上下文）。没有站点上下文时抛出 NoSiteContextException。 */
    SiteInfo current();

    /** 按 ID 查询站点；不存在时为空。 */
    java.util.Optional<SiteInfo> find(long siteId);

    /** 全部未归档的站点（SETUP、ACTIVE），按 ID 升序；系统级任务逐站点执行时使用。 */
    java.util.List<SiteInfo> listOperationalSites();

    /**
     * 按请求的 Host 解析站点（系统设计 5.2）。host 为小写、不含端口（国际化域名为 Punycode）。
     * 未登记的域名、归档站点的域名返回空。Delivery 热路径。
     */
    java.util.Optional<HostResolution> resolveHost(String host);

    /** 当前站点的全部语言（含源语言），按 sortOrder 排序。 */
    java.util.List<SiteLocaleInfo> locales();

    /** 当前站点的某个语言；站点未启用该语言时为空。 */
    java.util.Optional<SiteLocaleInfo> locale(String locale);

    /** 按 URL 前缀（不含斜杠，如 "de"、"pt-br"）查找语言；前台路径解析使用。Delivery 热路径。 */
    java.util.Optional<SiteLocaleInfo> localeByUrlPrefix(String urlPrefix);

    /** 对外公开的语言代码，按 sortOrder（hreflang、sitemap、语言切换器、IndexNow）。 */
    java.util.List<String> publicLocales();

    /** 启用自动翻译的目标语言代码（不含源语言）。 */
    java.util.List<String> autoTranslateLocales();

    /** 该语言是否已启用且对外公开（前台可访问）。 */
    boolean isPublicLocale(String locale);

    /** 模块在当前站点是否启用：核心模块恒为 true；不在构建中（MISSING）或未安装的模块为 false。 */
    boolean isModuleEnabled(String moduleId);

    /** 模块在指定站点是否启用（系统级任务使用）。 */
    boolean isModuleEnabled(long siteId, String moduleId);

    /** 当前站点某已安装模块的停用路由策略；空配置按NOT_FOUND。供url/delivery读取，不访问sys_表。 */
    ModuleDisablePolicy moduleDisablePolicy(String moduleId);

    /** 当前站点启用的模块 ID。 */
    java.util.Set<String> enabledModules();

    /** 当前站点主域名的 origin，如 "https://www.example.com"（canonical、sitemap、邮件与 Webhook 中的链接）。 */
    String primaryOrigin();

    /** 后台的 origin，如 "https://admin.example.com"（通知邮件中的后台链接）。系统级配置。 */
    String adminOrigin();

    /** 构建中全部模块的平台级信息。 */
    java.util.List<ModuleInfo> modules();
}

public enum ModuleDisableAction { NOT_FOUND, REDIRECT }

public record ModuleDisablePolicy(ModuleDisableAction action,
                                  @org.jspecify.annotations.Nullable Long redirectContentId) {
    public ModuleDisablePolicy {
        java.util.Objects.requireNonNull(action);
        if (action == ModuleDisableAction.REDIRECT && (redirectContentId == null || redirectContentId <= 0)
            || action == ModuleDisableAction.NOT_FOUND && redirectContentId != null)
            throw new IllegalArgumentException("模块停用路由策略不合法");
    }
    public static ModuleDisablePolicy notFound() { return new ModuleDisablePolicy(ModuleDisableAction.NOT_FOUND, null); }
}

```

```java
package com.b2bweb.api.site;

/**
 * @param primaryDomain 主域名（小写，不含协议），如 "www.example.com"
 * @param version       站点乐观锁版本（域名、语言变化时同样递增），可用作缓存版本
 */
public record SiteInfo(long siteId, String code, String name, String themeKey, String sourceLocale,
                       String defaultLocale, UrlStrategy urlStrategy, TrailingSlash trailingSlash,
                       java.time.ZoneId timezone, SiteStatus status, String primaryDomain, int version) {}

public enum SiteStatus {
    /** 建设中：前台返回 503 + noindex；后台与预览可用 */
    SETUP,
    /** 对外服务 */
    ACTIVE,
    /** 已归档：前台不再解析其域名，后台只读 */
    ARCHIVED
}

/** URL 策略：V1 固定 SUBDIRECTORY（全部语言带前缀）；DOMAIN 为 V2 语言独立域名。 */
public enum UrlStrategy { SUBDIRECTORY, DOMAIN }

/** 尾斜杠策略（系统设计 13.2）。 */
public enum TrailingSlash { NEVER, ALWAYS }

/**
 * 站点语言。
 *
 * @param urlPrefix        URL 前缀（不含斜杠），全部语言都有前缀
 * @param hreflang         hreflang 值（默认只用语言代码：zh 或 zh-Hans、en、de）
 * @param publiclyVisible  对外公开（数据库列 public）：前台可访问，出现在语言切换器、hreflang、sitemap 中
 * @param autoTranslate    源语言发布、站点资源变化时自动翻译到该语言；源语言恒为 false
 * @param source           是否为源语言（zh-CN）
 */
public record SiteLocaleInfo(String locale, String urlPrefix, String hreflang, boolean publiclyVisible,
                             boolean autoTranslate, int sortOrder, boolean source) {}

/**
 * Host 解析结果。
 *
 * @param locale V2 语言独立域名（domainType = LOCALE）对应的语言；V1 为空
 */
public record HostResolution(long siteId, String host, DomainType domainType, String primaryDomain,
                             @org.jspecify.annotations.Nullable String locale, SiteStatus status) {
    /** 别名域名需要 301 到主域名 https（系统设计 13.2 第 1 步）。 */
    public boolean redirectToPrimary() { return domainType == DomainType.ALIAS; }
}

public enum DomainType { PRIMARY, ALIAS, LOCALE }

/**
 * 模块的平台级信息。
 *
 * @param layer   "L0" / "L2" / "L3" / "L4"
 * @param version 已安装版本；未安装成功时为构建中的版本
 */
public record ModuleInfo(String moduleId, String name, String layer, String version, boolean disableable,
                         ModuleStatus status, @org.jspecify.annotations.Nullable String apiPrefix,
                         @org.jspecify.annotations.Nullable String tablePrefix) {}

/** 模块的平台级状态（系统设计 3.6）。 */
public enum ModuleStatus { INSTALLING, INSTALLED, UPGRADING, FAILED, MISSING, PURGED }
```

#### 4.4.2 SiteContextProvider：站点上下文

读取当前线程的站点上下文（`SiteContext`，4.3.1）；在系统级代码中切换到指定站点执行。上下文的建立与传递（请求过滤器、监听器包装器、任务执行器、`TaskDecorator`）由 platform-core 实现（第 5 章）。

- 提供方：core。使用方：全部模块。

```java
package com.b2bweb.api.site;

public interface SiteContextProvider {

    /** 当前站点上下文；没有时为空（如应用启动、系统级任务）。 */
    java.util.Optional<SiteContext> current();

    /** 当前站点上下文；没有时抛出 NoSiteContextException。 */
    SiteContext require();

    /** 当前站点 ID；没有时抛出 NoSiteContextException。 */
    long requireSiteId();

    /**
     * 以指定站点为上下文执行 action，结束后恢复原上下文（origin = SYSTEM）。
     * 只允许系统级代码使用：系统级定时任务逐站点处理、模块安装时为各站点初始化数据、数据主体请求的逐站点处理。
     * 站点不存在时抛出 NotFoundException（CORE_SITE_NOT_FOUND）。
     */
    <T> T callAs(long siteId, java.util.function.Supplier<T> action);

    /** 同 callAs，无返回值。 */
    void runAs(long siteId, Runnable action);
}
```

#### 4.4.3 SettingsApi：设置

读写模块设置。设置键为 `{moduleId}.{名称}`，Schema 来自声明模块的 `settings.schema.json`（`module.json` 的 `settings`，系统设计 3.5）；没有保存值时返回 Schema 中的默认值。站点管理员通过 core 的 Schema 驱动设置页面修改设置；修改后 core 发布 `SiteSettingsChangedEvent`。

- 提供方：core。使用方：全部模块。
- 模块只能读取任意设置键，但**只能写入自己声明的设置键**（PR 评审）。
- 敏感字段（Schema 中标注 `x-secret`）在读取结果中是密文信封，需要明文时经 `SecretCipher.decrypt` 解密；接口返回给后台时由 core 掩码。

```java
package com.b2bweb.api.site;

public interface SettingsApi {

    /** 当前站点的设置值，映射为 type（通常是声明模块中的 record）。未声明的键抛出 CORE_SETTING_NOT_DECLARED。 */
    <T> T get(String key, Class<T> type);

    /** 当前站点的设置值（JSON，顶层带 schemaVersion）。 */
    tools.jackson.databind.JsonNode getJson(String key);

    /** 指定站点的设置值（系统级任务使用）。 */
    <T> T get(long siteId, String key, Class<T> type);

    /** 系统级设置（site_id = 0），如 core.smtp、core.dsr。 */
    <T> T getSystem(String key, Class<T> type);

    /**
     * 写入当前站点的设置（按 Schema 校验，敏感字段自动加密）。
     * expectedVersion 为空表示不做乐观锁检查（只用于 onInstall 写入初始值）。
     *
     * @return 新的版本号
     * @throws com.b2bweb.api.common.ValidationException CORE_SETTING_INVALID
     * @throws com.b2bweb.api.common.VersionConflictException CORE_SETTING_VERSION_CONFLICT
     */
    int put(String key, tools.jackson.databind.JsonNode value, @org.jspecify.annotations.Nullable Integer expectedVersion);

    /** 写入系统级设置；规则同 put。 */
    int putSystem(String key, tools.jackson.databind.JsonNode value, @org.jspecify.annotations.Nullable Integer expectedVersion);

    /** 恢复默认（删除当前站点的保存值）。 */
    void reset(String key);

    /** 当前站点某设置的元信息。 */
    SettingMeta meta(String key);
}

/**
 * @param defaultValue 是否为 Schema 默认值（没有保存值）
 * @param version      保存值的版本号；默认值为 0
 */
public record SettingMeta(String key, String moduleId, boolean defaultValue, int version,
                          java.time.@org.jspecify.annotations.Nullable Instant updatedAt) {}
```

`CoreErrors` 中补充：`CORE_SETTING_VERSION_CONFLICT`（409，“设置已被他人修改”）。

#### 4.4.4 SiteProfileApi：站点资料

读取当前站点的站点资料（公司信息、Logo、联系方式、社交账号、法定信息，系统设计 16.3）。站点资料的编辑是 core 的后台功能；可翻译文本作为站点资源翻译（资源类型 `core.site_profile`）。

- 提供方：core。使用方：notification（邮件外层布局与签名）、seo（`{siteName}` 等模板变量）。delivery 的站点引导数据与 `Organization` JSON-LD 由 core 自己的 `SiteBootstrapContributor` / `SchemaOrgProvider` 提供，不经本接口。

```java
package com.b2bweb.api.site;

public interface SiteProfileApi {

    /**
     * 当前站点资料在指定语言的视图：可翻译文本取该语言的版本，该语言尚无译文的字段为空（不回退到其他语言）；
     * 法定信息按登记原文，各语言一致。
     */
    SiteProfileView get(String locale);
}

/**
 * @param companyName   对外公司名称（可翻译）
 * @param tagline       一句话介绍（可翻译）
 * @param openingHours  营业时间说明（可翻译）
 */
public record SiteProfileView(
        String locale,
        @org.jspecify.annotations.Nullable String companyName,
        @org.jspecify.annotations.Nullable String tagline,
        @org.jspecify.annotations.Nullable Long logoMediaId,
        @org.jspecify.annotations.Nullable Long logoInverseMediaId,
        @org.jspecify.annotations.Nullable Long faviconMediaId,
        @org.jspecify.annotations.Nullable Long appIconMediaId,
        @org.jspecify.annotations.Nullable String email,
        @org.jspecify.annotations.Nullable String phone,
        @org.jspecify.annotations.Nullable String whatsapp,
        java.util.List<Address> addresses,
        java.util.List<SocialLink> socialLinks,
        @org.jspecify.annotations.Nullable String openingHours,
        @org.jspecify.annotations.Nullable Integer foundedYear,
        LegalInfo legal) {

    public SiteProfileView {
        addresses = java.util.List.copyOf(addresses);
        socialLinks = java.util.List.copyOf(socialLinks);
    }

    /** @param type HEADQUARTERS / OFFICE / FACTORY / WAREHOUSE / SHOWROOM / OTHER；@param label 可翻译 */
    public record Address(String type, @org.jspecify.annotations.Nullable String label, String street,
                          @org.jspecify.annotations.Nullable String postalCode, String city,
                          @org.jspecify.annotations.Nullable String region, String countryCode,
                          @org.jspecify.annotations.Nullable String phone) {}

    /** @param platform LINKEDIN / FACEBOOK / YOUTUBE / X / INSTAGRAM / TIKTOK / WECHAT / OTHER */
    public record SocialLink(String platform, String url) {}

    /** 法定信息（Imprint，系统设计 16.3）；不翻译，内容由业主确认（32 章 Q14）。 */
    public record LegalInfo(@org.jspecify.annotations.Nullable String legalName,
                            @org.jspecify.annotations.Nullable String registeredAddress,
                            java.util.List<String> representatives,
                            @org.jspecify.annotations.Nullable String registerAuthority,
                            @org.jspecify.annotations.Nullable String registerNumber,
                            @org.jspecify.annotations.Nullable String vatId,
                            @org.jspecify.annotations.Nullable String supervisoryAuthority,
                            @org.jspecify.annotations.Nullable String contentResponsible,
                            @org.jspecify.annotations.Nullable String legalEmail,
                            @org.jspecify.annotations.Nullable String legalPhone) {
        public LegalInfo { representatives = java.util.List.copyOf(representatives); }
    }
}
```

> 字段与 `sys_site_profile`、`sys_site_profile_localization` 的列一一对应（第 6 章）；两者不一致时以第 6 章的表结构为准修订本 record。

#### 4.4.5 UiTextApi：前台界面文案

按文案键读取前台界面文案（系统设计 5.4、21.7）：查找顺序为站点覆盖 / 译文 → 主题默认文案（只有中文）。前台组件只写文案键；后端需要输出用户可见文字时（如 `BreadcrumbList` 中“首页”的名称、客户邮件中的固定文字）同样经本接口取文案。

- 提供方：core。使用方：delivery、seo、notification。

```java
package com.b2bweb.api.site;

public interface UiTextApi {

    /**
     * 文案键在指定语言的文本。zh-CN：站点覆盖 → 主题默认；其他语言：该语言的译文，没有译文时为空（不回退到中文）。
     */
    java.util.Optional<String> text(String key, String locale);

    /** 批量读取；结果只含有文本的键。 */
    java.util.Map<String, String> texts(java.util.Collection<String> keys, String locale);

    /** 某语言的全部文案（站点引导数据使用）。Delivery 热路径。 */
    java.util.Map<String, String> allTexts(String locale);

    /** 读取并替换 {name} 形式的插值占位符；文案不存在时为空。 */
    java.util.Optional<String> format(String key, String locale, java.util.Map<String, String> args);
}
```

#### 4.4.6 CurrentUserProvider：当前用户

读取当前登录的后台用户与当前操作人。

- 提供方：core。使用方：全部模块。

```java
package com.b2bweb.api.security;

public interface CurrentUserProvider {

    /** 当前登录的后台用户；前台请求、事件监听、任务中为空。 */
    java.util.Optional<CurrentUser> current();

    /** 当前登录的后台用户；没有时抛出 CORE_NOT_AUTHENTICATED（401）。 */
    CurrentUser require();

    /** 当前操作人：有登录用户时为该用户；否则为 ActorRef.SYSTEM（任务执行器以任务发起人身份执行时返回发起人）。 */
    com.b2bweb.api.common.ActorRef actor();
}

/**
 * @param systemAdmin 系统管理员（拥有所有站点的权限）
 * @param roleCodes   在当前站点担任的角色代码
 * @param mfaVerified 本次会话是否已通过 2FA
 */
public record CurrentUser(long userId, String name, String email, boolean systemAdmin,
                          java.util.Set<String> roleCodes, boolean mfaVerified) {
    public CurrentUser { roleCodes = java.util.Set.copyOf(roleCodes); }

    public com.b2bweb.api.common.ActorRef toActor() { return com.b2bweb.api.common.ActorRef.user(userId, name); }
}
```

#### 4.4.7 PermissionChecker：权限、数据范围与 @RequiresPermission

在应用服务中检查当前用户在当前站点的权限、数据范围与语言范围（系统设计 26.2）。权限码格式 `{module}:{resource}:{action}`，由各模块在 `module.json` 中声明。

- 提供方：core（`@RequiresPermission` 的切面也由 core 实现）。使用方：全部模块。

```java
package com.b2bweb.api.security;

public interface PermissionChecker {

    /** 当前用户在当前站点是否拥有该权限；系统管理员恒为 true；没有登录用户时为 false。 */
    boolean has(String permission);

    /** 拥有任一权限。 */
    boolean hasAny(String... permissions);

    /** 没有权限时抛出 ForbiddenException（CORE_FORBIDDEN）。 */
    void check(String permission);

    /**
     * 当前用户对该权限的数据范围：多个角色取最大（ALL > GROUP > OWN）；
     * 权限不区分数据范围时返回 ALL；没有该权限时为空。
     */
    java.util.Optional<DataScope> dataScope(String permission);

    /** 语言范围：权限声明了 localeScoped 时按用户角色的语言范围返回；否则返回 LocaleScope.all()。 */
    LocaleScope localeScope(String permission);

    /** 当前用户是否可以对该语言执行该权限（翻译审校人员限定语言，系统设计 5.8）。 */
    boolean canAccessLocale(String permission, String locale);

    /** 指定用户在当前站点是否拥有该权限（筛选通知收件人等）。 */
    boolean userHas(long userId, String permission);
}

/** 数据范围（系统设计 26.2）。GROUP 的成员关系由拥有数据的模块解释（如 inquiry 的销售组）。 */
public enum DataScope { ALL, GROUP, OWN }

/** 语言范围：unrestricted 为 true 时不限语言。 */
public record LocaleScope(boolean unrestricted, java.util.Set<String> locales) {
    public LocaleScope { locales = java.util.Set.copyOf(locales); }

    public static LocaleScope all() { return new LocaleScope(true, java.util.Set.of()); }

    public static LocaleScope of(java.util.Set<String> locales) { return new LocaleScope(false, locales); }

    public boolean allows(String locale) { return unrestricted || locales.contains(locale); }
}

/**
 * 声明应用服务方法（或类）需要的权限，由 core 的切面在方法执行前校验（系统设计 26.2）。
 * 前端隐藏按钮只是体验优化，不能代替本注解。
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface RequiresPermission {

    /** 权限码，如 "inquiry:inquiry:export"。 */
    String[] value();

    /** 多个权限码的关系：ALL（全部拥有）或 ANY（任一）。 */
    Logical logical() default Logical.ALL;

    /** 是否需要近期二次验证（修改密码、导出数据、修改脚本等敏感操作，系统设计 26.1）。 */
    boolean reauthenticate() default false;

    enum Logical { ALL, ANY }
}

/** 权限码工具。 */
public final class Permissions {
    private Permissions() {}

    /** 内容生命周期动作（系统设计 6.3）。 */
    public enum ContentAction { EDIT, REVIEW, PUBLISH, DELETE }

    /**
     * 内容生命周期权限码：content:{type小写且下划线转连字符}:{action 小写}，
     * 如 forContent("PRODUCT", PUBLISH) = "content:product:publish"，forContent("CASE_STUDY", EDIT) = "content:case-study:edit"。
     */
    public static String forContent(String contentType, ContentAction action) {
        if (!ContentTypes.isValidFormat(contentType)) throw new IllegalArgumentException("内容类型格式无效");
        return "content:" + contentType.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ":" + action.name().toLowerCase(java.util.Locale.ROOT);
    }
}
```

#### 4.4.8 UserDirectory：用户目录

查询后台用户的基本信息（姓名、邮箱、状态），用于通知收件人、负责人显示与分配。用户与角色的管理是 core 的后台功能。

- 提供方：core。使用方：content（审核人）、inquiry（负责人、销售组成员）、notification（员工通知收件人）、translation（翻译审校人员）。

```java
package com.b2bweb.api.security;

public interface UserDirectory {

    /** 按 ID 查询用户（含已停用；已软删除的用户 name 为匿名化后的值）。 */
    java.util.Optional<UserSummary> find(long userId);

    /** 批量查询；不存在的 ID 不出现在结果中。 */
    java.util.Map<Long, UserSummary> findAll(java.util.Collection<Long> userIds);

    /**
     * 当前站点内拥有某权限的启用用户（含系统管理员）。
     * locale 非空时，对 localeScoped 权限只返回语言范围包含该语言的用户。
     */
    java.util.List<UserSummary> usersWithPermission(String permission, @org.jspecify.annotations.Nullable String locale);

    /** 用户是否启用且在当前站点有角色（分配询盘、选择负责人前校验）。 */
    boolean isActiveMember(long userId);
}

/** @param active 用户状态为 ACTIVE 且未删除 */
public record UserSummary(long userId, String name, String email, boolean active) {}
```

#### 4.4.9 AuditApi：审计

写入审计日志（系统设计 26.5）：所有后台写操作、登录登出与失败、角色与权限变更、数据导出、脚本修改、模块启停、设置修改。操作人、IP、User-Agent、traceId、站点由 core 从上下文填充。

- 提供方：core。使用方：全部模块。

```java
package com.b2bweb.api.security;

public interface AuditApi {

    /** 写入一条审计记录，加入调用方事务（业务回滚则审计一并回滚）。 */
    void record(AuditEntry entry);

    /** 在独立事务中写入（REQUIRES_NEW），用于失败的操作：登录失败、被拒绝的导出、权限不足等。 */
    void recordIndependently(AuditEntry entry);
}

/**
 * @param moduleId    模块 ID
 * @param action      动作 {资源}.{动作}，如 "localization.publish"、"inquiry.export"
 * @param targetType  目标类型，如 "PRODUCT"、"setting"、"sys_user"
 * @param targetId    目标 ID（TSID 十进制字符串、设置键、模块 ID 等）
 * @param summary     中文摘要
 * @param diff        字段变更；密码、密钥、恢复码等必须用 AuditChange.masked() 脱敏
 * @param systemLevel 系统级操作（站点、模块、数据主体请求等），写入时 site_id 为空
 */
public record AuditEntry(String moduleId, String action,
                         @org.jspecify.annotations.Nullable String targetType,
                         @org.jspecify.annotations.Nullable String targetId,
                         String summary,
                         java.util.Map<String, AuditChange> diff,
                         AuditResult result,
                         boolean sensitive,
                         boolean systemLevel) {
    public AuditEntry { diff = java.util.Map.copyOf(diff); }

    public static AuditEntry success(String moduleId, String action, String targetType, String targetId, String summary) {
        return new AuditEntry(moduleId, action, targetType, targetId, summary, java.util.Map.of(), AuditResult.SUCCESS, false, false);
    }
}

/** 字段变更；值必须可以序列化为 JSON。 */
public record AuditChange(@org.jspecify.annotations.Nullable Object from, @org.jspecify.annotations.Nullable Object to) {
    public static AuditChange masked() { return new AuditChange("****", "****"); }
}

public enum AuditResult { SUCCESS, FAILURE }
```

#### 4.4.10 SecretCipher：密文加解密

用统一的密钥与格式加密敏感值（系统设计 26.6）：SMTP 密码、第三方 API Key、AI 引擎 API Key（`trn_engine`）、Webhook 签名密钥、Turnstile 密钥等。密文信封格式 `v1:{keyId}:{Base64(nonce‖密文‖tag)}`（AES-256-GCM，12 字节随机 nonce；密钥来自环境变量或密钥管理服务，`keyId` 用于轮换）。模块不得自行实现加密。

- 提供方：core。使用方：translation、notification、form、tracking、infrastructure。

```java
package com.b2bweb.api.security;

public interface SecretCipher {

    /** 用当前密钥加密为密文信封。 */
    String encrypt(com.b2bweb.api.common.Secret plaintext);

    /** 解密信封；格式错误、密钥不存在或认证失败时抛出 CORE_SECRET_DECRYPT_FAILED。 */
    com.b2bweb.api.common.Secret decrypt(String envelope);

    /** 是否为本平台的密文信封。 */
    boolean isEnvelope(String value);

    /** 信封是否使用了非当前密钥（密钥轮换任务据此重新加密）。 */
    boolean needsReEncryption(String envelope);

    /** 掩码显示：只保留末 4 位，如 "****abcd"；长度 ≤ 8 时全部掩码。 */
    String mask(com.b2bweb.api.common.Secret plaintext);
}
```

#### 4.4.11 IdGenerator：ID 生成

生成 64 位 TSID（ADR-014）。多实例部署时由 core 为每个实例分配节点号，保证不冲突。

- 提供方：core。使用方：全部模块（实体主键、事件 ID、存储键）。

```java
package com.b2bweb.api.common;

public interface IdGenerator {

    /** 下一个 TSID（时间有序，正数）。 */
    long nextId();

    /** 十进制字符串形式（事件 ID 等）。 */
    default String nextIdString() { return Long.toString(nextId()); }

    /** 从 ID 推断创建时间：只用于排查，不得用作业务排序或权限判断。 */
    java.time.Instant timestampOf(long id);
}
```

#### 4.4.12 JobApi、JobHandler、ScheduledTask：异步任务与定时任务

Core 提供的持久化异步任务框架（系统设计 2.4，表 `sys_job`）：产品导入导出、重定向导入、询盘导出、数据主体请求导出、保留期清理等。模块提交任务，并以 `JobHandler` 注册执行逻辑；执行器在任务所属站点的上下文中、以发起人身份执行。导入采用“校验预览 → 用户确认 → 执行”两个任务（`parentJobId` 关联）。AI 翻译任务使用 translation 自己的 `trn_job`，不使用本框架。

定时任务以 `ScheduledTask` 注册：core 用 Spring Scheduling + ShedLock 调度；`PER_SITE` 任务逐个站点执行，并跳过停用了所属模块的站点（系统设计 3.8“定时任务不执行”）。

- 提供方：core。使用方：product、url、inquiry、graph、seo、search、media、form、tracking、translation 等。

```java
package com.b2bweb.api.common;

public interface JobApi {

    /**
     * 提交任务（当前站点；系统级任务在没有站点上下文时提交，site_id = 0）。
     * idempotencyKey 非空时，同一站点、同一 jobType、同一 key 在任务保留期内只创建一次，重复提交返回已有任务。
     * 加入调用方事务：事务回滚则任务不存在；事务提交后执行器才可见。
     *
     * @throws com.b2bweb.api.common.BusinessException CORE_JOB_TYPE_UNKNOWN（没有对应的 JobHandler）
     */
    JobInfo submit(JobRequest request);

    /** 查询任务（当前站点）。 */
    java.util.Optional<JobInfo> find(long jobId);

    /** 当前站点的任务列表；排序字段白名单：createdAt（默认倒序）、finishedAt。 */
    PageResult<JobInfo> list(JobQuery query, PageRequest page);

    /** 请求取消：PENDING 直接取消；RUNNING 设置取消标记，由执行逻辑在检查点退出。其他状态抛出 CORE_JOB_STATE_INVALID。 */
    void requestCancel(long jobId);

    /** 重试失败的任务（FAILED → PENDING，attempts 不清零）。其他状态抛出 CORE_JOB_STATE_INVALID。 */
    JobInfo retry(long jobId);

    /**
     * 保存任务的输入文件（如导入用的 xlsx、图片 zip）到私有存储，7 天后自动删除。
     * 返回的 fileKey 放入 JobRequest.params，执行时用 JobExecutionContext.openInputFile 读取。
     */
    StoredFile storeInputFile(String fileName, String contentType, long size, java.io.InputStream content);

    /** 任务结果文件的短期签名下载链接；没有结果文件或已过期时抛出 CORE_JOB_NOT_FOUND。 */
    java.net.URI resultDownloadUrl(long jobId, java.time.Duration ttl);
}

/**
 * @param jobType     任务类型 {moduleId}.{名称}，如 "product.import"；必须有对应的 JobHandler
 * @param title       任务名称（中文，任务中心列表显示）
 * @param params      任务参数（JSON，不得包含明文密钥）
 * @param scheduledAt 最早执行时间；为空表示立即
 * @param maxAttempts 最多执行次数（默认 1，即不自动重试）
 */
public record JobRequest(String moduleId, String jobType, String title,
                         tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode params,
                         @org.jspecify.annotations.Nullable String idempotencyKey,
                         @org.jspecify.annotations.Nullable Long parentJobId,
                         java.time.@org.jspecify.annotations.Nullable Instant scheduledAt,
                         int maxAttempts) {
    public static JobRequest of(String moduleId, String jobType, String title, tools.jackson.databind.JsonNode params) {
        return new JobRequest(moduleId, jobType, title, params, null, null, null, 1);
    }
}

public enum JobStatus { PENDING, RUNNING, SUCCEEDED, FAILED, CANCELED }

public record JobProgress(@org.jspecify.annotations.Nullable Integer total, int done, int failed,
                          @org.jspecify.annotations.Nullable String message) {}

/** @param expiresAt 结果文件过期时间（默认 7 天） */
public record JobFile(String fileName, long size, java.time.Instant expiresAt) {}

public record JobInfo(long jobId, String moduleId, String jobType, String title, JobStatus status,
                      @org.jspecify.annotations.Nullable Long parentJobId,
                      JobProgress progress,
                      tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode result,
                      @org.jspecify.annotations.Nullable JobFile resultFile,
                      @org.jspecify.annotations.Nullable String errorCode,
                      @org.jspecify.annotations.Nullable String errorMessage,
                      int attempts, int maxAttempts, boolean cancelRequested,
                      long createdBy, java.time.Instant createdAt,
                      java.time.@org.jspecify.annotations.Nullable Instant startedAt,
                      java.time.@org.jspecify.annotations.Nullable Instant finishedAt) {}

public record JobQuery(@org.jspecify.annotations.Nullable String moduleId,
                       @org.jspecify.annotations.Nullable String jobType,
                       java.util.Set<JobStatus> statuses,
                       @org.jspecify.annotations.Nullable Long createdBy) {
    public JobQuery { statuses = java.util.Set.copyOf(statuses); }
}

/** @param fileKey 私有存储中的对象键（不透明） */
public record StoredFile(String fileKey, String fileName, String contentType, long size) {}
```

```java
package com.b2bweb.api.common;

/**
 * 任务执行逻辑。用 @Extension(module = "…") 注册；key() 即 jobType。
 * 执行器在任务所属站点的上下文中、以发起人身份调用；不在事务中调用，实现方按需开启短事务（如每个产品一个事务，系统设计 8.6）。
 * 抛出 PlatformException 时任务 FAILED，errorCode / errorMessage 取自异常；其他异常记为 CORE_JOB_FAILED。
 */
public interface JobHandler extends com.b2bweb.api.extension.KeyedExtension {

    /** 任务类型 {moduleId}.{名称}。 */
    String jobType();

    @Override
    default String key() { return jobType(); }

    JobOutcome execute(JobExecutionContext ctx) throws Exception;

    /** 心跳超时：超过该时间没有 progress / checkpoint 调用，视为实例失联。 */
    default java.time.Duration heartbeatTimeout() { return java.time.Duration.ofMinutes(5); }
}

public interface JobExecutionContext {

    long jobId();

    /** 任务所属站点；系统级任务为 0。 */
    long siteId();

    /** 发起人。 */
    ActorRef initiator();

    /** 第几次执行（从 1 开始）。 */
    int attempt();

    tools.jackson.databind.JsonNode params();

    <T> T params(Class<T> type);

    /** 上报进度（同时作为心跳）；节流写库（每秒最多一次）。 */
    void progress(@org.jspecify.annotations.Nullable Integer total, int done, int failed,
                  @org.jspecify.annotations.Nullable String message);

    /** 检查点：已请求取消时抛出 JobCanceledException（任务置为 CANCELED），否则刷新心跳。 */
    void checkpoint();

    boolean isCancelRequested();

    /** 读取 JobApi.storeInputFile 保存的输入文件；调用方负责关闭流。 */
    java.io.InputStream openInputFile(String fileKey);

    /** 写入结果文件（如错误报告 xlsx、导出文件）；同一任务只保留最后一个结果文件。 */
    void writeResultFile(String fileName, String contentType,
                         ThrowingConsumer<java.io.OutputStream> writer) throws java.io.IOException;

    @FunctionalInterface
    interface ThrowingConsumer<T> { void accept(T t) throws java.io.IOException; }
}

/** 执行结果；result 为结果摘要（如 {"created":120,"updated":30,"errors":2}）。 */
public record JobOutcome(tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode result) {
    public static JobOutcome empty() { return new JobOutcome(null); }
}

/** 协作取消时由 checkpoint() 抛出。 */
public class JobCanceledException extends RuntimeException {
    public JobCanceledException() { super("任务已取消"); }
}
```

```java
package com.b2bweb.api.common;

/**
 * 定时任务。用 @Extension(module = "…") 注册；key() 即 name()，也是 ShedLock 锁名。
 * 多实例部署时同一时刻只有一个实例执行（ShedLock，表 sys_lock）。
 */
public interface ScheduledTask extends com.b2bweb.api.extension.KeyedExtension {

    /** 任务名 {moduleId}.{名称}，全局唯一，如 "content.publish_due"、"graph.nightly_rebuild"。 */
    String name();

    @Override
    default String key() { return name(); }

    /** Spring cron 表达式（6 段，UTC），如 "0 * * * * *" 表示每分钟。 */
    String cron();

    /** SYSTEM：执行一次，没有站点上下文；PER_SITE：对每个未归档且启用了所属模块的站点各执行一次。 */
    ScheduleScope scope();

    /** 锁的最长持有时间（超过后视为实例失联，其他实例可以接手）。 */
    default java.time.Duration lockAtMostFor() { return java.time.Duration.ofMinutes(10); }

    void run(ScheduledTaskContext ctx);
}

public enum ScheduleScope { SYSTEM, PER_SITE }

/** @param siteId PER_SITE 时为当前站点；SYSTEM 时为空 */
public record ScheduledTaskContext(@org.jspecify.annotations.Nullable Long siteId,
                                   java.time.Instant scheduledAt, java.time.Instant startedAt) {}
```

`CoreErrors` 中补充：`CORE_JOB_FAILED`（500，“任务执行失败”）。

#### 4.4.13 DictApi：字典

读取后台字典（全局、只用于后台、中文标签，如询盘关闭原因、跟进方式；系统设计 25.8）。业务表只保存字典值（代码），展示时经本接口取标签。前台可见的选项不使用字典（走文案键或站点资源）。

- 提供方：core。使用方：inquiry 等使用后台字典的模块。

```java
package com.b2bweb.api.common;

public interface DictApi {

    /** 启用的字典项，按 sortOrder。类型不存在时抛出 CORE_DICT_TYPE_NOT_FOUND。 */
    java.util.List<DictItem> items(String typeCode);

    /** 全部字典项（含停用，历史数据显示用）。 */
    java.util.List<DictItem> allItems(String typeCode);

    /** 字典值的标签（含停用项）；不存在时为空。 */
    java.util.Optional<String> label(String typeCode, String value);

    /** 字典值是否存在且启用（保存业务数据前校验）。 */
    boolean isValid(String typeCode, String value);

    /**
     * 幂等登记模块的内置字典类型与内置字典项（在 PlatformModule.onInstall / onUpgrade 中调用）：
     * 新增缺失的类型与项，更新内置项的标签与排序；不删除站点管理员添加的项。
     */
    void ensureBuiltin(DictTypeDefinition definition);
}

/** @param extra 扩展属性（如标签颜色） */
public record DictItem(String value, String label, int sortOrder, boolean enabled, boolean builtin,
                       tools.jackson.databind.@org.jspecify.annotations.Nullable JsonNode extra) {}

/**
 * @param typeCode      {moduleId}.{名称}，如 "inquiry.close_reason"
 * @param itemsEditable 是否允许在后台增删改字典项；false 表示代码枚举的只读镜像
 */
public record DictTypeDefinition(String typeCode, String moduleId, String name,
                                 @org.jspecify.annotations.Nullable String description,
                                 boolean itemsEditable, java.util.List<DictItemDefinition> items) {
    public DictTypeDefinition { items = java.util.List.copyOf(items); }

    public record DictItemDefinition(String value, String label, int sortOrder) {}
}
```

#### 4.4.14 EventMetaFactory：事件元数据

为领域事件生成公共字段（`eventId`、`occurredAt`、`siteId`、`actor`，系统设计 4.2），保证所有模块的事件元数据一致。`EventMeta` 与事件的定义见 4.6.1。

- 提供方：core。使用方：全部发布事件的模块。

```java
package com.b2bweb.api.event;

public interface EventMetaFactory {

    /** 当前站点（没有站点上下文时 siteId 为空）、当前操作人、新的事件 ID 与当前时间。 */
    EventMeta next();

    /** 显式指定站点与操作人（系统级事件、以 system 身份执行的处理）。 */
    EventMeta next(@org.jspecify.annotations.Nullable Long siteId, com.b2bweb.api.common.ActorRef actor);
}
```

#### 4.4.15 ExtensionRegistry：扩展点注册表

收集全部扩展点实现（标注 `@Extension` 的 Bean，4.5.1），**按当前站点的模块启用状态过滤**并排序后返回（系统设计 3.7、3.8）。注册表在启动流程第 7 步构建（系统设计 3.6），之后只读。

- 提供方：core。使用方：所有收集调用扩展点的模块（见 4.2.3）。
- 排序：`@Extension.order` 升序；相同时按模块 ID、类名排序（结果确定）。
- 过滤：实现所属模块（`@Extension.module`）在当前站点未启用、不在构建中或未安装成功时不返回。
- `forContentType`：`@Extension.contentTypes` 为空（适用全部类型）或包含该类型，且实现了 `ContentTypeScoped` 时其 `supports(contentType)` 为 true。

```java
package com.b2bweb.api.extension;

public interface ExtensionRegistry {

    /** 当前站点可用的某扩展点的全部实现。没有站点上下文时抛出 NoSiteContextException。 */
    <T> java.util.List<T> all(Class<T> point);

    /** 当前站点可用、且适用于该内容类型的实现。 */
    <T> java.util.List<T> forContentType(Class<T> point, String contentType);

    /** 按 key 查找当前站点可用的实现（DataSourceProvider、JobHandler 等 KeyedExtension）。 */
    <T extends KeyedExtension> java.util.Optional<T> byKey(Class<T> point, String key);

    /** 按 key 查找；不存在或所属模块停用时抛出 NotFoundException（CORE_EXTENSION_KEY_UNKNOWN）。 */
    <T extends KeyedExtension> T requireByKey(Class<T> point, String key);

    /** 指定站点可用的实现（系统级任务逐站点处理时使用）。 */
    <T> java.util.List<T> allForSite(long siteId, Class<T> point);

    /** 指定站点可用、且适用于该内容类型的实现。 */
    <T> java.util.List<T> forContentTypeForSite(long siteId, Class<T> point, String contentType);

    /**
     * 不按站点过滤的全部实现：只用于平台级用途（启动时同步声明式资源、模块安装、诊断）。业务调用不得使用。
     */
    <T> java.util.List<T> allRegardlessOfSite(Class<T> point);

    /** 实现所属的模块 ID（取自 @Extension.module）。 */
    String moduleOf(Object extension);

    /** 某扩展点的全部实现的注册信息（后台诊断页、契约测试）。 */
    java.util.List<ExtensionInfo> describe(Class<?> point);
}

/**
 * @param key                 KeyedExtension 的 key；其他为空
 * @param enabledInCurrentSite 所属模块在当前站点是否启用（没有站点上下文时为 false）
 */
public record ExtensionInfo(String point, String implementationClass, String moduleId,
                            java.util.Set<String> contentTypes, int order,
                            @org.jspecify.annotations.Nullable String key,
                            boolean enabledInCurrentSite) {
    public ExtensionInfo { contentTypes = java.util.Set.copyOf(contentTypes); }
}
```

> 自 4.4.16 起，代码块使用简单类名（import 省略，见 4.1.1）：`List`、`Set`、`Map`、`Collection`、`Optional` 为 `java.util`；`Instant`、`Duration` 为 `java.time`；`URI` 为 `java.net`；`InputStream` 为 `java.io`；`JsonNode` 为 `tools.jackson.databind.JsonNode`；`@Nullable` 为 `org.jspecify.annotations.Nullable`；约束注解为 `jakarta.validation.constraints`；其他类型均为本章已定义的类型（包见 4.2.1）。

#### 4.4.16 ContentApi：内容内核

内容的身份、本地化、草稿元数据、生命周期（审核、发布、定时、下线、回滚、回收站）、发布快照与投影的读取、预览，以及翻译所需的本地化操作（系统设计 4.4、5.5–5.7、6.1–6.7）。业务字段（草稿态）由各业务模块保存在自己的表中，以 `content_id` 关联；内容内核只保存身份、状态、修订与投影。

- 提供方：content。使用方：product、article、page、taxonomy（创建与维护业务内容、TERM_PAGE）；translation（目标语言本地化与翻译状态）；delivery、seo、graph、search、navigation、inquiry（读取快照与投影；media资格按4.10.1取消）。
- **生命周期写方法的权限**：存在当前用户时按 `Permissions.forContent(type, action)` 校验（4.1.3），无权限抛出 `ForbiddenException`（`CORE_FORBIDDEN`）；以 `system` 身份执行（定时发布、译文自动发布、任务）时跳过。
- **乐观锁**：本地化的 `version` 是该本地化全部草稿数据（内核与各业务模块）的共同版本：任何模块保存某本地化的草稿时都经 `updateDraftMeta` 递增它；翻译和交互保存必须携带非空expectedVersion；只有已持有内容父锁、没有外部计算窗口的受控系统导入可用空值。所有保存先lockDraftForWrite，再写业务表，最后updateDraftMeta。
- **草稿哈希**：`updateDraftMeta`、`markDraftChanged` 之后，内容内核以 `AssemblyPurpose.DRAFT_HASH` 调用全部 `PublishContributor.contribute`（4.5.3）组装快照并计算 `draft_hash`，与 `pub_hash` 比较得出“有未发布的修改”；因此草稿哈希与发布快照使用同一套代码，不会误报。
- **发布事务**（系统设计 4.4）：`publish` 在调用方事务中依次执行：状态 / 权限 / 乐观锁 → `RouteHints`（4.5.2）→ `UrlApi.reservePath` → `PublishContributor.validate` → `contribute` 组装快照 → 写修订、投影、`cnt_published_term` → `UrlApi.applyRoute` → 源语言发布时把 `MANUAL` 目标语言标记为 `OUTDATED` → 发布 `ContentPublishedEvent`。**不调用 AI 引擎**（CLAUDE.md 禁止 16），翻译由 translation 监听事件后异步执行。
- Delivery 热路径：`findPublishedSnapshot`、`findPublishedCards`、`publishedVersions`（4.1.3）。

```java
package com.b2bweb.api.content;

public interface ContentApi {

    // ───────────── 创建与草稿 ─────────────

    /**
     * 已有内容保存前调用：先锁cnt_content，再锁目标cnt_localization并核对expectedVersion。
     * 返回当前本地化；锁保持到调用方事务结束。必须先于所属模块业务表写入/贡献方apply。
     * MANDATORY，使用当前读；跨站/不存在拒绝。多内容操作按contentId排序；路由事务锁在本方法之前。
     */
    LocalizationInfo lockDraftForWrite(long localizationId, int expectedVersion);


    /**
     * 创建内容及其源语言本地化（系统设计 6.1）。业务模块在保存自己业务表的同一事务中调用。
     * <p>contentType 必须已由 ContentTypeProvider 注册且所属模块在当前站点启用；locale 必须为源语言 zh-CN。
     * slug 为空时：可路由类型由 title 经 UrlApi.generateSlug 生成（发布前可修改）；不可路由类型为空。
     * 内容的 owner_module 取自 ContentTypeDefinition.module。发布 ContentCreatedEvent、LocalizationCreatedEvent。
     *
     * @throws BusinessException   CONTENT_TYPE_UNKNOWN、CONTENT_LOCALE_NOT_SOURCE
     * @throws ValidationException CONTENT_SLUG_INVALID
     */
    ContentCreated create(@Valid CreateContentCommand command);

    /**
     * 为已有内容创建 MANUAL 模式的目标语言本地化（为某个市场独立编辑，系统设计 5.5）。
     * AUTO 模式的本地化只由 translation 经 ensureTargetLocalization 创建。发布 LocalizationCreatedEvent。
     *
     * @throws BusinessException CONTENT_LOCALE_EXISTS、CONTENT_LOCALE_NOT_ENABLED（站点未启用该语言）
     */
    LocalizationInfo createLocalization(@Valid CreateLocalizationCommand command);

    /**
     * 保存草稿后同步元数据：递增本地化 version，draft_state 置为 EDITING（IN_REVIEW / APPROVED 时退回 EDITING，系统设计 6.3），
     * 更新 draft_title / slug，重算 draft_hash；sharedDataChanged 为 true 时同时重算同一内容其他本地化的 draft_hash
     * （语言无关数据变化，如型号、图集；其他本地化的 version 同样递增）。发布 ContentDraftChangedEvent。
     *
     * @throws VersionConflictException CONTENT_VERSION_CONFLICT（properties.currentVersion）
     * @throws BusinessException        CONTENT_TRASHED
     * @throws ValidationException      CONTENT_SLUG_INVALID
     */
    DraftMetaUpdated updateDraftMeta(@Valid UpdateDraftMetaCommand command);

    /**
     * 语言无关的草稿数据被非编辑器途径修改后（分类指派、导入、批量操作），重算该内容全部本地化的 draft_hash 并递增 version。
     * 不做乐观锁检查；为每个本地化发布 ContentDraftChangedEvent。
     */
    void markDraftChanged(long contentId);

    // ───────────── 生命周期（系统设计 6.3） ─────────────

    /** 提交审核：EDITING → IN_REVIEW；站点对该类型未启用审核时抛出 CONTENT_REVIEW_NOT_ENABLED。发布 ReviewRequestedEvent。 */
    void submitForReview(long localizationId, int expectedVersion, @Nullable String comment);

    /** 审核通过：IN_REVIEW → APPROVED。发布 ReviewCompletedEvent。权限 review。 */
    void approve(long localizationId, int expectedVersion, @Nullable String comment);

    /** 驳回：IN_REVIEW → EDITING，comment 必填。发布 ReviewCompletedEvent。权限 review。 */
    void reject(long localizationId, int expectedVersion, String comment);

    /**
     * 发布（流程见本节开头）。前置条件：未进回收站；不需要审核或已 APPROVED（system 身份的译文自动发布按 6.7 的站点配置）。
     * WARNING / INFO 级问题不阻止发布，随结果返回。
     *
     * @throws ValidationFailedException CONTENT_PUBLISH_VALIDATION_FAILED（存在 ERROR 级问题）
     * @throws BusinessException         CONTENT_SLUG_CONFLICT（properties：conflictContentId、suggestedSlug）、
     *                                   CONTENT_REVIEW_REQUIRED、CONTENT_SNAPSHOT_TOO_LARGE、CONTENT_TRASHED
     * @throws VersionConflictException  CONTENT_VERSION_CONFLICT
     */
    PublishResult publish(@Valid PublishCommand command);

    /** 设置定时发布和 / 或定时下线（时间为 UTC，须晚于当前时间）；前置条件同 publish（定时发布到点时再执行一次完整校验）。 */
    LocalizationInfo schedule(@Valid ScheduleCommand command);

    /** 取消定时发布与定时下线。 */
    LocalizationInfo cancelSchedule(long localizationId, int expectedVersion);

    /**
     * 下线：PUBLISHED → UNPUBLISHED，经 UrlApi.retireRoute 按下线策略处理路由（系统设计 13.6）。
     * 源语言下线时，AUTO 模式的目标语言本地化在同一事务中跟随下线（策略相同）。发布 ContentUnpublishedEvent（每个本地化一条）。
     *
     * @throws BusinessException CONTENT_STATE_INVALID（未发布）、CONTENT_REDIRECT_TARGET_INVALID（REDIRECT_CONTENT 的目标不存在或是自身）
     */
    void unpublish(@Valid UnpublishCommand command);

    /** 下线内容的全部已发布本地化（删除可路由分类项、产品停产下线等），以 system 或当前用户身份执行；返回下线的本地化数量。 */
    int unpublishAll(long contentId, UnpublishStrategy strategy, @Nullable Long redirectContentId);

    /** 回滚：调用各 PublishContributor.restore 把历史修订恢复为草稿（需要再次发布）；递增 version。 */
    void rollback(long localizationId, int revisionNo, int expectedVersion);

    /**
     * 移入回收站（内容级）。前置条件：没有已发布的本地化（源语言下线后 AUTO 跟随下线；仍有已发布的 MANUAL 本地化时抛出
     * CONTENT_HAS_PUBLISHED_LOCALIZATION）。发布 ContentTrashedEvent。
     */
    void trash(long contentId);

    /** 从回收站恢复。发布 ContentRestoredEvent。 */
    void restore(long contentId);

    /**
     * 彻底删除（物理删除内核数据；回收站中 ≥ 30 天，或拥有 delete 权限的用户显式确认）。
     * 先经 UrlApi.releaseRoutes 释放路由，再发布 ContentPurgedEvent，各模块据此清理自己的表（系统设计 25.5）。
     */
    void purge(long contentId);

    /** 删除一个未发布的非源语言本地化。发布 LocalizationDeletedEvent。 */
    void deleteLocalization(long localizationId);

    /** 发布前校验（编辑器侧栏展示）：执行发布流程的校验部分，不写库。 */
    List<ValidationIssue> validateForPublish(long localizationId);

    /** 影响分析：收集 ContentImpactProvider（4.5.4）。locale 为空表示内容级操作（TRASH、PURGE）。 */
    ImpactReport analyzeImpact(long contentId, @Nullable String locale, ImpactAction action);

    // ───────────── 翻译（translation 调用，系统设计 5.7） ─────────────

    /**
     * 幂等确保目标语言本地化存在：不存在时创建（translation_mode = AUTO、translation_state = NONE、slug 为空——
     * 首次发布时由译文标题生成，系统设计 13.3），并发布 LocalizationCreatedEvent；已存在时原样返回（含 MANUAL）。
     *
     * @throws BusinessException CONTENT_LOCALE_NOT_ENABLED、CONTENT_LOCALE_IS_SOURCE、CONTENT_TRASHED
     */
    LocalizationInfo ensureTargetLocalization(long contentId, String targetLocale);

    /**
     * 更新翻译状态与所依据的源语言修订号。sourceRevisionNo 小于已记录值时忽略本次更新（过期写入）并返回 false。
     * 只允许目标语言本地化；不改变 version、不触发草稿事件。
     */
    boolean updateTranslationState(@Valid TranslationStateUpdate update);

    /** 切换翻译模式（AUTO ⇄ MANUAL，“脱离 / 恢复自动翻译”）。发布 TranslationModeChangedEvent。 */
    LocalizationInfo changeTranslationMode(long localizationId, TranslationMode mode, @Nullable Integer expectedVersion);

    // ───────────── 读取：内容与本地化 ─────────────

    Optional<ContentInfo> findContent(long contentId);

    /** 批量查询（≤ 500 个 ID，4.1.3）。 */
    Map<Long, ContentInfo> findContents(Collection<Long> contentIds);

    Optional<LocalizationInfo> findLocalization(long localizationId);

    Optional<LocalizationInfo> findLocalization(long contentId, String locale);

    Map<Long, LocalizationInfo> findLocalizations(Collection<Long> localizationIds);

    /** 内容的全部本地化，源语言在前，其余按站点语言 sortOrder。 */
    List<LocalizationInfo> localizations(long contentId);

    /** 当前站点可用的内容类型（所属模块已启用）。 */
    List<ContentTypeDefinition> contentTypes();

    Optional<ContentTypeDefinition> contentType(String contentType);

    // ───────────── 读取：快照与投影（前台只读发布态，系统设计 6.4） ─────────────

    /** 本地化当前发布的快照；未发布、已下线或内容在回收站中时为空。Delivery 热路径。 */
    Optional<PublishedSnapshot> findPublishedSnapshot(long localizationId);

    /** 同上，按内容与语言查询；该语言没有已发布的本地化时为空（不跨语言回退）。 */
    Optional<PublishedSnapshot> findPublishedSnapshot(long contentId, String locale);

    /** 指定修订的快照（不论当前状态；回滚预览、翻译依据的源语言修订、审计）。 */
    Optional<PublishedSnapshot> findRevision(long localizationId, int revisionNo);

    /** 修订列表；排序白名单：revisionNo（默认倒序）。 */
    PageResult<RevisionInfo> revisions(long localizationId, PageRequest page);

    /** 内容在指定语言的已发布卡片（投影）；未发布的 ID 不出现在结果中。Delivery 热路径。 */
    Map<Long, PublishedCard> findPublishedCards(Collection<Long> contentIds, String locale);

    /** 内容在所有语言的已发布版本（hreflang、语言切换器；是否对外公开由调用方按 SiteApi 过滤）。Delivery 热路径。 */
    List<PublishedCard> publishedVersions(long contentId);

    /** 按条件查询已发布内容（content-query 数据源的同一语义；search 不可用时的降级列表）。limit ≤ 100。 */
    List<PublishedCard> queryPublished(@Valid PublishedQuery query);

    /** 全量扫描已发布本地化（sitemap、搜索重建、图谱全量计算、批量翻译）；按 localizationId 升序，游标稳定。 */
    CursorPage<PublishedCard> scanPublished(@Valid PublishedScan scan, CursorRequest cursor);

    /** 发布态的分类指派（cnt_published_term）。 */
    Map<Long, List<PublishedTermRef>> publishedTerms(Collection<Long> contentIds, String locale);

    /**
     * 与给定分类项至少共享一个发布态指派的内容 ID（图谱候选集，系统设计 15.3）：同站点、可路由、未进回收站；
     * 按共享数量降序，最多 limit（≤ 2000）个。
     */
    List<Long> contentIdsByPublishedTerms(Collection<Long> termIds, String locale, Set<String> contentTypes, int limit);

    /** 各内容类型在当前站点已发布的本地化数量（停用影响分析）。 */
    Map<String, Long> countPublished(Set<String> contentTypes);

    // ───────────── 预览（系统设计 6.5） ─────────────

    /** 兼容入口：只签发PREVIEW（≤1小时）或SHARE（>1小时），绝不推断EDITOR。 */
    default PreviewToken createPreviewToken(long localizationId, PreviewMode mode, @Nullable Duration ttl) {
        return createPreviewToken(localizationId, mode,
                ttl != null && ttl.compareTo(Duration.ofHours(1)) > 0 ? PreviewPurpose.SHARE : PreviewPurpose.PREVIEW, ttl);
    }

    /**
     * 显式用途签发。PREVIEW/SHARE/EDITOR最长分别1小时/7天/12小时，空ttl默认1小时/1小时/12小时。
     * EDITOR只允许Draft，绑定当前用户并校验站点成员及edit权限/语言范围；其余后台入口同样自行校验编辑权限。
     */
    PreviewToken createPreviewToken(long localizationId, PreviewMode mode, PreviewPurpose purpose, @Nullable Duration ttl);

    /** 校验签名/签发行、撤销、有效期、站点和内容可见性；EDITOR另复核签发人当前编辑权限/语言范围。无效时为空。 */
    Optional<PreviewTarget> resolvePreviewToken(String token);

    /**
     * 从草稿（或指定修订）临时组装快照，与发布同一套代码（AssemblyPurpose.PREVIEW），不持久化、不发布事件。
     * partOverrides仅用于验证过用途的EDITOR/Draft；V1只接受声明的layout并经对应贡献方/Schema校验，AUTO目标只读。
     */
    PreviewSnapshot assemblePreview(@Valid PreviewRequest request);

    /** M0新增草案，ADR-022；content任务读取当前树，事务内提交，见4.10.5。 */
    JobInfo requestRouteRecalculation(Set<String> locales);

    /** 内容级策略：锁cnt_content并比较其version，变化时使各本地化旧审批/版本失效；见4.10.7。 */
    ContentInfo setTranslationReviewRequirement(long contentId, int expectedContentVersion, boolean required);
}
```

```java
package com.b2bweb.api.content;

// ───── 命令 ─────

/** @param title 源语言标题（后台列表与 slug 生成） */
public record CreateContentCommand(@NotBlank String contentType, @NotBlank String locale,
                                   @NotBlank @Size(max = 255) String title,
                                   @Nullable @Size(max = 80) String slug) {}

public record ContentCreated(long contentId, long localizationId, String locale,
                             @Nullable String slug, int version) {}

public record CreateLocalizationCommand(long contentId, @NotBlank String locale,
                                        @NotBlank @Size(max = 255) String title,
                                        @Nullable @Size(max = 80) String slug) {}

/**
 * @param expectedVersion 翻译/交互保存必填；仅持有父锁的受控导入可空，禁止用null绕过翻译冲突
 * @param title             新的草稿标题；为空表示不变
 * @param slug              新的 slug；为空表示不变（已发布的本地化修改 slug 时，下次发布自动生成 301）
 * @param sharedDataChanged 语言无关的数据是否也变化了（见 updateDraftMeta）
 */
public record UpdateDraftMetaCommand(long localizationId, @Nullable Integer expectedVersion,
                                     @Nullable @Size(max = 255) String title,
                                     @Nullable @Size(max = 80) String slug,
                                     boolean sharedDataChanged) {}

/** @param hasUnpublishedChanges draft_hash ≠ pub_hash */
public record DraftMetaUpdated(long localizationId, int version, DraftState draftState,
                               @Nullable String slug, boolean hasUnpublishedChanges) {}

/** @param comment 修订说明（写入 cnt_revision.comment） */
public record PublishCommand(long localizationId, @Nullable Integer expectedVersion,
                             @Nullable @Size(max = 500) String comment) {}

/**
 * @param path         发布后的路径（含语言前缀）；不可路由类型为空
 * @param previousPath 上一次发布的路径；首次发布或路径未变时与 path 相同或为空
 * @param contentChanged payload_hash 是否变化（为 false 时 content_modified_at 不更新）
 * @param warnings     WARNING / INFO 级校验问题
 */
public record PublishResult(long localizationId, long revisionId, int revisionNo,
                            @Nullable String path, @Nullable String previousPath,
                            boolean firstPublish, boolean contentChanged,
                            List<ValidationIssue> warnings) {
    public PublishResult { warnings = List.copyOf(warnings); }
}

/** publishAt、unpublishAt 至少一个非空。 */
public record ScheduleCommand(long localizationId, int expectedVersion,
                              @Nullable Instant publishAt, @Nullable Instant unpublishAt,
                              @Nullable UnpublishStrategy unpublishStrategy,
                              @Nullable Long unpublishRedirectContentId) {}

/** @param redirectContentId strategy = REDIRECT_CONTENT 时必填：301 到该内容的同语言版本 */
public record UnpublishCommand(long localizationId, int expectedVersion, UnpublishStrategy strategy,
                               @Nullable Long redirectContentId) {}

/** @param sourceRevisionNo 本次翻译依据的源语言修订号；状态为 NONE 时可空 */
public record TranslationStateUpdate(long localizationId, TranslationState state,
                                     @Nullable Integer sourceRevisionNo) {}

// ───── 读取结果 ─────

/** @param primaryLocale 源语言（zh-CN） */
public record ContentInfo(long contentId, String contentType, String ownerModule, boolean routable,
                          String primaryLocale, @Nullable Instant trashedAt,
                          long createdBy, Instant createdAt, int version, boolean translationReviewRequired) {
    public boolean trashed() { return trashedAt != null; }
}

/**
 * 本地化（cnt_localization）。
 *
 * @param pubPath               当前发布路径（投影，与 url_route 同步）；未发布为空
 * @param sourceRevisionNo      生成译文时依据的源语言修订号（目标语言）
 * @param hasUnpublishedChanges draft_hash ≠ pub_hash
 */
public record LocalizationInfo(
        long localizationId, long contentId, String contentType, String locale,
        @Nullable String slug, String draftTitle, DraftState draftState, PublishState publishState,
        @Nullable Long publishedRevisionId, @Nullable Integer publishedRevisionNo,
        @Nullable Instant firstPublishedAt, @Nullable Instant publishedAt,
        @Nullable Instant scheduledPublishAt, @Nullable Instant scheduledUnpublishAt,
        UnpublishStrategy unpublishStrategy, @Nullable Long unpublishRedirectContentId,
        TranslationMode translationMode, TranslationState translationState,
        @Nullable String sourceLocale, @Nullable Integer sourceRevisionNo,
        @Nullable String pubPath, boolean hasUnpublishedChanges, int version, Instant updatedAt,
        boolean translationReviewRequired) {

    public boolean isSource() { return translationMode == TranslationMode.SOURCE; }
    public boolean isPublished() { return publishState == PublishState.PUBLISHED; }
}

/**
 * 发布快照（cnt_revision）。payload 结构见系统设计 6.4 与 4.5.3 的 SnapshotParts。
 *
 * @param path              当前发布路径；findRevision 返回的历史修订为发布时的路径
 * @param contentModifiedAt 内容实际变化的时间（sitemap lastmod）
 */
public record PublishedSnapshot(long contentId, long localizationId, String contentType, String locale,
                                long revisionId, int revisionNo, JsonNode payload, String payloadHash,
                                @Nullable String path, Instant publishedAt, Instant contentModifiedAt,
                                long publishedBy, PublishTrigger trigger) {}

/** 已发布卡片（投影：cnt_localization 的 pub_* 字段）。 */
public record PublishedCard(long contentId, long localizationId, String contentType, String locale,
                            String title, @Nullable String summary, @Nullable Long coverMediaId,
                            @Nullable String path, boolean routable,
                            Instant firstPublishedAt, Instant publishedAt, Instant contentModifiedAt) {}

/** 发布态分类指派（cnt_published_term）。 */
public record PublishedTermRef(long termId, String vocabularyKey, boolean primary, int sort) {}

public record RevisionInfo(long revisionId, int revisionNo, String payloadHash, @Nullable String comment,
                           long publishedBy, Instant publishedAt, PublishTrigger trigger, int payloadBytes) {}

// ───── 查询 ─────

/**
 * 已发布内容查询（基于投影，不读草稿）。
 *
 * @param anyTermIds 命中任一分类项（空表示不限）
 * @param contentIds 手选列表：非空时只返回这些内容，并按列表顺序（忽略 sort）
 * @param sort       白名单：publishedAt、firstPublishedAt、contentModifiedAt、title；默认 -publishedAt
 */
public record PublishedQuery(String locale, Set<String> contentTypes, Set<Long> anyTermIds,
                             List<Long> contentIds, Set<Long> excludeContentIds,
                             List<SortOrder> sort, @Positive @Max(100) int limit) {
    public PublishedQuery {
        contentTypes = Set.copyOf(contentTypes);
        anyTermIds = Set.copyOf(anyTermIds);
        contentIds = List.copyOf(contentIds);
        excludeContentIds = Set.copyOf(excludeContentIds);
        sort = List.copyOf(sort);
    }
}

/**
 * @param contentTypes   空表示全部类型
 * @param routableOnly   只返回可路由内容（sitemap）
 * @param modifiedSince  只返回 content_modified_at 晚于该时间的（增量同步）；为空表示全部
 */
public record PublishedScan(String locale, Set<String> contentTypes, boolean routableOnly,
                            @Nullable Instant modifiedSince) {
    public PublishedScan { contentTypes = Set.copyOf(contentTypes); }
}

// ───── 预览 ─────

/** 预览模式：草稿，或指定修订。 */
public sealed interface PreviewMode permits PreviewMode.Draft, PreviewMode.Revision {
    record Draft() implements PreviewMode {}
    record Revision(int revisionNo) implements PreviewMode {}
}

public record PreviewToken(String token, Instant expiresAt) {}

/** 用途与草稿/修订模式独立；EDITOR不由TTL或请求模式推断。 */
public enum PreviewPurpose { PREVIEW, SHARE, EDITOR }

public record PreviewTarget(long siteId, long contentId, long localizationId, String locale, PreviewMode mode,
                            Instant expiresAt, PreviewPurpose purpose, @Nullable Long issuedBy) {
    public PreviewTarget {
        Objects.requireNonNull(mode); Objects.requireNonNull(expiresAt); Objects.requireNonNull(purpose);
        if (purpose == PreviewPurpose.EDITOR && (!(mode instanceof PreviewMode.Draft) || issuedBy == null || issuedBy <= 0))
            throw new IllegalArgumentException("编辑器令牌必须绑定用户与草稿模式");
    }
}

/** @param partOverrides 仅EDITOR/Draft允许：声明过且通过Schema的未保存layout，不允许保留元数据或AUTO目标布局。 */
public record PreviewRequest(long localizationId, PreviewMode mode, Map<String, JsonNode> partOverrides, PreviewPurpose purpose) {
    public PreviewRequest { partOverrides = Map.copyOf(partOverrides); Objects.requireNonNull(mode); Objects.requireNonNull(purpose); }
    /** 兼容读取入口，不能通过传入覆盖来升级用途。 */
    public PreviewRequest(long localizationId, PreviewMode mode, Map<String, JsonNode> partOverrides) {
        this(localizationId, mode, partOverrides, PreviewPurpose.PREVIEW);
    }
}

/** 预览快照：结构与 PublishedSnapshot.payload 相同；issues 为组装时的校验问题（预览不因 ERROR 中止）。 */
public record PreviewSnapshot(long contentId, long localizationId, String contentType, String locale,
                              JsonNode payload, List<ValidationIssue> issues) {
    public PreviewSnapshot { issues = List.copyOf(issues); }
}

/** 审核结论。 */
public enum ReviewDecision { APPROVED, REJECTED }
```

```java
package com.b2bweb.api.content;

public final class ContentErrors {
    private ContentErrors() {}
    public static final ErrorCode NOT_FOUND                  = ErrorCode.of("CONTENT_NOT_FOUND", 404, "内容不存在");
    public static final ErrorCode LOCALIZATION_NOT_FOUND     = ErrorCode.of("CONTENT_LOCALIZATION_NOT_FOUND", 404, "本地化不存在");
    public static final ErrorCode REVISION_NOT_FOUND         = ErrorCode.of("CONTENT_REVISION_NOT_FOUND", 404, "修订不存在");
    public static final ErrorCode TYPE_UNKNOWN               = ErrorCode.of("CONTENT_TYPE_UNKNOWN", 422, "未注册或已停用的内容类型");
    public static final ErrorCode LOCALE_NOT_SOURCE          = ErrorCode.of("CONTENT_LOCALE_NOT_SOURCE", 422, "新建内容必须使用源语言");
    public static final ErrorCode LOCALE_IS_SOURCE           = ErrorCode.of("CONTENT_LOCALE_IS_SOURCE", 422, "目标语言不能是源语言");
    public static final ErrorCode LOCALE_NOT_ENABLED         = ErrorCode.of("CONTENT_LOCALE_NOT_ENABLED", 422, "站点未启用该语言");
    public static final ErrorCode LOCALE_EXISTS              = ErrorCode.of("CONTENT_LOCALE_EXISTS", 409, "该语言的本地化已存在");
    public static final ErrorCode VERSION_CONFLICT           = ErrorCode.of("CONTENT_VERSION_CONFLICT", 409, "内容已被他人修改");
    public static final ErrorCode STATE_INVALID              = ErrorCode.of("CONTENT_STATE_INVALID", 409, "当前状态不允许该操作");
    public static final ErrorCode REVIEW_NOT_ENABLED         = ErrorCode.of("CONTENT_REVIEW_NOT_ENABLED", 409, "该内容类型未启用审核");
    public static final ErrorCode REVIEW_REQUIRED            = ErrorCode.of("CONTENT_REVIEW_REQUIRED", 409, "需要审核通过后才能发布");
    public static final ErrorCode PUBLISH_VALIDATION_FAILED  = ErrorCode.of("CONTENT_PUBLISH_VALIDATION_FAILED", 422, "发布校验未通过");
    public static final ErrorCode SLUG_INVALID               = ErrorCode.of("CONTENT_SLUG_INVALID", 422, "slug 格式不正确");
    public static final ErrorCode SLUG_CONFLICT              = ErrorCode.of("CONTENT_SLUG_CONFLICT", 409, "路径已被其他内容占用");
    public static final ErrorCode CARD_MISSING               = ErrorCode.of("CONTENT_CARD_MISSING", 422, "发布快照缺少卡片信息");
    public static final ErrorCode SNAPSHOT_TOO_LARGE         = ErrorCode.of("CONTENT_SNAPSHOT_TOO_LARGE", 422, "快照超过 1 MB");
    public static final ErrorCode REDIRECT_TARGET_INVALID    = ErrorCode.of("CONTENT_REDIRECT_TARGET_INVALID", 422, "下线跳转目标无效");
    public static final ErrorCode TRASHED                    = ErrorCode.of("CONTENT_TRASHED", 409, "内容在回收站中");
    public static final ErrorCode NOT_TRASHED                = ErrorCode.of("CONTENT_NOT_TRASHED", 409, "内容不在回收站中");
    public static final ErrorCode HAS_PUBLISHED_LOCALIZATION = ErrorCode.of("CONTENT_HAS_PUBLISHED_LOCALIZATION", 409, "仍有已发布的语言版本，请先下线");
    public static final ErrorCode IMPACT_BLOCKED             = ErrorCode.of("CONTENT_IMPACT_BLOCKED", 409, "影响分析存在阻止项");
    public static final ErrorCode PREVIEW_TTL_INVALID        = ErrorCode.of("CONTENT_PREVIEW_TTL_INVALID", 422, "预览链接有效期超出上限");
    public static final ErrorCode TRANSLATION_MODE_INVALID   = ErrorCode.of("CONTENT_TRANSLATION_MODE_INVALID", 422, "不允许的翻译模式切换");
}
```

| 跨模块用例 | 调用 |
|---|---|
| 业务模块新建内容（产品、文章、页面） | 在自己的事务中：写业务表 → `create` → 用返回的 `contentId` 关联业务表 |
| 业务模块保存草稿（含 SEO 面板、页面插槽） | 先`lockDraftForWrite(localizationId, expectedVersion)`取得父/子行锁 → 写自己的表 → `updateDraftMeta`；新内容先create取得身份及锁 |
| taxonomy 为可路由分类项创建落地页 | `create(TERM_PAGE, zh-CN, 分类名)`；删除分类项时 `unpublishAll(contentId, REDIRECT_CONTENT, 上级分类落地页)` → `trash` |
| product 导入后直接发布、批量发布 / 下线 | 每个产品一个事务：`publish` / `unpublish`（当前用户须有 publish 权限） |
| translation 开始翻译 | 调用引擎前捕获并持久化expectedTargetVersion；回写事务先lockDraftForWrite校验；非拥有者provider先apply、拥有者最后apply并以该版本调用一次updateDraftMeta；事件携带回写后targetVersion |
| 译文自动发布 | content 监听 `TranslationCompletedEvent`，以 `system` 身份、`PublishTrigger.TRANSLATION` 执行 `publish` 的同一流程（系统设计 6.3） |
| delivery 渲染线上页面 | `UrlApi.resolve(path)` → `findPublishedSnapshot(localizationId)`；卡片与 hreflang：`findPublishedCards`、`publishedVersions` |
| delivery 预览 / 编辑器画布 | `resolvePreviewToken` → `assemblePreview` |
| seo sitemap、search 重建、graph 全量计算 | `scanPublished(locale, types, routableOnly, modifiedSince)` |
| graph 候选集 | `contentIdsByPublishedTerms` + `publishedTerms` |
| inquiry 询盘明细快照 | `findPublishedCards(contentIds, inquiryLocale)`（标题、路径） |

#### 4.4.17 UrlApi：URL 引擎

路径计算与唯一性、路由、自动重定向、下线路由、请求路径解析、slug 生成（系统设计 13）。URL 规则（`url_pattern`）、手工重定向、CSV 导入、404 报告是 url 自己的后台功能，不进入契约。

- 提供方：url。使用方：content（发布、下线、彻底删除）、delivery（请求解析）、seo（canonical 检查、IndexNow）、product（导入旧站 URL）、navigation（菜单 URL 校验）。
- **调用方向**：只有 content 调用 `reservePath` / `applyRoute` / `retireRoute` / `releaseRoutes`（4.1.3 的 content → url）；url 不调用 content，所需信息（内容类型的默认 URL 规则、`RouteHints`）由 content 在参数中传入。
- **路径计算**：`语言前缀 + 规则展开 + 尾斜杠策略`。规则取 `url_pattern` 中（站点 × 语言 × `RouteHints.patternKey` 或内容类型）的覆盖，没有时用 `defaultPattern`；变量 `{slug}` 取参数，`{parentPath}` 取 `RouteHints.parentContentId` 在同一语言的当前路由（去掉语言前缀），其他变量取 `RouteHints.variables`；`RouteHints.localeRoot` 为 true 时路径就是语言前缀本身（首页，如 `/zh`）。
- **层级变化**：`applyRoute` 发现某内容的路径变化且存在以它为 `parentContentId` 的路由时，url 提交任务（`url.recomputeSubtree`，4.4.12）重算子树路径，并为每个变化的路径生成 301 与 `ContentPathChangedEvent`（系统设计 13.4）。
- Delivery 热路径：`resolve`、`pathOf`、`pathsOf`。

```java
package com.b2bweb.api.url;

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
```

```java
package com.b2bweb.api.url;

/**
 * 内容类型的所属模块为某本地化提供的路由提示（ContentTypeProvider.routeHints，4.5.2）。
 *
 * @param patternKey             url_pattern 的匹配键：为空时按内容类型；TERM_PAGE 为词汇表 key（如 "product_category"）
 * @param defaultPatternOverride 覆盖 ContentTypeDefinition.defaultUrlPattern（如各词汇表的落地页规则）；为空表示不覆盖
 * @param parentContentId        {parentPath} 的来源：父页面或上级分类落地页
 * @param fallbackParentContentId UnpublishStrategy.REDIRECT_PARENT 的目标：主分类落地页或父页面
 * @param localeRoot             是否为语言首页（路径 = 语言前缀）
 * @param variables              其他规则变量
 */
public record RouteHints(@Nullable String patternKey, @Nullable String defaultPatternOverride,
                         @Nullable Long parentContentId, @Nullable Long fallbackParentContentId,
                         boolean localeRoot, Map<String, String> variables) {
    public RouteHints { variables = Map.copyOf(variables); }

    public static RouteHints none() { return new RouteHints(null, null, null, null, false, Map.of()); }
}

/** @param defaultPattern ContentTypeDefinition.defaultUrlPattern，如 "/products/{slug}"（不含语言前缀） */
public record RouteRequest(long contentId, long localizationId, String contentType, String locale,
                           String slug, String defaultPattern, RouteHints hints) {}

public sealed interface PathReservation
        permits PathReservation.Available, PathReservation.Conflict, PathReservation.Invalid {

    /** @param previousOwnerLocalizationId 该路径以前属于的本地化（历史路径被占用时） */
    record Available(String path, @Nullable Long previousOwnerLocalizationId) implements PathReservation {}

    /** 路径已被其他本地化的路由占用。 */
    record Conflict(String path, long conflictContentId, long conflictLocalizationId,
                    String suggestedSlug) implements PathReservation {}

    /** 路径不合法：保留字（URL_PATH_RESERVED）、规则变量缺失（URL_PATTERN_INVALID）、长度超限等。 */
    record Invalid(String path, String code, String message) implements PathReservation {}
}

public record ApplyRouteCommand(long contentId, long localizationId, String contentType, String locale,
                                String path, @Nullable Long parentContentId) {}

/** 退役方式：由 content 按 UnpublishStrategy 换算（REDIRECT_CONTENT / REDIRECT_PARENT → REDIRECT + 目标的同语言本地化）。 */
public enum RetireMode { GONE, REDIRECT }

/** @param targetLocalizationId mode = REDIRECT 时必填（目标内容在同一语言的本地化） */
public record RetireRouteCommand(long localizationId, RetireMode mode, @Nullable Long targetLocalizationId) {}

/**
 * @param removedRedirectSources 因路由优先而删除的重定向源路径
 */
public record RouteChange(long localizationId, @Nullable String previousPath, @Nullable String currentPath,
                          boolean changed, List<String> removedRedirectSources) {
    public RouteChange { removedRedirectSources = List.copyOf(removedRedirectSources); }
}

/** 请求路径解析结果。 */
public sealed interface RouteResolution permits RouteResolution.Content, RouteResolution.Redirect,
        RouteResolution.Gone, RouteResolution.NotFound {

    record Content(long contentId, long localizationId, String locale, String path) implements RouteResolution {}

    /** @param status 301 / 302 / 308；location 为站内路径或外部绝对 URL */
    record Redirect(int status, String location, RedirectReason reason) implements RouteResolution {}

    record Gone(String path) implements RouteResolution {}

    record NotFound(String path) implements RouteResolution {}
}

public enum RedirectReason {
    /** 规范化：尾斜杠、重复斜杠、大写（301） */
    NORMALIZE,
    /** 根路径到默认对外语言首页（302） */
    ROOT,
    /** 命中 url_redirect */
    REDIRECT_RULE
}

public enum RedirectMatchType { EXACT, PREFIX, REGEX }

public enum RedirectTargetKind { LOCALIZATION, PATH, EXTERNAL, GONE }

public enum RedirectOrigin { AUTO, MANUAL, IMPORT }

/** @param statusCode 301 / 302 / 308 / 410（GONE 时必须为 410） */
public record CreateRedirectCommand(@NotBlank @Size(max = 2048) String sourcePath, RedirectMatchType matchType,
                                    RedirectTargetKind targetKind, @Nullable Long targetLocalizationId,
                                    @Nullable @Size(max = 2048) String targetPath, int statusCode,
                                    RedirectOrigin origin, @Nullable @Size(max = 255) String note) {}

public record RedirectInfo(long redirectId, String sourcePath, RedirectMatchType matchType,
                           RedirectTargetKind targetKind, @Nullable Long targetLocalizationId,
                           @Nullable String targetPath, int statusCode, RedirectOrigin origin) {}
```

```java
package com.b2bweb.api.url;

public final class UrlErrors {
    private UrlErrors() {}
    public static final ErrorCode PATH_CONFLICT          = ErrorCode.of("URL_PATH_CONFLICT", 409, "路径已被占用");
    public static final ErrorCode PATH_RESERVED          = ErrorCode.of("URL_PATH_RESERVED", 422, "路径使用了保留字");
    public static final ErrorCode SLUG_INVALID           = ErrorCode.of("URL_SLUG_INVALID", 422, "slug 格式不正确");
    public static final ErrorCode PATTERN_INVALID        = ErrorCode.of("URL_PATTERN_INVALID", 422, "URL 规则无法展开");
    public static final ErrorCode REDIRECT_LOOP          = ErrorCode.of("URL_REDIRECT_LOOP", 422, "重定向形成环路");
    public static final ErrorCode REDIRECT_SOURCE_IS_ROUTE = ErrorCode.of("URL_REDIRECT_SOURCE_IS_ROUTE", 409, "源路径是现有内容的路由");
    public static final ErrorCode REDIRECT_LIMIT         = ErrorCode.of("URL_REDIRECT_LIMIT", 422, "前缀与正则重定向超过上限");
    public static final ErrorCode REDIRECT_INVALID       = ErrorCode.of("URL_REDIRECT_INVALID", 422, "重定向定义不合法");
}
```

#### 4.4.18 MediaApi：媒体

上传（含病毒扫描与去重）、媒体元数据与按语言的 alt / 标题、图片 URL（imgproxy 签名）、公开文件与私有文件的下载、留资文档的下载授权、引用查询与删除（系统设计 8.4、12、17.3）。业务模块只依赖本接口，不接触 `ObjectStorage`（系统设计 12.5）。

- 提供方：media。使用方：form（表单附件、留资文档下载）、product（图集、文档、导入图片）、article、page、inquiry（附件下载）、delivery、seo、search。
- 引用追踪（`mda_usage`）按4.10.1的事务内登记方案维护；旧的media异步收集写入路径被替代。业务拥有者通过core协调器登记，不直接访问mda_表。
- 媒体的 alt、标题、说明以中文录入，作为站点资源翻译（资源类型 `media.media`，4.5.21）；按语言读取时该语言没有译文的字段为空（不回退）。
- Delivery 热路径：`views`、`imageSources`。

```java
package com.b2bweb.api.media;

public interface MediaApi {

    /**
     * 上传文件（流式，单文件 ≤ 50 MB；表单附件 ≤ 20 MB，由 command.maxBytes 收紧）。流程见系统设计 12.2：
     * 扩展名白名单 → Tika 识别真实 MIME（与扩展名不符则拒绝）→ sha256 → 图片尺寸、blurhash、主色；SVG 净化 →
     * 文档与压缩包异步病毒扫描（status = PROCESSING，扫描后 READY / QUARANTINED 并发布 MediaUpdatedEvent）→ 存储 → 发布 MediaUploadedEvent。
     * reuseDuplicate 为 true 且当前站点已有相同 sha256、相同可见性的媒体时，直接返回已有媒体（reused = true）。
     * 调用方负责关闭 content。
     *
     * @throws BusinessException MEDIA_TYPE_NOT_ALLOWED、MEDIA_TOO_LARGE、MEDIA_MIME_MISMATCH、MEDIA_SVG_FORBIDDEN（非管理员上传 SVG）
     */
    UploadResult upload(@Valid UploadCommand command, InputStream content);

    /**
     * 从远程 URL 导入（产品导入的图片 URL，系统设计 8.6）：经 RemoteFileFetcher（4.7.13）下载，域名必须在站点白名单中，
     * 其余同 upload。不得在持有数据库事务时调用（网络 I/O）。
     *
     * @throws BusinessException MEDIA_REMOTE_FETCH_FAILED（不在白名单、内网地址、超时、超限）
     */
    UploadResult uploadFromUrl(@Valid RemoteUploadCommand command);

    Optional<MediaInfo> find(long mediaId);

    /** 批量查询（≤ 500）；发布校验中检查引用的媒体存在、类型正确、READY。 */
    Map<Long, MediaInfo> findAll(Collection<Long> mediaIds);

    /** 媒体在指定语言的视图（含 alt、标题、说明；公开文件带下载 URL）。Delivery 热路径。 */
    Map<Long, MediaView> views(Collection<Long> mediaIds, String locale);

    /**
     * 图片的响应式来源：按预设与标准宽度（320、640、960、1280、1920，不超过原图宽度）生成 AVIF / WebP / 原格式的签名 URL
     * （系统设计 12.3）。非图片、私有或非 READY 的媒体不出现在结果中。Delivery 热路径。
     */
    Map<Long, ImageSources> imageSources(Collection<Long> mediaIds, ImagePreset preset, String locale);

    /** 单个图片 URL（JSON-LD、OG、邮件）。width 取不小于它的最近标准宽度。 */
    Optional<URI> imageUrl(long mediaId, ImagePreset preset, int width, ImageFormat format);

    /** 公开文件（PDF 等）的 CDN 下载 URL；私有文件为空。 */
    Optional<URI> publicFileUrl(long mediaId);

    /**
     * 私有文件的短期预签名下载链接（员工下载表单与询盘附件；调用方记录审计，系统设计 17.3）。ttl ≤ 1 小时。
     *
     * @throws BusinessException MEDIA_NOT_READY（扫描未完成）、MEDIA_QUARANTINED、MEDIA_NOT_PRIVATE
     */
    URI privateDownloadUrl(long mediaId, Duration ttl, @Nullable String downloadFileName);

    /**
     * 设置留资文档的门控表单（框架补充：product 保存文档的 gated_form_key 时同步到 media；为空表示取消门控）。
     * 只允许私有文件。
     */
    void setDownloadGate(long mediaId, @Nullable String formKey);

    /**
     * 签发留资文档票据：同站点、未软删、PRIVATE/READY、门控表单匹配，且mda_usage存在
     * 此contentId/locale的PUBLISHED文档引用。仅本地签名，无对象存储/SMTP网络调用。
     * 签名包括site/submission/media/content/locale/formKey/当前download_gate_version/固定expiresAt。
     * form把返回URL及expiresAt写入自身download_grant，数据库提交后才可返回/发送；详见4.10.6。
     */
    DownloadTicket issueGatedDownload(@Valid GatedDownloadCommand command);

    /** 媒体的引用位置（来自 mda_usage）。 */
    List<MediaUsageInfo> usages(long mediaId);

    /**
     * 普通资产检查引用后移入回收站，文件保留30天；到期由清理任务物理删除。
     * force只清理由调用模块管理且无受保护引用的私有附件，不能忽略mda_usage。
     * 排他媒体行锁保护状态变化；引用共享锁内必须确认deleted_at为空。幂等。
     */
    void delete(long mediaId, boolean force);
}
```

```java
package com.b2bweb.api.media;

public enum MediaKind { IMAGE, VIDEO, DOCUMENT, ARCHIVE, OTHER }

public enum MediaStatus { PROCESSING, READY, QUARANTINED }

/** 上传来源：决定校验规则、默认可见性与“未使用媒体”报告的范围。 */
public enum UploadSource {
    /** 后台媒体库 */
    ADMIN,
    /** 前台表单附件（私有，form 管理生命周期） */
    FORM_ATTACHMENT,
    /** 导入（产品导入的图片） */
    IMPORT,
    /** 系统生成 */
    SYSTEM
}

/** 图片预设（系统设计 12.3；OG 为框架补充，用于 og:image 与 JSON-LD）。 */
public enum ImagePreset {
    THUMB(ResizeMode.FILL, 1.0, 320),
    CARD(ResizeMode.FILL, 4.0 / 3, 640),
    CONTENT(ResizeMode.FIT, null, 1280),
    HERO(ResizeMode.FILL, 16.0 / 9, 1920),
    OG(ResizeMode.FILL, 1200.0 / 630, 1200);

    private final ResizeMode mode;
    private final @Nullable Double aspectRatio;
    private final int maxWidth;

    ImagePreset(ResizeMode mode, @Nullable Double aspectRatio, int maxWidth) {
        this.mode = mode; this.aspectRatio = aspectRatio; this.maxWidth = maxWidth;
    }
    public ResizeMode mode() { return mode; }
    /** 宽高比；FIT 为空（保持原比例）。FILL 时按焦点裁剪。 */
    public @Nullable Double aspectRatio() { return aspectRatio; }
    public int maxWidth() { return maxWidth; }

    /** 响应式标准宽度（系统设计 12.3）。 */
    public static final List<Integer> STANDARD_WIDTHS = List.of(320, 640, 960, 1280, 1920);
}

public enum ResizeMode { FIT, FILL }

/** 在 URL 中显式指定格式，不依赖 Accept 协商（系统设计 12.3）。 */
public enum ImageFormat { AVIF, WEBP, JPEG, PNG }

/**
 * @param declaredContentType 客户端声明的 MIME（只作参考，以 Tika 识别结果为准）
 * @param allowedKinds        允许的类型；空表示按 source 的默认白名单
 * @param maxBytes            更严格的大小上限；为空表示按 source 的默认上限
 */
public record UploadCommand(@NotBlank @Size(max = 255) String fileName, @Nullable String declaredContentType,
                            @Positive long size, com.b2bweb.api.port.Visibility visibility, UploadSource source,
                            @Nullable Long folderId, boolean reuseDuplicate, Set<MediaKind> allowedKinds,
                            @Nullable Long maxBytes) {
    public UploadCommand { allowedKinds = Set.copyOf(allowedKinds); }
}

public record RemoteUploadCommand(URI url, @Nullable String fileName, com.b2bweb.api.port.Visibility visibility,
                                  UploadSource source, @Nullable Long folderId, boolean reuseDuplicate,
                                  Set<MediaKind> allowedKinds) {
    public RemoteUploadCommand { allowedKinds = Set.copyOf(allowedKinds); }
}

public record UploadResult(MediaInfo media, boolean reused) {}

/**
 * 媒体元数据（语言无关）。
 *
 * @param focalX / focalY 焦点（0–1），FILL 裁剪时使用；未设置为空
 * @param downloadGateFormKey 留资文档的门控表单 key
 */
public record MediaInfo(long mediaId, MediaKind kind, com.b2bweb.api.port.Visibility visibility, MediaStatus status,
                        String originalName, String mime, long size, String sha256,
                        @Nullable Integer width, @Nullable Integer height, @Nullable Integer durationSeconds,
                        @Nullable Double focalX, @Nullable Double focalY,
                        @Nullable String blurhash, @Nullable String dominantColor,
                        @Nullable Long folderId, @Nullable String downloadGateFormKey,
                        UploadSource source, Instant createdAt) {

    public boolean isImage() { return kind == MediaKind.IMAGE; }
    public boolean isReady() { return status == MediaStatus.READY; }
}

/**
 * 媒体在某语言的视图。
 *
 * @param alt / title / caption 该语言的文本；没有译文时为空（不回退）
 * @param publicUrl             公开文件的下载 URL；图片请用 imageSources；私有文件为空
 */
public record MediaView(long mediaId, String locale, MediaKind kind, com.b2bweb.api.port.Visibility visibility,
                        MediaStatus status, String fileName, String mime, long size,
                        @Nullable Integer width, @Nullable Integer height,
                        @Nullable String alt, @Nullable String title, @Nullable String caption,
                        @Nullable String blurhash, @Nullable String dominantColor,
                        @Nullable URI publicUrl) {}

/** srcset 的一项。 */
public record ImageCandidate(int width, URI url) {}

/**
 * 图片的响应式来源（前台输出 picture / srcset）。
 *
 * @param width / height 按预设裁剪后的最大输出尺寸（用于 width / height 属性，防止 CLS）
 * @param fallbackUrl    原格式（JPEG / PNG）在默认宽度的 URL
 * @param srcsets        AVIF、WEBP 与原格式各一组
 */
public record ImageSources(long mediaId, ImagePreset preset, int width, int height,
                           @Nullable String alt, @Nullable String blurhash, @Nullable String dominantColor,
                           URI fallbackUrl, Map<ImageFormat, List<ImageCandidate>> srcsets) {
    public ImageSources {
        var copy = new java.util.EnumMap<ImageFormat, List<ImageCandidate>>(ImageFormat.class);
        srcsets.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        srcsets = java.util.Collections.unmodifiableMap(copy);
    }
}

public record DownloadTicket(URI url, Instant expiresAt) {}

/** 只由form在ACCEPT且非测试的原提交事务调用；expiresAt固定为提交创建时刻+站点配置TTL（≤7天）。 */
public record GatedDownloadCommand(long submissionId, long mediaId, String formKey,
                                   long contentId, String locale, Instant expiresAt) {}

/**
 * 引用位置。
 *
 * @param contentId    引用方内容；资源引用为空
 * @param resourceType 引用方站点资源类型（如 "navigation.menu"、"core.site_profile"）；内容引用为空
 * @param part         快照部分或资源字段组，如 "product"、"layout"、"seo"
 * @param fieldPath    字段路径，如 "gallery[2]"、"sections[sec_01J9X4K2].props.image"
 */
public record MediaUsageInfo(long mediaId, @Nullable Long contentId, @Nullable String locale,
                             @Nullable String resourceType, @Nullable String resourceId,
                             String part, String fieldPath, boolean published) {}
```

```java
package com.b2bweb.api.media;

public final class MediaErrors {
    private MediaErrors() {}
    public static final ErrorCode NOT_FOUND             = ErrorCode.of("MEDIA_NOT_FOUND", 404, "媒体不存在");
    public static final ErrorCode TYPE_NOT_ALLOWED      = ErrorCode.of("MEDIA_TYPE_NOT_ALLOWED", 422, "不允许的文件类型");
    public static final ErrorCode TOO_LARGE             = ErrorCode.of("MEDIA_TOO_LARGE", 413, "文件过大");
    public static final ErrorCode MIME_MISMATCH         = ErrorCode.of("MEDIA_MIME_MISMATCH", 422, "文件内容与扩展名不符");
    public static final ErrorCode SVG_FORBIDDEN         = ErrorCode.of("MEDIA_SVG_FORBIDDEN", 403, "当前角色不允许上传 SVG");
    public static final ErrorCode NOT_READY             = ErrorCode.of("MEDIA_NOT_READY", 409, "文件仍在处理中");
    public static final ErrorCode QUARANTINED           = ErrorCode.of("MEDIA_QUARANTINED", 409, "文件未通过病毒扫描");
    public static final ErrorCode NOT_PRIVATE           = ErrorCode.of("MEDIA_NOT_PRIVATE", 422, "该操作只适用于私有文件");
    public static final ErrorCode NOT_IMAGE             = ErrorCode.of("MEDIA_NOT_IMAGE", 422, "媒体不是图片");
    public static final ErrorCode IN_USE                = ErrorCode.of("MEDIA_IN_USE", 409, "媒体仍被引用");
    public static final ErrorCode DOWNLOAD_NOT_ALLOWED  = ErrorCode.of("MEDIA_DOWNLOAD_NOT_ALLOWED", 403, "不允许下载该文件");
    public static final ErrorCode REMOTE_FETCH_FAILED   = ErrorCode.of("MEDIA_REMOTE_FETCH_FAILED", 422, "远程文件下载失败");
}
```

> 框架补充：`setDownloadGate` / `issueGatedDownload` 需要 media 在 `mda_media` 中保存门控表单 key；`MediaUsageInfo` 的资源引用（`resourceType`、`resourceId`）需要 `mda_usage` 支持资源引用（菜单推荐图片、站点资料 Logo，见 4.5.16）。两者写入 media 模块设计文档的数据模型，并同步系统设计 12.1。

#### 4.4.19 TaxonomyApi：分类体系

词汇表与分类项的读取、层级关系、按语言的名称，以及内容的草稿态分类指派（系统设计 7）。发布态指派由 taxonomy 的 `PublishContributor` 写入快照 `terms` 部分，再由内容内核投影到 `cnt_published_term`（读取用 `ContentApi.publishedTerms`）。

- 提供方：taxonomy。使用方：product（主分类、属性集继承、导入）、article、page、inquiry（分配规则按产品分类）、navigation（菜单链接到分类）、delivery、seo、search、graph（同属一个父分类）。
- 分类名以中文录入，作为站点资源翻译（资源类型 `taxonomy.term`）；`termViews` 按语言返回，没有译文时 `name` 为空（不回退）。
- `replaceAssignments` 修改草稿态指派后，由 taxonomy 调用 `ContentApi.markDraftChanged`（taxonomy → content 是允许的方向，4.1.3），调用方不需要再调用。
- Delivery 热路径：`termViews`。

```java
package com.b2bweb.api.taxonomy;

public interface TaxonomyApi {

    Optional<VocabularyInfo> vocabulary(String vocabularyKey);

    /** 当前站点的全部词汇表（内置 + 自定义），按 sort。 */
    List<VocabularyInfo> vocabularies();

    /** 适用于某内容类型的词汇表（applicable_types 包含该类型）。 */
    List<VocabularyInfo> vocabulariesFor(String contentType);

    Optional<TermInfo> findTerm(long termId);

    /** 批量查询（≤ 500）。 */
    Map<Long, TermInfo> findTerms(Collection<Long> termIds);

    /** 分类项在指定语言的视图（名称、落地页）。Delivery 热路径。 */
    Map<Long, TermView> termViews(Collection<Long> termIds, String locale);

    /** 祖先链（从根到父，不含自身）；面包屑与结构化数据使用。 */
    List<TermInfo> ancestors(long termId);

    /** 批量取祖先 ID（从根到父）；属性集继承、分配规则、图谱“同属一个父分类”使用。 */
    Map<Long, List<Long>> ancestorIds(Collection<Long> termIds);

    /** 直接子项；parentTermId 为空时返回根项。按 sort。 */
    List<TermInfo> children(String vocabularyKey, @Nullable Long parentTermId);

    /** termId 是否等于 ancestorTermId 或是其后代（询盘分配规则“产品分类”条件，系统设计 17.7）。 */
    boolean isSameOrDescendant(long termId, long ancestorTermId);

    /** 按源语言名称路径查找（产品导入，如 ["电池", "锂电池"]）；名称精确匹配。 */
    Optional<TermInfo> findBySourceNamePath(String vocabularyKey, List<String> sourceNames);

    /** 由 TERM_PAGE 内容反查分类项（落地页的面包屑、SEO 变量）。 */
    Optional<TermInfo> findByLandingContent(long contentId);

    /** 内容的草稿态指派，按词汇表、sort。 */
    List<TermAssignment> assignments(long contentId);

    /** 批量查询草稿态指派；vocabularyKey 为空表示全部词汇表。 */
    Map<Long, List<TermAssignment>> assignmentsOf(Collection<Long> contentIds, @Nullable String vocabularyKey);

    /**
     * 替换内容在某词汇表中的草稿态指派（编辑页“分类”标签、导入、批量指派）。加入调用方事务。
     * 校验：词汇表适用于该内容类型、multiple = false 时最多一项、hierarchical 词汇表的主分类恰好一个（产品必须有主分类，系统设计 7.3）。
     *
     * @throws BusinessException TAXONOMY_VOCABULARY_NOT_APPLICABLE、TAXONOMY_MULTIPLE_NOT_ALLOWED、TAXONOMY_PRIMARY_INVALID、TAXONOMY_TERM_NOT_FOUND
     */
    void replaceAssignments(long contentId, String vocabularyKey, List<AssignmentInput> assignments);
}
```

```java
package com.b2bweb.api.taxonomy;

/** 内置词汇表 key（系统设计 7.2）。站点可以自定义词汇表。 */
public final class Vocabularies {
    private Vocabularies() {}
    public static final String PRODUCT_CATEGORY = "product_category";
    public static final String APPLICATION = "application";
    public static final String INDUSTRY = "industry";
    public static final String ARTICLE_CATEGORY = "article_category";
    public static final String TAG = "tag";
    public static final String CERTIFICATION = "certification";
}

/** @param name 中文名称（后台显示） */
public record VocabularyInfo(String vocabularyKey, String name, boolean hierarchical, boolean multiple,
                             boolean routable, boolean required, Set<String> applicableTypes,
                             boolean builtin, int sort) {
    public VocabularyInfo { applicableTypes = Set.copyOf(applicableTypes); }
}

/**
 * 分类项（语言无关）。
 *
 * @param path             物化路径，如 "/1/5/9/"
 * @param landingContentId 可路由分类项的 TERM_PAGE 内容 ID
 * @param sourceName       源语言（中文）名称
 */
public record TermInfo(long termId, String vocabularyKey, @Nullable Long parentId, String path, int depth,
                       int sort, @Nullable Long landingContentId, String sourceName) {}

/** @param name 该语言的名称；没有译文时为空 */
public record TermView(long termId, String vocabularyKey, @Nullable Long parentId, int depth, String locale,
                       @Nullable String name, @Nullable Long landingContentId) {}

public record TermAssignment(long contentId, long termId, String vocabularyKey, boolean primary, int sort) {}

public record AssignmentInput(long termId, boolean primary, int sort) {}

public final class TaxonomyErrors {
    private TaxonomyErrors() {}
    public static final ErrorCode VOCABULARY_NOT_FOUND      = ErrorCode.of("TAXONOMY_VOCABULARY_NOT_FOUND", 404, "词汇表不存在");
    public static final ErrorCode TERM_NOT_FOUND            = ErrorCode.of("TAXONOMY_TERM_NOT_FOUND", 404, "分类项不存在");
    public static final ErrorCode VOCABULARY_NOT_APPLICABLE = ErrorCode.of("TAXONOMY_VOCABULARY_NOT_APPLICABLE", 422, "该词汇表不适用于此内容类型");
    public static final ErrorCode MULTIPLE_NOT_ALLOWED      = ErrorCode.of("TAXONOMY_MULTIPLE_NOT_ALLOWED", 422, "该词汇表只允许选择一项");
    public static final ErrorCode PRIMARY_INVALID           = ErrorCode.of("TAXONOMY_PRIMARY_INVALID", 422, "必须且只能有一个主分类");
    public static final ErrorCode TERM_HAS_ASSIGNMENTS      = ErrorCode.of("TAXONOMY_TERM_HAS_ASSIGNMENTS", 409, "分类项仍有内容指派，请先转移");
}
```

#### 4.4.20 FormApi：表单

表单与提交记录的读取（系统设计 17.1、17.2）。表单定义的编辑、公开提交接口（限流、Turnstile、formToken、反垃圾）、附件预上传都是 form 自己的功能；提交保存后 form 发布 `FormSubmittedEvent`（4.6.9），inquiry 据此调用本接口读取提交内容。

- 提供方：form。使用方：inquiry（读取提交、复制隐私字段与附件）、product（选择与校验留资表单）。delivery 不调用本接口：表单定义由 form 的 `DeliveryContributor` 内嵌到 resolve 结果中（4.5.7）。
- 提交记录含个人数据：调用方只在处理该提交时读取，不得缓存或写入日志。

```java
package com.b2bweb.api.form;

public interface FormApi {

    /** 按 key 查找表单（含停用）。 */
    Optional<FormSummary> findForm(String formKey);

    Optional<FormSummary> findForm(long formId);

    /** 某用途的启用表单（product 的留资表单选择器：purpose = DOWNLOAD）。 */
    List<FormSummary> forms(FormPurpose purpose);

    /** 提交记录（含 REJECT；REJECT 在保留期内仍可读取）。 */
    Optional<SubmissionView> findSubmission(long submissionId);
}
```

```java
package com.b2bweb.api.form;

/** 表单用途（系统设计 17.1）；NEWSLETTER 为 V2。 */
public enum FormPurpose { INQUIRY, SAMPLE, DOWNLOAD, CONTACT, CUSTOM }

/** 反垃圾判定（系统设计 17.4）。 */
public enum SpamVerdict { ACCEPT, QUARANTINE, REJECT }

public enum FormStatus { ACTIVE, DISABLED }

/** 标准字段 key（映射到询盘，系统设计 17.1）。 */
public final class FormFieldKeys {
    private FormFieldKeys() {}
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String COMPANY = "company";
    public static final String COUNTRY = "country";
    public static final String JOB_TITLE = "job_title";
    public static final String MESSAGE = "message";
    public static final String QUANTITY = "quantity";
    public static final String WEBSITE = "website";
}

/** @param name 中文名称（后台显示） */
public record FormSummary(long formId, String formKey, String name, FormPurpose purpose, FormStatus status,
                          Set<String> locales) {
    public FormSummary { locales = Set.copyOf(locales); }
}

/**
 * 提交记录（frm_submission）。
 *
 * @param fields            字段值：key → 值（文本为字符串；多选为数组；电话为 E.164；国家为 ISO 3166-1 alpha-2）
 * @param items             产品选择字段（询价篮或当前产品）
 * @param attachmentMediaIds 附件（私有媒体）
 * @param downloadMediaId   DOWNLOAD 表单对应的留资文档
 * @param ip                原始 IP（保留 30 天后置空，系统设计 17.9）
 * @param test              测试提交（后台“测试表单”或测试标记）
 */
public record SubmissionView(long submissionId, long formId, String formKey, FormPurpose purpose, String locale,
                             JsonNode fields, List<SubmittedItem> items, List<Long> attachmentMediaIds,
                             @Nullable Long downloadMediaId, SubmissionAttribution attribution, PrivacyRecord privacy,
                             int spamScore, SpamVerdict spamVerdict, @Nullable String ip, @Nullable String ipCountry,
                             @Nullable String userAgent, boolean test, Instant createdAt) {
    public SubmissionView {
        items = List.copyOf(items);
        attachmentMediaIds = List.copyOf(attachmentMediaIds);
    }

    /** 标准字段的文本值；不存在或为空时为空。 */
    public Optional<String> text(String fieldKey) {
        JsonNode v = fields.get(fieldKey);
        return v == null || v.isNull() || v.asString().isBlank() ? Optional.empty() : Optional.of(v.asString());
    }
}

/** @param variantId 型号（ContentVariantProvider，4.5.17）；@param quantity 访客填写的数量文本 */
public record SubmittedItem(long contentId, @Nullable Long variantId, @Nullable String quantity) {}

/** 来源归因（系统设计 17.9），服务端已校验并截断。 */
public record SubmissionAttribution(@Nullable String landingUrl, @Nullable String referrer,
                                    @Nullable String utmSource, @Nullable String utmMedium,
                                    @Nullable String utmCampaign, @Nullable String utmTerm,
                                    @Nullable String utmContent, @Nullable String gclid,
                                    @Nullable String msclkid, @Nullable String fbclid,
                                    @Nullable JsonNode firstTouch, @Nullable JsonNode lastTouch,
                                    List<Long> viewedContentIds, @Nullable String device) {
    public SubmissionAttribution { viewedContentIds = List.copyOf(viewedContentIds); }
}

/** 隐私字段（系统设计 17.1、17.11），inquiry 原样复制到询盘。 */
public record PrivacyRecord(int privacyNoticeVersion, boolean marketingConsent,
                            @Nullable Instant marketingConsentAt, boolean gpc, boolean optOutSaleShare) {}

public final class FormErrors {
    private FormErrors() {}
    public static final ErrorCode NOT_FOUND            = ErrorCode.of("FORM_NOT_FOUND", 404, "表单不存在");
    public static final ErrorCode SUBMISSION_NOT_FOUND = ErrorCode.of("FORM_SUBMISSION_NOT_FOUND", 404, "提交记录不存在");
}
```

#### 4.4.21 NotificationApi：通知

发送邮件通知与 Webhook 事件（系统设计 18）。`send` 在调用方事务中写入发件箱（`ntf_message`），由 notification 的后台任务经 `MailTransport` / `WebhookTransport` 投递，确保“业务保存成功就一定会发通知”；事务回滚则通知不存在。

- 提供方：notification。使用方：inquiry（分配通知、SLA 提醒、客户自动回复、Webhook）、form（留资文档下载链接邮件）。content（审核请求）与 translation（翻译过期、翻译失败）的员工通知由 notification 监听对应事件发送（4.2.4），不调用本接口。
- **语言**：员工通知（`StaffUser`）固定使用 `zh-CN` 模板；客户邮件在send入队时按locale/fallbackLocale确定effectiveLocale并验证模板；仍没有时抛NOTIFICATION_TEMPLATE_NOT_FOUND，不创建发件箱，调用方事务回滚。入队后的effectiveLocale固定；工作线程渲染/SMTP故障按发件箱FAILED/重试处理，不静默换语言。是否提供 `fallbackLocale`（通常为站点默认对外语言）由调用方决定。
- **模型**：模板变量只能是 JSON 可序列化的值；模板引擎自动转义 HTML；不得放入密钥。客户邮件的模型中可以包含该客户自己的联系信息，员工通知中只放后台链接与必要摘要。
- **幂等**：`dedupKey` 非空时，同一站点、同一模板、同一收件人、同一 `dedupKey` 只入队一次（事件监听重复投递时不重复发送）。

```java
package com.b2bweb.api.notification;

public interface NotificationApi {

    /**
     * 为每个收件人写入一条发件箱消息（加入调用方事务），返回消息 ID（与 recipients 顺序一致；被 dedupKey 去重的返回已有 ID）。
     * 员工收件人的邮箱经 UserDirectory 解析，已停用的用户被跳过（不出现在结果中）。
     *
     * @throws BusinessException NOTIFICATION_TEMPLATE_NOT_FOUND（模板 key 未注册）、NOTIFICATION_RECIPIENT_INVALID
     */
    List<Long> send(@Valid NotificationRequest request);

    /**
     * 向当前站点订阅了该事件类型的 Webhook 投递事件（发件箱 + HMAC 签名 + 重试，系统设计 18）。加入调用方事务。
     * 没有订阅时不产生消息，返回空列表。
     */
    List<Long> sendWebhookEvent(@Valid WebhookEventRequest request);

    /** 消息状态（后台时间线展示“自动回复已发送”等）。 */
    Optional<MessageStatusView> status(long messageId);

    /** 该模板在该语言是否可用（有启用的模板正文）。 */
    boolean templateAvailable(String templateKey, String locale);
}
```

```java
package com.b2bweb.api.notification;

public enum NotificationChannel { EMAIL, WEBHOOK }

/** 模板受众：员工通知只有中文；客户邮件作为站点资源翻译（系统设计 18）。 */
public enum NotificationAudience { STAFF, CUSTOMER }

public enum MessageStatus { PENDING, SENT, FAILED }

/** 收件人。 */
public sealed interface Recipient permits Recipient.StaffUser, Recipient.Customer {
    /** 后台用户（邮箱取自用户目录） */
    record StaffUser(long userId) implements Recipient {}
    /** 客户（询盘联系人等） */
    record Customer(@Email @NotBlank String email, @Nullable String name) implements Recipient {}
}

/**
 * @param templateKey    {模块 ID}.{场景}.{接收方}，如 "inquiry.autoreply.customer"
 * @param locale         客户邮件的语言；员工通知忽略（固定 zh-CN）
 * @param fallbackLocale locale 没有模板时使用的语言；为空表示不回退
 * @param model          模板变量
 * @param replyTo        Reply-To（客户自动回复设为负责人邮箱）
 * @param dedupKey       幂等键，如 "inquiry:7201893453912345:autoreply"
 * @param notBefore      最早发送时间；为空表示立即
 * @param relatedType / relatedId 关联对象（如 "inquiry" / 询盘 ID），用于后台查询与数据主体请求
 */
public record NotificationRequest(@NotBlank String templateKey, @NotEmpty List<Recipient> recipients,
                                  @Nullable String locale, @Nullable String fallbackLocale,
                                  JsonNode model, @Nullable @Email String replyTo,
                                  @Nullable @Size(max = 128) String dedupKey, @Nullable Instant notBefore,
                                  @Nullable String relatedType, @Nullable String relatedId) {
    public NotificationRequest { recipients = List.copyOf(recipients); }
}

/**
 * @param eventType 如 "inquiry.created"
 * @param payload   默认只含事件类型、询盘编号与后台链接，不含联系人信息（系统设计 18）
 */
public record WebhookEventRequest(@NotBlank String eventType, JsonNode payload,
                                  @Nullable @Size(max = 128) String dedupKey) {}

public record MessageStatusView(long messageId, String templateKey, NotificationChannel channel, MessageStatus status,
                                int attempts, @Nullable Instant sentAt, @Nullable String errorCode,
                                Instant createdAt) {}

public final class NotificationErrors {
    private NotificationErrors() {}
    public static final ErrorCode TEMPLATE_NOT_FOUND = ErrorCode.of("NOTIFICATION_TEMPLATE_NOT_FOUND", 422, "通知模板不存在");
    public static final ErrorCode RECIPIENT_INVALID  = ErrorCode.of("NOTIFICATION_RECIPIENT_INVALID", 422, "收件人无效");
    public static final ErrorCode MODEL_INVALID      = ErrorCode.of("NOTIFICATION_MODEL_INVALID", 422, "模板变量不符合模板声明");
}
```

#### 4.4.22 TranslationApi：翻译

translation 对外提供的同步能力：按需翻译一段文本（询盘留言“翻译为中文”，系统设计 5.7、17.10），以及译文来源统计（SEO 健康检查规则“关键页面的机器译文未经审校”，系统设计 14.9）。内容与站点资源的翻译由事件驱动、异步执行（4.5.20、4.5.21、4.6.11），不经本接口。

- 提供方：translation。使用方：inquiry、seo。
- translation 可停用：停用时 `translateText` 抛出 `ModuleDisabledException`；调用方先用 `isTextTranslationAvailable()` 决定是否显示“翻译为中文”按钮（17.10）。
- **`translateText` 不得在持有数据库事务时调用**（4.1.3）：它同步调用 AI 引擎（OpenAI 兼容接口，经 `TranslationEngine` 端口，4.7.9），耗时为秒级。inquiry 的做法：事务外调用 → 新事务中保存 `message_translated`。
- **隐私与安全**：文本按纯数据处理，不作为指令（系统设计 26.3）；**不查询、不写入翻译记忆**（询盘留言属于个人数据，CLAUDE.md 禁止 13）；不保存原文与译文，只记录用量（token、费用）；计入站点月度预算（系统设计 5.10）。

```java
package com.b2bweb.api.translation;

public interface TranslationApi {

    /** translation 已启用、站点开启了询盘翻译功能、且配置了可用的主引擎。 */
    boolean isTextTranslationAvailable();

    /**
     * 同步翻译一段纯文本（不经翻译记忆、术语表只用于提示词、不写任何业务数据）。
     * 主引擎失败时按站点配置切换备用引擎；429 / 5xx 的重试由引擎适配器完成（4.7.9）。
     *
     * @throws ModuleDisabledException     translation 在当前站点停用
     * @throws BusinessException           TRANSLATION_FEATURE_DISABLED、TRANSLATION_ENGINE_NOT_CONFIGURED、
     *                                     TRANSLATION_TEXT_TOO_LONG、TRANSLATION_BUDGET_EXCEEDED
     * @throws ExternalServiceException    TRANSLATION_ENGINE_UNAVAILABLE（retryable = true）、TRANSLATION_OUTPUT_INVALID
     */
    TextTranslation translateText(@Valid TextTranslationRequest request);

    /** 本地化当前草稿各翻译单元的译文来源统计（来自 trn_job_item 与翻译记忆）；没有翻译记录的本地化不出现在结果中。 */
    Map<Long, SegmentOriginStats> segmentOrigins(Collection<Long> localizationIds);
}
```

```java
package com.b2bweb.api.translation;

/** 文本翻译用途（决定提示词模板、长度上限与用量统计分类）。 */
public enum TextPurpose {
    /** 询盘留言：目标语言通常为 zh-CN；≤ 5000 个字符 */
    INQUIRY_MESSAGE
}

/**
 * @param sourceLocale 为空表示由模型识别（结果中返回识别出的语言）
 * @param context      中文上下文说明，如“客户询盘留言”
 */
public record TextTranslationRequest(@NotBlank @Size(max = 5000) String text, @NotBlank String targetLocale,
                                     @Nullable String sourceLocale, TextPurpose purpose,
                                     @Nullable @Size(max = 200) String context) {}

/**
 * @param detectedSourceLocale 识别出的源语言（BCP 47）；无法识别时为空
 * @param sameLanguage         原文已是目标语言（此时 translatedText 等于原文，不计费）
 * @param engineName           引擎配置名称（trn_engine.name）；@param model 模型名（取自响应）
 */
public record TextTranslation(String translatedText, @Nullable String detectedSourceLocale, boolean sameLanguage,
                              String engineName, String model, int inputTokens, int outputTokens,
                              Instant translatedAt) {}

/**
 * 译文来源统计（系统设计 5.6、5.8）。
 *
 * @param memoryHits 本次生成时命中翻译记忆的单元数（含 HUMAN / REVIEWED / MACHINE 条目）
 */
public record SegmentOriginStats(long localizationId, int total, int human, int reviewed, int machine,
                                 int memoryHits, int needsReview) {
    /** 是否仍有未经人工修订或确认的机器译文。 */
    public boolean hasUnreviewedMachine() { return machine > 0; }
}

public final class TranslationErrors {
    private TranslationErrors() {}
    public static final ErrorCode FEATURE_DISABLED      = ErrorCode.of("TRANSLATION_FEATURE_DISABLED", 422, "站点已关闭该翻译功能");
    public static final ErrorCode ENGINE_NOT_CONFIGURED = ErrorCode.of("TRANSLATION_ENGINE_NOT_CONFIGURED", 422, "未配置可用的翻译引擎");
    public static final ErrorCode ENGINE_UNAVAILABLE    = ErrorCode.of("TRANSLATION_ENGINE_UNAVAILABLE", 503, "翻译引擎暂时不可用");
    public static final ErrorCode OUTPUT_INVALID        = ErrorCode.of("TRANSLATION_OUTPUT_INVALID", 502, "翻译引擎返回的结果无效");
    public static final ErrorCode TEXT_TOO_LONG         = ErrorCode.of("TRANSLATION_TEXT_TOO_LONG", 422, "待翻译文本过长");
    public static final ErrorCode BUDGET_EXCEEDED       = ErrorCode.of("TRANSLATION_BUDGET_EXCEEDED", 429, "已达到本月翻译预算上限");
}
```

询盘翻译的调用顺序（系统设计 17.10）：

```text
inquiry 详情页“翻译为中文”
  1. 读取询盘（只读事务结束）；已有 message_translated 时直接返回，不调用
  2. translationApi.isTextTranslationAvailable() 为 false → 返回 404（按钮本不应显示）
  3. translationApi.translateText(text = message, targetLocale = "zh-CN", purpose = INQUIRY_MESSAGE)   ← 事务外
  4. 新事务：保存 inq_inquiry.message_translated、message_translated_at（乐观锁冲突时以先保存者为准）
  5. 审计：inquiry.translate（不记录原文与译文）
```

### 4.5 扩展点

本节给出 4.2.3 中全部扩展点与平台 SPI 的完整签名（`JobHandler`、`ScheduledTask` 已在 4.4.12 定义）。扩展点由实现方模块用 `@Extension` 声明，由收集方经 `ExtensionRegistry`（4.4.15）按当前站点的模块启用状态取得并调用。

**调用约定**（收集方负责执行，实现方负责遵守 4.1.3 的回调约束）：

| 扩展点 | 调用时机 | 在收集方事务中 | 实现抛出异常时 |
|---|---|---|---|
| `ContentTypeProvider`、`RelationTypeProvider`、`NotificationTemplateProvider`、`TemplateProvider` 的定义方法 | 启动流程第 6 步同步声明式资源；之后缓存 | 否 | 启动失败（定义错误属于编程错误） |
| `ContentTypeProvider.routeHints`、`PublishContributor`、`SeoVariableProvider` | 发布、预览、草稿哈希 | **是**（发布事务） | 发布中止并回滚；预览中记为该部分的 ERROR 问题 |
| `ContentImpactProvider`、`PlatformModule.analyzeDisableImpact` | 下线 / 回收站 / 删除 / 停用前 | 只读 | 记为一条 `ERROR` 影响项（来源为该模块），阻止操作 |
| `DataSourceProvider`、`DeliveryContributor`、`SiteBootstrapContributor`、`BreadcrumbProvider`、`SchemaOrgProvider` | Delivery 请求（虚拟线程并行） | 否（只读） | 该部分为空，记录日志与指标 `b2bweb.delivery.extension.error`；不影响整页 |
| `SeoRule`、`SitemapProvider`、`SearchDocumentProvider`、`MediaReferenceCollector` | 事件监听或任务（异步） | 监听器自己的事务 | 该条目失败并按事件或任务的规则重试；不影响其他实现 |
| `TranslatableContentProvider`、`TranslatableResourceProvider` | translation 的翻译任务 | translation 的短事务（`apply`） | 该任务项 `FAILED`，记录错误，可重试 |
| `ContentVariantProvider` | inquiry 创建询盘明细、前台询价篮校验 | 只读 | 明细不带型号，记录日志 |
| `PersonalDataProvider` | 数据主体请求工具（系统管理） | 每个站点一个事务 | 该步骤失败，请求保持处理中，可重试 |
| `PlatformModule` 生命周期 | 启动、启用 / 停用、彻底删除 | 模块管理器的事务 | 启动：模块 `FAILED` 并阻止启动；启用 / 停用：操作失败并回滚 |

#### 4.5.1 @Extension、ContentTypeScoped、KeyedExtension

```java
package com.b2bweb.api.extension;

/**
 * 声明扩展点实现（系统设计 3.7）。以 @Component 为元注解，实现类自动成为 Spring Bean，并由 ExtensionRegistry 收集。
 * 一个类可以实现多个扩展点接口。
 */
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
@org.springframework.stereotype.Component
public @interface Extension {

    /** 实现所属模块 ID（与 module.json 的 id 一致）；注册表据此按站点启用状态过滤。 */
    String module();

    /** 适用的内容类型；空表示全部。只影响 ExtensionRegistry.forContentType 的结果。 */
    String[] contentTypes() default {};

    /** 调用顺序，升序（相同时按模块 ID、类名）。 */
    int order() default ExtensionOrder.DEFAULT;
}

/** 常用顺序值。 */
public final class ExtensionOrder {
    private ExtensionOrder() {}
    /** 平台内置实现（core） */
    public static final int PLATFORM = 100;
    /** 内容类型的所属模块（如 card 部分、主 JSON-LD 节点） */
    public static final int OWNER = 500;
    public static final int DEFAULT = 1000;
    /** 依赖其他实现结果的后置实现（如 seo 的发布贡献读取最终标题） */
    public static final int LATE = 5000;
}

/**
 * 需要按内容类型动态判断是否适用的扩展点。注册表先按 @Extension.contentTypes 过滤，再调用 supports。
 * 默认适用于 @Extension.contentTypes 允许的全部类型。
 */
public interface ContentTypeScoped {
    default boolean supports(String contentType) { return true; }
}

/** 按 key 查找的扩展点（数据源、任务、定时任务、站点资源提供方等）。同一扩展点内 key 全局唯一。 */
public interface KeyedExtension {
    String key();
}
```

注册表在启动流程第 7 步校验（失败则启动失败，错误信息中文）：

| 检查 | 规则 |
|---|---|
| 模块 | `@Extension.module` 必须是构建中的模块；实现类必须位于 `com.b2bweb.<module>..`（ArchUnit 同样检查） |
| 接口 | 实现类至少实现一个 `com.b2bweb.api..` 中的扩展点接口；未标注 `@Extension` 的扩展点实现视为错误 |
| key | 同一扩展点内 `KeyedExtension.key()` 唯一 |
| 内容类型 | `contentTypes` 中的值必须是已注册的内容类型 |
| 扩展点特有规则 | 同一内容类型的 `PublishContributor.partKey()` 唯一、`card` 部分恰好一个提供方（4.5.3）；内容类型、关系类型、通知模板、资源类型的定义唯一 |

#### 4.5.2 ContentTypeProvider：内容类型

注册内容类型（系统设计 6.2），并为可路由类型的本地化提供路由提示。

- 收集方：content（启动时同步、发布时取路由提示）；url 经 content 获得（4.4.17）；Admin 经 content 的后台接口获得。实现方：page、product、article、taxonomy。

```java
package com.b2bweb.api.content;

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

/**
 * 内容类型定义（系统设计 6.2；label、translatable、sitemap、searchable、adminEditRoute 为框架补充）。
 *
 * @param type              ContentTypes.isValidFormat
 * @param module            所属模块 ID，必须等于实现的 @Extension.module
 * @param label             中文名称（后台显示）
 * @param defaultUrlPattern 默认 URL 规则，不含语言前缀，如 "/products/{slug}"；不可路由为空
 * @param templates         可用模板 key（第一个为默认）
 * @param vocabularies      适用的词汇表 key
 * @param layoutSlots       是否允许在模板插槽中放置组件
 * @param reviewable        是否参与审核流（站点再按类型配置是否启用审核）
 * @param translatable      是否由 AI 翻译（有对应的 TranslatableContentProvider）
 * @param sitemap           已发布的可路由本地化是否进入 sitemap
 * @param searchable        是否进入站内搜索索引
 * @param adminEditRoute    后台编辑页路由模板，如 "/product/products/{contentId}"（通知邮件、影响报告中的链接）
 */
public record ContentTypeDefinition(String type, String module, String label, boolean routable,
                                    @Nullable String defaultUrlPattern, List<String> templates,
                                    Set<String> vocabularies, boolean layoutSlots, boolean reviewable,
                                    boolean translatable, boolean sitemap, boolean searchable,
                                    String adminEditRoute) {
    public ContentTypeDefinition {
        templates = List.copyOf(templates);
        vocabularies = Set.copyOf(vocabularies);
        if (routable && (defaultUrlPattern == null || !defaultUrlPattern.startsWith("/"))) {
            throw new IllegalArgumentException("可路由类型必须声明以 / 开头的默认 URL 规则: " + type);
        }
    }
}

public record RouteHintsContext(long contentId, long localizationId, String contentType, String locale) {}
```

V1 注册（系统设计 6.2）：

| 实现方 | 类型 | routable | defaultUrlPattern | RouteHints |
|---|---|---|---|---|
| page | `PAGE` | 是 | `/{parentPath}/{slug}` | `parentContentId` = 父页面；首页 `localeRoot = true`；`fallbackParentContentId` = 父页面 |
| page | `GLOBAL_BLOCK` | 否 | — | — |
| product | `PRODUCT` | 是 | `/products/{slug}` | `fallbackParentContentId` = 主分类的落地页 |
| article | `ARTICLE`、`CASE_STUDY` | 是 | `/blog/{slug}`、`/case-studies/{slug}` | `fallbackParentContentId` = 文章栏目落地页 |
| article | `FAQ` | 否 | — | — |
| taxonomy | `TERM_PAGE` | 是 | `/product-category/{slug}` | `patternKey` 与 `defaultPatternOverride` 按词汇表（`/applications/{slug}`、`/industries/{slug}` 等）；`parentContentId` 为空（分类 URL 不含层级）；`fallbackParentContentId` = 上级分类落地页 |

#### 4.5.3 PublishContributor：发布贡献

发布前校验、向快照贡献本模块的部分、回滚时恢复草稿（系统设计 4.4、6.3、6.4）。同一套实现用于发布、预览与草稿哈希（`AssemblyPurpose`），保证三者一致。

- 收集方：content。实现方：product、article、page（`layout`）、taxonomy（`terms`）、seo（`seo`）；每个内容类型的所属模块提供 `card`。

```java
package com.b2bweb.api.content;

public interface PublishContributor extends com.b2bweb.api.extension.ContentTypeScoped {

    /**
     * 快照部分的 key：SnapshotParts.CARD / TERMS 为顶层保留部分，其他进入 payload.parts.{partKey}。
     * 同一内容类型内唯一。
     */
    String partKey();

    /** 校验草稿（必填、引用存在、布局 Schema 等）。只追加问题，不抛出业务异常。 */
    void validate(PublishContext ctx, ValidationResult result);

    /**
     * 产出本部分（只包含本内容自身的数据；其他内容、媒体、分类只保存 $content / $media / $term 引用）。
     * 返回 null 表示没有该部分。结果必须是确定性的：同一草稿多次调用得到相同的 JSON（数组顺序稳定）。
     */
    @Nullable JsonNode contribute(PublishContext ctx);

    /**
     * 回滚：把历史快照中的本部分恢复为本模块的草稿（ctx.purpose() = RESTORE）。
     * 历史快照中没有本部分时不调用。修订的 schemaVersion 较旧时由实现方迁移。
     */
    void restore(PublishContext ctx, JsonNode part);
}

/** 组装用途。 */
public enum AssemblyPurpose {
    /** 正式发布（写入修订） */
    PUBLISH,
    /** 预览与编辑器画布（不持久化） */
    PREVIEW,
    /** 计算 draft_hash（不持久化，不执行 validate） */
    DRAFT_HASH,
    /** 只校验（validateForPublish） */
    VALIDATE,
    /** 回滚恢复草稿 */
    RESTORE
}

/** 发布上下文。由 content 创建，同一次组装中所有贡献方共享（只读）。 */
public interface PublishContext {
    long siteId();
    long contentId();
    long localizationId();
    String contentType();
    String locale();
    TranslationMode translationMode();
    /** 源语言本地化 ID（源语言时为自身）。 */
    long sourceLocalizationId();
    AssemblyPurpose purpose();
    /** PUBLISH：将生成的修订号；RESTORE：被恢复的修订号；其他为 0。 */
    int revisionNo();
    /** PUBLISH 且该本地化从未发布过。 */
    boolean firstPublish();
    com.b2bweb.api.common.ActorRef actor();
    Instant now();
    /** 当前已发布快照中的某部分（seo 比较覆盖值、增量判断）；未发布为空。 */
    Optional<JsonNode> publishedPart(String partKey);
    /** 编辑器传入的未保存部分（PREVIEW 且 PreviewRequest.partOverrides 含该 key）；贡献方应以它代替草稿。 */
    Optional<JsonNode> overridePart(String partKey);
}

/** 快照结构常量（系统设计 6.4）。 */
public final class SnapshotParts {
    private SnapshotParts() {}
    /** 快照结构版本（payload.schemaVersion）。 */
    public static final int SCHEMA_VERSION = 1;
    /** 快照大小上限（序列化后的字节数）。 */
    public static final int MAX_BYTES = 1_048_576;
    /** 顶层：卡片 { title, summary, coverMediaId }，由内容类型的所属模块提供；投影到 pub_title / pub_summary / pub_cover_media_id。 */
    public static final String CARD = "card";
    /** 顶层：发布态分类指派 [{ vocabulary, termId, primary, sort }]，由 taxonomy 提供；投影到 cnt_published_term。 */
    public static final String TERMS = "terms";
    /** parts.layout：模板与插槽或完整布局文档（page）。 */
    public static final String LAYOUT = "layout";
    /** parts.seo：最终值、自动值、被覆盖字段列表（seo，系统设计 14.1）。 */
    public static final String SEO = "seo";
}

/** card 部分的结构（由所属模块序列化为 JSON）。 */
public record SnapshotCard(@NotBlank String title, @Nullable String summary, @Nullable Long coverMediaId) {}
```

组装规则（content 实现）：

| 步骤 | 规则 |
|---|---|
| 收集 | `ExtensionRegistry.forContentType(PublishContributor.class, type)`，按 `order` |
| 校验 | 先对全部贡献方调用 `validate`（`ValidationResult.forSource(模块 ID)`），再追加内核自身的校验（`card.title` 必填、slug、路径可用）；存在 `ERROR` 时 `PUBLISH` 抛出 `ValidationFailedException(CONTENT_PUBLISH_VALIDATION_FAILED)`，`PREVIEW` 照常组装并返回问题 |
| 组装 | `{ schemaVersion, content: { id, type, locale, revisionNo }, card, terms, parts: { … } }`；`card` 缺失时为 `ERROR`（`CONTENT_CARD_MISSING`） |
| 哈希 | 按键排序的规范 JSON 计算 SHA-256（`payload_hash` / `draft_hash`）；`content.revisionNo` 不参与哈希 |
| 大小 | 超过 `MAX_BYTES` 抛出 `CONTENT_SNAPSHOT_TOO_LARGE` |
| 目标语言 | AUTO 模式的目标语言同样调用全部贡献方：贡献方读取该语言的草稿（由翻译回写），语言无关数据从源语言读取（系统设计 5.5） |

#### 4.5.4 ContentImpactProvider：影响分析

下线、移入回收站、彻底删除、停用模块之前的影响分析（被哪些页面引用、导航是否指向它、有多少入站链接等，系统设计 3.6、13.6）。

- 收集方：content（`ContentApi.analyzeImpact`）、core（停用模块时，与 `PlatformModule.analyzeDisableImpact` 合并）。实现方：seo（链接索引）、navigation（菜单项）、graph（人工关系）。

```java
package com.b2bweb.api.content;

public interface ContentImpactProvider {

    /** 内容级或本地化级操作的影响。只读；不得调用 content 的写方法。 */
    ImpactReport analyze(ContentImpactContext ctx);

    /** 停用模块对其内容类型的影响（如“导航中有 5 个菜单项指向文章”）。 */
    default ImpactReport analyzeModuleDisable(ModuleImpactContext ctx) { return ImpactReport.none(); }
}

/**
 * @param locale 本地化级操作（UNPUBLISH）的语言；内容级操作（TRASH、PURGE）为空
 * @param action UNPUBLISH / TRASH / PURGE
 */
public record ContentImpactContext(long siteId, long contentId, String contentType,
                                   @Nullable String locale, ImpactAction action) {}

/** @param contentTypes 被停用模块拥有的内容类型 */
public record ModuleImpactContext(long siteId, String moduleId, Set<String> contentTypes) {
    public ModuleImpactContext { contentTypes = Set.copyOf(contentTypes); }
}
```

- `ImpactItem.severity`：`ERROR` 阻止操作（如 V1 无）；`WARNING` 需要用户确认（如“该产品被 3 个页面引用，下线后对应卡片将被移除”）；`INFO` 仅提示。
- 后台在确认对话框中展示合并后的 `ImpactReport`；用户确认后再调用 `unpublish` / `trash` / `purge`，content 不重复分析。

#### 4.5.5 TemplateProvider：模板

提供模板定义（区域、插槽、固定区域的组件与数据源，系统设计 10.2）。模板元数据来自共享包 `frontend/packages/component-schemas`，构建时复制到 page 模块的 classpath，由 page 读取并注册。

- 收集方：delivery（渲染）、Admin（经 page 的后台接口：插槽编辑器、模板选择）。实现方：page。

```java
package com.b2bweb.api.delivery;

public interface TemplateProvider {

    /** 全部模板定义。 */
    List<TemplateDefinition> templates();

    /** 按 key 查找。 */
    default Optional<TemplateDefinition> template(String key) {
        return templates().stream().filter(t -> t.key().equals(key)).findFirst();
    }
}

/**
 * @param label 中文名称
 * @param legal 法律页面模板：目标语言的机器译文不自动发布（系统设计 5.5、10.1）
 */
public record TemplateDefinition(String key, int version, Set<String> contentTypes, String label,
                                 List<TemplateRegion> regions, boolean legal) {
    public TemplateDefinition {
        contentTypes = Set.copyOf(contentTypes);
        regions = List.copyOf(regions);
    }
}

public enum RegionKind { FIXED, SLOT }

/**
 * @param component 固定区域渲染的组件类型
 * @param allowed   插槽允许的组件类型
 * @param max       插槽最多放置的组件数
 * @param source    固定区域的数据源（读取时解析，修改不需要重新发布内容）
 * @param defaults  插槽的默认 Section 列表（JSON 数组，结构同布局文档 sections）
 */
public record TemplateRegion(String key, RegionKind kind, @Nullable String component, Set<String> allowed,
                             int max, @Nullable DataSourceSpec source, JsonNode defaults) {
    public TemplateRegion { allowed = Set.copyOf(allowed); }
}
```

#### 4.5.6 DataSourceProvider：数据源

组件数据绑定（系统设计 10.5）：按条件查询已发布内容、相关内容、子分类、FAQ、带分面的产品列表。数据源只读发布态数据。

- 收集方：delivery（按 `$source.provider` 经 `ExtensionRegistry.byKey` 查找）；Admin 经 delivery 的后台接口读取 `paramsSchema` 生成参数表单。实现方：content（`content-query`）、graph（`related`）、taxonomy（`term-children`）、article（`faq-by-context`）、search（`search-query`）。

```java
package com.b2bweb.api.delivery;

public interface DataSourceProvider extends com.b2bweb.api.extension.KeyedExtension {

    /** 数据源 key，小写连字符，如 "content-query"。 */
    @Override
    String key();

    /** 中文名称（后台参数表单标题）。 */
    String label();

    /** 参数的 JSON Schema（draft 2020-12）；后台据此生成表单，delivery 据此校验参数。 */
    JsonNode paramsSchema();

    /** Schema 之外的业务校验（如分类项存在）；在保存布局与发布校验时调用。 */
    default void validateParams(JsonNode params, ValidationResult result) {}

    /**
     * 解析数据。结果中的对象引用一律使用 $content / $media / $term，由 delivery 统一解析为 refs 并生成 c / m / t 缓存标签；
     * 列表类缓存标签（list:{type}、g:{contentId}）由实现方放入 cacheTags。
     * 必须在 500 ms 内返回，最多 request.limit() 条（≤ 24）；只返回 request.locale() 已发布的内容（不跨语言回退）。
     */
    DataSourceResult resolve(DataSourceRequest request);
}

/**
 * @param params           已用 paramsSchema 校验、并已把 "$current" 替换为当前内容 ID 的参数
 * @param currentContentId 当前页面的内容（模板固定区域、插槽）；全局区块与独立页面为空
 * @param sectionId        发起解析的 Section ID（模板区域为区域 key）
 * @param limit            min(参数中的 limit, 24)
 */
public record DataSourceRequest(long siteId, String locale, DeliveryMode mode, JsonNode params,
                                @Nullable Long currentContentId, @Nullable String currentContentType,
                                String sectionId, int limit, RefResolver refs) {}

/**
 * @param data      写入 resolve 响应的 dataSources[sectionId]，如 { "items": [ { "$content": "…" } ] }
 * @param itemCount 结果条数（指标与日志）
 */
public record DataSourceResult(JsonNode data, int itemCount, Set<CacheTag> cacheTags) {
    public DataSourceResult { cacheTags = Set.copyOf(cacheTags); }

    public static DataSourceResult empty() {
        return new DataSourceResult(tools.jackson.databind.node.JsonNodeFactory.instance.objectNode(), 0, Set.of());
    }
}

/** 数据源限制（系统设计 10.5）。 */
public final class DataSourceLimits {
    private DataSourceLimits() {}
    public static final int MAX_ITEMS = 24;
    public static final int MAX_SOURCES_PER_PAGE = 10;
    public static final Duration TIMEOUT = Duration.ofMillis(500);
    /** 参数中的上下文变量：当前内容 ID。 */
    public static final String CURRENT = "$current";
}
```

#### 4.5.7 DeliveryContributor：交付贡献

向 `resolve` 结果贡献一个顶层部分（系统设计 14.2、24.4）。delivery 先完成路由、快照、面包屑与数据源，再并行调用贡献方，最后统一解析引用（`refs`）与汇总缓存标签。

- 收集方：delivery。实现方：seo（`seo`：title、meta、alternates、jsonLd）；框架补充：page（`globalBlocks`：布局中 `global-block` 组件引用的全局区块的已发布布局）、form（`forms`：布局中表单组件引用的表单定义，系统设计 24.4“通常已内嵌在 resolve 结果中”）。
- 保留的顶层 key（贡献方不得使用）：`kind`、`locale`、`route`、`content`、`refs`、`dataSources`、`breadcrumbs`、`cache`。
- 贡献方输出中的 `$content` / `$media` / `$term` 由 delivery 统一解析进 `refs`；输出中的 `$source`（全局区块内的数据源）由 delivery 在合并后解析进 `dataSources`。

```java
package com.b2bweb.api.delivery;

public interface DeliveryContributor extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 顶层部分的 key，如 "seo"、"forms"、"globalBlocks"；全局唯一。 */
    String sectionKey();

    /** 只读发布态数据（LIVE）或预览快照（PREVIEW / EDITOR）；不得写库。 */
    void contribute(DeliveryContext ctx, DeliveryOutput out);
}

/** 交付上下文（delivery 实现）。 */
public interface DeliveryContext {
    long siteId();
    String locale();
    DeliveryMode mode();
    long contentId();
    long localizationId();
    String contentType();
    /** LIVE 为当前修订号；PREVIEW 草稿与 EDITOR 为 0。 */
    int revisionNo();
    /** 快照（结构见 4.5.3）。 */
    JsonNode snapshot();
    /** 当前路径（含语言前缀）；PREVIEW 中未发布的本地化为空。 */
    @Nullable String path();
    /** 规范地址：主域名 https + 路径，去除查询参数（分页参数除外，系统设计 14.2、14.3）。 */
    String canonicalUrl();
    /** 请求的查询参数（seo 按分页与筛选参数决定 robots 与 canonical）。 */
    Map<String, List<String>> queryParameters();
    /**
     * 布局中引用的 key：delivery 按组件 Schema 中标注 "x-ref": "<kind>" 的属性收集（框架补充，同步系统设计 10.4）。
     * referencedKeys("form") → 表单 key；referencedKeys("global-block") → 全局区块 key。
     */
    Set<String> referencedKeys(String refKind);
    /** 面包屑（已加首页与当前页，4.5.9）。 */
    List<BreadcrumbItem> breadcrumbs();
    RefResolver refs();
}

/**
 * 交付输出收集器（4.1.1 允许的可变容器）。delivery 为每个贡献方创建一个实例，非线程安全。
 */
public final class DeliveryOutput {
    private final Map<String, JsonNode> sections = new java.util.LinkedHashMap<>();
    private final Set<CacheTag> cacheTags = new java.util.LinkedHashSet<>();
    private ReferenceSet references = ReferenceSet.empty();
    private @Nullable Duration maxAge;

    /** 写入一个顶层部分（同 key 覆盖）。DeliveryContributor 只能写入自己的 sectionKey()。 */
    public DeliveryOutput put(String section, JsonNode value) { sections.put(section, value); return this; }

    public DeliveryOutput addCacheTag(CacheTag tag) { cacheTags.add(tag); return this; }

    public DeliveryOutput addCacheTags(Collection<CacheTag> tags) { cacheTags.addAll(tags); return this; }

    /** 额外的引用（输出 JSON 中的 $content / $media / $term 会被自动扫描，不必重复登记）。 */
    public DeliveryOutput addReferences(ReferenceSet refs) { references = references.merge(refs); return this; }

    /** 收紧响应的缓存时长（多个贡献方取最小值）。 */
    public DeliveryOutput capMaxAge(Duration value) {
        maxAge = (maxAge == null || value.compareTo(maxAge) < 0) ? value : maxAge;
        return this;
    }

    public Map<String, JsonNode> sections() { return Map.copyOf(sections); }
    public Set<CacheTag> cacheTags() { return Set.copyOf(cacheTags); }
    public ReferenceSet references() { return references; }
    public Optional<Duration> maxAge() { return Optional.ofNullable(maxAge); }
}
```

| 实现方 | sectionKey | 内容 | 缓存标签 |
|---|---|---|---|
| seo | `seo` | `title`、`meta[]`、`alternates[]`（只含已发布且对外公开的语言）、`jsonLd`（收集 `SchemaOrgProvider` 组装 `@graph`） | 被 hreflang 引用的本地化已含在 `c:{contentId}` 中 |
| page | `globalBlocks` | `{ key: { contentId, revisionNo, layout } }`，只含该语言已发布的全局区块 | `c:{blockContentId}`（系统设计 10.6） |
| form | `forms` | `{ formKey: 公开表单定义（该语言的标签、选项、告知文本与版本） }`；该语言没有表单本地化的表单不输出（组件渲染为空） | `f:{formId}` |

#### 4.5.8 SiteBootstrapContributor：站点引导

向 `GET /api/public/v1/delivery/site` 的站点引导数据贡献内容（系统设计 21.2、24.4）。

- 收集方：delivery。实现方：navigation（`navigation`）、tracking（`tracking`：同意横幅、统计与脚本配置）、core（`site`：站点资料、主题设置、语言列表、界面文案）。
- 结果按 `s{site}:nav`、`s{site}:settings` 等标签缓存；引导数据只读发布态或“保存即生效”的站点资源。

```java
package com.b2bweb.api.delivery;

public interface SiteBootstrapContributor {

    /** 顶层部分的 key，如 "navigation"、"tracking"、"site"；全局唯一。 */
    String sectionKey();

    void contribute(SiteBootstrapContext ctx, DeliveryOutput out);
}

/** @param locale 当前请求的语言（必须对外公开；PREVIEW / EDITOR 可以是未公开语言） */
public record SiteBootstrapContext(long siteId, String locale, DeliveryMode mode, RefResolver refs) {}
```

#### 4.5.9 BreadcrumbProvider：面包屑

按内容类型生成面包屑中间段（系统设计 16.2）。delivery 在前面加“首页”（文案键 `breadcrumb.home`，路径为语言首页），在后面加当前页（card 标题，不带链接）；结果同时用于前台展示与 `BreadcrumbList`（seo）。

- 收集方：delivery、seo。实现方：page（父页面链）、product（产品列表页 › 主分类祖先路径）、article（博客 › 文章栏目）、taxonomy（分类祖先路径）。
- 只输出当前语言已发布的节点（经 `ctx.refs()` 解析）；某个祖先未发布时跳过该节点，不中断。

```java
package com.b2bweb.api.delivery;

public interface BreadcrumbProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 中间段，从上到下（不含首页与当前页）。同一内容类型只使用 order 最小的一个实现。 */
    List<BreadcrumbItem> trail(BreadcrumbContext ctx);
}

public record BreadcrumbContext(long siteId, String locale, long contentId, String contentType,
                                JsonNode snapshot, RefResolver refs) {}

/**
 * @param name 该语言的显示名（内容标题、分类名或文案）
 * @param path 站内路径；当前页为空
 */
public record BreadcrumbItem(String name, @Nullable String path,
                             @Nullable Long contentId, @Nullable Long termId) {}
```

#### 4.5.10 SchemaOrgProvider：结构化数据

生成 JSON-LD 节点，由 seo 组装为一个 `@graph`（系统设计 14.5）。

- 收集方：seo（在其 `DeliveryContributor` 中调用）。实现方：core（`Organization`、`WebSite`，范围 `SITE`）、product（`Product`）、article（`Article` / `BlogPosting`）、page（`FAQPage`、`VideoObject`、页面类型细化）。seo 自己输出 `WebPage` 与 `BreadcrumbList`。
- 节点用 `ctx` 提供的 `@id` 互相引用；多个实现返回相同 `@id` 的节点时按 `order` 浅合并（后者覆盖同名属性），如 page 以 `webPageId()` 返回 `{ "@type": "ContactPage" }` 细化页面类型。
- 图片、链接一律经 `ctx.refs()` 解析为绝对 URL；不得输出未发布内容的链接。

```java
package com.b2bweb.api.seo;

public interface SchemaOrgProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /** SITE：每个页面都输出（Organization、WebSite）；CONTENT：只对 supports 的内容类型输出。 */
    default SchemaOrgScope scope() { return SchemaOrgScope.CONTENT; }

    /** 返回 JSON-LD 节点（不含 @context）；没有时返回空列表。 */
    List<JsonNode> nodes(SchemaOrgContext ctx);
}

public enum SchemaOrgScope { SITE, CONTENT }

public interface SchemaOrgContext {
    long siteId();
    String locale();
    com.b2bweb.api.delivery.DeliveryMode mode();
    /** 当前内容；站点级页面（如搜索页）为空。 */
    Optional<Long> contentId();
    Optional<String> contentType();
    Optional<JsonNode> snapshot();
    /** 当前页面的规范 URL。 */
    String pageUrl();
    /** {origin}/#organization */
    String organizationId();
    /** {origin}/#website */
    String websiteId();
    /** {pageUrl}#webpage */
    String webPageId();
    /** {pageUrl}#breadcrumb */
    String breadcrumbId();
    /** {pageUrl}#{fragment}，如 nodeId("product") */
    String nodeId(String fragment);
    List<com.b2bweb.api.delivery.BreadcrumbItem> breadcrumbs();
    com.b2bweb.api.delivery.RefResolver refs();
}
```

#### 4.5.11 SeoVariableProvider：SEO 模板变量

为 SEO 标题与描述模板提供变量（如 `{modelNo}`、`{primaryCategory}`、`{brand}`，系统设计 14.1）。seo 在自己的 `PublishContributor` 中计算自动值时调用，因此与发布同一事务、同一快照语义。

- 收集方：seo。实现方：product、article、taxonomy。内置变量 `{title}`、`{summary}`、`{siteName}` 由 seo 自己提供。

```java
package com.b2bweb.api.seo;

public interface SeoVariableProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 本实现提供的变量（后台模板编辑器的变量提示）。 */
    List<SeoVariableDefinition> variables();

    /** 变量值（变量名不含花括号）；值为该语言的纯文本，没有值的变量不放入结果（模板中替换为空串）。在发布事务中调用。 */
    Map<String, String> resolve(com.b2bweb.api.content.PublishContext ctx);
}

/** @param name 变量名，如 "modelNo"；@param label 中文说明；@param example 示例值 */
public record SeoVariableDefinition(String name, String label, @Nullable String example) {}
```

#### 4.5.12 SeoRule：SEO 健康检查规则

SEO 健康检查规则（系统设计 14.9）。发布后异步检查单个页面；每晚对全站检查一次（重复检测需要全站数据）。结果保存在 `seo_audit`，评分 = 100 − Σ 扣分（ERROR 15、WARNING 5、INFO 1）。

- 收集方：seo。实现方：seo（通用规则）、product（产品专属规则，如规格表为空）。

```java
package com.b2bweb.api.seo;

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

public enum SeoRuleScope { PAGE, SITE }

/**
 * @param code    {MODULE}_{REASON}，前端据此显示说明与修复入口
 * @param message 中文说明
 * @param field   相关字段路径（如 "seo.title"、"layout.sections[sec_01J9X4K2]"）
 */
public record SeoIssue(String ruleId, com.b2bweb.api.common.Severity severity, String code, String message,
                       @Nullable String field) {}

public interface SeoAuditContext {
    long siteId();
    long contentId();
    long localizationId();
    String contentType();
    String locale();
    /** 当前发布的快照。 */
    JsonNode snapshot();
    /** 该页面的头部输出（与 DeliveryContributor 的 seo 部分同源）。 */
    SeoHead head();
    /** 入站链接（seo_link，不计 sitemap）。 */
    List<SeoLinkRef> inboundLinks();
    /** 出站链接。 */
    List<SeoLinkRef> outboundLinks();
    /** 全站索引；只在 SITE 范围的全站检查中可用，PAGE 检查中调用抛出 IllegalStateException。 */
    SeoSiteIndex siteIndex();
    com.b2bweb.api.delivery.RefResolver refs();
}

/** @param canonical 绝对 URL；@param alternates hreflang → 绝对 URL（含 x-default） */
public record SeoHead(String title, @Nullable String description, String robots, String canonical,
                      Map<String, String> alternates, JsonNode jsonLd) {
    public SeoHead { alternates = Map.copyOf(alternates); }
}

public enum LinkKind { NAV, BREADCRUMB, RELATED, BODY, COMPONENT }

public record SeoLinkRef(long sourceLocalizationId, long targetContentId, LinkKind kind) {}

/** 全站数据（同站点、同语言）。 */
public interface SeoSiteIndex {
    /** 使用相同标题的其他本地化。 */
    List<Long> localizationsWithSameTitle(String locale, String title);
    /** 使用相同描述的其他本地化。 */
    List<Long> localizationsWithSameDescription(String locale, String description);
}
```

#### 4.5.13 SitemapProvider：非内容 URL（预留）

让非内容类 URL 进入 sitemap（系统设计 14.6）。V1 没有实现方；sitemap 主体由 seo 基于内容投影（`ContentApi.scanPublished`）生成。

```java
package com.b2bweb.api.seo;

public interface SitemapProvider extends com.b2bweb.api.extension.KeyedExtension {

    /** sitemap 文件类型段，小写连字符，如 "showroom"（文件名 /sitemaps/{key}-{locale}-{n}.xml）。 */
    @Override
    String key();

    /** 分页返回条目；只返回该语言对外公开、可索引、canonical 指向自身的 URL。 */
    CursorPage<SitemapEntry> entries(SitemapContext ctx, CursorRequest cursor);
}

/** @param origin 主域名 origin，如 "https://www.example.com" */
public record SitemapContext(long siteId, String locale, String origin) {}

/**
 * @param path       站内路径（含语言前缀）
 * @param alternates hreflang → 站内路径（xhtml:link）
 * @param imageMediaIds image:image（公开图片）
 */
public record SitemapEntry(String path, @Nullable Instant lastModified, Map<String, String> alternates,
                           List<Long> imageMediaIds) {
    public SitemapEntry {
        alternates = Map.copyOf(alternates);
        imageMediaIds = List.copyOf(imageMediaIds);
    }
}
```

#### 4.5.14 SearchDocumentProvider：搜索文档

根据快照构建搜索文档的类型专属字段（系统设计 8.2、19）。search在4.10.4的私有代次构建中调用，补充公共字段（`id`、`type`、`url`、`published_at`、`terms`的名称、修订号）并逐任务await；活跃代次不接受事件直接upsert。

- 收集方：search。实现方：product、article、page。
- 只读快照（不读草稿表）；结果只含该语言的文本。

```java
package com.b2bweb.api.extension;

public interface SearchDocumentProvider extends ContentTypeScoped {

    /** 构建文档字段；该内容不应进入搜索（如设置了 noindex）时返回空，search 随之删除已有文档。 */
    Optional<SearchDocumentFields> build(SearchDocumentContext ctx);

    /** 本类型的分面字段声明（attr.* 的类型与可筛选、可排序），用于索引设置。 */
    default List<SearchFacetDefinition> facets() { return List.of(); }
}

public record SearchDocumentContext(long siteId, long contentId, long localizationId, String contentType,
                                    String locale, int revisionNo, JsonNode snapshot,
                                    com.b2bweb.api.delivery.RefResolver refs) {}

/**
 * @param bodyText   去除标记的正文（≤ 10,000 个字符，超出由 search 截断）
 * @param termIds    参与筛选的分类项（发布态）
 * @param attributes 分面字段：key 为不带 "attr." 前缀的属性 key，值为字符串、数值、布尔或其数组
 * @param modelNos   型号（关闭拼写容错的字段）
 * @param sort       业务排序值（可排序字段 sort）
 */
public record SearchDocumentFields(String title, @Nullable String summary, String bodyText, Set<Long> termIds,
                                   JsonNode attributes, List<String> modelNos, @Nullable Long coverMediaId,
                                   int sort) {
    public SearchDocumentFields {
        termIds = Set.copyOf(termIds);
        modelNos = List.copyOf(modelNos);
    }
}

public enum FacetType { STRING, NUMBER, BOOLEAN }

/** @param key 不带 "attr." 前缀；@param label 中文名称（后台） */
public record SearchFacetDefinition(String key, String label, FacetType type, boolean filterable, boolean sortable) {}
```

#### 4.5.15 RelationTypeProvider：关系类型

注册内容关系类型（系统设计 15.2）。启动时同步到 `grh_relation_type`。

- 收集方：graph。实现方：graph（`RELATED`、`RECOMMENDED`、`EXCLUDED`、`USES` / `USED_IN`、`CASE_OF` / `HAS_CASE`）、product（`ACCESSORY_OF` / `HAS_ACCESSORY`、`REPLACED_BY` / `REPLACES`）。

```java
package com.b2bweb.api.extension;

public interface RelationTypeProvider {
    List<RelationTypeDefinition> relationTypes();
}

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
```

#### 4.5.16 MediaReferenceCollector：媒体引用收集

M0补充方案（ADR-019，Proposed）：收集草稿与资源的完整媒体集合，供源保存事务通过ReferenceWriteApi登记；完整锁与删除协议见4.10.1。当前发布态由content直接扫描本次组装的快照。

- 收集方：content（草稿）、资源拥有者/core（资源）；实现方：product、article、page、seo、core、navigation。
- 触发：updateDraftMeta/markDraftChanged与资源保存的原事务；异步事件不覆盖权威引用。media只实现MediaReferenceSink，不回查ContentApi。

```java
package com.b2bweb.api.media;

public interface MediaReferenceCollector extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 内容某本地化的草稿（含语言无关数据）引用的媒体。只收集站点资源的实现覆盖 supports 返回 false。 */
    default List<MediaReference> collectDraft(MediaReferenceContext ctx) { return List.of(); }

    /** 本实现负责的站点资源类型（TranslatableResourceTypes 中的值）。 */
    default Set<String> resourceTypes() { return Set.of(); }

    /** 某站点资源引用的媒体。 */
    default List<MediaReference> collectResource(String resourceType, String resourceId) { return List.of(); }
}

public record MediaReferenceContext(long siteId, long contentId, String contentType, long localizationId,
                                    String locale) {}

/** @param part 快照部分或资源字段组；@param fieldPath 字段路径（与 MediaUsageInfo 一致） */
public record MediaReference(long mediaId, String part, String fieldPath) {}
```

#### 4.5.17 ContentVariantProvider：内容子项

提供内容下可选择的子项（产品型号），用于询价篮与询盘明细（系统设计 3.5、8.3、17.5）。

- 收集方：inquiry（创建询盘明细时取型号快照，校验 `variantId` 属于该内容）。实现方：product。

```java
package com.b2bweb.api.content;

public interface ContentVariantProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /** 内容在指定语言的已发布版本中的子项（来自快照，不读草稿）；未发布的内容不出现在结果中。≤ 500 个内容。 */
    Map<Long, List<ContentVariant>> variants(Collection<Long> contentIds, String locale);
}

/**
 * @param variantId 子项 ID（如 prd_model.id）
 * @param code      型号，如 "X200-24V"（不翻译）
 * @param label     该语言的说明（可空）
 */
public record ContentVariant(long variantId, long contentId, String code, @Nullable String label, int sort) {}
```

#### 4.5.18 PersonalDataProvider：个人数据

数据主体请求（访问、可携带、更正、删除、反对、限制，系统设计 17.11）：按邮箱查找、导出、更正、匿名化本模块保存的个人数据。

- 收集方：core（数据主体请求工具，系统管理接口，权限 `system:dsr:manage`）。实现方：inquiry（询盘、活动备注、附件关联）、form（提交记录、附件）、notification（发件箱收件人与载荷）。
- **按站点调用**：core 对每个站点用 `SiteContextProvider.runAs(siteId, …)` 建立站点上下文后调用（`ExtensionRegistry.allForSite`），实现方照常使用带站点过滤的查询，不得绕过行级拦截器（4.1.3）。
- 邮箱一律规范化后比较（去除首尾空白、小写；不处理 Gmail 点号等服务商规则）。
- 删除即匿名化：替换联系字段、保留统计字段；附件经 `MediaApi.delete(mediaId, true)` 删除；实现必须幂等（重复执行不报错，计数为 0）。

```java
package com.b2bweb.api.extension;

public interface PersonalDataProvider extends KeyedExtension {

    /** 数据集 key：{模块 ID}.{名称}，如 "inquiry.inquiries"、"form.submissions"。 */
    @Override
    String key();

    /** 中文名称（数据主体请求报告中的分组标题）。 */
    String label();

    /** 查找：返回该站点中与邮箱相关的记录摘要；没有时 recordCount = 0。 */
    PersonalDataSummary find(PersonalDataRequest request);

    /** 导出（访问、可携带）：结构化 JSON，字段名为英文 camelCase，值为原文。 */
    JsonNode export(PersonalDataRequest request);

    /** 更正联系字段；返回更新的记录数。 */
    int rectify(PersonalDataRequest request, PersonalDataCorrection correction);

    /** 删除（匿名化）；返回处理的记录数。 */
    int erase(PersonalDataRequest request);

    /** 反对：撤销营销同意；返回处理的记录数。 */
    default int objectToMarketing(PersonalDataRequest request) { return 0; }

    /** 限制处理：标记或取消标记（被限制的记录不再用于跟进与通知）；返回处理的记录数。 */
    default int restrict(PersonalDataRequest request, boolean restricted) { return 0; }
}

/**
 * @param dsrRequestId    sys_dsr_request.id（审计与幂等）
 * @param normalizedEmail 规范化后的邮箱
 * @param handler         处理人
 */
public record PersonalDataRequest(long dsrRequestId, long siteId, String normalizedEmail,
                                  com.b2bweb.api.common.ActorRef handler) {}

/** @param categories 数据类别（中文），如 “联系信息”“留言”“附件”“归因数据” */
public record PersonalDataSummary(String key, long siteId, int recordCount, List<String> categories,
                                  @Nullable Instant earliest, @Nullable Instant latest) {
    public PersonalDataSummary { categories = List.copyOf(categories); }
}

/** 为空的字段表示不修改。 */
public record PersonalDataCorrection(@Nullable String email, @Nullable String name, @Nullable String phone,
                                     @Nullable String company, @Nullable String jobTitle,
                                     @Nullable String country) {}
```

#### 4.5.19 NotificationTemplateProvider：通知模板

注册默认通知模板（中文，系统设计 18）。启动流程第 6 步按站点同步到 `ntf_template`：缺失的模板以默认正文创建；站点修改过的模板不覆盖，默认正文有新版本时在后台提示。客户邮件模板（`CUSTOMER`）作为站点资源翻译（资源类型 `notification.template`）。

- 收集方：notification。实现方：inquiry、content；框架补充：form（下载链接邮件）、translation（翻译失败通知）。

```java
package com.b2bweb.api.notification;

public interface NotificationTemplateProvider {
    List<NotificationTemplateDefinition> templates();
}

/**
 * @param key       {模块 ID}.{场景}.{接收方}，前缀必须是实现所属的模块 ID
 * @param name      中文名称（后台模板列表）
 * @param subject   主题模板（中文）
 * @param body      正文模板（中文，HTML；变量引用语法与转义规则见 notification 模块设计文档）
 * @param version   默认正文版本（变化时提示站点“有新版默认模板”）
 */
public record NotificationTemplateDefinition(String key, String name, NotificationAudience audience,
                                             NotificationChannel channel, String subject, String body,
                                             List<TemplateVariable> variables, int version) {
    public NotificationTemplateDefinition { variables = List.copyOf(variables); }
}

/** @param label 中文说明；@param example 示例值（后台预览） */
public record TemplateVariable(String name, String label, boolean required, @Nullable String example) {}
```

V1 模板（模板 key 按 fw 3.7 的 `{模块 ID}.{场景}.{接收方}` 规则统一，右列为系统设计 18 中的写法，需同步修订）：

| 实现方 | key | 受众 | 触发 | 系统设计 18 |
|---|---|---|---|---|
| inquiry | `inquiry.assigned.staff` | STAFF | 分配、改派 | 同 |
| inquiry | `inquiry.autoreply.customer` | CUSTOMER | 询盘创建（仅 ACCEPT） | 同 |
| inquiry | `inquiry.sla.staff` | STAFF | SLA 超时提醒 | `inquiry.sla.reminder` |
| content | `content.review.staff` | STAFF | `ReviewRequestedEvent`（审核人） | `content.review.requested` |
| content | `content.reviewed.staff` | STAFF | `ReviewCompletedEvent`（提交人） | （6.7“审核结果”） |
| content | `content.outdated.staff` | STAFF | `TranslationOutdatedEvent`（翻译审校人员） | `translation.outdated` |
| translation | `translation.failed.staff` | STAFF | `TranslationFailedEvent` | `translation.failed` |
| form | `form.download.customer` | CUSTOMER | DOWNLOAD 表单提交通过 | `download.link` |

#### 4.5.20 TranslatableContentProvider：可翻译内容

从源语言提取翻译单元，把译文写回目标语言草稿（系统设计 3.7、5.6、5.7）。每个内容类型可以有多个实现（所属模块、page 的插槽布局、seo 的覆盖值），translation 合并它们的单元，按实现所属模块给 `key` 加前缀存储。

- 收集方：translation。实现方：product、article、page（布局中 `x-translatable` 的属性）、taxonomy（TERM_PAGE 的分类描述）、seo（源语言的覆盖值）。
- **提取来源**：以 `ctx.content().sourceRevisionNo()` 对应的源语言**发布快照**为准（`ContentApi.findRevision`，或实现方自己的等价数据）；不读取源语言之后尚未发布的草稿修改（框架补充：系统设计 3.7 写作“源语言草稿”，发布后的草稿可能含未发布修改，从快照提取避免把它们带入译文）。
- **回写**：translation在外部引擎调用前捕获expectedTargetVersion并写入trn_job_item；同一回写事务先调用lockDraftForWrite校验该值，再让非拥有者provider（如seo）apply，拥有者provider最后apply并以ctx.expectedTargetVersion()调用一次updateDraftMeta、写入译文标题并重算所有parts的哈希。除拥有者之外的provider不递增内容version；整个事务失败全部回滚。资源上下文expectedTargetVersion为空。非文本结构来自同一源修订，不借此覆盖人工新版本。
- **部分回写**：`translations` 可能只含部分 key（校验未通过的单元、翻译编辑器中单段修订）：缺少的 key 保留目标语言的现值；目标语言没有现值时留空（该本地化处于 `NEEDS_REVIEW`，不会自动发布）。
- **安全**：译文是纯文本数据（系统设计 26.3）：按字段的格式写入，`RICH_INLINE` 只按编号占位标签还原行内标记，不解析其中的任何其他标记，不执行、不拼接为指令。
- MANUAL 模式的本地化不调用 `apply`；`extract` 仍用于对比源语言两个修订的变化字段（OUTDATED 面板）。

```java
package com.b2bweb.api.translation;

public interface TranslatableContentProvider extends com.b2bweb.api.extension.ContentTypeScoped {

    /**
     * 提取翻译单元（ctx.subject() 为 Content）。key 在本实现内唯一且跨修订稳定（如布局属性用 Section ID 而不是下标）。
     * 空文本不提取。只读。
     */
    List<TranslationUnit> extract(TranslationContext ctx);

    /** 回写译文（key → 译文，key 为 extract 返回的 key）。在 translation 的事务中调用；必须幂等。 */
    void apply(TranslationContext ctx, Map<String, String> translations);

    /**
     * 该内容的机器译文是否必须人工审校后才能发布（如 page 对 legal 模板返回 true，系统设计 5.5）。
     * 任一实现返回 true，TranslationCompletedEvent.humanReviewRequired 即为 true。
     */
    default boolean requiresHumanReview(TranslationContext ctx) { return false; }
}
```

#### 4.5.21 TranslatableResourceProvider：可翻译站点资源

提取与回写站点资源中的可翻译文本（系统设计 3.7、5.5、5.7）。资源变化时，所属模块在保存资源的事务中发布 `TranslatableResourceChangedEvent`（4.6.11），translation 据此提取、翻译、回写；资源保存即生效，不走内容发布流程。

- 收集方：translation（按资源类型经 `supports` 选择实现）。实现方：navigation、form、media、product、taxonomy、notification、core、tracking。
- 回写规则同 4.5.20（部分回写、纯文本、幂等）；资源的源语言文本只用中文录入，目标语言文本只经 `apply` 写入（人工修订经翻译编辑器 → 翻译记忆 `HUMAN` → `apply`）。
- 表单的告知或营销同意文本译文变化时，form 在 `apply` 中递增该语言的 `privacy_notice_version` 并追加 `frm_form_notice_version`（系统设计 17.1）。

```java
package com.b2bweb.api.translation;

public interface TranslatableResourceProvider {

    /** 本实现负责的资源类型（TranslatableResourceTypes 中的值）。 */
    Set<String> resourceTypes();

    default boolean supports(String resourceType) { return resourceTypes().contains(resourceType); }

    /** 提取资源的翻译单元（ctx.subject() 为 Resource）；资源不存在时返回空列表。 */
    List<TranslationUnit> extract(TranslationContext ctx);

    /** 写回目标语言（ctx.targetLocale()）的资源文本；资源已删除时忽略。必须幂等。 */
    void apply(TranslationContext ctx, Map<String, String> translations);

    /** 当前站点该类型的全部资源 ID（开放新语言、术语表变化后的批量翻译）；按 ID 升序，游标稳定。 */
    CursorPage<String> resourceIds(String resourceType, CursorRequest cursor);
}

/** V1 可翻译资源类型：{模块 ID}.{名称}（与 4.3.10、4.4.4 一致），resourceId 的含义见注释。 */
public final class TranslatableResourceTypes {
    private TranslatableResourceTypes() {}
    /** 菜单（菜单项标签与可见性）；resourceId = menuId */
    public static final String NAVIGATION_MENU = "navigation.menu";
    /** 表单（标签、选项、提示语、成功提示、告知与营销同意文本）；resourceId = formId */
    public static final String FORM = "form.form";
    /** 媒体 alt、标题、说明；resourceId = mediaId */
    public static final String MEDIA = "media.media";
    /** 产品属性分组名；resourceId = groupId */
    public static final String PRODUCT_ATTRIBUTE_GROUP = "product.attribute_group";
    /** 产品属性名、单位说明与枚举选项名；resourceId = attributeId */
    public static final String PRODUCT_ATTRIBUTE = "product.attribute";
    /** 分类名（分类描述随 TERM_PAGE 作为内容翻译）；resourceId = termId */
    public static final String TAXONOMY_TERM = "taxonomy.term";
    /** 客户邮件模板（只限 CUSTOMER 受众）；resourceId = 模板 key */
    public static final String NOTIFICATION_TEMPLATE = "notification.template";
    /** 站点资料中的文本；resourceId = siteId */
    public static final String SITE_PROFILE = "core.site_profile";
    /** 前台界面文案；resourceId = 文案命名空间（如 "form"、"common"），一个命名空间一个资源 */
    public static final String UI_TEXT = "core.ui_text";
    /** 作者职务与简介；resourceId = authorId（4.10.2，Proposed） */
    public static final String ARTICLE_AUTHOR = "article.author";
    /** Cookie 横幅与同意设置文案；resourceId = siteId */
    public static final String CONSENT_BANNER = "tracking.consent_banner";
}
```

#### 4.5.22 PlatformModule：模块生命周期 SPI

每个模块提供一个实现（模块描述类 `<Id>Module`，fw 3.7），由 core 的模块管理器在启动流程与启用 / 停用 / 彻底删除时调用（系统设计 3.6）。`PlatformModule` 不需要 `@Extension` 注解（模块管理器按 Bean 类型收集，且不按站点过滤）。

```java
package com.b2bweb.api.extension;

public interface PlatformModule {

    /** 读取 module.json 得到的描述（由 core 提供的 ModuleDescriptor.load 解析 classpath 中的 module.json）。 */
    ModuleDescriptor descriptor();

    /** 首次安装：迁移已由平台执行；在此初始化默认数据（如默认表单、内置字典 DictApi.ensureBuiltin）。必须幂等。 */
    default void onInstall(ModuleContext ctx) {}

    /** 版本升级：数据修正（结构迁移由 Flyway 完成）。必须幂等。 */
    default void onUpgrade(ModuleContext ctx, com.b2bweb.api.common.SemVer fromVersion) {}

    /** 每次启动：安装/升级后按依赖拓扑顺序同步声明资源；必须幂等，不使用站点过滤。 */
    default void onStartup(ModuleContext ctx) {}

    /** 站点启用：初始化该站点的默认数据（首次启用时）。在站点上下文中调用。 */
    default void onEnable(SiteModuleContext ctx) {}

    /** 站点停用：停止该站点的后台处理（数据保留，系统设计 3.8）。在站点上下文中调用。 */
    default void onDisable(SiteModuleContext ctx) {}

    /** 停用前的影响分析，例如“该站点有 120 篇已发布文章将不可访问”。 */
    default com.b2bweb.api.common.ImpactReport analyzeDisableImpact(SiteModuleContext ctx) {
        return com.b2bweb.api.common.ImpactReport.none();
    }

    /** 彻底删除模块数据（仅 CLI / 系统管理员，所有站点均已停用，需二次确认且已备份）。 */
    default void onPurge(ModuleContext ctx) {}
}

/**
 * module.json 的描述（字段与附录 A 的 Schema 一致；未列出的字段保留在 raw 中）。
 *
 * @param layer    "L0" / "L2" / "L3" / "L4"
 * @param platform 兼容的平台版本范围，如 ">=1.0.0 <2.0.0"
 * @param requires 必需依赖：模块 ID → 版本范围
 * @param optional 可选依赖：模块 ID → 版本范围
 */
public record ModuleDescriptor(String id, String name, com.b2bweb.api.common.SemVer version, String layer,
                               String platform, Map<String, String> requires, Map<String, String> optional,
                               boolean disableable, @Nullable String tablePrefix, @Nullable String apiPrefix,
                               List<String> permissions, Set<String> publishes, Set<String> subscribes,
                               JsonNode raw) {
    public ModuleDescriptor {
        requires = Map.copyOf(requires);
        optional = Map.copyOf(optional);
        permissions = List.copyOf(permissions);
        publishes = Set.copyOf(publishes);
        subscribes = Set.copyOf(subscribes);
    }
}

/** 平台级生命周期上下文（安装、升级、彻底删除）；没有站点上下文。 */
public interface ModuleContext {
    String moduleId();
    /** 本次安装 / 升级的目标版本。 */
    com.b2bweb.api.common.SemVer version();
    /** 全部未归档站点的 ID。 */
    List<Long> siteIds();
    /** 依次以每个未归档站点为上下文执行（SiteContextProvider.runAs），用于为已有站点初始化数据。 */
    void forEachSite(java.util.function.LongConsumer action);
    com.b2bweb.api.common.ActorRef actor();
}

/** @param firstEnable 该站点是否第一次启用本模块 */
public record SiteModuleContext(long siteId, String moduleId, com.b2bweb.api.common.ActorRef actor,
                                boolean firstEnable) {}
```

### 4.6 领域事件

本节给出 4.2.4 中全部领域事件的定义。机制见系统设计 4.1：发布方在业务事务中调用 Spring 的 `ApplicationEventPublisher.publishEvent`，Spring Modulith 的 Event Publication Registry 在同一事务中写入 `EVENT_PUBLICATION`；事务提交后，订阅方用 `@ApplicationModuleListener` 在新事务中异步处理；未完成的投递在启动时与定时任务中重新投递（至少一次，ADR-003）。

**发布规则**（发布方遵守，PR 评审与 ArchUnit 检查）：

| 规则 | 说明 |
|---|---|
| 时机 | 在改变状态的同一事务中发布，事务回滚则事件不存在；不得在事务外发布（ArchUnit：调用 `publishEvent` 的方法或其所在类须标注 `@Transactional`） |
| 元数据 | 只用 `EventMetaFactory`（4.4.14）生成 `meta`；以 system 身份处理时用 `next(siteId, ActorRef.SYSTEM)` |
| 载荷 | 只带 ID 与必要的事实（系统设计 4.2）；不带完整实体与快照；**不带个人数据**（姓名、邮箱、电话、IP、留言）、密钥、待翻译原文与译文 |
| 例外 | 订阅方受调用方向限制无法回查发布方时（4.1.3，如 notification 不能调用 content），事件携带渲染通知所需的最少文本（如中文标题），在字段注释中注明 |
| 粒度 | 一个聚合的一次变化发布一条；批量操作逐条发布（如源语言下线时每个跟随下线的本地化一条）；派生事件可以合并（如 `RelationChangedEvent` 单条最多 1000 个 ID） |
| 站点 | 站点级事件 `siteId` 非空；只有系统级事件（`UserDeactivatedEvent` 的 `DEACTIVATED`）为空 |
| 命名与位置 | `<聚合><过去分词>Event`，record，位于 `com.b2bweb.api.event`；事件专用的枚举与值对象同包 |

**订阅规则**（订阅方遵守）：

| 规则 | 说明 |
|---|---|
| 幂等 | 至少一次投递，监听器必须幂等。三种方式任选：① **按当前状态重算**并整体替换（推荐：如 delivery 重复清除缓存；media同步引用和search事务日志按4.10，不再异步覆盖权威集合）；② **业务唯一键**拒绝重复（如 inquiry 以 `submission_id` 唯一）；③ **按 `eventId` 去重**：在监听事务中写入 platform-core 的已处理事件记录（第 5 章），重复时跳过 |
| 顺序 | 不保证顺序。同一本地化的发布类事件按 `(revisionNo, occurredAt)` 比较，丢弃不新于已处理值的事件；或者不信任载荷中的状态，处理时经服务接口读取当前状态（如 `ContentApi.findLocalization`、`UrlApi.pathOf`） |
| 站点上下文 | platform-core 的监听器包装按 `siteId` 建立站点上下文（`ContextOrigin.EVENT`，4.3.1）；`siteId` 为空的系统级事件没有站点上下文，订阅方用 `SiteContextProvider` 逐站点执行 |
| 模块停用 | 订阅方模块在事件所属站点停用时，监听器包装跳过执行并标记完成（系统设计 3.8；`deliverWhenModuleDisabled()` 为 true 的事件除外）；重新启用后由该模块的全量任务补齐（如 search 重建索引、graph 全量计算） |
| 操作人 | 监听器以 system 身份执行（`CurrentUserProvider.current()` 为空）；需要原操作人时读取 `actor()` |
| 失败 | 抛出异常则该投递保持未完成并按 Modulith 规则重新投递；永久性失败（如引用的对象已删除）应记录日志后正常返回，不得无限重试 |
| 耗时 | 单次处理应在 10 秒内完成；更长的工作（全量重建、批量翻译）提交为任务（`JobApi`、translation 的 `trn_job`）后返回 |

#### 4.6.1 DomainEvent 与 EventMeta

框架补充：系统设计 4.2 的示意代码把公共字段（`eventId`、`schemaVersion`、`occurredAt`、`siteId`、`actor`）平铺在每个事件中；本框架把除 `schemaVersion` 外的公共字段集中为第一个组件 `EventMeta meta`，`DomainEvent` 的访问方法名与语义不变。这样每个事件只需一个公共组件，发布方写作 `new XxxEvent(eventMetaFactory.next(), …)`。系统设计 4.2 需同步。

```java
package com.b2bweb.api.event;

/**
 * 领域事件（系统设计 4.2）。全部事件都是本包中的 record，第一个组件为 meta。
 * JSON 形式：{ "meta": { "eventId", "occurredAt", "siteId", "actor" }, "schemaVersion": 1, …事件字段 }；
 * ID 一律序列化为字符串（4.1.5）。
 */
public interface DomainEvent {

    /** 公共元数据，由 EventMetaFactory 生成。 */
    EventMeta meta();

    /**
     * 事件结构版本：默认 1。发生不兼容变更时由事件类覆盖本方法返回新版本，并按 4.1.8 过渡。
     * 序列化时作为只读属性输出，反序列化时忽略（以类为准）。
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "schemaVersion",
            access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    default int schemaVersion() { return 1; }

    /** TSID 的十进制字符串。 */
    default String eventId() { return meta().eventId(); }

    default Instant occurredAt() { return meta().occurredAt(); }

    /** 站点 ID；系统级事件为空。 */
    default @Nullable Long siteId() { return meta().siteId(); }

    /** 用户 ID 的十进制字符串，或 "system"。 */
    default String actor() { return meta().actor(); }

    /** 事件类型名（类的简单名，如 "ContentPublishedEvent"），用于日志、指标与监控。 */
    default String eventType() { return getClass().getSimpleName(); }

    /**
     * 订阅方模块在该站点停用时是否仍然投递（框架补充）。默认 false：监听器包装跳过执行（系统设计 3.8）；
     * 清理类事件（ContentPurgedEvent）覆盖为 true，避免停用模块中留下孤儿数据。
     */
    default boolean deliverWhenModuleDisabled() { return false; }
}

/**
 * 事件公共元数据（系统设计 4.2）。只由 EventMetaFactory（4.4.14）创建；测试中用 testkit 的 TestEventMeta。
 *
 * @param eventId    TSID 的十进制字符串（IdGenerator.nextId）
 * @param occurredAt 发生时间（UTC，取自注入的 Clock）
 * @param siteId     站点 ID；系统级事件为空
 * @param actor      ActorRef.toEventActor()：用户 ID 的十进制字符串，或 "system"
 */
public record EventMeta(String eventId, Instant occurredAt, @Nullable Long siteId, String actor) {

    public EventMeta {
        if (!eventId.matches("^[0-9]{1,20}$")) throw new IllegalArgumentException("eventId 必须为 TSID 十进制字符串");
        if (!"system".equals(actor) && !actor.matches("^[1-9][0-9]{0,19}$")) {
            throw new IllegalArgumentException("actor 必须为用户 ID 或 system");
        }
    }

    /** 还原操作人（displayName 不可还原）。 */
    public com.b2bweb.api.common.ActorRef actorRef() { return com.b2bweb.api.common.ActorRef.fromEventActor(actor); }

    /** 站点级事件的站点 ID；系统级事件调用时抛出 IllegalStateException。 */
    public long requireSiteId() {
        if (siteId == null) throw new IllegalStateException("系统级事件没有 siteId");
        return siteId;
    }
}
```

发布与订阅示例：

```java
// 发布方（content 的应用服务，发布事务内）
events.publishEvent(new ContentPublishedEvent(eventMetaFactory.next(), contentId, contentType, locale,
        localizationId, revisionId, revisionNo, path, previousPath, firstPublish, contentChanged, PublishTrigger.MANUAL));

// 订阅方（seo），事务提交后异步执行，新事务；站点上下文由监听器包装建立
@ApplicationModuleListener
void on(ContentPublishedEvent event) {
    if (isStale(event.localizationId(), event.revisionNo(), event.occurredAt())) return;   // 顺序规则
    sitemapService.markDirty(event.locale());                                           // 天然幂等
}
```

下文各小节给出事件定义与“发布方 / 订阅方 / 幂等键”表；表中“幂等键”是订阅方用于去重或判断过期的字段组合。代码块省略 `import`：除本包类型外，`ActorRef` 在 `common`，`PublishTrigger`、`UnpublishStrategy`、`ReviewDecision` 在 `content`，`TranslationMode`、`TranslationState`、`TranslationSubject` 在 `translation`，`RedirectMatchType`、`RetireMode` 在 `url`，`MediaKind`、`MediaStatus`、`UploadSource` 在 `media`，`Visibility` 在 `port`，`FormPurpose`、`SpamVerdict` 在 `form`。

#### 4.6.2 内容事件（content 发布）

```java
package com.b2bweb.api.event;

/** 内容创建（ContentApi.create）。随后发布源语言的 LocalizationCreatedEvent。 */
public record ContentCreatedEvent(EventMeta meta, long contentId, String contentType, String ownerModule,
                                  long sourceLocalizationId) implements DomainEvent {}

/**
 * 草稿变化（框架补充，系统设计 12.1 草稿引用追踪）：updateDraftMeta 每次一条；markDraftChanged 与
 * sharedDataChanged = true 时为每个受影响的本地化各一条。
 *
 * @param version 变化后的本地化 version
 */
public record ContentDraftChangedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                       String locale, int version, boolean sharedDataChanged) implements DomainEvent {}

/**
 * 发布（系统设计 4.2、4.4）：人工发布、定时发布、译文自动发布都发布本事件。
 *
 * @param path           发布后的路径（含语言前缀）；不可路由类型为空
 * @param previousPath   上一次发布的路径；首次发布或路径未变时为空或等于 path
 * @param firstPublish   该本地化第一次发布
 * @param contentChanged 快照的 payload_hash 是否变化（为 false 时只是重新发布，订阅方可跳过重算）
 * @param trigger        发布方式（框架补充）：MANUAL、SCHEDULED、TRANSLATION
 */
public record ContentPublishedEvent(EventMeta meta, long contentId, String contentType, String locale,
                                    long localizationId, long revisionId, int revisionNo,
                                    @Nullable String path, @Nullable String previousPath,
                                    boolean firstPublish, boolean contentChanged,
                                    PublishTrigger trigger) implements DomainEvent {

    public boolean isSourceLocale() { return com.b2bweb.api.common.Locales.isSource(locale); }

    public boolean pathChanged() { return previousPath != null && path != null && !previousPath.equals(path); }
}

/**
 * 下线（ContentApi.unpublish / unpublishAll、定时下线）：每个本地化一条。
 *
 * @param lastRevisionNo    下线前发布的修订号（订阅方据此判断过期）
 * @param previousPath      下线前的路径；不可路由类型为空
 * @param redirectContentId strategy = REDIRECT_CONTENT 时的目标内容
 * @param followedSource    AUTO 本地化随源语言下线（系统设计 5.7）
 */
public record ContentUnpublishedEvent(EventMeta meta, long contentId, String contentType, String locale,
                                      long localizationId, int lastRevisionNo, @Nullable String previousPath,
                                      UnpublishStrategy strategy, @Nullable Long redirectContentId,
                                      boolean followedSource) implements DomainEvent {}

/** 移入回收站（内容级；此时已没有已发布的本地化）。 */
public record ContentTrashedEvent(EventMeta meta, long contentId, String contentType,
                                  List<Long> localizationIds) implements DomainEvent {
    public ContentTrashedEvent { localizationIds = List.copyOf(localizationIds); }
}

/** 从回收站恢复（恢复后全部本地化为未发布状态）。 */
public record ContentRestoredEvent(EventMeta meta, long contentId, String contentType) implements DomainEvent {}

/**
 * 彻底删除（系统设计 25.5）：内核数据已物理删除、路由已释放；各模块据此删除自己以 content_id 关联的数据。
 *
 * @param ownerModule 内容类型的所属模块（删除业务表的责任方）
 */
public record ContentPurgedEvent(EventMeta meta, long contentId, String contentType, String ownerModule,
                                 List<Long> localizationIds) implements DomainEvent {
    public ContentPurgedEvent { localizationIds = List.copyOf(localizationIds); }

    /** 停用模块同样必须清理自己的数据。 */
    @Override
    public boolean deliverWhenModuleDisabled() { return true; }
}

/** 本地化创建：create（源语言）、createLocalization（MANUAL）、ensureTargetLocalization（AUTO）。 */
public record LocalizationCreatedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                       String locale, TranslationMode translationMode) implements DomainEvent {}

/** 删除未发布的非源语言本地化（ContentApi.deleteLocalization）。 */
public record LocalizationDeletedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                       String locale) implements DomainEvent {}

/** 翻译模式切换（框架补充，系统设计 5.5“脱离 / 恢复自动翻译”）。 */
public record TranslationModeChangedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                          String locale, TranslationMode previousMode,
                                          TranslationMode translationMode) implements DomainEvent {}

/**
 * 源语言发布后，MANUAL 本地化被标记为 OUTDATED（系统设计 5.5）。
 *
 * @param sourceTitle             源语言标题（中文；notification 不能回查 content，用于邮件正文）
 * @param sourceRevisionNo        新发布的源语言修订号
 * @param basedOnSourceRevisionNo 该本地化依据的源语言修订号；从未记录时为空
 */
public record TranslationOutdatedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                       String locale, String sourceTitle, int sourceRevisionNo,
                                       @Nullable Integer basedOnSourceRevisionNo) implements DomainEvent {}

/**
 * 提交审核（系统设计 6.7）。
 *
 * @param title       该本地化的草稿标题（notification 用于邮件正文）
 * @param version     提交时的本地化 version（通知去重）
 * @param submittedBy 提交人用户 ID
 * @param comment     提交说明（员工填写的内部文本）
 */
public record ReviewRequestedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                   String locale, String title, int version, long submittedBy,
                                   @Nullable String comment) implements DomainEvent {}

/** 审核完成（通过或驳回）；submittedBy 为本轮提交人（通知对象），reviewedBy 为审核人。 */
public record ReviewCompletedEvent(EventMeta meta, long contentId, String contentType, long localizationId,
                                   String locale, String title, ReviewDecision decision, long submittedBy,
                                   long reviewedBy, @Nullable String comment) implements DomainEvent {}
```

| 事件 | 发布时机 | 订阅方与处理 | 幂等键 / 过期判断 |
|---|---|---|---|
| `ContentCreatedEvent` | `create` | graph：初始化该内容的关系设置（默认 AUTO 模式） | `contentId`（upsert） |
| `ContentDraftChangedEvent` | `updateDraftMeta`、`markDraftChanged` | 媒体引用已按4.10.1在源事务登记；本事件仅作诊断/源模块修复提示，不异步覆盖引用 | 按当前状态重算；`(localizationId, version)` 不新于已处理值时跳过 |
| `ContentPublishedEvent` | `publish`（含定时、译文自动发布） | delivery：清除 `c:{id}`，每次发布加 `list:{type}`，路径变化加旧路径条目（系统设计 23.3）；seo：sitemap、IndexNow、链接索引、健康检查；graph：增量重算（30 秒合并）；search：唤醒4.10.4构建任务，不直接改活跃索引；media引用已在发布事务同步登记；translation：`locale` 为源语言时为 `SiteApi.autoTranslateLocales()` 创建翻译任务（系统设计 5.7） | `(localizationId, revisionNo)`；translation 以 `(contentId, revisionNo, targetLocale)` 去重 |
| `ContentUnpublishedEvent` | `unpublish`、`unpublishAll`、定时下线 | delivery：清除 `c:{id}`、`list:{type}`；seo：sitemap、IndexNow（旧 URL）；graph：从候选集与列表移除；search：按已捕获事务日志构建下一代；media发布态引用已在下线事务清除 | `(localizationId, lastRevisionNo, occurredAt)`；之后再次发布的 `ContentPublishedEvent` 修订号更大 |
| `ContentTrashedEvent` / `ContentRestoredEvent` | `trash` / `restore` | graph：排除 / 恢复候选；search：删除文档（恢复时无操作，再次发布时重建）；delivery：清除 `c:{id}` | `(contentId, occurredAt)`；处理时读取 `ContentInfo.trashed()` 当前值 |
| `ContentPurgedEvent` | `purge` | 所有以 `content_id` 保存数据的模块删除自己的行：所属模块删除业务表；taxonomy 删除草稿指派；graph、media、seo、search、translation 删除各自数据；inquiry 保留询盘明细中的快照（标题、型号、URL，系统设计 17.5），不处理 | `contentId`（删除天然幂等） |
| `LocalizationCreatedEvent` | 见注释 | seo：更新 hreflang 计划；delivery：清除 `c:{id}`（语言切换器）；translation：忽略 AUTO（自己创建）；MANUAL 登记到翻译中心的“独立编辑”列表，不自动翻译 | `localizationId` |
| `LocalizationDeletedEvent` | `deleteLocalization` | seo、delivery：同上；translation：取消该本地化的未完成任务项 | `localizationId` |
| `TranslationModeChangedEvent` | `changeTranslationMode` | translation：MANUAL → AUTO 时以源语言当前发布修订创建翻译任务（翻译记忆中的 `HUMAN` / `REVIEWED` 优先）；AUTO → MANUAL 时取消未完成的任务项 | `(localizationId, occurredAt)`；处理时读取当前 `translationMode` |
| `TranslationOutdatedEvent` | 源语言 `publish` 事务内 | notification：模板 `content.outdated.staff`，收件人 `UserDirectory.usersWithPermission("translation:memory:review", locale)` | `dedupKey = "outdated:{localizationId}:{sourceRevisionNo}"` |
| `ReviewRequestedEvent` | `submitForReview` | notification：模板 `content.review.staff`，收件人 `usersWithPermission(Permissions.forContent(type, REVIEW), locale)` | `dedupKey = "review:{localizationId}:{version}"` |
| `ReviewCompletedEvent` | `approve` / `reject` | notification：模板 `content.reviewed.staff`，收件人 `submittedBy` | `dedupKey = "reviewed:{eventId}"` |

- `ContentPurgedEvent` 的订阅方在模块设计文档“跨模块引用与清理”（模板 4.6）中逐表写明处理方式；停用的模块同样必须清理（`deliverWhenModuleDisabled() = true`，框架补充，需同步系统设计 3.8）。
- `translation:memory:review` 是 translation 声明的权限码（审校译文，可按语言限定数据范围，系统设计 5.8），由 translation 模块设计文档登记。

#### 4.6.3 URL 事件（url 发布）

```java
package com.b2bweb.api.event;

/** 通用变化类型（导航、表单、重定向等简单资源）。 */
public enum ChangeKind { CREATED, UPDATED, DELETED }

/** 路由变化的原因。 */
public enum PathChangeReason {
    /** 发布时首次获得路由或路径变化（UrlApi.applyRoute） */
    PUBLISHED,
    /** 父内容路径变化导致的子树重算（任务 url.recomputeSubtree，系统设计 13.4） */
    PARENT_MOVED,
    /** 下线退役（UrlApi.retireRoute）：currentPath 为空 */
    RETIRED
}

/**
 * 内容路由变化（系统设计 4.3、13.4）。路径未变化的 applyRoute 不发布；releaseRoutes（彻底删除）由 ContentPurgedEvent 覆盖。
 *
 * @param previousPath 变化前的路径（含语言前缀）；首次发布为空
 * @param currentPath  变化后的路径；RETIRED 为空
 * @param retireMode   RETIRED 时为 GONE（410）或 REDIRECT（301）；其他为空
 */
public record ContentPathChangedEvent(EventMeta meta, long contentId, long localizationId, String locale,
                                      @Nullable String previousPath, @Nullable String currentPath,
                                      PathChangeReason reason, @Nullable RetireMode retireMode) implements DomainEvent {}

/**
 * 手工与导入的重定向变化（addRedirect、后台编辑与删除、CSV 导入）。applyRoute / retireRoute 自动维护的 AUTO 重定向
 * 不发布本事件（由 ContentPathChangedEvent 覆盖）。
 *
 * @param redirectId 单条变化时的重定向 ID；批量导入时为空
 * @param sourcePath 单条变化时的源路径（EXACT 为规范化路径，PREFIX 为前缀，REGEX 为表达式）；批量时为空
 * @param matchType  单条变化时的匹配方式；批量时为空
 * @param count      本次变化的条数（单条为 1）
 */
public record RedirectChangedEvent(EventMeta meta, ChangeKind change, @Nullable Long redirectId,
                                   @Nullable String sourcePath, @Nullable RedirectMatchType matchType,
                                   int count) implements DomainEvent {}
```

| 事件 | 发布时机 | 订阅方与处理 | 幂等键 / 过期判断 |
|---|---|---|---|
| `ContentPathChangedEvent` | `applyRoute`（路径变化）、子树重算任务、`retireRoute` | seo：IndexNow 推送新旧 URL（只推送对外公开语言）、更新链接索引；delivery：清除 `previousPath`、`currentPath` 的路由解析缓存与 `c:{contentId}`（重定向响应同样带该标签）；content：`PARENT_MOVED` 时把投影 `pub_path` 同步为 `UrlApi.pathOf(localizationId)` 的当前值 | 按当前状态处理（`UrlApi.pathOf`）；IndexNow 以 URL 去重 |
| `RedirectChangedEvent` | `addRedirect`、后台编辑 / 删除、导入完成 | delivery：EXACT 单条清除该路径的解析缓存与 CDN URL；PREFIX、REGEX 与批量清除该站点全部解析缓存 | 清除天然幂等 |

#### 4.6.4 分类事件（taxonomy 发布）

```java
package com.b2bweb.api.event;

/** 分类项变化类型（系统设计 4.3：新建、更名、移动、删除）。 */
public enum TermChange {
    CREATED,
    /** 源语言（中文）名称变化；目标语言名称经翻译回写，见 TranslationCompletedEvent */
    RENAMED,
    /** 父项变化（物化路径随之变化） */
    MOVED,
    /** 其他属性变化：排序、落地页开关、图标等 */
    UPDATED,
    DELETED
}

/**
 * @param parentId          变化后的父项；根项为空
 * @param previousParentId  MOVED 时变化前的父项；其他为空
 * @param landingContentId  可路由分类项的 TERM_PAGE 内容
 * @param descendantTermIds MOVED / DELETED 时受影响的后代分类项（不含自身）；其他为空列表
 */
public record TermChangedEvent(EventMeta meta, long termId, String vocabularyKey, TermChange change,
                               @Nullable Long parentId, @Nullable Long previousParentId,
                               @Nullable Long landingContentId, List<Long> descendantTermIds) implements DomainEvent {
    public TermChangedEvent { descendantTermIds = List.copyOf(descendantTermIds); }
}
```

| 订阅方 | 处理 |
|---|---|
| graph | `MOVED`、`DELETED`：重算“同属一个父分类”受影响内容的候选与评分（经 `ContentApi.contentIdsByPublishedTerms`，30 秒合并） |
| search | `RENAMED`、`MOVED`：重建源语言中引用该分类项及其后代的文档的分类名与层级（任务）；`DELETED`：从文档中移除 |
| delivery | 清除 `t:{termId}` 与后代的 `t:{id}`；`RENAMED` 同时清除 `list:{type}`（列表筛选器显示分类名） |
| content | `DELETED`：从投影 `cnt_published_term` 删除该分类项（快照不变，下次发布时消失） |
| product | `MOVED`：重算以该分类及其后代为主分类的产品的属性集继承（`prd_category_attribute_set` 的子分类继承，系统设计 8.1） |
| navigation | `RENAMED`、`MOVED`、`DELETED`：菜单引用该分类项时更新菜单项状态（`DELETED` 标记为失效并在后台提示），并发布 `NavigationChangedEvent` |

幂等：按 `(termId, occurredAt)` 判断过期，处理时经 `TaxonomyApi` 读取当前状态。

#### 4.6.5 媒体事件（media 发布）

```java
package com.b2bweb.api.event;

/** 媒体属性变化。 */
public enum MediaChange {
    /** 源语言 alt、标题、说明或文件名（目标语言文本经翻译回写） */
    METADATA,
    /** 焦点 */
    FOCAL_POINT,
    /** 状态：病毒扫描完成（PROCESSING → READY / QUARANTINED） */
    STATUS,
    /** 所在文件夹 */
    FOLDER,
    /** 留资文档的门控表单（MediaApi.setDownloadGate） */
    DOWNLOAD_GATE
}

public record MediaUploadedEvent(EventMeta meta, long mediaId, MediaKind kind, Visibility visibility,
                                 MediaStatus status, UploadSource source) implements DomainEvent {}

public record MediaUpdatedEvent(EventMeta meta, long mediaId, MediaKind kind, Visibility visibility,
                                MediaStatus status, Set<MediaChange> changes) implements DomainEvent {
    public MediaUpdatedEvent { changes = Set.copyOf(changes); }
}

/** @param forced MediaApi.delete(force = true)（无引用追踪的私有附件清理） */
public record MediaDeletedEvent(EventMeta meta, long mediaId, MediaKind kind, Visibility visibility,
                                UploadSource source, boolean forced) implements DomainEvent {}
```

| 事件 | 订阅方与处理 | 幂等键 |
|---|---|---|
| `MediaUploadedEvent` | delivery、seo：V1 无处理（新媒体尚未被引用；订阅保留给 V2 的媒体 CDN 预热） | — |
| `MediaUpdatedEvent` | delivery：`METADATA`、`FOCAL_POINT`、`STATUS` 时清除 `m:{mediaId}`；seo：`METADATA`、`STATUS` 时更新图片 sitemap 条目并重新检查引用它的 OG 图片 | 清除天然幂等 |
| `MediaDeletedEvent` | delivery：清除 `m:{mediaId}`；seo：移除图片 sitemap 条目 | `mediaId` |

`METADATA` 变化时 media 还在同一事务中发布 `TranslatableResourceChangedEvent(media.media, mediaId)`（4.6.11）。

#### 4.6.6 导航事件（navigation 发布）

```java
package com.b2bweb.api.event;

/** 菜单变化（菜单项、排序、可见性、链接）。@param menuKey 菜单位置 key，如 "header"、"footer" */
public record NavigationChangedEvent(EventMeta meta, long menuId, String menuKey, ChangeKind change)
        implements DomainEvent {}
```

| 订阅方 | 处理 | 幂等键 |
|---|---|---|
| delivery | 清除 `nav` 标签（系统设计 23.3：相当于清除整站 HTML） | 天然幂等 |
| 引用维护 | 菜单保存事务已通过ReferenceWriteApi同步登记；本事件不写mda_usage | 不适用 |

菜单项标签变化时 navigation 同时发布 `TranslatableResourceChangedEvent(navigation.menu, menuId)`。

#### 4.6.7 平台事件（core 发布）

```java
package com.b2bweb.api.event;

/** 站点配置变化的范围。 */
public enum SiteChangeScope {
    /** 基本信息：名称、时区、默认对外语言、尾斜杠策略、主题、站点状态 */
    SITE,
    /** 域名：主域名、别名 */
    DOMAINS,
    /** 站点语言：增删、对外公开、自动翻译、URL 前缀、hreflang、排序 */
    LOCALES,
    /** 模块设置（SettingsApi）；settingKeys 列出变化的键 */
    SETTINGS,
    /** 站点资料（SiteProfileApi） */
    PROFILE,
    /** 前台界面文案（UiTextApi）；uiTextNamespaces 列出变化的命名空间 */
    UI_TEXT
}

public enum LocaleChangeType {
    ADDED, REMOVED,
    /** 设为对外公开 / 取消对外公开 */
    MADE_PUBLIC, MADE_PRIVATE,
    /** 开启 / 关闭自动翻译 */
    AUTO_TRANSLATE_ENABLED, AUTO_TRANSLATE_DISABLED,
    URL_PREFIX_CHANGED, HREFLANG_CHANGED, SORT_CHANGED
}

public record LocaleChange(String locale, LocaleChangeType type) {}

/**
 * 站点配置变化（系统设计 4.3）。一次保存一条，scopes 至少一项。
 *
 * @param settingKeys      SETTINGS：变化的设置键（{moduleId}.{名称}）；否则为空
 * @param localeChanges    LOCALES：逐项变化；否则为空
 * @param uiTextNamespaces UI_TEXT：变化的文案命名空间（如 "form"、"common"）；否则为空
 */
public record SiteSettingsChangedEvent(EventMeta meta, Set<SiteChangeScope> scopes, Set<String> settingKeys,
                                       List<LocaleChange> localeChanges, Set<String> uiTextNamespaces)
        implements DomainEvent {

    public SiteSettingsChangedEvent {
        if (scopes.isEmpty()) throw new IllegalArgumentException("scopes 不能为空");
        scopes = Set.copyOf(scopes);
        settingKeys = Set.copyOf(settingKeys);
        localeChanges = List.copyOf(localeChanges);
        uiTextNamespaces = Set.copyOf(uiTextNamespaces);
    }

    public boolean has(SiteChangeScope scope) { return scopes.contains(scope); }

    /** 某模块的设置是否变化（键以 "{moduleId}." 开头）。 */
    public boolean settingsOf(String moduleId) {
        return settingKeys.stream().anyMatch(k -> k.startsWith(moduleId + "."));
    }

    /** 某类语言变化涉及的语言代码。 */
    public Set<String> locales(LocaleChangeType type) {
        return localeChanges.stream().filter(c -> c.type() == type).map(LocaleChange::locale)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

/** 模块在站点启用（系统设计 3.6、3.8）。@param firstEnable 该站点第一次启用本模块 */
public record ModuleEnabledEvent(EventMeta meta, String moduleId, boolean firstEnable) implements DomainEvent {}

/** 模块在站点停用（数据保留）。 */
public record ModuleDisabledEvent(EventMeta meta, String moduleId) implements DomainEvent {}

public enum UserDeactivationReason {
    /** 用户被停用或删除（全局；meta.siteId 为空） */
    DEACTIVATED,
    /** 用户失去在某站点的全部角色（meta.siteId 为该站点） */
    REMOVED_FROM_SITE
}

/**
 * 用户不再能处理某些站点的工作（框架补充，系统设计 17.7“跳过停用的成员”）。
 *
 * @param siteIds DEACTIVATED：用户此前拥有角色的全部站点；REMOVED_FROM_SITE：只含该站点
 */
public record UserDeactivatedEvent(EventMeta meta, long userId, UserDeactivationReason reason,
                                   Set<Long> siteIds) implements DomainEvent {
    public UserDeactivatedEvent { siteIds = Set.copyOf(siteIds); }
}
```

`SiteSettingsChangedEvent` 的订阅方：

| 订阅方 | 关注的范围 | 处理 |
|---|---|---|
| delivery | `SITE`、`DOMAINS`、`LOCALES` | 清除 `s{site}`（整站）与域名解析缓存 |
| delivery | `SETTINGS`、`PROFILE`、`UI_TEXT` | 清除 `settings` 标签 |
| seo | `LOCALES`（公开状态、增删、前缀、hreflang）、`SITE`、`settingsOf("seo")` | 重建 sitemap 与 hreflang；robots 与 Meta 规则按新设置生效 |
| url | `LOCALES` 的 `URL_PREFIX_CHANGED`；`SITE`（尾斜杠策略） | 前缀变化：提交任务重算该语言全部路由并生成 301；尾斜杠变化：清除路由解析缓存 |
| search | `LOCALES` 的 `ADDED` / `REMOVED`；`settingsOf("search")` | 创建 / 删除该语言的索引 `s{site}_{locale}_content`；更新同义词、停用词等索引设置 |
| translation | `LOCALES` 的 `AUTO_TRANSLATE_ENABLED` / `AUTO_TRANSLATE_DISABLED`；`settingsOf("translation")` | 开启：为该语言创建批量任务（全部已发布内容与全部站点资源，系统设计 5.7“开放新语言”）；关闭：取消该语言未完成的任务项；引擎与预算设置即时生效 |
| core | `PROFILE` | 资料保存事务已通过ReferenceWriteApi登记引用；事件只通知缓存/翻译，不重写引用 |

其他平台事件：

| 事件 | 订阅方与处理 | 幂等键 |
|---|---|---|
| `ModuleEnabledEvent` | delivery：清除 `s{site}`；seo：重建 sitemap；search：为该模块的内容类型重建索引文档（任务）；graph：全量重算（任务） | `(siteId, moduleId, occurredAt)`；任务以 `idempotencyKey` 去重 |
| `ModuleDisabledEvent` | delivery：清除 `s{site}`；seo：重建 sitemap（排除该模块的类型）；search：删除该模块类型的文档；graph：排除并全量重算 | 同上 |
| `UserDeactivatedEvent` | inquiry：在 `siteIds` 的每个站点中（`SiteContextProvider` 切换），把该用户负责的未关闭询盘按分配规则重新分配（无匹配时给站点默认负责人），并从销售组轮询中排除 | 按当前状态处理（查询 `owner_user_id = userId` 的未关闭询盘），天然幂等 |

`PROFILE`、`UI_TEXT` 的源语言文本变化时，core 还在同一事务中发布 `TranslatableResourceChangedEvent`（`core.site_profile`、`core.ui_text`）。core 自己的应用内缓存（站点、域名、设置）经 Redis Pub/Sub 广播失效（系统设计 23.1），不经事件监听。

#### 4.6.8 关系事件（graph 发布）

```java
package com.b2bweb.api.event;

public enum RelationChangeSource {
    /** 后台人工维护（添加、删除、排序、排除、模式切换） */
    MANUAL,
    /** 事件触发的增量计算 */
    INCREMENTAL,
    /** 夜间或启用模块后的全量重建 */
    FULL_REBUILD,
    /** 关系类型或评分配置变化 */
    CONFIG
}

/**
 * 关系列表变化。graph 只在某内容的对外列表（前 N 名）实际变化时列入 contentIds。
 *
 * @param contentIds 关系列表变化的源内容，1–1000 个；更多时拆分为多条
 */
public record RelationChangedEvent(EventMeta meta, Set<Long> contentIds, RelationChangeSource source)
        implements DomainEvent {

    public static final int MAX_CONTENT_IDS = 1000;

    public RelationChangedEvent {
        if (contentIds.isEmpty() || contentIds.size() > MAX_CONTENT_IDS) {
            throw new IllegalArgumentException("contentIds 数量必须在 1–1000 之间");
        }
        contentIds = Set.copyOf(contentIds);
    }
}
```

订阅方：delivery 清除每个 `g:{contentId}`，随后直接清除 CDN；二者成功才完成可靠投递（4.10.3），重复清除安全。

#### 4.6.9 表单事件（form 发布）

```java
package com.b2bweb.api.event;

/**
 * 提交已保存（系统设计 17.2 第 7 步）。不含字段值与任何个人数据；inquiry 经 FormApi.findSubmission 读取。
 *
 * @param spamVerdict REJECT 同样发布（inquiry 不创建询盘；REJECT 记录的保留期清理由 form 负责）
 * @param test        测试提交（后台“测试表单”）
 */
public record FormSubmittedEvent(EventMeta meta, long submissionId, long formId, String formKey,
                                 FormPurpose purpose, String locale, SpamVerdict spamVerdict,
                                 boolean test) implements DomainEvent {}

/** 表单定义变化（框架补充）：字段、设置、状态。表单定义内嵌在页面的 resolve 结果中（4.5.7）。 */
public record FormChangedEvent(EventMeta meta, long formId, String formKey, ChangeKind change)
        implements DomainEvent {}
```

| 事件 | 订阅方与处理 | 幂等键 |
|---|---|---|
| `FormSubmittedEvent` | inquiry：`purpose` 为 INQUIRY / SAMPLE / DOWNLOAD / CONTACT 时创建询盘（系统设计 17.2 第 10–11 步）：ACCEPT 正常分配与通知；QUARANTINE 进入“待审”、不发通知；REJECT 与 CUSTOM 不创建 | `submissionId`（`inq_inquiry.submission_id` 唯一） |
| `FormChangedEvent` | delivery：清除 `f:{formId}`（4.3.7） | 清除天然幂等 |

标签、选项、提示语、告知文本变化时 form 还发布 `TranslatableResourceChangedEvent(form.form, formId)`。

#### 4.6.10 询盘事件（inquiry 发布）

V1 没有跨模块订阅方：Webhook 由 inquiry 在同一事务中直接调用 `NotificationApi.sendWebhookEvent`，不经事件。事件为 V2 的 crm、analytics 预留（系统设计 4.3），载荷不含联系人信息。询盘状态等枚举作为共享取值放在本包，inquiry 直接使用（4.1.1）。

```java
package com.b2bweb.api.event;

/** 询盘类型（系统设计 17.5），由表单用途决定。 */
public enum InquiryType { INQUIRY, SAMPLE, DOWNLOAD, CONTACT }

/** 询盘状态（系统设计 17.6）；CONVERTED 为 V2 的 CRM 预留。 */
public enum InquiryStatus { NEW, ASSIGNED, CONTACTED, QUALIFIED, CONVERTED, CLOSED }

/** 关闭原因（系统设计 17.6）。 */
public enum InquiryCloseReason { INVALID, SPAM, DUPLICATE, NO_RESPONSE, OTHER }

/** 分配方式（系统设计 17.7）。 */
public enum AssignmentMethod {
    /** 命中分配规则（指定用户） */
    RULE,
    /** 命中分配规则（销售组轮询） */
    ROUND_ROBIN,
    /** 无规则匹配，使用站点默认负责人 */
    DEFAULT_OWNER,
    /** 7 天内同一邮箱的重复询盘，沿用原负责人 */
    DUPLICATE_OWNER,
    /** 手动改派 */
    MANUAL
}

/**
 * @param inquiryNo     询盘编号，如 INQ-20261006-0001
 * @param locale        客户提交时的语言
 * @param duplicateOfId 重复询盘关联的原询盘
 */
public record InquiryCreatedEvent(EventMeta meta, long inquiryId, String inquiryNo, InquiryType type,
                                  long submissionId, String locale, SpamVerdict spamVerdict,
                                  @Nullable Long duplicateOfId, boolean test) implements DomainEvent {}

public record InquiryAssignedEvent(EventMeta meta, long inquiryId, String inquiryNo,
                                   @Nullable Long previousOwnerUserId, long ownerUserId,
                                   AssignmentMethod method) implements DomainEvent {}

/** @param closeReason status = CLOSED 时必填 */
public record InquiryStatusChangedEvent(EventMeta meta, long inquiryId, String inquiryNo,
                                        InquiryStatus previousStatus, InquiryStatus status,
                                        @Nullable InquiryCloseReason closeReason) implements DomainEvent {}
```

幂等键（V2 订阅方）：`InquiryCreatedEvent` 为 `inquiryId`；其他为 `(inquiryId, occurredAt)`，以发生时间判断过期。

#### 4.6.11 翻译事件（translation 发布）与站点资源变化事件

```java
package com.b2bweb.api.event;

/**
 * 一次翻译的统计（系统设计 5.10）。
 *
 * @param memoryHits       命中翻译记忆的单元数
 * @param machineCount     调用引擎生成的单元数
 * @param needsReviewCount 校验未通过、需要人工处理的单元数
 */
public record TranslationStats(int unitCount, int memoryHits, int machineCount, int needsReviewCount,
                               int inputTokens, int outputTokens) {}

/**
 * 一个对象（内容本地化或站点资源）在一个目标语言的译文已回写（系统设计 5.7 第 8 步）。content 与 resource 恰好一个非空。
 *
 * @param jobId               trn_job.id
 * @param content             内容翻译（含目标本地化 ID 与依据的源语言修订号）
 * @param resource            站点资源翻译
 * @param state               TRANSLATED 或 NEEDS_REVIEW
 * @param humanReviewRequired 任一 TranslatableContentProvider.requiresHumanReview 返回 true（法律页面、legal 模板，4.5.20）
 * @param autoPublish         translation 的结论：内容翻译、state = TRANSLATED、humanReviewRequired = false，
 *                            且站点开启了译文自动发布（默认开启，系统设计 5.7 第 7 步、6.7）
 */
public record TranslationCompletedEvent(EventMeta meta, long jobId,
                                        TranslationSubject.@Nullable Content content,
                                        TranslationSubject.@Nullable Resource resource,
                                        String targetLocale, TranslationState state,
                                        boolean humanReviewRequired, boolean autoPublish,
                                        TranslationStats stats, @Nullable Integer targetVersion) implements DomainEvent {

    public TranslationCompletedEvent {
        // ADR-024（M0草案）：内容事件必须带本次回写后共同版本；资源事件不带版本。
        if ((content != null && targetVersion == null) || (resource != null && targetVersion != null)) {
            throw new IllegalArgumentException("targetVersion只用于内容翻译且必填");
        }
        if ((content == null) == (resource == null)) throw new IllegalArgumentException("content 与 resource 必须恰好一个非空");
        if (state != TranslationState.TRANSLATED && state != TranslationState.NEEDS_REVIEW) {
            throw new IllegalArgumentException("state 只能是 TRANSLATED 或 NEEDS_REVIEW");
        }
        if (autoPublish && (content == null || state != TranslationState.TRANSLATED || humanReviewRequired)) {
            throw new IllegalArgumentException("只有无需人工审校、已通过校验的内容译文可以自动发布");
        }
    }

    public TranslationSubject subject() { return content != null ? content : resource; }
}

/**
 * 翻译失败：引擎错误且重试耗尽、预算用尽、提取或回写异常（系统设计 5.7、5.10）。content 与 resource 恰好一个非空。
 *
 * @param subjectTitle 内容的源语言标题或资源名称（中文；notification 不能回查，用于邮件正文）
 * @param errorCode    如 TRANSLATION_ENGINE_UNAVAILABLE、TRANSLATION_OUTPUT_INVALID、TRANSLATION_BUDGET_EXCEEDED
 * @param errorMessage 中文说明；不得包含原文、译文与引擎返回的正文
 * @param attempts     已尝试次数
 */
public record TranslationFailedEvent(EventMeta meta, long jobId,
                                     TranslationSubject.@Nullable Content content,
                                     TranslationSubject.@Nullable Resource resource,
                                     String targetLocale, String subjectTitle, String errorCode,
                                     String errorMessage, int attempts) implements DomainEvent {

    public TranslationFailedEvent {
        if ((content == null) == (resource == null)) throw new IllegalArgumentException("content 与 resource 必须恰好一个非空");
    }

    public TranslationSubject subject() { return content != null ? content : resource; }
}

public enum ResourceChange { UPSERTED, DELETED }

/**
 * 站点资源的源语言（中文）可翻译文本变化（系统设计 4.3、5.7；CLAUDE.md 语言规则 5）。
 * 所属模块在保存资源的同一事务中发布；只在可翻译文本实际变化（新建、修改）或资源删除时发布，
 * 只改非文本属性（排序、链接、图片、开关）时不发布。
 *
 * @param resourceType TranslatableResourceTypes 中的值（4.5.21）
 * @param resourceId   含义见 TranslatableResourceTypes 的注释
 */
public record TranslatableResourceChangedEvent(EventMeta meta, String resourceType, String resourceId,
                                               ResourceChange change) implements DomainEvent {}
```

| 事件 | 发布方 | 订阅方与处理 | 幂等键 / 过期判断 |
|---|---|---|---|
| `TranslationCompletedEvent` | translation | content：`autoPublish` 为 true 时，在新事务中以 system 身份、`PublishTrigger.TRANSLATION` 发布目标本地化；发布前确认该本地化仍为 AUTO、`translationState = TRANSLATED`、`sourceRevisionNo` 等于源语言当前发布修订号（否则为过期事件，跳过），并确认事件targetVersion等于当前目标version、内容级审校标记和当前provider策略均不要求人工审校（CLAUDE.md禁止15）；发布校验失败时把翻译状态置为 `NEEDS_REVIEW` 并记录原因。delivery：资源翻译按下表清除缓存标签（内容翻译只改草稿，线上缓存由随后的 `ContentPublishedEvent` 清除） | 内容：`(targetLocalizationId, sourceRevisionNo)`；资源：`(resourceType, resourceId, targetLocale, occurredAt)` |
| `TranslationFailedEvent` | translation | notification：模板 `translation.failed.staff`，收件人为拥有翻译管理权限的用户（权限码由 translation 模块设计文档登记） | `dedupKey = "trn-failed:{jobId}:{targetLocale}"` 加对象标识 |
| `TranslatableResourceChangedEvent` | article（`article.author`，4.10.2）、navigation（`navigation.menu`）、form（`form.form`）、media（`media.media`）、product（`product.attribute_group`、`product.attribute`）、taxonomy（`taxonomy.term`）、notification（`notification.template`，只限 CUSTOMER 受众）、core（`core.site_profile`、`core.ui_text`）、tracking（`tracking.consent_banner`） | translation：`UPSERTED` 时为 `SiteApi.autoTranslateLocales()` 的每个语言提取、翻译、回写（同一资源的连续变化合并处理）；`DELETED` 时取消未完成的任务项（翻译记忆保留） | `(resourceType, resourceId, targetLocale)`；任务执行时总是提取当前文本，重复事件只产生记忆命中 |

delivery 处理资源翻译时的标签映射（`TranslationCompletedEvent.resource`）：

| 资源类型 | 清除的标签 |
|---|---|
| `navigation.menu` | `nav` |
| `form.form` | `f:{resourceId}` |
| `media.media` | `m:{resourceId}` |
| `taxonomy.term` | `t:{resourceId}` |
| `core.site_profile`、`core.ui_text`、`tracking.consent_banner` | `settings` |
| `product.attribute_group`、`product.attribute` | `s{site}`（低频；按4.10.3直接可靠失效） |
| `notification.template` | 无（只用于邮件） |
| `article.author`（4.10.2） | `settings`；发布快照中的作者不自动改写 |
| 其他（V2 新增类型） | `s{site}`；新增资源类型时在本表登记更细的标签 |

### 4.7 基础设施端口

本节给出 4.2.5 中全部端口的完整签名。端口是 `platform-api` 中的技术能力接口，由 `backend/infrastructure` 的适配器实现（做法同系统设计 12.5、5.9）；业务模块只依赖端口，不依赖 infrastructure 与第三方客户端。

**端口约定**：

| 约定 | 说明 |
|---|---|
| 实现与装配 | 每个端口有一个默认适配器（`com.b2bweb.infrastructure.<技术>`，类名 `<技术><端口名>`，如 `S3CompatibleObjectStorage`），由 `application` 按配置 `b2bweb.infra.<端口>.type` 装配；testkit 为每个端口提供假实现（`InMemory<端口名>` / `Fake<端口名>`，见下表与第 9 章） |
| 使用范围 | 只有 4.2.5 表中列出的使用方可以注入该端口（ArchUnit 检查；如只有 translation 可以依赖 `TranslationEngine`，只有 media 与 core 可以依赖 `ObjectStorage`） |
| 站点隔离 | Redis 类端口（`TaggedCache`、`RateLimiter`、`DistributedLock`、`BufferStore`）自动给键加当前站点前缀 `s{siteId}:`，系统级用 `sys:`；对象键以站点 ID 开头；搜索索引名带站点（系统设计 5.3） |
| 配置与凭据 | 部署级配置（地址、桶名、默认 SMTP、imgproxy 密钥）来自 `application.yml` 与环境变量；站点级凭据（站点 SMTP、Turnstile 密钥、AI 引擎 API Key、Webhook 签名密钥）由使用方从自己的配置中读出、经 `SecretCipher` 解密后以 `Secret` 传入；适配器不读取业务表，不缓存明文 |
| 异常 | 远程失败抛出 `ExternalServiceException`（`retryable` 表示可以重试）；调用方输入导致的拒绝抛出 `BusinessException`；参数不合法抛出 `IllegalArgumentException`；第三方异常类型不得泄漏给调用方。错误码见 `InfraErrors` |
| 重试 | 除注明外（`TranslationEngine` 对 429 / 5xx 的退避重试）端口不自动重试；重试属于调用方（事件重新投递、任务重试、发件箱） |
| 超时与事务 | 每个远程调用都有超时（默认值见各小节，可按部署配置调整）；不得在持有数据库事务时调用远程端口（4.1.3）；纯计算端口（`ImageUrlSigner`、`GeoIpResolver`）除外 |
| 日志与观测 | 不记录密钥、个人数据、待翻译文本与译文；适配器输出指标 `b2bweb.infra.<端口>.calls / errors / latency`（标签 operation、outcome），纳入告警（系统设计 27.4） |
| 第三方库 | 客户端库只出现在 infrastructure 中，版本由 BOM 管理（ADR-011）；AI 引擎不使用任何厂商 SDK（CLAUDE.md 禁止 12） |

| 端口 | 默认适配器 | 本地 / 开发 | testkit 假实现 |
|---|---|---|---|
| `ObjectStorage` | `S3CompatibleObjectStorage` | MinIO；`LocalFileObjectStorage` | `InMemoryObjectStorage` |
| `ImageUrlSigner` | `ImgproxyImageUrlSigner` | imgproxy 容器 | `FakeImageUrlSigner`（可预测的未签名 URL） |
| `VirusScanner` | `ClamAvVirusScanner` | ClamAV 容器 | `FakeVirusScanner`（含 EICAR 字符串判为 INFECTED） |
| `MailTransport` | `SmtpMailTransport` | Mailpit | `InMemoryMailTransport`（记录已发送邮件） |
| `WebhookTransport` | `HttpWebhookTransport` | — | `FakeWebhookTransport`（记录请求，可设定应答） |
| `SearchEngineClient` | `MeilisearchSearchEngineClient` | Meilisearch 容器 | `InMemorySearchEngineClient`（子串匹配，支持过滤与分面） |
| `CdnPurger` | `CloudflareCdnPurger` | `NoopCdnPurger` | `RecordingCdnPurger` |
| `IndexNowClient` | `HttpIndexNowClient` | `NoopIndexNowClient` | `RecordingIndexNowClient` |
| `TranslationEngine` | `OpenAiCompatibleTranslationEngine` | 假引擎（第 11 章） | `FakeTranslationEngine`（确定性伪译文，保留占位标签与数字，可注入故障） |
| `CaptchaVerifier` | `TurnstileCaptchaVerifier` | Turnstile 测试密钥 | `FakeCaptchaVerifier`（令牌 `pass` 通过） |
| `GeoIpResolver` | `MaxMindGeoIpResolver`（本地 mmdb） | 同左 | `FakeGeoIpResolver`（IP → 国家映射表） |
| `MxRecordChecker` | `DnsMxRecordChecker` | 同左 | `FakeMxRecordChecker` |
| `RemoteFileFetcher` | `SafeHttpRemoteFileFetcher` | 同左 | `FakeRemoteFileFetcher`（URL → 文件映射） |
| `TaggedCache` | `RedisTaggedCache` | Redis 容器 | `InMemoryTaggedCache` |
| `RateLimiter` | `Bucket4jRedisRateLimiter` | Redis 容器 | `InMemoryRateLimiter`（使用可拨动时钟） |
| `DistributedLock` | `RedisDistributedLock` | Redis 容器 | `InMemoryDistributedLock` |
| `BufferStore` | `RedisBufferStore` | Redis 容器 | `InMemoryBufferStore`（使用可拨动时钟） |

```java
package com.b2bweb.api.port;

/**
 * 端口错误码。INFRA 是 infrastructure 的保留前缀（不是模块 ID，框架补充 4.1.7）；
 * 使用方通常把它们转换为自己的错误码再抛给前端（如 MEDIA_REMOTE_FETCH_FAILED、TRANSLATION_ENGINE_UNAVAILABLE）。
 */
public final class InfraErrors {
    private InfraErrors() {}
    public static final ErrorCode STORAGE_UNAVAILABLE       = ErrorCode.of("INFRA_STORAGE_UNAVAILABLE", 503, "对象存储暂时不可用");
    public static final ErrorCode STORAGE_OBJECT_NOT_FOUND  = ErrorCode.of("INFRA_STORAGE_OBJECT_NOT_FOUND", 404, "存储对象不存在");
    public static final ErrorCode VIRUS_SCANNER_UNAVAILABLE = ErrorCode.of("INFRA_VIRUS_SCANNER_UNAVAILABLE", 503, "病毒扫描服务暂时不可用");
    public static final ErrorCode MAIL_TEMPORARY_FAILURE    = ErrorCode.of("INFRA_MAIL_TEMPORARY_FAILURE", 503, "邮件服务暂时不可用");
    public static final ErrorCode MAIL_REJECTED             = ErrorCode.of("INFRA_MAIL_REJECTED", 502, "邮件被服务器拒绝");
    public static final ErrorCode WEBHOOK_URL_FORBIDDEN     = ErrorCode.of("INFRA_WEBHOOK_URL_FORBIDDEN", 422, "Webhook 地址不合法");
    public static final ErrorCode WEBHOOK_UNREACHABLE       = ErrorCode.of("INFRA_WEBHOOK_UNREACHABLE", 502, "Webhook 接收方无法访问");
    public static final ErrorCode SEARCH_UNAVAILABLE        = ErrorCode.of("INFRA_SEARCH_UNAVAILABLE", 503, "搜索服务暂时不可用");
    public static final ErrorCode SEARCH_TASK_FAILED        = ErrorCode.of("INFRA_SEARCH_TASK_FAILED", 502, "搜索索引任务失败");
    public static final ErrorCode CDN_PURGE_FAILED          = ErrorCode.of("INFRA_CDN_PURGE_FAILED", 502, "CDN 缓存清除失败");
    public static final ErrorCode INDEXNOW_FAILED           = ErrorCode.of("INFRA_INDEXNOW_FAILED", 502, "IndexNow 推送失败");
    public static final ErrorCode ENGINE_UNAVAILABLE        = ErrorCode.of("INFRA_ENGINE_UNAVAILABLE", 503, "翻译引擎暂时不可用");
    public static final ErrorCode ENGINE_AUTH_FAILED        = ErrorCode.of("INFRA_ENGINE_AUTH_FAILED", 502, "翻译引擎认证失败");
    public static final ErrorCode ENGINE_BAD_REQUEST        = ErrorCode.of("INFRA_ENGINE_BAD_REQUEST", 502, "翻译引擎拒绝了请求");
    public static final ErrorCode ENGINE_OUTPUT_INVALID     = ErrorCode.of("INFRA_ENGINE_OUTPUT_INVALID", 502, "翻译引擎返回的结果无法解析");
    public static final ErrorCode CAPTCHA_UNAVAILABLE       = ErrorCode.of("INFRA_CAPTCHA_UNAVAILABLE", 503, "人机验证服务暂时不可用");
    public static final ErrorCode FETCH_HOST_NOT_ALLOWED    = ErrorCode.of("INFRA_FETCH_HOST_NOT_ALLOWED", 422, "远程地址不在白名单中");
    public static final ErrorCode FETCH_ADDRESS_FORBIDDEN   = ErrorCode.of("INFRA_FETCH_ADDRESS_FORBIDDEN", 422, "远程地址指向内部网络");
    public static final ErrorCode FETCH_TOO_LARGE           = ErrorCode.of("INFRA_FETCH_TOO_LARGE", 413, "远程文件过大");
    public static final ErrorCode FETCH_TYPE_NOT_ALLOWED    = ErrorCode.of("INFRA_FETCH_TYPE_NOT_ALLOWED", 422, "远程文件类型不允许");
    public static final ErrorCode FETCH_TIMEOUT             = ErrorCode.of("INFRA_FETCH_TIMEOUT", 504, "远程文件下载超时");
    public static final ErrorCode FETCH_HTTP_ERROR          = ErrorCode.of("INFRA_FETCH_HTTP_ERROR", 502, "远程服务器返回错误");
    public static final ErrorCode REDIS_UNAVAILABLE         = ErrorCode.of("INFRA_REDIS_UNAVAILABLE", 503, "缓存服务暂时不可用");
}
```

> 代码块省略 `import`：`ErrorCode`、`CacheTag`、`Secret` 在 `common`；`ResizeMode`、`ImageFormat` 在 `media`；其余见 4.4.16 前的说明。

#### 4.7.1 ObjectStorage：对象存储

- 使用方：media（媒体文件）、core（任务输入与结果文件，`JobApi.storeInputFile`）。适配器：`S3CompatibleObjectStorage`（AWS S3、Cloudflare R2、阿里云 OSS、MinIO）、`LocalFileObjectStorage`（仅开发），系统设计 12.5。
- 框架补充：公开桶与私有桶分离（系统设计 5.3），因此 `get`、`stat`、`delete` 带 `Visibility`，`presignedGet` 只用于私有桶并可指定下载文件名；系统设计 12.5 的示意签名需同步。

```java
package com.b2bweb.api.port;

/** 可见性：公开桶（经 CDN 直接访问）与私有桶（只能经预签名链接访问）。 */
public enum Visibility { PUBLIC, PRIVATE }

/**
 * 对象键（不含桶名）。格式：
 * 媒体    {siteId}/{yyyy}/{mm}/{mediaId}-{slug 化的原文件名}.{ext}（系统设计 12.2）；
 * 任务文件 jobs/{siteId}/{yyyy}/{mm}/{fileId}-{slug 化的文件名}.{ext}（框架补充；系统级任务 siteId 为 0）。
 * 只允许小写字母、数字、"."、"_"、"-" 与 "/"；不得以 "/" 开头，不得包含 ".." 或 "//"；最长 512。
 */
public record StorageKey(String value) {
    private static final java.util.regex.Pattern FORMAT = java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9._\\-/]{0,511}$");

    public StorageKey {
        if (!FORMAT.matcher(value).matches() || value.contains("..") || value.contains("//")) {
            throw new IllegalArgumentException("非法的对象键: " + value);
        }
    }

    /** 媒体对象键。safeName 为 slug 化后的文件名主体（空时用 "file"），ext 为小写扩展名（不含点）。 */
    public static StorageKey media(long siteId, java.time.YearMonth month, long mediaId, String safeName, String ext) {
        return new StorageKey(siteId + "/" + month.getYear() + "/" + String.format("%02d", month.getMonthValue())
                + "/" + mediaId + "-" + (safeName.isBlank() ? "file" : safeName) + "." + ext);
    }

    /** 任务文件对象键。 */
    public static StorageKey jobFile(long siteId, java.time.YearMonth month, long fileId, String safeName, String ext) {
        return new StorageKey("jobs/" + media(siteId, month, fileId, safeName, ext).value());
    }

    /** 是否属于该站点（媒体键以 {siteId}/ 开头，任务文件键以 jobs/{siteId}/ 开头）。 */
    public boolean belongsToSite(long siteId) {
        return value.startsWith(siteId + "/") || value.startsWith("jobs/" + siteId + "/");
    }
}

public interface ObjectStorage {

    /**
     * 写入对象（同名覆盖）。size 必须等于流的实际长度，否则删除不完整的对象并抛出 IllegalArgumentException。
     * 调用方负责关闭 in。超时默认 120 秒。
     *
     * @throws ExternalServiceException INFRA_STORAGE_UNAVAILABLE（retryable = true）
     */
    void put(StorageKey key, InputStream in, long size, String contentType, Visibility visibility);

    /**
     * 读取对象；调用方负责关闭返回的流。
     *
     * @throws NotFoundException        INFRA_STORAGE_OBJECT_NOT_FOUND
     * @throws ExternalServiceException INFRA_STORAGE_UNAVAILABLE
     */
    InputStream get(StorageKey key, Visibility visibility);

    /** 对象元数据；不存在时为空。 */
    Optional<StoredObjectInfo> stat(StorageKey key, Visibility visibility);

    /** 删除；不存在时静默返回（幂等）。 */
    void delete(StorageKey key, Visibility visibility);

    /** 公开对象的 CDN URL（不访问网络；只用于 PUBLIC 桶中的对象）。 */
    URI publicUrl(StorageKey key);

    /**
     * 私有对象的预签名 GET 链接（不访问网络；只用于 PRIVATE 桶）。ttl 为 1 分钟–7 天。
     * downloadFileName 非空时设置 Content-Disposition: attachment; filename*=UTF-8''{编码后的文件名}。
     */
    URI presignedGet(StorageKey key, Duration ttl, @Nullable String downloadFileName);
}

public record StoredObjectInfo(StorageKey key, Visibility visibility, long size, String contentType,
                               @Nullable String etag, Instant lastModified) {}
```

#### 4.7.2 ImageUrlSigner：图片 URL 签名

- 使用方：media（`MediaApi.imageSources`、`imageUrl`）。适配器：`ImgproxyImageUrlSigner`（HMAC-SHA256 签名，密钥与盐来自部署配置）。

```java
package com.b2bweb.api.port;

public interface ImageUrlSigner {

    /**
     * 生成 imgproxy 签名 URL（系统设计 12.3）：
     * {imgproxyBase}/{签名}/rs:{fit|fill}:{w}:{h}/g:fp:{x}:{y}/q:{quality}/plain/{对象键}@{avif|webp|jpg|png}。
     * 只用于公开桶中的图片；格式在 URL 中显式指定，不依赖 Accept 协商；EXIF 等元数据由 imgproxy 配置统一去除。
     * 纯计算、不访问网络，可在 Delivery 热路径中调用。
     */
    URI sign(ImageTransform transform);
}

/**
 * @param height  FILL 时必填（由宽度 × 预设宽高比计算）；FIT 时为空（保持原比例）
 * @param focalX  焦点 0–1；为空时居中
 * @param quality 1–100；为空时用适配器默认值（AVIF 60、WebP 75、JPEG 80）
 */
public record ImageTransform(StorageKey source, ResizeMode mode, int width, @Nullable Integer height,
                             @Nullable Double focalX, @Nullable Double focalY, ImageFormat format,
                             @Nullable Integer quality) {
    public ImageTransform {
        if (width < 1 || width > 4096) throw new IllegalArgumentException("width 必须在 1–4096 之间");
        if (mode == ResizeMode.FILL && height == null) throw new IllegalArgumentException("FILL 必须指定 height");
        if (quality != null && (quality < 1 || quality > 100)) throw new IllegalArgumentException("quality 必须在 1–100 之间");
    }
}
```

#### 4.7.3 VirusScanner：病毒扫描

- 使用方：media（文档与压缩包上传后的异步扫描，系统设计 12.2、17.3）。适配器：`ClamAvVirusScanner`（clamd INSTREAM 协议）。

```java
package com.b2bweb.api.port;

public interface VirusScanner {

    /**
     * 扫描文件内容。media 在异步任务中调用（不在请求线程中）；调用方负责关闭 content。超时默认 60 秒。
     *
     * @throws ExternalServiceException INFRA_VIRUS_SCANNER_UNAVAILABLE（retryable = true；media 保持 PROCESSING 并重试）
     */
    ScanResult scan(InputStream content, long size, String fileName);

    /** 扫描服务是否可用（健康检查、后台状态）。 */
    boolean available();
}

public enum ScanVerdict {
    CLEAN,
    INFECTED,
    /** 无法扫描：超过扫描上限、加密压缩包、压缩炸弹等；media 按 QUARANTINED 处理 */
    UNSCANNABLE
}

/** @param signature 命中的特征名（INFECTED 时）；@param engineVersion 引擎与特征库版本 */
public record ScanResult(ScanVerdict verdict, @Nullable String signature, String engineVersion, Instant scannedAt) {}
```

#### 4.7.4 MailTransport：邮件传输

- 使用方：notification（发件箱投递）、core（数据主体请求的核验邮件）。适配器：`SmtpMailTransport`（SMTP；SES、Mailgun EU、Brevo 等服务商经其 SMTP 接口接入，系统设计 18）。
- 端口只负责传输：模板渲染、发件箱、重试与退信处理由 notification 负责。

```java
package com.b2bweb.api.port;

public interface MailTransport {

    /**
     * 投递一封邮件。server 为空时使用部署级默认配置（b2bweb.infra.mail；开发环境为 Mailpit）。超时默认 30 秒。
     *
     * @throws ExternalServiceException INFRA_MAIL_TEMPORARY_FAILURE（连接失败、超时、4xx 应答；retryable = true）、
     *                                  INFRA_MAIL_REJECTED（5xx 永久拒绝、认证失败；retryable = false）
     */
    MailSendResult send(MailMessage message, @Nullable MailServerConfig server);
}

public record MailAddress(@Email @NotBlank String email, @Nullable String name) {}

/**
 * @param messageId RFC 5322 Message-ID，由 notification 生成（如 <{ntf_message.id}@{发件域名}>），用于重试去重与退信关联
 * @param htmlBody  模板引擎输出的已转义 HTML
 * @param textBody  纯文本版本（必填）
 * @param headers   额外头部；不得覆盖 From、To、Reply-To、Subject、Message-ID
 */
public record MailMessage(String messageId, MailAddress from, List<MailAddress> to, @Nullable MailAddress replyTo,
                          String subject, String htmlBody, String textBody, Map<String, String> headers) {
    public MailMessage {
        if (to.isEmpty()) throw new IllegalArgumentException("收件人不能为空");
        to = List.copyOf(to);
        headers = Map.copyOf(headers);
    }
}

/** 传输安全；NONE 只允许在开发环境使用（启动时检查）。 */
public enum SmtpSecurity { STARTTLS, TLS, NONE }

/** 站点级 SMTP 配置（notification 的站点设置，password 经 SecretCipher 解密后传入）。 */
public record MailServerConfig(String host, int port, @Nullable String username, @Nullable Secret password,
                               SmtpSecurity security) {}

/** @param providerMessageId 服务商返回的队列 ID（可能为空） */
public record MailSendResult(String messageId, @Nullable String providerMessageId, Instant acceptedAt) {}
```

#### 4.7.5 WebhookTransport：Webhook 投递

- 使用方：notification（`NotificationApi.sendWebhookEvent` 的发件箱投递，系统设计 18）。适配器：`HttpWebhookTransport`（Spring HTTP 客户端）。

```java
package com.b2bweb.api.port;

public interface WebhookTransport {

    /**
     * 发送一次 Webhook 请求（不重试；重试由 notification 的发件箱负责）。请求头：
     * Content-Type: application/json；X-B2B-Event: {eventType}；X-B2B-Delivery: {deliveryId}；X-B2B-Timestamp: {Unix 秒}；
     * X-B2B-Signature: v1={hex(HMAC-SHA256(signingSecret, timestamp + "." + body))}。
     * URL 必须是 https；解析后的地址不得是回环、私有、链路本地或元数据地址（与 RemoteFileFetcher 相同的 SSRF 防护，系统设计 26.3）；
     * 不跟随重定向。超时默认 10 秒。非 2xx 应答作为结果返回，不抛出异常。
     *
     * @throws BusinessException        INFRA_WEBHOOK_URL_FORBIDDEN
     * @throws ExternalServiceException INFRA_WEBHOOK_UNREACHABLE（连接失败、超时；retryable = true）
     */
    WebhookResponse post(WebhookDelivery delivery);
}

/**
 * @param deliveryId 投递 ID（ntf_message.id 的十进制字符串，接收方去重用）
 * @param body       UTF-8 JSON（载荷默认只含事件类型、询盘编号与后台链接，系统设计 18）
 * @param timeout    为空时用默认值
 */
public record WebhookDelivery(URI url, String eventType, String deliveryId, String body, Secret signingSecret,
                              @Nullable Duration timeout) {}

/** @param bodyExcerpt 响应体前 1 KB（后台投递记录中排错用） */
public record WebhookResponse(int status, Duration elapsed, @Nullable String bodyExcerpt) {
    public boolean successful() { return status >= 200 && status < 300; }
}
```

#### 4.7.6 SearchEngineClient：搜索引擎

> M0补充方案（ADR-021，Proposed）：V1使用4.10.4的不可变物理代次和数据库指针，保留swap端口供兼容讨论，但V1调用方禁止使用，不能按下方旧重建示意实现。

- 使用方：search。适配器：`MeilisearchSearchEngineClient`（HTTP 客户端，主密钥只在后端，系统设计 19）。
- 索引名与设置由 search 决定；本端口不理解文档结构（文档字段见 4.5.14 与系统设计 19）。

```java
package com.b2bweb.api.port;

public interface SearchEngineClient {

    /** 确保索引存在并应用设置（幂等；设置未变化时不触发重新索引）。返回引擎任务 ID；无需变更时为空。 */
    Optional<Long> ensureIndex(String indexName, SearchIndexSettings settings);

    /** 删除索引；不存在时静默返回。 */
    void deleteIndex(String indexName);

    /** 以 prefix 开头的现有索引名（如 "s1_"）。 */
    List<String> listIndexes(String prefix);

    /** 新增或替换文档（按主键），异步生效，返回任务 ID。文档为 JSON 对象且带字符串主键 id；单批 ≤ 1000 个。 */
    long upsert(String indexName, List<JsonNode> documents);

    /** 按 ID 删除文档（不存在的 ID 忽略）；单批 ≤ 1000 个。返回任务 ID。 */
    long delete(String indexName, Collection<String> documentIds);

    /** 按过滤表达式删除（如模块停用时删除某类型的全部文档）。返回任务 ID。 */
    long deleteByFilter(String indexName, String filter);

    /** 底层引擎的交换能力；ADR-021的V1搜索重建禁止调用，必须发布私有不可变代次并CAS数据库指针。返回任务 ID。 */
    long swap(String indexName, String otherIndexName);

    /** 等待任务结束；超过 timeout 时返回当前状态。 */
    SearchTaskStatus await(long taskId, Duration timeout);

    /** 查询；超时默认 2 秒。 */
    SearchResponse search(String indexName, SearchRequest request);

    /** 文档数量（每晚的数量比对，系统设计 19）。 */
    long count(String indexName);

    /** M0新增草案，ADR-021：仅浏览不可变代次，游标稳定、每批≤1000；见4.10.4。 */
    CursorPage<JsonNode> scanDocuments(String indexName, CursorRequest cursor);
}

/** 索引命名（系统设计 5.3、19）。 */
public final class SearchIndexNames {
    private SearchIndexNames() {}

    /** s{site}_{locale}_content，如 s1_de_content、s1_zh-CN_content。 */
    public static String content(long siteId, String locale) { return "s" + siteId + "_" + locale + "_content"; }

    /** 全量重建用的临时索引名。 */
    public static String rebuilding(String indexName) { return indexName + "_rebuild"; }

    /** 某站点全部索引的名称前缀。 */
    public static String sitePrefix(long siteId) { return "s" + siteId + "_"; }
}

/**
 * 索引设置（系统设计 19）。
 *
 * @param searchableAttributes            按优先级排序：title > model_nos > summary > terms > body_text
 * @param typoToleranceDisabledAttributes 关闭拼写容错的字段（model_nos）
 * @param localizedLanguage               localizedAttributes 声明的语言（ISO 639-3，如中文 "cmn"、德语 "deu"）；为空表示自动识别
 * @param synonyms                        同义词：词 → 同义词
 */
public record SearchIndexSettings(String primaryKey, List<String> searchableAttributes,
                                  List<String> filterableAttributes, List<String> sortableAttributes,
                                  List<String> typoToleranceDisabledAttributes, @Nullable String localizedLanguage,
                                  Map<String, List<String>> synonyms, Set<String> stopWords) {
    public SearchIndexSettings {
        searchableAttributes = List.copyOf(searchableAttributes);
        filterableAttributes = List.copyOf(filterableAttributes);
        sortableAttributes = List.copyOf(sortableAttributes);
        typoToleranceDisabledAttributes = List.copyOf(typoToleranceDisabledAttributes);
        synonyms = Map.copyOf(synonyms);
        stopWords = Set.copyOf(stopWords);
    }
}

/**
 * @param filter 过滤表达式：由 search 用白名单字段构造，值必须经 SearchFilters 转义，不得拼接用户输入
 * @param sort   如 "published_at:desc"；字段必须在 sortableAttributes 中
 * @param attributesToRetrieve 空表示默认全部字段；非空仅返回指定字段，*表示全部（ADR-026候选澄清）
 * @param limit  1–100
 */
public record SearchRequest(String query, @Nullable String filter, List<String> facets, List<String> sort,
                            int offset, int limit, List<String> attributesToRetrieve,
                            List<String> attributesToHighlight) {
    public SearchRequest {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit 必须在 1–100 之间");
        if (offset < 0) throw new IllegalArgumentException("offset 不能为负数");
        facets = List.copyOf(facets);
        sort = List.copyOf(sort);
        attributesToRetrieve = List.copyOf(attributesToRetrieve);
        attributesToHighlight = List.copyOf(attributesToHighlight);
    }
}

/** @param facetDistribution 分面字段 → 取值 → 数量 */
public record SearchResponse(List<JsonNode> hits, long estimatedTotalHits,
                             Map<String, Map<String, Long>> facetDistribution, int processingTimeMs) {
    public SearchResponse {
        hits = List.copyOf(hits);
        facetDistribution = Map.copyOf(facetDistribution);
    }
}

public enum SearchTaskState { ENQUEUED, PROCESSING, SUCCEEDED, FAILED, CANCELED }

public record SearchTaskStatus(long taskId, SearchTaskState state, @Nullable String error) {}

/** 过滤表达式构造（纯函数），防止表达式注入。 */
public final class SearchFilters {
    private SearchFilters() {}
    private static final java.util.regex.Pattern FIELD = java.util.regex.Pattern.compile("^[a-z_][a-z0-9_.]*$");

    /** 转义为双引号字符串字面量（转义 \ 与 "）。 */
    public static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** field = "value"；field 必须是白名单形式的字段名。 */
    public static String eq(String field, String value) { return checked(field) + " = " + quote(value); }

    /** field IN ["a", "b"]。 */
    public static String in(String field, Collection<String> values) {
        return checked(field) + " IN [" + String.join(", ", values.stream().map(SearchFilters::quote).toList()) + "]";
    }

    /** 用 AND 连接（每个子句加括号）。 */
    public static String and(List<String> clauses) {
        return String.join(" AND ", clauses.stream().map(c -> "(" + c + ")").toList());
    }

    private static String checked(String field) {
        if (!FIELD.matcher(field).matches()) throw new IllegalArgumentException("非法的字段名: " + field);
        return field;
    }
}
```

异常：引擎不可用时抛出 `ExternalServiceException`（`INFRA_SEARCH_UNAVAILABLE`，retryable = true），search 据此降级（系统设计 19：分类列表降级为 MySQL 投影列表）；`await` 得到 `FAILED` 时由调用方按 `INFRA_SEARCH_TASK_FAILED` 处理。

#### 4.7.7 CdnPurger：CDN 缓存清除

- 使用方：delivery（缓存失效，系统设计 23.3）、seo（sitemap、robots 等不经 delivery 缓存的响应）。适配器：`CloudflareCdnPurger`（支持 Cache-Tag 清除的 CDN）；开发环境 `NoopCdnPurger`。

```java
package com.b2bweb.api.port;

public interface CdnPurger {

    /**
     * 按缓存标签清除（标签必须属于同一站点）。V1可靠路径直接调用，不经BufferStore去抖；
     * 适配器按服务商上限拆分批次（如每次 30 个标签）。不支持标签清除的 CDN：适配器把请求转为该站点全部域名的整站清除。
     *
     * @throws ExternalServiceException INFRA_CDN_PURGE_FAILED（retryable = true）
     */
    void purgeTags(Collection<CacheTag> tags);

    /** 按绝对 URL 清除（重定向响应、sitemap 文件等）；单批 ≤ 500 个。 */
    void purgeUrls(Collection<URI> urls);

    /** 是否支持按标签清除（后台状态页显示；为 false 时缓存失效粒度退化为整站）。 */
    boolean supportsTagPurge();
}
```

#### 4.7.8 IndexNowClient：IndexNow 推送

- 使用方：seo（发布、下线、路径变化时按分钟批量推送，只推送对外公开语言的 URL，系统设计 14.7）。适配器：`HttpIndexNowClient`（端点可配置，默认 `https://api.indexnow.org/indexnow`）。

```java
package com.b2bweb.api.port;

public interface IndexNowClient {

    /**
     * 提交一批 URL：POST JSON { host, key, keyLocation, urlList }。超时默认 10 秒。
     * 200 / 202 为成功；400、403、422 作为结果返回（配置问题，不重试）。
     *
     * @throws ExternalServiceException INFRA_INDEXNOW_FAILED（429、5xx、网络错误；retryable = true）
     */
    IndexNowResult submit(IndexNowSubmission submission);
}

/**
 * @param host        站点主域名（全部 URL 必须属于该域名）
 * @param key         IndexNow 密钥（公开值，Nuxt 在 /{key}.txt 提供）
 * @param keyLocation 密钥文件的绝对 URL
 * @param urls        1–10,000 个绝对 URL
 */
public record IndexNowSubmission(String host, String key, URI keyLocation, List<URI> urls) {
    public IndexNowSubmission {
        if (urls.isEmpty() || urls.size() > 10_000) throw new IllegalArgumentException("urls 数量必须在 1–10000 之间");
        urls = List.copyOf(urls);
    }
}

public record IndexNowResult(int status, int submitted) {
    public boolean accepted() { return status == 200 || status == 202; }
}
```

#### 4.7.9 TranslationEngine：AI 翻译引擎（OpenAI 兼容接口）

- 使用方：只有 translation（ArchUnit 检查）。适配器：`OpenAiCompatibleTranslationEngine`：用 Spring 的 `RestClient` 调用 `POST {baseUrl}/chat/completions`，**不使用任何厂商 SDK**；因此可接入 OpenAI、DeepSeek、通义千问、Kimi 或自部署的 vLLM、Ollama，切换服务商只改引擎配置（系统设计 5.9；CLAUDE.md 语言规则 6）。
- 引擎配置（base URL、模型名、API Key 等）只来自 translation 的 `trn_engine`：translation 读取并经 `SecretCipher` 解密后放入 `EngineConfig` 传入；代码与配置文件中不得出现 API Key。
- 职责划分：

| 职责 | 归属 |
|---|---|
| 翻译记忆查询与写入、术语表与风格指南、组装系统提示词、分批（单次单元数与 token 上限）、速率与预算控制、备用引擎切换、译文校验（占位标签、数字、术语、长度、残留中文，系统设计 5.8）、单元 ID 生成与映射 | translation |
| 组装 HTTP 请求、`response_format`、429 / 5xx 退避重试（尊重 `Retry-After`，最多 3 次）、严格解析输出（合法 JSON，`items` 的 ID 与请求一一对应）、读取 `usage` | 适配器 |

```java
package com.b2bweb.api.port;

public interface TranslationEngine {

    /**
     * 翻译一批单元：返回每个单元的译文与本次调用的 token 用量（系统设计 5.9）。
     * 请求：messages = [ { role: system, content: request.systemPrompt },
     *                    { role: user,   content: Jackson 序列化的 {"source","target","items":[{"id","text","maxLength","context"}]} } ]；
     * 待翻译文本只出现在 user 消息的 JSON 数据中，不做字符串拼接，不进入 system 消息（提示词注入防护，系统设计 26.3）。
     * 输出：{"detectedSource": string|null, "items":[{"id","text"}]}，见下方 JSON Schema。
     * 不得在持有数据库事务时调用。
     *
     * @throws ExternalServiceException INFRA_ENGINE_UNAVAILABLE（网络错误、超时、重试后仍为 429 / 5xx；retryable = true）、
     *                                  INFRA_ENGINE_AUTH_FAILED（401 / 403；retryable = false）、
     *                                  INFRA_ENGINE_BAD_REQUEST（其他 4xx，如上下文超长；retryable = false）、
     *                                  INFRA_ENGINE_OUTPUT_INVALID（输出不是合法 JSON、ID 缺失 / 多余 / 重复；retryable = false，
     *                                  translation 可缩小批次后重新请求）
     */
    TranslationResult translate(TranslationRequest request);

    /** 测试连接（后台“引擎配置 → 测试”）：翻译一个固定的短单元，返回结果、耗时与模型名；不抛出异常。 */
    EngineProbeResult probe(EngineConfig engine);
}

/** 结构化输出方式（系统设计 5.9“结构化输出”配置项）。 */
public enum StructuredOutputMode {
    /** response_format = { type: "json_schema", json_schema: { name: "translations", strict: true, schema } } */
    JSON_SCHEMA,
    /** response_format = { type: "json_object" } */
    JSON_OBJECT,
    /** 不支持 response_format：依靠提示词约束 + 严格解析 */
    NONE
}

/**
 * 引擎配置（trn_engine）。
 *
 * @param engineId        trn_engine.id（指标标签、日志）
 * @param name            配置名称（后台显示）
 * @param baseUrl         如 https://api.openai.com/v1、https://api.deepseek.com/v1 或自部署地址；适配器拼接 /chat/completions
 * @param apiKey          以 Authorization: Bearer 发送；自部署服务不需要时可为空串
 * @param temperature     默认 0.2
 * @param maxOutputTokens 请求的 max_tokens；为空表示不设置
 * @param timeout         单次 HTTP 请求的超时
 * @param extraHeaders    额外请求头（如自部署网关所需）；不得包含 Authorization
 */
public record EngineConfig(long engineId, String name, URI baseUrl, Secret apiKey, String model, double temperature,
                           @Nullable Integer maxOutputTokens, Duration timeout, StructuredOutputMode structuredOutput,
                           Map<String, String> extraHeaders) {
    public EngineConfig {
        if (!"https".equals(baseUrl.getScheme()) && !"http".equals(baseUrl.getScheme())) {
            throw new IllegalArgumentException("baseUrl 必须是 http(s) 地址");
        }
        if (temperature < 0 || temperature > 2) throw new IllegalArgumentException("temperature 必须在 0–2 之间");
        if (extraHeaders.keySet().stream().anyMatch(h -> h.equalsIgnoreCase("Authorization"))) {
            throw new IllegalArgumentException("extraHeaders 不得包含 Authorization");
        }
        extraHeaders = Map.copyOf(extraHeaders);
    }
}

/**
 * 待翻译单元。
 *
 * @param id        由 translation 生成的批内 ID（如 "u1"…"u50"），只含小写字母与数字；不得使用字段路径或原文
 * @param text      原文：纯数据，不作为指令（系统设计 26.3）
 * @param maxLength 长度上限（字符数），超长时 translation 校验失败
 * @param context   上下文说明（中文，来自 TranslationUnit.context）
 */
public record EngineItem(String id, String text, @Nullable Integer maxLength, @Nullable String context) {
    public EngineItem {
        if (!id.matches("^[a-z0-9]{1,16}$")) throw new IllegalArgumentException("单元 ID 只能包含小写字母与数字");
    }
}

/**
 * @param systemPrompt 固定的系统提示词（角色、规则、输出格式）+ 风格指南 + 本批相关术语，由 translation 组装；
 *                     前缀保持稳定以利用服务商的前缀缓存（系统设计 5.9）；不得包含待翻译文本
 * @param sourceLocale 为空表示由模型识别（询盘留言），结果中返回 detectedSourceLocale
 * @param items        1–100 个单元，ID 在批内唯一（translation 按引擎配置的单次上限分批）
 * @param purpose      用量统计分类："content"、"resource"、"inquiry"、"probe"
 */
public record TranslationRequest(EngineConfig engine, String systemPrompt, @Nullable String sourceLocale,
                                 String targetLocale, List<EngineItem> items, String purpose) {
    public TranslationRequest {
        if (items.isEmpty() || items.size() > 100) throw new IllegalArgumentException("items 数量必须在 1–100 之间");
        if (items.stream().map(EngineItem::id).distinct().count() != items.size()) {
            throw new IllegalArgumentException("单元 ID 必须唯一");
        }
        items = List.copyOf(items);
    }
}

/** @param text 译文（纯文本数据；translation 校验通过后才使用） */
public record EngineTranslation(String id, String text) {}

/** 用量（取自响应的 usage）。@param cachedInputTokens 命中前缀缓存的输入 token（服务商返回时） */
public record EngineUsage(int inputTokens, int outputTokens, @Nullable Integer cachedInputTokens) {}

/**
 * @param items                与请求的单元一一对应，顺序与请求一致
 * @param detectedSourceLocale 请求未指定源语言时模型识别出的语言（BCP 47）
 * @param model                响应中的模型名
 * @param attempts             HTTP 请求次数（含重试）
 */
public record TranslationResult(List<EngineTranslation> items, @Nullable String detectedSourceLocale,
                                EngineUsage usage, String model, Duration elapsed, int attempts) {
    public TranslationResult { items = List.copyOf(items); }
}

public record EngineProbeResult(boolean success, @Nullable String model, Duration elapsed,
                                @Nullable String errorCode, @Nullable String errorMessage) {}
```

`JSON_SCHEMA` 模式使用的输出 Schema（`JSON_OBJECT` 与 `NONE` 模式下同一结构写在系统提示词的输出格式说明中）：

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["detectedSource", "items"],
  "properties": {
    "detectedSource": { "type": ["string", "null"] },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["id", "text"],
        "properties": { "id": { "type": "string" }, "text": { "type": "string" } }
      }
    }
  }
}
```

- 适配器的日志只记录引擎 ID、模型、单元数、用量、耗时与状态码，不记录提示词、原文、译文与响应正文；`apiKey` 是 `Secret`，`toString()` 为掩码。
- `FakeTranslationEngine`（testkit）：译文为确定性的伪翻译（如 `"[de] " + 原文`），原样保留编号占位标签与数字；可注入 429、超时、ID 缺失、非法 JSON 等故障；记录收到的请求，用于断言“同一原文只调用一次引擎”（系统设计 28“AI 翻译”一行）。

#### 4.7.10 CaptchaVerifier：人机验证

- 使用方：form（公开提交，系统设计 17.2 第 4 步）。适配器：`TurnstileCaptchaVerifier`（Cloudflare Turnstile siteverify）。

```java
package com.b2bweb.api.port;

public interface CaptchaVerifier {

    /**
     * 服务端校验令牌。超时默认 5 秒。令牌只能校验一次（重复校验返回 success = false）。
     * form 还需校验 result.hostname 属于本站域名、result.action 与表单 key 一致。
     *
     * @throws ExternalServiceException INFRA_CAPTCHA_UNAVAILABLE（retryable = true）；服务不可用时是否放行由 form 决定
     */
    CaptchaResult verify(CaptchaVerification verification);
}

/**
 * @param secretKey      站点的 Turnstile 密钥（form 设置中的 x-secret 字段，经 SecretCipher 解密）
 * @param remoteIp       访客 IP
 * @param idempotencyKey 同一令牌网络重试时的幂等键
 */
public record CaptchaVerification(String token, Secret secretKey, @Nullable String remoteIp,
                                  @Nullable String idempotencyKey) {}

/** @param errorCodes 服务商返回的错误码（如 "invalid-input-response"、"timeout-or-duplicate"） */
public record CaptchaResult(boolean success, List<String> errorCodes, @Nullable String hostname,
                            @Nullable String action, @Nullable Instant challengeTs) {
    public CaptchaResult { errorCodes = List.copyOf(errorCodes); }
}
```

#### 4.7.11 GeoIpResolver：IP 地理位置

- 使用方：form（预选国家字段、记录 `ip_country`）、tracking（同意横幅的地区判断），系统设计 17.2、17.9。适配器：`MaxMindGeoIpResolver`（本地 GeoIP 数据库文件，定期更新；不访问网络）。

```java
package com.b2bweb.api.port;

public interface GeoIpResolver {

    /** 解析 IP（IPv4 / IPv6 文本）。私有、保留地址、格式错误或数据库未命中时为空。纯本地查询，可在请求线程中调用。 */
    Optional<GeoLocation> resolve(String ip);

    /** 数据库版本（构建日期），后台状态显示。 */
    String databaseVersion();
}

/**
 * @param countryCode   ISO 3166-1 alpha-2（大写）
 * @param regionCode    ISO 3166-2 一级行政区（如 "US-CA"）；数据库不提供时为空
 * @param continentCode 大洲代码（如 "EU"、"NA"）
 */
public record GeoLocation(String countryCode, @Nullable String regionCode, @Nullable String continentCode) {}
```

#### 4.7.12 MxRecordChecker：邮箱域名 MX 检查

- 使用方：form（反垃圾评分的“邮箱 MX 记录”项，异步执行，系统设计 17.4）。适配器：`DnsMxRecordChecker`（JDK DNS 查询，结果缓存 24 小时）。

```java
package com.b2bweb.api.port;

public interface MxRecordChecker {

    /** 查询邮箱域名（小写、Punycode）的 MX 记录。超时默认 3 秒；超时与查询失败返回 UNKNOWN，不抛出异常。 */
    MxCheckResult check(String domain);
}

public enum MxStatus {
    /** 有 MX 记录 */
    HAS_MX,
    /** 没有 MX，但有 A / AAAA 记录（可隐式投递） */
    IMPLICIT_A,
    /** 域名不存在或没有可投递记录 */
    NONE,
    /** 查询失败或超时（不计分） */
    UNKNOWN
}

public record MxCheckResult(String domain, MxStatus status, List<String> exchanges, Instant checkedAt) {
    public MxCheckResult { exchanges = List.copyOf(exchanges); }
}
```

#### 4.7.13 RemoteFileFetcher：安全的远程文件下载

- 使用方：media（`MediaApi.uploadFromUrl`）；4.2.5 表中 product 的用途“导入图片 URL”（系统设计 8.6）通过调用 `MediaApi.uploadFromUrl` 实现，product 不需要直接注入本端口（ArchUnit 仍允许，以备导入非媒体文件）。适配器：`SafeHttpRemoteFileFetcher`。

```java
package com.b2bweb.api.port;

public interface RemoteFileFetcher {

    /**
     * 下载远程文件到临时文件（SSRF 防护，系统设计 26.3）：
     * 只允许 http / https；主机必须匹配 allowedHosts（支持 "*.example.com"）；DNS 解析后拒绝回环、私有、链路本地、组播、
     * 保留地址与云元数据地址，并直接连接已校验的 IP（防 DNS 重绑定）；每次重定向重新校验（最多 maxRedirects 次）；
     * 流式计数超过 maxBytes 立即中止；总耗时超过 timeout 中止。不得在持有数据库事务时调用。
     *
     * @throws BusinessException        INFRA_FETCH_HOST_NOT_ALLOWED、INFRA_FETCH_ADDRESS_FORBIDDEN、INFRA_FETCH_TOO_LARGE、
     *                                  INFRA_FETCH_TYPE_NOT_ALLOWED
     * @throws ExternalServiceException INFRA_FETCH_TIMEOUT（retryable = true）、
     *                                  INFRA_FETCH_HTTP_ERROR（5xx 与网络错误 retryable = true；4xx retryable = false）
     */
    FetchedFile fetch(RemoteFetchRequest request);
}

/**
 * @param allowedHosts        域名白名单（站点设置，小写）；为空时一律拒绝
 * @param allowedContentTypes 允许的响应 Content-Type（如 "image/*"）；为空表示不限（最终类型由 media 用 Tika 识别）
 */
public record RemoteFetchRequest(URI url, Set<String> allowedHosts, long maxBytes, Duration timeout,
                                 int maxRedirects, Set<String> allowedContentTypes) {
    public RemoteFetchRequest {
        allowedHosts = Set.copyOf(allowedHosts);
        allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    /** 导入图片的默认参数：≤ 50 MB、30 秒、最多 3 次重定向、image/*。 */
    public static RemoteFetchRequest image(URI url, Set<String> allowedHosts) {
        return new RemoteFetchRequest(url, allowedHosts, 50L * 1024 * 1024, Duration.ofSeconds(30), 3, Set.of("image/*"));
    }
}

/** 已下载的文件。调用方必须关闭（try-with-resources），关闭时删除临时文件。 */
public interface FetchedFile extends AutoCloseable {
    /** 跟随重定向后的最终 URL。 */
    URI finalUrl();
    @Nullable String contentType();
    long size();
    /** 文件名：取自 Content-Disposition，否则取 URL 路径最后一段；都没有时为空。 */
    @Nullable String fileName();
    /** 打开读取流（可多次打开，调用方关闭）。 */
    InputStream open();
    @Override
    void close();
}
```

#### 4.7.14 TaggedCache：带标签的缓存

- 使用方：delivery（Delivery 结果、站点引导数据）、seo（sitemap 分片、robots）、url（路由解析）。适配器：`RedisTaggedCache`。系统设计 23.1–23.3。
- 键：`s{siteId}:{namespace}:{key}`，自动使用当前站点（没有站点上下文时抛出 `NoSiteContextException`）；标签索引：`s{siteId}:tag:{标签值}` → 键集合，TTL 不短于其中最长的条目。
- 值以 JSON 存储（Jackson），类型必须是可序列化的 record 或 `JsonNode`。应用内存缓存（扩展点注册表、Schema、域名映射）不使用本端口。

```java
package com.b2bweb.api.port;

public interface TaggedCache {

    /** namespace 格式 {moduleId}.{名称}，如 "delivery.resolve"。 */
    <T> Optional<T> get(String namespace, String key, Class<T> type);

    /** ADR-020：公开交付链必须使用带绝对新鲜度的读取，不得将命中当成新回源。 */
    <T> Optional<CachedValue<T>> getWithFreshness(String namespace, String key, Class<T> type);

    /** 写入并登记标签。tags 必须属于当前站点；单条超过 CacheTag.MAX_TAGS_PER_RESPONSE 个时由调用方先合并（系统设计 23.2）。 */
    void put(String namespace, String key, Object value, Set<CacheTag> tags, Duration ttl);

    /** M0新增草案，ADR-020：开始回源前取得代次；Redis不可用时为空。 */
    Optional<CacheWriteFence> beginLoad();

    /** M0新增草案：与代次比较、数据及标签登记在一个原子操作中；不匹配时不写并返回false。 */
    boolean putIfUnchanged(String namespace, String key, Object value, Set<CacheTag> tags,
                           Duration ttl, CacheWriteFence fence);

    /**
     * 读取；未命中时合并并发回源（系统设计 23.3 防击穿）：同一键只有一个调用执行 loader，其他调用最多等待 waitTimeout
     * 后读取其结果（仍未得到时自行执行 loader）。loader 返回空表示结果不缓存（如 404）。
     */
    <T> Optional<T> getOrLoad(String namespace, String key, Class<T> type, Duration ttl, Duration waitTimeout,
                              Supplier<Optional<CacheEntry<T>>> loader);

    /** ADR-020：与getOrLoad同样代次保护，额外返回当前结果的绝对有效时刻。 */
    <T> Optional<CachedValue<T>> getOrLoadWithFreshness(String namespace, String key, Class<T> type,
            Duration ttl, Duration waitTimeout, Supplier<Optional<CacheEntry<T>>> loader);

    /** 删除带有任一标签的全部键并清理标签索引；返回删除的键数量。标签必须属于当前站点。 */
    long invalidate(Collection<CacheTag> tags);

    void evict(String namespace, String key);

    /** 删除当前站点某命名空间的全部键（如重定向批量变化时清除路由解析缓存）；返回删除数量。 */
    long evictNamespace(String namespace);
}

/** 回源结果：值与它依赖的标签。 */
public record CachedValue<T>(T value, Instant freshUntil) {}

public record CacheEntry<T>(T value, Set<CacheTag> tags, Instant freshUntil) {
    public CacheEntry { tags = Set.copyOf(tags); java.util.Objects.requireNonNull(freshUntil); }
}
```

Redis 不可用时：`get` 返回空、`getOrLoad` 直接执行 loader、`put` 记录错误指标后返回（缓存只是加速层）；`evict`、`invalidate` 与 `evictNamespace` 抛出 `ExternalServiceException`（`INFRA_REDIS_UNAVAILABLE`，retryable = true），因为失效失败会让旧内容残留——delivery 的监听器据此失败并由事件重新投递。

#### 4.7.15 RateLimiter：限流

- 使用方：form（提交、附件上传）、search（站内搜索）、tracking（同意记录）、core（登录、后台 API）。适配器：`Bucket4jRedisRateLimiter`（Bucket4j + Redis，系统设计 26.4）。
- 键：`s{siteId}:rl:{rule.name}:{subject}`；`siteScoped = false` 的规则为 `sys:rl:{rule.name}:{subject}`。

```java
package com.b2bweb.api.port;

public interface RateLimiter {

    /**
     * 尝试消耗 tokens 个令牌。subject 为限流维度的值：IP 用规范化后的地址；邮箱等个人数据由调用方先做 SHA-256，
     * Redis 中不保存明文。Redis 不可用时：rule.failClosed = true 返回拒绝，否则放行；两种情况都记录指标并告警。
     */
    RateLimitDecision tryConsume(RateLimitRule rule, String subject, int tokens);

    default RateLimitDecision tryConsume(RateLimitRule rule, String subject) { return tryConsume(rule, subject, 1); }
}

/**
 * @param name       规则名 {moduleId}.{名称}，如 "form.submit_ip"
 * @param capacity   一个周期内允许的次数
 * @param period     周期（令牌按 capacity / period 匀速补充）
 * @param siteScoped 是否按站点隔离
 * @param failClosed Redis 不可用时是否拒绝
 */
public record RateLimitRule(String name, long capacity, Duration period, boolean siteScoped, boolean failClosed) {
    public RateLimitRule {
        if (!name.matches("^[a-z]+\\.[a-z0-9_]+$")) throw new IllegalArgumentException("规则名格式为 {moduleId}.{名称}");
        if (capacity < 1) throw new IllegalArgumentException("capacity 必须为正数");
    }
}

/** @param retryAfter 被拒绝时建议的等待时间（响应头 Retry-After）；允许时为 Duration.ZERO */
public record RateLimitDecision(boolean allowed, long remaining, Duration retryAfter) {}
```

V1 规则（系统设计 26.4；规则常量定义在各使用方模块中）：

| 规则名 | 维度 | 限制 | siteScoped | failClosed |
|---|---|---|---|---|
| `core.login` | 账号 + IP | 5 次 / 15 分钟 | 否 | 是 |
| `core.admin_api` | 用户 | 600 次 / 分钟 | 否 | 否 |
| `form.submit_ip` | IP | 5 次 / 10 分钟 | 是 | 否 |
| `form.submit_email` | 邮箱（SHA-256） | 10 次 / 天 | 是 | 否 |
| `form.attachment_ip` | IP | 10 次 / 10 分钟 | 是 | 否 |
| `tracking.consent_ip` | IP | 30 次 / 10 分钟（超限静默丢弃） | 是 | 否 |
| `search.query_ip` | IP | 60 次 / 分钟 | 是 | 否 |

#### 4.7.16 DistributedLock：分布式锁

- 使用方：delivery（合并并发回源）、graph（同一内容的增量计算）、translation（同一对象同一语言的任务）、url（子树重算）。适配器：`RedisDistributedLock`（`SET NX PX` + 令牌校验释放）。
- **只用于合并重复工作，不用于保证数据正确性**：租约可能过期，正确性依靠数据库唯一约束与乐观锁。定时任务的单实例执行由 core 的 ShedLock 负责（4.4.12），不使用本端口。
- 锁的键：`s{siteId}:lock:{name}`，没有站点上下文时 `sys:lock:{name}`；`name` 格式 `{moduleId}.{用途}:{对象}`，如 `graph.recompute:7201893453912345`。

```java
package com.b2bweb.api.port;

public interface DistributedLock {

    /** 立即尝试获取；已被持有时返回空。 */
    Optional<LockHandle> tryAcquire(String name, Duration lease);

    /** 在 wait 内重试获取（50–200 ms 随机间隔）；仍未获取时返回空。 */
    Optional<LockHandle> tryAcquire(String name, Duration wait, Duration lease);

    /** 获取锁后执行 action 并释放；没有获取到锁时不执行，返回 false。 */
    default boolean runIfAcquired(String name, Duration wait, Duration lease, Runnable action) {
        Optional<LockHandle> handle = tryAcquire(name, wait, lease);
        if (handle.isEmpty()) return false;
        try (LockHandle ignored = handle.get()) {
            action.run();
            return true;
        }
    }
}

public interface LockHandle extends AutoCloseable {
    String name();

    /** 续租；锁已失效（过期或被他人持有）时返回 false。 */
    boolean extend(Duration lease);

    /** 释放：只释放自己持有的锁；重复调用无副作用。 */
    @Override
    void close();
}
```

#### 4.7.17 BufferStore：计数与合并缓冲

- 使用方：url（404 计数、重定向命中计数，系统设计 13.2）、graph（同一内容的触发合并 30 秒，15.4）、delivery（仅可丢失的优化提示，不承担可靠清除义务，23.3）、seo（IndexNow 按分钟批量，14.7）。适配器：`RedisBufferStore`（计数用 Hash，延迟集合用 Sorted Set）。
- 键：`s{siteId}:buf:{bucket}`，自动使用当前站点；`bucket` 格式 `{moduleId}.{名称}`。消费方以 `PER_SITE` 的 `ScheduledTask`（4.4.12）定期取出处理。
- Redis 故障时数据可能丢失：只用于可容忍少量丢失、或可由全量任务补齐的数据。

```java
package com.b2bweb.api.port;

public interface BufferStore {

    // ───── 计数器 ─────

    /** 累加计数（如 bucket = "url.not_found"，member = 路径）。member 最长 2048 个字符。 */
    void increment(String bucket, String member, long delta);

    /** 原子取出并清零一个桶中的计数，最多 maxMembers 个（其余留到下次）；定时任务据此批量写库。 */
    Map<String, Long> drainCounters(String bucket, int maxMembers);

    // ───── 延迟集合（合并触发） ─────

    /**
     * 登记一个待处理成员，到期时间 = 当前时间 + delay；成员已存在时保留较早的到期时间，窗口内的重复触发合并为一次。
     * 例：graph.recompute（contentId，30 秒）、seo.indexnow（URL，1 分钟）。
     */
    void schedule(String queue, String member, Duration delay);

    /** 原子取出已到期的成员（按到期时间升序，最多 limit 个）。处理失败时由调用方重新 schedule。 */
    List<String> pollDue(String queue, int limit);

    /** 队列中的成员数量（指标与积压告警）。 */
    long size(String queue);
}
```

### 4.8 契约测试

契约测试保证“替身与真实实现行为一致”（ADR-018）：同一个契约只有一套测试，提供方的真实实现与 testkit 中的替身都必须通过它。模块用替身开发时依赖的每一条行为，都必须写在契约测试中；没有写进契约测试的行为（缓存时长、未声明的排序、性能）不得被使用方依赖（4.1.9）。测试框架、替身清单与夹具的完整说明见第 9 章，本节规定契约测试的组织方式与必须覆盖的内容。

#### 4.8.1 类别与组织

| 类别 | 被测对象 | 套件（testkit） | 运行方 |
|---|---|---|---|
| 服务接口契约 | 4.4 的每个服务接口 | `com.b2bweb.testkit.contract.<包>.<接口名>Contract`（抽象类） | 提供方模块的 `<接口名>ContractTest`（真实实现）；testkit 自身的 `InMemory<接口名>ContractTest`（替身） |
| 端口契约 | 4.7 的每个端口 | `com.b2bweb.testkit.contract.port.<端口名>Contract` | infrastructure 的 `<适配器类名>ContractTest`（Testcontainers：MinIO、Redis、Meilisearch、Mailpit、ClamAV；HTTP 类端口用本地模拟服务器）；testkit 自身的假实现测试 |
| 扩展点一致性 | 4.5 中有通用规则的扩展点 | `com.b2bweb.testkit.conformance.<扩展点>Conformance` | 实现方模块的 `<实现类名>ConformanceTest` |
| 事件契约 | 4.6 的全部事件 | `com.b2bweb.testkit.contract.event.DomainEventContract` | testkit 自身（遍历 `com.b2bweb.api.event` 包） |
| 覆盖检查 | 4.2 总表与代码、替身、套件的对应关系 | `ContractCatalogTest` + CI 脚本 `tools/checks/contract-coverage` | CI 每个 PR |

- 提供方的 `ContractTest` 中，提供方依赖的**其他模块**的服务一律使用 testkit 替身（如 content 的契约测试使用 `InMemoryUrlApi`），只有提供方自己是真实实现。
- 契约测试不访问外部网络：AI 引擎、Turnstile、Webhook、IndexNow、CDN 的适配器测试使用本地模拟 HTTP 服务器（系统设计 28“AI 翻译”一行）。

#### 4.8.2 运行环境与夹具

契约套件不知道被测对象是真实实现还是替身，只通过两个钩子与环境交互：

```java
package com.b2bweb.testkit.contract;

/**
 * 契约测试的运行环境。提供方一侧由 platform-core 的测试支撑实现（真实的站点上下文、事务、事件登记，第 5、9 章）；
 * 替身一侧由 testkit 的 InMemoryContractEnvironment 实现。
 */
public interface ContractEnvironment {

    long SITE_A = 1001L;
    long SITE_B = 1002L;

    /**
     * 恢复初始状态：清空数据与事件记录；时钟拨回 2026-01-01T00:00:00Z；两个站点（源语言 zh-CN，另启用 en、de，
     * zh-CN 与 en 对外公开，en 与 de 开启自动翻译）；全部模块启用；testkit 示例扩展点（示例内容类型 SAMPLE 等）已注册。
     */
    void reset();

    /** 切换当前站点上下文。 */
    ContractEnvironment useSite(long siteId);

    /** 清除站点上下文（验证 NoSiteContextException）。 */
    ContractEnvironment withoutSite();

    /** 切换当前用户：TestUsers.ADMIN、TestUsers.EDITOR（全部内容权限）、TestUsers.VIEWER（无写权限）、TestUsers.SYSTEM。 */
    ContractEnvironment useUser(TestUser user);

    /** 在当前站点停用 / 启用模块（验证 ModuleDisabledException 与扩展点过滤）。 */
    void setModuleEnabled(String moduleId, boolean enabled);

    /** 可拨动的时钟（注入为 java.time.Clock）。 */
    ManualClock clock();

    /** 在事务中执行并提交；事务提交后，事件进入 events()。 */
    <T> T inTransaction(java.util.function.Supplier<T> action);

    /** 在事务中执行后回滚（验证“加入调用方事务”）。 */
    void inRolledBackTransaction(Runnable action);

    /** 已提交事务中发布的事件（按发布顺序）。 */
    RecordedEvents events();
}
```

- **数据准备**：优先通过契约自身的方法准备数据（如先 `create` 再 `publish`）。契约无法创建的数据（如 `FormApi` 的提交记录、`TaxonomyApi` 的分类项——它们的创建是提供方的后台功能，不在契约中）由该契约的夹具接口 `<接口名>Fixture`（testkit 中定义）准备：真实实现一侧用提供方内部的应用服务实现，替身一侧用替身的预置方法实现。
- **扩展点**：需要扩展点参与的契约（如 `ContentApi.publish` 收集 `PublishContributor`）使用 testkit 的示例实现（示例内容类型 `SAMPLE` 的 `ContentTypeProvider`、`card` 部分的 `PublishContributor`、`TranslatableContentProvider`），真实实现与替身看到同一组扩展点。

#### 4.8.3 服务接口契约：必须覆盖的语义

| 语义 | 必须覆盖的用例 |
|---|---|
| 正常路径 | 每个公开方法至少一个用例 |
| 空值（4.1.4） | 不存在的对象返回 `Optional.empty()`；集合返回空集合；批量查询中不存在的 ID 不出现在结果中；从不返回 `null` |
| 异常（4.1.7） | Javadoc 中每个 `@throws`：异常类型、错误码，以及约定的 `properties`（如 `currentVersion`、`conflictContentId`） |
| 站点隔离（系统设计 5.3） | 站点 A 创建的数据在站点 B 的上下文中不可见、不可修改；没有站点上下文时抛出 `NoSiteContextException` |
| 模块停用（4.1.3） | 可停用模块的服务：写操作与面向前台的操作抛出 `ModuleDisabledException`，只读查询照常返回 |
| 事务（4.1.3） | 写方法加入调用方事务：回滚后数据与事件都不存在；事件只在提交后出现在 `events()` 中 |
| 事件（4.6） | 写方法发布的事件：类型、数量与关键字段（ID、语言、修订号、`actor`） |
| 幂等 | Javadoc 标注幂等的方法重复调用结果一致（如 `ensureTargetLocalization`、`MediaApi.delete`、`UrlApi.releaseRoutes`、带 `dedupKey` 的 `NotificationApi.send`） |
| 乐观锁 | `expectedVersion` 不匹配时抛出 `VersionConflictException`，`currentVersion` 正确；为空时不检查 |
| 批量与分页（4.1.3、4.1.6） | 批量查询超过 500 个 ID 时抛出 `IllegalArgumentException`；页码与游标分页结果稳定、无重复无遗漏；非白名单排序字段被拒绝 |
| 权限（4.1.3） | `ContentApi` 生命周期写方法：无权限用户抛出 `ForbiddenException`；system 身份跳过检查 |
| 语言 | 按 `locale` 查询不返回其他语言的数据（不跨语言回退，系统设计 5.4） |

不进入契约测试的内容：性能目标（Delivery 热路径的 P95 由提供方的性能测试验证）、缓存行为、未在 Javadoc 中声明的排序与内部细节。

示例（节选）：

```java
package com.b2bweb.testkit.contract.content;

public abstract class ContentApiContract {

    protected abstract ContentApi contentApi();

    protected abstract ContractEnvironment env();

    @BeforeEach
    void resetEnvironment() {
        env().reset();
        env().useSite(ContractEnvironment.SITE_A).useUser(TestUsers.EDITOR);
    }

    @Test
    @DisplayName("create：返回源语言本地化，提交后发布 ContentCreatedEvent 与 LocalizationCreatedEvent")
    void createPublishesEvents() {
        ContentCreated created = env().inTransaction(() -> contentApi().create(
                new CreateContentCommand(SampleContentType.TYPE, Locales.SOURCE, "关于我们", null)));

        assertThat(created.locale()).isEqualTo(Locales.SOURCE);
        assertThat(env().events().ofType(ContentCreatedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.contentId()).isEqualTo(created.contentId());
            assertThat(e.siteId()).isEqualTo(ContractEnvironment.SITE_A);
            assertThat(e.actor()).isEqualTo(TestUsers.EDITOR.eventActor());
        });
        assertThat(env().events().ofType(LocalizationCreatedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("create：locale 不是源语言时抛出 CONTENT_LOCALE_NOT_SOURCE")
    void createRejectsTargetLocale() {
        assertThatThrownBy(() -> env().inTransaction(() -> contentApi().create(
                new CreateContentCommand(SampleContentType.TYPE, "de", "Über uns", null))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ContentErrors.LOCALE_NOT_SOURCE));
    }

    @Test
    @DisplayName("create：加入调用方事务，回滚后内容与事件都不存在")
    void createJoinsCallerTransaction() {
        long[] contentId = new long[1];
        env().inRolledBackTransaction(() -> contentId[0] = contentApi().create(
                new CreateContentCommand(SampleContentType.TYPE, Locales.SOURCE, "临时", null)).contentId());

        assertThat(contentApi().findContent(contentId[0])).isEmpty();
        assertThat(env().events().all()).isEmpty();
    }

    @Test
    @DisplayName("findContent：站点 B 看不到站点 A 的内容")
    void siteIsolation() {
        long contentId = env().inTransaction(() -> contentApi().create(
                new CreateContentCommand(SampleContentType.TYPE, Locales.SOURCE, "关于我们", null))).contentId();

        env().useSite(ContractEnvironment.SITE_B);
        assertThat(contentApi().findContent(contentId)).isEmpty();
    }
}
```

提供方与替身各自继承同一个套件：

```java
// modules/content/src/test/java/com/b2bweb/content/ContentApiContractTest.java（真实实现）
@ModuleContractTest   // platform-core 测试支撑：@ApplicationModuleTest + Testcontainers MySQL + ContractEnvironment Bean（第 9 章）
class ContentApiContractTest extends ContentApiContract {
    @Autowired ContentApi contentApi;
    @Autowired ContractEnvironment env;

    @Override protected ContentApi contentApi() { return contentApi; }
    @Override protected ContractEnvironment env() { return env; }
}

// backend/platform/platform-testkit/src/test/java/com/b2bweb/testkit/content/InMemoryContentApiContractTest.java（替身）
class InMemoryContentApiContractTest extends ContentApiContract {
    private final InMemoryContractEnvironment env = new InMemoryContractEnvironment();
    private final InMemoryContentApi api = env.contentApi();

    @Override protected ContentApi contentApi() { return api; }
    @Override protected ContractEnvironment env() { return env; }
}
```

#### 4.8.4 端口契约

端口契约验证适配器与假实现对使用方可见的行为一致；适配器特有的故障（如 HTTP 429 后重试）由套件中的可选用例覆盖，假实现通过“故障注入”钩子支持同样的用例。

| 端口 | 必须覆盖的行为 |
|---|---|
| `ObjectStorage` | put / get 往返；`stat` 大小与类型；`delete` 幂等；读取不存在的对象抛出 `INFRA_STORAGE_OBJECT_NOT_FOUND`；公开桶与私有桶互不可见；`presignedGet` 可下载且带文件名 |
| `ImageUrlSigner` | 同一输入得到同一 URL；URL 含格式与尺寸；FILL 缺少高度被拒绝 |
| `VirusScanner` | EICAR 测试文件判为 `INFECTED`；普通文件 `CLEAN` |
| `MailTransport` | 收件人、主题、HTML 与纯文本、Reply-To、Message-ID 原样投递（Mailpit / 内存记录） |
| `WebhookTransport` | 签名头可被接收方按 4.7.5 的算法验证；非 2xx 作为结果返回；内网地址被拒绝 |
| `SearchEngineClient` | `ensureIndex` 幂等；upsert → `await` → `search` 命中；过滤、分面、排序；`delete` 与 `deleteByFilter`；不可变代次scanDocuments游标完整/稳定；任务PROCESSING不得当成功；迟到写入不能改活跃代次（4.10.4） |
| `CdnPurger`、`IndexNowClient` | 请求格式与批次拆分（模拟服务器）；可重试错误抛出 `retryable = true` |
| `TranslationEngine` | 结果与请求单元一一对应、顺序一致；待翻译文本只出现在 user 消息的 JSON 中；非法 JSON、ID 缺失或重复抛出 `INFRA_ENGINE_OUTPUT_INVALID`；429 / 5xx 重试最多 3 次并尊重 `Retry-After`；401 抛出 `INFRA_ENGINE_AUTH_FAILED`；`usage` 正确读取（模拟 OpenAI 兼容服务器） |
| `CaptchaVerifier` | 成功、失败、重复令牌（模拟服务器） |
| `GeoIpResolver`、`MxRecordChecker` | 已知地址 / 域名的结果；私有地址为空；查询失败返回 `UNKNOWN` |
| `RemoteFileFetcher` | 白名单外主机、解析到内网的域名、重定向到内网、超限、超时分别得到约定的错误码；关闭后临时文件被删除 |
| `TaggedCache` | 按标签失效删除全部相关键；不同站点同名键互不影响；同键正常等待窗口合并loader，超时回退允许额外loader；空结果不缓存；失效中回填/Redis重启旧token无效；多层绝对新鲜度不延长 |
| `RateLimiter` | 达到容量后拒绝并给出 `retryAfter`；时钟前进后恢复；站点隔离 |
| `DistributedLock` | 已持有时他人获取失败；释放只对持有者生效；租约到期后可被他人获取 |
| `BufferStore` | `schedule` 保留较早的到期时间；`pollDue` 只返回到期成员且原子取出；`drainCounters` 取出后清零；站点隔离 |

#### 4.8.5 扩展点一致性测试

扩展点没有“替身与实现一致”的问题，但有所有实现都必须遵守的通用规则。testkit 为这些扩展点提供一致性套件，实现方继承并提供样例输入：

| 扩展点 | 套件检查的规则 |
|---|---|
| `ContentTypeProvider` | 定义合法（类型格式、默认 URL 规则可展开）；`routeHints` 对同一输入结果一致 |
| `PublishContributor` | `partKey` 格式；`contribute` 对同一草稿两次调用得到相同 JSON（4.5.3）；快照中只有 `$content` / `$media` / `$term` 引用、没有 URL；`restore(contribute(x))` 后再 `contribute` 结果不变；`validate` 不抛出异常 |
| `DataSourceProvider` | 样例参数通过 `paramsSchema`；结果不超过 `request.limit()`；只返回该语言已发布的内容；列表类结果带缓存标签 |
| `SchemaOrgProvider` | 节点带 `@type` 与 `@id`（用上下文提供的 ID 生成）；URL 只来自 `RefResolver` |
| `SearchDocumentProvider` | 只读快照（不调用草稿接口）；`facets()` 的 key 与 `attributes` 一致 |
| `TranslatableContentProvider` / `TranslatableResourceProvider` | `extract` 的 key 唯一且两次提取一致；不提取空文本；`apply` 幂等；部分回写保留缺少的 key 的现值；`RICH_INLINE` 只还原编号占位标签，其他标记按纯文本写入（系统设计 26.3） |
| `MediaReferenceCollector` | `fieldPath` 格式与 `MediaUsageInfo` 一致；同一输入结果一致 |
| `PersonalDataProvider` | `find` 与 `export` 的记录数一致；`erase` 后 `find` 的 `recordCount = 0`；`erase` 幂等；只处理当前站点 |
| `NotificationTemplateProvider` | 模板 key 格式 `{模块}.{场景}.{接收方}`；正文中引用的变量都已声明；STAFF 模板只有中文 |

示例（节选）：

```java
package com.b2bweb.testkit.conformance;

public abstract class TranslatableContentProviderConformance {

    protected abstract TranslatableContentProvider provider();

    /** 实现方准备一个源语言已发布、目标语言本地化已存在的样例，返回翻译上下文。 */
    protected abstract TranslationContext sampleContext();

    /** 读取目标语言草稿中由本实现负责的文本（key → 文本），用于断言回写结果。 */
    protected abstract Map<String, String> readTarget(TranslationContext ctx);

    @Test
    @DisplayName("extract：key 唯一、两次提取结果一致、不含空文本")
    void extractIsStable() {
        List<TranslationUnit> first = provider().extract(sampleContext());
        List<TranslationUnit> second = provider().extract(sampleContext());

        assertThat(first).isEqualTo(second);
        assertThat(first).extracting(TranslationUnit::key).doesNotHaveDuplicates();
        assertThat(first).allSatisfy(u -> assertThat(u.text()).isNotBlank());
    }

    @Test
    @DisplayName("apply：部分回写时保留缺少的 key 的现值，重复回写结果不变")
    void partialApplyKeepsOtherKeysAndIsIdempotent() {
        TranslationContext ctx = sampleContext();
        List<TranslationUnit> units = provider().extract(ctx);
        Map<String, String> all = pseudoTranslate(units);                  // 每个单元 "[de] " + 原文
        provider().apply(ctx, all);

        Map<String, String> partial = Map.of(units.getFirst().key(), "geändert");
        provider().apply(ctx, partial);
        provider().apply(ctx, partial);

        Map<String, String> target = readTarget(ctx);
        assertThat(target.get(units.getFirst().key())).isEqualTo("geändert");
        units.stream().skip(1).forEach(u -> assertThat(target.get(u.key())).isEqualTo(all.get(u.key())));
    }
}
```

#### 4.8.6 事件契约

`DomainEventContract` 用类路径扫描找到 `com.b2bweb.api.event` 中全部实现 `DomainEvent` 的类，逐个检查：

| 检查 | 规则 |
|---|---|
| 形式 | 是 record；类名以 `Event` 结尾；第一个组件是 `EventMeta meta`；集合组件不可变 |
| 序列化 | testkit 的 `EventSamples` 为每个事件提供一个样例（缺少样例则失败，迫使新增事件时补充）；用平台的 Jackson 配置往返序列化后与原值相等；JSON 中 ID 为字符串、含 `schemaVersion` |
| 个人数据 | 组件名不得是 `email`、`phone`、`name`、`ip`、`message`、`fields` 等（启发式检查；确有必要时须在本章写明理由并加入白名单） |
| 目录一致 | 全部事件都出现在 4.2.4 总表（经 `ContractCatalog`）中，反之亦然 |

#### 4.8.7 覆盖检查与 CI

testkit 中的 `ContractCatalog` 以代码形式登记 4.2 的四张总表（契约名、包、提供方或收集方、小节号）。`ContractCatalogTest` 检查：

1. `com.b2bweb.api` 中每个公开接口、每个事件都已登记，登记项都存在（防止“只改了代码没改总表”或反之）；
2. 每个服务接口有 testkit 替身（`InMemory<接口名>` 或 `Fake<接口名>`）与 `<接口名>Contract`；每个端口有假实现与 `<端口名>Contract`；每个事件有样例；
3. 替身自身通过对应的契约套件（testkit 自己的测试）。

CI 脚本 `tools/checks/contract-coverage` 读取 `ContractCatalogTest` 导出的目录（`target/contract-catalog.json`），检查每个服务接口的提供方模块中存在 `<接口名>ContractTest`、每个端口的适配器存在 `<适配器类名>ContractTest`。提供方实现尚未合入时（替身先行阶段），目录中该项标记为 `pending`，集成看板显示为“契约测试未通过”（13.5）；里程碑验收前不得存在 `pending` 项。

全部契约测试在每个 PR 中运行（2.5“每个 PR”一行）；失败时不得合入。替身与真实实现不一致按框架缺陷处理，由框架组优先修复（13.7）。

### 4.9 契约变更流程

本节落实 ADR-018 与 2.3“契约的生命周期”：契约在 M0 结束时冻结（`platform-api` 1.0.0），之后的任何变更都按本节流程进行。M0 期间的契约草案可以直接修改，但同样需要架构负责人审核。

#### 4.9.1 角色与权限

| 角色 | 可以做 | 不可以做 |
|---|---|---|
| 模块负责人 | 提出契约变更请求（CCR）；评审影响本模块的变更；等待期间在本模块内部定义防腐接口（2.3） | 直接修改 `platform-api`、`platform-testkit` 与本章（CODEOWNERS 拦截）；把模块内部接口放到任何共享位置 |
| 框架组 | 初审 CCR；起草 ADR 与签名；在同一个 PR 中实施四部分（本章文本、`platform-api` 代码、testkit 替身、契约测试）；维护替身 | 未经架构负责人批准合入契约变更 |
| 架构负责人 | 批准 CCR 与 ADR；`platform-api`、`platform-core`、`docs/framework` 的必审人（2.6） | — |
| 提供方模块负责人 | 在同一个 PR（或紧随其后的 PR，期间替身已可用）中更新真实实现，并通过契约测试 | 在契约之外增加使用方可见的行为 |

#### 4.9.2 流程

| 步骤 | 内容 | 产出 | 时限 |
|---|---|---|---|
| ① 提出 | 仓库 Issue，标签 `contract-change`；模板字段见 2.3：需要的能力、调用方向（是否符合系统设计 3.3 R2 与 4.1.3 的 L2 方向表）、选择服务接口 / 事件 / 扩展点的理由（P3）、提议的签名、受影响的模块、替身与契约测试的变化 | CCR | — |
| ② 初审 | 框架组检查：能否用现有契约满足；调用方向与层次；是否引入环；兼容性分类（4.1.8）；是否需要同步系统设计 | 结论：接受 / 修改后接受 / 拒绝并给出替代方案 | 2 个工作日 |
| ③ ADR | 兼容新增：简写 ADR（背景、签名、受影响模块）；不兼容变更：完整 ADR（含备选方案与迁移计划）；澄清：不需要 ADR（流程见 `docs/adr/README.md`） | `docs/adr/NNNN-*.md`，状态 Accepted | 随 PR |
| ④ 实施 | 同一个 PR 修改四部分：本章（签名、Javadoc、总表 4.2、修订记录 1.8）、`platform-api`、testkit 替身、契约测试；涉及系统设计示意代码或表格时同步修改系统设计并更新其版本号与修订记录 | PR | — |
| ⑤ 评审 | 架构负责人 + 另一名评审人批准（2.6）；CODEOWNERS 自动请求受影响模块负责人评审；CI 全部通过（含契约测试与覆盖检查） | 合入 | — |
| ⑥ 发布与通知 | 合入后提升版本号（4.9.3），在 `platform-api` 的 CHANGELOG 中记录；在受影响模块的 Issue 中留言并在周集成会上通报（2.5）；不兼容变更附迁移说明与截止里程碑 | 版本、通知 | 合入当天 |
| ⑦ 迁移与清理 | 使用方迁移到新契约；废弃项在过渡期结束且没有使用方后删除（CI 列出仍在使用废弃 API 的模块） | 删除 PR | 过渡期见 4.9.3 |

#### 4.9.3 版本与兼容

| 变更类型（4.1.8） | 例子 | 版本 | 过渡 |
|---|---|---|---|
| 兼容新增 | 新增服务接口、扩展点、事件、端口；接口新增方法（扩展点必须是 `default` 方法）；record 末尾新增组件；新增枚举值、错误码 | 次版本 +1（如 1.1.0） | 无 |
| 不兼容变更 | 删除或改名；改变参数、返回类型或语义（空值、事务、异常） | V1 期间不提升主版本：以“新增替代 + 废弃旧契约”实施（次版本 +1）；过渡期结束后删除旧契约（次版本 +1，修订记录中列出） | 旧契约标记 `@Deprecated(since = "1.x", forRemoval = true)`，Javadoc `@deprecated` 指向替代；至少保留到下一个里程碑结束 |
| 澄清 | Javadoc 与本章措辞，不改变行为 | 修订号 +1（如 1.0.1） | 无 |

- 4.1.8 表中标为“不兼容（主版本）”的变更，在 V1 期间按上表第二行实施（与 2.3 一致）；`platform-api` 的主版本保持 1，主版本升级只在 V2 规划中另行决定（ADR）。
- **事件的不兼容变更**：V1 是单体内进程投递，发布方与订阅方随同一次部署变更，因此在同一个 PR 中修改事件类、全部发布方与订阅方，并覆盖 `schemaVersion()` 返回新版本；部署前确认 `EVENT_PUBLICATION` 中该事件类型没有未完成的记录（写入部署检查清单），否则旧 JSON 无法按新结构反序列化。V2 事件外部化后，按 4.1.8 在过渡期内同时发布新旧两个版本。
- **扩展点新增方法**必须提供“等价于不参与”的默认实现；**端口新增方法**必须是 `default` 方法，或在同一个 PR 中更新全部适配器与假实现（4.1.8）。
- 模块的 `module.json` 用 `platform` 字段声明兼容的平台版本范围（如 `>=1.0.0 <2.0.0`），启动时由模块管理器校验（附录 A）；V1 期间各模块无需因次版本变化修改该字段。

#### 4.9.4 紧急情况与例外

| 情况 | 处理 |
|---|---|
| 替身与真实实现行为不一致、契约测试遗漏 | 按缺陷处理：先补契约测试用例，再修复替身或实现；框架组 1 个工作日内响应（风险与对策见 13.7） |
| CCR 等待期间阻塞开发 | 使用方在本模块内部定义防腐接口与本地替身先行开发；契约合入后改为调用 `platform-api`（2.3） |
| 契约存在安全缺陷 | 可以先合入修复再补 ADR（3 个工作日内补齐），仍需架构负责人批准；不兼容时同样提供过渡 |
| 发现本章与系统设计不一致 | 先在 PR 或 Issue 中指出，再修改：签名与示意代码的细节以本章为准（第 4 章开头）；架构约束与原则以系统设计和状态为 Accepted 的 ADR 为准；本章的“框架补充”在合入时同步回系统设计 |

### 4.10 实现前设计问题的统一补充方案（M0 契约草案）

本节对应[11 项设计问题处理说明](../reviews/design-issue-resolution-2026-10-20.md)，是可评审的 M0 补充草案。ADR-019～ADR-025 均为 Proposed，尚未评审通过；本节不宣称 platform-api 已冻结或测试已通过。评审前不得把这些新增签名视为现成实现。本节明确替代哪些旧草案语义；Accepted ADR 的模块边界、站点隔离和人工审校规则继续适用。

#### 4.10.1 MEDIA-01 / MEDIA-02：事务内媒体引用登记

采用 [ADR-019](../adr/0019-transactional-media-references.md)。media 不调用 ContentApi；废止 4.4.16 使用方表中 media 的快照读取资格。当前发布快照由 content 在组装后直接 `ReferenceSet.scan`；草稿由 content 在 `updateDraftMeta` / `markDraftChanged` 内收集 `MediaReferenceCollector`，资源由所属模块从保存后的完整对象收集。三者都在原保存事务提交前调用 core 提供的 ReferenceWriteApi。

```java
// com.b2bweb.api.common，core 提供；只允许在已有数据库事务内调用
public interface ReferenceWriteApi {
    void replaceMedia(MediaReferenceOwner owner, List<MediaReference> references);
}
// com.b2bweb.api.media；均隐式使用当前站点
public record MediaReferenceOwner(String ownerType, long ownerId,
                                  @Nullable String locale, ReferenceStage stage) {}
public enum ReferenceStage { DRAFT, PUBLISHED }
// core 收集的事务 SPI，由 media 唯一实现；不是业务模块对 media 的反向读取接口
public interface MediaReferenceSink {
    void replace(MediaReferenceOwner owner, List<MediaReference> references);
}
```

- `ownerType` 为 CONTENT 或已登记资源类型；内容按 `(contentId, locale, stage)` 替换全部 part，资源只用 PUBLISHED。内容的共享字段按每个本地化分别登记，避免覆盖其他语言；owner 的行锁/共同 version 在登记前已取得。`references` 必须是完整集合，空集合明确清除。
- core 仅协调 SPI，不读取业务表；media 仅写 mda_ 表。启动时缺少/重复 MediaReferenceSink 失败，不能退化成无保护保存。其他 L2 不新增 requires(media/content)，L2 同步调用方向表保持不变。
- sink 对新增集合涉及的 mda_media 行按 ID 升序 `SELECT ... FOR SHARE`，确认同站点、deleted_at IS NULL且符合引用用途；对可发布内容要求 READY，草稿允许 PROCESSING，QUARANTINED 与不存在拒绝。共享锁持有到原事务结束，再按 owner 全组替换 mda_usage；保存/发布失败会同时回滚引用。
- 删除在独立 media 事务中先对媒体行 `FOR UPDATE`，再以当前读检查 mda_usage（例如按site_id/media_id查询id并FOR UPDATE，不能用事务开始时的一致性快照或普通COUNT代替）。保存先获得共享锁时，删除等待并看到已提交引用后拒绝；软删除先提交时，保存必须根据deleted_at拒绝（行仍存在）；物理删除先提交时按不存在拒绝。删除时不回调源对象，不引入反向锁顺序。
- `force` 仍只用于没有引用追踪、由业务模块管理的私有附件；带任何受保护引用的资产不能强制删除。普通软删除只写deleted_at、递增媒体/门控版本并失效公开访问，保留对象；恢复持同一排他锁校验对象及扫描状态并再递增版本。到期purge才在事务中重验保留期/无引用（保留期取已FOR UPDATE锁定的媒体行或同一锁定查询的到期表达式，禁止另用旧RR快照判断），创建media.purge_object持久任务并同事务写mda_media.purge_job_id；标记非空拒绝恢复，提交后幂等删对象；清理完成前不允许恢复。可使用既有sys_job保存清理义务，不能只在内存afterCommit里发起删除。私有无引用附件强制清理同样记录持久义务。
- 下线在原事务清除 PUBLISHED，草稿仍保留；源对象物理删除清除其所有阶段。历史修订不登记，回滚恢复为草稿时重新验证与登记。源对象删除、资源删除、翻译回写及批量导入也必须经过同一保存入口。
- 旧的 ContentDraftChanged/Published/Unpublished 事件不得异步覆盖这个权威引用集合。media 的旧“读快照→整组替换”监听器删除；补偿由源模块任务重新取得源对象锁、读取当前对象并重走 coordinator。停用模块仍保留引用，purge 清理例外继续执行。

对照 4.5.16：MediaReferenceCollector 的签名保留，收集方改为 content（草稿）、资源所属模块/core（资源），media 不再负责主动读取源内容。站点资料同样由 core 保存入口调用 coordinator。当前设计库没有存量数据；后续升级必须在禁止资产删除的维护窗口重建引用，完成一致性核对后才开放删除。

#### 4.10.2 ARTICLE-01 / TRACKING-01：独立资源生命周期

采用 [ADR-025](../adr/0025-site-resource-lifecycle.md)，补登记 `TranslatableResourceTypes.ARTICLE_AUTHOR = "article.author"`，resourceId 为 authorId。article 加入 4.2.3 TranslatableResourceProvider 实现方与 4.6.11 源资源事件发布方；中文职务/简介实际变化才触发翻译，头像、排序、开关变化不冒充翻译事件。

```java
// com.b2bweb.api.event；源资源事务内登记，不含邮箱、脚本、密钥、头像 URL 等正文
public record SiteResourceChangedEvent(EventMeta meta, String resourceType,
                                       String resourceId, ChangeKind change) implements DomainEvent {
    @Override public boolean deliverWhenModuleDisabled() { return true; }
}
```

| 资源与发布方 | 必须发布的变化 | 订阅方与处理 |
|---|---|---|
| article.author / article | 新建、保存、删除/恢复；含头像及非文本变化 | delivery 清除 settings；作者快照已固化在文章中，不擅自改历史发布稿；media 引用已在源事务由 4.10.1 登记，不异步重写 |
| tracking.setting / tracking | trk_setting、统计开关及同意策略保存 | delivery 清除 settings；运行时从当前配置重新组装 |
| tracking.script / tracking | 脚本新建、保存、删除、启停与类别变化 | delivery 清除 settings；不把脚本正文放入事件 |
| url.pattern / url | 模式新建、修改、删除 | content请求全部配置语言的当前树重算；delivery清整站；url源事务先持有route_tree锁并advance |

同一事务同时存在可翻译文本变化时，另发 TranslatableResourceChangedEvent；translation 回写发 TranslationCompletedEvent，禁止递归发源语言翻译事件。资源翻译标签表补 `article.author → settings`。SiteResourceChangedEvent覆盖deliverWhenModuleDisabled()返回true，保持停用时的缓存清除义务；已提交事件仍须处理。所有失效按 4.10.3 重试；重复事件重新失效安全，不凭 occurredAt 丢弃失效。原 core SiteSettingsChangedEvent 保持 core 发布，不由 tracking 冒充 core。模块停用后仍投递本事件进行失效。

#### 4.10.3 DELIVERY-02：缓存回填保护与可靠失效

采用 [ADR-020](../adr/0020-cache-invalidation-fences.md)。保留 TaggedCache.getOrLoad；补充其强语义：回源前原子获取站点缓存代次 `(incarnation UUID, counter)`，任何 invalidate / evict / evictNamespace 都先推进 counter，再删除相关键。回源结束后的写入与代次比较在同一 Lua 操作中完成；代次变化时丢弃回填，不能把旧结果登记成新缓存。缓存命中也验证代次，标签仅用于物理清理。V1 接受一次失效逻辑上使整站缓存过期的额外回源成本，以换取无遗漏保护。

```java
// com.b2bweb.api.port；补入 TaggedCache；没有 Redis 可用代次时不允许缓存写入
public record CacheWriteFence(String incarnation, long counter) {}
// TaggedCache 的两个新增方法已登记在4.7.14完整接口。
```

新增方法的签名见4.7.14。公开缓存链使用getWithFreshness/getOrLoadWithFreshness；CacheEntry/CachedValue携带freshUntil，父结果取全部依赖的最早freshUntil，loader不得以当前时间重置来自下层命中的时刻。putIfUnchanged的ttl由调用者截断为freshUntil的剩余寿命，过期不写；原get/getOrLoad包装方法只用于无需继续缓存的内部消费。`getOrLoad` 内部自动执行相同步骤。原无 fence 的 put 不能用于数据库/远端回源结果；V1 使用方全部迁移，不能以该方法绕过保护。Redis 重启或状态被清空必须产生新的 incarnation，旧进程 token 永不重新有效。代次与条目写入原子操作，不能使用会在条目存活时先过期的代次键。

可靠性采用已有 Event Publication Registry：事件监听器直接 invalidate 并 purgeTags，二者都成功后才正常返回；任一步失败就抛出，保留未完成投递。V1 暂不在可靠路径去抖；BufferStore 只作可丢失的优化提示，pollDue 的结果不等于业务完成。若后续引入合并任务，必须在同一数据库事务中先提交带 eventId 幂等键的 JobApi 任务，监听器才可完成；任务失败、告警、人工重试与保留期不可丢弃未完成义务。

- 发布、更新、下线始终清除 content、list(type) 及受影响 form/term/settings 标签，不只首次发布清 list；路径变化同时处理旧/新路径。
- Redis 不可用时读绕过缓存，失效抛错重试；本地缓存 Pub/Sub 只加速，保留硬 TTL。恢复门闩覆盖读、beginLoad和写；旧RDB/AOF恢复出的ready不能直接开闸。单节点原型以Lua读取本次server run_id并核对control.boot/epoch，进程变化即关闭缓存读写；恢复轮换epoch，使各站点惰性生成新incarnation，再重放恢复边界内义务；开闸须CAS当前boot/epoch，过期恢复者不能放行。站点代次不设置先于条目的TTL；计数器按精确十进制比较，溢出前轮换incarnation。生产恢复边界须持久记录并协调，不能把一次pending=0查询当跨MySQL/Redis原子证明；无证明时继续绕过缓存。该内部机制不新增跨模块接口，Cluster/ACL/性能另验。
- 60 秒是正常依赖可用时的端到端新鲜度预算，不能承诺故障期间瞬时一致。后端Delivery/API及site引导响应通过服务端生成的X-Content-Fresh-Until传递最早依赖时刻，Nuxt只信任自身后端头；CDN-Cache-Control以该时刻的剩余秒数向下取整，<=0返回no-store。公开响应从回源开始计龄，回源上限 5 秒、各层共享绝对过期时刻且余寿命至多 55 秒；CDN 转发 Age、不重新给命中响应完整 TTL，禁止 stale-if-error / stale-while-revalidate 突破上限。超时回源不缓存；积压/清除失败告警，隐私和权限变化不靠长 TTL 掩盖。
- 检查时序：loader 读旧值→事件提交→invalidate→loader 结束；重启后旧 token；Redis 成功/CDN 失败；pollDue 后宕机；列表中已有内容改标题和分类。

#### 4.10.4 SEARCH-01：事务变更日志与不可变索引代次

采用 [ADR-021](../adr/0021-immutable-search-generations.md)。V1 保留 Meilisearch，新增 search 自有的 sch_ 控制表，DDL 只在 search 文档。正式查询从 sch_index_state.active_index 取得物理索引名，不再假定固定别名 + swap。原 SearchIndexNames.content 只表示逻辑索引标识；物理名 `{逻辑名}_g_{buildId}`，每次尝试使用全新 buildId，V1 不调用 swap。

```java
// com.b2bweb.api.common，core 提供，事务内协调可选的 search 变更捕获 SPI
public interface ProjectionChangeApi {
    void searchChanged(Set<String> locales, Set<Long> localizationIds, boolean fullRefresh);
}
// com.b2bweb.api.extension，由 search 实现；缺失/站点停用时不阻塞事实保存
public interface SearchChangeCapture {
    void append(Set<String> locales, Set<Long> localizationIds, boolean fullRefresh);
}
// SearchEngineClient.scanDocuments 已登记在4.7.6，用于复制只读活跃代次。
```

`fullRefresh=true` 时 ID 集合必须为空；有限对象变化用完整 ID 集合。site 隐式来自上下文，locale 为站点配置语言（包括待关闭语言），单批≤500；大范围配置/模块/分类变化用 fullRefresh。core 不查询业务表，只调用 SPI。源模块的本地写事务在提交前经 ProjectionChangeApi 登记；append 按语言排序锁住 sch_index_state 行，递增 change_seq 并写 sch_change，锁持有到源事务结束，序号顺序因此也是提交顺序。source 回滚时日志也回滚，不能在异步事件监听后才建立这条日志。searchChanged先在当前事务收集意图，合并重复ID并注册beforeCommit回调，所有源行修改完成后再一次调用append；超过500个ID升级为fullRefresh。回调失败必须回滚源事务，不允许afterCommit补日志。

| 必须接入的事实写入口 | 捕获范围 |
|---|---|
| content 发布/下线/回收/恢复/物理删除/删除本地化 | 受影响本地化；purge 必须先取得 IDs 再删除，源语言跟随下线一起登记 |
| url.applyRoute / retireRoute、子树重算 | 实际变更的本地化；与路由事务一起捕获 |
| 分类名/搜索字段语义变化、索引设置与语言公开状态、模块启停 | fullRefresh；core 设置与模块事务也调用 coordinator |
| 新语言/模块重新启用 | 同事务 fullRefresh；站点停用搜索时保留指针但不查询，重新启用强制全量重建 |

事务顺序：先锁源聚合，再按 locale 排序锁索引状态；捕获后不再获取其他源聚合锁，联动写应收集完成后一次登记。构建任务不在持有索引状态锁时读取源对象，避免与源事务反向死锁。序号、指针和构建认领都由数据库保护，Redis 租约仅减少重复工作。

构建/增量流程：

1. 在短事务中记录 `(expectedActiveVersion, buildFence, H0=change_seq)` 并分配唯一 generation。完整重建从ContentApi.scanPublished开始，日志起点为H0；增量批次可用新增scanDocuments从不再变化的active generation复制，此时日志起点必须为该活跃代次的applied_seq（记为C），不能从当前change_seq=H0开始而漏掉(C,H0]。同一短事务把base_index及start_seq写入sch_build，GC保护被存活构建引用的基础代次。复制成本为 O(N)，按站点/语言短窗口合并变更；这是 V1 明确的正确性取舍，必须用 1 万产品/5 千文章基线评估吞吐与延迟。
2. 私有 generation 应用设置、写全部文档，每次保存 taskId 并 await 到 SUCCEEDED；PROCESSING 继续观察，FAILED/CANCELED 不发布。若写入/设置请求超时或断联导致是否被引擎受理无法确认，必须废弃整个私有代次，不得重试后继续发布同代次；迟到请求只能落入废弃索引。每个build仅一个本地执行流发送写入，失联恢复不接管同一 generation，而是创建新 generation、提高数据库 buildFence；迟到的旧 worker 只会写其私有废弃代次。
3. 从sch_change重放`(start_seq,H]`（全量start_seq=H0，复制start_seq=C）。对象变更重读当前发布态：存在且公开则构建，否则删除；fullRefresh 使本次增量复制失效，重新走完整扫描。扫描期间的新建、删除、改路径均有同事务日志，不依赖 eventId、时间戳或一次扫描充当屏障。日志重放后再次取 H，持续追平。
4. 所有引擎任务成功后，短事务锁 sch_index_state；仅当 `change_seq==已追平 H`、activeVersion 和 buildFence 均匹配时 CAS 更新 active_index、active_version、applied_seq，标记 build ACTIVE。变化则继续追平，已被新 attempt 接管则废弃。切换与最后源变更的捕获争用同一行锁，不能绕过屏障。
5. 切换后 generation 永远不可写；后续事件只调度下一代构建；事件丢失唤醒时定时扫描change_seq>applied_seq的状态行兜底。源事实可能继续变更，搜索仍是最终一致读模型；公开查询批量核对当前发布/模块/语言/路径，不复活已下线内容，total 为估算。
6. 切换事务前宕机，旧指针继续服务；提交后响应丢失，读取指针和 build 状态即可确认，不执行第二次 swap。旧索引保留到在途查询最大超时之后（至少 5 分钟），GC必须排除active、BUILDING/READY目标、所有存活构建的base_index与未过保留期的代次；按 buildFence 取消的旧 worker 无发布资格。未完成构建使用的日志不清理，重建失败不能把当前指针切成空。

新增持久控制信息必须正常备份；Meilisearch 文档仍可重建。sch_change 只存 ID、locale、序号和 fullRefresh，不存正文/个人信息。持续写入导致追不平时保留旧代次、报告积压，重新调度；不能强制越过 CAS。测试需覆盖捕获事务回滚、扫描边界写入、并发发布/下线、租约过期旧 worker、切换前后 kill、日志保留与 GC，包含所有适配器与替身。

#### 4.10.5 URL-02：由内容内核编排当前路由重算

采用 [ADR-022](../adr/0022-current-route-recalculation.md)。重算任务归 content（JobHandler `content.recalculate_routes`），url 只按已有 reservePath/applyRoute 更新自己的表，不反向调用 page/content/taxonomy。

```java
// ContentApi.requestRouteRecalculation 已登记在4.4.16。
// core 提供数据库事务互斥服务（platform-api/common），不是租约锁
public interface TransactionMutexApi {
    long lock(String name);       // 加锁并返回当前 generation，锁随本事务提交/回滚释放
    long advance(String name);    // 同一事务加锁并递增 generation
}
```

锁由 core 的 sys_transaction_mutex 表实现，唯一 `(site_id,name)`；以INSERT ... ON DUPLICATE KEY UPDATE id=id取得排他锁（新id来自IdGenerator），再SELECT FOR UPDATE读取generation；不使用INSERT IGNORE共享锁再升级的实现。lock/advance必须加入已有事务（MANDATORY），无事务先拒绝，不另开事务或在autocommit下返回已释放的锁；不设自动过期。`name` 仅允许 `{moduleId}.{用途}` 白名单，禁止用户传任意键。提交前释放连接或失联导致事务回滚，不能继续写。所有页面层级、影响 routeHints 的字段、slug 发布、URL 模式、语言前缀保存入口，**在任何源对象行锁之前**取得 `content.route_tree` 互斥锁；有效拓扑/路径输入变化 advance，统一锁顺序避免 page→content 与 job→page 死锁。多站点批处理逐站执行。初始化阶段宜用短事务预建已注册的站点互斥行；不同站点首次建行仍可能争用唯一索引间隙，不能承诺零物理争用。1213死锁与1205锁超时必须回滚整个调用方事务，由外层限次重试，不能保留源写后只重试加锁语句。候选集合的发布/下线/删除/恢复变化同样属于有效输入变化，需要advance，不能只对页面移动推进代次。

requestRouteRecalculation 以最新generation与locales提交JobApi任务，幂等键包含站点/任务类型、generation和规范化语言集合（去重排序后编码或摘要）；同集合不同顺序是同一请求，不同集合不得因generation相同而丢任务。content 对 SiteSettingsChangedEvent 的 URL_PREFIX_CHANGED 以及 SiteResourceChangedEvent(url.pattern)订阅补交同类任务；事件只作调度，执行时不把 payload 当成当前事实。

相关路由源写入及重算批次显式采用READ_COMMITTED，且先取得事务互斥锁再读取业务输入，禁止复用等待前建立的旧快照。每次恢复/批次先取该事务互斥锁，再复核JobApi当前执行租约及任务可运行状态；不能仅比较可能复用的worker_id，旧认领版本不得写路由或检查点。租约失效、FAILED暂停或取消时不得继续提交批次。content 从自己的 cnt_ 表取得当前可路由本地化，经 ContentTypeProvider.routeHints 获取当前业务树与参数，建立父先于子的候选顺序，再调用 reservePath/applyRoute。父路径来自本批已更新或先前已提交的当前 url 路由。批次最多100项，遇路径冲突该批回滚、记录冲突并暂停，不改 slug、不覆盖第三方路由。

任务记录起始generation及不透明检查点（语言、最后完成的contentId和完成位置）。同generation、同语言集合内按确定的父先子后序列定位检查点后继，不能用WHERE content_id > lastId分页：父ID可能大于子ID。检查点找不到或位置不一致必须拒绝，不能静默跳过。generation变化则放弃旧游标，从当前树重新排序重算，以保留原语言范围的新目标替代旧任务；旧任务不能继续覆盖新树。每批的url路由、自动重定向、content路径投影、JobApi检查点和search变更捕获必须同事务提交或回滚；完成所有源/检查点写入后才按语言排序执行捕获。每批发布对应ContentPathChangedEvent，事件也在此事务登记。重试读取当前路由，不重复生成跳转链；取消保留已提交批次，下一任务从当前事实修复。只承诺任务最终完成后路径一致，不把多批更新描述为全站原子切换。

#### 4.10.6 FORM-01 / NOTIFICATION-02：原子下载提交与通知权限

ADR-023只新增`form → notification`的同步服务方向，form.requires包含notification；notification不反向读form/media。NotificationApi.send只在调用方事务内验证模板/有效语言并写ntf_message，SMTP不在事务内。原“签名失败保留已提交记录”的流程取消。

| 提交结果 | 提交记录 / 询盘 | 下载票据 | 下载邮件 | 对外结果 |
|---|---|---|---|---|
| ACCEPT且非测试，DOWNLOAD | 同事务保存；提交事件由inquiry幂等处理 | 校验当前门控与对应content/locale的PUBLISHED引用，本地签名并保存download_grant | 同事务入发件箱，dedupKey=download:{submissionId}:{mediaId} | 仅原事务提交成功后返回成功及已保存URL |
| QUARANTINE | 保存待审；inquiry待审 | 不签发 | 不发送 | 普通成功响应，不泄露垃圾评分 |
| REJECT | 按既有反垃圾保留策略保存；不创建询盘 | 不签发 | 不发送 | 普通成功响应 |
| 测试提交 | 测试记录，按既有运维测试通知规则 | 不签发 | 不发客户下载邮件 | 测试成功响应 |
| 签名、模板/有效语言或发件箱写入失败 | 整个原事务回滚，提交事件也回滚 | 不对外暴露任何签名URL | 无已提交发送义务 | 明确失败，允许相同幂等键重试 |

正常提交的顺序：限流/幂等命中→验证码及业务校验→原事务写提交和关联附件→MediaApi.issueGatedDownload(GatedDownloadCommand)→把URL和固定expiresAt写入frm_submission.download_grant→NotificationApi.send→事务提交→HTTP序列化成功响应。事务内只做数据库及本地签名，禁止对象存储、SMTP等网络I/O。

- 签发在已有表单事务中以媒体FOR SHARE当前读完成资格校验，持锁至调用方事务结束；门控修改/软删除持FOR UPDATE，避免用旧媒体快照签发。
- 媒体保存`gated_form_key`与单调`download_gate_version`（media独占）；门控修改、软删除与恢复使门控版本增加。ticket只在受信任form调用方签发，签名含siteId/submissionId/mediaId/contentId/locale/formKey/gateVersion/expiresAt/keyId，不含个人信息。
- 下载入口验签并检查期限、当前未删除/PRIVATE/READY、门控表单与版本，以及该content/locale的PUBLISHED媒体引用，再签不超过60秒且不晚于票据到期的对象URL；此前已发出的对象URL按其期限到期。取消门控、移除发布引用或软删除立即阻止新访问；media不回查form表。签名密钥轮换按keyId验证，旧钥至少保留到仍合法票据最长到期时间。
- 无状态签名没有数据库回滚能力。保证是：提交前不向HTTP、日志或外部邮件暴露票据，rollback的download_grant和发件箱都不存在；不声称能撤销已外泄签名。提交后门控版本及访问时校验承担撤销条件。
- 幂等重放直接返回首次已提交的download_grant，不重新签名、延长expiresAt或再次消耗验证码；过期/已撤销票据仍是首次结果，客户端应发起新的下载请求，而非用旧幂等键申请新授权。无提交记录表示此前事务未提交，可以重试。并发首次请求由唯一键决定唯一提交，失败方回滚且不留下第二个事件/发件箱。唯一键冲突后的重放必须在失败事务回滚后使用新事务读取首个已提交结果；同一RR事务里的旧快照可能仍看不到赢家。先按既有规范化请求核对幂等键复用，内容不同时409，不可把任何唯一键冲突都当成合法重放。
- 模板/有效语言缺失在NotificationApi.send入队时抛NOTIFICATION_TEMPLATE_NOT_FOUND，使原DOWNLOAD提交失败；已入队后发生SMTP故障才进入可观察重试/FAILED。故障不能被表述为邮件恰好一次送达。

NOTIFICATION-02使用已有`translation:memory:review`，按站点、目标locale、localeScoped及有效用户筛选；翻译失败/过期通知同规则。无合法接收人记录审计/指标并告警，不广播扩大权限，不新增两段权限码。

#### 4.10.7 content #1：内容级人工审校策略与版本化回写

ADR-024采用唯一权威字段`cnt_content.translation_review_required`，不在cnt_localization复制该字段。ContentInfo返回内容version与该策略；LocalizationInfo.translationReviewRequired只是内容字段的读取投影。setter完整签名在4.4.16，按contentId与expectedContentVersion写入。

1. page等可信内容拥有者在模板/法律身份保存的同一事务调用setter；不能从公共表单或任意HTTP布尔字段直接赋值。新建legal内容在create后、事务提交前写入true。目标语言本地化只关联同一内容策略，不异步复制一个可能过期的false。
2. 所有已有内容的保存入口在业务表写入前调用lockDraftForWrite：先锁cnt_content再锁目标本地化，并验证共同version。发布/回滚/模式变更等内核写操作同样遵守父先子后；批量内容按contentId排序。路由输入事务的content.route_tree锁在这些锁之前。父锁持有期间不能调用外部引擎。
3. setter先对父内容行FOR UPDATE并比较expectedContentVersion，再锁该内容全部本地化。策略实际变化时递增cnt_content.version及各本地化共同version，取消其PENDING审核，APPROVED/IN_REVIEW退回EDITING；旧审批与旧完成事件失效。值未变化且版本匹配为无副作用返回；版本过期仍报CONTENT_VERSION_CONFLICT。策略本身不制造新发布修订、不改已发布正文快照。
4. translation在引擎调用/记忆匹配之前读取目标version并写入trn_job_item.expected_target_version，带入TranslationContext.expectedTargetVersion；重试恢复不能把旧输出绑定到重新读取的新version。捕获入口必须在任务项锁内判断已有值；已有值原样保留，不得因恢复/再次进入初始化逻辑而覆盖。只读预估extract可没有该值，实际内容apply必填。
5. 回写事务先校验任务租约，再lockDraftForWrite(expectedTargetVersion)与当前源修订/模式；不匹配时标记该项SUPERSEDED并不调用任何apply。先apply非拥有者provider，最后apply拥有者provider；后者仅一次用非空expectedVersion调用updateDraftMeta，从而哈希覆盖全部贡献并递增一次共同版本。任一步失败，全部业务行、引用与状态回滚。禁止翻译用null expectedVersion。
6. 内容TranslationCompletedEvent.targetVersion为上述回写后的新共同版本。content消费事件时取得父/子锁，重新检查AUTO、源修订、目标version、当前内容策略以及provider.requiresHumanReview。任何不符都不自动发布；合法但要求人工审校的译文进入待审。MANUAL不允许apply，资源翻译的targetVersion为空。
7. legal的自动发布始终拒绝；人工发布同时要求合法审核/发布权限及当前草稿的审核。模板合法移出legal时，拥有者根据当前元数据决定策略；不能仅凭旧事件中的humanReviewRequired=false解除要求。清除再开启策略也单调推进version，不能发生旧事件ABA复活。

只读接口与事件不会引入第二个策略真值。ContentApi、page/translation流程、DDL归属、testkit和ADR-024必须遵守以上同一语义；没有已发布1.0.0二进制，当前属于M0草案修正。

#### 4.10.8 GRAPH-01：人工关系集合的持久版本

不修改已有 grh_relation 行结构，新增 graph 自有 grh_relation_set（DDL 见模块 4.3.6），唯一 `(site_id,source_content_id)`，version 从0起永久单调增长，集合为空也保留状态行，彻底 purge 才删除。GET在显式REPEATABLE_READ只读事务的同一快照中读取集合及version，返回 ETag `"v{version}"`；PUT 必须 If-Match，缺失428，过期412。

写入按sourceContentId升序，对全部受影响集合逐个执行排他INSERT ... ON DUPLICATE KEY UPDATE id=id初始化及SELECT FOR UPDATE当前读；不先INSERT IGNORE取得共享锁再升级。按同一顺序锁定全部受影响集合（含反向关系的两端）；在事务内比较 expectedVersion，再替换人工/排除关系、递增全部受影响集合 version、登记 RelationChangedEvent。多个源行锁必须一次排序后取得，不能先持有A再发现B<A补锁；冲突整体回滚，不允许部分反向行成功。AUTO 重算、pinned、模式修改、清理目标等只要改变 GET 返回集合也遵守相同锁与 version 协议；高成本算分在事务外，写回前检查计算所依据版本，过期丢弃/重排。

不能复用内容草稿 version，不能用 Redis 租约保证原子性，不能用 JSON 摘要伪造平台 vN 协议。图谱集合版本由模块内部实现，不新增跨模块 API。

#### 4.10.9 登记、数据模型与实施门槛

| 新增/调整项 | 提供方 / 收集方 → 使用方 / 实现方 | 实施要求 |
|---|---|---|
| ReferenceWriteApi、ProjectionChangeApi、TransactionMutexApi（common） | core → 业务保存入口 | 同事务、站点上下文、失败回滚；不得在 core 依赖业务模块 |
| MediaReferenceSink（media SPI） | core 收集 → media 唯一实现 | mda_usage 权威引用；进程内事务调用 |
| SearchChangeCapture（extension SPI） | core 收集 → search 可选实现 | 模块未装/停用时 no-op；重新启用 fullRefresh |
| SiteResourceChangedEvent | article、tracking、url → delivery；url.pattern另给content | 事务登记，停用也失效，只有 ID 不含正文 |
| TranslatableResourceTypes.ARTICLE_AUTHOR | article → translation | 同步 4.2.3、4.6.11、testkit 资源清单 |
| CacheWriteFence、CachedValue/CacheEntry.freshUntil及四个方法；scanDocuments；ContentApi 两方法/读取字段 | 端口/content → 原使用方 | 适配器、替身、契约测试同批实现 |
| sys_transaction_mutex | core 独占 | 6 章追加 DDL；数据只经 TransactionMutexApi 访问 |
| sch_index_state / sch_change / sch_build | search 独占 | search 第4章；sch_ 登记模块前缀；控制表需要备份 |
| grh_relation_set | graph 独占 | graph 第4章；新迁移追加，不改旧迁移 |

新增ContentApi/TaggedCache/SearchEngineClient签名已并入原完整接口；正式冻结前把其余新增SPI/协调器并入4.2逐项目录，移除已被替代的旧草案路径，生成代码骨架、OpenAPI/Schema 与 testkit；当前文档阶段在 4.2 加统一登记入口，避免把 Proposed 方案伪装成已发布契约。新接口的事务要求采用 MANDATORY，批量限额、不可变集合、错误映射沿用 4.1；并发/故障测试矩阵见处理说明。本节所有示意省略 import；新协调器/SPI的完整签名在本节，已有接口的增量签名在其原小节，行为以本节为补充。


---

## 5. 后端框架：platform-core 与基础设施

本章说明 platform-core（L0）与 infrastructure 如何实现第 4 章的契约与系统设计的运行机制（系统设计 3.6–3.8、4.1、5.2、5.3、23–27）。每节先说明“框架提供什么”，再说明“模块如何使用”。

- 本章的类名、配置键、锁名与指标名是 M0 的实现约定；代码与配置为示意（见本文开头的阅读提示）。与第 4 章的签名或语义不一致时，以第 4 章为准。
- platform-core 的表结构见第 6 章；框架表（`EVENT_PUBLICATION`、`flyway_{模块 ID}_history` 等）与数据库通用约定见第 7 章；测试替身与测试切片的用法见第 9 章。
- 标注“框架补充”的内容是对系统设计的细化，评审确认后同步修订系统设计（1.2）。

### 5.1 能力总览

#### 5.1.1 能力清单

| # | 能力 | 框架提供（主要类，均在 `com.b2bweb.core` 或 `com.b2bweb.infrastructure` 下） | 模块如何使用 | 小节 |
|---|---|---|---|---|
| 1 | 启动与模块管理 | `ModuleManager`、`ModuleDescriptorLoader`、`ModuleMigrator`、`SiteModuleService`、`ModuleGuardFilter` | 编写 `module.json`、`<Id>Module`（继承 `AbstractPlatformModule`）与迁移脚本；不写启停判断代码 | 5.2 |
| 2 | 扩展点注册表 | `DefaultExtensionRegistry` | 用 `@Extension` 声明实现；经 `ExtensionRegistry` 收集 | 5.3 |
| 3 | 请求上下文 | `RequestContext`、`ContextHolder`、站点上下文过滤器、`PlatformContextTaskDecorator` | 注入 `SiteContextProvider`、`CurrentUserProvider` 读取；异步执行只用平台提供的执行器 | 5.4 |
| 4 | Web 层约定 | `@AdminController` 等注解、`GlobalExceptionHandler`、分页 / 版本 / 幂等参数解析器、Jackson ID 模块 | 按约定写控制器；抛出 `PlatformException` 子类 | 5.5 |
| 5 | 安全 | 三条 `SecurityFilterChain`、登录 / TOTP / 会话、`RequiresPermissionAspect`、`PermissionCheckerImpl`、`AesGcmSecretCipher`、内部令牌 | `@RequiresPermission`；`PermissionChecker.dataScope`；`SecretCipher`；`RateLimiter` 端口 | 5.6 |
| 6 | 数据访问 | MyBatis-Plus 配置、`SiteLineHandler`（行级拦截）、`AuditFieldsMetaObjectHandler`、`TsidIdGenerator`、JSON TypeHandler、持久化基类 | PO 继承 `BasePO` / `SitePO`；Mapper 只用 `#{}`；事务放在应用服务 | 5.7 |
| 7 | 事件 | Spring Modulith 配置、`ModuleListenerAspect`、`DefaultEventMetaFactory`、`ProcessedEvents`、重新投递与清理任务 | 在事务内 `publishEvent`；`@ApplicationModuleListener` | 5.8 |
| 8 | 异步任务与调度 | `JobApiImpl`、`JobExecutor`、`ScheduledTaskRunner`、ShedLock | 实现 `JobHandler` / `ScheduledTask`；`JobApi.submit` | 5.9 |
| 9 | 缓存 | `LocalCaches`、`CacheInvalidationBus`；infrastructure 的 `RedisTaggedCache` | `TaggedCache` 端口；`LocalCaches` | 5.10 |
| 10 | 审计、设置、字典、界面文案 | `AuditApiImpl`、`SettingsApiImpl`、`DictApiImpl`、`UiTextApiImpl`、`SiteProfileApiImpl` | 第 4 章的接口 | 5.11 |
| 11 | 基础设施适配器 | infrastructure 中 17 个端口的适配器（4.7） | 只注入端口，不依赖 infrastructure | 5.12 |
| 12 | 可观测性 | 结构化日志、指标、链路追踪、健康检查、Sentry | 按命名规范输出指标；日志不含个人数据 | 5.13 |
| 13 | 配置与 Profile | `application` 的配置文件、运行模式、生产安全检查 | `b2bweb.<id>.*` 配置属性类 | 5.14 |

#### 5.1.2 包结构与开发支撑包

```text
backend/platform/platform-core/src/main/java/com/b2bweb/core/
├── package-info.java        @ApplicationModule(displayName = "平台核心")
├── CoreModule.java          core 的 PlatformModule 实现（描述符 resources/module.json，附录 A.5）
├── support/                 ★ 开发支撑包：业务模块唯一可以编译依赖的 core 代码（下表）
├── boot/                    模块管理器：描述符加载与校验、拓扑排序、迁移、安装升级、声明式资源同步、运行模式
├── extension/               ExtensionRegistry 实现
├── context/                 RequestContext、ContextHolder、过滤器、TaskDecorator、SiteContextProvider 实现
├── web/                     过滤链装配、全局异常处理、Jackson 配置、OpenAPI 分组
├── security/                Spring Security 配置、登录、TOTP、会话、权限切面、内部令牌、密文加解密
├── persistence/             MyBatis-Plus 配置、行级拦截、字段填充、ID 生成
├── event/                   监听器包装、事件元数据、登记表维护、已处理事件记录
├── job/                     sys_job 执行器、定时任务调度、sys_lock
├── cache/                   本地缓存注册与广播失效
├── observability/           日志字段、指标、健康检查
└── site/ user/ role/ setting/ dict/ audit/ uitext/ profile/ dsr/
                             core 自身的业务功能（按 3.4 的分层：domain / application / infrastructure.persistence / web.admin / facade）
```

**开发支撑包**（`com.b2bweb.core.support..`，3.3 说明 2）：

| 包 | 内容 | 小节 |
|---|---|---|
| `support.module` | `AbstractPlatformModule`（`<Id>Module` 的基类，按模块 ID 取得已加载的描述符） | 5.2 |
| `support.web` | `@AdminController` / `@PublicController` / `@InternalController`、`ApiPaths`、`@SystemLevel`（不需要站点上下文的系统管理接口）、`@PageDefault` / `@Sortable`、`@IfMatchVersion`、`ETags`、`@IdempotencyKey`、`@Idempotent`、`ClientInfo` | 5.5 |
| `support.validation` | 自定义约束 `@LocaleCode`、`@HttpUrl`、`@PlainText`；`JsonSchemaValidator`（输出 `FieldError`） | 5.5 |
| `support.persistence` | `BasePO`、`SitePO`、`@GlobalTable`、`JsonTypeHandler` / `JsonNodeTypeHandler`、`SortColumns`、`OptimisticUpdates` | 5.7 |
| `support.event` | `ProcessedEvents`（按 `eventId` 去重的幂等助手） | 5.8 |
| `support.cache` | `LocalCaches`、`BroadcastCache`、`CacheKeys` | 5.10 |
| `support.concurrent` | `PlatformExecutors`（已装配上下文传递的平台执行器与虚拟线程执行器） | 5.4 |
| `support.guard` | `@RequiresModuleEnabled`（服务接口实现的停用检查） | 5.2 |
| `support.log` | `IpAnonymizer`、`LogSafe`（日志脱敏工具） | 5.13 |

规则：

1. 业务模块只能引用 `com.b2bweb.core.support..`；core 的其他包是内部实现（ArchUnit 规则 `modules_only_depend_on_core_support`，第 10 章）。对 core 业务能力的调用一律通过 `platform-api` 的接口（4.1.2）。
2. 支撑包只放技术基类、注解与无业务语义的工具；新增类型由框架组评审。支撑包中的 Spring 组件由 core 的自动配置装配，模块不需要 `@Import`。
3. Spring Modulith中，support子包声明命名接口（如support.event）；业务模块统一使用已由Modulith 2.0.8验证的`allowedDependencies={"api","core :: *"}`。ArchUnit另行限制只能引用core.support，不能利用命名接口绕过内部实现边界。

**测试支撑**：需要 platform-core 运行时的测试切片放在 platform-core 的 test-jar（包 `com.b2bweb.core.testsupport`）中，而不放在 testkit（3.3 说明 3）：

| 类型 | 用途 |
|---|---|
| `@SitePersistenceTest` | MyBatis-Plus 切片 + 行级拦截 + Testcontainers MySQL + 本模块与 core 的迁移；测试方法内自动建立测试站点的上下文 |
| `SiteContextExtension` | JUnit 5 扩展：`@WithSite(siteId = 1)`、`@WithUser(userId = …, permissions = …)` 设置上下文 |
| `TwoSiteFixture` | 站点隔离测试夹具：两个站点 + 只在站点 1 有角色的用户（系统设计 5.3、28） |

> `b2bweb-modules` 的测试依赖需增加 `platform-core` 的 `test-jar`（3.2 的父 POM 清单同步补充）。

#### 5.1.3 M0 验收测试

13.1 第 3 条“platform-core 的运行机制可用”以下列测试通过为准（集成测试使用 Testcontainers 的 MySQL 8.4 与 Redis）：

| 机制 | 测试 | 通过标准 |
|---|---|---|
| 描述符与依赖 | `ModuleDescriptorValidationTest` | 附录 A.3 中检查方为“启动”的每条规则各有一个反例，启动失败且错误信息为中文、指明模块与规则 |
| 迁移与安装 | `ModuleLifecycleIT` | 空库启动：按拓扑序迁移、`onInstall` 各执行一次；提升版本后 `onUpgrade` 执行一次；`onUpgrade` 抛错时状态为 `FAILED` 且拒绝启动；两个实例并发启动只有一个执行迁移 |
| 站点启停 | `SiteModuleToggleIT` | 停用后：`ModuleGuardFilter` 返回 404、注册表不返回实现、监听器跳过、`PER_SITE` 定时任务跳过；被依赖时拒绝停用；重新启用 `firstEnable = false` |
| 扩展点注册表 | `ExtensionRegistryIT` | 4.5.1 的校验规则各一个反例；排序与过滤结果确定 |
| 站点上下文 | `SiteContextPropagationIT` | 后台、前台、内部请求、事件、任务、定时任务、`TaskDecorator` 七种来源的上下文正确；无上下文访问站点级表抛出 `NoSiteContextException` |
| 行级拦截 | `SiteIsolationIT` | 两站点夹具下，站点级表的查询、更新、删除都不能越过站点；`information_schema` 中没有 `site_id` 的表与豁免清单一致 |
| 事件 | `EventReliabilityIT` | 事务回滚时事件不存在；监听器抛错后被重新投递；同一事件投递两次，`ProcessedEvents` 只放行一次；事务外发布领域事件抛出异常 |
| 错误处理 | `ProblemDetailsIT` | 每类异常输出 RFC 9457 响应，`title` / `detail` 为中文，不含堆栈 |
| 安全 | `AdminSecurityIT` | 无会话 401；缺少 CSRF 403；无权限 403；需要 2FA 的角色未验证 403；二次验证窗口；会话绝对过期 |
| 任务与调度 | `JobExecutorIT`、`ScheduledTaskIT` | 认领互斥、心跳超时回收、协作取消、幂等键；ShedLock 单实例执行 |

### 5.2 应用启动与模块管理器

模块管理器（`com.b2bweb.core.boot`）实现系统设计 3.6 的启动流程与平台级状态、3.8 的停用行为，以及附录 A 的描述符校验。

#### 5.2.1 组件

| 类 | 职责 |
|---|---|
| `ModuleDescriptorLoader` | 扫描 `classpath*:module.json`，按 `schema/module.schema.json`（platform-api 中）校验，解析为 `ModuleDescriptor`（4.5.22）；`settings` 等相对路径相对于该 `module.json` 解析 |
| `ModuleGraph` | 附录 A.3 中检查方为“启动”的规则；依赖拓扑排序 |
| `ModuleMigrator` | 获取迁移锁，按拓扑序执行各模块的 Flyway 迁移 |
| `ModuleInstaller` | 与 `sys_module` 比较版本，调用 `onInstall` / `onUpgrade`，维护平台级状态 |
| `DeclarativeResourceSync` | 同步权限、菜单、设置 Schema 等声明式资源 |
| `ModuleManager` | 编排以上步骤，构建扩展点注册表，切换就绪状态；提供 `ModuleCatalog`（只读：描述符、状态、`apiPrefix` → 模块 ID） |
| `SiteModuleService` | 站点级启用 / 停用（`sys_site_module`）与影响分析 |
| `ModuleGuardFilter` | 按 `apiPrefix` 拦截停用模块的接口 |
| `AbstractPlatformModule`（support） | 模块描述类的基类：`descriptor()` 返回模块管理器已加载的描述符 |

模块的写法（以 inquiry 为例）：

```java
package com.b2bweb.inquiry;

/** 询盘模块描述类：模块根包中除 package-info 外唯一的类（3.4）。 */
@Component
public class InquiryModule extends AbstractPlatformModule {

    private final DictApi dicts;

    public InquiryModule(ModuleCatalog catalog, DictApi dicts) {
        super(catalog, "inquiry");                       // descriptor() 取自 inquiry 的 module.json
        this.dicts = dicts;
    }

    @Override
    public void onInstall(ModuleContext ctx) {          // 幂等
        dicts.ensureBuiltin(InquiryDicts.CLOSE_REASON);
        ctx.forEachSite(siteId -> defaultRules.ensureFor(siteId));
    }

    @Override
    public ImpactReport analyzeDisableImpact(SiteModuleContext ctx) {
        return impactService.onDisable(ctx);             // 如“有 35 条未关闭的询盘”
    }
}
```

- 4.5.22 的 `descriptor()` Javadoc 中“由 core 提供的 `ModuleDescriptor.load`”在本框架落地为 `AbstractPlatformModule` + `ModuleCatalog`：`platform-api` 只放契约，不读取 classpath（4.1.1）。
- 每个 `module.json` 必须恰好对应一个 `PlatformModule` Bean，Bean 类位于 `com.b2bweb.<id>` 根包；反之亦然。

#### 5.2.2 启动流程

系统设计 3.6 的八个步骤在 Spring 生命周期中的落点：

| 步骤 | 系统设计 3.6 | 实现 | 执行时机 |
|---|---|---|---|
| 1 | 扫描描述符与 `PlatformModule` | `ModuleDescriptorLoader` 读取全部 `module.json`；`PlatformModule` Bean 的匹配在步骤 5 前完成 | 数据源就绪后、`SqlSessionFactory` 创建前（`ModuleMigrator` 以 `DependsOn` 方式插入，做法同 Spring Boot 的 Flyway 初始化器） |
| 2 | 校验 | JSON Schema 校验 + 附录 A.3 第 1、5–9、15 条；平台版本取自 platform-api jar 中的 `META-INF/b2bweb/platform-version` | 同上 |
| 3 | 获取锁 | MySQL 命名锁 `GET_LOCK('platform:migrate', 超时)`，在专用连接上持有到步骤 6 结束（框架补充：首次启动时 `sys_lock` 尚未创建，且命名锁在连接断开时自动释放；需同步修订系统设计 3.6 第 3 步，第 6 章 `sys_lock` 说明） | 同上 |
| 4 | Flyway 迁移 | 按拓扑序逐模块执行（下文）；记录 `MISSING` 模块 | 同上 |
| 5 | 安装 / 升级 | `ModuleInstaller`：新模块 `onInstall`，版本升高 `onUpgrade` | 容器刷新完成后、接收流量前（`ApplicationRunner`，最高优先级） |
| 6 | 同步声明式资源 | `DeclarativeResourceSync`（5.2.4） | 同上 |
| 7 | 构建扩展点注册表 | `DefaultExtensionRegistry.freeze()`（5.3） | 同上 |
| 8 | 释放锁；就绪 | 释放命名锁；重新投递未完成事件（5.8）；启动任务执行器与定时任务（5.9）；发布 `AvailabilityChangeEvent(ReadinessState.ACCEPTING_TRAFFIC)` | 同上 |

- **拓扑序**：以 `requires ∪ optional` 为边（附录 A.3 第 7 条），Kahn 算法；入度相同时按层号（L0 → L4）再按模块 ID 排序，保证每次结果相同。`core` 恒为第一个。
- **就绪前的请求**：Web 服务器在步骤 5 前已监听端口，`StartupGateFilter` 对 `/api/**` 返回 503（`Retry-After: 5`）直到步骤 8 完成；健康检查的 liveness 为 UP、readiness 为 DOWN（5.13）。
- **任何步骤失败**：记录中文错误（模块、规则、原因），对应模块置为 `FAILED` 并写入 `sys_module.last_error`，进程以非零码退出（系统设计 3.6“阻止启动，保留现场”）。

**Flyway 配置**（每个模块一个 `Flyway` 实例，不使用 Spring Boot 的 Flyway 自动配置：`spring.flyway.enabled = false`）：

```java
Flyway.configure()
      .dataSource(migrationDataSource)                  // 迁移账号（有 DDL 权限，26.6）
      .locations("classpath:db/migration/" + moduleId)  // 3.7
      .table("flyway_" + moduleId + "_history")
      .validateMigrationNaming(true)                    // V{三位序号}__{描述}.sql
      .outOfOrder(false)
      .cleanDisabled(true)
      .load()
      .migrate();
```

| 运行方式 | 步骤 3–4 的行为 |
|---|---|
| `--mode=migrate`（生产发布第 1 步，系统设计 27.3） | 用迁移账号执行步骤 1–6 后退出（退出码 0 / 非 0）；不启动 Web、任务执行器与定时任务 |
| 应用启动，`b2bweb.core.modules.migrate-on-startup = true`（local、test、dev） | 用迁移账号执行迁移（系统设计 3.6 的“兜底”） |
| 应用启动，`migrate-on-startup = false`（staging、prod） | 只做 Flyway `validate`：有未执行或被修改的迁移时拒绝启动，提示“请先运行 migrate 任务”；应用账号没有 DDL 权限（26.6） |

#### 5.2.3 平台级状态与安装、升级

`ModuleInstaller` 维护 `sys_module`（第 6 章）的状态机（系统设计 3.6）：

| 情况 | 处理 | 结果状态 |
|---|---|---|
| `sys_module` 中没有该模块 | 写入 `INSTALLING`（`target_version`）→ 独立事务中调用 `onInstall` → 为全部未归档站点补齐 `sys_site_module` 行（不可停用模块恒为启用；可停用模块按 DDL 默认值启用，并在站点上下文中调用 `onEnable(firstEnable = true)`） | `INSTALLED` |
| 构建版本 > `installed_version` | `UPGRADING` → `onUpgrade(fromVersion)` | `INSTALLED` |
| 构建版本 = `installed_version` | 跳过 | 不变 |
| 构建版本 < `installed_version` | 拒绝启动（附录 A.3 第 9 条：不支持降级） | 不变 |
| 上次为 `FAILED` | 按 `target_version` 重试同一步骤 | `INSTALLED` 或 `FAILED` |
| `sys_module` 中有、构建中没有 | 置为 `MISSING`、记录 `missing_since`；数据保留；`SiteApi.isModuleEnabled` 返回 false | `MISSING` |
| 原为 `MISSING`、重新出现在构建中 | 按版本比较处理（相同版本直接恢复） | `INSTALLED` |

- `onInstall` / `onUpgrade` 在没有站点上下文、以 `ActorRef.SYSTEM` 身份执行；需要逐站点初始化时用 `ModuleContext.forEachSite`（内部即 `SiteContextProvider.runAs`）。每个模块一个独立事务（`REQUIRES_NEW`），失败只回滚该模块。
- `PURGED`（系统设计 3.6）只能经命令行执行：`--mode=purge --module=<id> --confirm=<id> --backup-confirmed`。前置条件：所有站点均已停用该模块、没有已启用模块 `requires` 它。执行 `onPurge` 后删除该模块的 `sys_site_module`、`sys_setting` 行，权限与菜单标记 `removed_at`，写系统级审计。迁移历史表保留。

#### 5.2.4 声明式资源同步（步骤 6）

`descriptor_hash` 与模块版本都未变化时跳过该模块（系统设计 3.5“幂等同步”）；`b2bweb.core.modules.force-sync = true` 时全部重做。

| 资源 | 来源 | 目标 | 同步规则 |
|---|---|---|---|
| 权限 | `module.json` 的 `permissions` | `sys_permission` | 按 `code` upsert（名称、说明、`dataScopes`、`sensitive`、`localeScoped`）；描述符中消失的权限标记 `removed_at`，不参与鉴权；随后重算受影响角色的 `mfa_required`（第 6 章不变量 10） |
| 菜单 | `menus` | `sys_menu` | 按 `key` upsert；消失的标记 `removed_at` |
| 设置 Schema | `settings` 指向的文件 | 内存中的 `SettingsSchemaRegistry`（每次启动加载，不入库） | 顶层属性即设置键 `{id}.{属性名}`（附录 A.3 第 15 条）；校验默认值符合 Schema |
| 组件清单 | `components` | `sys_module.descriptor` | 后台启动数据与编辑器按模块启用状态过滤（3.6） |
| 内容类型、关系类型、通知模板、模板与组件元数据 | 扩展点（`ContentTypeProvider`、`RelationTypeProvider`、`NotificationTemplateProvider`、`TemplateProvider`）与 `component-schemas` | 收集方模块自己的表（content、graph、notification、page） | 由收集方模块同步：模块管理器在本步骤按拓扑序调用各模块的启动同步回调，收集方经 `ExtensionRegistry.allRegardlessOfSite` 读取定义（4.4.15） |

> RC补齐（ADR-027，待评审）：4.5.22的`PlatformModule.onStartup`每次启动在步骤6按拓扑序调用，必须幂等，不依赖扩展点的站点过滤；不得另用顺序不确定的Spring启动事件代替。

#### 5.2.5 站点级启用与停用

后台“模块管理”（`/api/admin/v1/system/sites/{siteId}/modules`，权限 `system:module:manage`，敏感操作需二次验证）调用 `SiteModuleService`：

```text
停用（disable）：
  1. 校验：模块 disableable = true；该站点没有已启用的模块 requires 它（附录 A.3 第 10 条）
  2. 影响分析：在站点上下文中调用 PlatformModule.analyzeDisableImpact → 返回 ImpactReport 给后台展示
  3. 用户确认（请求带 confirm = true 与停用后路由处理 onDisable = NOT_FOUND / REDIRECT）
  4. 事务内（按ADR-029持有sys_site站点行的FOR UPDATE排他锁，串行化本站全部模块启停）：
       更新 sys_site_module（enabled = 0、disabled_at、config、version）
       → onDisable(SiteModuleContext) → 发布 ModuleDisabledEvent → 审计（AuditApi.record，敏感）
  5. 提交后：广播本地缓存失效（core.site_modules，5.10）

启用（enable）：
  校验模块为 INSTALLED、其 requires 已在该站点启用 → 事务内更新 enabled = 1、enabled_at
  → onEnable(firstEnable = 原 enabled_at 为空) → 发布 ModuleEnabledEvent → 审计 → 提交后广播失效
```

停用行为由平台统一实现（系统设计 3.8），模块不写判断代码：

| 对象 | 由谁实现 | 机制 |
|---|---|---|
| 后台菜单与路由 | core | `/api/admin/v1/system/bootstrap` 只返回启用模块的菜单与组件清单；Admin 路由守卫对未返回的前缀显示 404（第 8 章） |
| Admin / Public / Internal API | core | `ModuleGuardFilter`（下文） |
| 服务接口 | 提供方模块 + core 支撑 | 实现类的写方法与面向前台的方法标注 `@RequiresModuleEnabled("form")`，停用时抛出 `ModuleDisabledException`（4.1.3） |
| 事件监听 | core | `ModuleListenerAspect` 跳过并标记完成（5.8） |
| 扩展点 | core | `ExtensionRegistry` 过滤（5.3） |
| 定时任务 | core | `PER_SITE` 任务跳过该站点（5.9） |
| 异步任务（`sys_job`） | core | 提交时抛出 `ModuleDisabledException`；已排队的任务在认领时置为 `CANCELED`（`CORE_MODULE_DISABLED`） |
| 前台组件、内容类型路由 | delivery、url（按组件定义的 `module` 字段与注册表结果） | 见各模块设计文档。停用后的路由处理（`sys_site_module.config` 的 `onDisable`、`redirectContentId`）经 `SiteApi.moduleDisablePolicy` 读取（4.4.1、ADR-029）；delivery、url 不得直接读 `sys_` 表。M0写入口仅接受NOT_FOUND；REDIRECT写入待路由目标校验与引用追踪就绪后开放 |
| 数据 | — | 全部保留 |

**`ModuleGuardFilter`**：

- 匹配 `/api/{admin|public|internal}/v1/{apiPrefix}/**`，经 `ModuleCatalog` 由 `apiPrefix` 找到模块。
- 模块不是 `INSTALLED`，或当前请求有站点上下文且该站点停用了此模块：返回 404 problem+json，`code = CORE_MODULE_DISABLED`（与 `ModuleDisabledException` 一致，4.1.7）。
- `apiPrefix = system`（core）恒放行；没有站点上下文的系统级后台接口只检查平台级状态。
- 位于站点上下文过滤器之后（5.5.1）；启用状态取自本地缓存（5.10），不查库。

### 5.3 扩展点注册表

`DefaultExtensionRegistry`（`com.b2bweb.core.extension`）实现 `ExtensionRegistry`（4.4.15），在启动步骤 7 构建，之后只读。

#### 5.3.1 构建与校验

```text
1. 收集：ApplicationContext.getBeansWithAnnotation(Extension.class)；目标类取 AopUtils.getTargetClass（代理不影响注解读取）
2. 归类：实现类实现的、登记在 ExtensionPoints 目录中的 com.b2bweb.api.. 接口，每个接口一个条目
3. 校验：4.5.1 的规则（模块在构建中且实现类位于 com.b2bweb.<module>..；KeyedExtension.key() 唯一；
        contentTypes 已注册；扩展点特有规则，如同一内容类型的 PublishContributor.partKey() 唯一）
4. 排序：@Extension.order 升序 → 模块 ID → 类名
5. 冻结：point → 不可变列表；(point, key) → 条目；实例 → 模块 ID
```

- `ExtensionPoints`（core 内部常量）列出 4.2.3 的全部扩展点与平台 SPI。testkit 的架构测试检查 `com.b2bweb.api..` 中的每个接口要么是服务接口（4.2.2）、要么是端口（4.2.5）、要么在该目录中，防止新增扩展点漏登记。
- 实现了目录中的接口却没有 `@Extension` 的 Bean 使启动失败（4.5.1）；`PlatformModule` 例外（按 Bean 类型收集，4.5.22）。
- 内容类型校验依赖已注册的类型：先读取 `ContentTypeProvider` 实现提供的类型代码（4.5.2），再校验其他实现的 `contentTypes`。
- 错误信息为中文并指明实现类，例如：`扩展点实现 com.b2bweb.product.extension.ProductSchemaOrgProvider 声明的内容类型 PRODCT 未注册（已注册：PAGE、PRODUCT、ARTICLE…）`。

#### 5.3.2 按站点过滤

| 方法 | 过滤依据 |
|---|---|
| `all` / `forContentType` / `byKey` / `requireByKey` | 当前站点（`SiteContextProvider.requireSiteId()`）启用的模块 |
| `allForSite` / `forContentTypeForSite` | 参数中站点启用的模块 |
| `allRegardlessOfSite` | 不过滤（4.4.15：只用于启动同步、模块安装、诊断）；ArchUnit 限制调用方为 core、各模块的 `PlatformModule` 实现与启动同步代码 |

- 启用模块集合取自本地缓存 `core.site_modules`（站点 ID → 启用且 `INSTALLED` 的模块 ID，含 `core`），模块启停后经 `CacheInvalidationBus` 广播失效（5.10）。
- 过滤结果按“扩展点 + 启用集合”缓存；启用集合变化时整体丢弃。Delivery 热路径上一次 `all()` 的开销是一次本地缓存读取。
- `forContentType`：先按 `@Extension.contentTypes`（空表示全部）过滤，再对实现了 `ContentTypeScoped` 的实现调用 `supports(contentType)`（4.5.1）；`supports` 的结果不缓存（实现方保证无状态且廉价）。
- `moduleOf(extension)` 按 Bean 实例查表（含代理）；`describe(point)` 供后台诊断接口 `GET /api/admin/v1/system/extensions?point=…`（系统管理员）与契约测试使用。

#### 5.3.3 模块如何使用

```java
// 实现方（product）
@Extension(module = "product", contentTypes = "PRODUCT", order = ExtensionOrder.OWNER)
public class ProductSchemaOrgProvider implements SchemaOrgProvider { … }

// 收集方（seo）：已按当前站点过滤并排序
List<SchemaOrgProvider> providers = extensions.forContentType(SchemaOrgProvider.class, contentType);

// 按 key 查找（数据源、任务处理器等 KeyedExtension）
DataSourceProvider source = extensions.requireByKey(DataSourceProvider.class, "content-query");
```

| 规则 | 说明 |
|---|---|
| 不保存实现列表 | 收集方每次调用注册表（站点不同、启用状态会变化）；不得把 `all()` 的结果存入字段 |
| 实现类位置 | `com.b2bweb.<id>.extension`（3.4）；`@Extension.module` 必须等于所在模块 ID |
| 异常处理 | 收集方按 4.5“调用约定”表处理实现抛出的异常 |
| 测试 | 实现方直接对实现类做单元测试；收集方使用 testkit 的示例实现与注册表替身（第 9 章） |

### 5.4 请求上下文

#### 5.4.1 RequestContext 与 ContextHolder

系统设计 5.2 的 `RequestContext` 落地如下（core 内部类型，模块经 `SiteContextProvider`、`CurrentUserProvider` 读取，不直接访问）：

```java
package com.b2bweb.core.context;

/**
 * 当前执行单元（请求、事件处理、任务、定时任务）的上下文。不可变；切换站点时建立新的绑定。
 *
 * @param site      站点上下文（4.3.1）；系统级执行为空
 * @param actor     当前操作人：后台请求为登录用户；任务为发起人；事件、定时任务、前台请求为 system
 * @param clientIp  客户端真实 IP（仅请求；只在可信来源时取自代理头，5.6.5）
 * @param userAgent 请求的 User-Agent（截断到 500 个字符）
 */
public record RequestContext(@Nullable SiteContext site, ActorRef actor,
                             @Nullable String clientIp, @Nullable String userAgent) {}

/** 上下文的绑定与读取：JDK 25 的 ScopedValue（JEP 506）。 */
public final class ContextHolder {
    private static final ScopedValue<RequestContext> CURRENT = ScopedValue.newInstance();
    private ContextHolder() {}

    public static Optional<RequestContext> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    public static <T> T callWith(RequestContext ctx, Callable<T> action) throws Exception {
        return ScopedValue.where(CURRENT, ctx).call(action::call);
    }

    public static void runWith(RequestContext ctx, Runnable action) {
        ScopedValue.where(CURRENT, ctx).run(action);
    }
}
```

> M0候选工程采用成对`Scope`/try-finally管理的不继承ThreadLocal，配合TaskDecorator在工作线程捕获及恢复；上面的ScopedValue为可替换的实现示意，不改变第4章API。事件监听没有登录用户，当前actor为SYSTEM；原发起人保留在事件meta。

- ScopedValue 的绑定随调用栈结束自动解除，不会泄漏给线程池中的下一个任务；嵌套绑定（`callAs`）结束后自动恢复外层上下文。
- 若 ADR-011 锁定的编译级别低于 25（ScopedValue 在 JDK 21 中为预览特性），`ContextHolder` 改用 `ThreadLocal` 实现（在 `runWith` 的 try/finally 中恢复），API 不变。
- `SiteContextProviderImpl`、`CurrentUserProviderImpl`（4.4.2、4.4.6）只读取 `ContextHolder` 与 Spring Security 的 `SecurityContextHolder`；绑定时同步设置日志 MDC 的 `siteId`、`userId`（5.13）。

#### 5.4.2 上下文的来源

| 来源 | 建立者 | 站点 | `requestLocale` | 操作人 / 当前用户 | `ContextOrigin` |
|---|---|---|---|---|---|
| 后台 `/api/admin/v1/**` | `AdminSiteContextFilter` | 请求头 `X-Site-Id`，校验用户在该站点有角色（系统管理员不限）；缺失时只允许访问标注 `@SystemLevel` 的系统管理接口 | 空 | 登录用户 | `ADMIN` |
| 前台经 Nuxt（Delivery） | `PublicSiteContextFilter` | 带有效 `X-Internal-Token` 时取 `X-Site-Host`，经 `SiteApi.resolveHost` 解析 | `locale` 查询参数；没有时取 `path` 参数的首段，经 `SiteApi.localeByUrlPrefix` 解析 | system，无当前用户 | `PUBLIC` |
| 浏览器 → Public API | `PublicSiteContextFilter` | 反向代理透传的 `Host`（系统设计 5.2） | 同上 | 同上 | `PUBLIC` |
| 内部 `/api/internal/v1/**` | `InternalSiteContextFilter` | 可选的 `X-Site-Id`；没有时为系统级 | 空 | system | `INTERNAL` |
| 领域事件 | `ModuleListenerAspect`（5.8） | `DomainEvent.siteId()`；为空时没有站点 | 空 | system；原操作人从事件的 `actor()` 读取（4.6） | `EVENT` |
| 异步任务 | `JobExecutor`（5.9） | `sys_job.site_id`（0 表示没有站点） | 空 | `actor()` 为发起人，`current()` 为空（4.4.6） | `JOB` |
| 定时任务 | `ScheduledTaskRunner`（5.9） | `PER_SITE` 逐站点；`SYSTEM` 没有站点 | 空 | system | `SCHEDULED` |
| 系统级代码切换 | `SiteContextProvider.callAs` / `runAs` | 参数中的站点（不存在时 `CORE_SITE_NOT_FOUND`） | 空 | 不变 | `SYSTEM` |

解析细则：

| 项 | 规则 |
|---|---|
| Host 规范化 | 小写、去掉端口、国际化域名转 Punycode（`IDN.toASCII`）后调用 `resolveHost`（结果缓存，5.10） |
| 未登记的域名 | 前台请求返回 404（`CORE_SITE_NOT_FOUND`）；归档站点的域名同样不解析（4.4.1） |
| 别名域名、`SETUP` 站点 | 过滤器只建立上下文；301 到主域名、建设中返回 503 + noindex 由 Nuxt 与 delivery 按 `HostResolution` 处理（系统设计 13.2） |
| 前台语言 | 显式的 `locale` 参数必须是站点已启用的语言，否则 422（`CORE_LOCALE_NOT_ENABLED`）；路径前缀不认识时 `requestLocale` 为空，由端点决定（delivery 返回 `NOT_FOUND`）；是否对外公开由端点经 `SiteApi.isPublicLocale` 判断（预览允许未公开语言） |
| `X-Site-Id` | 非数字 400；站点不存在 404；用户在该站点没有角色 403（`CORE_FORBIDDEN`）；`ARCHIVED` 站点只允许 GET（后台只读，4.4.1） |
| 切换站点 | 会话绑定用户而不绑定站点，每个请求独立校验 `X-Site-Id`（系统设计 5.3） |

#### 5.4.3 异步执行与上下文传递

core 装配的执行器都带有上下文传递装饰器（模块经 `support.concurrent.PlatformExecutors` 注入）：

| 执行器 | 用途 |
|---|---|
| `applicationTaskExecutor` | `@Async`、`@ApplicationModuleListener` |
| `platformVirtualExecutor` | 虚拟线程：Delivery 并行解析数据源与扩展点、批量 I/O |
| `jobWorkerExecutor` | `sys_job` 执行（5.9） |
| `taskScheduler` | 定时任务（5.9） |

```java
/** 把提交线程的 RequestContext、SecurityContext 与 MDC 带到执行线程；与 Spring 的 ContextPropagatingTaskDecorator（链路追踪）组合使用。 */
public final class PlatformContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        Optional<RequestContext> ctx = ContextHolder.current();      // 在提交线程捕获
        SecurityContext security = SecurityContextHolder.getContext();
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(security);
            if (mdc != null) MDC.setContextMap(mdc);
            try {
                ctx.ifPresentOrElse(c -> ContextHolder.runWith(c, task), task);
            } finally {
                SecurityContextHolder.setContext(previous);
                MDC.clear();
            }
        };
    }
}
```

- `@ApplicationModuleListener` 在 `applicationTaskExecutor` 上执行时，装饰器带来的是发布方的上下文；`ModuleListenerAspect` 随后按事件的 `siteId` 与 system 身份重新绑定并清空 `SecurityContext`（5.8），监听器不会以发布请求的用户身份执行。
- `spring.threads.virtual.enabled = true`：请求线程与 `@Async` 使用虚拟线程（系统设计 2.4）。
- 模块规则（ArchUnit）：不得 `new Thread`、`Executors.newXxx`、不带执行器参数的 `CompletableFuture.supplyAsync / runAsync`；需要并行时使用 `PlatformExecutors`。并行子任务继承提交方的站点，不得在子任务中切换站点。

### 5.5 Web 层约定

#### 5.5.1 路由前缀与过滤链

| 前缀（系统设计 24.1） | 控制器注解（`support.web`） | 模块内的包 | 访问控制 | OpenAPI 分组 |
|---|---|---|---|---|
| `/api/admin/v1/{apiPrefix}/…` | `@AdminController` | `web.admin` | 会话 + CSRF + `@RequiresPermission` | `admin` |
| `/api/public/v1/{apiPrefix}/…` | `@PublicController` | `web.publicapi` | 无会话；限流；只返回发布态数据 | `public` |
| `/api/internal/v1/{apiPrefix}/…` | `@InternalController` | `web.internal` | 内部令牌 + 内网（5.6.5） | 不生成前端类型 |

```java
@AdminController                                                   // = @RestController + 前缀标记
@RequestMapping(ApiPaths.ADMIN + "/inquiry/assignment-rules")      // ApiPaths.ADMIN = "/api/admin/v1"
public class AssignmentRuleAdminController {
    private final AssignmentRuleService service;                   // 只调用应用服务（3.4）
    …
}
```

ArchUnit 检查注解、包与路径一致，且路径中的模块段等于本模块的 `apiPrefix`（附录 A.3 第 3 条）。

**过滤链**（按执行顺序）：

| 顺序 | 组件 | 范围 | 作用 |
|---|---|---|---|
| 1 | `ServerHttpObservationFilter`（Spring） | 全部 | 链路追踪，生成 `traceId`（5.13） |
| 2 | `ClientInfoFilter` | 全部 | 解析真实 IP 与 User-Agent（可信代理规则见 5.6.5） |
| 3 | `StartupGateFilter` | `/api/**` | 启动步骤 8 完成前返回 503（5.2.2） |
| 4 | `FilterChainProxy`（Spring Security） | 三条链按前缀匹配 | 内部令牌、会话、CSRF、认证、2FA 与改密强制（5.6） |
| 5 | 站点上下文过滤器 | 按前缀 | 建立 `RequestContext` 并包住后续全部处理（5.4） |
| 6 | `ModuleGuardFilter` | `/api/*/v1/**` | 停用模块返回 404（5.2.5） |
| 7 | `AdminRateLimitFilter` | `/api/admin/**` | 规则 `core.admin_api`（用户，600 次 / 分钟，4.7.15） |
| 8 | `DispatcherServlet` | — | 参数解析器（分页、版本、幂等键）→ 控制器 → 应用服务（`@RequiresPermission`、`@Transactional` 切面） |

- 响应头：后台与内部接口 `Cache-Control: no-store`；全部接口 `X-Content-Type-Options: nosniff`；公开接口的缓存头由端点设置（delivery）。
- 不开启 CORS：后台与 `/api/admin` 同源部署（ADR-010），前台经站点域名的反向代理访问 Public API。

#### 5.5.2 RFC 9457 错误响应

`GlobalExceptionHandler`（`com.b2bweb.core.web`，继承 Spring 的 `ResponseEntityExceptionHandler`）把异常统一转换为 `application/problem+json`（系统设计 24.2、4.1.7）：

```json
{
  "type": "https://docs.example.com/errors/inquiry-not-found",
  "title": "询盘不存在或无权访问",
  "status": 404,
  "code": "INQUIRY_NOT_FOUND",
  "detail": "询盘 7201893453912345 不存在",
  "instance": "/api/admin/v1/inquiry/inquiries/7201893453912345",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "errors": []
}
```

| 字段 | 来源 |
|---|---|
| `type` | `{b2bweb.core.web.problem-type-base}/{错误码的小写连字符形式}` |
| `title` | `messages/<id>.properties` 中以错误码为键的中文标题（3.4）；没有时取 `ErrorCode.title` |
| `status` | `ErrorCode.status`（唯一例外：经 `If-Match` 提交的版本冲突为 412，见 5.5.6） |
| `code` / `detail` / `errors[]` | 错误码；异常的中文说明；`FieldError` 列表（4.1.7） |
| 扩展字段 | `PlatformException.properties()`（如 `currentVersion`、`moduleId`）；`ExternalServiceException` 另加 `retryable` |

异常映射：

| 异常 | 状态 | 错误码 |
|---|---|---|
| `PlatformException` 的子类 | 取自错误码 | 异常携带的错误码 |
| `MethodArgumentNotValidException`、`HandlerMethodValidationException`、`ConstraintViolationException` | 400 | `CORE_VALIDATION_FAILED`（带 `errors[]`） |
| 请求体无法解析、参数类型不匹配、缺少必填参数 | 400 | `CORE_VALIDATION_FAILED`（`detail` 为“请求格式错误”，不回显原始内容） |
| 未登录、会话过期 | 401 | `CORE_NOT_AUTHENTICATED` |
| Spring Security 拒绝访问 | 403 | `CORE_FORBIDDEN`，或下表中的具体原因 |
| 没有处理器 / 静态资源 | 404 | `CORE_NOT_FOUND` |
| 不支持的方法 / 媒体类型；请求体过大 | 405 / 415 / 413 | `CORE_METHOD_NOT_ALLOWED` / `CORE_UNSUPPORTED_MEDIA_TYPE` / `CORE_PAYLOAD_TOO_LARGE` |
| `ExternalServiceException` | 取自错误码（502 / 503） | 异常携带的错误码；503 时加 `Retry-After` |
| 其他异常 | 500 | `CORE_INTERNAL_ERROR`：`detail` 固定为“服务器内部错误，请稍后重试”；记录堆栈并上报 Sentry |

Web 层自身使用、模块不会抛出的错误码定义在 core 内部的 `CoreWebErrors`（4.1.7：模块内部才用到的错误码定义在模块自己的包中）：

| 错误码 | 状态 | 标题 |
|---|---|---|
| `CORE_NOT_FOUND` | 404 | 资源不存在 |
| `CORE_METHOD_NOT_ALLOWED` | 405 | 不支持的请求方法 |
| `CORE_UNSUPPORTED_MEDIA_TYPE` | 415 | 不支持的内容类型 |
| `CORE_PAYLOAD_TOO_LARGE` | 413 | 请求体过大 |
| `CORE_PRECONDITION_REQUIRED` | 428 | 缺少版本条件 |
| `CORE_RATE_LIMITED` | 429 | 请求过于频繁 |
| `CORE_IDEMPOTENCY_KEY_REUSED` | 422 | 幂等键已用于不同的请求 |
| `CORE_IDEMPOTENCY_IN_PROGRESS` | 409 | 相同的请求正在处理 |
| `CORE_CSRF_INVALID` | 403 | 安全令牌无效，请刷新页面 |
| `CORE_MFA_REQUIRED` | 403 | 需要完成两步验证 |
| `CORE_REAUTH_REQUIRED` | 403 | 需要重新验证身份 |
| `CORE_PASSWORD_CHANGE_REQUIRED` | 403 | 需要修改密码 |
| `CORE_SERVICE_STARTING` | 503 | 服务启动中 |
| `CORE_INTERNAL_ERROR` | 500 | 服务器内部错误 |

规则：

- 错误响应不含堆栈、SQL、类名与第三方异常信息（系统设计 26.3）；4xx 记 INFO 级日志（不带堆栈），5xx 记 ERROR 并上报 Sentry。
- 公开接口的响应结构相同；前端按 `code` 与 `errors[].code` 映射文案键 `error.{错误码}`（3.7），不展示 `detail`。模块写 `detail` 时不得包含个人数据。
- 启动时 `ErrorMessages` 加载 `classpath*:messages/*.properties`（UTF-8）；同一错误码在多个文件中出现时启动失败。

#### 5.5.3 分页与排序

| 场景 | 请求参数 | 控制器参数 | 响应 |
|---|---|---|---|
| 后台列表（4.1.6） | `page`（默认 1）、`size`（默认 20，≤ 100）、`sort`（如 `-createdAt,title`） | `PageRequest`，配合 `@PageDefault(size = 20, sort = "-createdAt")` 与 `@Sortable({"createdAt", "title"})` | 直接返回 `PageResult<T>`，序列化为 `{ "items", "page", "size", "total" }`（系统设计 24.2） |
| 导出与同步 | `cursor`、`size`（≤ 1000） | `CursorRequest` | `CursorPage<T>` → `{ "items", "nextCursor" }` |

- `sort` 中不在 `@Sortable` 白名单的字段、超过上限的 `size` 返回 400（`CORE_VALIDATION_FAILED`，`errors[].field` 为 `sort` / `size`）。
- 持久化层用 `SortColumns`（`support.persistence`）把白名单字段名映射为列名再交给 MyBatis-Plus，不拼接用户输入（系统设计 26.3）；总数由 `PaginationInnerInterceptor` 计算（5.7）。
- 游标由 `Cursors.encode(…)`（最后一行的排序键与 ID，Base64URL 编码的 JSON）生成；无法解码时 400。

#### 5.5.4 JSON 与 ID 序列化

core 提供唯一的 Jackson 3 `JsonMapper` Bean（`platformJsonMapper`），用于 Spring MVC、事件登记表的序列化、`TaggedCache` 的值、JSON TypeHandler 与快照，保证同一对象在各处的 JSON 一致：

| 项 | 规则 |
|---|---|
| ID | `B2bWebJacksonModule`：`long` / `Long` 序列化为字符串，反序列化接受字符串或数字（4.1.5、ADR-014）。规则作用于全部 `long`，因此计数等字段优先使用 `int` |
| 时间 | `Instant` 输出 ISO-8601 UTC，毫秒精度，带 `Z`；`Duration` 为 ISO-8601；不输出时间戳数字（系统设计 24.2） |
| 空值 | 响应省略值为 `null` 的字段；集合永远输出（空数组） |
| 未知字段 | 反序列化时忽略（兼容旧客户端与旧事件） |
| 枚举 | 按名称输出 |
| OpenAPI | springdoc 的模型转换器把 `int64` 声明为 `type: string, format: int64`，生成的前端类型为 `string`（系统设计 24.3） |

#### 5.5.5 校验

| 层 | 做法 |
|---|---|
| 请求 DTO | 控制器参数 `@Valid` + Jakarta 约束注解 |
| 应用服务、`facade` | 类上标注 `@Validated`，启用方法参数校验：跨模块调用时 `platform-api` record 上的约束（如 4.4.22 的 `@Valid` 参数）同样生效 |
| JSON 结构 | `JsonSchemaValidator.validate(schema, json)` → `List<FieldError>`（JSON Pointer 转为点分路径）；用于设置、组件属性、表单定义等 |
| 业务规则 | 应用服务或领域对象中抛出 `ValidationException` / `BusinessException`（模块自己的错误码） |

- `errors[]` 的 `field` 为请求中的属性路径（如 `items[2].name`），`code` 为约束名的大写下划线形式（`NOT_BLANK`、`SIZE`、`LOCALE_CODE`），`message` 为中文。
- 校验消息只用中文：`LocaleResolver` 固定为 `zh-CN`，不按 `Accept-Language` 切换（后台只有中文，语言规则 1）；模块的自定义消息写在 `messages/<id>.properties`（键 `validation.<id>.<名称>`），由 core 的 `MessageSource` 统一加载。
- `support.validation` 的自定义约束：`@LocaleCode`（`Locales.isValid`）、`@HttpUrl`（只允许 http / https，拒绝 `javascript:` 等）、`@PlainText`（拒绝控制字符与 HTML 标签）。

#### 5.5.6 乐观锁、ETag 与 If-Match

系统设计 24.2：“请求体带 `version`，或使用 `ETag` / `If-Match`；冲突返回 409 / 412”。

| 环节 | 约定 |
|---|---|
| 读取 | 可编辑资源的响应体带 `version`；单个资源的 GET 同时返回强 ETag `"v{version}"`（`ETags.of(version)`） |
| 提交 | 请求体带 `version`（后台默认方式）；或请求头 `If-Match: "v12"`，控制器参数 `@IfMatchVersion Integer expectedVersion` 解析 |
| 二者同时出现且不一致 | 400（`CORE_VALIDATION_FAILED`） |
| 都没有 | 端点声明 `@IfMatchVersion(required = true)` 时返回 428（`CORE_PRECONDITION_REQUIRED`）；否则按端点语义处理 |
| 冲突 | 持久化层更新 0 行时抛出 `VersionConflictException`（模块的错误码，409，带 `currentVersion`，5.7.4）；经 `If-Match` 提交的请求由异常处理器改为 412，`code` 不变（框架补充：`ErrorCode.status` 是默认状态） |

#### 5.5.7 Idempotency-Key

系统设计 24.2：公开表单提交与后台导入支持 `Idempotency-Key`，保存 24 小时。

- 请求头格式：8–64 个字符，`[A-Za-z0-9_-]`（建议 UUID）；格式错误 400。控制器参数 `@IdempotencyKey String key`（`required` 可选）。
- 三种落地方式，按优先级选择：

| 方式 | 适用 | 实现 |
|---|---|---|
| 业务唯一键（首选） | 写入本模块表的创建操作 | 幂等键进入命令并保存在业务表中，表上建唯一索引（如 form 的 `uk_sub_idem (site_id, idempotency_key)`）；唯一键冲突时读出首次的结果返回 |
| 任务幂等 | 导入、导出等提交任务的操作 | 传入 `JobRequest.idempotencyKey`（`uk_job_idem`；任务保留期内唯一，比 24 小时更严格，4.4.12） |
| 通用响应重放 `@Idempotent` | 没有天然存储位置的 POST | Redis 键 `s{siteId}:core:idem:{接口标识}:{SHA-256(幂等键)}`，TTL 24 小时；保存请求指纹（方法 + 路径 + 请求体的 SHA-256）、状态（`PROCESSING` / `DONE`）、响应状态与响应体（≤ 64 KB） |

`@Idempotent` 的行为：首次请求写入 `PROCESSING` 后执行；2xx 响应保存为 `DONE`，非 2xx 删除记录（允许客户端重试）；相同键、相同指纹且 `DONE` 时重放响应并加响应头 `Idempotent-Replayed: true`；`PROCESSING` 时 409（`CORE_IDEMPOTENCY_IN_PROGRESS`）；相同键、不同指纹时 422（`CORE_IDEMPOTENCY_KEY_REUSED`）。

### 5.6 安全

实现系统设计 26 章与 ADR-010。core 的安全代码在 `com.b2bweb.core.security`；模块只使用 `@RequiresPermission`、`PermissionChecker`、`CurrentUserProvider`、`SecretCipher` 与 `RateLimiter` 端口。

#### 5.6.1 过滤链与会话

| 链 | 匹配 | 会话 | CSRF | 认证 |
|---|---|---|---|---|
| admin | `/api/admin/**` | Spring Session（Redis） | 开启：Cookie `XSRF-TOKEN` → 请求头 `X-XSRF-TOKEN`（Spring Security 的 SPA 模式） | 会话中的用户；登录相关接口匿名可访问 |
| public | `/api/public/**` | 无（`STATELESS`，不创建会话） | 关闭：公开表单使用 formToken + Turnstile（系统设计 26.3） | 匿名；带有效内部令牌时标记为可信的 Nuxt 请求 |
| internal | `/api/internal/**` | 无 | 关闭 | `InternalTokenAuthenticationFilter`：内部令牌 + 内网地址（5.6.5） |
| 其他 | `/**` | — | — | 拒绝（actuator 在独立的管理端口，5.13）；local 与 test 放行 `/v3/api-docs/**`，供生成 `api-client`（系统设计 24.3） |

| 会话项 | 规定（ADR-010） |
|---|---|
| 存储 | Spring Session Data Redis 的索引型仓库（可按用户查找会话）；键前缀 `sys:core:session` |
| Cookie | `__Host-SESSION; Secure; HttpOnly; SameSite=Lax; Path=/` |
| 过期 | 空闲 2 小时（`maxInactiveInterval`）；绝对 12 小时：`AbsoluteSessionTimeoutFilter` 比较会话创建时间，超过即注销并返回 401 |
| 会话固定 | 登录成功、2FA 验证成功后更换会话 ID |
| 会话内容 | 用户 ID、认证阶段、`mfaVerified`、最近一次二次验证时间；**不保存权限**（每个请求从权限缓存读取，变更即时生效） |
| 注销全部会话 | 修改密码、管理员重置密码、重置 2FA、停用或删除用户时，经索引注销该用户的全部会话（修改密码时保留当前会话） |

#### 5.6.2 登录、两步验证与二次验证

认证接口（`/api/admin/v1/system/auth/…`，core 的控制器）：

| 端点 | 作用 |
|---|---|
| `GET csrf` | 下发 CSRF Cookie（Admin 启动时调用） |
| `POST login` | 邮箱 + 密码 |
| `POST mfa/verify` | 提交 TOTP 验证码或恢复码 |
| `POST mfa/setup`、`POST mfa/activate` | 绑定 TOTP：生成密钥 → 用一个验证码确认 |
| `POST reauthenticate` | 二次验证（密码；已启用 2FA 时再加验证码） |
| `POST password` | 修改密码（需要二次验证） |
| `POST logout` | 注销 |

登录流程：

```text
POST login { email, password }
 1. 限流：RateLimiter.tryConsume(core.login, SHA-256(规范化邮箱) + ":" + IP)（failClosed，4.7.15）→ 拒绝时 429
 2. 按 email_normalized 查找未删除的用户；用户不存在时同样执行一次 Argon2 校验（耗时一致，防止枚举账号）
 3. 账号锁定中（locked_until > now）或密码错误：返回同一条 401 信息“邮箱或密码错误，或尝试次数过多”
      失败时 failed_login_count + 1；达到 5 次起 locked_until = now + 2^(n−5) 分钟（上限 60 分钟）
      AuditApi.recordIndependently（FAILURE）；指标 b2bweb.core.auth.login{outcome=failure}
 4. 成功：清零失败计数；Argon2 参数升级时重新哈希；更换会话 ID；确定认证阶段：
      已启用 2FA，或在任一站点持有 mfa_required 的角色 ─▶ MFA_PENDING（未绑定时为 MFA_ENROLLMENT_REQUIRED）
      must_change_password = 1                         ─▶ PASSWORD_CHANGE_REQUIRED
      否则                                              ─▶ AUTHENTICATED
 5. 审计（login 成功）；返回当前阶段，Admin 据此进入验证、绑定或改密页面
```

- `AuthStageFilter`：阶段不是 `AUTHENTICATED` 时只放行该阶段需要的认证接口与 `logout`，其他请求返回 403（`CORE_MFA_REQUIRED` 或 `CORE_PASSWORD_CHANGE_REQUIRED`）。角色的 `mfa_required` 由权限推导（第 6 章不变量 10），自定义角色同样适用。
- 密码：长度 12–128；Argon2id（`Argon2PasswordEncoder`，参数按 OWASP 建议，由安全基线在 M0 确定）；泄露密码库检查为可选项（`b2bweb.core.security.breached-password-check`，默认关闭）。密码、TOTP 密钥与恢复码不得进入日志与审计差异（ADR-010）。

| TOTP 项 | 规定 |
|---|---|
| 算法 | RFC 6238：HMAC-SHA1、6 位、30 秒步长；接受前后各 1 个步长 |
| 防重放 | `sys_user_mfa.last_used_step`：不新于该步长的验证码拒绝 |
| 密钥 | 160 位随机数；以 `SecretCipher` 信封保存（第 6 章） |
| 绑定 | `mfa/setup` 返回 `otpauth://` URI，二维码由 Admin 在浏览器中生成（密钥不经过第三方服务）；`mfa/activate` 验证通过后启用，并返回 10 个恢复码（只显示一次） |
| 恢复码 | 每个128位随机数，以32位十六进制显示；独立环境密钥B2BWEB_RECOVERY_CODE_KEY作HMAC-SHA256哈希保存，行锁内单次消费；不随SecretCipher密钥轮换 |
| 实现 | core 基于 JDK `javax.crypto.Mac` 自行实现，不引入额外依赖 |

**二次验证**：`@RequiresPermission(reauthenticate = true)`（4.4.7）的方法要求会话中最近一次二次验证在 `b2bweb.core.security.reauth-window`（默认 10 分钟）之内，否则返回 403（`CORE_REAUTH_REQUIRED`）；Admin 的 HTTP 层弹出验证框，调用 `reauthenticate` 后自动重试原请求。没有登录用户的执行（任务、事件）不适用。

#### 5.6.3 权限与数据范围

`PermissionCheckerImpl`（4.4.7）与 `RequiresPermissionAspect`：

| 项 | 实现 |
|---|---|
| 权限快照 | 按（用户，站点）计算：`sys_user_site_role`（含语言范围）→ `sys_role_permission`（含数据范围）→ 未标记 `removed_at` 的 `sys_permission`；多个角色的数据范围取最大（ALL > GROUP > OWN） |
| 缓存 | Redis `s{siteId}:core:perm:{userId}`（TTL 10 分钟，系统设计 23.1）+ 请求内缓存；角色、角色权限、用户站点角色变化与权限同步后删除相关键 |
| 系统管理员 | `has` 恒为 true，`dataScope` 为 ALL，`localeScope` 不限 |
| 切面顺序 | `Ordered.HIGHEST_PRECEDENCE + 100`：在事务切面之前执行，无权限时不开启事务 |
| 没有登录用户 | 来源为 `JOB`、`EVENT`、`SCHEDULED`、`SYSTEM`（4.3.1）时跳过（任务在提交时已校验）；来源为 `ADMIN` 时 401；`PUBLIC`、`INTERNAL` 调用了带权限注解的方法时 403（属于编程错误） |
| 拒绝 | 抛出 `ForbiddenException(CORE_FORBIDDEN)`，扩展字段带 `permission`；敏感权限的拒绝经 `AuditApi.recordIndependently` 记录（同一用户同一权限 1 分钟内只记一次） |
| 启动检查 | 全部 `@RequiresPermission` 中的权限码都已在描述符中声明（附录 A.3 第 12 条的运行时兜底） |

数据范围在拥有数据的模块的查询层过滤（系统设计 26.2）：

```java
@RequiresPermission("inquiry:inquiry:view")
@Transactional(readOnly = true)
public PageResult<InquiryListItem> list(InquiryQuery query, PageRequest page) {
    long me = currentUser.require().userId();
    ScopeFilter filter = switch (permissions.dataScope("inquiry:inquiry:view").orElseThrow()) {
        case ALL   -> ScopeFilter.all();
        case GROUP -> ScopeFilter.groups(salesGroups.groupIdsOf(me));   // GROUP 的含义由 inquiry 解释
        case OWN   -> ScopeFilter.owner(me);
        default    -> throw new IllegalStateException("未知的数据范围");     // 4.1.8：switch 必须有 default
    };
    return repository.search(query, filter, page);
}
```

语言范围：按语言操作的方法用 `permissions.canAccessLocale("translation:memory:review", locale)` 校验（系统设计 5.8）。

#### 5.6.4 限流

- 端口 `RateLimiter`（4.7.15），适配器 `Bucket4jRedisRateLimiter`；键、规则名与 Redis 故障时的放行 / 拒绝策略见 4.7.15。
- core 使用 `core.login`（登录，5.6.2）与 `core.admin_api`（`AdminRateLimitFilter`，维度为用户 ID）。
- 模块在自己的包中定义规则常量（4.7.15），在控制器或应用服务中调用端口；被拒绝时抛出本模块的 429 错误码（如 `FORM_RATE_LIMITED`），并按 `RateLimitDecision.retryAfter` 设置 `Retry-After`。规则的容量与周期可以用 `b2bweb.<id>.rate-limits.<规则名>` 覆盖默认值。

#### 5.6.5 内部令牌与可信代理

| 项 | 规定（系统设计 26.7） |
|---|---|
| 令牌 | 请求头 `X-Internal-Token`；配置 `b2bweb.core.internal.tokens`（由环境变量注入的列表，至少 32 字节随机值）：第一个为当前令牌，其余在轮换期内同样接受；比较前先做 SHA-256，再用 `MessageDigest.isEqual` 等时比较 |
| 可信头 | 只有令牌有效时才读取 `X-Site-Host` 与 `X-Client-IP`（Nuxt 写入的访客 IP）；否则忽略这两个头 |
| 反向代理 | 不带令牌的请求，客户端 IP 只在直连地址属于可信代理时取自 `X-Forwarded-For`（`server.forward-headers-strategy = native` + `server.tomcat.remoteip.internal-proxies`），否则取直连地址 |
| `/api/internal/**` | 令牌有效且直连地址属于 `b2bweb.core.internal.allowed-cidrs`（默认仅回环；local启动脚本额外注入本次Docker网络子网，生产须显式配置来源网段）；不满足时 403，不说明原因 |
| 轮换 | 后端追加新令牌 → 部署 → Nuxt 切换到新令牌 → 后端删除旧令牌 → 部署（运维手册） |
| 记录 | `ClientInfo`（`support.web`）提供规范化后的 IP 与 UA；日志中的 IP 按 5.13 截断 |

#### 5.6.6 密钥与 SecretCipher

`AesGcmSecretCipher` 实现 `SecretCipher`（4.4.10）：

| 项 | 规定 |
|---|---|
| 算法 | AES-256-GCM；12 字节随机 nonce（`SecureRandom`）；128 位认证标签 |
| 信封 | `v1:{keyId}:{Base64(nonce‖密文‖tag)}`（4.4.10） |
| 密钥来源 | 环境变量 `B2BWEB_SECRET_KEYS`，格式 `k2:<Base64 的 32 字节>,k1:<…>`，第一个为当前密钥（系统设计 26.6）；local 与 test 使用文档中公开的开发密钥，生产安全检查拒绝它（5.14） |
| 轮换 | 新密钥加到列表首位并部署 → 系统管理员提交 core 的系统级任务（`sys_job` 类型 `core.secret_reencrypt`）重新加密 `sys_setting`（`encrypted = 1`）与 `sys_user_mfa`；保存信封的模块（如 translation 的 `trn_engine`、notification 的 Webhook 密钥）按 `needsReEncryption` 各自重新加密 → 全部完成后从列表移除旧密钥 |
| 使用 | 只在使用前解密，明文以 `Secret` 传递（`toString` 为掩码），不缓存、不记录；后台显示用 `mask` |

### 5.7 数据访问

#### 5.7.1 MyBatis-Plus 配置

```java
@Bean
MybatisPlusInterceptor mybatisPlusInterceptor(SiteLineHandler siteLine) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(siteLine));      // 1 站点过滤（类名是框架命名，列名为 site_id）
    PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(1000L);                                                   // 与 CursorRequest.MAX_SIZE 一致
    interceptor.addInnerInterceptor(pagination);                                     // 2 分页
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());         // 3 乐观锁
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());              // 4 禁止无条件的全表更新与删除
    return interceptor;
}
```

| 项 | 规定 |
|---|---|
| Mapper | 只放在 `com.b2bweb.<id>.infrastructure.persistence`，标注 `@Mapper`；XML 位于 `classpath*:mapper/<模块 ID>/*.xml`（资源名带模块 ID，3.4） |
| 命名映射 | `map-underscore-to-camel-case = true`；枚举按名称存储 |
| `${}` | 禁止（CI 扫描 XML 与注解，系统设计 26.3）；动态排序用 `SortColumns` 白名单（5.5.3） |
| 时间 | JVM 与连接时区均为 UTC（JDBC 参数 `connectionTimeZone=UTC&preserveInstants=true&forceConnectionTimeZoneToSession=true`）；`Instant` ↔ `DATETIME(3)`（系统设计 25.1） |
| 访问范围 | 只访问本模块前缀的表；不跨模块 JOIN（系统设计 25.5）；CI 扫描 SQL 中的表前缀 |

#### 5.7.2 站点过滤与豁免清单

```java
@Component
final class SiteLineHandler implements TenantLineHandler {

    /** 没有站点上下文时抛出 NoSiteContextException：宁可失败，不可漏过滤。 */
    @Override public Expression getTenantId()       { return new LongValue(siteContexts.requireSiteId()); }
    @Override public String getTenantIdColumn()     { return "site_id"; }
    @Override public boolean ignoreTable(String t)  { return exemptions.contains(t); }
}
```

- SELECT、UPDATE、DELETE 自动追加 `site_id = 当前站点`（JOIN 中每个站点级表都追加），因此不能按 ID 修改其他站点的数据；INSERT 的 `site_id` 由字段填充写入（5.7.3），拦截器不重复追加。
- 豁免清单（`SiteTableExemptions`）的来源：

| 来源 | 表 |
|---|---|
| core 的全局表（第 6 章） | `sys_site`、`sys_module`、`sys_user`、`sys_user_mfa`、`sys_role`、`sys_permission`、`sys_role_permission`、`sys_menu`、`sys_dict_type`、`sys_dict_item`、`sys_lock`、`sys_dsr_request`、`sys_audit_log`（`site_id` 可空，查询显式带条件）、`sys_processed_event`（5.8） |
| 框架表（第 7 章） | `EVENT_PUBLICATION`、`flyway_{模块 ID}_history` |
| 模块的全局表 | PO 类标注 `@GlobalTable`（`support.persistence`）；模块设计文档“站点过滤”一节（模板 4.5）写明理由；新增需架构负责人审核（`CLAUDE.md` 禁止 10） |

- `SiteIsolationIT`（5.1.3）对比 `information_schema` 中没有 `site_id` 列的表与豁免清单，两边必须一致。

**绕过拦截器的查询**（只允许下列情况）：

| 情况 | 做法 |
|---|---|
| 系统级行 `site_id = 0`（`sys_setting`、`sys_job`） | core 内部 Mapper 方法标注 `@InterceptorIgnore(tenantLine = "true")`，SQL 中显式带 `site_id = 0` |
| 站点上下文建立之前的查询（按域名解析站点、列出“我可访问的站点”） | 同上，只在 core 中 |
| 系统管理接口的跨站点查询 | 方法名以 `AcrossSites` 结尾，只被 `@SystemLevel` 接口或系统级任务调用 |
| 管控 | 每处 `@InterceptorIgnore` 登记在 `tools/checks/interceptor-ignore-allowlist.txt`，CI 比对；新增需架构负责人审核 |

#### 5.7.3 持久化基类与字段填充

```java
package com.b2bweb.core.support.persistence;

/** 通用字段（系统设计 25.3）。PO 是可变类（MyBatis-Plus 需要 setter），不使用 record。 */
public abstract class BasePO {
    @TableId(type = IdType.ASSIGN_ID)            private Long id;          // TsidIdentifierGenerator → IdGenerator
    @TableField(fill = FieldFill.INSERT)         private Instant createdAt;
    @TableField(fill = FieldFill.INSERT)         private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)  private Instant updatedAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)  private Long updatedBy;
    // getter / setter 省略
}

/** 站点级表的 PO。 */
public abstract class SitePO extends BasePO {
    @TableField(fill = FieldFill.INSERT)         private Long siteId;
}
```

```java
// 模块中的 PO（inquiry）：version 与 deleted_at 按需声明
@TableName(value = "inq_assignment_rule", autoResultMap = true)
public class AssignmentRulePO extends SitePO {
    private String name;
    @TableField(typeHandler = JsonTypeHandler.class) private RuleCondition condition;   // JSON 列
    @Version private Integer version;
    @TableLogic(value = "null", delval = "now(3)") private Instant deletedAt;
}
```

`AuditFieldsMetaObjectHandler` 的填充规则：

| 字段 | 插入 | 更新 |
|---|---|---|
| `id` | `IdGenerator.nextId()` | — |
| `site_id` | 当前站点；没有上下文时抛出 `NoSiteContextException` | 不更新 |
| `created_at` / `updated_at` | 注入的 `Clock`（4.1.5） | `updated_at` |
| `created_by` / `updated_by` | `CurrentUserProvider.actor().userId()`（系统为 0） | `updated_by` |

- 插入时代码已显式赋值的字段不覆盖（如导入保留原始创建时间）；更新时 `updated_*` 总是覆盖。DDL 中的 `ON UPDATE CURRENT_TIMESTAMP(3)` 只作兜底。
- 关联表与日志表不强制全部通用字段（系统设计 25.3），这类 PO 不继承基类，按表结构声明字段。

#### 5.7.4 乐观锁

- 可编辑实体带 `@Version Integer version`（DDL 默认 0），拦截器生成 `UPDATE … SET version = version + 1 WHERE id = ? AND version = ?`。
- 应用服务用 `OptimisticUpdates`（`support.persistence`）执行更新：

```java
// expectedVersion 来自请求体的 version 或 If-Match（5.5.6）
OptimisticUpdates.updateById(ruleMapper, po, expectedVersion,
        InquiryErrors.RULE_VERSION_CONFLICT,      // 409；更新 0 行时读出当前版本，抛出 VersionConflictException（带 currentVersion）
        InquiryErrors.RULE_NOT_FOUND);            // 记录已不存在时抛出 NotFoundException
```

- 状态流转使用条件更新（如 `WHERE status = 'PENDING' AND version = ?`），并检查影响行数（第 6 章不变量 17）。

#### 5.7.5 ID 生成（TSID）

`TsidIdGenerator` 实现 `IdGenerator`（4.4.11、ADR-014）：

| 项 | 规定 |
|---|---|
| 结构 | ADR-032候选使用正数long的41位UTC毫秒 + 10位节点 + 12位序列，纪元2020-01-01；实现未引入TSID库，见ADR-011候选表，待正式评审 |
| 节点号 | 启动时在 `sys_lock` 中租用 `core:tsid-node:{n}`（n = 0–1023，取第一个可用），租期 5 分钟、每分钟续租；续租失败时实例 readiness 变为 DOWN 并停止生成 ID，避免与接手该节点号的实例冲突（ADR-014“多实例不冲突”） |
| 时钟回拨 | 1 秒以内等待；更大时拒绝生成并告警 |
| 启动顺序 | 迁移（步骤 4）完成后租用；`--mode=migrate` 同样租用（`onInstall` 需要 ID） |
| MyBatis-Plus | `TsidIdentifierGenerator` 实现 MyBatis-Plus 的 `IdentifierGenerator`，委托 `IdGenerator`；模块不得直接使用 TSID 库 |

#### 5.7.6 JSON 列与 TypeHandler

| TypeHandler（`support.persistence`） | Java 类型 | 用途 |
|---|---|---|
| `JsonNodeTypeHandler` | `JsonNode` | 结构由提供方定义的 JSON：快照部分、布局文档、设置值 |
| `JsonTypeHandler` | record、`List`、`Map` | 模块内部结构化的 JSON 列（字段类型由 MyBatis-Plus 传入） |

- 两者都使用 `platformJsonMapper`（5.5.4），JSON 中的 ID 同样为字符串；写入前由应用层按结构校验（系统设计 25.1）。
- 使用 `JsonTypeHandler` 的 PO 必须 `@TableName(autoResultMap = true)`，XML 中的 `resultMap` 同样声明 `typeHandler`。

#### 5.7.7 软删除

- 只用于配置类实体（系统设计 25.3）；内容使用回收站状态，不用软删除。
- `deleted_at` + `@TableLogic(value = "null", delval = "now(3)")`：查询自动追加 `deleted_at IS NULL`；删除用 `deleteById(po)`，同时填充 `updated_by`。
- 唯一约束只在未删除的行之间生效：生成列 `alive`（系统设计 25.4，DDL 约定见第 7 章）。
- 物理删除只由保留期清理任务执行。

#### 5.7.8 事务边界

| 规则 | 说明 |
|---|---|
| 位置 | `@Transactional` 只放在应用服务（3.4）；查询方法 `readOnly = true`；`facade` 的写方法以 `REQUIRED` 加入调用方事务（4.1.3） |
| 超时 | 默认 30 秒（`spring.transaction.default-timeout`） |
| 隔离级别 | MySQL 默认（REPEATABLE READ）；“认领”类操作用条件更新或 `FOR UPDATE SKIP LOCKED` |
| 远程调用 | 事务内不得调用远程端口（4.1.3、4.7）：infrastructure 的远程适配器在入口检查 `TransactionSynchronizationManager.isActualTransactionActive()`，local 与 test 下抛出 `IllegalStateException`，其他环境记 WARN 与指标 `b2bweb.core.tx.remote_call` |
| 事件 | 在改变状态的同一事务内发布（5.8） |
| 批处理 | 任务中用 `TransactionTemplate` 分批提交（如每个产品一个事务，系统设计 8.6） |
| 数据源 | 应用账号（只有 DML）与迁移账号（DDL）分离（系统设计 26.6）；HikariCP，默认每实例最多 20 个连接 |

### 5.8 事件

机制见系统设计 4.1 与 4.6 的发布 / 订阅规则（ADR-003）；本节说明 core 如何装配 Spring Modulith，以及监听器包装、重新投递与幂等助手。

#### 5.8.1 Spring Modulith 配置

| 项 | 规定 |
|---|---|
| 登记表 | Spring Modulith JDBC 事件发布登记（Event Publication Registry），表 `EVENT_PUBLICATION` 由 core 的迁移脚本创建（第 7 章）：`spring.modulith.events.jdbc.schema-initialization.enabled = false` |
| 序列化 | `platformJsonMapper`（ID 为字符串，5.5.4） |
| 完成方式 | 完成的记录保留（`spring.modulith.events.completion-mode = update`），由清理任务删除（5.8.4） |
| 启动时重新投递 | 关闭 Modulith 的自动重投（`spring.modulith.events.republish-outstanding-events-on-restart = false`），改在启动步骤 8、扩展点注册表就绪之后执行（5.2.2） |
| 执行器 | `applicationTaskExecutor`（虚拟线程 + 上下文装饰，5.4.3） |
| 外部化 | V1 不启用（系统设计 4.1） |

#### 5.8.2 发布

```java
@Transactional
public void assign(AssignInquiryCommand cmd) {
    …                                                         // 改变状态
    events.publishEvent(new InquiryAssignedEvent(eventMetaFactory.next(), …));   // 同一事务内（4.6）
}
```

- `DefaultEventMetaFactory`（4.4.14）：`eventId` 取 `IdGenerator.nextIdString()`，`occurredAt` 取注入的 `Clock`，`siteId` 取当前站点（没有时为空），`actor` 取 `CurrentUserProvider.actor().toEventActor()`。
- **事务外发布保护**：`@ApplicationModuleListener` 是事务事件监听器，事务外发布的事件会被静默丢弃。core 注册同步监听器 `DomainEventPublicationGuard`：发布 `DomainEvent` 时没有活动事务即抛出 `IllegalStateException("领域事件必须在事务内发布：{事件类型}")`，与 4.6 的 ArchUnit 规则互为补充。

#### 5.8.3 监听器包装

```java
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 50)   // @Async 切换线程之后、事务切面之前执行
final class ModuleListenerAspect {

    @Around("@annotation(org.springframework.modulith.events.ApplicationModuleListener) && args(event)")
    Object around(ProceedingJoinPoint pjp, DomainEvent event) throws Throwable {
        String moduleId = catalog.moduleOfClass(AopUtils.getTargetClass(pjp.getTarget()));  // 包名 com.b2bweb.<id>..
        Long siteId = event.siteId();
        if (siteId != null && !event.deliverWhenModuleDisabled() && !siteApi.isModuleEnabled(siteId, moduleId)) {
            metrics.skipped(event.eventType(), moduleId);
            return null;                                      // 正常返回 → Modulith 标记完成（系统设计 3.8）
        }
        SecurityContextHolder.clearContext();                 // 以 system 身份执行（4.6“操作人”）
        RequestContext ctx = RequestContexts.forEvent(siteId); // origin = EVENT；siteId 为空时没有站点上下文
        return metrics.timeListener(event.eventType(), moduleId,
                () -> ContextHolder.callWith(ctx, pjp::proceed));
    }
}
```

| 情况 | 行为 |
|---|---|
| 站点级事件，订阅方模块在该站点启用 | 建立站点上下文（`EVENT`）后执行 |
| 订阅方模块在该站点停用 | 跳过并标记完成；`deliverWhenModuleDisabled() = true` 的事件（`ContentPurgedEvent`）照常投递 |
| 系统级事件（`siteId` 为空） | 没有站点上下文；订阅方用 `SiteContextProvider.runAs` 逐站点处理（4.6） |
| 监听器抛出异常 | 监听器事务回滚，投递保持未完成；记录 ERROR（`eventId`、事件类型、监听器），由重投任务再次投递 |
| 单次执行超过 10 秒 | 记录 WARN 与指标（4.6“耗时”规则） |

#### 5.8.4 重新投递与登记表清理

| 定时任务（`SYSTEM`，5.9.2） | 周期 | 内容 |
|---|---|---|
| `core.event_republish` | 每分钟 | `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan(5 分钟)`；5 分钟远大于单次处理上限（10 秒），正在处理的投递一般不会被重复提交，即使重复也由监听器幂等保证 |
| `core.event_cleanup` | 每天 03:10 UTC | 删除 7 天前完成的登记记录（`b2bweb.core.events.completed-retention`）；删除 30 天前的已处理事件记录（5.8.5） |

- 监控：每 30 秒统计未完成投递的数量与最早时间，输出 `b2bweb.core.events.incomplete.count` 与 `b2bweb.core.events.incomplete.oldest_age`，告警阈值见系统设计 27.4（> 100 条或最早一条超过 10 分钟）。
- 长期失败的投递在后台诊断页 `GET /api/admin/v1/system/events/incomplete`（系统管理员）中列出，可人工重投或标记完成（均写审计）。

#### 5.8.5 幂等助手 ProcessedEvents

4.6 订阅规则的第 ③ 种方式（按 `eventId` 去重）由 core 提供：

```java
package com.b2bweb.core.support.event;

/** 按 eventId 去重。必须在监听器自己的事务中调用：事务回滚时登记一并回滚，下次投递可以重新处理。 */
public interface ProcessedEvents {

    /**
     * 首次处理时登记并返回 true；已处理过返回 false。没有活动事务时抛出 IllegalStateException。
     *
     * @param consumer 消费者标识 {moduleId}.{用途}（snake_case），如 "notification.review_requested"
     */
    boolean firstTime(String consumer, DomainEvent event);
}
```

```java
@ApplicationModuleListener
void onReviewRequested(ReviewRequestedEvent event) {
    if (!processedEvents.firstTime("notification.review_requested", event)) return;   // 重复投递直接返回
    …
}
```

- 实现：`INSERT IGNORE INTO sys_processed_event (consumer, event_id, site_id, processed_at)`，影响 1 行为首次。
- `sys_processed_event`（框架补充，core 的全局表，列入豁免清单）：`consumer VARCHAR(96) bin`、`event_id VARCHAR(20) bin`、`site_id BIGINT NULL`、`processed_at DATETIME(3)`；主键 `(consumer, event_id)`，索引 `idx_pev_processed (processed_at)`；保留 30 天。DDL见6.8，core V002追加。
- 选择顺序仍按 4.6：优先“按当前状态重算”，其次业务唯一键，最后才用本助手。

#### 5.8.6 模块如何使用

| 要做的事 | 做法 |
|---|---|
| 发布 | 应用服务的事务内 `publishEvent(new XxxEvent(eventMetaFactory.next(), …))`；登记到 `module.json` 的 `events.publishes` |
| 订阅 | `listener` 包中的 `@ApplicationModuleListener` 方法，命名 `on<事件名去掉 Event>`（3.7）；登记到 `events.subscribes` |
| 幂等与顺序 | 4.6 的订阅规则；需要时用 `ProcessedEvents` |
| 长时间处理 | 提交 `JobApi` 任务后返回（4.6“耗时”） |
| 测试 | Spring Modulith 的 Scenario API；同一事件投递两次验证幂等（2.4） |

### 5.9 异步任务与调度

#### 5.9.1 通用异步任务（sys_job）

core 的实现：`JobApiImpl`（4.4.12）、`JobExecutor`、`JobExecutionContextImpl`；表结构见第 6 章 `sys_job`。

**提交**（`JobApi.submit`，加入调用方事务）：

| 步骤 | 说明 |
|---|---|
| 1 | 校验 `jobType` 以 `moduleId + "."` 开头，且有对应的 `JobHandler`（按 key 查找；没有时 `CORE_JOB_TYPE_UNKNOWN`） |
| 2 | 站点级任务：所属模块在当前站点停用时抛出 `ModuleDisabledException` |
| 3 | 插入 `PENDING` 行：`site_id` 为当前站点（没有站点上下文时为 0），`created_by` 为当前操作人。有幂等键时用排他 `INSERT ... ON DUPLICATE KEY UPDATE id=id` 后按 `(site_id, job_type, idempotency_key)` 做 `SELECT ... FOR UPDATE` 当前读；规范化参数/父任务不同拒绝CORE_IDEMPOTENCY_KEY_REUSED，相同返回已有任务。不能用RR普通快照读；1213/1205回滚整个调用方事务，由外层限次重试 |
| 4 | 事务提交后经 Redis Pub/Sub `sys:core:job-wakeup` 唤醒各实例的执行器（只用于加速，轮询兜底） |

**执行**：

```text
每个实例有 b2bweb.core.jobs.concurrency 个工作槽（默认 4，虚拟线程）。有空槽时：
 1. 短事务认领（站点过滤豁免的专用查询，第 6 章 idx_job_poll）：
      SELECT id FROM sys_job WHERE status = 'PENDING' AND scheduled_at <= now(3)
       ORDER BY scheduled_at LIMIT {空槽数} FOR UPDATE SKIP LOCKED
      UPDATE sys_job SET status = 'RUNNING', worker_id = ?, heartbeat_at = now(3), started_at = now(3),
                         attempts = attempts + 1, version = version + 1
       WHERE id = ? AND status = 'PENDING'
 2. 所属模块在该站点已停用 → CANCELED（error_code = CORE_MODULE_DISABLED）
 3. 建立上下文：站点 = site_id（0 表示没有站点）；actor = created_by；origin = JOB
 4. 不在事务中调用 JobHandler.execute(ctx)（实现方按需开启短事务，4.4.12）
 5. 结果（条件更新 WHERE id = ? AND status = 'RUNNING' AND version = ?）：
      正常返回               → SUCCEEDED，result = JobOutcome.result()
      JobCanceledException  → CANCELED
      PlatformException     → FAILED，error_code / error_message 取自异常
      其他异常               → FAILED，CORE_JOB_FAILED，error_message 为通用中文说明（堆栈只进日志）
      FAILED 且 attempts < max_attempts → 回到 PENDING，scheduled_at = now + 2^attempts 分钟
轮询间隔 2 秒（收到唤醒消息时立即），空闲时逐步退避到 10 秒。
```

`JobExecutionContextImpl`：

| 方法 | 实现 |
|---|---|
| `progress` | 节流写库（每秒最多一次），同时刷新 `heartbeat_at` |
| `checkpoint` | 读取 `cancel_requested`（节流 1 秒）：已请求取消时抛出 `JobCanceledException`，否则刷新心跳 |
| `openInputFile` | `ObjectStorage.get(key, PRIVATE)`；只允许读取本站点的键（`StorageKey.belongsToSite`，4.7.1） |
| `writeResultFile` | 写入私有桶的 `StorageKey.jobFile(…)`，`result_expires_at` 为 7 天后；同一任务只保留最后一个结果文件 |
| `params(Class)` | 用 `platformJsonMapper` 转换 |

- `storeInputFile` 写入私有桶的 `StorageKey.jobFile(…)`（4.7.1）；`resultDownloadUrl` 返回 `ObjectStorage.presignedGet` 的链接（带下载文件名）。私有桶为前缀 `jobs/` 配置 7 天过期的生命周期规则（部署要求，第 11 章与运维手册），输入与结果文件由存储自动删除。
- 任务完成后 core 不发送通知（L0 不调用 notification，4.1.3）：Admin 的任务中心与顶栏任务提醒轮询 `/api/admin/v1/system/jobs`。发起人可以查看、取消、重试自己的任务；拥有 `system:job:view` 的用户可以查看本站点全部任务；取消与重试他人的任务只限系统管理员。

| core 的任务维护（定时任务，`SYSTEM`） | 周期 | 内容 |
|---|---|---|
| `core.job_reaper` | 每分钟 | `RUNNING` 且心跳早于“当前时间 − 处理器的 `heartbeatTimeout()`”：`attempts < max_attempts` 时回到 `PENDING`，否则 `FAILED`（`CORE_JOB_FAILED`，“执行实例失联”） |
| `core.job_cleanup` | 每天 | 置空已过期结果文件的字段；删除超过保留期的任务记录（`core.retention` 设置，默认 180 天） |

模块的写法（以产品导入为例）：

```java
@Extension(module = "product")
public class ProductImportJobHandler implements JobHandler {

    @Override public String jobType() { return "product.import"; }

    @Override
    public JobOutcome execute(JobExecutionContext ctx) throws Exception {
        ImportParams params = ctx.params(ImportParams.class);
        try (InputStream in = ctx.openInputFile(params.fileKey())) {
            List<ImportRow> rows = parser.parse(in);
            int done = 0, failed = 0;
            for (ImportRow row : rows) {
                ctx.checkpoint();                                    // 协作取消 + 心跳
                if (importer.importOne(row)) done++; else failed++;  // 每行一个短事务（TransactionTemplate）
                ctx.progress(rows.size(), done, failed, null);
            }
            ctx.writeResultFile("导入报告.xlsx", XLSX, out -> report.write(rows, out));
            return new JobOutcome(json.valueToTree(Map.of("created", done, "errors", failed)));
        }
    }
}

// 提交（应用服务，事务内；第二阶段的 parentJobId 指向校验预览任务）
JobInfo job = jobs.submit(new JobRequest("product", "product.import", "产品导入", params,
        idempotencyKey, previewJobId, null, 1));
```

#### 5.9.2 定时任务

`ScheduledTaskRunner` 调度全部 `ScheduledTask`（4.4.12）：

| 项 | 规定 |
|---|---|
| 注册 | 启动步骤 8 读取全部实现（`allRegardlessOfSite`），以 `CronTrigger(task.cron(), UTC)` 注册到 `taskScheduler`；`--mode=migrate` 或 `b2bweb.core.scheduling.enabled = false`（集成测试）时不注册 |
| 单实例 | 每次触发经 ShedLock `LockingTaskExecutor` 执行，锁名为 `task.name()`，`lockAtMostFor` 取自任务，`lockAtLeastFor` 5 秒；拿不到锁即跳过（其他实例正在执行） |
| ShedLock | `JdbcTemplateLockProvider`：表 `sys_lock`，列名 `lock_name` / `lock_until` / `locked_at` / `locked_by`，使用数据库时间（第 6 章） |
| `SYSTEM` | 没有站点上下文，执行一次 |
| `PER_SITE` | 在同一把锁内遍历 `SiteApi.listOperationalSites()`；跳过停用了所属模块（`ExtensionRegistry.moduleOf(task)`）的站点（系统设计 3.8）；逐站点建立上下文（`SCHEDULED`）后调用 `run`；某个站点失败时记录错误并继续下一个 |
| 事务 | 不在事务中调用，任务按需开启短事务；工作量大时提交 `sys_job` 任务后返回 |
| 指标 | `b2bweb.core.scheduled.duration`（标签 task、outcome）、`b2bweb.core.scheduled.last_success`（每个任务） |

core 自己的定时任务：

| 任务名 | cron（UTC） | 范围 | 内容 |
|---|---|---|---|
| `core.event_republish` | `0 * * * * *` | SYSTEM | 5.8.4 |
| `core.event_cleanup` | `0 10 3 * * *` | SYSTEM | 5.8.4 |
| `core.job_reaper` | `30 * * * * *` | SYSTEM | 5.9.1 |
| `core.job_cleanup` | `0 20 3 * * *` | SYSTEM | 5.9.1 |
| `core.audit_retention` | `0 30 3 * * *` | SYSTEM | 删除超过保留期的审计日志（5.11.1） |

#### 5.9.3 锁的选择

| 需要 | 使用 |
|---|---|
| 定时任务单实例执行 | ShedLock（`sys_lock`），由 `ScheduledTaskRunner` 自动处理 |
| 合并重复的短期工作 | `DistributedLock` 端口（Redis，4.7.16） |
| core 的跨实例管理操作（模块启停、TSID 节点号租约） | `sys_lock` 业务锁（core 内部的 `DbLockService`，锁名 `{moduleId}:{用途}[:s{siteId}]`，第 6 章） |
| 模块迁移 | MySQL 命名锁（5.2.2） |
| 数据正确性 | 数据库唯一约束与乐观锁；不依赖任何锁（4.7.16） |

### 5.10 缓存

系统设计 23.1 的缓存层在本框架中的实现：

| 层 | 内容 | 实现 | 失效方式 |
|---|---|---|---|
| CDN | HTML、图片、sitemap | `CdnPurger` 端口（delivery、seo 调用，4.7.7） | 按缓存标签清除 |
| Redis（带标签） | Delivery 结果、站点引导数据、路由解析、sitemap 分片 | `TaggedCache` 端口 → `RedisTaggedCache`（4.7.14） | `invalidate(tags)` |
| Redis（键值） | 会话、权限快照、`@Idempotent` 记录、限流令牌桶 | core 内部（Spring Data Redis）与 `RateLimiter` 端口 | 删除键或 TTL |
| 应用内存 | 域名映射、站点与语言、模块启用状态、设置、字典、界面文案、描述符与 Schema、扩展点注册表 | `LocalCaches`（Caffeine，`support.cache`） | Redis Pub/Sub 广播 + TTL 兜底 |

#### 5.10.1 缓存标签与 TaggedCache

- 标签只由 `CacheTag` 的工厂方法生成（4.3.7），CDN 的 `Cache-Tag` 响应头与 Redis 的标签索引使用同一组值。
- `RedisTaggedCache`（`com.b2bweb.core.cache`（ADR-031候选））按4.7.14/4.10.3实现：值键 `s{siteId}:{namespace}:{key}`；标签索引 `s{siteId}:tag:{标签值}`（Set）。回源前capture；Lua在校验恢复门闩与站点fence的同一原子操作中写值及标签，按freshUntil绝对过期，标签TTL不短于最长条目；禁止无fence的MULTI回填。失效先推进代次再清理，命中也核对代次，丢失标签不能让旧值继续命中。物理清理须有界分批，不能照搬原型的小集合SMEMBERS/UNLINK或完整命名空间SCAN为生产长脚本；值用 `platformJsonMapper` 序列化。
- V1 使用非集群 Redis；以后改用集群时，键中的站点段改为 hash tag（`{s1}`），使同一站点的键与标签落在同一槽位。
- 使用方：delivery用 `getOrLoadWithFreshness` 合并并发回源，结果为 `CacheEntry(value, tags, freshUntil)`；依赖命中也传递原期限，父结果取最早期限。监听器直接 `invalidate` 后调用 `CdnPurger`，二者确认成功才完成持久投递；HTTP 2xx但业务失败/响应损坏或确认丢失均须保留重试义务（系统设计23.3，delivery设计文档）。

#### 5.10.2 应用内存缓存与广播失效

```java
package com.b2bweb.core.support.cache;

/** 带广播失效的本地缓存（Caffeine）。名称全局唯一，格式 {moduleId}.{名称}。 */
public interface BroadcastCache<K, V> {
    Optional<V> getIfPresent(K key);
    V get(K key, Function<? super K, ? extends V> loader);
    /** 本实例立即失效；在当前事务提交后（没有事务时立即）再次失效并广播给其他实例。 */
    void invalidate(K key);
    void invalidateAll();
}

public interface LocalCaches {
    /** spec：最大条目数、TTL（默认 10 分钟，作为丢失广播时的兜底）。 */
    <K, V> BroadcastCache<K, V> create(String name, LocalCacheSpec spec);
}
```

- `CacheInvalidationBus` 在 Redis 频道 `sys:core:cache-evict` 上发布 `{ cache, key | "*", origin }`，各实例订阅后失效本地条目（忽略自己发出的消息）。键必须是字符串、数字或由它们组成的 record（广播时序列化为字符串）。
- 提交后再失效一次，避免事务内读到旧数据并重新放入缓存。Pub/Sub 不保证送达：TTL 兜底；与 Redis 断线重连后，实例清空全部本地缓存。

core 的本地缓存：

| 名称 | 键 → 值 | 失效时机 |
|---|---|---|
| `core.host` | 域名 → `HostResolution` | 域名、站点状态变化（TTL 5 分钟） |
| `core.site` | 站点 ID → `SiteInfo` 与语言列表 | 站点、语言变化 |
| `core.site_modules` | 站点 ID → 启用的模块 ID 集合 | 模块启停、平台级状态变化 |
| `core.setting` | （站点 ID，设置键）→ 值 | 设置写入、恢复默认 |
| `core.dict` | 字典类型 → 字典项 | 字典修改、`ensureBuiltin` |
| `core.ui_text` | （站点 ID，语言）→ 文案表 | 文案修改、译文回写 |

规则：

- 不使用 Spring 的 `@Cacheable`：没有站点前缀、没有标签、不能跨实例失效。模块需要缓存时使用 `TaggedCache`（Redis）或 `LocalCaches`（内存）。
- Redis 键一律带站点前缀 `s{siteId}:` 或系统前缀 `sys:`（系统设计 5.3），用 `CacheKeys` 生成。
- 缓存中不得保存明文密钥；个人数据只允许出现在会话与权限快照（用户 ID）中。
- Caffeine 的统计接入 Micrometer（`cache.gets`、`cache.evictions`，标签为缓存名），用于命中率监控（系统设计 27.4）。

### 5.11 审计、设置、字典、界面文案服务

#### 5.11.1 审计（AuditApi）

`AuditApiImpl`（4.4.9）：

| 项 | 实现 |
|---|---|
| 自动填充 | 操作人 ID 与姓名快照（`CurrentUserProvider.actor()`）、IP 与 UA（`RequestContext`）、`trace_id`、站点（`systemLevel = true` 时为空，否则当前站点）、时间 |
| 事务 | `record` 加入调用方事务（业务回滚则审计一并回滚；审计写入失败使业务失败，不静默丢弃）；`recordIndependently` 为 `REQUIRES_NEW` |
| 差异 | `diff` 序列化为 JSON，超过64KiB时按完整字段删除并以`__truncated: {from:null,to:true}`标记，不截断UTF-8/JSON；`AuditChange.masked()` 原样记录为掩码；值为 `Secret` 时自动掩码 |
| 只追加 | Mapper 只有 insert 与 select（ArchUnit，第 6 章不变量 16）；唯一的删除路径是保留期清理 `core.audit_retention`（默认 365 天，`core.retention` 设置，系统设计 26.5）；候选实现每批1000、每次最多10批，保留边界等于cutoff的记录 |
| IP | 审计记录保存完整 IP（安全用途，保留期内可查）；应用日志中的 IP 按 5.13 截断 |
| 查询 | `/api/admin/v1/system/audit-logs`（`system:audit:view`）：表已豁免站点过滤，站点内查询显式带 `site_id = 当前站点`；系统级记录与全部站点只对系统管理员开放 |

模块的写法（每个后台写操作都有审计，系统设计 29.6）：

```java
audit.record(new AuditEntry("inquiry", "inquiry.export", "inquiry", null,
        "导出询盘 " + count + " 条", Map.of(), AuditResult.SUCCESS, true, false));   // sensitive = true
```

#### 5.11.2 设置（SettingsApi）

`SettingsApiImpl`（4.4.3）与 `SettingsSchemaRegistry`：

| 项 | 实现 |
|---|---|
| Schema | 启动时加载各模块的 `settings.schema.json`（5.2.4）；顶层属性即设置键 `{moduleId}.{属性名}`，其子 Schema 用于校验与默认值 |
| 读取 | 本地缓存 `core.setting` → `sys_setting` 行 → 没有行时按子 Schema 的 `default` 组装；`get(key, type)` 用 `platformJsonMapper` 转换为声明模块的 record；敏感字段保持信封（4.4.3）；未声明的键抛出 `CORE_SETTING_NOT_DECLARED` |
| 写入 | 子 Schema 校验（失败为 `CORE_SETTING_INVALID` + `errors[]`）→ `x-secret` 字段的明文经 `SecretCipher.encrypt` 存为信封并置 `encrypted = 1`，提交值等于掩码（如 `****abcd`）时保留原信封 → 乐观锁（`CORE_SETTING_VERSION_CONFLICT`）→ 审计（敏感字段掩码）→ 站点级设置在同一事务中发布 `SiteSettingsChangedEvent(SETTINGS, 键)`（4.6.7）→ 提交后广播缓存失效 |
| 系统级 | `site_id = 0`，经豁免查询读写（5.7.2）；`putSystem` 不发布站点事件，只广播缓存失效 |
| 后台 | `/api/admin/v1/system/settings`：按模块列出 Schema 与当前值（敏感字段用 `SecretCipher.mask` 掩码）；`PUT /settings/{key}`（带 `version`）；Admin 用 Schema 驱动的表单渲染（系统设计 22.4）；权限 `system:setting:edit` |
| 结构升级 | 值的顶层带 `schemaVersion`；Schema 不兼容变化时，声明模块在 `onUpgrade` 中转换已保存的值 |

```java
// inquiry：与 settings.schema.json 中 retention 属性对应的 record
public record InquiryRetention(int schemaVersion, int closedYears, int spamDays) {}

InquiryRetention retention = settings.get("inquiry.retention", InquiryRetention.class);
```

M0 RC设置文件（ADR-027，待评审）：`META-INF/b2bweb/settings.schema.json`承载以下默认对象，均带schemaVersion=1。SMTP host为空表示由notification选择部署级默认服务器，密码为x-secret；显式服务器的NONE只允许local/test。theme的空值表示使用主题默认Token/变体，不能据此把任意CSS写入页面；具体Token目录由前端校验。features.flags初始为空。DSR参数仅落实6.4.22既有规则，不表示法务审批完成。

| 键 | 字段 / 默认值 |
|---|---|
| core.smtp | host/username/password=null，port=587，security=STARTTLS |
| core.retention | auditDays=365，jobDays=180，jobFileDays=7；均为正整数天 |
| core.theme | brandColor/fontFamily/radius/buttonVariant/headerVariant/footerVariant=null（选用主题默认值） |
| core.features | flags={}；值为布尔值，键为camelCase或点分名称 |
| core.dsr | gdprMonths=1，gdprExtensionMonths=2，ccpaAckBusinessDays=10，ccpaDays=45，ccpaExtensionDays=45，ccpaOptOutBusinessDays=15，holidays=[] |

#### 5.11.3 字典（DictApi）

| 项 | 实现（4.4.13） |
|---|---|
| 存储 | `sys_dict_type`、`sys_dict_item`（全局表，第 6 章） |
| 读取 | 本地缓存 `core.dict` |
| 内置字典 | 模块在 `onInstall` / `onUpgrade` 中调用 `ensureBuiltin`：新增缺失的类型与项，更新内置项的标签与排序，不删除管理员添加的项 |
| 后台 | `/api/admin/v1/system/dicts`（`system:dict:manage`）：只能编辑 `itemsEditable = true` 的类型；内置项不能删除，只能停用 |
| 使用 | 业务表只存字典值；保存前 `isValid`，展示时 `label`；字典只用于后台（中文），前台可见的选项走文案键或站点资源 |

#### 5.11.4 界面文案（UiTextApi）与站点资料（SiteProfileApi）

| 项 | 实现 |
|---|---|
| 主题默认文案 | `application` 构建时把主题包的默认中文文案（键为文案键）复制到 classpath `theme-texts/{themeKey}/zh-CN.json`，做法同组件 Schema（3.5）；主题包中的源文件位置由第 8 章确定。`UiTextApiImpl` 启动时加载 |
| 查找 | `zh-CN`：站点覆盖（`sys_ui_text` 的 `zh-CN` 行）→ 主题默认；其他语言：该语言的译文行，没有时为空，不回退到中文（4.4.5） |
| 缓存 | 本地缓存 `core.ui_text`（站点 + 语言 → 全部文案）；`allTexts` 直接返回缓存（Delivery 热路径） |
| `format` | 替换 `{name}` 占位符；参数按纯文本插入 |
| 编辑 | `/api/admin/v1/system/ui-texts`（`system:ui-text:edit`）只编辑 `zh-CN` 覆盖；保存时在同一事务发布 `SiteSettingsChangedEvent(UI_TEXT, 命名空间)` 与 `TranslatableResourceChangedEvent(core.ui_text, 命名空间)`（4.6.7） |
| 翻译回写 | core 实现 `TranslatableResourceProvider`（`@Extension(module = "core")`，资源类型 `core.ui_text`、`core.site_profile`，4.5.21）：`extract` 读取源语言文本，`apply` 写入目标语言行（`translation_state`、`source_fingerprint`，第 6 章）并广播缓存失效 |
| 站点资料 | `SiteProfileApiImpl`（4.4.4）读取 `sys_site_profile` 与 `sys_site_profile_localization`；编辑后发布 `SiteSettingsChangedEvent(PROFILE)` 与 `TranslatableResourceChangedEvent(core.site_profile)`。core 的其他扩展点实现（4.2.3）：`SiteBootstrapContributor`（站点资料、语言列表、界面文案）、`SchemaOrgProvider`（`Organization`）、`MediaReferenceCollector`（Logo 等媒体引用） |

### 5.12 基础设施适配器与配置项

`backend/infrastructure`（包 `com.b2bweb.infrastructure.<技术>`）实现 4.7 的全部端口。端口约定（装配、使用范围、站点隔离、凭据、异常、重试、超时、日志与指标）见 4.7 开头的约定表，本节只补充实现与配置。

#### 5.12.1 适配器清单与配置

每个端口按 `b2bweb.infra.<端口>.type` 选择适配器（4.7）；装配类位于 `application` 的 `com.b2bweb.app.infra`，以 `@ConditionalOnProperty` 选择实现。

| 端口 | 适配器 | `type` | 主要配置（前缀 `b2bweb.infra.`） | 凭据 |
|---|---|---|---|---|
| `ObjectStorage` | `S3CompatibleObjectStorage`（AWS SDK v2 的 S3 客户端与预签名器）；`LocalFileObjectStorage`（仅 local） | `s3` / `local` | `object-storage.endpoint`、`region`、`path-style`（MinIO 为 true）、`public-bucket`、`private-bucket`、`public-base-url`（公开桶前的 CDN 域名）、`local.root` | 环境变量 `B2BWEB_S3_ACCESS_KEY`、`B2BWEB_S3_SECRET_KEY` |
| `ImageUrlSigner` | `ImgproxyImageUrlSigner` | `imgproxy` | `image-url-signer.base-url` | `B2BWEB_IMGPROXY_KEY`、`B2BWEB_IMGPROXY_SALT`（十六进制） |
| `VirusScanner` | `ClamAvVirusScanner`（clamd `INSTREAM`）；testkit 的 `FakeVirusScanner`（仅 local，含 EICAR 字符串的文件判为感染，11.3.3） | `clamav` / `fake` | `virus-scanner.host`、`port`、`timeout`、`max-stream-size` | — |
| `MailTransport` | `SmtpMailTransport`（Spring `JavaMailSender`） | `smtp` | `mail.host`、`port`、`security`、`username`（部署级默认服务器；local 为 Mailpit） | `B2BWEB_SMTP_PASSWORD`；站点级服务器由 notification 以 `MailServerConfig` 传入 |
| `WebhookTransport` | `HttpWebhookTransport` | `http` | `webhook.default-timeout` | 签名密钥由 notification 传入 |
| `SearchEngineClient` | `MeilisearchSearchEngineClient`（`RestClient` 调用 Meilisearch HTTP API） | `meilisearch` | `search-engine.url`、`index-prefix`（默认空；多人共用开发实例时区分） | `B2BWEB_MEILI_MASTER_KEY` |
| `CdnPurger` | `CloudflareCdnPurger`；`NoopCdnPurger` | `cloudflare` / `none` | `cdn.zone-id`、`cdn.max-tags-per-request`（默认 30） | `B2BWEB_CDN_API_TOKEN` |
| `IndexNowClient` | `HttpIndexNowClient`；`NoopIndexNowClient` | `http` / `none` | `index-now.endpoint`（默认 `https://api.indexnow.org/indexnow`） | 密钥是公开值，由 seo 传入 |
| `TranslationEngine` | `OpenAiCompatibleTranslationEngine`（5.12.2）；本地假引擎（第 11 章） | `openai-compatible` / `fake` | `translation-engine.connect-timeout`、`max-connections`、`proxy`（可选的出站代理） | 只来自 `trn_engine`，由 translation 以 `EngineConfig` 传入；配置文件中不得出现 API Key（`CLAUDE.md` 禁止 12） |
| `CaptchaVerifier` | `TurnstileCaptchaVerifier` | `turnstile` | `captcha.verify-url`（默认 Turnstile 官方 siteverify 地址） | 由 form 传入（4.7.10） |
| `GeoIpResolver` | `MaxMindGeoIpResolver`（本地 mmdb） | `maxmind` | `geoip.database-path`（文件由运维定期更新，热加载） | — |
| `MxRecordChecker` | `DnsMxRecordChecker`（JDK DNS） | `dns` | `mx.timeout`、`mx.cache-ttl` | — |
| `RemoteFileFetcher` | `SafeHttpRemoteFileFetcher` | `http` | `remote-fetch.temp-dir`、`remote-fetch.user-agent` | — |
| `TaggedCache`、`RateLimiter`、`DistributedLock`、`BufferStore` | `RedisTaggedCache`、`Bucket4jRedisRateLimiter`、`RedisDistributedLock`、`RedisBufferStore` | `redis` | 共用 `spring.data.redis.*` | `B2BWEB_REDIS_PASSWORD` |

实现要点：

| 适配器 | 要点 |
|---|---|
| `S3CompatibleObjectStorage` | 公开 URL = `public-base-url` + 对象键（不访问网络）；`presignedGet` 用预签名器并设置 `response-content-disposition`；为兼容 R2、OSS、MinIO，校验和计算设为“仅在必需时”；单次上传超过 16 MB 时分片 |
| `ImgproxyImageUrlSigner` | HMAC-SHA256（密钥、盐）签名 4.7.2 的路径；纯计算，可在 Delivery 热路径调用 |
| `ClamAvVirusScanner` | 按块发送流，超过 `max-stream-size` 返回 `UNSCANNABLE`；`available()` 发送 `PING` |
| `SmtpMailTransport` | 部署级默认服务器的发送器在启动时创建；站点级 `MailServerConfig` 每次发送时建立会话，不缓存含明文密码的对象（4.7）；SMTP 4xx 与连接错误为 `retryable = true`，5xx 与认证失败为 `false`（4.7.4） |
| `HttpWebhookTransport`、`SafeHttpRemoteFileFetcher` | 共用 `SsrfGuard`：DNS 解析后拒绝回环、私有、链路本地、组播、保留与云元数据地址，并直接连接已校验的 IP（4.7.5、4.7.13） |
| `CloudflareCdnPurger` | 按服务商上限拆分批次；不支持标签清除时转为整站清除（4.7.7） |
| Redis 类适配器 | 键前缀与故障策略见 4.7.14–4.7.17；Lua 脚本在启动时加载并按 SHA 调用 |

HTTP 客户端统一使用 Spring `RestClient` + JDK `HttpClient`（`JdkClientHttpRequestFactory`），每个适配器单独配置超时；不使用任何 AI 厂商 SDK（父 POM 的 Enforcer 规则，3.2）。

#### 5.12.2 OpenAI 兼容翻译引擎

`OpenAiCompatibleTranslationEngine` 实现 4.7.9 中“适配器”一行的职责：

```text
translate(request)
 1. 入口检查：持有数据库事务时拒绝（5.7.8）
 2. 组装请求：POST {engine.baseUrl}/chat/completions
      Authorization: Bearer {engine.apiKey}（apiKey 为空串时不发送）；engine.extraHeaders
      { model, temperature, max_tokens?, response_format（按 StructuredOutputMode）,
        messages: [ { role: system, content: request.systemPrompt },
                    { role: user,   content: Jackson 序列化的 {"source","target","items":[{"id","text","maxLength","context"}]} } ] }
      待翻译文本只出现在 user 消息的 JSON 数据中，不做字符串拼接（系统设计 26.3）
 3. 发送：超时 = engine.timeout；429 与 5xx、超时按指数退避重试（1 s、2 s、4 s 加随机抖动；有 Retry-After 时遵守，单次等待不超过 30 s），最多 3 次重试
 4. 解析：choices[0].message.content → JSON（NONE 模式先去掉 ```json 代码块围栏）；
      finish_reason = length、不是合法 JSON、items 的 ID 与请求不一一对应（缺失、多余、重复）→ INFRA_ENGINE_OUTPUT_INVALID
 5. 用量：usage.prompt_tokens / completion_tokens；命中前缀缓存的 token 取服务商返回的对应字段（如 prompt_tokens_details.cached_tokens），没有时为空
 6. 结果：items 按请求顺序排列；返回模型名、耗时与 HTTP 请求次数
```

| HTTP 结果 | 异常（4.7.9） |
|---|---|
| 网络错误、超时、重试后仍为 429 / 5xx | `INFRA_ENGINE_UNAVAILABLE`（`retryable = true`） |
| 401 / 403 | `INFRA_ENGINE_AUTH_FAILED`（`retryable = false`） |
| 其他 4xx（如上下文超长） | `INFRA_ENGINE_BAD_REQUEST`（`retryable = false`） |

- `probe(engine)` 翻译一个固定短单元，捕获全部异常转为 `EngineProbeResult`。
- 日志只记录引擎 ID、模型、单元数、用量、耗时与状态码；不记录提示词、原文、译文、响应正文与 API Key（4.7.9）。指标 `b2bweb.infra.translation_engine.calls / errors / latency`，另输出 `b2bweb.infra.translation_engine.tokens`（标签 direction = input / output）。
- 翻译记忆、术语、分批、预算、备用引擎切换与译文校验都属于 translation（4.7.9 职责表），适配器不做。

### 5.13 可观测性

实现系统设计 27.4 中后端部分。

| 方面 | 实现 |
|---|---|
| 日志 | Spring Boot 结构化日志，JSON 输出到标准输出，由 Loki 采集（local 为可读文本）；公共字段 `traceId`、`spanId`（Micrometer Tracing 写入 MDC）、`siteId`、`userId`（上下文绑定时写入，5.4.1）、`module`（由日志器名 `com.b2bweb.<id>..` 推断）；事件处理与任务中另加 `eventId` / `jobId` |
| 脱敏 | IP 截断：IPv4 末段置零，IPv6 保留前 48 位（`IpAnonymizer`）；不记录邮箱、电话、姓名、留言、密钥、待翻译文本与译文（4.7）；需要关联时用 `LogSafe.hash(…)` 输出哈希前缀；编码器对匹配邮箱与常见密钥格式的字符串做兜底掩码 |
| 指标 | Micrometer + Prometheus（`/actuator/prometheus`）；公共标签 `application`、`instance`、`env`；命名 `b2bweb.{模块 ID}.{点分小写}`（3.7）；`site` 标签只用于站点数有限的业务指标，不得用用户 ID、路径、内容 ID 等高基数值作标签 |
| 链路追踪 | Micrometer Tracing + OpenTelemetry 桥，OTLP 导出；接受 Nuxt 传来的 W3C `traceparent`（系统设计 27.4）；采样率 prod 10%，其他环境 100%（`management.tracing.sampling.probability`） |
| 错误上报 | Sentry Java SDK（欧盟数据区域；`send-default-pii = false`；`beforeSend` 去掉请求体、Cookie 与请求头中的令牌）；只上报 5xx 与未处理异常 |
| 管理端口 | `management.server.port = 8081`，只在内网开放（系统设计 26.3）；暴露 `health`、`info`、`prometheus`，非生产环境另暴露 `modulith` |

**健康检查**：

| 端点 | 包含 | 说明 |
|---|---|---|
| `/actuator/health/liveness` | `livenessState` | 进程存活 |
| `/actuator/health/readiness` | `readinessState`（启动步骤 8 完成）、`db`、`redis`、`tsidLease`（5.7.5） | 不包含 Meilisearch：其故障只算降级（系统设计 27.4） |
| `/actuator/health` | 以上 + `meilisearch`、`objectStorage`、`clamav` | 只在管理端口；后台“系统状态”页经 core 的系统管理接口读取 |

**core 输出的指标**（告警规则在 `deploy/` 中维护，阈值见系统设计 27.4）：

| 指标 | 类型（标签） | 用途 |
|---|---|---|
| `http.server.requests` | Spring 自带（uri 模板、status） | 请求量、错误率、延迟 |
| `b2bweb.core.events.incomplete.count`、`…oldest_age` | gauge | 未完成事件告警 |
| `b2bweb.core.events.listener` | timer（event、module、outcome） | 监听器耗时与失败 |
| `b2bweb.core.events.skipped` | counter（event、module） | 因模块停用跳过的投递 |
| `b2bweb.core.jobs.queue` | gauge（status） | `PENDING` / `RUNNING` 数量 |
| `b2bweb.core.jobs.duration` | timer（type、outcome） | 任务耗时与失败 |
| `b2bweb.core.scheduled.duration`、`…last_success` | timer / gauge（task） | 定时任务 |
| `b2bweb.core.auth.login` | counter（outcome） | 登录失败异常告警（ADR-010） |
| `b2bweb.core.ratelimit.rejected` | counter（rule） | 限流拒绝 |
| `b2bweb.core.tx.remote_call` | counter（port） | 事务内的远程调用（应为 0） |
| `cache.gets`、`cache.evictions` | Caffeine 统计（cache） | 本地缓存命中率 |
| `b2bweb.infra.<端口>.calls / errors / latency` | 适配器（operation、outcome） | 外部依赖（4.7） |

### 5.14 应用配置与 Profile

#### 5.14.1 启动类与运行模式

```java
package com.b2bweb;

@SpringBootApplication
public class B2bWebApplication {

    public static void main(String[] args) {
        PlatformMode mode = PlatformMode.fromArgs(args);          // serve（默认）/ migrate / purge
        new SpringApplicationBuilder(B2bWebApplication.class)
                .web(mode == PlatformMode.SERVE ? WebApplicationType.SERVLET : WebApplicationType.NONE)
                .properties("b2bweb.core.mode=" + mode.key())
                .run(args);
    }
}
```

| 模式 | 用途 | 行为 |
|---|---|---|
| `serve` | 正常运行 | 启动流程全部步骤（5.2.2）；接收流量 |
| `migrate` | 生产发布第 1 步（系统设计 27.3） | 用迁移账号执行步骤 1–6 后退出；不启动 Web、任务执行器与定时任务 |
| `purge` | 彻底删除模块数据（5.2.3） | 检查前置条件、执行 `onPurge` 后退出 |

**优雅停机**：`server.shutdown = graceful`，`spring.lifecycle.timeout-per-shutdown-phase = 30s`。停机时先把 readiness 置为 DOWN，执行器停止认领新任务；执行中的任务在超时前没有结束则保持 `RUNNING`，由其他实例的 `core.job_reaper` 回收；停止发号，保留剩余TSID租期作为交接隔离窗口（ADR-032候选）；不在关闭时立即复用节点。

#### 5.14.2 配置的分类

| 类别 | 位置 | 修改方式 | 示例 |
|---|---|---|---|
| 部署期配置 | `application` 的 `application.yml` 与 `application-{profile}.yml`；模块配置属性类中的默认值 | 改配置并重启 | 地址、端口、桶名、连接池、执行并发、超时 |
| 部署期密钥 | 只来自环境变量或密钥管理服务（系统设计 26.6） | 运维注入 | 数据库密码、`B2BWEB_SECRET_KEYS`、内部令牌 |
| 站点级运行期设置 | `sys_setting`（`SettingsApi`，5.11.2） | 后台修改，即时生效 | 保留期、SMTP（站点级）、Turnstile 密钥、AI 引擎（translation 自己的 `trn_engine`） |

配置键的命名空间：

| 前缀 | 归属 | 示例 |
|---|---|---|
| `b2bweb.core.*` | platform-core | `b2bweb.core.modules.migrate-on-startup`、`b2bweb.core.jobs.concurrency`、`b2bweb.core.internal.tokens`、`b2bweb.core.security.reauth-window`、`b2bweb.core.web.problem-type-base`、`b2bweb.core.events.completed-retention` |
| `b2bweb.infra.*` | infrastructure（5.12.1） | `b2bweb.infra.object-storage.type` |
| `b2bweb.<id>.*` | 业务模块（3.4、3.7） | `b2bweb.inquiry.export-max-rows` |
| `spring.*`、`server.*`、`management.*` | 框架 | 数据源、Redis、Session、Modulith、actuator |

模块的配置属性类（模块不提供 `application.yml` 片段，避免多个 jar 中的同名资源互相遮蔽，3.4）：

```java
@ConfigurationProperties(prefix = "b2bweb.inquiry")
@Validated
public record InquiryProperties(
        @DefaultValue("PT15M") Duration slaCheckInterval,
        @DefaultValue("5000") @Max(50000) int exportMaxRows) {}
```

站点管理员可以调整的值不得放在部署期配置中，应放入本模块的 `settings.schema.json`。

#### 5.14.3 Profile

| Profile | 环境（系统设计 27.2） | 要点 |
|---|---|---|
| `local` | 本地开发（第 11 章） | Docker Compose 的 MySQL、Redis、Meilisearch、MinIO、Mailpit、imgproxy；`migrate-on-startup = true`；开发密钥；`cdn.type = none`；`translation-engine.type = fake`；可读文本日志；尚未实现的服务接口由 testkit 替身补位（第 9、11 章）；`LocalDevSeeder`（`application` 的 `com.b2bweb.app.dev`，`b2bweb.dev.seed.enabled`，默认 true）在启动后经 core 的应用服务幂等创建本地站点、后台用户与角色（11.6） |
| `test` | CI 与集成测试 | Testcontainers；`b2bweb.core.scheduling.enabled = false`；可拨动时钟；事务内远程调用直接失败（5.7.8） |
| `dev` | 集成环境（主干自动部署） | `migrate-on-startup = true`；Sentry 环境 `dev` |
| `staging` | 验收 | 安全配置与 prod 相同；前台 `noindex` 与 Basic Auth 由反向代理与 Nuxt 负责 |
| `prod` | 生产 | `migrate-on-startup = false`（先运行 migrate 任务）；执行生产安全检查 |

`application.yml`（公共部分，示意）：

```yaml
spring:
  threads.virtual.enabled: true
  datasource:                                   # 应用账号（只有 DML）
    url: ${B2BWEB_DB_URL}
    username: ${B2BWEB_DB_USER}
    password: ${B2BWEB_DB_PASSWORD}
    hikari.maximum-pool-size: 20
  flyway.enabled: false                         # 迁移由模块管理器执行（5.2.2）
  data.redis.url: ${B2BWEB_REDIS_URL}
  session.redis.namespace: sys:core:session
  transaction.default-timeout: 30s
  modulith.events:
    completion-mode: update
    republish-outstanding-events-on-restart: false
    jdbc.schema-initialization.enabled: false
server:
  shutdown: graceful
  forward-headers-strategy: native
management:
  server.port: 8081
  endpoints.web.exposure.include: health,info,prometheus
b2bweb:
  core:
    modules.migrate-on-startup: false
    migration.datasource:                       # 迁移账号（DDL），只在迁移时使用
      username: ${B2BWEB_DB_MIGRATION_USER}
      password: ${B2BWEB_DB_MIGRATION_PASSWORD}
    internal.tokens: ${B2BWEB_INTERNAL_TOKENS}
    jobs.concurrency: 4
    security.reauth-window: 10m
  infra:
    object-storage.type: s3
    translation-engine.type: openai-compatible
    cdn.type: cloudflare
```

#### 5.14.4 生产安全检查

`ProductionSafetyCheck` 在 `staging`、`prod` 中于启动步骤 1 之前执行：

| 检查 | 不满足时 |
|---|---|
| `B2BWEB_SECRET_KEYS` 已配置，且不是公开的开发密钥 | 拒绝启动 |
| 内部令牌已配置，每个至少 32 字节 | 拒绝启动 |
| `object-storage.type ≠ local`；`translation-engine.type ≠ fake`；`virus-scanner.type ≠ fake` | 拒绝启动 |
| 没有开启任何 `b2bweb.dev.*` 开关（替身补位、本地种子，11.5、11.6） | 拒绝启动 |
| 部署级 SMTP 的 `security ≠ NONE`（4.7.4） | 拒绝启动 |
| `migrate-on-startup = false`；应用数据源账号不是迁移账号 | 拒绝启动 |
| 管理端口与 Web 端口不同 | 拒绝启动 |
| `cdn.type = none` | 警告 |

JVM 与容器：时区 `UTC`（`TZ=UTC`、`-Duser.timezone=UTC`）、`-Dfile.encoding=UTF-8`、`-XX:MaxRAMPercentage=75`；JDK 版本以 ADR-011 为准。

---

## 6. platform-core 数据模型
platform-core 是 L0 平台内核，不可停用：站点与语言、用户与角色权限（含数据范围、2FA）、模块注册与生命周期、设置、站点资料、前台界面文案、字典、审计、通用异步任务、跨实例锁、数据主体请求（见系统设计 3.1、3.2、3.5、3.6、5.1–5.3、16.3、17.11、22.2、25.7、25.8、26.1–26.6）。

### 6.1 领域模型

| 类型 | 名称 | 说明 |
|---|---|---|
| 聚合根 | Site（`sys_site`） | 站点：最高隔离单位；主题、源语言、默认对外语言、URL 策略、时区、状态 |
| 实体 | SiteDomain（`sys_site_domain`） | 站点域名：主域名 / 别名 /【V2】语言独立域名；变更时递增 `sys_site.version` |
| 实体 | SiteLocale（`sys_site_locale`） | 站点语言（含源语言行）：URL 前缀、hreflang、对外公开、自动翻译、排序；变更时递增 `sys_site.version` |
| 聚合根 | SiteModule（`sys_site_module`） | 某站点对某模块的启用状态；启停有独立的影响分析与并发控制，因此单独成聚合 |
| 聚合根 | Module（`sys_module`） | 平台级模块：安装状态、版本、描述符（module.json） |
| 实体 | Permission（`sys_permission`） | 权限点，由 module.json 幂等同步，后台只读 |
| 实体 | Menu（`sys_menu`） | 后台菜单项，由 module.json 幂等同步，后台只读 |
| 聚合根 | SiteProfile（`sys_site_profile`） | 站点资料（每站点一份）：Logo、联系方式、社交账号、地址结构、营业时间、法定信息 |
| 实体 | SiteProfileLocalization（`sys_site_profile_localization`） | 站点资料中可翻译文本的各语言版本（站点资源本地化，第 2.7 节） |
| 聚合根 | UiText（`sys_ui_text`） | 前台界面文案：一个（站点，文案键，语言）一行；`zh-CN` 行是站点对主题默认文案的覆盖，其他语言行是译文 |
| 聚合根 | User（`sys_user`） | 后台用户，全局唯一 |
| 实体 | UserMfa（`sys_user_mfa`） | 用户的 TOTP 密钥与恢复码 |
| 聚合根 | Role（`sys_role`） | 角色定义（全局），按站点分配 |
| 实体 | RolePermission（`sys_role_permission`） | 角色拥有的权限及数据范围；按角色整体替换 |
| 聚合根 | UserSiteRole（`sys_user_site_role`） | 用户在某站点担任某角色，可限定语言范围 |
| 聚合根 | Setting（`sys_setting`） | 一个设置键的值（`site_id = 0` 为系统级） |
| 聚合根 | DictType（`sys_dict_type`） | 字典类型（全局，只用于后台） |
| 实体 | DictItem（`sys_dict_item`） | 字典项 |
| 聚合根 | AuditLog（`sys_audit_log`） | 审计记录，只追加 |
| 聚合根 | Job（`sys_job`） | 通用持久化异步任务：进度、结果摘要、结果文件 |
| 聚合根 | Lock（`sys_lock`） | 带过期时间的跨实例互斥锁（ShedLock 兼容结构） |
| 聚合根 | DsrRequest（`sys_dsr_request`） | 数据主体请求（系统级，跨站点）：受理、核验、延期、处理、答复 |
| 值对象 | LocaleCode | BCP 47 语言代码；源语言固定 `zh-CN` |
| 值对象 | DataScope | 数据范围 `ALL` / `GROUP` / `OWN` |
| 值对象 | LocaleScope | 语言范围：目标语言代码数组；NULL 表示不限 |
| 值对象 | EncryptedSecret | 加密信封 `v1:{keyId}:{Base64(nonce‖密文‖tag)}`：AES-256-GCM，12 字节随机 nonce，密钥来自环境变量或密钥管理服务（系统设计 26.6）；`keyId` 用于密钥轮换。TOTP 密钥与设置中的敏感字段共用此格式 |
| 值对象 | ModuleDescriptor | module.json 全文及其 SHA-256 |
| 值对象 | SourceFingerprint | 站点资源源语言指纹（第 2.7 节） |
| 值对象 | JobProgress | 任务进度：总量、已处理、失败、阶段说明 |
| 值对象 | DsrDeadline | 数据主体请求时限：确认收到期限、答复期限、延期期限 |

**不变量**：

1. 站点的源语言恒为 `zh-CN`。——数据库 `CHECK`（`ck_sys_site_source_locale`）。
2. 每个站点有且只有一条源语言行（`sys_site_locale.locale = 'zh-CN'`），随站点创建，不可删除；源语言行不能开启自动翻译。——前者应用层保证；后者数据库 `CHECK`（`ck_sys_locale_source_auto`）。
3. `sys_site.default_locale` 必须是该站点 `public = 1` 的语言；取消某语言的对外公开前，若它是默认对外语言则拒绝。——应用层（站点聚合内校验）。
4. URL 前缀在站点内唯一、非空、只含小写字母、数字与连字符（全部语言都带前缀，系统设计 5.4）。——数据库唯一索引 + `CHECK`（`ck_sys_locale_prefix`）。
5. 域名全局唯一（一个域名只解析到一个站点）且为小写；每个站点至多一个主域名。——数据库唯一索引 + `CHECK`；状态为 `ACTIVE` 的站点必须恰好有一个主域名——应用层。
6. `type = 'LOCALE'` 的域名必须带语言，其他类型不得带语言。——数据库 `CHECK`（`ck_sys_domain_locale`）。
7. 每个站点对每个 `INSTALLED` 模块都有一行 `sys_site_module`（建站与模块首次安装时补齐）；`disableable = 0` 的模块恒为启用；启用某模块前其 `requires` 中的模块必须已在该站点启用，停用某模块前不得有已启用模块依赖它。——应用层。
8. 未删除用户之间邮箱唯一（不区分大小写）。——数据库唯一索引（`email_normalized` 为生成列，保证规范化值始终与 `email` 一致）。
9. 密码只保存 Argon2id 哈希；密码、TOTP 密钥、恢复码不得出现在日志与审计差异中。——应用层（ADR-010）。
10. 角色包含任一 `is_sensitive = 1` 的权限，或权限码的 action 为 `publish` / `manage` 的权限时，`mfa_required = 1`（开发者类权限如 `tracking:script:edit` 由声明模块标记为 `sensitive`）；用户在任一站点持有此类角色时，未启用 2FA（`sys_user_mfa.status = 'ACTIVE'`）不能访问后台（ADR-010）。——应用层（权限同步与角色权限变更后重算）。
11. `sys_role_permission.data_scope`：权限声明了 `data_scopes` 时必须非空且取自其中；未声明时必须为空。——应用层。
12. 非系统管理员只能访问自己在 `sys_user_site_role` 中有角色的站点。——应用层 + 站点隔离测试（系统设计 28）。
13. 角色定义、站点、模块、系统级设置、数据主体请求只能由系统管理员管理；站点管理员只能在本站点给用户分配角色。——应用层（权限点）。
14. `sys_permission`、`sys_menu` 只由模块同步写入；从 module.json 中消失的行标记 `removed_at`，不参与鉴权与菜单。——应用层。
15. `sys_setting` 中 Schema 标记为敏感（`x-secret`）的字段必须以 EncryptedSecret 保存，且该行 `encrypted = 1`；接口返回时只返回掩码。——应用层。
16. `sys_audit_log` 只追加：业务代码只能插入与查询，保留期清理由定时任务按 `created_at` 批量删除。——应用层（ArchUnit 检查其 Mapper 只有 insert / select）+ 数据库账号授权。
17. 任务状态只能按 `PENDING → RUNNING → SUCCEEDED / FAILED / CANCELED`、`PENDING → CANCELED`、`FAILED → PENDING`（`attempts < max_attempts` 时重试）流转；状态变更使用条件更新（`WHERE status = ? AND version = ?`）。——应用层。
18. 数据主体请求在 `verified_at` 为空时不得导出、不得删除（`verification_method = 'NOT_REQUIRED'` 的退出“出售 / 共享”请求除外）；导出文件只发送到记录中的邮箱；延期后 `due_at = extended_until`。——应用层。
19. 站点资源本地化行（`sys_site_profile_localization`、`sys_ui_text`）：源语言行 `translation_state = 'NONE'` 且 `source_fingerprint` 为空；目标语言行的 `source_fingerprint` 与当前源语言指纹不同即为过期。——应用层。
20. `sys_ui_text` 中 `zh-CN` 行只在站点覆盖了主题默认文案时存在；删除覆盖即恢复主题默认。——应用层。

**补充与取舍**（系统设计之外的补充及理由）：

| 项 | 内容与理由 |
|---|---|
| 模块 ID 用字符串 | `module_id` 存 module.json 的 `id`（如 `inquiry`），而不是 TSID：在各环境中稳定，与 `apiPrefix`、Admin 的 `modules/<id>/`、Flyway 位置一致。`sys_module.id` 仍是 BIGINT 主键，`module_id` 为唯一键，模块内其他表以它为外键 |
| 字典拆为两张表 | 系统设计 25.8 写作 `sys_dict`，落地为 `sys_dict_type` + `sys_dict_item`；字典只用于后台（中文标签），前台可见的选项一律走文案键或站点资源，因此字典不翻译 |
| 站点资料本地化表 | 新增 `sys_site_profile_localization`：16.3 要求站点资料的文本以中文录入、按语言翻译与修订 |
| 法定信息不翻译 | 法定名称、注册地址、代表人、登记机关、注册号、VAT ID 等按登记原文录入在 `sys_site_profile` 中，各语言一致，不进入翻译流程（避免机器改写法定名称与地址）；Imprint 中的字段标签使用界面文案键 |
| 站点源语言列 | `sys_site.source_locale` 固定 `zh-CN` 并用 `CHECK` 约束，供各模块读取，不在代码中散落常量 |
| 主域名生成列 | `sys_site_domain.primary_flag` + 唯一索引，在数据库层保证每站点至多一个主域名 |
| 模块描述符 | `sys_module` 增加 `descriptor`、`descriptor_hash`（未变化时跳过同步）、`target_version` 与 `last_error`（FAILED 时保留现场，系统设计 3.6） |
| 权限与菜单字段 | `sys_permission.name`、`locale_scoped`，`sys_menu.name`、`icon`：后台需要中文名称；翻译审校的语言范围需要声明哪些权限受语言限制。需要 module.json 对应增加字段（见 6.6） |
| 角色强制 2FA 标记 | `sys_role.mfa_required` 是由权限推导的反规范化字段，供登录时快速判断与后台展示 |
| 账号安全字段 | `sys_user.must_change_password`（管理员创建或重置密码后强制修改）、`failed_login_count` / `locked_until`（账号级锁定，配合 Redis 中“账号 + IP”的限流）；`sys_user_mfa.last_used_step`（拒绝同一 TOTP 时间步的重放） |
| 设置的归属与加密标记 | `sys_setting.module_id`（按模块列出与清理）、`encrypted`（密钥轮换任务按此查找）；系统级行 `site_id = 0` |
| 通用任务的执行字段 | `sys_job` 的 `site_id = 0` 表示系统级任务；`parent_job_id`（导入的“校验预览 → 执行”两阶段）、`idempotency_key`（系统设计 24.2）、`worker_id` / `heartbeat_at`（多实例认领与失联恢复）、`cancel_requested`（协作取消）、`result_file_*`（结果文件放私有桶，不进入媒体库） |
| 锁表兼容 ShedLock | `sys_lock` 采用 ShedLock JDBC 结构（自然键主键 `lock_name`），同时用作定时任务锁与带过期时间的业务锁。**模块迁移锁改用 MySQL 命名锁** `GET_LOCK('platform:migrate', …)`：首次启动时 `sys_lock` 尚未由迁移创建，且命名锁在会话断开时自动释放（需同步修订系统设计 3.6 第 3 步） |
| 数据主体请求字段 | 在 17.11 所列字段之外增加：`request_no`（与请求人往来）、`channel`、授权代理人字段（CCPA）、`ack_due_at`（CCPA 10 个工作日内确认收到）、`extension_notified_at`、确认链接令牌哈希与有效期、`export_job_id`、`result`（各模块处理结果）、`outcome_note`、`received_site_id`。完成的 `ERASE` 请求即“删除台账”，备份恢复后据此重新执行删除，不另建表 |
| 审计字段 | `sys_audit_log` 增加 `user_name`（操作人快照，用户改名或删除后仍可读）、`result`、`is_sensitive`、`trace_id`（与日志关联） |
| 站点模块配置 | `sys_site_module.config` 只放启停相关配置（如停用后路由处理）；模块业务设置统一放 `sys_setting`，避免两处存放 |

### 6.2 ER 图

```mermaid
erDiagram
    sys_site ||--o{ sys_site_domain : "has"
    sys_site ||--o{ sys_site_locale : "has"
    sys_site ||--o{ sys_site_module : "enables"
    sys_module ||--o{ sys_site_module : "enabled in"
    sys_module ||--o{ sys_permission : "declares"
    sys_module ||--o{ sys_menu : "declares"
    sys_module ||--o{ sys_dict_type : "declares"
    sys_dict_type ||--o{ sys_dict_item : "contains"
    sys_site ||--|| sys_site_profile : "has"
    sys_site_profile ||--o{ sys_site_profile_localization : "localized as"
    sys_site ||--o{ sys_ui_text : "customizes"
    sys_user ||--o| sys_user_mfa : "secured by"
    sys_user ||--o{ sys_user_site_role : "holds"
    sys_site ||--o{ sys_user_site_role : "grants"
    sys_role ||--o{ sys_user_site_role : "assigned as"
    sys_role ||--o{ sys_role_permission : "grants"
    sys_permission ||--o{ sys_role_permission : "granted by"
    sys_permission |o..o{ sys_menu : "guards"
    sys_site |o..o{ sys_setting : "configures"
    sys_site |o..o{ sys_job : "runs"
    sys_job |o--o{ sys_job : "follows"
    sys_job |o--o{ sys_dsr_request : "exports for"
    sys_site |o..o{ sys_audit_log : "records"
    sys_user ||..o{ sys_audit_log : "performs"
```

实线为外键，虚线为不建外键的逻辑关系（`site_id = 0` 的系统级行、只追加的审计日志、按权限码关联的菜单）。`sys_lock` 与其他表无关系，未画出。

### 6.3 表清单

| 表 | 说明 | 站点级 | 软删除 | 预估规模（单站，按系统设计假设 A4） |
|---|---|---|---|---|
| `sys_module` | 模块的平台级安装状态与版本 | 否（全局） | 否 | ≈ 20（全局） |
| `sys_site` | 站点 | 否（本身即站点） | 否（用 `ARCHIVED` 归档） | ≤ 10（全局） |
| `sys_site_domain` | 站点域名 | 是 | 否 | ≤ 10 |
| `sys_site_locale` | 站点语言 | 是 | 否 | ≤ 10 |
| `sys_site_module` | 模块的站点级启用状态 | 是 | 否 | ≈ 20 |
| `sys_site_profile` | 站点资料（语言无关部分与法定信息） | 是 | 否 | 1 |
| `sys_site_profile_localization` | 站点资料的可翻译文本 | 是 | 否 | ≤ 10 |
| `sys_ui_text` | 前台界面文案的站点覆盖与各语言译文 | 是 | 否 | ≈ 500 个键 × 10 种语言 ≈ 5 千 |
| `sys_user` | 后台用户 | 否（全局） | 是 | ≤ 200（全局） |
| `sys_user_mfa` | 用户 2FA | 否（全局） | 否 | ≤ 200（全局） |
| `sys_role` | 角色定义 | 否（全局） | 是 | ≤ 30（全局） |
| `sys_permission` | 权限点 | 否（全局） | 否（`removed_at` 标记） | ≈ 300（全局） |
| `sys_role_permission` | 角色权限 | 否（全局） | 否 | ≈ 3 千（全局） |
| `sys_user_site_role` | 用户站点角色 | 是 | 否 | ≤ 300 |
| `sys_menu` | 后台菜单 | 否（全局） | 否（`removed_at` 标记） | ≈ 150（全局） |
| `sys_setting` | 设置 | 是（`site_id = 0` 为系统级） | 否 | ≈ 100 |
| `sys_dict_type` | 字典类型 | 否（全局） | 否 | ≈ 50（全局） |
| `sys_dict_item` | 字典项 | 否（全局） | 否 | ≈ 1 千（全局） |
| `sys_audit_log` | 审计日志（只追加） | `site_id` 可空 | 否（默认保留 1 年） | ≈ 3 万 / 月，1 年 ≈ 40 万 |
| `sys_job` | 通用异步任务 | 是（`site_id = 0` 为系统级） | 否（默认保留 180 天） | ≈ 1 千 / 月 |
| `sys_lock` | 跨实例互斥锁 | 否（全局） | 否 | ≈ 50（全局） |
| `sys_dsr_request` | 数据主体请求 | 否（全局） | 否 | < 100 / 年（全局） |
| `sys_transaction_mutex`（Proposed） | 事务互斥与generation，详见6.7 | 是 | 否（正常任务不删除行） | 每站每注册技术名称1行 |

`EVENT_PUBLICATION`（Spring Modulith 事件发布登记）与 `flyway_core_history` 属于框架表，见第 20 章。

**站点过滤**（行级拦截器 `TenantLineInnerInterceptor`，列名 `site_id`，系统设计 25.7）：

| 表 | 拦截器 | 访问方式 |
|---|---|---|
| `sys_site`、`sys_module`、`sys_user`、`sys_user_mfa`、`sys_role`、`sys_permission`、`sys_role_permission`、`sys_menu`、`sys_dict_type`、`sys_dict_item`、`sys_lock`、`sys_dsr_request` | 豁免（全局表） | 写入只经系统管理接口或模块同步；`sys_site` 的列表按用户可访问的站点过滤 |
| `sys_audit_log` | 豁免（`site_id` 可空） | 站点内查询必须显式带 `site_id = 当前站点`；全部站点与系统级记录只对系统管理员开放 |
| `sys_setting`、`sys_job` | 不豁免 | 站点级行由拦截器过滤；系统级行（`site_id = 0`）只能通过标注 `@InterceptorIgnore` 的专用 Mapper 方法读写，并显式带 `site_id = 0`；任务执行器轮询全部站点的待执行任务也使用专用豁免查询 |
| `sys_site_domain` | 不豁免 | 例外：`SiteResolver` 按域名解析站点发生在站点上下文建立之前，使用专用豁免查询 `WHERE domain = ?`（结果缓存） |
| `sys_user_site_role` | 不豁免 | 例外：登录后列出“我可访问的站点”需要跨站点，使用专用豁免查询 `WHERE user_id = 当前用户` |
| 其余站点级表 | 不豁免 | — |


### 6.4 表结构

类型列中的 `bin` 表示 `COLLATE utf8mb4_0900_bin`；通用字段的定义见第 2.4 节。

#### 6.4.1 `sys_module`：模块（平台级安装状态与版本）

由启动流程（系统设计 3.6）维护：新模块 `INSTALLING → INSTALLED`，版本升高 `UPGRADING → INSTALLED`，失败为 `FAILED`（阻止启动、保留现场），构建中移除为 `MISSING`，系统管理员显式清除后为 `PURGED`。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `module_id` | VARCHAR(32) bin | 否 | — | 模块 ID（module.json 的 `id`，如 `core`、`inquiry`） |
| `name` | VARCHAR(64) | 否 | — | 模块名称（中文） |
| `layer` | VARCHAR(4) | 否 | — | 所在层：`L0` / `L2` / `L3` / `L4` |
| `disableable` | TINYINT(1) | 否 | 1 | 是否可按站点停用 |
| `table_prefix` | VARCHAR(16) bin | 是 | — | 表前缀（如 `inq_`）；无表模块为空 |
| `api_prefix` | VARCHAR(32) bin | 是 | — | API 前缀（`ModuleGuardFilter` 按此拦截） |
| `status` | VARCHAR(16) | 否 | `'INSTALLING'` | 平台级状态，见 6.5 |
| `installed_version` | VARCHAR(32) | 是 | — | 已安装成功的版本；首次安装完成前为空 |
| `target_version` | VARCHAR(32) | 是 | — | 正在安装或升级的目标版本；`INSTALLED` 时为空 |
| `descriptor` | JSON | 否 | — | 最近一次同步的 module.json 全文 |
| `descriptor_hash` | CHAR(64) bin | 否 | — | `descriptor` 的 SHA-256；未变化时跳过声明式资源同步 |
| `last_error` | TEXT | 是 | — | 最近一次安装或升级失败的错误摘要 |
| `installed_at` | DATETIME(3) | 是 | — | 首次安装成功时间 |
| `upgraded_at` | DATETIME(3) | 是 | — | 最近一次升级成功时间 |
| `missing_since` | DATETIME(3) | 是 | — | 从构建中移除（`MISSING`）的时间 |
| `purged_at` | DATETIME(3) | 是 | — | 数据清除（`PURGED`）时间 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 最后操作人（0 = 系统；`PURGED` 时为执行清除的系统管理员） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_mod_module_id (module_id)`：模块 ID 唯一；也是 `sys_site_module`、`sys_permission`、`sys_menu`、`sys_dict_type` 外键的被引用键

```sql
CREATE TABLE sys_module (
  id                 BIGINT       NOT NULL COMMENT '主键（TSID）',
  module_id          VARCHAR(32)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '模块 ID（module.json 的 id，如 core、inquiry）',
  name               VARCHAR(64)  NOT NULL COMMENT '模块名称（中文）',
  layer              VARCHAR(4)   NOT NULL COMMENT '所在层：L0 / L2 / L3 / L4',
  disableable        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否可按站点停用（module.json 的 disableable）',
  table_prefix       VARCHAR(16)  COLLATE utf8mb4_0900_bin NULL COMMENT '表前缀（如 inq_）；无表模块为空',
  api_prefix         VARCHAR(32)  COLLATE utf8mb4_0900_bin NULL COMMENT 'API 前缀（ModuleGuardFilter 按此拦截）',
  status             VARCHAR(16)  NOT NULL DEFAULT 'INSTALLING' COMMENT '平台级状态：INSTALLING / INSTALLED / UPGRADING / FAILED / MISSING / PURGED',
  installed_version  VARCHAR(32)  NULL COMMENT '已安装成功的版本（语义化版本）；首次安装完成前为空',
  target_version     VARCHAR(32)  NULL COMMENT '正在安装或升级的目标版本；INSTALLED 时为空',
  descriptor         JSON         NOT NULL COMMENT '最近一次同步的 module.json 全文',
  descriptor_hash    CHAR(64)     COLLATE utf8mb4_0900_bin NOT NULL COMMENT 'descriptor 的 SHA-256；未变化时跳过声明式资源同步',
  last_error         TEXT         NULL COMMENT '最近一次安装或升级失败的错误摘要',
  installed_at       DATETIME(3)  NULL COMMENT '首次安装成功时间',
  upgraded_at        DATETIME(3)  NULL COMMENT '最近一次升级成功时间',
  missing_since      DATETIME(3)  NULL COMMENT '从构建中移除（MISSING）的时间',
  purged_at          DATETIME(3)  NULL COMMENT '数据清除（PURGED）时间',
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by         BIGINT       NOT NULL DEFAULT 0 COMMENT '最后操作人（0 = 系统；PURGED 时为执行清除的系统管理员）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mod_module_id (module_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模块（平台级安装状态与版本）';
```

#### 6.4.2 `sys_site`：站点

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID），即其他表的 `site_id` |
| `code` | VARCHAR(32) bin | 否 | — | 站点代码（小写字母、数字、连字符，如 `brand-a`），用于运维与日志，创建后不可修改 |
| `name` | VARCHAR(100) | 否 | — | 站点名称（后台显示，中文） |
| `theme_key` | VARCHAR(64) bin | 否 | — | 主题 key（`theme.json` 的 key，如 `industrial`） |
| `source_locale` | VARCHAR(16) | 否 | `'zh-CN'` | 源语言（编辑录入语言），固定 `zh-CN` |
| `default_locale` | VARCHAR(16) | 否 | `'zh-CN'` | 默认对外语言：根路径 302 与 `x-default` 的目标；必须是对外公开的站点语言 |
| `url_strategy` | VARCHAR(16) | 否 | `'SUBDIRECTORY'` | URL 策略：`SUBDIRECTORY`（V1）/ `DOMAIN`（V2） |
| `trailing_slash` | VARCHAR(8) | 否 | `'NEVER'` | 尾斜杠策略：`NEVER`（统一去掉）/ `ALWAYS`（统一保留），系统设计 13.2 |
| `timezone` | VARCHAR(64) | 否 | — | 站点时区（IANA，如 `Europe/Berlin`）：定时发布的输入与展示、按天统计 |
| `status` | VARCHAR(16) | 否 | `'SETUP'` | 状态，见 6.5 |
| `version` | INT | 否 | 0 | 乐观锁；域名与站点语言变更时同样递增 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_site_code (code)`：站点代码唯一
- `ck_sys_site_source_locale`：`source_locale = 'zh-CN'`

```sql
CREATE TABLE sys_site (
  id              BIGINT        NOT NULL COMMENT '主键（TSID），即其他表的 site_id',
  code            VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '站点代码（小写字母、数字、连字符，如 brand-a）；创建后不可修改',
  name            VARCHAR(100)  NOT NULL COMMENT '站点名称（后台显示，中文）',
  theme_key       VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '主题 key（theme.json 的 key，如 industrial）',
  source_locale   VARCHAR(16)   NOT NULL DEFAULT 'zh-CN' COMMENT '源语言（编辑录入语言），固定 zh-CN',
  default_locale  VARCHAR(16)   NOT NULL DEFAULT 'zh-CN' COMMENT '默认对外语言：根路径 302 与 x-default 的目标；必须是对外公开的站点语言',
  url_strategy    VARCHAR(16)   NOT NULL DEFAULT 'SUBDIRECTORY' COMMENT 'URL 策略：SUBDIRECTORY（V1，全部语言带前缀）/ DOMAIN（V2，语言独立域名）',
  trailing_slash  VARCHAR(8)    NOT NULL DEFAULT 'NEVER' COMMENT '尾斜杠策略：NEVER（统一去掉）/ ALWAYS（统一保留）',
  timezone        VARCHAR(64)   NOT NULL COMMENT '站点时区（IANA，如 Europe/Berlin）：定时发布的输入与展示、按天统计',
  status          VARCHAR(16)   NOT NULL DEFAULT 'SETUP' COMMENT '状态：SETUP（建设中）/ ACTIVE（对外服务）/ ARCHIVED（已归档）',
  version         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（域名、站点语言变更时同样递增）',
  created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by      BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by      BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_site_code (code),
  CONSTRAINT ck_sys_site_source_locale CHECK (source_locale = 'zh-CN')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点';
```

#### 6.4.3 `sys_site_domain`：站点域名

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `domain` | VARCHAR(253) bin | 否 | — | 域名：小写，不含协议与端口；国际化域名存 Punycode（A-label） |
| `type` | VARCHAR(8) | 否 | `'ALIAS'` | 类型：`PRIMARY` / `ALIAS` / `LOCALE`（V2），见 6.5 |
| `locale` | VARCHAR(16) | 是 | — | 语言独立域名对应的语言；仅 `type = 'LOCALE'` 时非空 |
| `primary_flag` | TINYINT（生成列） | 是 | `IF(type = 'PRIMARY', 1, NULL)` | 主域名标记（唯一索引用） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_dom_domain (domain)`：域名全局唯一；`SiteResolver` 按域名解析站点（专用豁免查询）
- `uk_dom_primary (site_id, primary_flag)`：每站点至多一个主域名；同时服务“列出站点的域名”与外键
- `fk_sys_domain_site`：→ `sys_site.id`，`ON DELETE CASCADE`
- `ck_sys_domain_lower`：域名必须为小写
- `ck_sys_domain_locale`：`LOCALE` 类型必须带语言，其他类型不得带语言

```sql
CREATE TABLE sys_site_domain (
  id            BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id       BIGINT        NOT NULL COMMENT '站点',
  domain        VARCHAR(253)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '域名：小写，不含协议与端口；国际化域名存 Punycode（A-label）',
  type          VARCHAR(8)    NOT NULL DEFAULT 'ALIAS' COMMENT '类型：PRIMARY（主域名）/ ALIAS（别名，301 到主域名）/ LOCALE（V2：语言独立域名）',
  locale        VARCHAR(16)   NULL COMMENT '语言独立域名对应的语言；仅 type = LOCALE 时非空',
  primary_flag  TINYINT       AS (IF(type = 'PRIMARY', 1, NULL)) STORED COMMENT '主域名标记（生成列，唯一索引用：每站点至多一个主域名）',
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dom_domain (domain),
  UNIQUE KEY uk_dom_primary (site_id, primary_flag),
  CONSTRAINT fk_sys_domain_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE,
  CONSTRAINT ck_sys_domain_lower CHECK (domain = LOWER(domain)),
  CONSTRAINT ck_sys_domain_locale CHECK ((type = 'LOCALE') = (locale IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点域名';
```

#### 6.4.4 `sys_site_locale`：站点语言

包含源语言行（`locale = 'zh-CN'`，随站点创建，不可删除）。V1 不提供删除站点语言：不再使用的语言取消对外公开并关闭自动翻译即可，各模块按该语言保存的数据保留；如需支持删除，须先在 platform-api 中增加查询语言使用情况的契约（需 ADR，见 6.6）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `locale` | VARCHAR(16) | 否 | — | 语言代码（BCP 47，如 `zh-CN`、`en`、`de`、`pt-BR`） |
| `url_prefix` | VARCHAR(32) bin | 否 | — | URL 前缀（不含斜杠，如 `zh`、`en`、`pt-br`）；全部语言都带前缀 |
| `hreflang` | VARCHAR(16) | 否 | — | hreflang 值（默认只用语言代码：`zh` 或 `zh-Hans`、`en`、`de`） |
| `public` | TINYINT(1) | 否 | 0 | 对外公开：前台可访问，并出现在语言切换器、hreflang、sitemap 中 |
| `auto_translate` | TINYINT(1) | 否 | 0 | 自动翻译：源语言发布、站点资源变化时自动翻译到该语言；源语言行恒为 0 |
| `sort_order` | INT | 否 | 0 | 排序（语言切换器、后台语言标签），小的在前（系统设计 5.1 中的 `sort`） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_sloc_locale (site_id, locale)`：站点内语言唯一；服务“读取站点全部语言”（每站点 ≤ 10 行，排序在内存中完成）
- `uk_sloc_prefix (site_id, url_prefix)`：站点内 URL 前缀唯一；请求路径前缀 → 语言
- `fk_sys_locale_site`：→ `sys_site.id`，`ON DELETE CASCADE`
- `ck_sys_locale_prefix`：前缀只含小写字母、数字与连字符
- `ck_sys_locale_source_auto`：源语言行不能开启自动翻译

```sql
CREATE TABLE sys_site_locale (
  id              BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id         BIGINT       NOT NULL COMMENT '站点',
  locale          VARCHAR(16)  NOT NULL COMMENT '语言代码（BCP 47，如 zh-CN、en、de、pt-BR）；zh-CN 为源语言行',
  url_prefix      VARCHAR(32)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT 'URL 前缀（不含斜杠，如 zh、en、pt-br）；全部语言都带前缀',
  hreflang        VARCHAR(16)  NOT NULL COMMENT 'hreflang 值（默认只用语言代码：zh 或 zh-Hans、en、de）',
  public          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '对外公开：前台可访问，并出现在语言切换器、hreflang、sitemap 中',
  auto_translate  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '自动翻译：源语言发布、站点资源变化时自动翻译到该语言；源语言行恒为 0',
  sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序（语言切换器、后台语言标签），小的在前',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by      BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sloc_locale (site_id, locale),
  UNIQUE KEY uk_sloc_prefix (site_id, url_prefix),
  CONSTRAINT fk_sys_locale_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE,
  CONSTRAINT ck_sys_locale_prefix CHECK (REGEXP_LIKE(url_prefix, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
  CONSTRAINT ck_sys_locale_source_auto CHECK (locale <> 'zh-CN' OR auto_translate = 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点语言';
```

#### 6.4.5 `sys_site_module`：模块的站点级启用状态

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `module_id` | VARCHAR(32) bin | 否 | — | 模块 ID（`sys_module.module_id`） |
| `enabled` | TINYINT(1) | 否 | 1 | 站点级状态：1 = `ENABLED`，0 = `DISABLED`；`disableable = 0` 的模块恒为 1 |
| `config` | JSON | 是 | — | 启停相关配置，如停用后该模块内容类型路由的处理 `{"onDisable":"NOT_FOUND","redirectContentId":null}`（`REDIRECT` 时为批量 301 的目标内容）；模块业务设置不放这里 |
| `enabled_at` | DATETIME(3) | 是 | — | 最近一次启用时间 |
| `disabled_at` | DATETIME(3) | 是 | — | 最近一次停用时间 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_smod_module (site_id, module_id)`：每站点每模块一行；服务“当前站点启用的模块”（启动菜单、`ModuleGuardFilter`、扩展点过滤，结果缓存）
- `idx_smod_module (module_id)`：系统管理“哪些站点启用了某模块”、模块清除前检查；外键索引
- `fk_sys_sitemod_site`：→ `sys_site.id`，`ON DELETE CASCADE`
- `fk_sys_sitemod_module`：→ `sys_module.module_id`（RESTRICT：清除模块数据前先删除其站点级行）

```sql
CREATE TABLE sys_site_module (
  id           BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id      BIGINT       NOT NULL COMMENT '站点',
  module_id    VARCHAR(32)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '模块 ID（sys_module.module_id）',
  enabled      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '站点级状态：1 = ENABLED，0 = DISABLED；disableable = 0 的模块恒为 1',
  config       JSON         NULL COMMENT '启停相关配置，如停用后路由处理 {"onDisable":"NOT_FOUND 或 REDIRECT","redirectContentId":"…"}；模块业务设置不放这里',
  enabled_at   DATETIME(3)  NULL COMMENT '最近一次启用时间',
  disabled_at  DATETIME(3)  NULL COMMENT '最近一次停用时间',
  version      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_smod_module (site_id, module_id),
  KEY idx_smod_module (module_id),
  CONSTRAINT fk_sys_sitemod_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE,
  CONSTRAINT fk_sys_sitemod_module FOREIGN KEY (module_id) REFERENCES sys_module (module_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模块的站点级启用状态';
```

#### 6.4.6 `sys_site_profile`：站点资料（语言无关部分与法定信息）

每个站点一份（系统设计 16.3）。可翻译文本在 `sys_site_profile_localization`；法定信息按登记原文录入、各语言一致、不翻译，用于自动生成 Imprint（系统设计 10.1），具体内容由业主确认（32 章 Q14）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点（每站点一份） |
| `logo_media_id` | BIGINT | 是 | — | 标准 Logo（浅色背景上使用），`mda_media.id` |
| `logo_inverse_media_id` | BIGINT | 是 | — | 反白 Logo（深色背景上使用），`mda_media.id` |
| `favicon_media_id` | BIGINT | 是 | — | favicon，`mda_media.id` |
| `app_icon_media_id` | BIGINT | 是 | — | 应用图标（apple-touch-icon、Web App Manifest），`mda_media.id` |
| `email` | VARCHAR(254) | 是 | — | 对外联系邮箱 |
| `phone` | VARCHAR(32) | 是 | — | 对外联系电话（E.164） |
| `whatsapp` | VARCHAR(32) | 是 | — | WhatsApp 号码（E.164） |
| `social_links` | JSON | 是 | — | 社交账号 `[{"platform":"LINKEDIN","url":"…"}]`，平台取值见 6.5 |
| `addresses` | JSON | 是 | — | 地址的语言无关部分（可多个）`[{"id":"a1","type":"HEADQUARTERS","countryCode":"DE","postalCode":"80331","geo":{"lat":0,"lng":0},"phone":"…","primary":true}]`；街道、城市等文本在本地化表中按 `id` 对应 |
| `opening_hours` | JSON | 是 | — | 营业时间 `{"timezone":"Europe/Berlin","rules":[{"days":["MO","TU"],"opens":"09:00","closes":"18:00"}]}` |
| `founded_year` | SMALLINT | 是 | — | 成立年份 |
| `legal_name` | VARCHAR(300) | 是 | — | 法定名称（含法律形式，如 `XYZ GmbH`），按登记原文 |
| `registered_address` | VARCHAR(500) | 是 | — | 注册地址（可送达地址），按登记原文 |
| `legal_representatives` | VARCHAR(500) | 是 | — | 授权代表人（可多人），原文 |
| `register_authority` | VARCHAR(200) | 是 | — | 登记机关（如商业登记法院 `Amtsgericht München`），原文 |
| `register_number` | VARCHAR(64) | 是 | — | 注册号（如 `HRB 123456`） |
| `vat_id` | VARCHAR(32) | 是 | — | 增值税号（VAT ID，如 `DE123456789`） |
| `legal_email` | VARCHAR(254) | 是 | — | 法定联系邮箱（可快速电子联系） |
| `legal_phone` | VARCHAR(32) | 是 | — | 法定联系电话等第二联系渠道（E.164） |
| `supervisory_authority` | VARCHAR(300) | 是 | — | 监管机关（经营活动需要许可时），原文 |
| `content_responsible` | VARCHAR(500) | 是 | — | 内容负责人（姓名与地址；站点含新闻、博客等编辑内容时），原文 |
| `legal_extra` | JSON | 是 | — | 其他法定信息（法务确认后补充，如 GDPR 第 27 条欧盟代表 / 英国代表）`[{"key":"eu_representative","value":"…"}]`；`key` 对应界面文案键 `imprint.{key}` 作为标签 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_prof_site (site_id)`：每站点一份；站点引导数据、Organization JSON-LD、Imprint 按站点读取
- `fk_sys_profile_site`：→ `sys_site.id`，`ON DELETE CASCADE`

```sql
CREATE TABLE sys_site_profile (
  id                     BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id                BIGINT        NOT NULL COMMENT '站点（每站点一份）',
  logo_media_id          BIGINT        NULL COMMENT '标准 Logo（浅色背景上使用），mda_media.id',
  logo_inverse_media_id  BIGINT        NULL COMMENT '反白 Logo（深色背景上使用），mda_media.id',
  favicon_media_id       BIGINT        NULL COMMENT 'favicon，mda_media.id',
  app_icon_media_id      BIGINT        NULL COMMENT '应用图标（apple-touch-icon、Web App Manifest），mda_media.id',
  email                  VARCHAR(254)  NULL COMMENT '对外联系邮箱',
  phone                  VARCHAR(32)   NULL COMMENT '对外联系电话（E.164）',
  whatsapp               VARCHAR(32)   NULL COMMENT 'WhatsApp 号码（E.164）',
  social_links           JSON          NULL COMMENT '社交账号：[{"platform":"LINKEDIN","url":"…"}]',
  addresses              JSON          NULL COMMENT '地址的语言无关部分（可多个）：[{"id","type","countryCode","postalCode","geo","phone","primary"}]；文本在本地化表中按 id 对应',
  opening_hours          JSON          NULL COMMENT '营业时间：{"timezone":"Europe/Berlin","rules":[{"days":["MO"],"opens":"09:00","closes":"18:00"}]}',
  founded_year           SMALLINT      NULL COMMENT '成立年份',
  legal_name             VARCHAR(300)  NULL COMMENT '法定名称（含法律形式，如 XYZ GmbH），按登记原文录入，不翻译',
  registered_address     VARCHAR(500)  NULL COMMENT '注册地址（可送达地址），按登记原文录入，不翻译',
  legal_representatives  VARCHAR(500)  NULL COMMENT '授权代表人（可多人），原文',
  register_authority     VARCHAR(200)  NULL COMMENT '登记机关（如商业登记法院 Amtsgericht München），原文',
  register_number        VARCHAR(64)   NULL COMMENT '注册号（如 HRB 123456）',
  vat_id                 VARCHAR(32)   NULL COMMENT '增值税号（VAT ID，如 DE123456789）',
  legal_email            VARCHAR(254)  NULL COMMENT '法定联系邮箱（可快速电子联系）',
  legal_phone            VARCHAR(32)   NULL COMMENT '法定联系电话等第二联系渠道（E.164）',
  supervisory_authority  VARCHAR(300)  NULL COMMENT '监管机关（经营活动需要许可时），原文',
  content_responsible    VARCHAR(500)  NULL COMMENT '内容负责人（姓名与地址；站点含新闻、博客等编辑内容时），原文',
  legal_extra            JSON          NULL COMMENT '其他法定信息（法务确认后补充）：[{"key":"eu_representative","value":"…"}]；key 对应文案键 imprint.{key}',
  version                INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at             DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by             BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at             DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by             BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_prof_site (site_id),
  CONSTRAINT fk_sys_profile_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点资料（语言无关部分与法定信息）';
```

#### 6.4.7 `sys_site_profile_localization`：站点资料本地化

站点资源本地化表（第 2.7 节）：`zh-CN` 行由站点管理员录入，其他语言行由 translation 经 core 的 `TranslatableResourceProvider`（资源类型 `SITE_PROFILE`）写入，可在翻译编辑器中按段修订。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `profile_id` | BIGINT | 否 | — | 站点资料（`sys_site_profile.id`） |
| `locale` | VARCHAR(16) | 否 | — | 语言；`zh-CN` 为源语言行 |
| `company_name` | VARCHAR(300) | 是 | — | 公司名称（对外显示；Organization `name`、Header / Footer） |
| `description` | VARCHAR(1000) | 是 | — | 公司简介（Footer、Organization `description`） |
| `addresses` | JSON | 是 | — | 地址的可翻译部分，按地址 `id`：`{"a1":{"label":"总部","street":"…","city":"…","region":"…"}}` |
| `opening_hours_note` | VARCHAR(500) | 是 | — | 营业时间补充说明（如节假日安排） |
| `translation_state` | VARCHAR(16) | 否 | `'NONE'` | 翻译状态（共享枚举） |
| `source_fingerprint` | CHAR(64) bin | 是 | — | 生成本行译文时源语言行的指纹；源语言行为空 |
| `version` | INT | 否 | 0 | 乐观锁（人工修订与译文回写并发） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人（译文行为 0） |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_ploc_locale (profile_id, locale)`：每种语言一行；站点引导数据按（资料，语言）读取
- `fk_sys_profile_loc_profile`：→ `sys_site_profile.id`，`ON DELETE CASCADE`

```sql
CREATE TABLE sys_site_profile_localization (
  id                  BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id             BIGINT         NOT NULL COMMENT '站点',
  profile_id          BIGINT         NOT NULL COMMENT '站点资料（sys_site_profile.id）',
  locale              VARCHAR(16)    NOT NULL COMMENT '语言（BCP 47）；zh-CN 为源语言行',
  company_name        VARCHAR(300)   NULL COMMENT '公司名称（对外显示；Organization name、Header / Footer）',
  description         VARCHAR(1000)  NULL COMMENT '公司简介（Footer、Organization description）',
  addresses           JSON           NULL COMMENT '地址的可翻译部分，按地址 id：{"a1":{"label":"总部","street":"…","city":"…","region":"…"}}',
  opening_hours_note  VARCHAR(500)   NULL COMMENT '营业时间补充说明（如节假日安排）',
  translation_state   VARCHAR(16)    NOT NULL DEFAULT 'NONE' COMMENT '翻译状态：NONE（源语言）/ QUEUED / TRANSLATED / NEEDS_REVIEW / FAILED / OUTDATED',
  source_fingerprint  CHAR(64)       COLLATE utf8mb4_0900_bin NULL COMMENT '生成本行译文时源语言行的指纹（可翻译字段规范化后拼接的 SHA-256）；源语言行为空',
  version             INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ploc_locale (profile_id, locale),
  CONSTRAINT fk_sys_profile_loc_profile FOREIGN KEY (profile_id) REFERENCES sys_site_profile (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点资料本地化（可翻译文本）';
```

#### 6.4.8 `sys_ui_text`：前台界面文案

前台固定文案只写成文案键（如 `form.submit`），主题以文案键提供默认中文文案（随主题包构建进 classpath），站点可以覆盖；查找顺序为站点覆盖 → 主题默认（系统设计 5.4、21.7）。本表保存站点覆盖（`zh-CN` 行）与各语言译文（替代早期设计中的 `sys_i18n_message`；后台界面文案不入库）。译文的“有效源文案”是站点覆盖或主题默认文案，由 core 的 `TranslatableResourceProvider`（资源类型 `UI_TEXT`）提取与回写。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `text_key` | VARCHAR(128) bin | 否 | — | 文案键（如 `form.submit`） |
| `locale` | VARCHAR(16) | 否 | — | 语言；`zh-CN` 行为站点覆盖，其他语言行为译文 |
| `text_value` | VARCHAR(1000) | 否 | — | 文案（纯文本，可含 `{name}` 形式的插值占位符，翻译时原样保留） |
| `translation_state` | VARCHAR(16) | 否 | `'NONE'` | 翻译状态（共享枚举）；`zh-CN` 行为 `NONE` |
| `source_fingerprint` | CHAR(64) bin | 是 | — | 生成本行译文时有效源文案的 SHA-256；`zh-CN` 行为空。主题升级改动默认文案或站点修改覆盖后与当前指纹不同，即为过期 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人（译文行为 0） |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_uitx_key (site_id, locale, text_key)`：唯一；同时服务站点引导数据“读取某语言的全部文案”（前缀 `site_id, locale` 范围扫描）
- `fk_sys_uitext_site`：→ `sys_site.id`，`ON DELETE CASCADE`

```sql
CREATE TABLE sys_ui_text (
  id                  BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id             BIGINT         NOT NULL COMMENT '站点',
  text_key            VARCHAR(128)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '文案键（如 form.submit）',
  locale              VARCHAR(16)    NOT NULL COMMENT '语言：zh-CN 行为站点对主题默认文案的覆盖，其他语言行为译文',
  text_value          VARCHAR(1000)  NOT NULL COMMENT '文案（纯文本，可含 {name} 形式的插值占位符，翻译时原样保留）',
  translation_state   VARCHAR(16)    NOT NULL DEFAULT 'NONE' COMMENT '翻译状态：NONE（源语言）/ QUEUED / TRANSLATED / NEEDS_REVIEW / FAILED / OUTDATED',
  source_fingerprint  CHAR(64)       COLLATE utf8mb4_0900_bin NULL COMMENT '生成本行译文时有效源文案（站点覆盖或主题默认文案）的 SHA-256；zh-CN 行为空',
  created_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_uitx_key (site_id, locale, text_key),
  CONSTRAINT fk_sys_uitext_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '前台界面文案（站点覆盖与各语言译文）';
```

#### 6.4.9 `sys_user`：后台用户

用户全局唯一，角色按站点分配（系统设计 5.1、ADR-010）。删除为软删除：同时删除其 2FA 与站点角色；`id` 永久有效，其他表中的 `*_by` 引用不受影响；邮箱、姓名按保留期（由法务确认）匿名化。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `email` | VARCHAR(254) | 否 | — | 登录邮箱（去除首尾空白后保存） |
| `email_normalized` | VARCHAR(254) bin（生成列） | 否 | `LOWER(TRIM(email))` | 小写规范化邮箱（登录查找与唯一约束用） |
| `display_name` | VARCHAR(64) | 否 | — | 姓名（后台显示） |
| `password_hash` | VARCHAR(255) bin | 否 | — | Argon2id 哈希（PHC 字符串 `$argon2id$v=19$m=…,t=…,p=…$盐$哈希`） |
| `must_change_password` | TINYINT(1) | 否 | 0 | 下次登录必须修改密码（管理员创建账号或重置密码后为 1） |
| `password_changed_at` | DATETIME(3) | 是 | — | 最近一次修改密码时间 |
| `status` | VARCHAR(16) | 否 | `'ACTIVE'` | 状态：`ACTIVE` / `DISABLED` |
| `system_admin` | TINYINT(1) | 否 | 0 | 系统管理员：管理站点、模块与系统设置，拥有所有站点的全部权限 |
| `failed_login_count` | INT | 否 | 0 | 连续登录失败次数（成功登录后清零） |
| `last_failed_login_at` | DATETIME(3) | 是 | — | 最近一次登录失败时间 |
| `locked_until` | DATETIME(3) | 是 | — | 临时锁定截止时间（连续失败达到阈值后递增；为空或已过期表示未锁定） |
| `last_login_at` | DATETIME(3) | 是 | — | 最近一次成功登录时间 |
| `last_login_ip` | VARCHAR(45) | 是 | — | 最近一次成功登录 IP（30 天后由保留期任务置空，系统设计 26.8） |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |
| `deleted_at` | DATETIME(3) | 是 | — | 软删除时间 |
| `alive` | TINYINT（生成列） | 是 | `IF(deleted_at IS NULL, 1, NULL)` | 未删除标记（唯一索引用） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_user_email (email_normalized, alive)`：未删除用户之间邮箱唯一；登录按规范化邮箱查找

```sql
CREATE TABLE sys_user (
  id                    BIGINT        NOT NULL COMMENT '主键（TSID）',
  email                 VARCHAR(254)  NOT NULL COMMENT '登录邮箱（去除首尾空白后保存）',
  email_normalized      VARCHAR(254)  COLLATE utf8mb4_0900_bin AS (LOWER(TRIM(email))) STORED NOT NULL COMMENT '小写规范化邮箱（生成列，登录查找与唯一约束用）',
  display_name          VARCHAR(64)   NOT NULL COMMENT '姓名（后台显示）',
  password_hash         VARCHAR(255)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '密码哈希：Argon2id PHC 字符串',
  must_change_password  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '下次登录必须修改密码（管理员创建账号或重置密码后为 1）',
  password_changed_at   DATETIME(3)   NULL COMMENT '最近一次修改密码时间',
  status                VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE（正常）/ DISABLED（已停用，不能登录）',
  system_admin          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '系统管理员：管理站点、模块与系统设置，拥有所有站点的全部权限',
  failed_login_count    INT           NOT NULL DEFAULT 0 COMMENT '连续登录失败次数（成功登录后清零）',
  last_failed_login_at  DATETIME(3)   NULL COMMENT '最近一次登录失败时间',
  locked_until          DATETIME(3)   NULL COMMENT '临时锁定截止时间（连续失败达到阈值后递增；为空或已过期表示未锁定）',
  last_login_at         DATETIME(3)   NULL COMMENT '最近一次成功登录时间',
  last_login_ip         VARCHAR(45)   NULL COMMENT '最近一次成功登录 IP（30 天后由保留期任务置空）',
  version               INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at            DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by            BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at            DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by            BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  deleted_at            DATETIME(3)   NULL COMMENT '软删除时间',
  alive                 TINYINT       AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email_normalized, alive)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台用户（全局）';
```

#### 6.4.10 `sys_user_mfa`：用户 2FA

V1 每个用户至多一个 TOTP 因子。重置 2FA = 删除本行（需二次验证并写审计）。恢复码为高熵随机串，以 HMAC-SHA-256（密钥来自环境变量）哈希保存，使用时在 `SELECT … FOR UPDATE` 中校验并从数组移除。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `user_id` | BIGINT | 否 | — | 用户（`sys_user.id`） |
| `method` | VARCHAR(8) | 否 | `'TOTP'` | 方式：`TOTP` |
| `status` | VARCHAR(8) | 否 | `'PENDING'` | 状态：`PENDING`（已生成密钥，待首次验证）/ `ACTIVE` |
| `secret_ciphertext` | VARCHAR(255) bin | 否 | — | TOTP 密钥密文（EncryptedSecret） |
| `recovery_code_hashes` | JSON | 否 | `JSON_ARRAY()` | 未使用恢复码的哈希数组（十六进制）；使用后移除 |
| `recovery_codes_generated_at` | DATETIME(3) | 是 | — | 恢复码生成时间 |
| `last_used_step` | BIGINT | 是 | — | 最近一次通过验证的 TOTP 时间步（拒绝同一时间步重放） |
| `last_used_at` | DATETIME(3) | 是 | — | 最近一次通过 2FA 验证的时间 |
| `confirmed_at` | DATETIME(3) | 是 | — | 首次验证通过（启用）时间 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_mfa_user (user_id)`：每个用户一行；登录时按用户读取
- `fk_sys_mfa_user`：→ `sys_user.id`，`ON DELETE CASCADE`

```sql
CREATE TABLE sys_user_mfa (
  id                           BIGINT        NOT NULL COMMENT '主键（TSID）',
  user_id                      BIGINT        NOT NULL COMMENT '用户（sys_user.id）',
  method                       VARCHAR(8)    NOT NULL DEFAULT 'TOTP' COMMENT '方式：TOTP（V1 只有 TOTP）',
  status                       VARCHAR(8)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING（已生成密钥，待首次验证）/ ACTIVE（已启用）',
  secret_ciphertext            VARCHAR(255)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT 'TOTP 密钥密文（AES-256-GCM 加密信封 v1:{keyId}:{Base64}）',
  recovery_code_hashes         JSON          NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '未使用恢复码的哈希数组（HMAC-SHA-256 十六进制）；使用后移除',
  recovery_codes_generated_at  DATETIME(3)   NULL COMMENT '恢复码生成时间',
  last_used_step               BIGINT        NULL COMMENT '最近一次通过验证的 TOTP 时间步（拒绝同一时间步重放）',
  last_used_at                 DATETIME(3)   NULL COMMENT '最近一次通过 2FA 验证的时间',
  confirmed_at                 DATETIME(3)   NULL COMMENT '首次验证通过（启用）时间',
  version                      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at                   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by                   BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at                   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by                   BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mfa_user (user_id),
  CONSTRAINT fk_sys_mfa_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户 2FA（全局）';
```

#### 6.4.11 `sys_role`：角色

角色定义全局共享（内置角色对应系统设计 1.2，也支持自定义角色），按站点分配给用户。删除自定义角色前必须没有站点角色分配。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `code` | VARCHAR(64) bin | 否 | — | 角色代码（如 `site_admin`、`editor`、`reviewer`、`seo`、`translator`、`sales`、`sales_manager`、`developer`） |
| `name` | VARCHAR(64) | 否 | — | 角色名称（中文） |
| `description` | VARCHAR(500) | 是 | — | 说明 |
| `builtin` | TINYINT(1) | 否 | 0 | 内置角色：不可删除、不可修改 `code` |
| `mfa_required` | TINYINT(1) | 否 | 0 | 强制 2FA（由权限推导，见 6.1 不变量 10） |
| `version` | INT | 否 | 0 | 乐观锁（角色权限整体替换时同样递增） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |
| `deleted_at` | DATETIME(3) | 是 | — | 软删除时间 |
| `alive` | TINYINT（生成列） | 是 | `IF(deleted_at IS NULL, 1, NULL)` | 未删除标记（唯一索引用） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_role_code (code, alive)`：未删除角色之间代码唯一

```sql
CREATE TABLE sys_role (
  id            BIGINT        NOT NULL COMMENT '主键（TSID）',
  code          VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '角色代码（如 site_admin、editor、sales）',
  name          VARCHAR(64)   NOT NULL COMMENT '角色名称（中文）',
  description   VARCHAR(500)  NULL COMMENT '说明',
  builtin       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '内置角色：不可删除、不可修改 code',
  mfa_required  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '强制 2FA：包含高危、发布、管理或开发者类权限时为 1（由应用重算）',
  version       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（角色权限整体替换时同样递增）',
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  deleted_at    DATETIME(3)   NULL COMMENT '软删除时间',
  alive         TINYINT       AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code, alive)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色（全局定义，按站点分配）';
```

#### 6.4.12 `sys_permission`：权限点

启动时由各模块 module.json 的 `permissions` 幂等同步（系统设计 3.5、3.6）；权限码格式 `{module}:{resource}:{action}`（系统设计 26.2）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `code` | VARCHAR(128) bin | 否 | — | 权限码，如 `inquiry:inquiry:export`、`content:product:publish` |
| `module_id` | VARCHAR(32) bin | 否 | — | 声明该权限的模块 |
| `name` | VARCHAR(100) | 否 | — | 权限名称（中文；module.json 未声明时同步为 `code`） |
| `is_sensitive` | TINYINT(1) | 否 | 0 | 高危权限（module.json 的 `sensitive`）：操作需二次验证并强审计，持有角色强制 2FA |
| `data_scopes` | JSON | 是 | — | 支持的数据范围（module.json 的 `dataScopes`，如 `["ALL","GROUP","OWN"]`）；为空表示不区分 |
| `locale_scoped` | TINYINT(1) | 否 | 0 | 受语言范围限制（module.json 的 `localeScoped`，如翻译审校权限），按 `sys_user_site_role.locales` 过滤 |
| `removed_at` | DATETIME(3) | 是 | — | 同步时发现已不在 module.json 中（或模块 `MISSING`）的时间；非空时不参与鉴权，重新出现时清空 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_perm_code (code)`：权限码唯一；`@RequiresPermission` 鉴权按码匹配（结果缓存）
- `idx_perm_module (module_id)`：按模块同步与列出权限；外键索引
- `fk_sys_perm_module`：→ `sys_module.module_id`（RESTRICT）

```sql
CREATE TABLE sys_permission (
  id             BIGINT        NOT NULL COMMENT '主键（TSID）',
  code           VARCHAR(128)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '权限码 {module}:{resource}:{action}，如 inquiry:inquiry:export',
  module_id      VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '声明该权限的模块',
  name           VARCHAR(100)  NOT NULL COMMENT '权限名称（中文；module.json 未声明时同步为 code）',
  is_sensitive   TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '高危权限（module.json 的 sensitive）：操作需二次验证并强审计，持有角色强制 2FA',
  data_scopes    JSON          NULL COMMENT '支持的数据范围（module.json 的 dataScopes，如 ["ALL","GROUP","OWN"]）；为空表示不区分数据范围',
  locale_scoped  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '受语言范围限制（module.json 的 localeScoped，如翻译审校权限）',
  removed_at     DATETIME(3)   NULL COMMENT '已从 module.json 移除（或模块 MISSING）的时间；非空时不参与鉴权',
  created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (code),
  KEY idx_perm_module (module_id),
  CONSTRAINT fk_sys_perm_module FOREIGN KEY (module_id) REFERENCES sys_module (module_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限点（由 module.json 同步，全局）';
```

#### 6.4.13 `sys_role_permission`：角色权限

纯关联表（复合主键）。角色权限按角色整体替换（删除后重新插入），因此没有 `updated_*`。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `role_id` | BIGINT | 否 | — | 角色（`sys_role.id`） |
| `permission_id` | BIGINT | 否 | — | 权限（`sys_permission.id`） |
| `data_scope` | VARCHAR(8) | 是 | — | 数据范围：`ALL` / `GROUP` / `OWN`；权限不区分数据范围时为空 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 授予时间 |
| `created_by` | BIGINT | 否 | 0 | 授予人 |

索引与约束：
- `PRIMARY KEY (role_id, permission_id)`：同一权限只授予一次；按角色读取权限集合
- `idx_rp_permission (permission_id)`：查询“哪些角色拥有某权限”（重算 `mfa_required`、权限移除影响分析）；外键索引
- `fk_sys_roleperm_role`：→ `sys_role.id`，`ON DELETE CASCADE`
- `fk_sys_roleperm_perm`：→ `sys_permission.id`（RESTRICT：清除模块时先删除授予）

```sql
CREATE TABLE sys_role_permission (
  role_id        BIGINT       NOT NULL COMMENT '角色（sys_role.id）',
  permission_id  BIGINT       NOT NULL COMMENT '权限（sys_permission.id）',
  data_scope     VARCHAR(8)   NULL COMMENT '数据范围：ALL / GROUP / OWN；权限不区分数据范围时为空',
  created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '授予时间',
  created_by     BIGINT       NOT NULL DEFAULT 0 COMMENT '授予人（0 = 系统）',
  PRIMARY KEY (role_id, permission_id),
  KEY idx_rp_permission (permission_id),
  CONSTRAINT fk_sys_roleperm_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
  CONSTRAINT fk_sys_roleperm_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限';
```

#### 6.4.14 `sys_user_site_role`：用户站点角色

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `user_id` | BIGINT | 否 | — | 用户（`sys_user.id`） |
| `role_id` | BIGINT | 否 | — | 角色（`sys_role.id`） |
| `locales` | JSON | 是 | — | 语言范围：该角色在本站点可操作的目标语言（如 `["en","de"]`）；为空表示全部语言；只对 `locale_scoped = 1` 的权限生效（系统设计 5.8、26.2） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 分配人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_usr_assign (site_id, user_id, role_id)`：同一站点同一角色只分配一次；站点内“用户列表及其角色”
- `idx_usr_user (user_id)`：登录与切换站点时读取用户在各站点的角色（专用豁免查询）；外键索引
- `idx_usr_role (role_id)`：删除角色前检查分配、重算 `mfa_required` 后定位受影响用户；外键索引
- `fk_sys_usr_site`：→ `sys_site.id`，`ON DELETE CASCADE`
- `fk_sys_usr_user`：→ `sys_user.id`，`ON DELETE CASCADE`
- `fk_sys_usr_role`：→ `sys_role.id`（RESTRICT）

```sql
CREATE TABLE sys_user_site_role (
  id          BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id     BIGINT       NOT NULL COMMENT '站点',
  user_id     BIGINT       NOT NULL COMMENT '用户（sys_user.id）',
  role_id     BIGINT       NOT NULL COMMENT '角色（sys_role.id）',
  locales     JSON         NULL COMMENT '语言范围：该角色在本站点可操作的目标语言（如 ["en","de"]）；为空表示全部语言；只对 locale_scoped 权限生效',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '分配人（0 = 系统）',
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_usr_assign (site_id, user_id, role_id),
  KEY idx_usr_user (user_id),
  KEY idx_usr_role (role_id),
  CONSTRAINT fk_sys_usr_site FOREIGN KEY (site_id) REFERENCES sys_site (id) ON DELETE CASCADE,
  CONSTRAINT fk_sys_usr_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_sys_usr_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户站点角色';
```

#### 6.4.15 `sys_menu`：后台菜单

启动时由 module.json 的 `menus` 幂等同步；`GET /api/admin/v1/system/bootstrap` 按当前站点的模块启用状态与用户权限过滤后返回（系统设计 22.2）。顶级分组（如 `content`、`sales`、`settings`）由 core 的 module.json 声明。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `menu_key` | VARCHAR(64) bin | 否 | — | 菜单 key（module.json `menus[].key`，全局唯一） |
| `module_id` | VARCHAR(32) bin | 否 | — | 声明该菜单的模块；模块在当前站点停用时隐藏 |
| `parent_key` | VARCHAR(64) bin | 是 | — | 父菜单 key；顶级为空。父菜单不存在时挂到顶级并记录警告（不建自引用外键，避免跨模块同步顺序问题） |
| `name` | VARCHAR(64) | 否 | — | 菜单名称（中文） |
| `route` | VARCHAR(255) bin | 是 | — | 前端路由（如 `/inquiry/inquiries`）；分组节点为空 |
| `permission_code` | VARCHAR(128) bin | 是 | — | 访问所需权限码；为空表示登录即可见 |
| `icon` | VARCHAR(64) | 是 | — | 图标名（Element Plus 图标） |
| `sort_order` | INT | 否 | 0 | 同级排序（module.json 的 `order`），小的在前 |
| `removed_at` | DATETIME(3) | 是 | — | 已从 module.json 移除的时间；非空时不显示 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_menu_key (menu_key)`：菜单 key 唯一；同步按 key upsert
- `idx_menu_module (module_id)`：按模块同步；外键索引
- `fk_sys_menu_module`：→ `sys_module.module_id`（RESTRICT）

```sql
CREATE TABLE sys_menu (
  id               BIGINT        NOT NULL COMMENT '主键（TSID）',
  menu_key         VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '菜单 key（module.json menus[].key，全局唯一）',
  module_id        VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '声明该菜单的模块；模块在当前站点停用时隐藏',
  parent_key       VARCHAR(64)   COLLATE utf8mb4_0900_bin NULL COMMENT '父菜单 key（如 sales、settings）；顶级为空',
  name             VARCHAR(64)   NOT NULL COMMENT '菜单名称（中文）',
  route            VARCHAR(255)  COLLATE utf8mb4_0900_bin NULL COMMENT '前端路由（如 /inquiry/inquiries）；分组节点为空',
  permission_code  VARCHAR(128)  COLLATE utf8mb4_0900_bin NULL COMMENT '访问所需权限码；为空表示登录即可见',
  icon             VARCHAR(64)   NULL COMMENT '图标名（Element Plus 图标）',
  sort_order       INT           NOT NULL DEFAULT 0 COMMENT '同级排序（module.json 的 order），小的在前',
  removed_at       DATETIME(3)   NULL COMMENT '已从 module.json 移除的时间；非空时不显示',
  created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_menu_key (menu_key),
  KEY idx_menu_module (module_id),
  CONSTRAINT fk_sys_menu_module FOREIGN KEY (module_id) REFERENCES sys_module (module_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台菜单（由 module.json 同步，全局）';
```

#### 6.4.16 `sys_setting`：设置

系统级（`site_id = 0`）与站点级共用一张表。键的 Schema 来自声明模块的 `settings.schema.json`；没有行时取 Schema 中的默认值，“恢复默认”即删除该行。典型键：`core.smtp`（系统级，含加密的密码）、`core.theme`（品牌色、字体、圆角、按钮与 Header / Footer 变体，系统设计 11.3；Logo 只存 `sys_site_profile`）、`core.features`（功能开关）、`core.dsr`（数据主体请求的时限规则与工作日历，系统级）、`core.retention`、`inquiry.retention`。修改后发布 `SiteSettingsChangedEvent`。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点；0 表示系统级设置 |
| `module_id` | VARCHAR(32) bin | 否 | — | 声明该设置的模块 |
| `setting_key` | VARCHAR(128) bin | 否 | — | 设置键 `{moduleId}.{名称}`，如 `core.smtp` |
| `setting_value` | JSON | 否 | — | 设置值（JSON 对象，顶层带 `schemaVersion`）；敏感字段以 EncryptedSecret 字符串保存 |
| `encrypted` | TINYINT(1) | 否 | 0 | 含加密字段：1 表示值中有按 Schema（`x-secret`）加密的字段；密钥轮换任务按此查找 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_set_key (site_id, setting_key)`：同一作用域内键唯一；按键读取，以及按模块前缀范围扫描（`setting_key LIKE 'inquiry.%'`）列出某模块的设置
- 不建 `site_id` 外键（系统级行为 0）

```sql
CREATE TABLE sys_setting (
  id             BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id        BIGINT        NOT NULL COMMENT '站点；0 表示系统级设置',
  module_id      VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '声明该设置的模块（settings.schema.json 所属模块）',
  setting_key    VARCHAR(128)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '设置键 {moduleId}.{名称}，如 core.smtp、core.theme、inquiry.retention',
  setting_value  JSON          NOT NULL COMMENT '设置值（JSON 对象，顶层带 schemaVersion）；敏感字段以加密信封字符串保存',
  encrypted      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '含加密字段：1 表示值中有按 Schema（x-secret）加密的字段；密钥轮换任务按此查找',
  version        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by     BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by     BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_set_key (site_id, setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设置（系统级 site_id = 0 与站点级共用）';
```

#### 6.4.17 `sys_dict_type`：字典类型

字典是全局的、只用于后台的可配置选项（中文标签，如询盘关闭原因、跟进方式）；业务表保存字典值（代码），不保存标签。内置类型由声明模块在 `onInstall` / `onUpgrade` 中写入。前台可见的选项不使用字典（走文案键或站点资源），因此字典不翻译。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `type_code` | VARCHAR(64) bin | 否 | — | 字典类型代码 `{moduleId}.{名称}`，如 `inquiry.close_reason` |
| `module_id` | VARCHAR(32) bin | 否 | — | 所属模块（自定义字典为 `core`） |
| `name` | VARCHAR(64) | 否 | — | 字典名称（中文） |
| `description` | VARCHAR(500) | 是 | — | 说明 |
| `builtin` | TINYINT(1) | 否 | 0 | 内置类型：不可删除 |
| `items_editable` | TINYINT(1) | 否 | 1 | 允许在后台增删改字典项；0 表示代码枚举的只读镜像 |
| `version` | INT | 否 | 0 | 乐观锁（字典项变更时同样递增） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_dtype_code (type_code)`：类型代码唯一；按代码加载字典（应用内缓存）
- `idx_dtype_module (module_id)`：按模块同步与清除；外键索引
- `fk_sys_dtype_module`：→ `sys_module.module_id`（RESTRICT）

```sql
CREATE TABLE sys_dict_type (
  id              BIGINT        NOT NULL COMMENT '主键（TSID）',
  type_code       VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '字典类型代码 {moduleId}.{名称}，如 inquiry.close_reason',
  module_id       VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '所属模块（自定义字典为 core）',
  name            VARCHAR(64)   NOT NULL COMMENT '字典名称（中文）',
  description     VARCHAR(500)  NULL COMMENT '说明',
  builtin         TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '内置类型（模块在 onInstall / onUpgrade 中写入）：不可删除',
  items_editable  TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '允许在后台增删改字典项；0 表示代码枚举的只读镜像',
  version         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（字典项变更时同样递增）',
  created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by      BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by      BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dtype_code (type_code),
  KEY idx_dtype_module (module_id),
  CONSTRAINT fk_sys_dtype_module FOREIGN KEY (module_id) REFERENCES sys_module (module_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型（全局，只用于后台）';
```

#### 6.4.18 `sys_dict_item`：字典项

字典值创建后不可修改；不再使用的项应停用而不是删除（历史数据仍按其标签显示）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `dict_type_id` | BIGINT | 否 | — | 字典类型（`sys_dict_type.id`） |
| `item_value` | VARCHAR(64) bin | 否 | — | 字典值（业务表中保存的代码，如 `NO_RESPONSE`） |
| `label` | VARCHAR(100) | 否 | — | 显示名称（中文） |
| `sort_order` | INT | 否 | 0 | 排序，小的在前 |
| `enabled` | TINYINT(1) | 否 | 1 | 启用；停用的项不再可选 |
| `builtin` | TINYINT(1) | 否 | 0 | 内置项：不可删除 |
| `extra` | JSON | 是 | — | 扩展属性（如标签颜色） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_ditem_value (dict_type_id, item_value)`：类型内字典值唯一；按类型读取全部项
- `fk_sys_ditem_type`：→ `sys_dict_type.id`，`ON DELETE CASCADE`

```sql
CREATE TABLE sys_dict_item (
  id            BIGINT        NOT NULL COMMENT '主键（TSID）',
  dict_type_id  BIGINT        NOT NULL COMMENT '字典类型（sys_dict_type.id）',
  item_value    VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '字典值（业务表中保存的代码，如 NO_RESPONSE）；创建后不可修改',
  label         VARCHAR(100)  NOT NULL COMMENT '显示名称（中文）',
  sort_order    INT           NOT NULL DEFAULT 0 COMMENT '排序，小的在前',
  enabled       TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启用；停用的项不再可选，历史数据仍按其标签显示',
  builtin       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '内置项：不可删除',
  extra         JSON          NULL COMMENT '扩展属性（如标签颜色）',
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ditem_value (dict_type_id, item_value),
  CONSTRAINT fk_sys_ditem_type FOREIGN KEY (dict_type_id) REFERENCES sys_dict_type (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典项（全局，只用于后台）';
```

#### 6.4.19 `sys_audit_log`：审计日志

只追加（系统设计 26.5）：覆盖所有后台写操作、登录登出与失败、角色与权限变更、数据导出、脚本修改、模块启停、设置修改；默认保留 1 年（`core.retention` 可配置）。不建外键（写入路径最短，引用对象可能已删除）。`ip` 保存完整 IP，随审计日志一起保留（后台安全审计）；是否需要提前截断由法务确认（系统设计 26.8、32 章 Q13）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID，时间有序） |
| `site_id` | BIGINT | 是 | — | 站点；系统级操作（登录、站点与模块管理、数据主体请求等）为空 |
| `user_id` | BIGINT | 否 | 0 | 操作人（`sys_user.id`；0 = 系统） |
| `user_name` | VARCHAR(64) | 是 | — | 操作人姓名快照；登录失败且账号不存在时为尝试的邮箱 |
| `module_id` | VARCHAR(32) bin | 否 | — | 模块 |
| `action` | VARCHAR(64) bin | 否 | — | 动作 `{资源}.{动作}`，如 `user.login`、`user.login_failed`、`localization.publish`、`inquiry.export` |
| `target_type` | VARCHAR(64) bin | 是 | — | 目标类型（如 `sys_user`、`PRODUCT`、`setting`） |
| `target_id` | VARCHAR(64) bin | 是 | — | 目标 ID（TSID 的十进制字符串，或设置键、模块 ID 等） |
| `summary` | VARCHAR(500) | 否 | — | 摘要（中文） |
| `diff` | JSON | 是 | — | 变更差异 `{"字段":{"from":…,"to":…}}`；密码、密钥、恢复码等敏感字段脱敏 |
| `result` | VARCHAR(8) | 否 | `'SUCCESS'` | 结果：`SUCCESS` / `FAILURE` |
| `is_sensitive` | TINYINT(1) | 否 | 0 | 高危操作（导出、脚本修改、权限变更、数据主体请求等） |
| `ip` | VARCHAR(45) | 是 | — | 客户端 IP |
| `user_agent` | VARCHAR(500) | 是 | — | User-Agent（截断到 500 个字符） |
| `trace_id` | VARCHAR(32) bin | 是 | — | 链路追踪 ID（与应用日志关联） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 发生时间（UTC） |

索引与约束：
- `PRIMARY KEY (id)`
- `idx_audit_site_time (site_id, created_at)`：站点内按时间倒序浏览（后台审计页默认查询）；`site_id IS NULL` 查系统级记录
- `idx_audit_user_time (user_id, created_at)`：按操作人筛选
- `idx_audit_target (target_type, target_id, created_at)`：某对象的操作历史
- `idx_audit_time (created_at)`：系统管理员跨站点按时间浏览；保留期清理按时间批量删除

```sql
CREATE TABLE sys_audit_log (
  id            BIGINT        NOT NULL COMMENT '主键（TSID，时间有序）',
  site_id       BIGINT        NULL COMMENT '站点；系统级操作（登录、站点与模块管理、数据主体请求等）为空',
  user_id       BIGINT        NOT NULL DEFAULT 0 COMMENT '操作人（sys_user.id；0 = 系统）',
  user_name     VARCHAR(64)   NULL COMMENT '操作人姓名快照；登录失败且账号不存在时为尝试的邮箱',
  module_id     VARCHAR(32)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '模块',
  action        VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '动作 {资源}.{动作}，如 user.login、user.login_failed、localization.publish、inquiry.export',
  target_type   VARCHAR(64)   COLLATE utf8mb4_0900_bin NULL COMMENT '目标类型（如 sys_user、PRODUCT、setting）',
  target_id     VARCHAR(64)   COLLATE utf8mb4_0900_bin NULL COMMENT '目标 ID（TSID 的十进制字符串，或设置键、模块 ID 等）',
  summary       VARCHAR(500)  NOT NULL COMMENT '摘要（中文）',
  diff          JSON          NULL COMMENT '变更差异 {"字段":{"from":…,"to":…}}；密码、密钥、恢复码等敏感字段脱敏',
  result        VARCHAR(8)    NOT NULL DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS / FAILURE',
  is_sensitive  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '高危操作（导出、脚本修改、权限变更、数据主体请求等）',
  ip            VARCHAR(45)   NULL COMMENT '客户端 IP',
  user_agent    VARCHAR(500)  NULL COMMENT 'User-Agent（截断到 500 个字符）',
  trace_id      VARCHAR(32)   COLLATE utf8mb4_0900_bin NULL COMMENT '链路追踪 ID（与应用日志关联）',
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间（UTC）',
  PRIMARY KEY (id),
  KEY idx_audit_site_time (site_id, created_at),
  KEY idx_audit_user_time (user_id, created_at),
  KEY idx_audit_target (target_type, target_id, created_at),
  KEY idx_audit_time (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审计日志（只追加）';
```

#### 6.4.20 `sys_job`：通用异步任务

Core 提供的持久化异步任务框架（系统设计 2.4）：产品导入导出、重定向导入、询盘导出、数据主体请求导出、保留期清理等。执行器以 `UPDATE … SET status = 'RUNNING', worker_id = ?, heartbeat_at = ? WHERE id = ? AND status = 'PENDING'` 认领任务；心跳超时的 `RUNNING` 任务视为实例失联，按 `attempts < max_attempts` 重新排队或置为 `FAILED`。导入分两阶段：“校验预览”任务成功后，用户确认再创建“执行”任务（`parent_job_id` 指向前者）。结果文件放私有桶，默认 7 天过期；任务记录默认保留 180 天（`core.retention`）。AI 翻译任务使用 translation 自己的 `trn_job`，不使用本表。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点；0 表示系统级任务 |
| `module_id` | VARCHAR(32) bin | 否 | — | 发起任务的模块 |
| `job_type` | VARCHAR(64) bin | 否 | — | 任务类型 `{moduleId}.{名称}`，如 `product.import`、`url.redirect_import`、`inquiry.export`、`core.dsr_export` |
| `title` | VARCHAR(200) | 否 | — | 任务名称（中文，列表显示） |
| `status` | VARCHAR(16) | 否 | `'PENDING'` | 状态（共享枚举）：`PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELED` |
| `parent_job_id` | BIGINT | 是 | — | 前一阶段任务（`sys_job.id`） |
| `idempotency_key` | VARCHAR(64) bin | 是 | — | 幂等键（请求头 `Idempotency-Key`）；在任务保留期内唯一，比 24 小时更严格（系统设计 24.2） |
| `params` | JSON | 是 | — | 任务参数（含输入文件的对象键等） |
| `progress_total` | INT | 是 | — | 总量（如总行数；未知时为空） |
| `progress_done` | INT | 否 | 0 | 已处理数量 |
| `progress_failed` | INT | 否 | 0 | 处理失败数量 |
| `progress_message` | VARCHAR(300) | 是 | — | 当前阶段说明（中文） |
| `result` | JSON | 是 | — | 结果摘要（如 `{"created":120,"updated":30,"errors":2}`） |
| `result_file_key` | VARCHAR(500) bin | 是 | — | 结果文件在私有桶中的对象键（`{siteId}/jobs/{jobId}/…`；系统级为 `sys/jobs/{jobId}/…`） |
| `result_file_name` | VARCHAR(255) | 是 | — | 结果文件下载名 |
| `result_file_size` | BIGINT | 是 | — | 结果文件字节数 |
| `result_expires_at` | DATETIME(3) | 是 | — | 结果文件过期时间（到期由清理任务删除文件并置空对象键） |
| `error_code` | VARCHAR(64) | 是 | — | 失败错误码（`{MODULE}_{REASON}`） |
| `error_message` | VARCHAR(1000) | 是 | — | 失败说明（中文） |
| `attempts` | INT | 否 | 0 | 已执行次数 |
| `max_attempts` | INT | 否 | 1 | 最多执行次数（默认不自动重试） |
| `cancel_requested` | TINYINT(1) | 否 | 0 | 已请求取消：执行中的任务在检查点协作退出 |
| `scheduled_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 最早执行时间 |
| `worker_id` | VARCHAR(64) | 是 | — | 执行实例标识（主机名 + 进程） |
| `heartbeat_at` | DATETIME(3) | 是 | — | 执行实例最近心跳 |
| `started_at` | DATETIME(3) | 是 | — | 开始执行时间 |
| `finished_at` | DATETIME(3) | 是 | — | 结束时间 |
| `version` | INT | 否 | 0 | 乐观锁（状态流转） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 发起人（完成后通知此人） |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_job_idem (site_id, job_type, idempotency_key)`：幂等键唯一（NULL 不参与比较）
- `idx_job_poll (status, scheduled_at)`：执行器轮询 `status = 'PENDING' AND scheduled_at <= now()`（`FOR UPDATE SKIP LOCKED`），以及查找心跳超时的 `RUNNING` 任务（专用豁免查询）
- `idx_job_list (site_id, created_at)`：站点任务中心按时间倒序列表（按类型、发起人筛选在此范围内过滤）
- `idx_job_parent (parent_job_id)`：查找后续阶段任务；外键索引
- `idx_job_expire (result_expires_at)`：清理过期结果文件
- `fk_sys_job_parent`：→ `sys_job.id`，`ON DELETE SET NULL`

```sql
CREATE TABLE sys_job (
  id                 BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id            BIGINT         NOT NULL COMMENT '站点；0 表示系统级任务（如数据主体请求导出、保留期清理）',
  module_id          VARCHAR(32)    COLLATE utf8mb4_0900_bin NOT NULL COMMENT '发起任务的模块',
  job_type           VARCHAR(64)    COLLATE utf8mb4_0900_bin NOT NULL COMMENT '任务类型 {moduleId}.{名称}，如 product.import、url.redirect_import、inquiry.export、core.dsr_export',
  title              VARCHAR(200)   NOT NULL COMMENT '任务名称（中文，列表显示）',
  status             VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED',
  parent_job_id      BIGINT         NULL COMMENT '前一阶段任务（如导入的执行任务指向其校验预览任务）',
  idempotency_key    VARCHAR(64)    COLLATE utf8mb4_0900_bin NULL COMMENT '幂等键（请求头 Idempotency-Key；同一站点、同一任务类型内唯一）',
  params             JSON           NULL COMMENT '任务参数（含输入文件的对象键等）',
  progress_total     INT            NULL COMMENT '总量（如总行数；未知时为空）',
  progress_done      INT            NOT NULL DEFAULT 0 COMMENT '已处理数量',
  progress_failed    INT            NOT NULL DEFAULT 0 COMMENT '处理失败数量',
  progress_message   VARCHAR(300)   NULL COMMENT '当前阶段说明（中文）',
  result             JSON           NULL COMMENT '结果摘要（如 {"created":120,"updated":30,"errors":2}）',
  result_file_key    VARCHAR(500)   COLLATE utf8mb4_0900_bin NULL COMMENT '结果文件在私有桶中的对象键（{siteId}/jobs/{jobId}/…；系统级为 sys/jobs/{jobId}/…）',
  result_file_name   VARCHAR(255)   NULL COMMENT '结果文件下载名',
  result_file_size   BIGINT         NULL COMMENT '结果文件字节数',
  result_expires_at  DATETIME(3)    NULL COMMENT '结果文件过期时间（到期由清理任务删除文件并置空对象键）',
  error_code         VARCHAR(64)    NULL COMMENT '失败错误码（{MODULE}_{REASON}）',
  error_message      VARCHAR(1000)  NULL COMMENT '失败说明（中文）',
  attempts           INT            NOT NULL DEFAULT 0 COMMENT '已执行次数',
  max_attempts       INT            NOT NULL DEFAULT 1 COMMENT '最多执行次数（默认不自动重试）',
  cancel_requested   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '已请求取消：执行中的任务在检查点协作退出',
  scheduled_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最早执行时间',
  worker_id          VARCHAR(64)    NULL COMMENT '执行实例标识（主机名 + 进程）',
  heartbeat_at       DATETIME(3)    NULL COMMENT '执行实例最近心跳；超时视为实例失联',
  started_at         DATETIME(3)    NULL COMMENT '开始执行时间',
  finished_at        DATETIME(3)    NULL COMMENT '结束时间（SUCCEEDED / FAILED / CANCELED）',
  version            INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（状态流转）',
  created_at         DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by         BIGINT         NOT NULL DEFAULT 0 COMMENT '发起人（0 = 系统；完成后通知此人）',
  updated_at         DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by         BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_idem (site_id, job_type, idempotency_key),
  KEY idx_job_poll (status, scheduled_at),
  KEY idx_job_list (site_id, created_at),
  KEY idx_job_parent (parent_job_id),
  KEY idx_job_expire (result_expires_at),
  CONSTRAINT fk_sys_job_parent FOREIGN KEY (parent_job_id) REFERENCES sys_job (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通用异步任务';
```

#### 6.4.21 `sys_lock`：跨实例互斥锁

结构与 ShedLock JDBC 提供者兼容（配置表名 `sys_lock`、列名 `lock_name` / `lock_until` / `locked_at` / `locked_by`，使用数据库时间），用于定时任务锁（Spring Scheduling + ShedLock，系统设计 2.4），以及需要跨实例互斥、带过期时间的业务锁（如模块启停、站点批量操作）。主键为自然键 `lock_name`（ShedLock 插入时不提供 `id`）。模块迁移锁不使用本表，改用 MySQL 命名锁（见 6.1“补充与取舍”）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `lock_name` | VARCHAR(64) bin | 否 | — | 锁名（定时任务名，或 `{moduleId}:{用途}[:s{siteId}]`） |
| `lock_until` | DATETIME(3) | 否 | — | 锁过期时间（UTC）；早于当前时间即视为已释放 |
| `locked_at` | DATETIME(3) | 否 | — | 加锁时间 |
| `locked_by` | VARCHAR(255) | 否 | — | 持有者（实例标识） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 首次创建时间 |

索引与约束：
- `PRIMARY KEY (lock_name)`：按锁名加锁（`INSERT` 或 `UPDATE … WHERE lock_name = ? AND lock_until <= now()`）

```sql
CREATE TABLE sys_lock (
  lock_name   VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '锁名（定时任务名，或 {moduleId}:{用途}[:s{siteId}]）',
  lock_until  DATETIME(3)   NOT NULL COMMENT '锁过期时间（UTC）；早于当前时间即视为已释放',
  locked_at   DATETIME(3)   NOT NULL COMMENT '加锁时间',
  locked_by   VARCHAR(255)  NOT NULL COMMENT '持有者（实例标识）',
  created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次创建时间',
  PRIMARY KEY (lock_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '跨实例互斥锁（ShedLock 兼容）';
```

#### 6.4.22 `sys_dsr_request`：数据主体请求

系统级（跨站点），需要 `system:dsr:manage` 权限（系统设计 17.11、26.8、ADR-016）。处理时通过 `PersonalDataProvider` 按邮箱在全部站点查找、导出、更正、匿名化各模块保存的个人数据。时限（以法务确认为准，规则保存在系统级设置 `core.dsr`）：

| 法域 / 类型 | `ack_due_at` | `due_at`（初始） | 可延期 |
|---|---|---|---|
| GDPR / UK_GDPR | 空 | `received_at` + 1 个月 | 再延长至多 2 个月，须在原期限内告知理由 |
| CCPA：访问、删除、更正等 | `received_at` + 10 个工作日 | `received_at` + 45 个日历日 | 再延长至多 45 天，须在原期限内告知 |
| CCPA：`OPT_OUT_SALE_SHARE` | 空 | `received_at` + 15 个工作日 | 否 |

完成的 `ERASE` 请求即“删除台账”：从备份恢复后，按 `completed_at` 顺序对恢复点之后完成的请求重新执行匿名化。本表本身也含个人数据，保留期由法务确认；`ERASE` 记录至少保留到最后一份可能含被删除数据的备份过期之后（月度快照保留 12 个月，系统设计 27.5）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `request_no` | VARCHAR(32) bin | 否 | — | 受理编号（如 `DSR-20261007-001`），用于与请求人往来 |
| `type` | VARCHAR(24) | 否 | — | 请求类型，见 6.5 |
| `jurisdiction` | VARCHAR(8) | 否 | — | 适用法域：`GDPR` / `UK_GDPR` / `CCPA` |
| `status` | VARCHAR(16) | 否 | `'RECEIVED'` | 状态，见 6.5 |
| `channel` | VARCHAR(16) | 否 | `'EMAIL'` | 受理渠道，见 6.5 |
| `received_site_id` | BIGINT | 是 | — | 受理来源站点（只决定答复的品牌与联系方式；处理范围始终是全部站点） |
| `subject_email` | VARCHAR(254) | 否 | — | 数据主体邮箱（按此跨站点查找个人数据） |
| `subject_email_normalized` | VARCHAR(254) bin（生成列） | 否 | `LOWER(TRIM(subject_email))` | 小写规范化邮箱（查找用） |
| `subject_name` | VARCHAR(200) | 是 | — | 数据主体姓名 |
| `authorized_agent` | TINYINT(1) | 否 | 0 | 由授权代理人代为提交（CCPA）：需本人书面授权并核验本人身份 |
| `agent_name` | VARCHAR(200) | 是 | — | 授权代理人名称 |
| `agent_email` | VARCHAR(254) | 是 | — | 授权代理人邮箱 |
| `request_detail` | TEXT | 是 | — | 请求内容原文（如需更正的字段与新值、反对的处理活动） |
| `received_at` | DATETIME(3) | 否 | — | 收到请求的时间（时限起算点） |
| `ack_due_at` | DATETIME(3) | 是 | — | 确认收到的期限（CCPA） |
| `acknowledged_at` | DATETIME(3) | 是 | — | 已向请求人确认收到的时间 |
| `due_at` | DATETIME(3) | 否 | — | 当前答复期限：受理时计算，延期后等于 `extended_until`；提醒任务只看此字段 |
| `extended_until` | DATETIME(3) | 是 | — | 延期后的期限；未延期为空 |
| `extension_reason` | VARCHAR(1000) | 是 | — | 延期理由 |
| `extension_notified_at` | DATETIME(3) | 是 | — | 已告知请求人延期及理由的时间（须早于原期限） |
| `verification_method` | VARCHAR(16) | 是 | — | 核验方式，见 6.5 |
| `verification_token_hash` | CHAR(64) bin | 是 | — | 一次性确认链接令牌的 SHA-256（明文只出现在邮件中） |
| `verification_sent_at` | DATETIME(3) | 是 | — | 确认链接发送时间 |
| `verification_expires_at` | DATETIME(3) | 是 | — | 确认链接过期时间 |
| `verified_at` | DATETIME(3) | 是 | — | 核验通过时间；为空时不得导出、不得删除 |
| `handled_by` | BIGINT | 是 | — | 处理人（`sys_user.id`）；未分配为空 |
| `export_job_id` | BIGINT | 是 | — | 导出任务（`sys_job.id`；`ACCESS` / `PORTABILITY`） |
| `result` | JSON | 是 | — | 各模块处理结果（`PersonalDataProvider` 返回）`[{"module":"inquiry","found":3,"exported":3,"rectified":0,"anonymized":0}]` |
| `outcome_note` | VARCHAR(1000) | 是 | — | 处理说明（拒绝或无法核验的原因、答复方式等） |
| `completed_at` | DATETIME(3) | 是 | — | 处理完成时间 |
| `replied_at` | DATETIME(3) | 是 | — | 答复请求人的时间 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 登记人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_dsr_no (request_no)`：受理编号唯一
- `idx_dsr_email (subject_email_normalized, received_at)`：按邮箱查找历史请求（重复请求识别、删除台账重放）
- `idx_dsr_due (status, due_at)`：提醒任务查找临近或超过期限的未完成请求；`ack_due_at` 的提醒在同一批未完成请求中过滤
- `idx_dsr_received (received_at)`：后台按受理时间列表
- `idx_dsr_job (export_job_id)`：外键索引
- `fk_sys_dsr_job`：→ `sys_job.id`，`ON DELETE SET NULL`（任务记录到期清理后保留请求本身）

```sql
CREATE TABLE sys_dsr_request (
  id                        BIGINT         NOT NULL COMMENT '主键（TSID）',
  request_no                VARCHAR(32)    COLLATE utf8mb4_0900_bin NOT NULL COMMENT '受理编号（如 DSR-20261007-001），用于与请求人往来',
  type                      VARCHAR(24)    NOT NULL COMMENT '请求类型：ACCESS / RECTIFY / ERASE / PORTABILITY / OBJECT / RESTRICT / OPT_OUT_SALE_SHARE',
  jurisdiction              VARCHAR(8)     NOT NULL COMMENT '适用法域：GDPR / UK_GDPR / CCPA',
  status                    VARCHAR(16)    NOT NULL DEFAULT 'RECEIVED' COMMENT '状态：RECEIVED / VERIFYING / PROCESSING / COMPLETED / REJECTED / WITHDRAWN',
  channel                   VARCHAR(16)    NOT NULL DEFAULT 'EMAIL' COMMENT '受理渠道：EMAIL / WEB_FORM / PHONE / POST / OTHER',
  received_site_id          BIGINT         NULL COMMENT '受理来源站点（只决定答复的品牌与联系方式；处理范围始终是全部站点）',
  subject_email             VARCHAR(254)   NOT NULL COMMENT '数据主体邮箱（按此跨站点查找个人数据）',
  subject_email_normalized  VARCHAR(254)   COLLATE utf8mb4_0900_bin AS (LOWER(TRIM(subject_email))) STORED NOT NULL COMMENT '小写规范化邮箱（生成列，查找用）',
  subject_name              VARCHAR(200)   NULL COMMENT '数据主体姓名',
  authorized_agent          TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '由授权代理人代为提交（CCPA）：需本人书面授权并核验本人身份',
  agent_name                VARCHAR(200)   NULL COMMENT '授权代理人名称',
  agent_email               VARCHAR(254)   NULL COMMENT '授权代理人邮箱',
  request_detail            TEXT           NULL COMMENT '请求内容原文（如需更正的字段与新值、反对的处理活动）',
  received_at               DATETIME(3)    NOT NULL COMMENT '收到请求的时间（时限起算点）',
  ack_due_at                DATETIME(3)    NULL COMMENT '确认收到的期限（CCPA：10 个工作日；其他为空）',
  acknowledged_at           DATETIME(3)    NULL COMMENT '已向请求人确认收到的时间',
  due_at                    DATETIME(3)    NOT NULL COMMENT '当前答复期限：受理时按法域与类型计算，延期后等于 extended_until',
  extended_until            DATETIME(3)    NULL COMMENT '延期后的期限；未延期为空',
  extension_reason          VARCHAR(1000)  NULL COMMENT '延期理由',
  extension_notified_at     DATETIME(3)    NULL COMMENT '已告知请求人延期及理由的时间（须早于原期限）',
  verification_method       VARCHAR(16)    NULL COMMENT '核验方式：EMAIL_LINK（向记录中的邮箱发送一次性确认链接）/ MANUAL（人工核验）/ NOT_REQUIRED（无需核验）',
  verification_token_hash   CHAR(64)       COLLATE utf8mb4_0900_bin NULL COMMENT '一次性确认链接令牌的 SHA-256（明文只出现在邮件中）',
  verification_sent_at      DATETIME(3)    NULL COMMENT '确认链接发送时间',
  verification_expires_at   DATETIME(3)    NULL COMMENT '确认链接过期时间',
  verified_at               DATETIME(3)    NULL COMMENT '核验通过时间；为空时不得导出、不得删除',
  handled_by                BIGINT         NULL COMMENT '处理人（sys_user.id）；未分配为空',
  export_job_id             BIGINT         NULL COMMENT '导出任务（sys_job.id；ACCESS / PORTABILITY）',
  result                    JSON           NULL COMMENT '各模块处理结果（PersonalDataProvider 返回）：[{"module":"inquiry","found":3,"exported":3,"rectified":0,"anonymized":0}]',
  outcome_note              VARCHAR(1000)  NULL COMMENT '处理说明（拒绝或无法核验的原因、答复方式等）',
  completed_at              DATETIME(3)    NULL COMMENT '处理完成时间',
  replied_at                DATETIME(3)    NULL COMMENT '答复请求人的时间',
  version                   INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at                DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by                BIGINT         NOT NULL DEFAULT 0 COMMENT '登记人（0 = 系统）',
  updated_at                DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by                BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dsr_no (request_no),
  KEY idx_dsr_email (subject_email_normalized, received_at),
  KEY idx_dsr_due (status, due_at),
  KEY idx_dsr_received (received_at),
  KEY idx_dsr_job (export_job_id),
  CONSTRAINT fk_sys_dsr_job FOREIGN KEY (export_job_id) REFERENCES sys_job (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据主体请求（系统级，跨站点）';
```

### 6.5 枚举

| 字段 | 取值 | 说明 |
|---|---|---|
| `sys_module.layer` | `L0` / `L2` / `L3` / `L4` | 模块所在层（platform-api 是 L1 契约，无表、不登记） |
| `sys_module.status` | `INSTALLING` | 首次安装中（迁移与 `onInstall` 执行中） |
| | `INSTALLED` | 已安装，版本为 `installed_version` |
| | `UPGRADING` | 升级中（从 `installed_version` 到 `target_version`） |
| | `FAILED` | 安装或升级失败；阻止启动，保留现场（`last_error`） |
| | `MISSING` | 已从构建中移除；数据保留，权限与菜单标记 `removed_at` |
| | `PURGED` | 系统管理员在所有站点停用后显式清除了数据 |
| `sys_site.status` | `SETUP` | 建设中：前台请求返回 503 并带 `X-Robots-Tag: noindex`；后台与预览可用 |
| | `ACTIVE` | 对外服务 |
| | `ARCHIVED` | 已归档：前台不再解析其域名，后台只读；数据保留 |
| `sys_site.url_strategy` | `SUBDIRECTORY` / `DOMAIN` | V1 固定 `SUBDIRECTORY`（全部语言带前缀）；`DOMAIN` 为 V2 语言独立域名 |
| `sys_site.trailing_slash` | `NEVER` / `ALWAYS` | URL 规范化时统一去掉 / 统一保留尾斜杠 |
| `sys_site_domain.type` | `PRIMARY` | 主域名（规范域名；canonical、sitemap 使用） |
| | `ALIAS` | 别名域名：301 到主域名 https |
| | `LOCALE` | 【V2】语言独立域名，`locale` 必填 |
| `sys_site_module.enabled` | 1 / 0 | `ENABLED` / `DISABLED`（系统设计 3.6 的站点级状态） |
| `sys_site_module.config.onDisable` | `NOT_FOUND` / `REDIRECT` | 停用后该模块内容类型的路由返回 404，或批量 301 到 `redirectContentId` |
| `sys_site_profile.social_links[].platform` | `LINKEDIN` / `FACEBOOK` / `YOUTUBE` / `X` / `INSTAGRAM` / `TIKTOK` / `WECHAT` / `OTHER` | 社交平台 |
| `sys_site_profile.addresses[].type` | `HEADQUARTERS` / `OFFICE` / `FACTORY` / `WAREHOUSE` / `SHOWROOM` / `OTHER` | 地址类型 |
| `sys_site_profile_localization.translation_state`、`sys_ui_text.translation_state` | `NONE` / `QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED` | 共享枚举（第 2.7 节）；源语言行为 `NONE` |
| `sys_user.status` | `ACTIVE` | 正常 |
| | `DISABLED` | 已停用（管理员操作），不能登录；临时锁定用 `locked_until` 表达，不改状态 |
| `sys_user_mfa.method` | `TOTP` | V1 只有 TOTP |
| `sys_user_mfa.status` | `PENDING` / `ACTIVE` | 已生成密钥待首次验证 / 已启用 |
| `sys_permission.data_scopes[]`、`sys_role_permission.data_scope` | `ALL` / `GROUP` / `OWN` | 共享枚举：当前站点全部 / 所在销售组 / 本人 |
| `sys_audit_log.result` | `SUCCESS` / `FAILURE` | 操作结果（如登录失败为 `FAILURE`） |
| `sys_job.status` | `PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELED` | 共享枚举（通用任务状态） |
| `sys_dsr_request.type` | `ACCESS` | 访问 |
| | `RECTIFY` | 更正 |
| | `ERASE` | 删除（匿名化联系字段，保留统计） |
| | `PORTABILITY` | 可携带（导出 JSON） |
| | `OBJECT` | 反对（撤销营销同意） |
| | `RESTRICT` | 限制处理 |
| | `OPT_OUT_SALE_SHARE` | CCPA 退出“出售 / 共享” |
| `sys_dsr_request.jurisdiction` | `GDPR` / `UK_GDPR` / `CCPA` | 适用法域（决定时限） |
| `sys_dsr_request.status` | `RECEIVED` | 已受理（已登记，计时开始） |
| | `VERIFYING` | 核验中（已发送确认链接或人工核验中） |
| | `PROCESSING` | 核验通过，处理中 |
| | `COMPLETED` | 已处理完成 |
| | `REJECTED` | 拒绝（无法核验或不适用；原因写入 `outcome_note`） |
| | `WITHDRAWN` | 请求人撤回 |
| `sys_dsr_request.channel` | `EMAIL` / `WEB_FORM` / `PHONE` / `POST` / `OTHER` | 受理渠道（具体开放哪些渠道由法务确认） |
| `sys_dsr_request.verification_method` | `EMAIL_LINK` | 向记录中的邮箱发送一次性确认链接（默认） |
| | `MANUAL` | 人工核验（如授权代理人提交） |
| | `NOT_REQUIRED` | 无需核验（如退出“出售 / 共享”） |

### 6.6 跨模块引用与清理

**本模块引用其他模块的对象**：

| 本模块字段 | 引用对象（模块.表） | 被引用对象删除 / 下线时的处理 | 触发事件 |
|---|---|---|---|
| `sys_site_profile.logo_media_id`、`logo_inverse_media_id`、`favicon_media_id`、`app_icon_media_id` | media.`mda_media` | core 实现 `MediaReferenceCollector` 上报这些引用，media 据引用记录阻止删除；若仍收到删除事件，置空对应字段、清除 `settings` 缓存标签并写审计 | `MediaDeletedEvent` |
| `sys_site_module.config.redirectContentId` | content.`cnt_content` | 内容被彻底删除时把 `onDisable` 改回 `NOT_FOUND` 并写审计 | `ContentPurgedEvent` |
| `sys_setting.setting_value` 中的对象 ID（如产品列表页 `content_id`、主 / 备翻译引擎 `engine_id`） | 声明该设置的模块所引用的表 | core 不解析业务设置的语义；由声明该设置的模块在自己的监听器中校验与清理 | 各模块自己的删除事件（如 `ContentPurgedEvent`） |
| `sys_job.result_file_key`、`params` 中的输入文件对象键 | 对象存储私有桶（infrastructure 端口，非模块表） | 到期由清理任务删除文件；任务记录保留期满后物理删除 | 定时任务 |
| `sys_audit_log.target_type` / `target_id` | 任意模块的对象 | 不清理（审计历史） | — |
| `sys_dsr_request.result` | 各模块（`PersonalDataProvider` 的处理结果） | 不清理（处理证明） | — |

**其他模块引用本模块的对象**（其他章节需配合）：

| 被引用对象 | 引用方 | 本模块对象变化时的处理 | 事件 |
|---|---|---|---|
| `sys_site.id` | 所有站点级表的 `site_id` | 站点不物理删除，只归档（`ARCHIVED`），各模块数据保留；站点设置变化时清除 `s{site}:settings` 缓存标签 | `SiteSettingsChangedEvent` |
| `sys_site_locale`（站点语言） | content、url、seo、search、translation 等以 `locale` 保存的数据 | V1 不删除站点语言（只取消对外公开并关闭自动翻译），其他模块的数据无需清理；对外公开、自动翻译、hreflang、默认对外语言变化时发布事件：translation 据此创建批量翻译任务，seo 更新 hreflang 与 sitemap，search 创建或停用该语言索引，delivery 清除缓存 | `SiteSettingsChangedEvent`（需携带变化的语言与字段） |
| `sys_site_module`（站点启用状态） | 所有可停用模块 | 启停时发布事件；停用前调用各模块 `analyzeDisableImpact` | `ModuleEnabledEvent` / `ModuleDisabledEvent` |
| `sys_user.id` | 所有表的 `created_by` / `updated_by` / `*_by`、inquiry 的负责人（`owner_user_id`）与销售组成员 | 用户只停用或软删除，`id` 永久有效；用户停用或删除时，其名下未关闭的询盘需要重新分配 | 目前事件目录（系统设计 4.3）没有用户事件，建议新增 `UserDeactivatedEvent`（见下方跨模块事项） |
| `sys_site_profile_localization`、`sys_ui_text` | translation（经 core 实现的 `TranslatableResourceProvider` 回写，资源类型 `SITE_PROFILE`、`UI_TEXT`；translation 不直接写 `sys_` 表） | 源语言文本或主题默认文案变化时发布事件，translation 重新翻译变化的单元 | `TranslatableResourceChangedEvent` |
| `sys_job.id` | 各模块通过 core 的任务服务创建任务（如 product 导入导出、url 重定向导入、inquiry 导出） | 任务记录到期物理删除；模块只保存 `job_id` 用于展示时须容忍其不存在 | — |

### 6.7 事务互斥技术表（ADR-022，Proposed）

sys_transaction_mutex 仅由 core 的 TransactionMutexApi 使用。它是事务互斥与 generation 存储，不是有租约的工作认领表；name 必须来自注册的技术命名空间，业务模块不得直接访问本表。站点删除由 core 清理，正常任务完成不删除行。

```sql
CREATE TABLE sys_transaction_mutex (
  id          BIGINT       NOT NULL COMMENT 'TSID',
  site_id     BIGINT       NOT NULL COMMENT '站点ID',
  name        VARCHAR(100) COLLATE utf8mb4_0900_bin NOT NULL COMMENT '注册的事务互斥名称',
  generation  BIGINT       NOT NULL DEFAULT 0 COMMENT '当前事务代次，不自动归零',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by  BIGINT       NOT NULL DEFAULT 0,
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by  BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tx_mutex (site_id, name),
  CONSTRAINT ck_tx_mutex_generation CHECK (generation >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事务互斥技术状态';
```

在M0 core初始化迁移之后追加独立迁移，不改写已有迁移。M0-07B已以真实MySQL验证首次建行、共享锁升级死锁负对照、独立站点generation及常态行锁隔离；冷建行的唯一索引末端间隙争用已记录。正式core的断连/连接池恢复、限次重试及完整生命周期仍待集成，原型不等于生产迁移或G0验收。

---

### 6.8 消费者幂等标记（M0补齐5.8.5）

core全局技术表，行级拦截豁免；site_id只记录事件归属，不作为自然主键。首次落地以core/V002追加，不改写V001。保留30天，清理须排除尚有未完成持久投递的eventId。

```sql
CREATE TABLE sys_processed_event (
  consumer VARCHAR(96) COLLATE utf8mb4_0900_bin NOT NULL,
  event_id VARCHAR(20) COLLATE utf8mb4_0900_bin NOT NULL,
  site_id BIGINT NULL,
  processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (consumer, event_id),
  KEY idx_pev_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消费者事务幂等标记';
```

### 6.9 Event Publication Registry技术表（Modulith2.0.8 schema v2）

此第三方技术表由core V003和迁移账号管理；应用DML账号关闭jdbc.schema-initialization。原厂v2结构保持字段/索引，SERIALIZED_EVENT改为MEDIUMTEXT以容纳合法事件载荷；payload上限仍由应用校验。补入迁移不改写V001。它不是新增业务模块或业务表前缀。

```sql
CREATE TABLE EVENT_PUBLICATION
(
  ID                     VARCHAR(36) NOT NULL,
  LISTENER_ID            VARCHAR(512) NOT NULL,
  EVENT_TYPE             VARCHAR(512) NOT NULL,
  SERIALIZED_EVENT       MEDIUMTEXT NOT NULL,
  PUBLICATION_DATE       TIMESTAMP(6) NOT NULL,
  COMPLETION_DATE        TIMESTAMP(6) DEFAULT NULL NULL,
  STATUS                 VARCHAR(20),
  COMPLETION_ATTEMPTS    INT,
  LAST_RESUBMISSION_DATE TIMESTAMP(6) DEFAULT NULL NULL,
  PRIMARY KEY (ID),
  INDEX EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX (COMPLETION_DATE)
);
```

### 6.10 缓存恢复技术表（ADR-031，Proposed）

全局Core技术表，不含site_id，列入行级过滤豁免。源事务在发布DomainEvent前共享锁定id=1；恢复事务排他锁定该行并快照未完成发布ID。DRAINING边界确认及逐站CDN清除后才CAS放行；详细协议见[ADR-031](../adr/0031-cache-recovery-publication-barrier.md)。旧迁移不改写。

```sql
CREATE TABLE sys_cache_recovery (
  id TINYINT NOT NULL PRIMARY KEY,
  boot VARCHAR(64) NOT NULL DEFAULT '',
  epoch CHAR(36) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
  started_at DATETIME(6) NULL
);
INSERT INTO sys_cache_recovery(id) VALUES(1);
```

```sql
CREATE TABLE sys_cache_recovery_publication (
  epoch CHAR(36) NOT NULL,
  publication_id VARCHAR(36) NOT NULL,
  PRIMARY KEY(epoch, publication_id)
);
```

## 7. 数据库通用约定与框架表

### 7.1 数据库与连接

| 项 | 约定 |
|---|---|
| 版本 | MySQL 8.4 LTS，InnoDB |
| 字符集 | 库与表默认 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci` |
| 精确匹配列 | 路径、slug、key、哈希、令牌等使用 `utf8mb4_0900_bin`，避免 `café` 与 `cafe`、大小写之间的误判 |
| 时区 | 所有时间以 UTC 存储；JDBC 连接设置 `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`，保证 `CURRENT_TIMESTAMP(3)` 也是 UTC |
| SQL 模式 | 使用 MySQL 8.4 默认的严格模式（`STRICT_TRANS_TABLES` 等），不得放宽 |
| 账号 | 应用账号只有 DML 权限；迁移任务使用单独的 DDL 账号（见系统设计 26.6） |

### 7.2 命名

| 对象 | 规则 | 示例 |
|---|---|---|
| 表 | `{模块前缀}_{名词单数}`，snake_case | `prd_product`、`cnt_localization` |
| 本地化表 | `{主表}_localization` | `prd_product_localization` |
| 关联表 | `{前缀}_{实体A}_{实体B}` | `sys_role_permission` |
| 主键 | `id` | |
| 外键字段 | `{被引用实体}_id` | `content_id`、`media_id` |
| 普通索引 | `idx_{表名缩写}_{含义}` | `idx_loc_list` |
| 唯一索引 | `uk_{表名缩写}_{含义}` | `uk_route_path` |
| 外键约束 | `fk_{本表缩写}_{被引用表缩写}`（外键名在库内必须唯一） | `fk_revision_localization` |
| 布尔字段 | 用形容词或过去分词，不加 `is_` 前缀；与 SQL 保留字冲突时加 `is_`（如 `is_primary`、`is_test`） | `enabled`、`pinned`、`is_primary` |
| 时间字段 | `{动作}_at` | `published_at` |
| 人员字段 | `{动作}_by`，存用户 ID | `published_by` |

索引名只需在同一张表内唯一，因此用表名缩写即可；外键约束名在整个库内唯一，必须带足以区分的表名缩写。

### 7.3 数据类型

| 用途 | 类型 | 说明 |
|---|---|---|
| ID | `BIGINT` | TSID（时间有序 64 位），由应用生成，不使用 `AUTO_INCREMENT`；JSON 中序列化为字符串（ADR-014） |
| 时间 | `DATETIME(3)` | UTC，毫秒精度 |
| 日期 | `DATE` | 如节假日 |
| 布尔 | `TINYINT(1)` | `NOT NULL DEFAULT 0/1` |
| 枚举 | `VARCHAR(n)` | 存英文大写常量；不使用 MySQL `ENUM` 类型，取值写在列注释与所属模块设计文档的 4.4 节 |
| 语言代码 | `VARCHAR(16)` | BCP 47，如 `zh-CN`、`en`、`de` |
| 国家代码 | `CHAR(2)` | ISO 3166-1 alpha-2，大写 |
| 标题 | `VARCHAR(300)` | |
| slug | `VARCHAR(160)` + `utf8mb4_0900_bin` | |
| URL 路径 | `VARCHAR(500)` + `utf8mb4_0900_bin` | 可建唯一索引（500 × 4 字节 < 3072 字节上限） |
| 完整 URL | `VARCHAR(2048)` | 不建索引 |
| 邮箱 | `VARCHAR(254)` | 去重或查找使用小写规范化的冗余列 |
| 电话 | `VARCHAR(32)` | E.164 |
| IP | `VARCHAR(45)` | 兼容 IPv6 |
| 哈希 | `CHAR(64)` + `utf8mb4_0900_bin` | SHA-256 十六进制 |
| 短文本 | `VARCHAR(1000)` 以内 | |
| 长文本 | `TEXT` / `MEDIUMTEXT` | 纯文本 |
| 富文本 | `JSON` | ProseMirror JSON，不存 HTML |
| 结构化数据 | `JSON` | 由应用按 Schema 校验；需要查询的字段用生成列建索引 |
| 数值（属性值） | `DECIMAL(20,6)` | 避免浮点误差 |
| 计数 | `INT` / `BIGINT` | |

### 7.4 通用字段

| 字段 | 定义 | 适用 |
|---|---|---|
| `id` | `BIGINT NOT NULL` 主键 | 所有实体表（纯关联表可用复合主键） |
| `site_id` | `BIGINT NOT NULL` | 站点级表；全局表没有（见各章表清单“站点级”列） |
| `created_at` | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | 所有表（只追加的日志表也需要） |
| `created_by` | `BIGINT NOT NULL DEFAULT 0` | 有人员操作的表；0 表示系统 |
| `updated_at` | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 可修改的表 |
| `updated_by` | `BIGINT NOT NULL DEFAULT 0` | 可修改的表 |
| `version` | `INT NOT NULL DEFAULT 0` | 可编辑的聚合根（乐观锁） |
| `deleted_at` | `DATETIME(3) NULL` | 软删除的配置类表 |
| `alive` | `TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) STORED` | 有软删除且需要唯一约束的表 |

- 应用层负责写入 `created_by`、`updated_by`、`version`；数据库默认值只是兜底。
- 只追加的表（日志、事件、修订）没有 `updated_*` 与 `version`。

### 7.5 软删除与唯一约束

- 内容使用回收站状态（`cnt_content.trashed_at`），不使用 `deleted_at`。
- 配置类实体（表单、规则、术语等）使用 `deleted_at` 软删除。
- 需要“未删除的行之间唯一”时，把生成列 `alive` 加入唯一索引；已删除行的 `alive` 为 NULL，唯一索引中 NULL 互不相等：

```text
alive TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) STORED,
UNIQUE KEY uk_form_key (site_id, form_key, alive)
```

- 日志、关联、修订等表不做软删除，按所属模块设计文档中的保留规则物理清理。

### 7.6 外键

- **模块内部**：组合关系（子表随父表存在）使用外键 `ON DELETE CASCADE`；引用关系使用外键 `ON DELETE RESTRICT`（默认）。
- **跨模块**：不建外键，只保存 ID；被引用对象删除时由事件驱动清理（每章 N.6 列出）。
- 指向全局表（如 `sys_user`）的人员字段（`created_by` 等）不建外键。

### 7.7 本地化表

- 每个需要多语言的实体都有对应的 `*_localization` 表，`(主表 ID, locale)` 唯一。
- 源语言（`zh-CN`）的行由编辑录入；其他语言的行由翻译模块通过 `TranslatableContentProvider` / `TranslatableResourceProvider` 写入（系统设计 5.5–5.7）。
- **内容类**本地化表（如 `prd_product_localization`）只存草稿数据，翻译状态记录在 `cnt_localization`。
- **站点资源类**本地化表（如 `nav_item_localization`、`prd_attribute_localization`、`tx_term_localization`）额外带两个字段：

| 字段 | 定义 | 说明 |
|---|---|---|
| `translation_state` | `VARCHAR(16) NOT NULL DEFAULT 'NONE'` | `NONE`（源语言）/ `QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED` |
| `source_fingerprint` | `CHAR(64) NULL` | 生成本行译文时源语言行的指纹（各可翻译字段规范化后拼接的 SHA-256）；源语言行为空；与当前源语言指纹不同即为过期 |

### 7.8 JSON 列

- 只用于结构由应用定义、整体读写的数据：快照、布局文档、富文本、设置、条件、规则参数等。
- 每个 JSON 文档顶层带 `schemaVersion`（快照、布局、设置类），便于升级迁移。
- 需要按 JSON 内部字段查询时，使用生成列 + 普通索引，不直接在 `WHERE` 中解析 JSON。

### 7.9 索引

- 每张站点级表的常用查询索引以 `site_id` 开头。
- 单表索引一般不超过 6 个；写入频繁的日志表只建必要索引。
- 唯一约束优先用唯一索引表达业务规则（如路径唯一、键唯一）。
- 不建立只有低选择性字段（如单独的状态字段）的索引。

### 7.10 迁移脚本

- 每个模块一个 Flyway 位置：`classpath:db/migration/{module}`，history 表 `flyway_{module}_history`（系统设计 3.6、25.6）。
- 初始脚本 `V001__init.sql` 由所属模块设计文档第 4.3 节的 DDL 组成（platform-core 见本文第 6 章），文件清单见各模块文档 4.7 节。
- 已合入主干的迁移脚本禁止修改；变更使用 expand / contract 模式。

### 7.11 文本长度与校验

- 数据库长度是上限，业务长度规则（如 SEO 标题建议 30–60 个字符）在应用层校验。
- 所有用户输入的文本在应用层去除首尾空白；邮箱另存小写规范化值用于查找。

### 7.12 框架表

以下表不属于任何业务模块，由框架或框架引入的组件使用。

| 表 | 来源 | 建表方式 | 说明 |
|---|---|---|---|
| `EVENT_PUBLICATION` | Spring Modulith 事件发布登记表（JDBC） | 框架自动建表（`spring.modulith.events.jdbc.schema-initialization.enabled=true`），结构以所选版本自带的 MySQL 脚本为准 | 事务内登记待投递事件，监听器成功后标记完成；未完成的事件在启动时与定时任务中重新投递（系统设计 4.1、ADR-003）。已完成记录由定时任务按保留期清理（本文 5.8） |
| `flyway_{module}_history` | Flyway | Flyway 自动创建 | 每个模块一张迁移历史表（7.10、本文 5.2） |
| `sys_lock` | ShedLock（JDBC 布局） | platform-core 的迁移脚本 | 定时任务与带过期时间的业务锁共用，定义见 6.4；模块迁移锁使用 MySQL 命名锁 `GET_LOCK('platform:migrate', …)`，不依赖本表 |

`EVENT_PUBLICATION` 的参考结构（以框架版本自带脚本为准，不纳入本项目的迁移脚本）：

```text
EVENT_PUBLICATION
  id                VARCHAR(36)   主键（UUID）
  listener_id       VARCHAR(512)  监听器标识
  event_type        VARCHAR(512)  事件类型
  serialized_event  VARCHAR(4000) 序列化后的事件
  publication_date  TIMESTAMP(6)  登记时间
  completion_date   TIMESTAMP(6)  完成时间（未完成为空）
  （较新版本另有状态、尝试次数等字段）
```

---

## 8. 前端框架

本章细化系统设计 10、11、21、22、24.3 的前端部分与本文 3.5 的工程结构，规定 M0 交付的前端框架：pnpm workspace 与依赖规则（8.1）、Admin 外壳与模块开发套件 `@b2bweb/admin-kit`（8.2）、前台渲染框架 `@b2bweb/website-kit`（8.3）、组件与模板 Schema 包（8.4）、`api-client` 生成（8.5）、Token 与主题包（8.6）。

- 业务模块的前端代码只放在 3.6 规定的三个前端位置（Admin 模块目录、组件 Schema、主题组件），并且只使用本章列出的 API；框架代码（`admin-kit`、`website-kit`、`shared`、`design-tokens`、`api-client` 的封装、两个应用的外壳）由框架组维护（2.6）。
- **规范性内容**：组件与模板定义的字段（8.4.2、8.4.3）、编辑器画布消息协议（8.3.13）、`defineAdminModule` / `registerContentTab` / `defineTheme` 的签名（8.2.8、8.2.9、8.3.5）是 Admin、Nuxt、后端与各模块之间的约定，修改按契约对待：Admin 与 Nuxt（或前后端）在同一个 PR 中同步修改，并更新本章（ADR-007 “需要遵守的规则”）。其余代码为示意，以 M0 实现为准。
- 第三方库的具体版本以 ADR-011 的版本表为准，本章只写库名。

### 8.1 pnpm workspace 与包

#### 8.1.1 工作区配置

```yaml
# frontend/pnpm-workspace.yaml
packages:
  - apps/*
  - packages/*
  - packages/themes/*
  - e2e                         # @b2bweb/e2e：Playwright E2E 与截图对比（9.6），框架补充，同步 3.5
catalog:                        # 第三方依赖的版本只在这里声明（取值见 ADR-011）
  vue: …
  nuxt: …
  element-plus: …
  pinia: …
  vue-router: …
  openapi-fetch: …
  "@tiptap/vue-3": …
  ajv: …
  vitest: …
  "@playwright/test": …
```

| 规则 | 说明 |
|---|---|
| Node 与 pnpm | Node 版本写在 `frontend/.nvmrc` 与根 `package.json` 的 `engines`；pnpm 由 Corepack 按 `packageManager` 字段锁定（版本取自 ADR-011） |
| 第三方版本 | 只在 `pnpm-workspace.yaml` 的 `catalog` 中声明，各包写 `"vue": "catalog:"`（对应后端“子工程不写版本号”，3.2）；`tools/checks/pnpm-catalog.mjs` 检查（10.2） |
| 内部依赖 | 一律 `workspace:*` |
| 锁文件 | `pnpm install --frozen-lockfile`；锁文件变化随 PR 提交 |
| 包的形式 | 全部 `"private": true`，不发布到 npm。除 `design-tokens`（Style Dictionary 产物）外，包的 `exports` 直接指向 TypeScript 源码（`src/index.ts`），由使用方（Vite / Nuxt）编译，省去包级构建与 watch |
| TypeScript | `tsconfig.base.json`：`strict`、`noUncheckedIndexedAccess`、`moduleResolution: "bundler"`、`verbatimModuleSyntax`；各包用项目引用（`references`），`vue-tsc --build` 增量检查 |
| 代码规范 | 根目录 `eslint.config.js`（flat config）、`stylelint.config.js`、`.dependency-cruiser.cjs`；自定义 ESLint 规则放在 `frontend/lint/`（框架补充，同步 3.5 的目录树），见 8.1.4 |

根 `package.json` 的脚本（CI 与本地使用同一组命令，第 10 章）：

| 命令 | 作用 |
|---|---|
| `pnpm lint` | ESLint（含 8.1.4 的自定义规则）+ Stylelint + dependency-cruiser |
| `pnpm typecheck` | `vue-tsc --build` + `nuxi typecheck apps/website` |
| `pnpm test` | Vitest workspace 中的全部单元与组件测试（9.6.1） |
| `pnpm build` | `design-tokens` → `admin` → `website` |
| `pnpm dev:admin` / `pnpm dev:website` | 本地开发服务器（第 11 章） |
| `pnpm gen:tokens` | 构建 Token（8.6.1） |
| `pnpm gen:api` | 由 OpenAPI 快照生成 `api-client`（8.5） |
| `pnpm check:schemas` | 组件与模板 Schema 校验与破坏性变更检查（8.4.6） |
| `pnpm check:theme` | 主题实现与组件定义的一致性检查（8.6.2） |
| `pnpm test:visual` | 组件画廊截图对比（9.6.2） |
| `pnpm test:e2e` | Playwright E2E（9.6.3） |

提交前必须通过 `pnpm lint && pnpm typecheck && pnpm test`（`CLAUDE.md`）。

#### 8.1.2 包清单

在 3.5 包表的基础上补充形式、入口与主要第三方依赖：

| 包 | 形式 | 主要入口（`exports`） | 主要第三方依赖 | 说明 |
|---|---|---|---|---|
| `@b2bweb/design-tokens` | 构建产物（`dist/`，不提交） | `./tokens.css`、`./utilities.css`、`./tokens`（TS 常量与类型）、`./style-options.json` | Style Dictionary | 8.6.1 |
| `@b2bweb/component-schemas` | JSON + TS 源码 | `.`（类型、加载、`migrate`、`validate`）、`./components/*`、`./templates/*`、`./defs/*` | Ajv（draft 2020-12） | 8.4；JSON 同时被后端复制到 classpath |
| `@b2bweb/api-client` | 生成的 TS 源码（提交） | `./admin`、`./public`、`./problem` | openapi-fetch；openapi-typescript（开发依赖） | 8.5 |
| `@b2bweb/shared` | TS 源码，不依赖 Vue | `./richtext`、`./editor-protocol`、`./json-patch`、`./locales`、`./ids`、`./text-keys` | — | 富文本模型与与框架无关的渲染器（接收 `h` 函数）、编辑器消息协议类型、JSON Patch、语言与 ID 工具 |
| `@b2bweb/admin-kit` | Vue 源码 | `.`、`./http`、`./auth`、`./layout`、`./schema-form`、`./pickers`、`./content`、`./texts`、`./testing` | Element Plus、Pinia、Vue Router、Tiptap、Ajv、`@element-plus/icons-vue` | 8.2 |
| `@b2bweb/website-kit` | Vue 源码 + Nuxt 模块 | `.`、`./nuxt`（Nuxt 模块）、`./render`、`./editor`、`./gallery` | Nuxt、`@nuxt/image` | 8.3 |
| `@b2bweb/theme-industrial` | Vue 源码 + DTCG JSON | `.`（`defineTheme` 结果）、`./theme.css`、`./texts/zh-CN.json` | — | 8.6.2 |
| `@b2bweb/admin` | 应用（Vite） | — | 同 `admin-kit` | 外壳装配 + 模块目录（8.2.1） |
| `@b2bweb/website` | 应用（Nuxt 4） | — | Nuxt、Sentry | 页面与服务端路由都是薄封装（8.3.1） |
| `@b2bweb/e2e` | 测试工程 | — | Playwright、`@axe-core/playwright` | 9.6.2、9.6.3 |

#### 8.1.3 依赖规则

在 3.5“依赖方向规则”的基础上，由 dependency-cruiser（`frontend/.dependency-cruiser.cjs`）与 ESLint `no-restricted-imports` 检查，违反即 `pnpm lint` 失败：

| # | 规则 | 检查 |
|---|---|---|
| FE-1 | `packages/**` 不得依赖 `apps/**` 与 `e2e`；两个应用之间不得互相 import | dependency-cruiser |
| FE-2 | Admin 的 `src/modules/<a>/` 不得 import `src/modules/<b>/`、`src/editor/` 与 `src/core/`；模块只依赖 `admin-kit`、`api-client`、`component-schemas`、`shared` 与本模块目录 | dependency-cruiser |
| FE-3 | 主题与 `website-kit` 不得依赖 `admin-kit`、Element Plus；`website-kit` 不得依赖任何主题（主题向它注册，8.3.5） | dependency-cruiser |
| FE-4 | `design-tokens` 只能被 `website-kit`、主题、`component-schemas`（样式选项）与 Admin 的主题设置页（`src/modules/system/theme/**`，系统设计 11.1）使用 | dependency-cruiser |
| FE-5 | 只能经入口使用 `api-client`，不得 deep import `src/generated/*` | dependency-cruiser |
| FE-6 | 禁止循环依赖（包之间与包内文件之间） | dependency-cruiser `no-circular` |
| FE-7 | 禁止的包：`vue-i18n` 及同类国际化库（Admin 与前台都不用，前台文案走 8.3.9）；AI 厂商 SDK（`openai`、`@anthropic-ai/sdk`、`@google/generative-ai`、`dashscope` 等，`CLAUDE.md` 禁止 12）；`element-plus` 在 Admin 与 `admin-kit` 之外 | ESLint `no-restricted-imports` + `tools/checks/pnpm-catalog.mjs`（依赖清单） |
| FE-8 | Admin 模块的 `api/` 中，字面量接口路径的模块段必须等于本模块的 `apiPrefix`；调用其他模块的后台接口只能经 `admin-kit` 的共享组件（8.2.11）或注册机制（8.2.9、8.2.10） | ESLint 规则 `b2bweb/admin-api-own-prefix` |

#### 8.1.4 自定义 lint 规则

| 规则 | 范围 | 内容 | 依据 |
|---|---|---|---|
| `vue/no-v-html` | 全部 | 禁止 `v-html`；例外清单只有富文本渲染器的路径（8.3.8），新增例外需架构负责人批准 | 系统设计 11.4；`CLAUDE.md` 禁止 8 |
| `b2bweb/no-literal-ui-text` | `website-kit`、主题、`apps/website` | 模板中的文本节点，以及 `alt`、`title`、`placeholder`、`aria-label`、`aria-description` 的字面量一律禁止，必须经 `t('…')`（8.3.9） | 系统设计 21.7；`CLAUDE.md` 语言规则 4 |
| `b2bweb/text-key-exists` | 同上 | `t('…')` / `tp('…')` 的字面量键必须存在于主题默认文案 `texts/zh-CN.json`；动态键须以 `// text-keys: a.b, a.c` 注释列出可能的取值 | 8.6.2 |
| `b2bweb/no-cjk-outside-texts` | `admin-kit`、`apps/admin` | 含中文字符的字符串字面量与模板文本只能出现在 `texts.ts`、`admin-kit/src/texts/**` 中（界面文案集中，系统设计 22.5） | `CLAUDE.md` 语言规则 1 |
| `b2bweb/no-inline-style-literal` | `website-kit`、主题 | 禁止带字面量的 `style` 绑定；只允许 CSS 变量与工具类 | 系统设计 11.4 |
| `b2bweb/admin-api-own-prefix` | `apps/admin/src/modules/*/api/**` | FE-8 | 3.6 |
| Stylelint `declaration-strict-value` | 全部 `.vue` / `.css`（`design-tokens` 除外） | 颜色、间距、字号、行高、圆角、阴影、`z-index` 只能使用 `var(--…)`；禁止十六进制、`rgb()`、`hsl()` 等颜色字面量 | 系统设计 11.4；`CLAUDE.md` 禁止 5 |
| Stylelint `custom-property-pattern` | 主题、`website-kit` | 不得定义新的 `--color-*`、`--space-*` 等 Token 变量（Token 只能在 `design-tokens` 中新增） | `CLAUDE.md` 禁止 5 |

### 8.2 Admin 外壳（@b2bweb/admin-kit）

Admin 是“外壳 + 模块”的结构（系统设计 22.1）：外壳的可复用部分（HTTP、认证、布局、路由、权限、站点切换、内容编辑外壳、SchemaForm、共享选择器、中文文案常量）放在 `@b2bweb/admin-kit`，便于独立测试与约束依赖方向（3.5）；`apps/admin/src/core/` 只做装配。系统设计 22.1 中 `src/shared/` 下的组件在本框架中都属于 `admin-kit`。

#### 8.2.1 应用结构与启动

```text
frontend/apps/admin/src/
├── main.ts              # createAdminApp({ modules })：装配 admin-kit 的插件、路由、布局
├── core/                # 外壳页面：登录、两步验证与绑定、改密、403 / 404、仪表盘框架（只调用 admin-kit）
├── modules/
│   ├── index.ts         # import.meta.glob('./*/index.ts', { eager: true }) 自动收集，新增模块不改共享文件
│   ├── system/          # core 的后台（apiPrefix = system，3.6）
│   └── <apiPrefix>/     # 每个业务模块一个目录（8.2.12）
└── editor/              # 页面编辑器（其实现归属在 page 模块设计文档中确定；框架提供 SchemaForm、画布协议与 EditorCanvasHost）
```

```text
启动
 1. GET /api/admin/v1/system/auth/csrf            下发 XSRF-TOKEN Cookie（5.6.2）
 2. GET /api/admin/v1/system/bootstrap（用户级）   401 → 登录页；阶段不是 AUTHENTICATED → 两步验证 / 绑定 / 改密页
      → 当前用户、可访问站点（含状态）、功能开关
 3. 选定站点：localStorage 中上次的站点 ∈ 可访问站点，否则取第一个（8.2.6）
 4. GET /api/admin/v1/system/bootstrap（站点级，带 X-Site-Id）
      → 权限快照（权限码、数据范围、语言范围）、当前站点启用的模块、菜单树（已按权限与启用状态过滤，系统设计 22.2）、组件清单
 5. 注册路由：只注册“已启用且 apiPrefix 出现在 bootstrap 中”的模块路由（懒加载），其余前缀落入 404
 6. 渲染布局；执行各已启用模块的 setup（8.2.8）
```

- bootstrap 是 core 的接口（5.2 的“后台菜单与路由”一行）；本节按 Admin 的需要把响应分为用户级与站点级两部分（站点级部分只在带 `X-Site-Id` 时返回），字段以 OpenAPI 为准，同步第 5 章。
- 所有模块的代码都编译进 Admin（按模块分包、懒加载）；模块停用只影响路由注册与菜单，不需要模块写判断代码（系统设计 3.8）。

#### 8.2.2 HTTP 层：openapi-fetch、CSRF 与统一错误处理

```ts
import { useApi } from '@b2bweb/admin-kit/http'

const api = useApi()                                        // openapi-fetch Client<AdminPaths>，已装配中间件
const { data } = await api.GET('/api/admin/v1/inquiry/inquiries', {
  params: { query: { page: 1, size: 20, sort: '-createdAt' } },
})                                                          // 出错时抛出 ApiError，不返回 error 分支
```

中间件（按顺序）：

| # | 中间件 | 行为 |
|---|---|---|
| 1 | 站点 | 加请求头 `X-Site-Id`（当前站点）；系统级接口以 `{ siteless: true }` 调用时不加（5.4） |
| 2 | CSRF | 非 GET / HEAD 请求从 Cookie `XSRF-TOKEN` 取值写入 `X-XSRF-TOKEN`（5.6.1） |
| 3 | 幂等键 | 调用方声明 `{ idempotent: true }` 时生成 `Idempotency-Key`（UUID），同一次操作的自动重试复用同一个键（5.5.7） |
| 4 | 版本 | `{ ifMatch: version }` 时加 `If-Match: "v{version}"`（5.5.6） |
| 5 | 错误 | 非 2xx 响应按 RFC 9457 解析为 `ApiError { status, code, title, detail, errors[], traceId, properties }`，再按下表统一处理 |

| 响应 | 统一处理 |
|---|---|
| 401 `CORE_NOT_AUTHENTICATED` | 清空会话状态，跳转 `/login?redirect=当前路径` |
| 403 `CORE_CSRF_INVALID` | 重新获取 CSRF Cookie，自动重试一次 |
| 403 `CORE_MFA_REQUIRED` / `CORE_PASSWORD_CHANGE_REQUIRED` | 跳转两步验证 / 改密页 |
| 403 `CORE_REAUTH_REQUIRED` | 弹出二次验证框（`POST …/auth/reauthenticate`），成功后自动重试原请求（5.6.2） |
| 403 `CORE_FORBIDDEN` | 提示 `title`；路由级的 403 显示 403 页 |
| 404 `CORE_MODULE_DISABLED` | 提示“该模块已在当前站点停用”，重新获取站点级 bootstrap 并刷新路由 |
| 400 `CORE_VALIDATION_FAILED` | 抛给调用方；`useFormErrors(form)` 把 `errors[].field` 映射到表单项 |
| 409 / 412 版本冲突（带 `currentVersion`） | 抛出 `VersionConflictError`；表单显示冲突对话框（“重新加载最新版本”或“对比后手工合并”），不提供强制覆盖 |
| 429 | 提示稍后重试，显示 `Retry-After` |
| 503 `CORE_SERVICE_STARTING` | 2 秒后重试，最多 5 次 |
| 其他 5xx、网络错误 | 通用错误提示，附 `traceId` 供排查；上报 Sentry（不含请求体） |

- 提示内容直接使用后端的中文 `title`，`detail` 与 `traceId` 折叠显示（后台错误信息只用中文，`CLAUDE.md` 语言规则 2）；前端不维护错误码到文案的映射表。
- 模块不得自行创建 `fetch` / axios 客户端；需要上传进度等特殊能力时使用 `admin-kit` 的 `upload()`（同样经过上述中间件）。

#### 8.2.3 认证与会话

| 能力 | 实现 |
|---|---|
| 页面 | 登录、两步验证、绑定 TOTP（二维码由浏览器端库生成，密钥不经过第三方服务，5.6.2）、恢复码展示（只显示一次）、改密；位于 `src/core/`，使用 `admin-kit/auth` 的组件与 store |
| 状态 | Pinia store `useAuthStore`：认证阶段、当前用户；不在前端保存权限以外的会话信息（会话在服务端，ADR-010） |
| 路由守卫 | 阶段不是 `AUTHENTICATED` 时只允许进入对应的验证页面 |
| 二次验证 | `ReauthDialog` 由 HTTP 层统一触发（8.2.2），模块不需要单独处理 |
| 注销 | `POST …/auth/logout` 后清空全部 Pinia store 与 `sessionStorage` |
| 会话过期 | 依赖 401 处理；不做前端倒计时 |

#### 8.2.4 布局、菜单与路由

- **布局**：侧栏菜单（bootstrap 的菜单树；一级分组的图标为 `@element-plus/icons-vue` 组件名，附录 A.1）、顶栏（站点切换、全局搜索入口、用户菜单）、面包屑（由路由 `meta.title` 生成）、内容区（列表页 `keep-alive`）。界面只用简体中文，Element Plus 使用其自带的中文语言包（组件内部文案，不属于 vue-i18n）。
- **路由约定**（3.7）：路径 `/{apiPrefix}/{页面}`，路由名 `{apiPrefix}.{页面}`；`meta.title`（取自本模块 `texts.ts`）、`meta.permission`、`meta.menuKey`（高亮的菜单）。
- **守卫顺序**：认证阶段 → 模块已启用（否则 404 页）→ `meta.permission`（否则 403 页）→ 离开有未保存修改的页面时确认（`useUnsavedGuard`）→ 设置文档标题。
- 菜单项的 `route` 必须存在于该模块 `defineAdminModule` 的路由中（附录 A.3 第 14 条，CI 检查），因此路由定义必须是静态字面量（8.2.8）。

#### 8.2.5 权限指令与组合式函数

```vue
<el-button v-perm="'inquiry:inquiry:export'" @click="exportList">{{ texts.export }}</el-button>   <!-- 无权限时不渲染 -->
<el-button v-perm.disable="'inquiry:inquiry:assign'">{{ texts.assign }}</el-button>             <!-- 无权限时禁用并提示 -->
<el-button v-perm="['content:product:publish', 'content:product:review']">…</el-button>            <!-- 任一权限 -->
```

```ts
const perm = usePermission()
perm.has('inquiry:inquiry:view')
perm.dataScope('inquiry:inquiry:view')          // 'ALL' | 'GROUP' | 'OWN' | null（4.4.7）
perm.canAccessLocale('translation:memory:review', 'de') // 语言范围（系统设计 5.8）
```

- 前端权限判断只用于界面体验，后端 `@RequiresPermission` 才是授权依据（系统设计 26.2）。
- `v-perm`、`registerContentTab` 与路由 `meta.permission` 中出现的权限码都必须在某个 `module.json` 中声明（附录 A.3 第 12 条）；`tools/checks/module-json.mjs` 静态提取这些字面量比对。

#### 8.2.6 站点切换

| 项 | 规定 |
|---|---|
| 入口 | 顶栏站点切换器：显示可访问站点的名称与代码；`ARCHIVED` 站点带“只读”标识 |
| 切换流程 | 有未保存修改时先确认 → 更新当前站点（写入 `localStorage` 键 `b2bweb.admin.siteId`）→ 重新获取站点级 bootstrap → 移除并重新注册模块路由 → 重置全部站点级 store → 当前路由在新站点可用则停留，否则回到仪表盘 |
| 站点级 store | 模块用 `defineSiteScopedStore()`（`pinia` 的 `defineStore` 包装）定义与站点相关的状态，切换站点时自动 `$reset()`；不得把站点 A 的数据带到站点 B |
| 多标签页 | 每个标签页独立选择站点（每个请求独立携带 `X-Site-Id`，会话绑定用户而不绑定站点，5.4）；顶栏始终醒目显示当前站点，降低误操作 |
| 只读站点 | `useSite().readonly` 为 true 时，`v-perm` 的写操作按钮统一禁用，SchemaForm 进入只读（后端同样拒绝写入，4.4.1） |

#### 8.2.7 中文文案常量

| 位置 | 内容 |
|---|---|
| `admin-kit/src/texts/common.ts` | 通用操作（保存、取消、删除、发布、提交审核、导出……）、确认语、空状态、分页 |
| `admin-kit/src/texts/status.ts` | 内容状态、翻译状态（`QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED`）、翻译模式、任务状态的中文名 |
| `admin-kit/src/texts/validation.ts` | SchemaForm 与表单校验的中文消息（按 Ajv 关键字与约束名） |
| `apps/admin/src/modules/<apiPrefix>/texts.ts` | 本模块的界面文案：`export const texts = { … } as const` |

- 不引入 vue-i18n，不做语言切换（`CLAUDE.md` 语言规则 1、禁止 11）；中文字面量只能出现在上述文件中（8.1.4 `b2bweb/no-cjk-outside-texts`）。
- 后台字典（`DictApi`）的中文名由接口返回，前端不重复维护；`module.json` 中菜单与权限的中文名由 bootstrap 返回。
- 日期时间统一用 `formatDateTime(value)`：按站点时区显示，格式固定为中文习惯（`YYYY-MM-DD HH:mm`）。

#### 8.2.8 defineAdminModule

规范性签名（`@b2bweb/admin-kit`）：

```ts
export interface AdminModuleDefinition {
  /** 模块 ID，等于 module.json 的 id。 */
  id: string
  /** 路由前缀与目录名，默认等于 id；只有 core 为 'system'（3.6）。 */
  apiPrefix?: string
  /** 本模块的页面路由（静态字面量，CI 静态提取）。 */
  routes: AdminRoute[]
  /** 注册到内容编辑外壳的标签（8.2.9）。 */
  contentTabs?: ContentTabDefinition[]
  /** 仪表盘卡片。 */
  widgets?: DashboardWidgetDefinition[]
  /** 键引用来源（8.2.10 的 key-ref 控件），如 form 提供 'form'、page 提供 'global-block'。 */
  refSources?: RefSourceDefinition[]
  /** 模块在当前站点启用后执行；不得调用其他模块的接口，不得修改全局状态以外的其他模块状态。 */
  setup?: (ctx: AdminModuleContext) => void
}

export interface AdminRoute {
  /** 相对于 /{apiPrefix}/ 的路径，如 'inquiries'、'inquiries/:id'。 */
  path: string
  /** {apiPrefix}.{页面}，如 'inquiry.inquiries'。 */
  name: string
  component: () => Promise<Component>
  meta: { title: string; permission?: string; menuKey?: string; keepAlive?: boolean }
}

export interface DashboardWidgetDefinition {
  key: string                       // 全局唯一，以模块 ID 开头
  title: string
  permission?: string
  size: 'sm' | 'md' | 'lg'
  order?: number
  component: () => Promise<Component>
}

export interface RefSourceDefinition {
  kind: string                      // 与组件 Schema 中的 "x-ref" 取值一致（8.4.2）
  label: string
  /** 按关键字搜索可选的 key（调用本模块自己的后台接口）。 */
  search: (keyword: string) => Promise<Array<{ key: string; label: string }>>
}

export function defineAdminModule(definition: AdminModuleDefinition): AdminModuleDefinition
```

示例：

```ts
// frontend/apps/admin/src/modules/inquiry/index.ts
import { defineAdminModule } from '@b2bweb/admin-kit'
import { texts } from './texts'

export default defineAdminModule({
  id: 'inquiry',
  routes: [
    { path: 'inquiries', name: 'inquiry.inquiries', component: () => import('./views/InquiryList.vue'),
      meta: { title: texts.menu.inquiries, permission: 'inquiry:inquiry:view', menuKey: 'inquiry', keepAlive: true } },
    { path: 'inquiries/:id', name: 'inquiry.inquiry-detail', component: () => import('./views/InquiryDetail.vue'),
      meta: { title: texts.menu.detail, permission: 'inquiry:inquiry:view', menuKey: 'inquiry' } },
    { path: 'assignment-rules', name: 'inquiry.assignment-rules', component: () => import('./views/AssignmentRules.vue'),
      meta: { title: texts.menu.rules, permission: 'inquiry:rule:manage', menuKey: 'inquiry-rules' } },
  ],
  widgets: [
    { key: 'inquiry-today', title: texts.widget.today, permission: 'inquiry:inquiry:view', size: 'sm',
      component: () => import('./components/TodayWidget.vue') },
  ],
})
```

| 规则 | 检查 |
|---|---|
| `id` 与目录名、`module.json` 一致；`apiPrefix` 只有 core 不等于 `id` | `tools/checks/module-json.mjs` |
| 路由 `path`、`name` 符合 3.7；`meta.permission` 已声明 | 同上（静态提取） |
| 每个模块只有一个 `index.ts` 默认导出 `defineAdminModule` 的结果；不得在运行时动态拼接路由 | ESLint + 检查脚本 |
| 模块停用时：不注册路由、不显示菜单、不显示内容标签与仪表盘卡片、键引用来源不可用 | admin-kit 统一实现（系统设计 3.8） |

#### 8.2.9 registerContentTab 与内容编辑外壳

所有内容类型共用 `admin-kit/content` 的 `ContentEditorShell`（系统设计 22.3）：顶栏（状态徽标、语言切换与各语言的翻译模式 / 翻译状态、保存、预览、提交审核、发布 / 定时发布、历史修订）+ 标签页。内容类型所属模块用它注册自己的编辑路由，例如 product 注册 `/product/products/:contentId` 并渲染 `<ContentEditorShell content-type="PRODUCT" />`。外壳经 `api-client` 调用 content 的后台接口（生命周期、本地化、修订、预览令牌），不经任何业务模块的代码。

注册标签（规范性签名，扩展系统设计 22.3 的示例）：

```ts
export interface ContentTabDefinition {
  /** 注册方模块 ID；该模块在当前站点停用时标签不显示。 */
  module: string
  /** 全局唯一的标签 key。 */
  key: string
  /** 中文标签名。 */
  label: string
  /** '*' 或内容类型数组。 */
  contentTypes: '*' | string[]
  /** 显示标签需要的权限码（编辑权限由标签内部按需判断）。 */
  permission?: string
  /** 'source'：只在源语言（zh-CN）；'target'：只在目标语言；'all'（默认）。 */
  locales?: 'source' | 'target' | 'all'
  /** 排序，升序；建议值见下表。 */
  order?: number
  component: () => Promise<Component>
}

export function registerContentTab(tab: ContentTabDefinition): void   // 通常经 defineAdminModule 的 contentTabs 调用
```

标签组件的约定：

```ts
// 外壳传入的 props
interface ContentTabProps {
  contentId: string
  localizationId: string
  contentType: string
  locale: string
  isSource: boolean
  translationMode: 'AUTO' | 'MANUAL' | null     // 源语言为 null
  readonly: boolean                              // 外壳计算：无编辑权限、只读站点、AUTO 目标语言的文本、审核锁定等
}

// 标签内部向外壳登记保存逻辑
const tab = useContentTab()
tab.registerSave({
  isDirty: () => form.dirty,
  validate: async () => form.validate(),        // 返回中文错误列表；有错误时外壳阻止保存并定位到该标签
  save: async () => api.PUT('/api/admin/v1/seo/meta/{localizationId}', { … }),
})
tab.onReload(() => form.reset())                // 外壳重新加载（冲突、切换语言）时调用
```

| 规则 | 说明 |
|---|---|
| 保存顺序 | 外壳先对全部有修改的标签执行 `validate`，再按 `order` 依次 `save`：主标签（本地化草稿，带乐观锁版本）最先；某个标签失败时停止并在该标签显示错误，已保存的部分不回滚（各模块的数据各自独立，系统设计 4.4） |
| 自动保存 | 外壳每 30 秒对有修改的标签执行一次保存（系统设计 22.5）；失败只提示，不打断编辑 |
| AUTO 目标语言 | 文本只能在“翻译”标签中按段修订（系统设计 5.5、22.3）；其他标签以 `readonly = true` 渲染 |
| 标签之间不通信 | 标签不得读取或修改其他标签的状态；需要协作时经后端（各自的接口）或领域事件 |

建议的标签与顺序：

| key | order | 注册方 | 适用 |
|---|---|---|---|
| `main` | 100 | 内容类型所属模块（product、article、page、taxonomy） | 本模块的内容类型；业务字段表单 |
| `slots` | 200 | page | 使用模板的内容类型；模板插槽编辑 |
| `seo` | 300 | seo | 全部 |
| `relations` | 400 | graph | 全部可路由类型 |
| `translation` | 500 | translation | 目标语言（`locales: 'target'`） |
| `revisions` | 900 | 外壳内置（`RevisionHistory`） | 全部 |

系统设计 22.1 列在 `shared/` 中的 `SeoPanel`、`RelationPanel` 在本框架中分别由 seo、graph 模块以内容标签实现，不放入 `admin-kit`（属于业务模块的界面）。

#### 8.2.10 SchemaForm 与 x-editor 控件

`SchemaForm` 根据 JSON Schema 渲染表单（系统设计 22.4），用于组件属性、模块设置（`settings.schema.json`）与数据源参数（`DataSourceProvider.paramsSchema`）：

```vue
<SchemaForm
  v-model="section.props"
  :schema="definition.propsSchema"
  :context="{ locale, contentId, contentType, mode: 'component' }"
  :readonly="readonly"
  :server-errors="errors"
  @validate="onValidate"
/>
```

控件（`x-editor` 取值；“补充”为框架补充，同步系统设计 22.4）：

| `x-editor` | 适用 Schema | 值形态 | 控件 | 相关关键字 |
|---|---|---|---|---|
| `text` | `string` | 字符串 | 单行输入，显示 `maxLength` 计数 | `maxLength`、`x-placeholder` |
| `textarea` | `string` | 字符串 | 多行输入 | `maxLength` |
| `richtext` | `$defs/richText` | ProseMirror JSON | Tiptap 编辑器，节点白名单同前台渲染器（系统设计 9 章的富文本规则，8.3.8）；内部链接存为 `content://{id}` | — |
| `richtext-inline` | `$defs/richTextInline` | ProseMirror JSON（只有行内节点） | Tiptap 行内编辑器（加粗、斜体、链接） | — |
| `media` | `$defs/mediaRef` | `{ "$media": "id" }` | `MediaPicker` | `x-media.kinds`、`x-media.requireAlt` |
| `content-ref` | `$defs/contentRef` | `{ "$content": "id" }` | `ContentPicker` | `x-content.types` |
| `term-ref` | `$defs/termRef` | `{ "$term": "id" }` | `TermPicker` | `x-term.vocabulary` |
| `select` | `enum` | 枚举值 | 下拉框 | `x-enum-labels`（中文显示名，按枚举顺序） |
| `color-token` | `$defs/styleValue`（颜色类） | Token 名 | 色块选择器（取自 `style-options.json`） | — |
| `spacing-token` | `$defs/styleValue`（间距类） | Token 名 | 刻度选择器 | — |
| `link` | `$defs/link` | `{ "$content": "id" }` 或 `{ "href": "https://…", "rel": [] }` | `LinkEditor`：站内内容或外部 URL（只允许 http / https） | — |
| `repeater` | `array` | 数组 | 可增删、拖拽排序的列表，元素按 `items` 递归渲染 | `minItems`、`maxItems` |
| `group` | `object` | 对象 | 可折叠的分组 | — |
| `data-source` | `$defs/dataSource` | `{ "$source": { "provider", "params" } }` | `DataSourceEditor`：数据源列表与 `paramsSchema` 来自 delivery 的后台接口，参数表单再由 SchemaForm 渲染 | `x-source.providers`（可选的数据源白名单） |
| `number`（补充） | `integer` / `number` | 数字 | 数字输入 | `minimum`、`maximum` |
| `switch`（补充） | `boolean` | 布尔 | 开关 | — |
| `key-ref`（补充） | `string` + `x-ref` | 业务 key（如表单 key） | `RefKeySelect`：候选项来自对应 `x-ref` 种类的 `RefSourceDefinition`（8.2.8） | `x-ref` |

- 省略 `x-editor` 时按类型推断：`string` → `text`、`boolean` → `switch`、`integer` / `number` → `number`、带 `enum` → `select`、`object` → `group`、`array` → `repeater`；媒体、引用、富文本、Token、数据源、键引用必须显式标注。
- 每个属性必须有中文 `title`；`description` 显示为帮助文字；`x-group`（`content` / `data` / `advanced`，默认 `content`）把属性分到属性面板的“内容 / 数据源 / 高级”分组，Section 的样式（`styleOptions`）与可见性由面板固定提供“样式 / 可见性”分组（系统设计 10.8）；`x-order` 控制顺序；`x-visible-if`（如 `{ "prop": "mode", "equals": "video" }`）控制条件显示，隐藏的属性不提交。
- 校验：Ajv（draft 2020-12）编译 Schema，全部 `x-*` 关键字登记为注解关键字（不参与校验）；错误消息取自 `texts/validation.ts`；服务端返回的 `errors[]`（字段路径）通过 `server-errors` 映射回对应控件。
- 设置表单中 `x-secret` 属性渲染为密码框：读取时显示掩码，留空表示不修改（4.4.3）。
- `readonly` 时所有控件只读；AUTO 目标语言的组件属性只能预览（系统设计 10.8）。

#### 8.2.11 共享选择器与通用组件

| 组件 | 用途 | 使用的接口 | 说明 |
|---|---|---|---|
| `MediaPicker` | 选择 / 上传媒体，快速编辑 alt | media 的后台接口 | `kinds` 过滤；`requireAlt` 时缺少 alt 不能确认 |
| `ContentPicker` | 选择内容（单选 / 多选） | content 的后台检索接口 | 按类型、状态、语言过滤；显示该语言是否已发布 |
| `TermPicker` | 选择分类项（树） | taxonomy 的后台接口 | 按词汇表过滤 |
| `RichTextEditor` | 富文本编辑 | — | 节点白名单与 `@b2bweb/shared/richtext` 一致；行内内容链接用 `ContentPicker` |
| `LocaleTabs` | 语言切换 | — | 每种语言显示翻译模式与翻译状态（系统设计 22.3） |
| `RevisionHistory` | 修订列表、对比、恢复为草稿 | content 的后台接口 | 外壳内置标签 |
| `ImpactDialog` | 展示影响报告（4.3.9）并要求确认 | — | 下线、移入回收站、停用模块等操作前使用 |
| `PageTable` | 列表页：筛选、分页（≤ 100）、排序白名单、批量操作 | — | 筛选条件按路由保存在 `localStorage`（系统设计 22.5） |
| `JobProgress` | 跟踪 `sys_job` 任务进度、下载结果文件 | core 的任务接口 | 导入、导出共用 |
| `ImportWizard` | 上传 → 校验预览（逐行错误）→ 确认 → 执行 | core 的任务接口 + 模块的导入端点 | 场景 S6；提交带 `Idempotency-Key` |
| `ConfirmDangerDialog` | 高危操作的二次确认（输入名称确认） | — | 删除、彻底删除、停用模块 |
| `EditorCanvasHost` | 页面编辑器的 iframe 宿主：加载画布、收发 8.3.13 的消息 | — | 校验 `origin`；页面编辑器使用 |

- 共享组件调用 L2 模块的后台接口是框架代码的职责（模块之间不互相 import，FE-2、FE-8）；对应模块在当前站点停用时，组件显示为不可用并说明原因。
- 新增共享组件由框架组评审；只被一个模块使用的组件留在该模块目录中。

#### 8.2.12 Admin 模块目录模板

```text
frontend/apps/admin/src/modules/<apiPrefix>/
├── index.ts          # defineAdminModule（8.2.8）——唯一的对外入口
├── texts.ts          # 本模块的中文界面文案（8.2.7）
├── api/              # 对 api-client 的薄封装：只调用 /api/admin/v1/<apiPrefix>/**（FE-8）
├── stores/           # Pinia；站点相关状态用 defineSiteScopedStore
├── views/            # 路由页面
├── components/       # 本模块私有组件
├── tabs/             # 注册到内容编辑外壳的标签组件（如有）
├── widgets/          # 仪表盘卡片（如有）
└── __tests__/        # Vitest 组件与逻辑测试（9.6.1）
```

- 由脚手架生成（3.8）：`index.ts`、`texts.ts`、空目录与一个通过的示例测试。
- 只能 import：本目录、`@b2bweb/admin-kit`、`@b2bweb/api-client`、`@b2bweb/component-schemas`、`@b2bweb/shared`（FE-2）。
- 模块设计文档第 7 节列出本模块的菜单、路由、页面、注册的标签与文案（第 12 章模板）。

### 8.3 前台网站框架（@b2bweb/website-kit）

#### 8.3.1 结构

```text
frontend/packages/website-kit/src/
├── nuxt/            # Nuxt 模块（@b2bweb/website-kit/nuxt）：自动导入、运行时配置、服务端中间件、路由规则
├── delivery/        # resolve / site / preview 调用、请求头与超时、站点引导数据的进程内缓存
├── render/          # TemplateResolver、TemplateRegion、LayoutRenderer、SectionRenderer、RichTextRenderer、错误边界
├── theme/           # defineTheme、组件注册表、变体回退、基础主题（base）
├── refs/            # useRefs、B2bImage、B2bLink
├── text/            # useText、useFormat、useApiError
├── seo/             # useSeoHead
├── http/            # 缓存头、状态码、Retry-After
├── editor/          # 画布运行时：消息桥、补丁应用、覆盖层（8.3.13）
└── gallery/         # 组件画廊页面（9.6.2）
```

应用 `frontend/apps/website/` 的目录保持系统设计 21.1 的结构，但页面与服务端路由都是薄封装（每个文件只调用 `website-kit`，不写业务逻辑）：

```vue
<!-- frontend/apps/website/app/pages/[...path].vue -->
<script setup lang="ts">
const page = await useContentRoute()          // website-kit：resolve → 分支 → 缓存头 → SEO head
</script>
<template>
  <TemplateResolver v-if="page.kind === 'CONTENT'" :page="page" />
</template>
```

`nuxt.config.ts` 只做装配：`modules: ['@b2bweb/website-kit/nuxt']`，`b2bweb.themes: ['@b2bweb/theme-industrial']`（构建时打包全部主题，运行时按站点选择，系统设计 11.2），以及运行时配置：

| 配置（环境变量） | 范围 | 用途 |
|---|---|---|
| `NUXT_B2BWEB_API_BASE` | 仅服务端 | 后端内网地址（如 `http://backend:8080`） |
| `NUXT_B2BWEB_INTERNAL_TOKEN` | 仅服务端 | 内部令牌 `X-Internal-Token`（5.6.5）；不得进入客户端包 |
| `NUXT_B2BWEB_RESOLVE_TIMEOUT` | 仅服务端 | resolve 超时，默认 3 秒 |
| `NUXT_B2BWEB_SITE_CACHE_TTL` | 仅服务端 | 站点引导数据进程内缓存时长，默认且最长 60 秒（系统设计 23.1） |
| `NUXT_PUBLIC_ADMIN_ORIGIN` | 公开 | 后台源（编辑器画布的 `frame-ancestors` 与 `postMessage` 校验） |
| `NUXT_PUBLIC_GALLERY` | 公开 | 是否开放 `/__gallery`（生产为 false） |
| `NUXT_PUBLIC_SENTRY_DSN` | 公开 | 错误上报（系统设计 27.4） |

#### 8.3.2 内容路由与 Delivery 调用

```text
GET https://www.example.com/de/produkte/li-ion-pack-x200
 1. 服务端中间件：请求 ID、CSP nonce 与安全头（系统设计 21.6）
 2. useContentRoute()（只在服务端取数）
    a. GET {API_BASE}/api/public/v1/delivery/resolve?path=<原始路径>
         请求头：X-Site-Host（请求 Host，去掉端口）、X-Internal-Token、X-Client-IP（访客 IP）、traceparent
         超时 NUXT_B2BWEB_RESOLVE_TIMEOUT；不自动重试；期限内缓存可用，过期无可用结果时返回503
    b. useSite(resolve.locale)：站点引导数据 GET …/delivery/site?locale=…（进程内缓存，键 = 站点 Host + 语言）
    c. 按 kind 分支（下表）
 3. 渲染：TemplateResolver → 模板 → TemplateRegion / LayoutRenderer → SectionRenderer
 4. 输出 HTML：缓存头（8.3.11）、SEO head（8.3.10）、<html lang dir>
```

| 结果 | 处理 |
|---|---|
| `kind = CONTENT` | 渲染；缓存头取自 `cache` |
| `kind = REDIRECT` | 服务端 `sendRedirect(location, status)`，状态码原样使用（301 / 302 / 308）；根路径 `/` 由 Delivery 返回 302 到默认对外语言，Nuxt 不自行决定语言（`CLAUDE.md` 核心架构） |
| `kind = GONE` | 410 页（`error.vue`） |
| `kind = NOT_FOUND` | 404 页 |
| 连接失败、超时、5xx、`CORE_SERVICE_STARTING` | 503 维护页 + `Retry-After`，**绝不返回 404**（系统设计 21.5） |
| 400（路径格式非法） | 404 页 |
| 401、403、429 等其他 4xx | 视为配置或容量问题（如内部令牌失效）：503 维护页并记录错误，同样不返回 404 |

- **站点引导数据缓存**：进程内 LRU（≤ 500 项），TTL ≤ 60 秒；后端不可用时继续使用过期数据，最长 24 小时（只用于渲染 404 / 503 等页面的外框与文案）。
- **客户端导航**：内容页之间的链接是普通 `<a>`（整页导航，命中 CDN 缓存的 HTML），不做客户端路由取数，因此浏览器端永远不需要内部令牌。悬停预取用 Speculation Rules（`prefetch`，`eagerness: "moderate"`，即悬停触发，系统设计 21.4）。
- **浏览器端的公开接口**（表单提交、站内搜索、同意记录）：同源调用 `/api/public/v1/**`（站点域名的反向代理转发到后端，本地由 Nuxt 的 devProxy 模拟，第 11 章），使用 `@b2bweb/api-client/public`（8.5）。
- Nuxt 不缓存 HTML（系统设计 23.1）；HTML 不得因 Cookie、同意状态、询价篮等访客状态而不同，这类内容只在客户端渲染。

#### 8.3.3 TemplateResolver

每个 `CONTENT` 结果都有模板 key：`content.template`（模板渲染的内容类型取自内容的模板设置，PAGE 取自布局文档的 `template`，系统设计 10.1、10.3）。

| 步骤 | 规则 |
|---|---|
| 1. 模板定义 | 从 `@b2bweb/component-schemas` 读取模板定义（8.4.3）；不存在 → 用基础主题的 `generic` 模板并记录 `template.missing` |
| 2. 模板实现 | 当前主题 `templates[key]` → 基础主题 `templates[key]` → 基础主题 `generic`（按顺序渲染布局中的全部 Section 与插槽）并记录日志 |
| 3. 主题布局 | 模板实现用 `defineTemplate({ layout: 'default' })` 声明使用的主题布局（`default` / `landing` / `blank`，系统设计 11.2） |
| 4. 渲染 | 模板组件收到 `content`（快照中的业务数据 `data`、布局 `layout`）与模板定义；区域用 `<TemplateRegion name="…" />` 渲染 |

`TemplateRegion` 的行为：

| 区域 | 渲染 |
|---|---|
| 固定区域（`fixed`） | 渲染定义中的 `component`（默认变体或模板指定的变体）；业务数据经 `useContentData()` 读取快照；数据源结果取 `dataSources[区域 key]`（如 `related`，系统设计 24.4） |
| 插槽（`slot`） | `<LayoutRenderer :sections="content.layout.slots[name]" />`；布局中没有该插槽时渲染定义中的 `default` |

模板组件不得直接读取 `resolve` 中其他模块的顶层部分；需要时使用对应的组合式函数（如 `useForms()` 读取 `forms`、`useGlobalBlocks()` 读取 `globalBlocks`，4.5.7）。

#### 8.3.4 LayoutRenderer 与 SectionRenderer

`LayoutRenderer` 按顺序渲染 Section 列表；`SectionRenderer` 处理单个 Section：

```text
SectionRenderer(section, depth)
 1. section.hidden = true                         → 不输出（系统设计 10.3）
 2. def = schemas.component(section.component)    → 不存在：不输出，记录 component.unknown
 3. def.module 在当前站点未启用（站点引导数据）    → 不输出（系统设计 3.8）
 4. props = migrate(section.component, section.props, section.componentVersion)   （8.4.4；旧快照即时迁移）
 5. impl = registry.resolve(section.component, section.variant)                     （8.3.6 变体回退；null → 不输出）
 6. 外层 <section id="s-{section.id}" data-section-id class="样式工具类 + 可见性类">
      style → 工具类（b-bg-{值}、b-py-{值}、b-container-{值}，由 design-tokens 生成，8.6.1），不输出内联样式
      visibility → b-hide-mobile / b-hide-tablet / b-hide-desktop（内容仍在 HTML 中，系统设计 10.3）
 7. 注入：props、data = dataSources[section.id]、priority（LCP 候选）、children（布局组件的具名插槽，depth ≤ 2）
 8. 错误边界：渲染异常 → 该 Section 输出为空并记录 section.render_error；编辑器模式另发 preview:error（8.3.13）
```

- **LCP**：页面中第一个 `seo.lcpCandidate = true` 的组件得到 `priority = true`，其图片输出 `fetchpriority="high"` 并预加载（系统设计 10.4、12.3）。
- **按需水合**：变体在主题中声明 `hydrate`（8.3.5）：`none`（默认，静态区块不水合）、`visible`、`idle`、`interaction`；SectionRenderer 用 Vue 3.5 异步组件的懒水合策略实现（系统设计 21.4）。
- 日志事件带 `siteId`、`path`、`sectionId`、组件类型与变体；指标 `website.render.section_errors`（标签：组件类型）。

#### 8.3.5 组件注册表与 defineTheme

规范性签名：

```ts
export interface ThemeDefinition {
  key: string                               // 与 theme.json 的 key 一致
  label: string                             // 中文（后台主题设置显示）
  extends?: 'base'                          // 变体回退的上一级（8.3.6）
  layouts: Record<'default' | 'landing' | 'blank', AsyncComponent>
  header: VariantSet                        // Header 变体（系统设计 10.7）
  footer: VariantSet
  templates: Record<string, AsyncComponent> // 模板 key → 模板实现
  components: Record<string, VariantSet>    // 组件类型 → 变体实现
  texts: () => Promise<Record<string, string>>  // 主题默认中文文案（texts/zh-CN.json）
}

export interface VariantSet {
  default: string                           // 该主题中此组件的默认变体
  variants: Record<string, AsyncComponent | {
    component: AsyncComponent
    hydrate?: 'none' | 'visible' | 'idle' | 'interaction'
  }>
}

export function defineTheme(definition: ThemeDefinition): ThemeDefinition
```

```ts
// frontend/packages/themes/industrial/index.ts（节选）
export default defineTheme({
  key: 'industrial',
  label: '工业',
  extends: 'base',
  layouts: { default: () => import('./layouts/DefaultLayout.vue'), landing: …, blank: … },
  header: { default: 'header-classic', variants: { 'header-classic': () => import('./header/HeaderClassic.vue') } },
  footer: { default: 'footer-columns', variants: { 'footer-columns': () => import('./footer/FooterColumns.vue') } },
  templates: { 'product-detail': () => import('./templates/ProductDetail.vue'), … },
  components: {
    hero: {
      default: 'hero-centered',
      variants: {
        'hero-centered': () => import('./components/page/hero/HeroCentered.vue'),
        'hero-split':    () => import('./components/page/hero/HeroSplit.vue'),
        'hero-video':    { component: () => import('./components/page/hero/HeroVideo.vue'), hydrate: 'visible' },
      },
    },
  },
  texts: () => import('./texts/zh-CN.json'),
})
```

变体组件的 props 约定（全部主题组件统一）：

```ts
export interface SectionComponentProps<P> {
  props: P              // 已迁移到当前版本、并已通过 Schema 的属性
  sectionId: string
  variant: string
  data?: unknown        // 数据源结果 dataSources[sectionId]；失败或没有数据源时为 undefined
  priority: boolean     // LCP 候选
}
```

- 布局组件（`columns`、`tabs`、`accordion-group`）通过具名插槽接收 `children`（如 `<slot name="main" />`），由 SectionRenderer 填充。
- 注册表在构建时检查（`pnpm check:theme`，8.6.2）：组件类型必须存在于 `component-schemas`；变体 key 必须在该组件定义的 `variants` 中；`default` 必须在 `variants` 中；定义中有而主题中没有实现的变体报警告（附录 A.3 第 16 条）。
- 运行时按站点引导数据中的主题 key 选择主题；未知 key 使用基础主题并记录错误。

#### 8.3.6 变体回退

实现系统设计 11.2 的回退顺序，Header、Footer 与组件相同：

| 顺序 | 查找 | 命中后 |
|---|---|---|
| 1 | 当前主题 `components[type].variants[variant]`（`variant` 必须在组件定义的 `variants` 中，否则视为缺失） | 渲染 |
| 2 | 当前主题 `components[type].variants[components[type].default]` | 渲染，记录 `variant.fallback`（warn，含 type、variant、theme） |
| 3 | 基础主题（`extends: 'base'`）中同一变体，再到其默认变体 | 渲染，记录 `variant.fallback` |
| 4 | 都没有 | 不渲染，记录 `variant.missing`（error）；编辑器模式发送 `preview:error` |

- 基础主题 `base` 位于 `website-kit/src/theme/base/`，只提供无品牌样式、符合无障碍要求的最小实现：布局类组件、`rich-text`、`global-block`、`breadcrumb` 与 `generic` 模板；其余组件由具体主题实现。
- `section.variant` 为空时直接使用第 2 步的默认变体（不记录）。

#### 8.3.7 数据源注入与引用解析

| 机制 | 规定 |
|---|---|
| 数据源结果 | Delivery 按 Section ID 把结果放在 `dataSources[sectionId]`，模板固定区域按区域 key（系统设计 10.5、24.4）；SectionRenderer 以 `data` 注入，组件也可以用 `useDataSource(sectionId)` 读取 |
| 每个 Section 一个数据源 | 组件的 `propsSchema` 中至多一个 `x-editor: "data-source"` 属性（8.4.2），保证 `dataSources[sectionId]` 无歧义 |
| 失败 | 单个数据源失败或超时，Delivery 返回空结果；组件按 `data` 为空渲染空状态（文案键）或不输出，页面其余部分正常（系统设计 21.5） |
| 引用 | 结果与属性中的引用一律是 `$content` / `$media` / `$term` 对象，经 `useRefs()` 从 `resolve.refs` 解析：`refs.content(ref)`、`refs.media(ref)`、`refs.term(ref)`、`refs.file(ref)`；解析不到（该语言未发布、已下线）返回 `null`，组件跳过该项——**不回退到其他语言**（`CLAUDE.md` 禁止 17） |
| 图片 | `<B2bImage :media="props.image" :preset :sizes :priority>`：使用 refs 中由后端签名的 URL、`width` / `height`、alt、blurhash；非首屏 `loading="lazy"`（系统设计 12.3）。前台不拼接 imgproxy 签名 |
| 链接 | `<B2bLink :link="props.link">`：`$content` 取 refs 中的路径；外部链接加 `rel="noopener"`，并按链接对象输出 `nofollow` / `sponsored`；没有可用目标时只渲染文字 |
| 禁止 | 前台不得用 ID、slug 自行拼接 URL；不得为缺失的引用发起额外请求 |

前端M0候选的`provideDeliveryRefs`按Vue应用/SSR请求注入当前站点、当前语言引用表；导航后响应式更新，不能使用模块级全局表。`refs.media[id].imageSources[preset]`保存Media已签名的`ImageSources`；`useRefs().image(ref,preset)`只取完全匹配的mediaId/预设，私有、缺失或无效数据返回null。Nuxt的`B2bImage`按上表接收media与preset，底层图片VNode仅接收解析结果。正式Delivery/Media业务提供方的批量装配仍需配对验收，辅助函数不自行发请求。

#### 8.3.8 富文本渲染器

| 层 | 内容 |
|---|---|
| `@b2bweb/shared/richtext` | 节点白名单与类型（标题、段落、列表、表格、图片、视频、引用、提示框、产品卡片、CTA；行内：加粗、斜体、链接、换行，系统设计 9 章）、`content://{id}` 解析、纯文本提取、目录生成；与框架无关的 `renderRichText(doc, { h, resolveLink, components })` |
| `website-kit` 的 `RichTextRenderer` | 以 Vue 的 `h` 调用上述函数生成 VNode；图片走 `B2bImage`，产品卡片走 refs；标题按 `headingOffset` 调整，正文从 H2 开始（H1 留给页面标题）；`useRichTextToc(doc)` 生成长文目录 |

- 白名单之外的节点与标记不渲染并记录日志（不会把未知 HTML 输出到页面）。
- `content://{id}` 链接解析为当前语言版本的 URL；该语言没有已发布版本时降级为纯文本（系统设计 9 章）。外部链接加 `rel="noopener"`，可选 `nofollow` / `sponsored`。
- 渲染器用渲染函数输出 VNode，**不使用 `v-html`**；`vue/no-v-html` 的例外清单只保留渲染器路径，作为系统设计 11.4 规定的唯一例外位置。
- `custom-html` 组件（仅开发者角色，系统设计 10.4、26.3）的输出方式由 page 模块设计文档确定；若需要 `v-html`，须登记到例外清单并经架构负责人批准。

#### 8.3.9 文案键与 useText

```ts
const { t, tp } = useText()
t('form.submit')                          // 文案键（3.7）
t('inquiryCart.added', { name })           // {name} 插值，参数按纯文本插入
tp('inquiryCart.count', count)             // 复数：按 Intl.PluralRules 选 'inquiryCart.count.one' / '.other'，缺失时取 '.other'

const f = useFormat()
f.date(article.publishedAt, 'medium')      // Intl.DateTimeFormat（当前语言 + 站点时区）
f.number(spec.value, { maximumFractionDigits: 2 })
```

| 规则 | 说明 |
|---|---|
| 来源 | 站点引导数据中的界面文案（已是当前语言：`zh-CN` 为站点覆盖 → 主题默认；其他语言为译文，4.4.5、系统设计 21.7） |
| 缺失 | 输出空字符串，并记录一次 `text.missing`（含键与语言）；非生产环境显示 `⟦键⟧` 便于发现；不回退到中文 |
| 默认文案 | 新增文案键时在主题的 `texts/zh-CN.json` 中提供默认中文（8.6.2）；`b2bweb/text-key-exists` 检查键存在（8.1.4） |
| 公开接口错误 | `useApiError(problem)` 按 `code` 映射为 `t('error.{code}')`，字段错误按 `errors[].code` 映射；没有对应文案键时用 `error.generic`；**不展示 `detail`**（系统设计 24.2） |
| `<html>` | `lang` 取 resolve 的语言（BCP 47），`dir` 按语言计算 |
| 禁止 | 组件中硬编码任何用户可见文字（含 `alt`、`aria-label`）；前台渲染时调用 AI（`CLAUDE.md` 语言规则 4） |

#### 8.3.10 SEO head

`useSeoHead(resolve)` 只输出 Delivery 的 `seo` 部分（由 seo 模块的 `DeliveryContributor` 生成，4.5.7），前台不计算任何 SEO 内容（`CLAUDE.md` 禁止 6）：

| 输出 | 来源 |
|---|---|
| `<title>`、`<meta>` | `seo.title`、`seo.meta[]` 原样输出 |
| `<link rel="canonical">` | `route.canonical` |
| `<link rel="alternate" hreflang>` | `seo.alternates[]`（只含已发布且对外公开的语言） |
| `<script type="application/ld+json">` | `seo.jsonLd`；序列化时转义 `<`、`>`、`&`、U+2028、U+2029，防止脚本注入 |
| 预览与编辑器 | 强制 `robots: noindex, nofollow`（ADR-007） |

#### 8.3.11 缓存响应头

动态响应统一服从4.10.3的绝对新鲜度预算，浏览器不持久复用动态页面；CDN使用独立CDN-Cache-Control（RFC9213）。剩余秒数为floor(min(55, freshUntil-now))；<=0或缺少可信freshUntil时no-store。严禁命中后重新计时，所有动态响应不启用stale扩展。

| 响应 | Cache-Control（浏览器） | CDN-Cache-Control | Cache-Tag |
|---|---|---|---|
| CONTENT、分类、搜索外壳、REDIRECT、GONE、404、sitemap、robots | public, max-age=0, must-revalidate | max-age={剩余秒数}；无可信期限则no-store | 当前响应依赖的标签 |
| 预览、编辑器、503/其他5xx、已授权私有下载 | no-store | no-store；503另带Retry-After:120 | 无 |
| 带哈希静态资源（/_nuxt/**） | public, max-age=31536000, immutable | 同左 | 无 |

后端以X-Content-Fresh-Until传递最早依赖期限；Nuxt只信任自有后端生成的头，不能采信浏览器同名请求头。CDN需转发Age并尊重剩余寿命。只有website-kit/src/http可设置页面响应头；组件不能覆盖缓存规则。部署由业主负责，其他CDN按等价语义配置。

#### 8.3.12 503、404、410 与错误页

| 情况 | 状态码 | 页面 | 说明 |
|---|---|---|---|
| Delivery `NOT_FOUND` | 404 | 主题 `default` 布局 + 404 内容（文案键 `error.notFound.*`），含站内搜索框与首页链接 | 站点引导数据可用时带 Header / Footer |
| Delivery `GONE` | 410 | 同上（`error.gone.*`） | 系统设计 13.6 的下线策略 |
| 后端不可用、超时、5xx | 503 + `Retry-After: 120` | 主题 `blank` 布局的维护页（`error.unavailable.*`） | **绝不返回 404**（系统设计 21.5） |
| 渲染异常（Nuxt 内部错误） | 500 | 同 503 维护页 | 上报 Sentry |
| 单个 Section 渲染失败 | 200 | 该 Section 为空 | 8.3.4 |

- 错误页的文案同样来自站点引导数据；连过期的引导数据都没有时（如进程刚启动且后端不可用），使用主题默认中文文案渲染维护页——这是唯一允许不按当前语言显示的情况。
- Nuxt 健康检查 `/__health`（系统设计 27.4）：进程存活即 200；`/__health?deep=1` 另检查后端 readiness，供部署探针选择使用。

#### 8.3.13 预览与编辑器画布

**预览**（系统设计 6.5）：`/__preview/[token]` 在服务端调用 `GET /api/public/v1/delivery/preview?token=…`（带内部令牌），按 `CONTENT` 渲染，顶部显示预览横幅（文案键 `preview.*`）；响应 `noindex, nofollow` 与 `no-store`（ADR-007）。

**编辑器画布**（系统设计 10.8、ADR-007）：`/__editor?token=…`

入口必须先验证PreviewTarget.purpose=EDITOR及issuedBy的当前编辑权限/语言范围；PREVIEW/SHARE不能用于画布或partOverrides。用途不符返回404、鉴权服务故障返回503，均no-store。见ADR-030。

| 项 | 规定 |
|---|---|
| 渲染方式 | 客户端渲染（路由规则 `ssr: false`）；`noindex, nofollow`、`no-store` |
| 嵌入限制 | 该路由的 CSP `frame-ancestors` 只允许 `NUXT_PUBLIC_ADMIN_ORIGIN`；其他前台路由为 `'self'`（系统设计 21.6） |
| 数据 | 布局文档由 Admin 持有并下发；数据源与引用由画布带编辑器令牌调用 Delivery 的预览接口解析，接口在请求中携带未保存的布局（`ContentApi.assemblePreview` 的 `partOverrides`，4.4.16），具体端点由 delivery 模块设计文档定义；不得新增绕过 `PublishContributor` 的草稿读取接口（ADR-007） |
| 重新解析 | 结构变化（增删 Section、改变组件或变体）以及 `$source` / `$media` / `$content` / `$term` 变化时重新请求解析（防抖 300 ms）；纯文本与样式变化只在本地重渲染 |
| 覆盖层 | 选中框、悬停框、插入点按钮由画布内的覆盖层绘制；`AUTO` 模式的目标语言只能预览，覆盖层不显示插入点（系统设计 10.8） |

消息协议（规范性；类型定义在 `@b2bweb/shared/editor-protocol`，Admin 与画布共用）：

```ts
export interface EditorEnvelope<T extends string, P> {
  source: 'b2bweb-editor'   // 非此值的消息直接忽略
  version: 1                // 协议版本；不一致时画布发送 preview:error 并停止处理
  type: T
  payload: P
}
```

| 方向 | `type` | `payload` | 说明 |
|---|---|---|---|
| Admin → 画布 | `editor:init` | `{ document: LayoutDocument, locale: string, device: 'desktop' \| 'tablet' \| 'mobile' }` | 初始化；也用于整体重置 |
| Admin → 画布 | `editor:patch` | `{ ops: JsonPatchOperation[] }` | RFC 6902 增量更新 |
| Admin → 画布 | `editor:select` / `editor:scrollTo` | `{ sectionId: string \| null }` | 同步选中 / 滚动 |
| 画布 → Admin | `preview:ready` | `{ protocolVersion: 1 }` | 画布加载完成、令牌有效 |
| 画布 → Admin | `preview:select` / `preview:hover` | `{ sectionId: string \| null }` | 画布内点击 / 悬停 |
| 画布 → Admin | `preview:insert` | `{ parentId: string \| null, slot: string \| null, index: number }` | 点击插入点：顶层 Section 列表为 `parentId = null, slot = null`；模板插槽为 `parentId = null, slot = 区域 key`；布局组件内部为 `parentId = 布局组件的 Section ID, slot = children 名` |
| 画布 → Admin | `preview:error` | `{ sectionId: string \| null, message: string }` | 渲染错误、变体缺失、协议不匹配；`message` 为中文（显示在后台） |

- 双方都校验 `origin`：画布只接受 `NUXT_PUBLIC_ADMIN_ORIGIN` 的消息；Admin 只接受 iframe 所加载站点源（站点主域名）的消息；只处理上表中的类型。
- 消息与画布状态的往返由 Vitest 契约测试覆盖（`shared/editor-protocol` 的类型守卫 + 画布消息桥的单元测试，9.6.1）；新增或修改消息时 Admin 与 Nuxt 在同一个 PR 中修改（ADR-007）。

### 8.4 组件与模板 Schema 包（@b2bweb/component-schemas）

组件与模板定义只有一份，Admin（生成表单、校验）、Nuxt（迁移与渲染）、后端（保存校验、翻译单元提取、`x-ref` 收集）共用（系统设计 10.4、ADR-007）。

#### 8.4.1 目录

```text
frontend/packages/component-schemas/
├── meta/
│   ├── component.schema.json        # 组件定义的元 Schema（8.4.2）
│   └── template.schema.json         # 模板定义的元 Schema（8.4.3）
├── defs/
│   ├── common.json                  # 公共 $defs：mediaRef、contentRef、termRef、link、cta、richText、richTextInline、dataSource、styleValue
│   └── style-options.json           # 样式选项与可选值（由 design-tokens 生成并提交，8.6.1）
├── components/<模块 ID>/
│   ├── <组件类型>.json               # 组件定义（文件名 = type）
│   ├── migrations/<组件类型>.ts       # 迁移函数（version > 1 时必需，8.4.4）
│   ├── history/<组件类型>.v<N>.json   # 破坏性变更前的归档定义（8.4.4）
│   ├── examples/<组件类型>.json        # 画廊示例（9.6.2）
│   └── __fixtures__/<组件类型>/        # 迁移测试的输入与期望输出
├── templates/<模块 ID>/<模板 key>.json  # 模板定义
├── src/                             # 类型、加载、validate()、migrate()、defineMigrations()
└── scripts/                         # check（8.4.6）、gen-types（由元 Schema 生成 TS 类型）
```

目录按模块 ID 划分，与 CODEOWNERS 对应（2.6、3.6）。

#### 8.4.2 组件定义（规范性）

在系统设计 10.4 示例的基础上，字段如下（`meta/component.schema.json` 校验；未列出的顶层字段视为错误）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `type` | string | 是 | 组件类型，小写连字符（3.7），全局唯一，等于文件名 |
| `version` | integer ≥ 1 | 是 | 属性结构的版本；破坏性变更时 +1（8.4.4） |
| `module` | string | 是 | 所属模块 ID，等于所在目录；与该模块 `module.json` 的 `components` 一一对应（附录 A.3 第 16 条）；模块停用时组件在编辑器面板隐藏、前台不渲染（系统设计 3.8） |
| `category` | enum | 是 | `layout`、`marketing`、`content`、`product`、`lead`、`system`（对应系统设计 10.4 的组件分类：布局、营销、内容、产品、获客、系统） |
| `label` | string | 是 | 中文名称（至少含一个汉字）；不得是按语言区分的对象（`CLAUDE.md` 语言规则 1） |
| `description` | string | 否 | 中文说明，组件面板的提示 |
| `variants` | string[] | 是 | 变体 key（小写连字符），非空；第一个为插入时的默认变体；主题中的默认变体由主题决定（8.3.5） |
| `variantAliases` | object | 否 | 已删除或改名的变体 → 替代变体，供旧布局渲染与迁移（8.4.4） |
| `propsSchema` | JSON Schema | 是 | draft 2020-12；根为 `type: object`、`additionalProperties: false`；引用、富文本、链接、数据源一律 `$ref` 到 `defs/common.json` |
| `styleOptions` | string[] | 是 | 允许的样式项（如 `background`、`paddingY`、`container`），必须是 `defs/style-options.json` 中的 key；没有时为 `[]` |
| `seo` | object | 否 | `{ "lcpCandidate": boolean }`：位于首屏时其图片设置 `fetchpriority="high"` 并预加载（系统设计 10.4） |
| `allowedIn` | string[] | 是 | 可以放置的位置：`page`（页面布局顶层）、`slot`（模板插槽）、`children`（布局组件内部）、`global-block`（全局区块的布局） |
| `children` | object | 布局类必填 | `{ "slots": ["main", "aside"], "allowed": "*" \| string[] }`；只有 `category = layout` 可以声明；嵌套深度 ≤ 2（系统设计 10.3） |
| `restricted` | string | 否 | 插入与编辑需要的权限码（如 `custom-html` 只给开发者角色，系统设计 26.3）；框架补充 |
| `deprecated` | boolean | 否 | 已废弃：组件面板隐藏，渲染器保留，直到 `pg_component_usage` 显示已无已发布内容使用（系统设计 10.9） |
| `migrations` | string | `version > 1` 时必填 | 迁移函数文件的相对路径 |

**属性级关键字**（`propsSchema` 内）：

| 关键字 | 取值 | 含义 |
|---|---|---|
| `title` | 中文字符串 | 必填，SchemaForm 的标签 |
| `description` | 中文字符串 | 帮助文字 |
| `x-editor` | 8.2.10 的控件名 | 编辑控件；可推断时可以省略 |
| `x-translatable` | boolean | 是否作为翻译单元提取（系统设计 5.6、10.4）。默认值：`string` 与富文本为 `true`；`enum`、`const`、`format: uri` / `email`、带 `pattern` 的代码类字符串必须为 `false`（显式写出）；`$media` / `$content` / `$term` / `$source` 引用、数字、布尔不翻译。`$defs` 中的复合类型在内部字段上标注（如 `cta.label` 可翻译、`cta.link` 不翻译）。`maxLength` 同时是翻译单元的长度上限 |
| `x-seo` | `heading` / `text` / `image` | 供 SEO 健康检查：`heading` 是标题文本（层级取同一组件的 `headingLevel` 属性，没有时为 h2）；`text` 计入正文字数；`image` 检查 alt |
| `x-media` | `{ "kinds": [...], "requireAlt": boolean }` | 媒体限制 |
| `x-content` / `x-term` | `{ "types": [...] }` / `{ "vocabulary": "…" }` | 引用候选范围 |
| `x-ref` | 引用种类，如 `form`、`global-block` | 属性值是另一模块的业务 key；delivery 按它收集 `referencedKeys`（4.5.7），Admin 用对应的 `RefSourceDefinition` 提供候选（8.2.8）；框架补充，同步系统设计 10.4 |
| `x-source` | `{ "providers": [...] }` | 数据源白名单（可选） |
| `x-group`、`x-order`、`x-visible-if`、`x-enum-labels`、`x-placeholder` | 见 8.2.10 | 只影响表单展示（中文） |

**公共 `$defs`**（`defs/common.json`）：

| 名称 | 值形态 | 翻译 |
|---|---|---|
| `mediaRef` | `{ "$media": "<id>" }` | 否（alt 属于媒体的站点资源，由 media 登记，4.5.21） |
| `contentRef` | `{ "$content": "<id>" }` | 否 |
| `termRef` | `{ "$term": "<id>" }` | 否 |
| `link` | `{ "$content": "<id>" }` 或 `{ "href": "https://…", "rel": ["nofollow" \| "sponsored"] }` | 否 |
| `cta` | `{ "label": string, "link": link, "style": "primary" \| "secondary" }` | `label` 是 |
| `richText` / `richTextInline` | ProseMirror JSON（节点白名单见 8.3.8） | 是（按块级节点拆分，系统设计 9 章） |
| `dataSource` | `{ "$source": { "provider": "<key>", "params": { … } } }` | 否 |
| `styleValue` | 某样式项的 Token 枚举 | 否 |

**附加约束**（`pnpm check:schemas` 检查，8.4.6）：

1. 任何属性都不得保存 URL 字面量来指向站内对象；媒体、内容、分类、数据源只能使用上表的引用对象（系统设计 10.3；`CLAUDE.md` 禁止 4）。外部链接只能经 `link.href`。
2. 每个组件至多一个 `x-editor: "data-source"` 属性（8.3.7）。
3. 样式只能经 `styleOptions` 与 Token 枚举，不得出现 `style`、`className`、颜色字面量类的属性（系统设计 10.3）。
4. 不得出现硬编码的界面文字默认值：属性的 `default` 只能是非文本值；组件的固定文字在主题中使用文案键（8.3.9）。
5. 每个组件至少有一个画廊示例（`examples/<type>.json`），示例通过 `propsSchema` 校验，并覆盖全部变体。

示例（节选，系统设计 10.4 的 hero 补齐必填字段）：

```json
{
  "$schema": "../../meta/component.schema.json",
  "type": "hero",
  "version": 2,
  "module": "page",
  "category": "marketing",
  "label": "首屏横幅",
  "variants": ["hero-centered", "hero-split", "hero-video"],
  "propsSchema": {
    "type": "object",
    "additionalProperties": false,
    "required": ["title"],
    "properties": {
      "eyebrow":      { "title": "眉标", "type": "string", "maxLength": 60, "x-group": "content" },
      "title":        { "title": "标题", "type": "string", "maxLength": 120, "x-seo": "heading", "x-group": "content" },
      "headingLevel": { "title": "标题层级", "enum": ["h1", "h2"], "default": "h2", "x-translatable": false,
                        "x-enum-labels": ["一级标题", "二级标题"], "x-group": "advanced" },
      "description":  { "title": "说明", "$ref": "../../defs/common.json#/$defs/richTextInline", "x-editor": "richtext-inline" },
      "image":        { "title": "图片", "$ref": "../../defs/common.json#/$defs/mediaRef", "x-editor": "media",
                        "x-media": { "kinds": ["IMAGE"], "requireAlt": true }, "x-seo": "image" },
      "primaryCta":   { "title": "主按钮", "$ref": "../../defs/common.json#/$defs/cta" }
    }
  },
  "styleOptions": ["background", "paddingY", "container"],
  "seo": { "lcpCandidate": true },
  "allowedIn": ["page", "slot"],
  "migrations": "./migrations/hero.ts"
}
```

#### 8.4.3 模板定义（规范性）

格式同系统设计 10.2；后端由 page 读取并转换为 `TemplateDefinition`（4.5.5）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `key` | string | 是 | 模板 key，小写连字符，全局唯一，等于文件名 |
| `version` | integer ≥ 1 | 是 | 区域结构变化（删除或改名区域、收紧 `allowed`）时 +1 |
| `contentTypes` | string[] | 是 | 适用的内容类型（大写下划线） |
| `label` | string | 是 | 中文名称 |
| `legal` | boolean | 否（默认 `false`） | 法律页面模板：目标语言的机器译文不自动发布（系统设计 5.5、10.1；`CLAUDE.md` 禁止 15） |
| `regions[]` | array | 是 | 区域列表，顺序即默认渲染顺序 |

`regions[]` 的元素：

| 字段 | 说明 | 对应 `TemplateRegion`（4.5.5） |
|---|---|---|
| `key` | 区域 key（lowerCamelCase），模板内唯一 | `key` |
| `fixed: true` / `slot: true` | 二选一 | `kind = FIXED` / `SLOT` |
| `component` | 固定区域渲染的组件类型（必须存在） | `component` |
| `source` | 固定区域的数据源 `{ provider, params }`（读取时解析，修改不需要重新发布内容，系统设计 10.2） | `source` |
| `allowed` | 插槽允许的组件类型；每个类型的 `allowedIn` 必须包含 `slot` | `allowed` |
| `max` | 插槽最多放置的组件数 | `max` |
| `default` | 插槽的默认 Section 列表（结构同布局文档的 `sections`，每项须通过对应组件的 Schema） | `defaults` |

模板的所属模块由所在目录决定（`templates/<模块 ID>/`）；模板实现位于主题的 `templates/`（8.6.2）。

#### 8.4.4 迁移函数

```ts
// frontend/packages/component-schemas/components/page/migrations/hero.ts
import { defineMigrations, inlineFromText } from '@b2bweb/component-schemas'

export default defineMigrations('hero', {
  /** 1 → 2：subtitle（纯文本）改为 description（行内富文本） */
  1: ({ subtitle, ...rest }) => ({
    ...rest,
    ...(subtitle ? { description: inlineFromText(subtitle) } : {}),
  }),
})
```

```ts
// 运行时 API
migrate(type: string, props: unknown, fromVersion: number): { props: unknown; version: number }   // 逐步链式执行到当前版本
resolveVariant(type: string, variant: string): string                                               // 按 variantAliases 映射
```

| 规则 | 说明 |
|---|---|
| 签名 | 每一步 `(props) => props`，键为起始版本（系统设计 10.9 的 `(props, fromVersion) => props` 由 `migrate` 组合） |
| 纯函数 | 不访问网络、存储、时间与随机数；不改写文本内容本身（只调整结构），不丢弃 `$media` / `$content` / `$term` / `$source` 引用。M0候选逐步冻结输入并比对引用集合，拒绝异步/非法结果；每步复制结果后才继续 |
| 测试 | 每一步在 `__fixtures__/<type>/` 中有输入与期望输出，Vitest 逐步验证（9.6.1）；迁移结果必须通过当前版本的 `propsSchema` |
| 归档 | 破坏性变更时，把变更前的定义复制到 `history/<type>.v<N>.json`（检查脚本校验存在） |
| 执行方 | Admin 打开草稿时执行（保存的总是最新版本）；Nuxt 渲染旧快照时即时执行（旧内容无需重新发布）（系统设计 10.9）；后端不执行 TypeScript 迁移 |
| 后端如何处理旧版本 | 保存时只接受当前版本并用当前 Schema 校验；读取已发布的旧版本布局（如翻译提取、`x-ref` 收集、SEO 检查）时，按 Section 的 `componentVersion` 使用 `history/` 中对应版本的定义 |
| 变体改名或删除 | 在 `variantAliases` 中登记映射，视为破坏性变更 |

**破坏性变更**（`pnpm check:schemas --base origin/main` 判定）：删除或改名属性、改变类型、新增必填属性、收紧约束（`maxLength` 变小、`enum` 减少、`pattern` 变化）、改变 `$ref` 目标、删除或改名变体、从 `allowedIn` 中移除位置。破坏性变更必须同时：`version` 恰好 +1、提供该步迁移与测试、归档旧定义（系统设计 10.9、29.3；`CLAUDE.md` 提交前检查）。新增可选属性、放宽约束、新增变体不是破坏性变更，版本不变。模板的区域变化按同样规则判定（模板没有迁移函数：删除区域时插槽中的组件在渲染时忽略，并由 page 模块提供数据修正，见其模块设计文档）。

客户端Schema索引由`pnpm check:schemas --write-index`从当前JSON文件生成；普通检查对比静态导入目录，不能只增加JSON却遗漏Admin/Nuxt注册。主题套件枚举当前全部Schema变体并实际加载，禁止用默认变体回退掩盖缺失。

#### 8.4.5 后端加载

| 环节 | 规定 |
|---|---|
| 复制 | `application` 的 `pom.xml` 用 `maven-resources-plugin` 在 `generate-resources` 阶段复制 `frontend/packages/component-schemas/{meta,defs,components,templates}/**/*.json`（不含 `examples/`、`__fixtures__/`）到 `target/classes/component-schemas/`；同时复制主题默认文案 `frontend/packages/themes/<key>/texts/zh-CN.json` 到 `target/classes/theme-texts/<key>/zh-CN.json`（5.11.4）。复制的是源 JSON，后端构建不需要运行 pnpm |
| 模块测试 | 需要真实定义的模块（page、delivery、seo）在自己的 `pom.xml` 中把同一目录加为测试资源（`<testResources>`），不依赖 `application`；4.5.5 中“复制到 page 模块的 classpath”据此细化为“运行时位于 application 的 classpath，测试时作为 page 等模块的测试资源” |
| 加载器 | `ComponentSchemaCatalog`（框架补充：platform-core 开发支撑包 `com.b2bweb.core.support.schema`，同步 5.1.2 的支撑包清单）：启动时加载全部定义并用元 Schema 校验，失败则拒绝启动（中文错误信息，含文件与 JSON Pointer）；编译每个 `propsSchema`（含 `history/` 版本）；只读，部署后不变 |
| 提供的能力 | `component(type)` / `component(type, version)`；`template(key)` / `templates()`；`validateLayout(layout, scope)`（Section 结构、组件存在且允许出现在该位置、`componentVersion` 等于当前版本、属性通过 Schema、嵌套深度、样式值为合法 Token）；`translatablePaths(type, version)`；`refKeys(layout, kind)`；`moduleOf(type)` |
| 使用方 | page：保存校验、`TranslatableContentProvider`（按 `x-translatable` 提取与回写）、`TemplateProvider`、`pg_component_usage`；delivery：`referencedKeys`（`x-ref`）、按 `module` 过滤停用模块的组件；seo：`x-seo` 健康检查 |
| 一致性 | 启动时检查每个定义的 `module` 都是已知模块（不在构建中的模块只警告）；`module.json` 的 `components` 与定义的对应关系由 CI 检查（附录 A.3 第 16 条） |

#### 8.4.6 校验命令

`pnpm check:schemas`（CI 门禁 F6，第 10 章）依次执行：

| # | 检查 | 失败条件 |
|---|---|---|
| 1 | 元 Schema | 组件或模板定义不通过 `meta/*.schema.json` |
| 2 | 唯一与位置 | `type` / `key` 重复；文件名与 `type` 不一致；`module` 与目录不一致 |
| 3 | 中文标签 | `label`、属性 `title` 不含汉字，或是对象 |
| 4 | 翻译标注 | 违反 8.4.2 的 `x-translatable` 默认规则（如枚举标为 `true`、URL 字符串未标 `false`） |
| 5 | 引用与样式 | 违反 8.4.2 附加约束 1–4；`styleOptions` 不在 `style-options.json` 中；`defs/style-options.json` 与 `design-tokens` 的生成结果不一致 |
| 6 | 模板 | 区域 key 重复；固定区域组件不存在；插槽 `allowed` 中的组件不允许出现在 `slot`；`default` 不通过 Schema |
| 7 | 迁移 | `version > 1` 而缺少迁移文件或某一步；迁移测试失败（调用 Vitest） |
| 8 | 破坏性变更 | 与 `origin/main` 比较，发现破坏性变更而版本未 +1、缺少迁移或归档（8.4.4） |
| 9 | 示例 | 缺少示例；示例不通过 Schema；示例未覆盖全部变体 |

### 8.5 api-client 生成与使用

#### 8.5.1 生成流程

```text
后端（springdoc，按 admin / public 分组，5.5.1；int64 → string，5.5.4）
   │  ./mvnw -B -ntp -pl application -am verify -Popenapi -DskipUTs
   │     OpenApiSnapshotIT：以 test profile 启动完整应用（Testcontainers），读取 /v3/api-docs/admin 与 /v3/api-docs/public，
   │     按路径与 schema 名排序后写入：
   ▼
docs/api/admin.openapi.json   docs/api/public.openapi.json        （快照，提交；oasdiff 的比较对象，第 10 章）
   │  pnpm gen:api            （openapi-typescript，配置固定在 packages/api-client/openapi-ts.config.ts）
   ▼
frontend/packages/api-client/src/generated/admin.ts   public.ts  （提交；文件头注明“生成文件，不得手改”）
```

- 一条命令完成两步：`node tools/dev/b2b.mjs api`（第 11 章）。
- `/api/internal/**` 不生成前端类型（5.5.1）。
- 涉及 API 的 PR 必须重新生成并提交快照与生成代码（`CLAUDE.md` 提交前必须通过）；CI 重新生成并比较，不一致即失败，并用 oasdiff 检查破坏性变更（10.2 B9、B10）。

#### 8.5.2 包结构与使用

```text
frontend/packages/api-client/src/
├── generated/admin.ts  generated/public.ts   # 生成代码：paths、components 类型
├── admin.ts          # createAdminClient(options)：openapi-fetch Client<AdminPaths>，不含中间件（由 admin-kit 装配，8.2.2）
├── public.ts         # createPublicClient(options)
├── problem.ts        # ProblemDetail 类型（RFC 9457 + code、traceId、errors[]、扩展字段）
└── index.ts          # 类型别名：AdminSchemas、PublicSchemas、AdminPaths、PublicPaths
```

| 使用方 | 方式 |
|---|---|
| Admin 模块 | 只经 `admin-kit` 的 `useApi()`（带站点、CSRF、错误处理中间件，8.2.2）；类型用 `AdminSchemas['InquiryResponse']` |
| website-kit（服务端） | Delivery 调用（resolve、site、preview）用 Nuxt 的 `$fetch` 加内部令牌头，响应类型取自 `PublicSchemas` |
| 浏览器端公开接口 | `createPublicClient({ baseUrl: '' })` 同源调用（表单提交、搜索、同意记录，8.3.2）；错误经 `useApiError` 映射为文案键 |
| 测试 | `@b2bweb/admin-kit/testing` 基于 MSW 提供按 `AdminPaths` 类型检查的接口模拟（9.6.1） |

| 规则 | 说明 |
|---|---|
| 不手写接口类型 | 前端不得手写后端请求 / 响应的 TypeScript 类型；需要的类型都从 `api-client` 取 |
| 不手改生成代码 | `src/generated/**` 列入 ESLint 与 Prettier 的忽略清单；CI 比较生成结果（B9） |
| ID 与时间 | ID 为 `string`（ADR-014），时间为 ISO-8601 字符串；前端不得把 ID 转为 `number` |
| 破坏性变更 | 接口只做兼容新增；破坏性变更需要新版本路径与 `Sunset` 头（系统设计 24.2），oasdiff 阻断（B10） |

### 8.6 design-tokens 与主题包

#### 8.6.1 @b2bweb/design-tokens

```text
frontend/packages/design-tokens/
├── tokens/
│   ├── primitive/     # color、font、space、radius、shadow、border、motion、z-index、opacity、breakpoint、container（DTCG JSON）
│   ├── semantic/      # color.text.default、color.surface.alt、space.section.md、radius.card …（只引用 primitive）
│   └── component/     # button.primary.bg …（只引用 semantic）
├── style-options.ts   # 编辑器样式项定义：样式项 → 可选的 semantic Token + 中文显示名
├── build.ts           # Style Dictionary 配置
└── dist/              # 构建产物（不提交）
```

| 产物 | 内容 | 使用方 |
|---|---|---|
| `dist/tokens.css` | `:root { --color-brand-primary: …; … }`，变量名为 Token 路径以 `-` 连接（系统设计 11.1） | `website-kit`、主题 |
| `dist/utilities.css` | 样式项的工具类：`b-bg-{值}`、`b-py-{值}`、`b-container-{值}`，可见性类 `b-hide-{mobile\|tablet\|desktop}`；只引用 CSS 变量 | SectionRenderer（8.3.4） |
| `dist/tokens.ts` | Token 常量与类型 | 主题、画廊 |
| `dist/style-options.json` | 样式项 → 可选值与中文显示名 | 复制为 `component-schemas/defs/style-options.json`（提交，后端校验样式值，8.4.5） |
| `dist/docs/` | Token 文档页 | 开发者查阅 |

| 规则 | 说明 |
|---|---|
| 唯一字面量位置 | 颜色、间距等字面量只能出现在 `tokens/`（Stylelint 对本包豁免，8.1.4）；新增 Token 只能在本包中修改（`CLAUDE.md` 禁止 5） |
| 分层引用 | semantic 只引用 primitive，component 只引用 semantic / component；构建时检查，违反即失败 |
| 断点与设备 | 断点 Token 与编辑器设备尺寸（1440 / 768 / 390，系统设计 10.8）对应 |
| 减弱动画 | motion Token 提供 `prefers-reduced-motion` 下的取值（系统设计 11.5） |
| 生成物同步 | `pnpm gen:tokens` 后若 `style-options.json` 变化，须同步提交 `component-schemas/defs/style-options.json`（检查 F6 第 5 项） |

#### 8.6.2 主题包结构

在系统设计 11.2 的基础上细化：

```text
frontend/packages/themes/industrial/
├── package.json        # @b2bweb/theme-industrial
├── theme.json          # 主题元数据（下表）
├── tokens/             # 覆盖 semantic / component 层（DTCG），构建为 dist/theme.css，作用域 [data-theme="industrial"]
├── fonts/              # 自托管 woff2 子集（拉丁字体；中文使用系统字体栈，系统设计 11.6）
├── layouts/            # DefaultLayout、LandingLayout、BlankLayout
├── header/  footer/    # 变体
├── templates/          # 模板实现：ProductDetail.vue、ArticleDetail.vue、CaseDetail.vue、TermPage.vue、Legal.vue …
├── components/<模块 ID>/<组件类型>/<组件类型的 Pascal 形式><变体>.vue   # 如 components/page/hero/HeroSplit.vue（3.6）
├── texts/zh-CN.json    # 主题默认中文文案：文案键 → 文本（复制到后端 classpath，5.11.4）
├── index.ts            # defineTheme（8.3.5）
└── __tests__/
```

`theme.json`：

| 字段 | 说明 |
|---|---|
| `key`、`label` | 主题 key；中文名称 |
| `extends` | 回退的基础主题，V1 为 `base` |
| `adjustableTokens` | 站点可以覆盖的 Token 白名单（品牌色、圆角风格等，系统设计 11.3） |
| `fonts` | 可选的拉丁字体列表（标题、正文），每项含字体文件与 `size-adjust` 回退参数 |
| `options` | 主题设置项的 JSON Schema（Header / Footer 变体、是否吸顶、是否显示语言切换、右上角 CTA 等，系统设计 10.7），由 SchemaForm 渲染 |
| `defaults` | 主题设置的默认值 |

| 规则 | 说明 |
|---|---|
| 品牌定制 | 站点的品牌设置经站点引导数据下发；SSR 在 `<head>` 中注入只含白名单 Token 的 `:root{--color-brand-primary:…}`（带 CSP nonce）；值由后端校验（含对比度 ≥ 4.5:1，系统设计 11.3），前端再按格式校验（只接受颜色 / 枚举值，防止 CSS 注入）；派生色用 `color-mix(in oklch, …)` |
| 样式 | 组件只使用 CSS 变量与工具类；不得写颜色、间距字面量，不得新建 Token（8.1.4） |
| 文案 | 固定文字一律文案键；新键在 `texts/zh-CN.json` 中提供默认中文（只有中文，其他语言由翻译流程生成，`CLAUDE.md` 语言规则 4） |
| 无障碍 | 语义化标签与地标、可见焦点、跳到主内容链接、表单标签与错误提示、图片 alt、尊重 `prefers-reduced-motion`（系统设计 11.5）；画廊的 axe 检查覆盖每个变体（9.6.2） |
| 一致性检查 | `pnpm check:theme`：组件类型与变体存在于 `component-schemas`；变体文件命名与目录符合 3.6；`texts/zh-CN.json` 覆盖主题与 `website-kit` 中使用的全部文案键；每个变体有画廊截图基线 |

#### 8.6.3 V1 主题视觉基线

V1 的 `industrial` 主题按系统设计 11.7 和[前台视觉设计方向与 UI 基线](../design/visual-direction.md)细化：参考 Tesla 的影像主导、黑白灰、留白、简洁导航与清晰 CTA，面向企业产品参数、资料下载和询盘场景。

视觉参数先在 design-tokens 中完成 primitive/semantic/component 映射，再由主题变体消费。Header 的 `overlay` / `solid` 为建议的呈现变体，后续通过既有主题设置声明；Header/Footer 仍是固定区域。不得通过自由样式、布局硬编码颜色或新增重复组件来实现该方向。

编辑器标准预览沿用 1440/768/390，补测 360px、短屏、长文案及 200% 文字缩放。画廊、axe、截图和性能验收仍依第 9、10 章执行。Admin 继续使用 Element Plus 独立主题变量，iframe 使用真实前台主题。

本节确认方向与交付要求；高保真稿和组件实现尚未交付，不改变第 4 章契约、DTO、路由或组件类型清单。

---

## 9. 测试框架与测试替身

本章细化系统设计 28 章的测试策略，并给出 ADR-018 要求的 `platform-testkit`：模块只依赖 `platform-api` 与 testkit 中的测试替身即可独立开发与测试（2.4）。契约测试的规则见 4.8，本章说明测试的组织、工具与替身清单。

### 9.1 测试分层与约定

| 层级 | 工具 | 位置与命名 | 运行阶段 | 门禁（第 10 章） |
|---|---|---|---|---|
| 单元 | JUnit 5 + AssertJ + testkit 替身 | 模块 `src/test`，`<被测类>Test` | Surefire（`./mvnw verify` 的 test 阶段） | 必须通过；领域层行覆盖率 ≥ 80%（JaCoCo，包 `com.b2bweb.*.domain..`） |
| 架构 | ArchUnit（testkit 规则集）、Spring Modulith `verify()` | 每个模块的 `ArchitectureTest`；`application` 的 `ModularityTest` | Surefire | 必须通过（9.5） |
| 契约 | testkit 契约套件（4.8） | 提供方 `<接口名>ContractTest`；infrastructure `<适配器类名>ContractTest`；实现方 `<实现类名>ConformanceTest` | 替身侧：Surefire；真实实现侧：Failsafe | 必须通过 |
| 模块集成 | `@ModuleIntegrationTest`（`@ApplicationModuleTest` + Testcontainers，9.3） | 模块 `src/test`，`<场景>IT` | Failsafe（`integration-test` 阶段） | 必须通过 |
| AI 翻译 | 模块集成 + `FakeTranslationEngine` | translation 模块 | Failsafe | 必须通过（系统设计 28） |
| 站点隔离 | `AdminSiteIsolationSuite`（9.3.4） | 每个有后台接口的模块 | Failsafe | 必须通过 |
| 跨模块集成 | 完整应用 + Testcontainers | `application` 的 `src/test`（9.3.5） | Failsafe | 必须通过 |
| 前端单元 / 组件 | Vitest | 各包 `__tests__/`、`*.test.ts` | `pnpm test` | 必须通过 |
| 组件视觉 | Playwright 截图 `/__gallery` | `frontend/e2e/visual/` | PR（涉及主题时）、夜间 | 差异需人工确认（9.6.2） |
| E2E | Playwright | `frontend/e2e/scenarios/` | main（冒烟）、发布（全量） | staging 必须通过 |
| SEO、性能、安全、无障碍 | `tools/seo-check`、Lighthouse CI、k6、ZAP、axe-core | 第 10 章 | 发布流水线 | 见 10.5 |

约定：

| 约定 | 说明 |
|---|---|
| 单元与集成的划分 | 按 JUnit 标签而不是只按类名：`@ModuleIntegrationTest`、`@ModuleContractTest`、`@SitePersistenceTest` 都带 `@Tag("integration")`，Surefire 排除、Failsafe 只运行该标签；因此真实实现侧的 `<接口名>ContractTest` 虽以 `Test` 结尾，也在 Failsafe 中运行 |
| 确定性 | 时间只来自注入的 `Clock`（测试中为 `ManualClock`）；随机数要有固定种子；ID 需要稳定时用 `FakeIdGenerator`（9.2.7）；不得 `Thread.sleep` 等待异步结果（用 Scenario API 或 Awaitility） |
| 不访问外部网络 | 只使用 Testcontainers 与进程内模拟服务器；AI 引擎、Turnstile、Webhook、CDN、IndexNow 一律假实现或本地模拟（系统设计 28“AI 翻译”一行，4.8.1） |
| 测试数据 | 每个测试自己准备数据，不依赖执行顺序；跨测试共享的只有只读夹具（9.7） |
| 语言 | `@DisplayName` 与断言消息用中文；测试数据中的用户可见文本可以是任意语言 |

### 9.2 platform-testkit

#### 9.2.1 结构与依赖

testkit 只依赖 `platform-api` 与测试库（JUnit 5、AssertJ、ArchUnit、Awaitility、Spring Test 与 Spring Context），不依赖 platform-core（3.3 说明 3）；需要 platform-core 运行时的测试支撑在 core 的 test-jar 中（`com.b2bweb.core.testsupport`，5.1.2）。

```text
backend/platform/platform-testkit/src/main/java/com/b2bweb/testkit/
├── TestPlatform.java              # 全部替身的装配入口（9.2.2）
├── site/  security/  common/      # 服务接口替身，按 platform-api 的包划分（9.2.3）
├── content/  url/  media/  taxonomy/  form/  notification/  translation/
├── extension/                     # InMemoryExtensionRegistry
├── port/                          # 端口假实现（4.7 表）与故障注入
├── sample/                        # 扩展点示例实现（测试模块 sample，9.2.5）
├── context/                       # 扩展点上下文夹具：FakePublishContext、FakeDeliveryContext、FakeRefResolver …
├── event/                         # RecordedEvents、EventSamples、EventAssertions（9.2.6）
├── fixture/                       # TestSites、TestUsers、ManualClock、FakeIdGenerator（9.2.7）
├── contract/                      # 契约套件、ContractEnvironment、InMemoryContractEnvironment、ContractCatalog（4.8）
├── conformance/                   # 扩展点一致性套件（4.8.5）
├── arch/                          # ArchUnit 规则集 PlatformArchitectureRules（9.5）
└── spring/                        # @ImportTestDoubles、StandInConfiguration（9.2.8）
```

#### 9.2.2 TestPlatform：替身的装配

替身之间共享同一个站点上下文、当前用户、时钟、ID 生成器、事件记录与扩展点注册表，因此由 `TestPlatform` 统一装配：

```java
class AssignmentServiceTest {

    private final TestPlatform platform = TestPlatform.standard();   // 两个站点、TestUsers、示例扩展点、全部替身
    private final InMemoryNotificationApi notifications = platform.notificationApi();
    private final AssignmentService service = new AssignmentService(
            platform.siteContextProvider(), platform.currentUserProvider(), notifications,
            platform.userDirectory(), new InMemoryAssignmentRuleRepository(), platform.clock());

    @BeforeEach
    void setUp() {
        platform.useSite(TestSites.A).useUser(TestUsers.EDITOR);
    }

    @Test
    @DisplayName("按国家命中规则后分配给销售，并发送分配通知")
    void assignsByCountry() {
        service.assign(InquiryFixtures.fromCountry("DE"));

        assertThat(notifications.sent()).singleElement()
                .satisfies(n -> assertThat(n.templateKey()).isEqualTo("inquiry.assigned.staff"));
    }
}
```

| 方法 | 作用 |
|---|---|
| `TestPlatform.standard()` | 预置 9.2.7 的站点与用户、主题默认文案、示例扩展点；时钟为 2026-01-01T00:00:00Z（与 `ContractEnvironment.reset()` 一致，4.8.2） |
| `TestPlatform.empty()` | 不预置数据与扩展点，用于需要精确控制的测试 |
| `useSite(id)` / `withoutSite()` / `useUser(user)` | 切换上下文 |
| `xxxApi()` | 取得某个替身（类型为具体替身类，可调用预置与断言方法） |
| `extensions()` | `InMemoryExtensionRegistry`：注册被测扩展点实现 |
| `events()` / `clock()` / `ids()` | 事件记录、`ManualClock`、`FakeIdGenerator` |
| `reset()` | 恢复初始状态 |

`InMemoryContractEnvironment`（4.8.2）基于 `TestPlatform` 实现。替身的依赖（站点上下文、当前用户、时钟、ID 生成器、事件发布、扩展点注册表）全部经构造参数注入，因此同一个替身既可以由 `TestPlatform` 装配，也可以在 Spring 中与 platform-core 的真实 Bean 组合（9.2.8）。

#### 9.2.3 服务接口替身

每个服务接口一个替身（4.2.2 总表逐一对应）。全部替身满足：通过对应的契约套件（4.8）；遵守站点隔离与“不跨语言回退”；写方法在调用方事务中发布与真实实现相同的事件（记录到 `events()`，或在 Spring 中经 `ApplicationEventPublisher` 发布）；可停用模块的替身支持 `ModuleDisabledException`；不实现契约之外的行为。

| 契约 | 替身 | 行为要点 | 预置与断言（testkit 专有方法） | 契约套件 / 夹具 | 提供方的契约测试 |
|---|---|---|---|---|---|
| `SiteApi` | `InMemorySiteApi` | 站点、域名、语言（开放与自动翻译设置）、模块启用状态 | `addSite`、`setLocalePublic`、`setModuleEnabled` | `SiteApiContract` / `SiteApiFixture` | platform-core |
| `SiteContextProvider` | `FakeSiteContextProvider` | 线程本地上下文；`callAs` / `runAs` 结束后恢复原上下文 | `use(siteId)`、`clear()` | `SiteContextProviderContract` | platform-core |
| `SettingsApi` | `InMemorySettingsApi` | 默认值取自 classpath 中模块的 `settings.schema.json`；`x-secret` 以 `FakeSecretCipher` 信封保存 | `put(key, json)` | `SettingsApiContract` | platform-core |
| `SiteProfileApi` | `InMemorySiteProfileApi` | 每个站点一份资料与各语言版本 | `setProfile(locale, …)` | `SiteProfileApiContract` / `SiteProfileApiFixture` | platform-core |
| `UiTextApi` | `InMemoryUiTextApi` | `zh-CN`：站点覆盖 → 主题默认；其他语言只有译文，不回退 | `defaults(map)`、`override(locale, key, text)` | `UiTextApiContract` / `UiTextApiFixture` | platform-core |
| `CurrentUserProvider` | `FakeCurrentUserProvider` | 当前用户与操作人取自 `TestUsers` | `use(user)` | `CurrentUserProviderContract` | platform-core |
| `PermissionChecker`（含 `DataScope`） | `FakePermissionChecker` | 权限码、数据范围、语言范围取自 `TestUser`；system 身份全部通过。`@RequiresPermission` 切面只在 Spring 中生效（core testsupport），单元测试直接断言应用服务是否调用了 `PermissionChecker` | `grant(user, code, scope)`、`revoke` | `PermissionCheckerContract` | platform-core |
| `UserDirectory` | `InMemoryUserDirectory` | 预置 `TestUsers`；支持停用用户 | `addUser`、`deactivate` | `UserDirectoryContract` / `UserDirectoryFixture` | platform-core |
| `AuditApi` | `InMemoryAuditApi` | 记录全部审计（含 `recordIndependently`） | `records()`、`assertRecorded(action)` | `AuditApiContract` | platform-core |
| `SecretCipher` | `FakeSecretCipher` | 可逆、确定性的信封 `v1:test:{Base64}`；`mask` 同真实实现 | — | `SecretCipherContract` | platform-core |
| `IdGenerator` | `FakeIdGenerator` | 固定 TSID 序列（9.2.7） | `reset(seed)` | `IdGeneratorContract` | platform-core |
| `JobApi`（`JobHandler`、`ScheduledTask` 为 SPI） | `InMemoryJobApi` | 提交后为 `PENDING`；`runPending()` 在提交时的站点与身份下同步执行已注册的 `JobHandler`；幂等键、取消、进度与结果文件（存入 `InMemoryObjectStorage`） | `register(handler)`、`runPending()`、`jobs()` | `JobApiContract` | platform-core |
| `DictApi` | `InMemoryDictApi` | 字典项与中文名 | `define(dictKey, items)` | `DictApiContract` / `DictApiFixture` | platform-core |
| `EventMetaFactory` | `FakeEventMetaFactory` | `eventId` 取 `FakeIdGenerator`，`occurredAt` 取 `ManualClock`，站点与操作人取当前上下文（同 5.8.2） | — | `EventMetaFactoryContract` | platform-core |
| `ExtensionRegistry` | `InMemoryExtensionRegistry` | 手工注册实现（模块 ID、`order`、内容类型、key）；按当前站点的模块启用状态过滤，排序同 4.4.15 | `register(point, impl, module)`、`registerSamples()` | `ExtensionRegistryContract` | platform-core |
| `ContentApi` | `InMemoryContentApi` | 内容、本地化、修订、状态机、发布快照与投影；发布时收集注册表中的 `PublishContributor` 并校验；预览令牌与 `assemblePreview` | 通过契约方法准备数据；`snapshotOf(…)` | `ContentApiContract` | content |
| `UrlApi` | `InMemoryUrlApi` | 路由表、slug 规范化与冲突、保留字、重定向与链检测 | — | `UrlApiContract` | url |
| `MediaApi` | `InMemoryMediaApi` | 媒体元数据 + `InMemoryObjectStorage`；URL 由 `FakeImageUrlSigner` 生成 | 夹具 `image(…)`、`file(…)` | `MediaApiContract` / `MediaApiFixture` | media |
| `TaxonomyApi` | `InMemoryTaxonomyApi` | 词汇表、分类项、指派与祖先路径 | 夹具 `vocabulary(…)`、`term(…)` | `TaxonomyApiContract` / `TaxonomyApiFixture` | taxonomy |
| `FormApi` | `InMemoryFormApi` | 表单定义与提交记录 | 夹具 `form(…)`、`submission(…)` | `FormApiContract` / `FormApiFixture` | form |
| `NotificationApi` | `InMemoryNotificationApi` | 记录发送请求；`dedupKey` 幂等；只检查模板变量齐全，不渲染邮件 | `sent()`、`assertSent(templateKey, recipient)` | `NotificationApiContract` | notification |
| `TranslationApi` | `InMemoryTranslationApi` | `translateText` 返回确定性伪译文 `[{目标语言}] 原文`（同一原文返回已是目标语言时 `sameLanguage = true`）；不保存原文与译文 | `setAvailable(bool)`、`failNext(errorCode)`、`setOrigins(…)` | `TranslationApiContract` / `TranslationApiFixture` | translation |

- 契约套件位于 `com.b2bweb.testkit.contract.<包>.<接口名>Contract`；夹具接口 `<接口名>Fixture` 只在契约无法创建数据时存在（4.8.2），真实实现侧由提供方用内部应用服务实现，替身侧由替身的预置方法实现。
- `ContractCatalog` 登记上表（4.8.7）；新增服务接口时，替身、套件与总表在同一个 PR 中补齐（4.9.2 第 ④ 步）。

#### 9.2.4 端口假实现

端口假实现的类名与行为见 4.7 开头的表（`InMemoryObjectStorage`、`FakeTranslationEngine`、`RecordingCdnPurger` 等）。testkit 另为全部远程端口提供统一的故障注入与记录：

```java
FakeTranslationEngine engine = platform.ports().translationEngine();
engine.faults().failNext(new ExternalServiceException(
        InfraErrors.ENGINE_UNAVAILABLE, "模拟引擎不可用", true, null));                       // 下一次调用失败
engine.faults().delay(Duration.ofSeconds(2));                                                  // 模拟慢响应（配合 ManualClock）
assertThat(engine.calls()).hasSize(1);                                                         // 每次调用的单元数、引擎 ID、模型
```

- `FakeTranslationEngine` 的伪译文规则：`[{目标语言}] 原文`，原样保留编号占位标签与数字；可以按单元 ID 设定输出（用于校验失败的用例，如丢失占位标签、超长）。
- 假实现同样遵守“不在事务中调用远程端口”：Spring 测试中，持有事务时调用远程端口的假实现直接失败（与 `test` profile 的 5.7.8 规则一致）。

#### 9.2.5 扩展点测试工具

**示例实现**：testkit 为每个扩展点提供一个示例实现，属于测试模块 `sample`。收集方用它们开发与测试收集、过滤与编排逻辑，契约测试中真实实现与替身看到同一组扩展点（4.8.2）。

| 扩展点 | 示例实现 | 关键取值 |
|---|---|---|
| `ContentTypeProvider` | `SampleContentTypeProvider` | 内容类型 `SAMPLE`（可路由，URL 规则 `/{locale}/samples/{slug}`） |
| `PublishContributor` | `SampleCardPublishContributor` | 快照部分 `card`（标题、摘要、`$media` 封面） |
| `ContentImpactProvider` | `SampleContentImpactProvider` | 对 `SAMPLE` 返回一条 `WARNING` |
| `TemplateProvider` | `SampleTemplateProvider` | 模板 `sample-detail`（一个固定区域 + 一个插槽） |
| `DataSourceProvider` | `SampleDataSourceProvider` | key `sample-list` |
| `DeliveryContributor` / `SiteBootstrapContributor` | `SampleDeliveryContributor` / `SampleSiteBootstrapContributor` | `sectionKey = sample` |
| `BreadcrumbProvider` | `SampleBreadcrumbProvider` | 一段固定中间节点 |
| `SchemaOrgProvider` / `SeoVariableProvider` / `SeoRule` / `SitemapProvider` | `SampleSchemaOrgProvider` / `SampleSeoVariableProvider` / `SampleSeoRule` / `SampleSitemapProvider` | `Thing` 节点；变量 `sample.code`；规则 `sample.title.present` |
| `SearchDocumentProvider` | `SampleSearchDocumentProvider` | 一个分面 `sampleColor` |
| `RelationTypeProvider` | `SampleRelationTypeProvider` | 关系类型 `SAMPLE_OF` |
| `MediaReferenceCollector` | `SampleMediaReferenceCollector` | 字段 `card.cover` |
| `ContentVariantProvider` | `SampleContentVariantProvider` | 两个型号 |
| `PersonalDataProvider` | `SamplePersonalDataProvider` | key `sample.contacts` |
| `NotificationTemplateProvider` | `SampleNotificationTemplateProvider` | 模板 `sample.created.staff` |
| `TranslatableContentProvider` / `TranslatableResourceProvider` | `SampleTranslatableContentProvider` / `SampleTranslatableResourceProvider` | 内容单元 `card.title`、`card.summary`；资源类型 `sample.resource` |
| `PlatformModule` | `SampleModule` | 记录生命周期回调的调用 |
| `JobHandler` / `ScheduledTask` | `SampleJobHandler` / `SampleScheduledTask` | 任务类型 `sample.echo`；定时任务 `sample.tick` |

- 测试模块 `sample` 的描述符位于 testkit 的 `classpath:testkit/module.json`（不在根路径，避免被 `classpath*:module.json` 扫描到生产装配中），只在 `test` profile 由模块管理器额外加载（框架补充，同步 5.2）。
- **上下文夹具**（`com.b2bweb.testkit.context`）：接口类型的上下文提供可构建的假实现——`FakePublishContext`、`FakeDeliveryContext`、`FakeSchemaOrgContext`、`FakeSeoAuditContext`、`FakeJobExecutionContext`、`FakeRefResolver`（按预置的内容、媒体、分类返回解析结果，并记录 `touched()`）；记录类型的上下文（如 `TranslationContext`、`BreadcrumbContext`）提供带默认值的构建器 `Contexts.translation()…build()`。
- **一致性套件**：`com.b2bweb.testkit.conformance.<扩展点>Conformance`（4.8.5），实现方继承并提供样例输入。

```java
class ProductSchemaOrgProviderTest {
    private final FakeRefResolver refs = FakeRefResolver.forLocale("en")
            .content(42L, "PRODUCT", "Li-ion Pack X200", "/en/products/x200")
            .image(7L, 1600, 1200, "X200 front view");

    @Test
    @DisplayName("Product 节点的图片与 URL 只来自 RefResolver")
    void usesRefResolver() {
        SchemaOrgContext ctx = FakeSchemaOrgContext.builder().site(TestSites.A).locale("en")
                .content(42L, "PRODUCT").snapshot(ProductSnapshots.x200()).refs(refs).build();

        assertThat(new ProductSchemaOrgProvider().nodes(ctx)).singleElement()
                .satisfies(n -> assertThat(n.get("image").asText()).startsWith("https://"));
    }
}
```

#### 9.2.6 事件断言

| 工具 | 场景 | 用法 |
|---|---|---|
| `RecordedEvents` | 单元测试（`TestPlatform`）与契约测试（`ContractEnvironment.events()`） | `all()`、`ofType(Class)`、`single(Class)`、`clear()`；只记录已提交事务中的事件（替身侧以 `inTransaction` 的边界模拟） |
| `EventAssertions` | 同上 | `assertThatEvents(events).published(InquiryCreatedEvent.class).matching(e -> …)`、`.notPublished(…)`、`.inOrder(A.class, B.class)` |
| `EventSamples` | 订阅方测试、事件契约（4.8.6） | 每个事件一个样例：`EventSamples.contentPublished().site(TestSites.A).contentId(42L).locale("zh-CN").build()`；新增事件必须补样例（否则 `DomainEventContract` 失败） |
| Spring Modulith Scenario API | 模块集成测试 | `scenario.publish(event).andWaitForStateChange(…)`；`scenario.stimulate(() -> service.x()).andWaitForEventOfType(…)`（9.3.3） |
| `ProcessedEvents` 幂等验证 | 模块集成测试 | `ModuleITSupport.deliverTwice(scenario, event)`：同一事件投递两次，断言副作用只发生一次（ADR-003） |

#### 9.2.7 站点上下文工具与固定 TSID 生成器

| 工具 | 内容 |
|---|---|
| `TestSites` | `A = 1001L`、`B = 1002L`（与 `ContractEnvironment.SITE_A / SITE_B` 一致）；代码 `site-a` / `site-b`；域名 `site-a.test` / `site-b.test`；源语言 `zh-CN`，另启用 `en`、`de`，`zh-CN` 与 `en` 对外公开，`en`、`de` 为 `AUTO` |
| `TestUsers` | `ADMIN`（系统管理员）、`EDITOR`（站点 A 的全部内容权限）、`VIEWER`（站点 A 只读）、`SALES`（站点 A 的询盘权限，数据范围 `OWN`）、`ONLY_B`（只在站点 B 有角色，用于隔离测试）、`SYSTEM`（system 身份）；每个用户带 `eventActor()` 与权限快照 |
| `ManualClock` | `java.time.Clock` 的可拨动实现：默认 `2026-01-01T00:00:00Z`、时区 UTC；`advance(Duration)`、`set(Instant)` |
| `FakeIdGenerator` | 固定 TSID 生成器：从种子（默认 `7201893453900001L`，与文档示例同量级）起按 1 递增，`reset()` 后序列相同，使 JSON 快照与断言稳定；`timestampOf(id)` 返回生成该 ID 时 `ManualClock` 的时间 |
| 单元测试中切换站点 | `platform.useSite(TestSites.B)`；`SiteContexts.runAs(TestSites.A, () -> …)` |
| Spring 测试中切换站点 | core testsupport 的 `@WithSite(TestSites.A)`、`@WithUser("editor")`、`TwoSiteFixture`（5.1.2） |
| Spring 测试中固定 ID | `@Import(FixedIdGeneratorConfiguration.class)` 以 `FakeIdGenerator` 替换真实的 TSID 生成器（仅在需要稳定 ID 的测试中使用） |

#### 9.2.8 在 Spring 中使用替身

**模块集成测试**：`@ImportTestDoubles`（`com.b2bweb.testkit.spring`）为每个没有 Bean 的服务接口与端口注册 testkit 替身（`@ConditionalOnMissingBean`）。在 `@ModuleIntegrationTest` 中，platform-core 是真实的，其他模块的服务接口与全部端口由替身提供；测试可以直接注入具体替身以预置数据与断言：

```java
@ModuleIntegrationTest
class InquiryAssignmentIT extends ModuleITSupport {
    @Autowired InMemoryFormApi forms;                     // form 模块的替身
    @Autowired InMemoryNotificationApi notifications;     // notification 模块的替身
    …
}
```

**本地运行的替身补位**（2.4、5.14.3）：

| 项 | 规定 |
|---|---|
| 依赖 | `application` 的 Maven profile `local-stand-ins` 以 `runtime` 范围加入 `platform-testkit`；本地启动脚本默认开启（第 11 章），CI 构建镜像时不开启 |
| 装配 | `StandInConfiguration`：只在 Spring profile `local` 且 `b2bweb.dev.stand-ins.enabled = true`（默认 true）时生效；为每个没有真实实现的服务接口注册内存替身，替身注入 platform-core 的真实 Bean（站点上下文、当前用户、时钟、ID、事件发布、扩展点注册表），事件经 `ApplicationEventPublisher` 发布给真实监听器 |
| 提示 | 启动时以 WARN 列出全部补位的接口；数据只在内存中，重启即丢失；补位的模块没有 REST 接口 |
| 防误用 | `ProductionSafetyCheck`（5.14.4）在 `staging`、`prod` 中检测到 classpath 上有 `com.b2bweb.testkit` 时拒绝启动（框架补充，同步 5.14.4） |

### 9.3 模块集成测试

#### 9.3.1 @ModuleIntegrationTest 与容器

模块集成测试的支撑位于 platform-core 的 test-jar（`com.b2bweb.core.testsupport`，与 `@SitePersistenceTest`、`@ModuleContractTest` 同包，5.1.2），业务模块的父 POM 已以 `test` 范围引入（5.1.2 的注）。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)  // 本模块 + api + core
@ActiveProfiles("test")
@Import({ PlatformTestContainers.class, TestDoublesConfiguration.class })                // 容器 + 9.2.8 的替身
@ExtendWith(SiteContextExtension.class)                                                   // @WithSite / @WithUser
public @interface ModuleIntegrationTest {}

/** 契约测试（4.8.3）：在 @ModuleIntegrationTest 基础上提供 ContractEnvironment Bean（SpringContractEnvironment）。 */
@ModuleIntegrationTest
@Import(SpringContractEnvironmentConfiguration.class)
public @interface ModuleContractTest {}
```

- 业务模块的 `allowedDependencies` 只有 `api` 与 `core`（3.4），因此 `DIRECT_DEPENDENCIES` 只启动本模块与 platform-core；其他模块的服务接口与全部端口由 testkit 替身补齐（业务模块不依赖 infrastructure，2.4）。
- 模块管理器在测试上下文中只对已启动的模块（core、本模块、测试模块 `sample`）执行迁移与 `onInstall`。

| 容器 | 镜像（标签以 ADR-011 为准） | 使用方 | 配置 |
|---|---|---|---|
| MySQL 8.4 | `mysql:8.4` | 全部模块集成测试、契约测试（真实实现侧）、`application` 集成测试 | 挂载与本地环境相同的 `deploy/compose/mysql/conf.d/` 与初始化脚本（11.3.1），字符集、排序规则、时区与第 7 章约定一致；`@ServiceConnection` |
| Redis | `redis:7.x` | 同上（会话、权限缓存、本地缓存广播、ShedLock 等 core 的用法） | `@ServiceConnection` |
| Meilisearch | `getmeili/meilisearch:v1.x` | infrastructure 的 `MeilisearchSearchEngineClientContractTest`；`application` 中涉及搜索的跨模块集成测试 | 动态属性 `b2bweb.infra.search-engine.url`、主密钥 |
| MinIO、Mailpit、ClamAV | 对应官方镜像 | 只用于 infrastructure 的端口契约测试（4.8.1） | — |

- 系统设计 28 中“模块集成测试（MySQL、Redis、Meilisearch）”据此细化：业务模块（包括 search）的集成测试使用 `InMemorySearchEngineClient`，真实 Meilisearch 行为由端口契约测试与 `application` 的集成测试覆盖，因为业务模块不依赖 infrastructure（2.4）。
- 容器在每个 JVM 中只启动一次（单例容器）；本地可在 `~/.testcontainers.properties` 中开启 `testcontainers.reuse.enable=true` 复用，CI 不复用。
- 数据清理：`ModuleITSupport` 在每个测试后清空本模块前缀的表与 core 的业务数据（保留迁移历史与 `TestSites` / `TestUsers` 夹具），不依赖事务回滚（事件监听需要真实提交）。

#### 9.3.2 ModuleITSupport

可选的基类，提供常用的请求与断言工具：

| 成员 | 作用 |
|---|---|
| `admin()` | MockMvc 请求构建器：自动带会话（`@WithUser` 指定的用户）、CSRF、`X-Site-Id`（`@WithSite` 指定的站点）；`admin().as(TestUsers.ONLY_B).site(TestSites.B).get(…)` 可覆盖 |
| `publicApi()` | 公开接口请求构建器：带内部令牌与 `X-Site-Host`（模拟 Nuxt） |
| `clock()` | 注入的 `ManualClock` |
| `events()` | 本测试中已提交事务发布的事件（基于 Spring Modulith 的 `PublishedEvents`） |
| `inSite(siteId, action)` | 以指定站点上下文执行（准备数据） |
| `deliverTwice(scenario, event)` | 幂等验证（9.2.6） |
| `assertProblem(result, code)` | 断言 RFC 9457 响应的状态码、`code` 与中文 `title` |

```java
@ModuleIntegrationTest
@WithSite(TestSites.A) @WithUser("sales")
class InquiryApiIT extends ModuleITSupport {

    @Test
    @DisplayName("销售只能看到分配给自己的询盘（数据范围 OWN）")
    void ownScope() throws Exception {
        long mine = inquiries.create(InquiryFixtures.assignedTo(TestUsers.SALES));
        long others = inquiries.create(InquiryFixtures.assignedTo(TestUsers.EDITOR));

        admin().get("/api/admin/v1/inquiry/inquiries")
               .andExpect(jsonPath("$.items[*].id").value(containsInAnyOrder(String.valueOf(mine))))
               .andExpect(jsonPath("$.items[*].id").value(not(hasItem(String.valueOf(others)))));
    }
}
```

#### 9.3.3 事件场景

```java
@Test
@DisplayName("收到 FormSubmittedEvent 后创建询盘并发布 InquiryCreatedEvent；重复投递只创建一次")
void createsInquiryOnce(Scenario scenario) {
    FormSubmittedEvent event = EventSamples.formSubmitted().site(TestSites.A).formKey("default-inquiry").build();

    scenario.publish(event)
            .andWaitForEventOfType(InquiryCreatedEvent.class)
            .toArriveAndVerify(created -> assertThat(created.meta().siteId()).isEqualTo(TestSites.A));

    deliverTwice(scenario, event);
    assertThat(inquiries.countBySubmission(event.submissionId())).isEqualTo(1);
}
```

- 发布方测试：用 `scenario.stimulate(() -> service.publish(…))` 断言事件在业务事务提交后出现、字段正确；回滚时不出现（4.8.3“事务”一行）。
- 订阅方测试：用 `EventSamples` 构造事件，不依赖发布方模块。
- 监听器在模块停用时被跳过（5.2“事件监听”一行）：可停用模块用 `siteApi.setModuleEnabled(...)` 覆盖这一路径（系统设计 3.8）。

#### 9.3.4 站点隔离套件

系统设计 28 的站点隔离测试由 core testsupport 的 `AdminSiteIsolationSuite` 实现，每个有后台接口的模块继承一次：

```java
@ModuleIntegrationTest
class InquirySiteIsolationIT extends AdminSiteIsolationSuite {
    @Override
    protected IsolationTargets targets() {
        return IsolationTargets.builder()
                .resource("/api/admin/v1/inquiry/inquiries/{id}", site -> inquiries.createIn(site))
                .resource("/api/admin/v1/inquiry/assignment-rules/{id}", site -> rules.createIn(site))
                .build();
    }
}
```

| 步骤 | 规则 |
|---|---|
| 枚举端点 | 从 `RequestMappingHandlerMapping` 取本模块 `apiPrefix` 下的全部后台端点；没有被 `targets()` 覆盖、也没有标注 `@IsolationExempt(reason)` 的端点使测试失败（迫使新增接口补齐隔离测试） |
| 准备 | `TwoSiteFixture`：在站点 A 创建资源；用户 `ONLY_B` 只在站点 B 有角色 |
| 断言 1 | `ONLY_B` 带 `X-Site-Id = B` 访问站点 A 资源的 ID：读、改、删、状态动作全部为 404（不泄露存在性），数据不变 |
| 断言 2 | `ONLY_B` 带 `X-Site-Id = A`：403（`CORE_FORBIDDEN`） |
| 断言 3 | 站点 B 的列表、搜索、导出结果中不出现站点 A 的 ID |
| 例外 | `@SystemLevel` 的系统管理接口标注 `@IsolationExempt`，由 core 的权限测试覆盖 |

#### 9.3.5 跨模块集成测试（application）

| 测试 | 内容 |
|---|---|
| `ModularityTest` | `ApplicationModules.of(B2bWebApplication.class).verify()`；同时用 Documenter 生成模块关系图到 `target/`（不提交） |
| `CrossModuleFlowIT` | 全部真实模块装配，只把外部端口换成假实现（AI 引擎、邮件、CDN、IndexNow、Turnstile）：发布 → URL → SEO → Delivery 的链路；表单提交 → 询盘 → 通知；源语言发布 → 翻译 → 目标语言发布；停用模块的整体效果（系统设计 3.8） |
| `SearchFlowIT` | 使用 Meilisearch 容器：发布 → 索引 → 搜索 |
| `OpenApiSnapshotIT` | 只在 `openapi` profile 下运行，导出 OpenAPI 快照（8.5.1） |
| `SiteIsolationIT`、`EventReliabilityIT` 等 | platform-core 的机制测试（5.1.3）在 core 中运行，不在这里重复 |

`application` 的集成测试不使用服务接口替身；它们验证替身退场后的真实组合（2.5）。

### 9.4 契约测试的组织

规则见 4.8；文件位置与运行方式如下：

```text
backend/platform/platform-testkit/
├── src/main/java/com/b2bweb/testkit/contract/
│   ├── ContractEnvironment.java  InMemoryContractEnvironment.java  ContractCatalog.java
│   ├── site/SiteApiContract.java  site/SiteApiFixture.java  …            # 每个服务接口一个套件（9.2.3 表）
│   ├── content/ContentApiContract.java  …
│   ├── port/ObjectStorageContract.java  …                               # 每个端口一个套件（4.8.4）
│   └── event/DomainEventContract.java  event/EventSamples.java
├── src/main/java/com/b2bweb/testkit/conformance/                         # 扩展点一致性套件（4.8.5）
└── src/test/java/com/b2bweb/testkit/
    ├── content/InMemoryContentApiContractTest.java  …                    # 替身侧：每个替身继承对应套件
    ├── port/FakeTranslationEngineContractTest.java  …                    # 假实现侧
    ├── ContractCatalogTest.java                                          # 覆盖检查，导出 target/contract-catalog.json
    └── DomainEventContractTest.java

backend/platform/platform-core/src/test/java/com/b2bweb/core/…/SiteApiContractTest.java  …   # core 提供的接口
backend/modules/<id>/src/test/java/com/b2bweb/<id>/<接口名>ContractTest.java               # 提供方真实实现侧
backend/modules/<id>/src/test/java/com/b2bweb/<id>/<接口名>FixtureImpl.java                # 夹具的真实实现（经本模块应用服务）
backend/modules/<id>/src/test/java/com/b2bweb/<id>/…/<实现类名>ConformanceTest.java        # 扩展点实现方
backend/infrastructure/src/test/java/com/b2bweb/infrastructure/…/<适配器类名>ContractTest.java
```

| 情况 | 做法 |
|---|---|
| 提供方实现一个服务接口 | 在模块中新建 `<接口名>ContractTest extends <接口名>Contract`，标注 `@ModuleContractTest`；有夹具接口时同时实现 `<接口名>FixtureImpl`；全部用例通过后，`ContractCatalog` 中该项的 `pending` 自动消失（4.8.7） |
| 需要契约中没有的行为 | 不得依赖未写入契约测试的行为（4.8 开头）；提出契约变更请求（4.9），由框架组在同一个 PR 中补充签名、替身与套件用例 |
| 契约测试在真实实现侧失败、替身侧通过 | 视为实现缺陷，由提供方修复 |
| 替身与真实实现行为不一致 | 按框架缺陷处理：先补契约用例，再修复替身或实现（4.9.4） |
| 适配器测试需要外部服务 | 用 Testcontainers（MinIO、Redis、Meilisearch、Mailpit、ClamAV）或进程内模拟 HTTP 服务器（OkHttp `MockWebServer`，用于 OpenAI 兼容接口、Turnstile、Webhook、CDN、IndexNow） |

### 9.5 架构测试规则清单

后端规则集 `PlatformArchitectureRules`（testkit `arch` 包，模块清单、层号与表前缀以常量保存，附录 A.3 第 4 条）；每个模块的 `ArchitectureTest` 只有一行：

```java
class ArchitectureTest {
    @Test
    void platformRules() { PlatformArchitectureRules.forModule("inquiry").check(); }
}
```

| ID | 规则 | 工具 | 依据 |
|---|---|---|---|
| A01 | 业务模块不依赖其他业务模块的包；platform-core 不依赖任何业务模块 | Maven Enforcer（编译期）+ ArchUnit + Modulith `verify()` | 系统设计 3.3 R1、29.3；`CLAUDE.md` 禁止 1 |
| A02 | `modules_only_depend_on_core_support`：模块只引用 `com.b2bweb.core.support..` | ArchUnit | 5.1.2 |
| A03 | 模块内的包依赖方向（`domain` / `application` / `infrastructure.persistence` / `web.*` / `facade` 等） | ArchUnit | 3.4 |
| A04 | `domain` 不依赖 Spring、MyBatis | ArchUnit | 3.4 |
| A05 | 控制器注解、所在包与路径一致，路径的模块段等于 `apiPrefix` | ArchUnit | 5.5.1；附录 A.3 第 3 条 |
| A06 | Mapper 只在本模块的 `infrastructure.persistence` 中使用 | ArchUnit | 系统设计 29.3 |
| A07 | SQL（Mapper XML、注解、迁移脚本）只访问本模块前缀的表 | `tools/checks/table-prefix.mjs` | 系统设计 29.3；`CLAUDE.md` 禁止 2 |
| A08 | MyBatis 不使用 `${}` | `tools/checks/mybatis-dollar.mjs` | 系统设计 29.3；`CLAUDE.md` 禁止 7 |
| A09 | 站点级 PO 继承 `SitePO`；全局表的 PO 标注 `@GlobalTable` 且在豁免清单中 | ArchUnit + `SiteIsolationIT`（5.1.3） | 第 7 章；`CLAUDE.md` 禁止 10 |
| A10 | `@InterceptorIgnore` 只出现在允许清单登记的位置 | `tools/checks/interceptor-ignore.mjs` | 5.7 |
| A11 | 发布领域事件的方法或其所在类标注 `@Transactional`；监听器使用 `@ApplicationModuleListener` | ArchUnit | 4.6；5.8 |
| A12 | 端口只被 4.2.5 表中列出的使用方注入 | ArchUnit | 4.7 |
| A13 | `@Extension.module` 等于实现类所在模块；`allRegardlessOfSite` 只被允许的调用方使用 | ArchUnit | 4.4.15；5.3 |
| A14 | `com.b2bweb.api..` 只依赖允许的库；其中每个接口都在 `ContractCatalog` 中登记 | ArchUnit + `ContractCatalogTest` | 4.1；4.8.7 |
| A15 | 不使用 AI 厂商 SDK 的包（`com.openai..`、`dev.langchain4j..`、`org.springframework.ai..` 等） | Maven Enforcer + ArchUnit | `CLAUDE.md` 禁止 12 |
| A16 | 不直接创建线程与线程池；并行使用 `PlatformExecutors` | ArchUnit | 5.4 |
| A17 | 命名规范：`<Id>Module`、`…AdminController`、`…Repository`、`…PO`、`…Mapper`、`…Job` 等位于约定的包 | ArchUnit | 3.7 |
| A18 | 模块根包只有 `package-info.java` 与 `<Id>Module.java` | ArchUnit | 3.4 |
| A19 | 审计 Mapper 只有 insert 与 select | ArchUnit（core） | 5.11.1 |
| A20 | `module.json` 与代码一致（事件、权限码、菜单路由、组件、设置 Schema） | `tools/checks/module-json.mjs` | 附录 A.3 |
| A21 | 已合入的迁移脚本不可修改；版本号递增 | `tools/checks/migration-immutable.mjs` | 系统设计 29.3；`CLAUDE.md` 禁止 7 |
| FE-1 … FE-8 | 前端依赖方向与禁止的包 | dependency-cruiser、ESLint | 8.1.3 |
| FL-1 … FL-8 | `v-html`、硬编码文案、中文字面量、内联样式、Token 字面量等 | ESLint、Stylelint | 8.1.4 |

- 不使用 ArchUnit 的 `FreezingArchRule`（不设“已知违例”基线）：违反即修复。
- M0 验收（13.1 第 7 条）用“违反边界的示例”验证规则有效，并由每周的门禁自检持续验证（10.6）。

### 9.6 前端测试

#### 9.6.1 Vitest

`frontend/vitest.workspace.ts` 汇总各包的配置：`component-schemas`、`shared` 在 node 环境；`admin-kit`、`admin`、`website-kit`、主题在 happy-dom 环境（Vue Test Utils）；依赖 Nuxt 运行时的组合式函数用 `@nuxt/test-utils`。

| 包 | 必须覆盖 |
|---|---|
| `component-schemas` | 每一步迁移的夹具测试；`validate()` 对 `$defs` 的正反例；破坏性变更判定 |
| `shared` | 富文本节点白名单、`content://` 解析、纯文本提取；编辑器协议的类型守卫；JSON Patch |
| `admin-kit` | HTTP 中间件与 8.2.2 的每种错误处理；SchemaForm 的每个 `x-editor` 控件与推断规则；`v-perm`；`registerContentTab` 的过滤（模块停用、权限、语言）与保存顺序 |
| `website-kit` | 变体回退四种情况；SectionRenderer 的隐藏、停用模块、迁移、错误边界；`useText` 的缺失与复数；缓存头与状态码；`useSeoHead` 的 JSON-LD 转义；画布消息桥的 `origin` 校验 |
| 主题 | 每个变体用画廊示例渲染不报错；交互组件（表单、标签页、图集、同意横幅）的行为 |
| Admin 模块 | 页面逻辑与 store；接口用 `@b2bweb/admin-kit/testing` 的 MSW 模拟（按 `AdminPaths` 类型检查） |

覆盖率：`component-schemas`、`shared`、`admin-kit`、`website-kit` 行覆盖率 ≥ 80%（框架代码）；模块代码不设统一阈值。

#### 9.6.2 组件画廊与截图对比

| 项 | 规定 |
|---|---|
| 画廊 | `/__gallery`（`NUXT_PUBLIC_GALLERY=true` 时开放，生产为 404，系统设计 11.4）：列出组件 × 变体 × 示例；参数 `?type=hero&variant=hero-split&example=default&theme=industrial` 渲染单个组合 |
| 示例数据 | `component-schemas/components/<模块>/examples/<type>.json`：属性 + 配套的 `refs` 与 `dataSources` 夹具；图片来自 `examples/assets/` 的静态文件，不经 imgproxy，保证截图稳定 |
| 矩阵 | 组件 × 变体 × 示例 × 断点（390 / 768 / 1440）× 主题（V1 只有 industrial，系统设计 28） |
| 执行 | Playwright 项目 `visual`（`frontend/e2e/visual/gallery.spec.ts`），在 Playwright 官方 Docker 镜像中运行（字体渲染一致）；禁用动画与光标闪烁，固定时间 |
| 基线 | `frontend/e2e/visual/__screenshots__/{theme}/{type}/{variant}-{example}-{breakpoint}.png`，提交到仓库；阈值 `maxDiffPixelRatio = 0.001` |
| 人工确认 | 有意的视觉变化：在容器中运行 `node tools/dev/b2b.mjs visual-update` 更新基线并随 PR 提交，评审人在 PR 的图片差异中确认（系统设计 28“差异需人工确认”） |
| 无障碍 | 同一轮对每个组合运行 axe-core，`serious` / `critical` 违规失败 |

#### 9.6.3 Playwright E2E

```text
frontend/e2e/
├── playwright.config.ts        # 项目：smoke、scenarios、visual、a11y；baseURL 来自环境变量
├── fixtures/                   # 登录（后台接口）、演示数据加载、Mailpit 与 mock-openai 客户端
├── smoke/                      # @smoke：首页、产品页、sitemap、后台登录、一条测试询盘
├── scenarios/                  # s01-product-publish.spec.ts … s13-inquiry-translate.spec.ts
└── visual/                     # 9.6.2
```

| 场景 | 主要断言 | 里程碑 |
|---|---|---|
| S1 新建并发布产品 | SSR HTML 完整；meta、canonical、Product 与 BreadcrumbList JSON-LD；sitemap 更新；相关内容出现；开放英语时英语版本自动生成并发布 | M1（英语部分 M3） |
| S2 修改 slug | 旧 URL 301 到新 URL，无重定向链 | M1 |
| S3 页面编辑器搭建落地页 | 三种设备尺寸预览；草稿不影响线上；分享预览链接可访问 | M2 |
| S4 广告访客提交询盘 | 来源归因字段；反垃圾通过；自动分配；销售邮件与客户自动回复（Mailpit）；按同意状态发送转化事件 | M1（GA4 部分 M3） |
| S5 下载留资 | 填表后得到下载链接；生成 DOWNLOAD 线索 | M3 |
| S6 Excel 批量导入 | 校验预览 → 确认 → 执行；逐行错误报告；重复导入按型号更新 | M4 |
| S7 旧站 URL 映射导入 | 批量 301；循环与链检测 | M3 |
| S8 产品停产 | 410 或 301；引用处卡片移除；sitemap 移除 | M3 |
| S9 SEO 健康报告 | 问题列表与跳转修复 | M3 |
| S10 停用文章模块 | 菜单、API、组件面板中消失；影响提示；数据保留 | M2 |
| S11 开放英语 | 批量翻译完成后英语页面、hreflang、sitemap 生效；翻译记忆命中不调用引擎（经 mock-openai 的请求记录断言） | M3 |
| S12 修改一段描述后重新发布 | 只有被修改的段落调用引擎；人工修订保持不变 | M3 |
| S13 一键翻译德语询盘 | 译文保存在询盘上；再次查看不调用引擎 | M3 |

- 运行环境：本地（第 11 章）、dev（main 合入后的 `@smoke`）、staging（发布时全量，10.5）。翻译相关场景使用部署在测试环境的 mock-openai（11.7），不消耗真实额度；邮件断言读取 Mailpit 的 API。
- 数据：全局 setup 加载 `minimal` 演示数据（幂等，9.7）；每个用例自己创建所需数据，名称带运行 ID 后缀，不依赖其他用例。
- 稳定性：CI 中失败重试 1 次并保留 trace；异步结果用 `expect.poll` 等待（如 S1 的英语版本，超时 6 分钟，系统设计 1.3 要求 5 分钟内）；不得使用固定等待。
- 无障碍：主要模板（首页、产品详情、文章详情、分类页、落地页、表单）运行 axe-core，`serious` / `critical` 失败（系统设计 28）。

### 9.7 测试数据集

| 数据集 | 位置 | 内容 | 使用方 | 维护 |
|---|---|---|---|---|
| 替身夹具 | testkit `fixture/`、`TestPlatform.standard()` | `TestSites`、`TestUsers`、主题默认文案、示例扩展点 | 单元测试、契约测试 | 框架组 |
| 事件样例 | testkit `EventSamples` | 每个领域事件一个样例 | 订阅方测试、`DomainEventContract` | 新增事件的 PR 补充（4.8.6） |
| 模块测试数据 | 各模块 `src/test/.../<模块>Fixtures` | 本模块领域对象的构建器（经应用服务或仓储创建，不写其他模块的表） | 本模块测试 | 模块负责人 |
| 两站点夹具 | core testsupport `TwoSiteFixture` | 两个站点 + 只在一个站点有角色的用户 | 站点隔离测试（9.3.4） | 框架组 |
| 组件示例 | `component-schemas/components/<模块>/examples/` | 每个组件的属性、refs、数据源夹具 | 画廊、截图、组件测试 | 组件所属模块 |
| 演示数据集 | `tools/demo-data/` | 见下文 | 本地开发、E2E、演示、画廊之外的视觉检查 | 各模块负责人维护自己的加载器 |
| SEO 检查基线 | `tools/seo-check/expectations/` | 演示数据集上应满足的 SEO 断言 | 发布流水线的 SEO 爬取检查 | seo 模块负责人 |

**演示数据集**（系统设计 28：多分类、多语言的电池类产品、文章、案例）：

```text
tools/demo-data/
├── load.mjs                    # 入口：node tools/demo-data/load.mjs --dataset full --base http://localhost:8080 --site site-a
├── loaders/<模块 ID>.mjs        # 每个模块的加载器（CODEOWNERS 归模块负责人），按层号顺序执行：L2 → L3 → L4
└── datasets/
    ├── minimal/                # E2E 与 CI：1 个站点资料、6 个分类、10 个产品、3 篇文章、2 个案例、首页与落地页、菜单、询盘表单
    └── full/                   # 演示：约 60 个产品（含型号与参数）、12 个分类、20 篇文章、6 个案例、FAQ、法律页面、全局区块、
                                #       菜单、表单、媒体（CC0 图片与示例 PDF）、产品导入 Excel、旧站 URL 映射表、翻译引擎配置（指向 mock-openai）
```

| 规则 | 说明 |
|---|---|
| 只经后台接口加载 | 加载器登录后调用各模块的后台接口（产品经 Excel 导入接口，同时验证 S6），不直接写数据库，因此不破坏模块边界，也覆盖了接口本身 |
| 幂等 | 以自然键（slug、型号、分类 key、表单 key、菜单 key）判断是否已存在；重复执行只补缺 |
| 部分模块可用 | 先读取 bootstrap 的启用模块；某模块的接口不存在（尚未实现或由替身补位）时跳过其加载器并给出警告 |
| 语言 | 只录入中文源语言；英语等目标语言由翻译流程生成（`CLAUDE.md` 语言规则 3），本地使用假引擎或 mock-openai（11.7） |
| 个人数据 | 不使用任何真实客户数据；询盘与联系人使用虚构姓名与 `example.com` 邮箱（RFC 2606） |
| 与接口同步 | 模块修改后台接口时，在同一个 PR 中更新自己的加载器；main 流水线每次部署 dev 后执行 `minimal`（10.4），加载失败即流水线失败 |
| staging 数据 | staging 使用脱敏的生产数据（系统设计 27.2）加上 E2E 专用测试站点，不使用 `full` 数据集 |

---

## 10. 质量门禁与 CI

本章细化系统设计 27.3（CI/CD）、28（各测试层级的门禁）与 29.3（架构守护），规定 PR、主干、发布三条流水线以及定期任务中每个门禁的命令与失败条件。规则本身见 9.5（架构测试）、8.1.3 / 8.1.4（前端依赖与 lint）、8.4.6（Schema），测试的组织见第 9 章。

门禁编号：`G` 通用、`B` 后端、`F` 前端、`R` 发布前验收（staging）、`P` 生产发布。本章其他位置与第 8、9 章按编号引用（如 B9、F6）。

| 原则 | 说明 |
|---|---|
| 本地与 CI 同一组命令 | 工作流只调用 `./mvnw`、`pnpm` 与 `tools/` 下的脚本，YAML 中不写检查逻辑；`node tools/dev/b2b.mjs check` 在本地运行同一组门禁（10.7） |
| 违反即失败 | 门禁没有“警告后放行”；规则确实不适用时修改规则本身（10.8），不跳过门禁 |
| 构建一次 | 镜像只在主干流水线构建一次；staging 与生产部署同一个镜像摘要（digest），不重新构建 |
| 最小权限 | 工作流默认 `permissions: contents: read`；第三方 Action 按提交 SHA 固定；PR 工作流不使用任何部署凭据 |
| 时长目标 | PR 流水线 P90 ≤ 20 分钟；超出时由框架组分片或并行化，不以删减门禁换取速度（10.8） |

### 10.1 流水线总览

```text
.github/workflows/
├── pr.yml              # pull_request / merge_group → main：G1–G5、B1–B10、F1–F8（10.2）
├── pr-approvals.yml    # pull_request / pull_request_review：G6 审批规则（10.2.2）
├── main.yml            # push → main：全量验证 → 镜像 → 部署 dev → 演示数据 → 冒烟（10.4）
├── release.yml         # 推送标签 v*：staging → R1–R7 → 人工批准 → 生产 P1–P4（10.5）
├── nightly.yml         # 每天：全量视觉回归、dev 上的全量 E2E、漏洞复扫（10.6）
└── weekly.yml          # 每周：门禁自检、依赖更新、不稳定测试报告（10.6）
tools/ci/
├── affected.mjs        # 由改动路径计算要运行的门禁与受影响的 Maven 工程（10.2.1）
├── gate-selftest/      # “违反边界的示例”补丁与期望失败的门禁（10.6）
└── report.mjs          # 汇总门禁结果：Job Summary 与 PR 评论
```

| 项 | 规定 |
|---|---|
| 运行环境 | GitHub 托管的 Ubuntu runner（自带 Docker，供 Testcontainers）；JDK 与 Node 版本取自 ADR-011（`setup-node` 读取 `frontend/.nvmrc`） |
| 缓存 | Maven 本地仓库（键为全部 `pom.xml` 的哈希）、pnpm store（`pnpm-lock.yaml`）、Playwright 浏览器；Testcontainers 镜像按 ADR-011 锁定的标签预拉取 |
| 并发 | 同一 PR 的新提交取消旧运行；main 与发布流水线排队执行，不取消 |
| 结果反馈 | 每个门禁写 Job Summary；失败时 `report.mjs` 在 PR 中更新同一条评论：门禁编号、失败原因（文件、行号、规则 ID）、本地复现命令 |
| 产物 | Surefire / Failsafe 报告、JaCoCo、Vitest JUnit、`contract-catalog.json`、OpenAPI 快照、Playwright trace 与截图差异，保留 14 天 |

### 10.2 PR 流水线

#### 10.2.1 作业与触发范围

```text
changes（affected.mjs）
   ├── general      G1–G5
   ├── backend      B1–B5 → 导出 OpenAPI 快照 ─┬── backend-it（4 个分片）B6、B7、B8
   │                                           └── api                    B9、B10
   ├── frontend     F1–F7
   ├── visual       F8（按路径触发）
   └── pr-gate      needs 以上全部：任一失败或取消即失败，跳过视为通过；分支保护只要求它与 pr-approvals
```

`affected.mjs` 的规则：

| 改动路径 | 运行的门禁 |
|---|---|
| 只有 `docs/**`（`docs/api/**` 除外）与根目录 `*.md` | G1–G5 |
| `backend/modules/<id>/**` | G1–G5、B1–B5、B7–B10；B6 只运行 `<id>` |
| `backend/platform/**`、`backend/build/**`、`backend/pom.xml`、`backend/infrastructure/**`、`backend/application/**` | G1–G5、B1–B10；B6 运行全部工程 |
| `frontend/**` | G1–G5、F1–F7；F8 见 10.2.4 |
| `frontend/packages/component-schemas/{meta,defs,components,templates}/**`、`frontend/packages/themes/*/texts/**` | 另加 B1–B5、B8，以及 page、delivery、seo 的 B6（后端复制这些 JSON，8.4.5） |
| `docs/api/**`、`frontend/packages/api-client/**` | 另加 B9、B10 |
| `.github/**`、`tools/**`、`deploy/**`、`CLAUDE.md` | 全部门禁 |

- merge queue（10.3）中的运行（`merge_group` 事件）同样按上表裁剪，比较基准为队列中的前一个提交。
- `backend-it` 按历史耗时把工程均衡分为 4 片；各分片从 `backend` 作业上传的产物恢复已安装的内部构件（`~/.m2/repository/com/b2bweb`），不重复编译依赖。

#### 10.2.2 通用门禁

| ID | 门禁 | 命令 | 失败条件 |
|---|---|---|---|
| G1 | PR 标题 | `node tools/checks/pr-title.mjs`（读取 PR 标题与描述） | 标题不符合 `<type>(<scope>): <描述>`；type 不在 `CLAUDE.md` 提交规范的清单中；scope 出现时不是模块 ID（含 `core`）或 `website`、`admin`；带 `!` 而描述中没有 `BREAKING CHANGE:`。squash 合并以 PR 标题作为提交信息（系统设计 29.4） |
| G2 | 密钥扫描 | `gitleaks git --log-opts="origin/main..HEAD" --redact` 与 `gitleaks dir . --redact`（配置 `.gitleaks.toml`） | 任何发现。允许清单只登记本地开发用的公开值（11.3.4），按“路径 + 精确值”登记，新增条目需架构负责人批准（系统设计 26.6） |
| G3 | 依赖漏洞 | `osv-scanner scan source --recursive --config osv-scanner.toml .`（`pnpm-lock.yaml` 与各 `pom.xml` 的传递依赖） | 存在高危或严重（CVSS ≥ 7.0）且已有修复版本的漏洞，且不在忽略清单中；忽略清单每项必须写原因与到期日（≤ 90 天），过期即失败 |
| G4 | 迁移脚本不可修改 | `node tools/checks/migration-immutable.mjs --base origin/main` | `**/src/main/resources/db/migration/**` 中 main 已有的文件被修改、删除或改名（比较 SHA-256）；新文件不符合 `V{三位序号}__{描述}.sql`；新序号重复或不大于该目录已有的最大序号；目录不是 `db/migration/{模块 ID}`（系统设计 25.6；9.5 A21） |
| G5 | 文档检查 | `node tools/checks/docs-refs.mjs` | 1.7 的“引用检查”：Markdown 中的相对链接与文件路径不存在；引用的系统设计章节号、本文章节号、ADR 编号不存在；模块设计文档第 2 节出现的服务接口、扩展点、事件、端口不在第 4 章；状态为“已评审”的模块设计文档仍保留模板（12.2）中的填写提示；`docs/adr/README.md` 索引与 ADR 文件不一致 |
| G6 | 审批规则 | `pr-approvals.yml` 运行 `node tools/checks/approvals.mjs` | 改动 `backend/platform/platform-api/**` 或 `backend/platform/platform-core/**`，而有效批准少于 2 人或不含架构负责人（系统设计 29.4）；PR 触及两个及以上模块的目录（3.6 的四个位置），而描述中的“跨模块原因”一节为空（`CLAUDE.md` 提交规范） |

#### 10.2.3 后端门禁

`backend` 作业（在 `backend/` 下执行）：

```bash
./mvnw -B -ntp -T 1C install -DskipITs                       # B1–B4：validate 阶段 Enforcer；test 阶段单元与架构测试；verify 阶段 JaCoCo check
node ../tools/checks/run.mjs backend                          # B5
./mvnw -B -ntp -pl application verify -Popenapi -DskipUTs     # 导出 OpenAPI 快照，供 api 作业使用（8.5.1）
```

`backend-it` 作业（每个分片）：

```bash
./mvnw -B -ntp verify -DskipUTs -pl <本分片的受影响工程>                                    # B6；application 在受影响清单中时同时完成 B8
./mvnw -B -ntp verify -DskipUTs -pl <本分片的其余工程> \
       -Dit.test='*ContractTest,*ConformanceTest' -Dfailsafe.failIfNoSpecifiedTests=false   # B7：未受影响的工程只运行契约与一致性测试
```

| ID | 门禁 | 命令 | 失败条件 |
|---|---|---|---|
| B1 | 编译与构建规则 | `install -DskipITs`（Enforcer 绑定在 validate 阶段） | 编译错误；Enforcer：业务模块依赖其他模块、`infrastructure` 或 `application`（3.2）；引入 AI 厂商 SDK（A15）；子工程写版本号；依赖版本不收敛；Maven / JDK 版本低于下限 |
| B2 | 单元测试 | 同上（Surefire） | 任一测试失败或出错；替身侧的契约测试（`InMemory…ContractTest`）同在此运行 |
| B3 | 架构测试 | 同上：各模块的 `ArchitectureTest`、`application` 的 `ModularityTest`、testkit 的 `ContractCatalogTest` | 9.5 中由 ArchUnit、Spring Modulith `verify()` 与 `ContractCatalogTest` 检查的任一规则被违反 |
| B4 | 领域层覆盖率 | 同上（JaCoCo `check`） | 任一模块 `com.b2bweb.*.domain..` 的行覆盖率 < 80%（系统设计 28）；没有 `domain` 包的模块不检查 |
| B5 | 脚本检查 | `node tools/checks/run.mjs backend`：`table-prefix.mjs`（A07）、`mybatis-dollar.mjs`（A08）、`interceptor-ignore.mjs`（A10）、`module-json.mjs`（A20，附录 A.3） | 任一脚本报告违规（输出文件、行号与规则 ID） |
| B6 | 模块集成测试 | `verify -DskipUTs -pl <受影响工程>`（Failsafe，`@Tag("integration")`，9.1） | 任一 `*IT`、真实实现侧契约测试或站点隔离套件失败（9.3）；新增后台端点既未被 `AdminSiteIsolationSuite` 覆盖也未标注 `@IsolationExempt`（9.3.4） |
| B7 | 契约测试 | 其余工程只运行 `*ContractTest`、`*ConformanceTest`；随后运行 `tools/checks/contract-coverage` | 任一契约或一致性测试失败（4.8）；服务接口已有真实实现却缺少 `<接口名>ContractTest`，或端口适配器缺少 `<适配器类名>ContractTest`；`ContractCatalog` 与 4.2 总表不一致。`pending` 项在 PR 中允许（替身先行），里程碑验收前必须为 0（4.8.7） |
| B8 | 跨模块集成测试 | `verify -DskipUTs -pl application`（任一后端改动都运行） | `CrossModuleFlowIT`、`SearchFlowIT` 等失败（9.3.5）；应用无法启动（模块描述符、组件 Schema、迁移校验失败） |
| B9 | OpenAPI 与 api-client 一致 | `api` 作业：取 `backend` 作业导出的快照 → `pnpm gen:api` → `git diff --exit-code docs/api frontend/packages/api-client/src/generated` | 快照或生成代码与 PR 中提交的不一致（8.5.1、系统设计 24.3）；失败信息提示在本地运行 `node tools/dev/b2b.mjs api` 并提交 |
| B10 | API 破坏性变更 | `oasdiff breaking --fail-on ERR <main 上的快照> <PR 的快照>`，`admin` 与 `public` 两组分别比较 | 出现 ERR 级破坏性变更（删除端点或字段、改变类型、新增必填参数、收窄枚举等）。破坏性变更只能以新版本路径提供并用 `Sunset` 头宣布旧版本下线（系统设计 24.2）；删除已过 `Sunset` 日期的旧版本时，由架构负责人添加 `api-sunset` 标签放行，脚本核对日期 |

#### 10.2.4 前端门禁

`frontend` 作业（在 `frontend/` 下执行）：

| ID | 门禁 | 命令 | 失败条件 |
|---|---|---|---|
| F1 | 安装与依赖清单 | `pnpm install --frozen-lockfile` + `node ../tools/checks/pnpm-catalog.mjs` | 锁文件与 `package.json` 不一致；第三方依赖没有经 `catalog:` 声明；内部依赖不是 `workspace:*`；出现禁止的包（FE-7：国际化库、AI 厂商 SDK、越界使用 Element Plus）（8.1.1、8.1.3） |
| F2 | 代码规范 | `pnpm lint`（ESLint `--max-warnings 0`、Stylelint、dependency-cruiser） | FE-1–FE-8 与 8.1.4 的任一规则；任何警告 |
| F3 | 类型检查 | `pnpm typecheck` | 任一类型错误 |
| F4 | 单元与组件测试 | `pnpm test -- --coverage` | 任一测试失败；`component-schemas`、`shared`、`admin-kit`、`website-kit` 的行覆盖率 < 80%（9.6.1） |
| F5 | 构建与包体预算 | `pnpm build` + `node ../tools/checks/bundle-budget.mjs` | 构建失败；website 任一页面入口的初始 JS（gzip）> 150 KB（系统设计 1.5；构建产物的静态估算，最终以 R3 为准）；Admin 入口 chunk 比 `tools/checks/bundle-budget.json` 中的基线大 10% 以上 |
| F6 | 组件与模板 Schema | `pnpm check:schemas` | 8.4.6 的 9 项检查任一失败，含破坏性变更未提升版本或缺少迁移函数（系统设计 29.3） |
| F7 | 主题一致性 | `pnpm check:theme` | 8.6.2 的一致性检查失败 |
| F8 | 组件视觉与无障碍 | `visual` 作业：`pnpm test:visual`（Playwright 官方镜像，9.6.2） | 截图差异超过阈值；新变体缺少基线；axe `serious` / `critical` 违规。只在改动 `packages/themes/**`、`website-kit`、`design-tokens`、`component-schemas/**/examples/**` 时运行（系统设计 27.3“改动涉及主题时”）；有意的视觉变化以更新后的基线随 PR 提交，评审人在差异报告中确认 |

#### 10.2.5 与系统设计 27.3、29.3 的对应

| 系统设计中的规则或步骤 | 门禁 |
|---|---|
| Core 不依赖业务模块；模块之间不互相 import | B1（Enforcer）、B3（A01、Modulith `verify()`） |
| 模块只访问自己的表 | B3（A06）、B5（A07） |
| MyBatis 不使用 `${}` | B5（A08） |
| 跨模块契约只在 platform-api | 分支保护的 CODEOWNERS 必审（10.3）+ G6 |
| 前端模块之间不互相 import | F2（FE-2） |
| 前台样式只用 Design Token；禁止 `v-html` | F2（Stylelint、`vue/no-v-html`） |
| 已合入的迁移脚本不可修改 | G4 |
| API 破坏性变更 | B10 |
| 组件 Schema 破坏性变更必须提升版本并附迁移函数 | F6 |
| `module.json` 中声明的事件与代码一致 | B5（A20） |
| 性能预算 | F5（静态估算）、R3（Lighthouse CI） |
| 密钥泄露 | G2 |
| 依赖漏洞扫描 | G3；镜像扫描见 10.4、R6 |
| 集成测试（Testcontainers）；OpenAPI 生成 + 破坏性变更检查 | B6–B8；B9、B10 |
| 组件视觉回归（改动涉及主题时） | F8 |

### 10.3 合入规则

| 项 | 规定 |
|---|---|
| 分支保护（main） | 禁止直接推送与强制推送；只允许 squash 合并；必需状态检查为 `pr-gate` 与 `pr-approvals`；要求 CODEOWNERS 批准（2.6）；新提交使已有批准失效；全部评审对话已解决；关闭管理员绕过 |
| 合入队列 | 启用 GitHub merge queue：队列以“main + 本 PR”的合并结果再运行一次 `pr.yml`，避免两个各自通过的 PR 合在一起后失败 |
| 批准人数 | 至少 1 人（系统设计 29.4）；platform-api、platform-core 2 人且含架构负责人（G6）；跨模块修改由各相关模块负责人批准（2.6） |
| PR 描述 | 模板 `.github/pull_request_template.md`：改动说明、关联的模块设计文档与 ADR、跨模块原因（G6 检查）、PR 级 DoD 清单（2.8）、是否涉及 API / Schema / 迁移 / 可翻译字段 / 视觉基线 |
| 生成物 | OpenAPI 快照与 api-client、`style-options.json`、视觉基线由作者在本地生成并提交（10.7）；CI 只比较，不向 PR 分支推送提交 |
| 紧急修复 | 同样经 PR 与全部门禁；架构负责人可以加急评审，但不能跳过门禁 |

### 10.4 主干流水线

```text
push → main（squash 合入，或 merge queue 合入）
 1. 全量验证    ./mvnw -B -ntp verify（全部工程的集成测试，补齐 PR 中按受影响范围裁剪的 B6）
                pnpm lint && pnpm typecheck && pnpm test && pnpm build
 2. 构建镜像    deploy/docker/{backend,website,admin}.Dockerfile → <镜像仓库>/b2bweb-{backend,website,admin}:<git SHA>
                生成 SBOM（syft）；镜像漏洞扫描（trivy image）；website 与 admin 的 source map 上传 Sentry 后从镜像中删除
 3. 部署 dev    backend → website → admin（dev profile 的 migrate-on-startup = true，5.14.3）
 4. 等待就绪    /actuator/health/readiness（5.13）、Nuxt /__health（系统设计 27.4）
 5. 演示数据    node tools/demo-data/load.mjs --dataset minimal（幂等，9.7）
 6. 冒烟        pnpm test:e2e --project smoke（9.6.3）
 7. 集成看板    发布 contract-catalog.json 与 dev 上的替身补位清单（13.5）
```

| 步骤 | 失败条件 | 失败时 |
|---|---|---|
| 1 | 任一测试或构建失败 | 不构建镜像 |
| 2 | 镜像构建失败；存在严重（CRITICAL）且已有修复版本的漏洞 | 不部署 |
| 3–4 | 部署失败，或 5 分钟内未就绪 | dev 自动回到上一个成功的镜像 |
| 5 | 任一模块的加载器失败（如修改了后台接口而未更新加载器，9.7） | 流水线失败；dev 保留新版本以便排查 |
| 6 | 冒烟场景重试 1 次后仍失败 | 同上 |

| 规则 | 说明 |
|---|---|
| main 变红优先处理 | 失败通知作者与框架组；作者在 1 个工作小时内修复或回滚（revert PR）；在此之前合入队列只接受带 `fix-main` 标签的修复或回滚 PR |
| 镜像 | 标签只用 git SHA（不可变）；发布时追加版本标签（10.5）。Dockerfile 统一要求：非 root 用户、`TZ=UTC`、JVM 参数与 5.14.4 一致、容器健康检查；镜像中不含任何密钥与 `platform-testkit`（不启用 Maven profile `local-stand-ins`，9.2.8） |
| CHANGELOG | 不在 main 上生成；发布时由 Conventional Commits 生成（10.5） |

### 10.5 发布流水线

由发布负责人在 main 上一个已通过 10.4 的提交上推送标签 `v{主}.{次}.{修订}` 触发：

```text
 1. 准备         校验该提交的主干流水线成功；为 3 个镜像追加版本标签（同一摘要，不重新构建）；
                 git-cliff 按 Conventional Commits 生成 CHANGELOG 与 GitHub Release 草稿（系统设计 29.4）
 2. 部署 staging  migrate 任务（--mode=migrate，迁移账号，5.14.1）→ 更新 backend → R7 → 更新 website → admin
 3. 验收门禁      R1–R6 并行（下表）
 4. 人工批准      GitHub Environment `production` 的必需审批人：架构负责人 + 业主或其指定人；审批页附 R1–R7 报告
 5. 生产          P1–P4（系统设计 27.3）
 6. 收尾          发布 GitHub Release；在 Sentry 中标记部署；通知
```

**staging 验收门禁**（系统设计 28 中“staging 必须通过”的各项）：

| ID | 门禁 | 命令 | 失败条件 |
|---|---|---|---|
| R1 | E2E 全量 | `pnpm test:e2e`（项目 `smoke` + `scenarios`，`BASE_URL` 指向 staging 的 E2E 测试站点，9.6.3） | 当前里程碑范围内的任一场景重试 1 次后仍失败；尚未到里程碑的场景以 `test.fixme` 标注，并在报告中列出 |
| R2 | 无障碍 | 随 R1 对主要模板运行 axe-core | `serious` / `critical` 违规 |
| R3 | 性能预算 | `lhci autorun --config tools/perf/lighthouserc.json`：首页、产品详情、文章详情、分类页、落地页、表单页，中文与一个已公开的目标语言（如有）；移动端预设，每个 URL 运行 3 次取中位数 | Performance < 90、SEO < 100、Accessibility < 95；实验室 LCP > 2.5 s、CLS > 0.1、TBT > 200 ms（作为 INP 的实验室替代）；初始 JS（gzip）> 150 KB（系统设计 1.5、21.4） |
| R4 | SEO 爬取 | `node tools/seo-check/run.mjs --base <staging> --expect tools/seo-check/expectations/` | 可索引页面状态码不是 200；canonical 缺失或指向非 200 的 URL；hreflang 不互链；JSON-LD 不通过校验；不执行 JS 时缺少主要内容、链接或结构化数据；重定向链超过 1 跳；sitemap 中出现非 200 或 `noindex` 的 URL；出现不带语言前缀的前台 URL（系统设计 28；`CLAUDE.md` 禁止 17）。staging 全局的 `noindex` 与 Basic Auth（系统设计 27.2）由脚本按环境配置忽略与携带 |
| R5 | 负载 | `k6 run tools/perf/delivery.js`；`k6 run tools/perf/admin.js` | Delivery API 以 200 RPS 持续 5 分钟（直接请求后端，模拟 CDN 未命中）：缓存未命中 P95 > 300 ms、命中 P95 > 30 ms、错误率 > 0.1%；后台 50 个并发用户：P95 > 500 ms（系统设计 1.5）。staging 规格与生产不一致时结果只作参考，并在审批页标注 |
| R6 | 安全扫描 | OWASP ZAP baseline（前台首页、产品页、Admin 登录页；规则文件 `tools/security/zap-rules.tsv`）；`trivy image` 以最新漏洞库复扫 3 个镜像 | ZAP 报告高危告警；镜像存在严重或高危且已有修复版本的漏洞（系统设计 28“高危则阻断”）；规则文件中降级为 `IGNORE` 的告警必须写明原因 |
| R7 | 滚动兼容 | 第 2 步只更新 backend 后，用旧版本的 website 与 admin 运行 `pnpm test:e2e --project smoke` | 新后端与旧前台不兼容（生产按 backend → website → admin 滚动更新，期间新旧版本共存）；migrate 任务失败 |

**生产发布**（系统设计 27.3）：

| ID | 步骤 | 内容 | 失败时 |
|---|---|---|---|
| P1 | migrate 任务 | 独立容器运行同一镜像：`--mode=migrate`，使用迁移账号（5.14.1） | 停止发布，应用不更新；按运维手册“迁移失败的回滚”处理 |
| P2 | 滚动更新 | backend → website → admin；每个实例就绪（readiness / `__health`）后再更新下一个 | 任一实例 5 分钟内未就绪：停止并把已更新的实例回滚到上一版本 |
| P3 | 生产冒烟 | `pnpm test:e2e --project smoke`，含一条测试询盘（`is_test`，通知发到运维邮箱，系统设计 27.4） | 自动把 3 个服务回滚到上一版本镜像；迁移不回滚（expand / contract 保证上一版本兼容新的表结构，系统设计 25.6） |
| P4 | 观察 | 发布后 30 分钟内关注系统设计 27.4 的告警（前台 5xx、Delivery P95、未完成事件等） | 出现 P1 级告警时由值班人决定是否回滚 |

| 规则 | 说明 |
|---|---|
| 版本号 | 平台整体语义化版本（3.2 的 `${revision}` 由标签传入）；`module.json` 中的模块版本独立维护（附录 A） |
| 回滚 | 只回滚镜像，不回滚数据库；因此每个版本的迁移必须与上一版本的代码兼容（expand / contract），违反者在 R7 中暴露 |
| 修订版本 | 修复先合入 main（经全部门禁）；main 上有尚未准备发布的改动时，从发布标签建立 `release/{主}.{次}` 分支并 cherry-pick 修复，在该分支上运行 10.4 的第 1–2 步后打修订号标签，之后流程相同 |
| CDN | 版本涉及模板或主题渲染变化时，发布说明中注明，发布后按运维手册整站清除 CDN（系统设计 23.3） |

### 10.6 夜间与定期任务

| 工作流 | 时间（UTC） | 内容 | 失败时 |
|---|---|---|---|
| `nightly.yml` | 每天 01:00 | ① 全量组件视觉回归与 axe（全部组件 × 变体 × 断点 × 主题，9.6.2）；② 在 dev 上运行 E2E 全量场景；③ 以最新漏洞库对 main 复扫 G3 与镜像；④ 不复用容器、随机测试顺序的全量 `./mvnw verify`（暴露依赖执行顺序或时间的测试） | 创建或更新带 `nightly-failure` 标签的 Issue，由框架组分派 |
| `weekly.yml` | 每周一 02:00 | ① 门禁自检（下文）；② 不稳定测试报告（过去 7 天重试后才通过的测试）；③ 契约目录的 `pending` 项与替身补位清单推送到集成看板（13.5） | 同上 |
| Renovate | 每周一 | 依赖更新 PR：Maven 只改 `b2bweb-bom`，pnpm 只改 `catalog`；每个生态一组，依赖升级单独成 PR（`CLAUDE.md` 提交规范）；不自动合入；主版本升级须同步更新 ADR-011 的版本表 | PR 照常经过全部门禁 |

**门禁自检**（9.5；13.1 第 7 条“违反边界的示例模块使 CI 失败”）：`tools/ci/gate-selftest/` 中每个用例是一个补丁与它期望触发的门禁。自检在最新 main 的临时工作区中逐个应用补丁并只运行对应的门禁命令；门禁通过（即没有拦住违规）则自检失败。

| 用例 | 补丁内容 | 期望失败的门禁 |
|---|---|---|
| 跨模块 import | inquiry 引用 `com.b2bweb.product..` 的类 | B1、B3（A01） |
| 访问其他模块的表 | inquiry 的 Mapper XML 查询 `prd_product` | B5（A07） |
| `${}` | Mapper XML 使用 `${}` | B5（A08） |
| 修改已合入的迁移 | 修改 core 的 `V001__init.sql` | G4 |
| AI 厂商 SDK | 模块 POM 加入 `com.openai:*` | B1（A15） |
| 越过端口使用范围 | 非 translation 模块注入 `TranslationEngine` | B3（A12） |
| 站点隔离漏测 | 新增后台端点而不加入隔离套件 | B6 |
| `v-html`、颜色字面量、硬编码文案 | 主题组件中分别加入 | F2（`vue/no-v-html`、Stylelint、`b2bweb/no-literal-ui-text`） |
| 后台引入国际化库 | Admin 加入 `vue-i18n` | F1、F2（FE-7） |
| 前端模块互相 import | Admin 模块 a 引用模块 b | F2（FE-2） |
| Schema 破坏性变更 | 删除组件属性但不提升版本 | F6 |
| API 破坏性变更 | 删除公开接口的响应字段 | B10 |
| 密钥 | 提交形如云服务 API Key 的字符串 | G2 |

新增门禁或规则时必须同时新增自检用例（10.8）。

### 10.7 本地执行

| 命令（仓库根目录） | 运行的门禁 | 用途 |
|---|---|---|
| `node tools/dev/b2b.mjs check` | G2（本分支的提交）、G4、G5、B1–B5、F1–F7 | 推送前的常规检查，约 5–10 分钟，不需要 Docker |
| `node tools/dev/b2b.mjs check --module <id>` | 同上，但后端只构建该模块（`-pl modules/<id> -am`），并运行该模块的集成测试 | 模块开发中的快速反馈 |
| `node tools/dev/b2b.mjs check --full` | 另加 B6（受影响工程）、B7、B8、B9、B10、F8（需要 Docker） | 涉及 API、平台工程、多个模块或主题时；包含 `CLAUDE.md` 要求的提交前检查（`./mvnw verify`；`pnpm lint && pnpm typecheck && pnpm test`） |
| `node tools/dev/b2b.mjs api` | B9 的生成步骤 | 修改接口后重新生成快照与 api-client（8.5.1） |
| `node tools/dev/b2b.mjs visual-update` | F8 的基线更新 | 有意的视觉变化（9.6.2） |

- Git hooks（lefthook，`b2b.mjs init` 安装）：`pre-commit` 对暂存文件运行 gitleaks 与 ESLint / Stylelint；`commit-msg` 用 G1 的同一脚本检查提交信息。hooks 只是便利，CI 是唯一的判定依据。
- 本地与 CI 的差异只有并行度与缓存；本地通过而 CI 失败时，以 CI 为准并修复本地环境差异（如锁文件、生成物未提交）。

### 10.8 门禁的维护与例外

| 项 | 规定 |
|---|---|
| 新增或修改门禁 | 修改 `.github/`、`tools/checks/`、`tools/ci/` 或 testkit 规则集的 PR 由架构负责人批准（CODEOWNERS，2.6）；同一 PR 更新本章与 9.5，并新增门禁自检用例（10.6） |
| 不得绕过 | 不得用 `-DskipTests`、`@Disabled`、`eslint-disable`、`stylelint-disable` 绕过架构类规则（A01–A21、FE-1–FE-8、8.1.4）；ArchUnit 不设冻结基线（9.5） |
| 规则自带的例外 | 只能使用已定义的例外机制：`v-html` 例外清单（8.3.8）、`@InterceptorIgnore` 允许清单（`tools/checks/interceptor-ignore-allowlist.txt`，5.7）、`@IsolationExempt(reason)`（9.3.4）、gitleaks 允许清单（G2）、漏洞忽略清单（G3，带到期日）、ZAP 规则文件（R6）；每新增一项都需架构负责人批准 |
| 不稳定测试 | 发现后 1 个工作日内修复，或以 `@Disabled("FLAKY #<Issue 编号>")` / `test.fixme` 暂时禁用并登记 Issue（最长 14 天，周报跟踪）；架构测试、契约测试、站点隔离套件不得禁用 |
| 工具版本 | oasdiff、gitleaks、osv-scanner、trivy、syft、git-cliff、k6、Lighthouse CI、ZAP 的版本在 ADR-011 的版本表与工作流中固定，经 Renovate 升级 |
| 时长 | PR 流水线 P90 超过 20 分钟时由框架组优化（分片、缓存、拆分慢测试）；不得以删减门禁或降低阈值换取速度 |

---

## 11. 本地开发环境

本章细化系统设计 27.2 的 local 环境与 5.14.3 的 `local` profile：一条命令启动依赖服务与三个应用，尚未实现的模块由替身补位（9.2.8），新成员按 README 在半天内跑通（13.1 第 9 条）。本地环境不需要任何真实的云服务账号、邮件服务或 AI 服务 API Key。

### 11.1 前置要求与目录

| 项 | 要求 |
|---|---|
| 操作系统 | macOS、Linux；Windows 使用 WSL2，仓库检出在 WSL 的文件系统中 |
| Docker | Docker Engine 或 Docker Desktop，含 Compose v2；分配给 Docker 的内存 ≥ 6 GB（启用 ClamAV 时 ≥ 8 GB） |
| JDK | ADR-011 锁定的版本（仓库根目录 `.sdkmanrc` 记录，可用 SDKMAN! 安装） |
| Node 与 pnpm | `frontend/.nvmrc` 的 Node 版本；pnpm 由 Corepack 按 `packageManager` 提供（8.1.1） |
| 浏览器 | Chrome、Edge 或 Firefox（Safari 见 11.8） |
| 可选 | IntelliJ IDEA（仓库提供 `.run/` 运行配置）；mkcert（本地 HTTPS，11.8） |

```text
deploy/compose/
├── compose.local.yml          # 依赖服务（11.2），Compose 项目名 b2bweb
├── compose.apps.yml           # 可选：以容器运行 backend、website、admin（演示或没有 JDK 的机器）
├── .env.example               # 端口与本地开发凭据（公开值，11.3.4）；b2b.mjs init 复制为 .env（不提交）
├── mysql/conf.d/b2bweb.cnf    # 11.3.1；Testcontainers 挂载同一目录（9.3.1）
├── mysql/init/01-init.sql     # 建库与账号（11.3.1）
├── minio/init.sh              # 建桶与策略（11.3.2）
├── geoip/                     # MaxMind 公开的测试库（11.3.3）
└── reference/                 # 参考部署（系统设计 27.1），不属于本章
tools/dev/b2b.mjs              # 本地命令入口（11.4），只依赖 Node 标准库，可在 pnpm install 之前运行
tools/mock-openai/             # 本地 OpenAI 兼容模拟服务（11.7）
```

### 11.2 服务与端口

**依赖服务**（`compose.local.yml`，端口只绑定 `127.0.0.1`）：

| 服务 | 镜像（标签以 ADR-011 为准） | 宿主机端口 | 用途 | 健康检查 |
|---|---|---|---|---|
| `mysql` | `mysql:8.4` | 3306 | 主数据库 | `mysqladmin ping` |
| `redis` | `redis:7.x` | 6379 | 会话、缓存、限流、锁、缓冲 | `redis-cli ping` |
| `meilisearch` | `getmeili/meilisearch:v1.x` | 7700 | 站内搜索 | `GET /health` |
| `minio` | `minio/minio` | 9000（S3 API）、9001（控制台） | 对象存储：公开桶与私有桶 | `GET /minio/health/live` |
| `minio-init` | `minio/mc` | — | 一次性：建桶与策略后退出（11.3.2） | — |
| `mailpit` | `axllent/mailpit` | 1025（SMTP）、8025（Web 与 API） | 截获全部外发邮件 | `GET /livez` |
| `imgproxy` | `ghcr.io/imgproxy/imgproxy` | 8088 | 图片处理，读取 MinIO 公开桶 | `GET /health` |
| `mock-openai` | 由 `tools/mock-openai/Dockerfile` 构建 | 4010 | OpenAI 兼容模拟服务（11.7） | `GET /health` |
| `clamav`（可选） | `clamav/clamav` | 3310 | 病毒扫描；Compose profile `scan` | `clamdcheck` |

**应用**（宿主机进程，由 `b2b.mjs dev` 或 IDE 启动）：

| 应用 | 端口 | 访问地址 |
|---|---|---|
| backend | 8080（API）、8081（管理端口，5.13） | `http://localhost:8081/actuator/health`；Modulith 文档 `http://localhost:8081/actuator/modulith` |
| website（Nuxt 开发服务器） | 3000 | `http://site-a.localhost:3000/zh/`；组件画廊 `http://site-a.localhost:3000/__gallery`（9.6.2） |
| admin（Vite 开发服务器） | 5173 | `http://localhost:5173` |

- 端口可在 `.env` 中以 `B2B_PORT_*` 覆盖；`b2b.mjs` 启动前检查端口占用并报告占用的进程，应用之间的连接地址按 `.env` 生成。
- 数据卷：`b2bweb_mysql`、`b2bweb_minio`、`b2bweb_meili` 持久化；Redis 与 Mailpit 不持久化（重启后会话与邮件清空，会话丢失只需重新登录）。

### 11.3 初始化

各服务在首次启动时按本节初始化；数据卷已存在时跳过。数据库表一律由后端启动时的模块迁移创建（`migrate-on-startup = true`，5.14.3），初始化脚本只建库与账号。

#### 11.3.1 MySQL

`mysql/conf.d/b2bweb.cnf`（与 Testcontainers 共用，9.3.1）：

```ini
[mysqld]
character-set-server = utf8mb4
collation-server     = utf8mb4_0900_ai_ci        # 系统设计 25.1；路径、slug、key 类列在 DDL 中单独使用 utf8mb4_0900_bin
default-time-zone    = '+00:00'                  # 时间统一 UTC
skip-name-resolve
max_allowed_packet   = 64M
# sql_mode 使用 MySQL 8.4 的默认值（含 STRICT_TRANS_TABLES、ONLY_FULL_GROUP_BY），不得放宽
```

`mysql/init/01-init.sql`（只在数据卷首次创建时执行；密码取 `.env`）：

```sql
CREATE DATABASE IF NOT EXISTS b2bweb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'b2bweb_migrator'@'%' IDENTIFIED BY '<B2BWEB_DB_MIGRATION_PASSWORD>';   -- 迁移账号：DDL + DML
GRANT ALL PRIVILEGES ON b2bweb.* TO 'b2bweb_migrator'@'%';
CREATE USER 'b2bweb_app'@'%' IDENTIFIED BY '<B2BWEB_DB_PASSWORD>';                 -- 应用账号：只有 DML
GRANT SELECT, INSERT, UPDATE, DELETE ON b2bweb.* TO 'b2bweb_app'@'%';
```

本地同样区分应用账号与迁移账号（系统设计 26.6），因此“应用账号缺少 DDL 权限”类问题在本地即可暴露。

#### 11.3.2 MinIO

`minio/init.sh` 由一次性容器 `minio-init` 执行，可重复运行：

| 操作 | 说明 |
|---|---|
| 创建 `b2bweb-public`、`b2bweb-private` | 公开桶与私有桶分离（系统设计 27.1）；桶名即 `object-storage.public-bucket` / `private-bucket`（5.12.1） |
| `mc anonymous set download local/b2bweb-public` | 公开桶匿名只读；本地没有 CDN，`object-storage.public-base-url = http://localhost:9000/b2bweb-public` |
| 私有桶前缀 `jobs/` 的生命周期规则：7 天后过期 | 异步任务的输入与结果文件由存储自动删除（5.9.1 的部署要求） |

本地也可以把 `object-storage.type` 设为 `local`（`LocalFileObjectStorage`，5.12.1）以不启动 MinIO，但此时 imgproxy 无法读取图片，只建议在只开发后台非媒体功能时使用。

#### 11.3.3 其他服务

| 服务 | 初始化与配置 |
|---|---|
| Redis | 无密码；不持久化（`--save "" --appendonly no`）；键前缀规则与生产相同（3.7） |
| Meilisearch | 主密钥取 `.env`；`MEILI_ENV=development`、`MEILI_NO_ANALYTICS=true`；索引由 search 模块按需创建，不预建；多人共用一台开发实例时设置 `b2bweb.infra.search-engine.index-prefix`（5.12.1） |
| Mailpit | `MP_SMTP_AUTH_ACCEPT_ANY=1`、`MP_SMTP_AUTH_ALLOW_INSECURE=1`、`MP_MAX_MESSAGES=5000`；部署级 SMTP 为 `localhost:1025`、`security = NONE`（只在 local 允许，5.14.4）；站点级 SMTP 留空即使用部署级默认 |
| imgproxy | `IMGPROXY_KEY` / `IMGPROXY_SALT` 与后端的 `B2BWEB_IMGPROXY_KEY` / `B2BWEB_IMGPROXY_SALT` 取同一组开发值；`IMGPROXY_USE_S3=true`、`IMGPROXY_S3_ENDPOINT=http://minio:9000` 与 MinIO 凭据；`IMGPROXY_BASE_URL=s3://b2bweb-public/`（签名路径中的 `plain/{对象键}` 由它补全为源地址，4.7.2）；`IMGPROXY_STRIP_METADATA=true` |
| GeoIP | `geoip/` 中放 MaxMind 公开的测试库（MaxMind-DB 测试数据中的 `GeoLite2-Country-Test.mmdb`），`geoip.database-path` 指向它；它只覆盖少量测试网段，本机 IP 解析为“未知国家”属正常 |
| ClamAV（可选） | `b2b.mjs up --with clamav` 启用 profile `scan`，首次启动下载病毒库需要数分钟；未启用时 `virus-scanner.type = fake`，使用 testkit 的 `FakeVirusScanner`（含 EICAR 字符串的文件判为感染；`fake` 类型只在 local 可用，见 5.12.1） |
| 人机验证 | form 的演示数据加载器为站点写入 Cloudflare 公开的 Turnstile 测试密钥（总是通过） |
| CDN、IndexNow | `cdn.type = none`、`index-now.type = none`（5.14.3） |
| AI 引擎 | `translation-engine.type` 默认 `fake`（testkit 的 `FakeTranslationEngine`，5.14.3）；`b2b.mjs dev --engine mock`（或 `real`）时为 `openai-compatible`，经真实适配器调用 mock-openai（或开发者自行配置的真实引擎，11.7） |

#### 11.3.4 环境文件与开发凭据

`deploy/compose/.env.example`（节选）：

```dotenv
# 端口（可改）
B2B_PORT_MYSQL=3306
B2B_PORT_BACKEND=8080
B2B_PORT_WEBSITE=3000
B2B_PORT_ADMIN=5173
# 本地开发凭据：公开值，只用于本机
MYSQL_ROOT_PASSWORD=dev-root
B2BWEB_DB_USER=b2bweb_app
B2BWEB_DB_PASSWORD=dev-app
B2BWEB_DB_MIGRATION_USER=b2bweb_migrator
B2BWEB_DB_MIGRATION_PASSWORD=dev-migrator
B2BWEB_S3_ACCESS_KEY=b2bweb-dev
B2BWEB_S3_SECRET_KEY=dev-minio-secret
B2BWEB_MEILI_MASTER_KEY=dev-meili-master-key
B2BWEB_IMGPROXY_KEY=<文档公开的十六进制开发值>
B2BWEB_IMGPROXY_SALT=<文档公开的十六进制开发值>
B2BWEB_SECRET_KEYS=dev:<文档公开的开发密钥>          # 生产安全检查拒绝此值（5.14.4）
B2BWEB_INTERNAL_TOKENS=<首次启动随机生成并保存的私有开发令牌>   # 后端与 Nuxt 共用
B2B_DEV_PASSWORD=<种子用户的公开开发密码>             # 11.6
B2B_DEV_TOTP_SECRET=<种子用户的公开 TOTP 密钥>        # 11.6
```

| 规则 | 说明 |
|---|---|
| 文件 | `.env` 被 `.gitignore` 忽略；`.env.example` 只含上面的公开开发值，并按“路径 + 精确值”登记在 gitleaks 允许清单中（G2） |
| 后端默认值 | `application-local.yml` 以同名环境变量读取，默认值与 `.env.example` 相同，因此在 IDE 中不加载 `.env` 也能启动 |
| 生产隔离 | 开发密钥、`fake` 适配器与 testkit 只在 `local` / `test` 中有效，`staging`、`prod` 的生产安全检查拒绝启动（5.14.4、9.2.8） |
| 真实凭据 | 不写入 `.env` 或任何配置文件：真实 SMTP 在后台站点设置中配置；真实 AI 引擎的 API Key 只在后台的翻译引擎配置中录入，加密保存在本地数据库（`CLAUDE.md` 禁止 12） |

### 11.4 一键启动

**首次使用**：

```bash
node tools/dev/b2b.mjs init     # 检查前置要求、复制 .env、安装依赖、拉取镜像（首次约 10–20 分钟）
node tools/dev/b2b.mjs dev      # 启动依赖服务与三个应用；后端首次启动执行全部模块的迁移与基础种子（11.6）
node tools/dev/b2b.mjs seed     # 加载 minimal 演示数据（9.7）
# 后台 http://localhost:5173（admin@example.com）  前台 http://site-a.localhost:3000/zh/  邮件 http://localhost:8025
```

**命令**（`node tools/dev/b2b.mjs <命令>`）：

| 命令 | 作用 |
|---|---|
| `init` | 检查 Docker、JDK、Node 版本与 Docker 内存（11.1）；复制 `.env`；`corepack enable`、`pnpm install`、`pnpm gen:tokens`；预下载 Maven 依赖；拉取镜像并构建 mock-openai；安装 Git hooks（10.7） |
| `up [--with clamav]` | 启动依赖服务，等待全部健康检查通过，运行 `minio-init` |
| `dev [--engine fake\|mock\|real] [--only backend,website,admin] [--no-stand-ins] [--https]` | `up` 后依次启动三个应用，日志带前缀合并输出；Ctrl+C 只停止应用，容器继续运行 |
| `seed [--dataset minimal\|full] [--site site-a]` | 加载演示数据（11.6） |
| `status` | 容器健康状态与端口、后端就绪状态、站点启用的模块、由替身补位的服务接口 |
| `logs [服务]` | 查看容器日志 |
| `down` | 停止容器，保留数据卷 |
| `reset [--yes]` | 停止并删除数据卷（数据库、对象存储、搜索索引全部清空）；之后重新 `dev` 与 `seed` |
| `totp [用户]` | 打印种子用户当前的 TOTP 验证码（11.6） |
| `api`、`check`、`visual-update`、`e2e` | 见 8.5.1、10.7、9.6.2、9.6.3 |

`dev` 的启动顺序：

```text
 1. up：依赖服务全部健康
 2. backend：./mvnw -q -pl application -am install -DskipTests -Plocal-stand-ins
             ./mvnw -pl application spring-boot:run -Plocal-stand-ins -Dspring-boot.run.profiles=local（环境变量取自 .env）
             等待 /actuator/health/readiness 为 UP（启动步骤 8 完成，5.2.2）
 3. website 与 admin 并行：pnpm dev:website、pnpm dev:admin（8.1.1）
 4. 输出访问地址、种子账号与替身补位清单
```

- 后端也可以在 IDE 中启动：仓库提供 `.run/Backend (local).run.xml`（主类 `com.b2bweb.B2bWebApplication`，Spring profile `local`，Maven profile `local-stand-ins`，读取 `deploy/compose/.env`）；此时用 `b2b.mjs dev --only website,admin` 启动前端。
- 前端包的 `exports` 指向 TypeScript 源码（8.1.1），修改 `admin-kit`、`website-kit`、主题或 `component-schemas` 后两个开发服务器直接热更新；修改 Token 需运行 `pnpm gen:tokens`。
- `compose.apps.yml` 以 main 流水线产出的镜像运行三个应用（`docker compose -f compose.local.yml -f compose.apps.yml up`），用于演示或没有 JDK 的机器；此时没有替身补位，只包含已合入的真实模块。

### 11.5 应用的本地配置

| 应用 | 配置 | 说明 |
|---|---|---|
| backend | Spring profile `local`（5.14.3）+ Maven profile `local-stand-ins`（9.2.8） | 启动时用迁移账号执行迁移；`translation-engine.type` 默认 `fake`，`--engine mock` / `real` 时为 `openai-compatible`；`virus-scanner.type` 默认 `fake`；管理端口额外暴露 `modulith`（5.13）；可信代理包含回环地址（5.6.5） |
| 替身补位 | `b2bweb.dev.stand-ins.enabled`（默认 `true`） | 尚未实现的服务接口由 testkit 的内存替身补位，启动日志以 WARN 列出，数据只在内存中，补位的模块没有 REST 接口（9.2.8）；`--no-stand-ins` 关闭，用于确认真实实现已经齐全 |
| website | `NUXT_B2BWEB_API_BASE=http://localhost:8080`、`NUXT_B2BWEB_INTERNAL_TOKEN`（取 `.env` 中 `B2BWEB_INTERNAL_TOKENS` 的第一个）、`NUXT_PUBLIC_ADMIN_ORIGIN=http://localhost:5173`、`NUXT_PUBLIC_GALLERY=true`（8.3.1） | Nuxt 的 devProxy 把浏览器发出的 `/api/public/**` 转发到后端并保留 `Host`，模拟站点域名的反向代理（8.3.2、5.4.2） |
| admin | Vite 开发服务器把 `/api/**` 代理到 `http://localhost:8080` | 后台与 `/api/admin` 同源，不开启 CORS（ADR-010、5.5.1）；Cookie 与 CSRF 行为与生产一致 |
| 站点域名 | 种子站点的主域名为 `site-a.localhost`、`site-b.localhost` | 浏览器把 `*.localhost` 直接解析到本机（RFC 6761）；后端规范化 Host 时去掉端口（5.4.2），因此 `http://site-a.localhost:3000` 对应站点 A；以 `localhost:3000` 访问前台会得到 404（未登记的域名） |
| 编辑器画布 | — | Admin（`localhost:5173`）以 iframe 打开 `http://site-a.localhost:3000/__editor?token=…`；`frame-ancestors` 与 `postMessage` 的来源校验使用 `NUXT_PUBLIC_ADMIN_ORIGIN`（8.3.13） |

### 11.6 种子数据

| 层 | 内容 | 方式 |
|---|---|---|
| 基础种子（自动） | 站点、后台用户与角色 | `LocalDevSeeder`（`application` 的 `com.b2bweb.app.dev`，5.14.3）：只在 `local` profile 且 `b2bweb.dev.seed.enabled = true`（默认）时，于启动步骤 8 之后经 core 的应用服务创建，不直接写表；按自然键（站点代码、用户邮箱）幂等 |
| 演示数据（手动） | 9.7 的 `minimal` / `full` 数据集 | `b2b.mjs seed` 即 `node tools/demo-data/load.mjs --dataset minimal --base http://localhost:8080 --site site-a`；加载器以种子管理员登录（密码与 TOTP 取 `.env`），只经后台接口加载（9.7） |

**基础种子**（与 testkit 的 `TestSites`、`TestUsers` 同名，9.2.7）：

| 对象 | 取值 |
|---|---|
| 站点 | `site-a`（主域名 `site-a.localhost`）、`site-b`（`site-b.localhost`）；主题 `industrial`；源语言 `zh-CN`，另启用 `en`、`de`（`AUTO` 模式）；只有 `zh-CN` 对外公开（与前台 V1 一致），开放英语留给开发者与 E2E 场景 S11 演练 |
| 用户 | `admin@example.com`（系统管理员）、`editor@example.com`（站点 A 内容权限）、`viewer@example.com`（站点 A 只读）、`sales@example.com`（站点 A 询盘，数据范围 `OWN`）、`only-b@example.com`（只在站点 B 有角色）；密码均为 `.env` 的 `B2B_DEV_PASSWORD` |
| 两步验证 | 照常强制（发布、管理、开发者权限的角色，系统设计 26.1）；种子用户使用 `.env` 中固定的 `B2B_DEV_TOTP_SECRET`，`b2b.mjs totp admin` 打印当前验证码，E2E 夹具与演示数据加载器用同一密钥计算；不提供关闭两步验证的开关 |
| 生产防护 | `b2bweb.dev.*` 在 `staging`、`prod` 中开启时，生产安全检查拒绝启动（5.14.4） |

**演示数据的本地约定**：

| 项 | 说明 |
|---|---|
| 模块未实现 | 加载器读取站点启用的模块，接口不存在（尚未实现或由替身补位）时跳过并警告（9.7） |
| 翻译引擎 | translation 的加载器（两个数据集都执行）创建引擎配置“本地模拟”：`baseUrl = http://localhost:4010/v1`（应用以容器运行时为 `http://mock-openai:4010/v1`）、模型 `mock-translate`、API Key `mock-key`（不是密钥，照常加密保存），并设为站点 A 的主引擎；只在 `--engine mock` 时实际被调用 |
| 重新开始 | 重新执行 `seed` 只补缺，不删除或还原已修改的数据；需要干净的数据时 `b2b.mjs reset` → `dev` → `seed` |

### 11.7 本地 OpenAI 兼容模拟服务（mock-openai）

**用途**：`FakeTranslationEngine`（`fake`）在进程内工作，不经过 HTTP；mock-openai 让真实的 `OpenAiCompatibleTranslationEngine`（5.12.2）与完整的翻译流程（提取 → 翻译记忆 → 引擎 → 校验 → 回写，系统设计 5.7）在不产生费用、不访问外网的情况下运行。使用场景：本地 `--engine mock`；E2E 中 S1 的英语部分与 S11–S13（9.6.3）；dev 与 staging 的 E2E 测试站点（只在测试网络内可达，生产不部署）。

```text
tools/mock-openai/
├── server.mjs        # 只依赖 Node 标准库（node:http）
├── translate.mjs     # 伪译文规则：与 FakeTranslationEngine 相同（9.2.4）
├── Dockerfile
└── __tests__/        # 伪译文规则与各故障场景的测试（node:test）
```

| 端点 | 行为 |
|---|---|
| `POST /v1/chat/completions` | 校验 `Authorization: Bearer <非空>`（值为 `invalid` 时返回 401）；从最后一条 `user` 消息读取 JSON `{source, target, items: [{id, text, maxLength, context}]}`（5.12.2）；每个单元生成伪译文 `[{target}] 原文`，原样保留编号占位标签与数字；返回标准的 chat completion 响应：`choices[0].message.content` 为 `{"items":[{"id","text"}]}`、`finish_reason = "stop"`、`model` 原样回显、`usage` 按字符数估算。按请求的 `response_format` 返回：`json_schema` / `json_object` 为纯 JSON；没有 `response_format` 时用 ```` ```json ```` 代码块围栏包裹（覆盖适配器 `NONE` 模式的去围栏逻辑） |
| `GET /v1/models` | 返回 `mock-translate`（引擎配置的“测试连接”） |
| `GET /__mock/requests` | 请求记录：时间、模型、源语言与目标语言、单元 ID 与原文；支持 `?target=en&contains=<文本>` 过滤（S11、S12 断言“翻译记忆命中不调用引擎”“只有被修改的段落调用引擎”） |
| `DELETE /__mock/requests` | 清空请求记录 |
| `POST /__mock/scenario` | 设置后续请求的行为，`{"mode": …, "times": n}`，`times` 用完后恢复 `ok`：`ok`；`delay`（毫秒）；`status`（429 带 `Retry-After`、500、401、400“上下文超长”）；`timeout`（不响应）；`invalid-json`；`missing-id`、`extra-id`（单元 ID 不一一对应）；`length`（`finish_reason = "length"`）；`drop-placeholder`（丢失占位标签，触发 translation 的译文校验失败） |
| `POST /__mock/reset` | 清空请求记录并恢复 `ok` |
| `GET /health` | 健康检查 |

| 规则 | 说明 |
|---|---|
| 与 `FakeTranslationEngine` 一致 | 两者的伪译文规则相同，本地以 `fake` 或 `mock` 运行得到相同的译文，便于对照单元测试与本地结果 |
| 状态 | 请求记录与场景只在内存中，重启清空；E2E 用例按自己数据中的运行 ID 后缀过滤记录（9.6.3）；会改变全局行为的故障场景只在串行执行的用例中使用 |
| 数据 | 只接收测试与演示数据；请求记录中的原文不得来自生产数据（staging 的脱敏数据不经过它，只有 E2E 测试站点使用它） |
| 不用于性能测试 | 默认延迟 0–50 ms（`MOCK_LATENCY_MS` 可调），不代表真实引擎的耗时与限流 |
| 真实引擎 | 想在本地试用真实服务商时，在后台新建一个引擎配置并录入自己的 API Key（加密保存在本地数据库），设为站点主引擎，并以 `--engine real` 启动（与 `mock` 一样设为 `openai-compatible`，只是不要求 mock-openai 运行）；用量计入翻译预算（系统设计 5.10）；不得把 API Key 写入 `.env`、配置文件或演示数据（`CLAUDE.md` 禁止 12） |

### 11.8 常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动时报端口被占用 | `b2b.mjs` 会报告占用的进程；在 `.env` 中修改对应的 `B2B_PORT_*` |
| 后端启动失败：迁移校验和不一致 | 本地修改过已执行的迁移脚本（合入时 G4 也会拦截）：恢复脚本；只在自己分支上试验时用 `b2b.mjs reset` 重建数据库 |
| 后端一直返回 503 `CORE_SERVICE_STARTING` | 启动步骤 8 未完成（5.2.2）：查看日志中的模块描述符校验、组件 Schema 加载或迁移错误 |
| 后台登录后又回到登录页 | 会话 Cookie `__Host-SESSION` 带 `Secure`（系统设计 26.1）：Chrome、Edge、Firefox 把 `http://localhost` 视为安全上下文，可以保存；Safari 不保存——改用这些浏览器，或用 `b2b.mjs dev --https`（mkcert 证书，Admin 与 Nuxt 以 HTTPS 启动） |
| `site-a.localhost` 无法解析 | 浏览器可以直接解析；命令行工具或系统解析器不支持时，在 hosts 文件加入 `127.0.0.1 site-a.localhost site-b.localhost` |
| 前台 404 | 以 `localhost:3000` 访问（未登记的域名），改用 `site-a.localhost:3000`；或基础种子尚未执行（`b2b.mjs status`）；或该语言未对外公开（本地只公开 `zh-CN`，11.6） |
| 前台显示 503 维护页 | 后端未就绪或内部令牌不一致（前台不会因此返回 404，8.3.2）：检查 `b2b.mjs status` 与 `.env` 的 `B2BWEB_INTERNAL_TOKENS` |
| 图片不显示 | imgproxy 与后端的 key / salt 不一致，或公开桶策略缺失：核对 `.env` 后重新运行 `b2b.mjs up`（会重跑 `minio-init`） |
| 搜索没有结果 | Meilisearch 数据卷被删除或索引前缀不同：在后台执行搜索模块的重建索引（从数据库重建，系统设计 27.5） |
| 收不到邮件 | 本地全部邮件都被 Mailpit 截获，在 `http://localhost:8025` 查看，不会发到真实邮箱 |
| 目标语言版本没有生成 | 站点未启用该语言或不是 `AUTO`；`--engine mock` 时 mock-openai 未运行或仍处于故障场景（`POST /__mock/reset`）；查看 translation 的任务列表 |
| 某个模块的菜单或接口不存在 | 该模块尚未实现、由替身补位（替身没有 REST 接口，9.2.8），或在站点中被停用：`b2b.mjs status` 列出补位清单与启用状态 |
| 改了接口，前端类型没有更新 | 运行 `b2b.mjs api`（8.5.1） |
| 改了 Token，样式没有变化 | 运行 `pnpm gen:tokens`（Token 产物不提交，8.6.1） |
| 集成测试很慢 | 本地开启 Testcontainers 容器复用 `testcontainers.reuse.enable=true`（9.3.1）；只测一个模块用 `b2b.mjs check --module <id>`（10.7） |
| Docker 内存不足导致 MySQL 或 ClamAV 退出 | 提高 Docker 内存（11.1）；不需要病毒扫描时不启用 `scan` profile |

---

## 12. 模块设计文档模板与开发流程

### 12.1 文档位置与编写规则

**位置**：每个业务模块一份，路径 `docs/modules/<模块ID>.md`（共 17 份：L2 content、url、media、taxonomy、form、notification、translation；L3 product、article、page、inquiry、navigation；L4 delivery、seo、graph、search、tracking）。platform-core 的设计在本文第 5、6 章中，不另建模块文档。V2+ 的新模块使用同一模板，并且先写 ADR（系统设计 29.5）。

**编写规则**：

| 规则 | 说明 |
|---|---|
| 结构固定 | 使用 12.2 的模板，14 个小节的编号与标题不变（其他文档与评审清单按编号引用）；不适用的小节保留标题，写“不适用：<原因>”，不得删除 |
| 只引用契约 | 第 2 节中的服务接口、扩展点、事件、端口一律写本文第 4 章中的名称并注明小节号；不得在模块文档中定义新的跨模块接口、事件或 DTO（ADR-018）。缺少契约时按 2.3 提出契约变更请求 |
| 数据模型唯一来源 | 本模块的表结构与 DDL 只在本文档第 4 节维护，与迁移脚本保持一致（ADR-018）；遵守本文第 7 章的通用约定；DDL 在 MySQL 8.4 上执行通过后才能评审 |
| 不复述上层文档 | 系统设计与本文已有的规定只引用章节号，不复制内容；模块文档只写本模块的决定与细节 |
| 语言 | 正文中文；代码标识符英文；`module.json`、组件与模板定义中的名称、标签只写中文字符串（`CLAUDE.md` 语言规则 1） |
| 可验证 | 第 13 节的每一条验收项都能对应到测试（单元、集成、E2E）或明确的人工检查 |
| 状态 | `草案` → `已评审`（满足 DoR，可以开发）→ `已实现（M?）`（该里程碑切片满足 DoD）；之后的修改提升文档版本并记入修订记录 |

### 12.2 标准模板

脚手架（3.8）生成的 `docs/modules/<id>.md` 即为以下模板。尖括号中的内容需要替换，引用块中的说明在编写完成后删除。

````markdown
# <模块名称>（<模块 ID>）模块设计

| 项 | 内容 |
|---|---|
| 模块 ID | `<id>` |
| 名称 | <中文名称，与 module.json 的 name 一致> |
| 层 | L2 / L3 / L4 |
| 表前缀 | `<前缀>_`（无表时写“无”） |
| API 前缀 | `<apiPrefix>` |
| 可停用 | 是 / 否 |
| 模块版本 | 1.0.0（module.json 的 version） |
| 平台版本 | `>=1.0.0 <2.0.0`（module.json 的 platform） |
| 里程碑切片 | M1：<…>；M2：<…>；M3：<…>；M4：<…>（详见第 13 节） |
| 模块负责人 | @<账号> |
| 文档版本 | V1.0 |
| 状态 | 草案 / 已评审 / 已实现（M?） |
| 更新日期 | YYYY-MM-DD |
| 依据 | 系统设计 V1.8 第 <…> 章；平台框架设计 V1.<x> 第 4 章 <…> 节；ADR-<…> |

> 本文只引用《平台框架设计》第 4 章中的契约，不定义新的跨模块契约；需要新契约时按平台框架设计 2.3 提出契约变更请求。

## 1. 职责与边界

### 1.1 职责

> 一段概述 + 要点列表；与系统设计 3.2 的职责描述一致，并细化到本模块的功能清单。

### 1.2 不负责的事

| 事项 | 归属模块 | 说明 |
|---|---|---|

### 1.3 协作概览

> 用图或表说明本模块与哪些模块协作、通过哪种方式（服务接口 / 事件 / 扩展点）。只画契约，不画实现。

### 1.4 适用的架构约束

> 列出与本模块直接相关的原则（P1–P8）、ADR 规则与 CLAUDE.md 禁止项，以及本模块如何满足。

## 2. 依赖与契约

### 2.1 module.json

```json
{
  "id": "<id>",
  "name": "<中文名称>",
  "version": "1.0.0",
  "layer": "L3",
  "platform": ">=1.0.0 <2.0.0",
  "requires": {},
  "optional": {},
  "disableable": true,
  "tablePrefix": "<前缀>_",
  "apiPrefix": "<id>",
  "permissions": [],
  "menus": [],
  "settings": null,
  "components": [],
  "events": { "publishes": [], "subscribes": [] }
}
```

> 完整描述符，须通过平台框架设计附录 A 的 Schema 与 A.3 的校验规则。

### 2.2 使用的服务接口

| 接口（平台框架设计小节） | 方法 | 用途 | 调用位置与时机（事务内 / 外） | 测试替身 |
|---|---|---|---|---|

### 2.3 提供的服务接口

| 接口（平台框架设计小节） | 方法 | 实现类 | 契约测试 | 说明（幂等、权限、性能目标） |
|---|---|---|---|---|

### 2.4 实现的扩展点

| 扩展点（平台框架设计小节） | 实现类 | 适用范围（内容类型 / 资源类型） | order | 收集方 |
|---|---|---|---|---|

### 2.5 收集的扩展点

| 扩展点 | 调用时机 | 过滤（站点启用状态、内容类型等） | 实现缺失或抛出异常时的行为 | 测试用示例实现 |
|---|---|---|---|---|

### 2.6 发布与订阅的事件

发布：

| 事件 | 触发时机（所在事务） | 关键字段 | 主要订阅方 |
|---|---|---|---|

订阅：

| 事件 | 处理 | 幂等方式 | 过期判断 | 失败与重试 |
|---|---|---|---|---|

### 2.7 使用的端口

| 端口 | 用途 | 测试替身 |
|---|---|---|

## 3. 领域模型

### 3.1 聚合、实体与值对象

| 类型 | 名称 | 说明 |
|---|---|---|

### 3.2 不变量

1. <规则>——<数据库约束 / 应用层>。

### 3.3 状态机

```text
<状态与转换；没有状态机时写“不适用”>
```

## 4. 数据模型

> 遵守平台框架设计第 7 章的数据库通用约定。DDL 必须在 MySQL 8.4 上执行通过；表结构以本节为准，与迁移脚本保持一致。

### 4.1 表清单

| 表 | 说明 | 站点级 | 软删除 | 预估规模（单站，按系统设计假设 A4） |
|---|---|---|---|---|

### 4.2 ER 图

```mermaid
erDiagram
    <表A> ||--o{ <表B> : "has"
```

> 只画本模块的表；跨模块关系不画，在 4.6 中列出。

### 4.3 表结构

#### 4.3.1 `<表名>`：<中文说明>

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|

索引与约束：

- `PRIMARY KEY (id)`
- `uk_<…>`：<用途>
- `idx_<…>`：<服务于哪个查询>

```sql
CREATE TABLE <表名> (
  …
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '<中文表说明>';
```

### 4.4 枚举

| 字段 | 取值 | 说明 |
|---|---|---|

### 4.5 站点过滤

| 表 | 行级拦截器（不豁免 / 豁免） | 访问方式与理由 |
|---|---|---|

### 4.6 跨模块引用与清理

| 本模块字段 | 引用对象（模块.表） | 被引用对象删除 / 下线时的处理 | 触发事件 |
|---|---|---|---|

### 4.7 迁移脚本

| 文件 | 内容 | 里程碑 |
|---|---|---|
| `db/migration/<id>/V001__init.sql` | 4.3 中的全部表 | M? |

## 5. 业务流程

### 5.1 <流程名称>

```text
<步骤；标出事务边界、调用的服务接口、发布的事件、异步部分>
```

| 步骤 | 说明 | 失败处理 |
|---|---|---|

## 6. 接口

### 6.1 Admin 端点

| 方法 | 路径（/api/admin/v1/<apiPrefix>/…） | 说明 | 权限码 | 并发控制 / 幂等 | 审计 |
|---|---|---|---|---|---|

### 6.2 Public 端点

| 方法 | 路径（/api/public/v1/<apiPrefix>/…） | 说明 | 限流 | 缓存 |
|---|---|---|---|---|

### 6.3 Internal 端点

| 方法 | 路径（/api/internal/v1/<apiPrefix>/…） | 说明 | 调用方 |
|---|---|---|---|

### 6.4 权限码

| 权限码 | 名称 | 数据范围 | 高危（sensitive） | 受语言范围限制（localeScoped） |
|---|---|---|---|---|

### 6.5 错误码

| 错误码 | HTTP 状态 | 后台中文信息 | 前台文案键（仅公开接口） |
|---|---|---|---|

## 7. 后台界面

### 7.1 菜单与路由

| 菜单 key | 名称 | 父菜单 | 路由 | 权限码 |
|---|---|---|---|---|

### 7.2 页面

| 页面 | 路由 | 主要功能 | 使用的 admin-kit 组件 |
|---|---|---|---|

### 7.3 注册到内容编辑外壳的标签

| key | 适用内容类型 | 权限码 | 组件 |
|---|---|---|---|

### 7.4 界面文案

> texts.ts 中的分组与关键文案；后台只用简体中文。

## 8. 前台组件与模板

> 没有前台组件时写“不适用”。

### 8.1 组件

| 组件类型 | 名称（label） | 变体 | 分类 | allowedIn | 数据源 | 是否水合 |
|---|---|---|---|---|---|---|

### 8.2 属性与翻译

| 组件 | 属性 | 类型 / x-editor | x-translatable | maxLength |
|---|---|---|---|---|

### 8.3 模板

| 模板 key | 内容类型 | 固定区域与插槽 |
|---|---|---|

### 8.4 数据源

| key | 参数 | 返回 | 缓存标签 |
|---|---|---|---|

### 8.5 文案键

| 文案键 | 默认中文文案 | 使用位置 |
|---|---|---|

## 9. 配置项

### 9.1 站点设置（settings.schema.json）

| 键 | 类型 | 默认值 | 说明 | 敏感（x-secret） |
|---|---|---|---|---|

### 9.2 部署配置（application.yml，前缀 b2bweb.<id>）

| 属性 | 默认值 | 说明 |
|---|---|---|

## 10. 定时与异步任务

> 没有时写“不适用”。定时任务实现 `ScheduledTask`，通用异步任务实现 `JobHandler`（平台框架设计 4.4.12）；锁名与任务类型按平台框架设计 3.7 命名。事件监听器已在 2.6 中列出，这里只补充耗时长、需要单独调度或重试的处理。`PER_SITE` 任务由 core 在站点上下文中逐站点执行，停用了本模块的站点自动跳过（系统设计 3.8）。外部服务（AI 引擎、邮件、CDN）只在事务提交后的异步处理中调用，失败不回滚已提交的业务数据（ADR-003、ADR-017）。

| 任务 | 类型（定时 / sys_job / 事件监听） | 触发 | 锁名 | 幂等与重试 | 模块停用时 |
|---|---|---|---|---|---|

> 每个任务另写：执行粒度（`PER_SITE` / 系统级）、单次处理量与分批方式、超时、能否取消、失败后的可见结果（`sys_job` 的失败报告、指标 `b2bweb.<id>.*` 或告警）。

## 11. 测试要点

> 测试层级、替身与契约测试的写法见平台框架设计第 9 章，门禁与覆盖率见第 10 章。“至少”后的用例为必测项；不适用的行写“不适用：<原因>”，不得删除。

| 层级 | 覆盖内容 | 关键用例 |
|---|---|---|
| 单元 | <领域对象、不变量（3.2）、状态机（3.3）、扩展点实现（2.4）> | 至少：3.2 每条不变量的违反用例；3.3 每个非法状态转换被拒绝 |
| 模块集成（Testcontainers + 替身） | <应用服务 + 本模块的表与迁移> | 至少：迁移脚本在空库（MySQL 8.4）上执行通过；6.1 中声明的并发控制与幂等；唯一约束冲突转为 6.5 的错误码 |
| 契约（作为提供方） | <2.3 中本模块提供的服务接口> | 继承 testkit 中的 `<接口名>ContractTest`，用真实实现运行；不提供服务接口时写“不适用” |
| 事件（Scenario API，含重复投递） | <2.6 中发布与订阅的事件> | 至少：事件在业务事务内发布、事务回滚时不发布；每个监听器重复投递只生效一次、过期事件被丢弃（ADR-003） |
| 停用行为（可停用模块） | <系统设计 3.8 的各项> | 至少：停用后写操作与公开接口返回 `CORE_MODULE_DISABLED`；扩展点实现不再被收集；定时任务跳过；`analyzeDisableImpact` 的结果正确；重新启用后数据完整（S10） |
| 站点隔离 | <6.1 的全部后台接口与 4.5 的全部站点级表> | 至少：站点 A 的用户不能读写站点 B 的数据（遍历全部后台端点，系统设计 28）；豁免表只在 4.5 列出的位置访问 |
| 翻译（如有可翻译字段） | <`TranslatableContentProvider` / `TranslatableResourceProvider` 实现> | 至少：提取 → 回写往返一致；`x-translatable = false` 的字段不被提取；`HUMAN` / `REVIEWED` 段不被机器译文覆盖；资源变化时发布 `TranslatableResourceChangedEvent` |
| 前端（组件、Admin） | <主题组件、Admin 页面> | 至少：组件 Schema 校验与画廊示例（视觉回归）；前台文字只来自文案键、后台文字只来自 `texts.ts`；无权限时操作不可见 |
| E2E（场景） | <13.2 中验证方式为 E2E 的验收项> | Playwright 用例名包含 13.2 的验收项编号 |

## 12. 独立开发说明

### 12.1 依赖的测试替身

> 2.2 使用的每个服务接口、2.7 使用的每个端口、2.5 收集的每个扩展点，都要在此列出 testkit 中对应的替身或示例实现（平台框架设计 2.4、第 9 章）。testkit 中缺少的替身向框架组提出需求（DoR 第 3 条，平台框架设计 2.7），不在本模块中另建一套代替其他模块的模拟实现；单元测试可以用 Mockito 模拟个别方法，但对其他模块行为的假设以替身与契约测试为准。

| 替身（platform-testkit） | 代替的模块 | 用途 | 需要预置的数据 |
|---|---|---|---|

### 12.2 本模块提供的替身

> 只有提供服务接口的模块填写（core、content、url、media、taxonomy、form、notification、translation，见平台框架设计 4.2）；其他模块写“不适用：不提供服务接口”。替身由框架组编写并维护，与真实实现通过同一套契约测试；本模块改变服务接口的行为时，在同一个 PR 中同步修改契约测试，并通知框架组修改替身（平台框架设计 2.3）。无法消除的差异（如不做病毒扫描、不生成真实缩略图）列入下表，并确认不影响使用方的测试结论。

| 服务接口 | testkit 中的替身 | 与真实实现的差异（应为零，或已在契约测试中声明） |
|---|---|---|

### 12.3 本地运行

> 启动方式、依赖的本地服务（平台框架设计第 11 章）、需要的配置。

### 12.4 演示数据

> tools/demo-data 中本模块的数据及其用途。

## 13. 验收标准

### 13.1 里程碑切片

> 与平台框架设计 13.5 中本模块的一行一致；调整切片按平台框架设计 12.5 处理。该里程碑没有切片时写“—”（不表示不能提前开发）。

| 里程碑 | 交付功能 | 验收场景 |
|---|---|---|
| M1 | <…> | <S1、S2、S4 中与本模块相关的部分，或“模块自身”> |
| M2 | <…> | <…> |
| M3 | <…> | <…> |
| M4 | <…> | <…> |

### 13.2 验收项

> 每条验收项写明输入、期望结果与度量（如“5 分钟内”“P95 ≤ 200 ms”）。场景验收项只写本模块负责的部分（如 S2 中 url 负责“旧 URL 301 到新 URL、不产生重定向链”），不复述整个场景；各里程碑的场景见平台框架设计 13.6。验证方式为单元 / 集成 / E2E 时写出测试类或用例名；人工检查写明检查人与方法。

| # | 验收项 | 对应场景（S1–S13）或模块自身 | 验证方式（单元 / 集成 / E2E / 人工） | 里程碑 |
|---|---|---|---|---|
| A1 | <…> | <S? / 模块自身> | <集成：`<测试类>`> | M? |

> 每个模块都要包含以下通用验收项（不适用时注明原因）：架构测试与 Modulith 验证通过，无跨模块 import 与表访问；站点隔离测试覆盖全部后台接口；后台写操作都有权限码与审计记录；可停用模块满足 S10；可翻译字段与资源已登记并通过往返测试（S11、S12）；系统设计 1.5 中与本模块相关的非功能指标；文档与代码一致（DoD，平台框架设计 2.8）。

## 14. 待定问题

> 记录评审与开发中尚未决定的事项。“阻塞开发”的问题必须在评审通过（DoR）前关闭；需要新契约或修改上层文档的问题，写明契约变更请求或 Issue 编号。已关闭的问题保留在表中，填写结论。

| # | 问题 | 影响 | 是否阻塞开发 | 负责人 | 最晚解决 | 结论 |
|---|---|---|---|---|---|---|

## 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| V1.0 | YYYY-MM-DD | 初稿 |
````

### 12.3 模块从设计到合并的流程

```text
 ① 指派        架构负责人指定模块负责人，登记 CODEOWNERS（2.6）
     │
 ② 编写        分支 docs/<id>-design；按 12.2 模板编写；DDL 在 MySQL 8.4 上执行通过
     │
 ③ 契约核对    第 2 节的每个契约在本文第 4 章中存在？── 否 ──▶ 契约变更请求（2.3）──┐
     │ 是                                                                       │
     ◀──────────────────────────────── 契约变更合入 ◀───────────────────────────┘
     │
 ④ 评审        PR：模块负责人提交；架构负责人 + 相关模块负责人评审（12.4 清单）
     │          通过 = DoR 满足（2.7），文档状态改为“已评审”，squash 合入主干
     │
 ⑤ 开发        短分支 feat/<id>-<主题>；用 testkit 替身；每个 PR 满足 PR 级 DoD（2.8）
     │          文档随代码在同一个 PR 中更新（12.5）
     │
 ⑥ 合并        CI 全部门禁通过；CODEOWNERS 批准；squash 合并；main 自动部署 dev
     │
 ⑦ 集成        替身退场（2.5）；周集成演示；跨模块问题在集成看板跟踪
     │
 ⑧ 验收        里程碑场景验收；切片级 DoD 满足后文档状态改为“已实现（M?）”
```

| 步骤 | 负责人 | 输入 | 输出 | 时限建议 |
|---|---|---|---|---|
| ① 指派 | 架构负责人 | 13.4 的分组 | CODEOWNERS 条目 | M0 开始时 |
| ② 编写 | 模块负责人 | 系统设计相关章节；本文第 4、7 章 | 模块文档草案 | 每份 3–5 个工作日 |
| ③ 契约核对 | 模块负责人；框架组 | 模块文档第 2 节 | 契约变更请求或确认 | 契约变更请求 2 个工作日内答复 |
| ④ 评审 | 架构负责人；相关模块负责人 | 文档 PR | 已评审的文档（DoR） | 2 个工作日 |
| ⑤ 开发 | 模块负责人 | 已评审的文档 | PR | 按切片计划 |
| ⑥ 合并 | 模块负责人；评审人 | PR | 主干上的代码 | — |
| ⑦ 集成 | 框架组主持 | dev 环境 | 集成看板 | 每周 |
| ⑧ 验收 | 架构负责人、业主 | 场景 E2E 与人工检查 | 验收报告 | 每个里程碑末 |

“相关模块负责人”指本模块使用其服务接口、订阅其事件、或收集 / 实现其扩展点的模块的负责人；他们确认协作方式与预期一致，但无权要求在模块文档中定义新契约。

### 12.4 模块文档评审清单

| # | 检查项 | 对应 |
|---|---|---|
| 1 | 基本信息表完整；模块 ID、层、表前缀、API 前缀、可停用与系统设计 3.2 一致 | 系统设计 3.2 |
| 2 | 职责与边界清楚；“不负责的事”写明归属模块；没有侵入其他模块的职责 | P2、R5 |
| 3 | `module.json` 通过附录 A 的 Schema 与 A.3 的规则；`requires` 只包含允许调用的层（R2），无循环依赖 | 附录 A |
| 4 | 第 2 节中的每个契约都存在于本文第 4 章；没有自定义的跨模块接口、事件、DTO | ADR-018 |
| 5 | 选择服务接口、事件还是扩展点符合 P3；没有在发布事务中同步调用 AI 等外部服务 | P3、ADR-003、ADR-017 |
| 6 | 事件监听器写明幂等方式与过期判断 | ADR-003 |
| 7 | 表结构遵守第 7 章；站点级表带 `site_id`，全局表列入豁免清单并说明理由；没有跨模块外键与 JOIN；DDL 已在 MySQL 8.4 上执行通过 | 系统设计 25；ADR-009 |
| 8 | 跨模块引用列出了清理方式与触发事件 | 系统设计 25.5 |
| 9 | 接口路径、权限码、错误码、事件名、组件类型、文案键符合 3.7；后台接口都有权限码与审计说明 | 3.7；系统设计 24、26 |
| 10 | 前台组件不硬编码文案与业务字段；属性正确标注 `x-translatable`；样式只用 Token；布局中引用对象化 | P5、P7；系统设计 10.3、10.4 |
| 11 | 可翻译字段与站点资源已列出提取与回写方式（`TranslatableContentProvider` / `TranslatableResourceProvider`），资源变化时发布 `TranslatableResourceChangedEvent` | 系统设计 5.5–5.7 |
| 12 | 新内容类型实现了系统设计 29.2 第 3 步的全部扩展点 | 系统设计 29.2 |
| 13 | 可停用模块写明停用行为与 `analyzeDisableImpact` 的内容 | 系统设计 3.8 |
| 14 | 第 12 节写明依赖的替身与本模块提供的替身，替身都已在 testkit 中存在或已提出需求 | 2.4 |
| 15 | 第 13 节的验收项可测试，并对应到场景 S1–S13 或模块自身的验收项；里程碑切片与 13.5 一致 | 系统设计 1.3、30 |
| 16 | 第 14 节中阻塞开发的问题已关闭 | 2.7 |

### 12.5 开发中的变更

| 变更 | 处理方式 |
|---|---|
| 实现细节变化（流程、接口参数、页面） | 在同一个代码 PR 中更新模块文档；文档次版本 +1 |
| 表结构变化 | 新增迁移脚本（已合入的脚本不得修改），同一个 PR 中更新第 4 节的表结构与 DDL；遵循 expand / contract；不兼容的数据模型变更先写 ADR（系统设计 25.6、29.5） |
| 需要新的或修改已有的跨模块契约 | 停止在模块内绕行，按 2.3 提出契约变更请求；契约合入后再更新模块文档第 2 节 |
| 里程碑切片调整 | 模块负责人提出，架构负责人与业主确认后修改第 13 节与本文 13.5 |
| 发现与本文或系统设计冲突 | 先在 PR 或 Issue 中指出冲突点，由架构负责人决定修订哪一层文档（1.2），不在模块文档中自行覆盖上层规定 |

---

## 13. 并行开发计划

### 13.1 开工前提：契约冻结与 testkit 就绪

M0 结束、模块全面并行开工的条件（同时作为 M0 的验收清单，补充系统设计 30 章的 M0 验收标准）：

| # | 条件 | 判定方式 |
|---|---|---|
| 1 | `platform-api` 1.0.0 发布（契约冻结）：本文第 4 章的全部契约有代码与 Javadoc | 架构负责人批准；发布标签 |
| 2 | `platform-testkit` 就绪：每个服务接口有内存实现，每个端口有假实现，每个扩展点有示例实现，契约测试套件齐全 | testkit 自身的测试通过；替身通过对应的契约测试 |
| 3 | platform-core 的运行机制可用：模块管理器（描述符、依赖校验、迁移、启用开关）、扩展点注册表、事件登记与重新投递、站点上下文与行级拦截、统一错误处理 | 第 5 章所列测试通过 |
| 4 | infrastructure 的适配器至少有本地可用的实现（MinIO、Mailpit、Meilisearch、假 AI 引擎等，第 11 章） | 本地环境冒烟 |
| 5 | `application` 可启动；本地配置中缺失的服务接口由替身补位（2.4） | 本地启动冒烟 |
| 6 | 前端框架：Admin 外壳与 `defineAdminModule`；Nuxt 渲染框架、组件画廊；`component-schemas` 校验；`api-client` 生成 | 第 8 章所列检查通过 |
| 7 | CI 全部门禁上线；“违反边界的示例模块”使 CI 失败 | 系统设计 30（M0 验收）；第 10 章 |
| 8 | 脚手架可用，按模板新建一个模块并接入 ≤ 1 小时；17 个业务模块的骨架已由脚手架生成并合入主干（3.8） | 计时演练；主干构建通过 |
| 9 | 本地开发环境一键启动（第 11 章） | 新成员按 README 在半天内跑通 |
| 10 | 本文当前冻结候选版评审通过；M1 切片涉及的模块设计文档评审通过（DoR） | 文档状态 |
| 11 | ADR-011 的版本表填写完成并通过评审 | ADR-011 状态改为 Accepted |

M0 的内部顺序建议：先完成第 4 章契约草案并做一次走查（以发布流程 4.4、询盘流程 17.2、翻译流程 5.7 三条链路检查契约是否完整），再并行实现 platform-core、testkit、infrastructure 与前端框架；契约草案走查通过后，模块负责人即可开始编写模块设计文档（2.2）。

### 13.2 模块依赖图

**运行时依赖**（`module.json` 的 `requires` / `optional`）。下表为根据系统设计各章推导的建议值，最终以各模块设计文档中的 `module.json` 为准；所有模块隐含依赖 core，不写入 `requires`。

| 模块 | 层 | requires | optional | 依据 |
|---|---|---|---|---|
| url | L2 | — | — | 系统设计 13 |
| media | L2 | — | — | 系统设计 12 |
| notification | L2 | — | — | 系统设计 18 |
| content | L2 | url | — | 发布事务内预留路径、更新路由并生成 301（系统设计 4.4） |
| taxonomy | L2 | content | — | 可路由分类项自动拥有 `TERM_PAGE` 内容（系统设计 7.3） |
| translation | L2 | content | — | 目标语言本地化不存在时由内容内核创建（系统设计 5.7） |
| form | L2 | media、notification | — | 附件与留资文档的下载票据（`MediaApi`）；下载链接邮件（`NotificationApi`，4.4.21）（系统设计 8.4、17.3）；form → notification 须同时列入 4.1.3 的 L2 → L2 调用方向表 |
| product | L3 | content、media、taxonomy | form | 内容、图集与文档、分类与属性集（系统设计 8）；留资文档引用表单 |
| article | L3 | content、media、taxonomy | — | 系统设计 9 |
| page | L3 | content、media | — | 布局文档引用内容与媒体（系统设计 10.3） |
| inquiry | L3 | content、taxonomy、form、notification | product、translation | 系统设计 3.5 的描述符示例 |
| navigation | L3 | content、taxonomy | — | 菜单链接到内容与分类项（系统设计 16.1） |
| delivery | L4 | content、url、media、taxonomy | — | 解析路由、组装渲染数据、解析引用（系统设计 24.4） |
| seo | L4 | content、url、media | — | Meta、canonical、hreflang、含图片的 sitemap（系统设计 14） |
| graph | L4 | content、taxonomy | — | 候选集与评分基于发布态分类（系统设计 15.3） |
| search | L4 | content、taxonomy | — | 索引文档包含分类名，分类变化时更新（系统设计 19、4.3） |
| tracking | L4 | — | — | 系统设计 20 |

```mermaid
flowchart BT
    subgraph L2["L2 基础能力"]
        content
        url
        media
        taxonomy
        form
        notification
        translation
    end
    subgraph L3["L3 业务"]
        product
        article
        page
        inquiry
        navigation
    end
    subgraph L4["L4 聚合与横切"]
        delivery
        seo
        graph
        search
        tracking
    end
    content --> url
    taxonomy --> content
    translation --> content
    form --> media & notification
    product --> content & media & taxonomy
    product -.-> form
    article --> content & media & taxonomy
    page --> content & media
    inquiry --> content & taxonomy & form & notification
    inquiry -.-> product
    inquiry -.-> translation
    navigation --> content & taxonomy
    delivery --> content & url & media & taxonomy
    seo --> content & url & media
    graph --> content & taxonomy
    search --> content & taxonomy
```

实线为 `requires`，虚线为 `optional`；core 未画出。图中没有环；`requires` 只指向 L2（系统设计 3.3 R2），`inquiry -.-> product` 是同层的可选增强，通过 `ContentVariantProvider` 扩展点实现，不是服务调用（系统设计 3.5）。

**集成顺序**（按依赖拓扑排序，同一行可以同时集成）：

```text
1. core
2. url、media、notification、tracking
3. content
4. taxonomy、translation、form
5. product、article、page、inquiry、navigation
6. delivery、seo、graph、search
```

**扩展点带来的集成耦合**：扩展点不构成 `requires`，但构成集成耦合——收集方的完整行为要等实现方集成后才能端到端验证。M1 最关键的几处：

| 收集方 | 扩展点 | V1 实现方（系统设计 3.7） | 验证方式 |
|---|---|---|---|
| content | `ContentTypeProvider`、`PublishContributor` | product、taxonomy、page、seo（及 article） | 开发期用 testkit 示例内容类型；M1 用 PRODUCT 端到端验证快照与投影 |
| delivery | `TemplateProvider`、`DeliveryContributor`、`DataSourceProvider` | page；seo；content、graph、taxonomy、article、search | 开发期用示例模板与示例数据源；M1 用 `product-detail` 模板端到端验证 |
| seo | `SchemaOrgProvider`、`SeoVariableProvider`、`BreadcrumbProvider` | product、article、page、core；product、article、taxonomy | M1 验证 Product、BreadcrumbList、Organization JSON-LD |
| translation | `TranslatableContentProvider`、`TranslatableResourceProvider` | 见系统设计 3.7（十余个模块） | 开发期用示例实现；M3 逐个模块端到端验证 |
| content、资源拥有者/core | `MediaReferenceCollector` → `ReferenceWriteApi` → media的`MediaReferenceSink` | product、article、page、seo、core、navigation | 同事务引用登记、软删与新引用竞争 |

### 13.3 并行开工与提前开工

满足 13.1 后，**所有模块可以同时开工**：每个模块需要的其他模块的能力都已有替身，需要的契约都已冻结。以 M1 的几个关键模块为例：

| 模块 | 开工时使用的替身（platform-testkit） | 可以独立完成的部分 |
|---|---|---|
| content | url 服务接口的内存实现；扩展点示例实现（示例内容类型、示例快照贡献者） | 本地化、修订、发布状态机、快照与投影、预览、回收站 |
| product | content、media、taxonomy 服务接口的内存实现 | 产品与属性的草稿编辑、发布贡献（快照的 product 部分）、Schema.org 与面包屑扩展点 |
| delivery | content、url、media、taxonomy 服务接口的内存实现；示例模板与示例数据源 | 路由解析、渲染数据组装、引用解析、缓存标签计算 |
| inquiry | content、taxonomy、form、notification 服务接口的内存实现；直接构造 `FormSubmittedEvent` | 线索创建、去重、分配、通知请求 |
| translation | content 服务接口的内存实现；`FakeTranslationEngine`；翻译扩展点的示例实现 | 翻译单元、翻译记忆、校验、任务编排（系统设计 28“AI 翻译”一行） |

**关键路径与优先级**：

1. **content（内容内核）最先投入**：快照 + 投影模型是“内容内核设计错误导致大面积返工”风险的来源（系统设计 31），M1 先用 PRODUCT 验证（系统设计 30）。建议 content 与 product 由同一组负责或紧密配对。
2. **delivery 与前台渲染其次**：SSR 链路（Nuxt → Delivery → 快照）是 M1 演示的主线。
3. **form → inquiry → notification 获客链路**：M1 场景 S4 依赖它，链路短，可以较早端到端打通。

**提前开工**：属于后续里程碑、但只依赖契约与替身的模块，在人手允许时可以提前开发，集成与验收仍按里程碑进行（2.9）。适合提前开工的有：translation（翻译记忆、校验、引擎适配都可以用假引擎完成）、graph（评分算法是纯领域逻辑）、search（索引文档构建与降级逻辑）、tracking（同意管理与同意记录）、seo 的 sitemap 与健康检查规则。

### 13.4 团队分组

按系统设计 0.4 A6（团队 2–6 人，Java + Vue，使用 Claude Code）设计。每组负责所列模块的全部四个位置与模块文档：

| 分组 | 模块 | 技能侧重 | 说明 |
|---|---|---|---|
| F 框架组 | platform-api、platform-core、platform-testkit、infrastructure、application；Admin 外壳、前台渲染框架、共享前端包；CI 与本地环境 | 全栈 | M0 主力；之后负责契约变更落地、替身维护、集成支持 |
| K 内核组 | content、url、taxonomy、media | 后端 | 关键路径；M1 与 product 配合验证快照模型 |
| P 呈现组 | page、navigation、delivery；主题组件与模板；页面编辑器 | 前端为主 | M2 工作量最大（编辑器、≥ 20 个组件） |
| B 业务组 | product、article、graph、search | 后端 + 前端 | product 在 M1，其余在 M2–M4 |
| G 获客组 | form、notification、inquiry、tracking | 全栈 | M1 打通询盘链路，M3 完善分配、SLA、同意 |
| T SEO 与翻译组 | seo、translation | 后端 | seo 在 M1 有基础切片；translation 可以提前开工 |

按团队规模的分配建议：

| 规模 | 分配 |
|---|---|
| 6 人 | 架构负责人（F，兼契约评审与内核评审）；框架工程师（F，M0 后转 T）；后端 A（K）；后端 B（B）；全栈（G）；前端（P） |
| 4 人 | 架构负责人（F + K）；后端（B + T）；全栈（G）；前端（P，兼 F 的前端部分） |
| 2–3 人 | 按 13.5 的里程碑切片顺序推进，每人同时负责多个组；利用多个 AI 编码会话并行开发不同模块（每个会话只聚焦一个模块，系统设计 29.7），由人负责评审与集成 |

### 13.5 开发波次与里程碑切片

开发并行，集成按波次：每个波次对应一个里程碑，波次内各模块交付自己的切片，按 13.2 的集成顺序集成（先 L2，后 L3 / L4）。下表是各模块的里程碑切片（依据系统设计 30 章），各模块设计文档第 13 节据此细化：

| 模块 | 波次 1（M1） | 波次 2（M2） | 波次 3（M3） | 波次 4（M4） |
|---|---|---|---|---|
| core | 站点与语言、用户、角色、2FA、审计、设置（运行机制在 M0） | 站点资料；模块停用与影响报告（S10） | 站点资料与界面文案作为可翻译资源 | 数据主体请求、保留期任务；数据范围完善 |
| content | 本地化、修订、发布、预览；翻译模式与状态字段就绪 | 审核、定时发布、回滚、回收站 | AUTO 本地化的自动发布与跟随下线；`OUTDATED` 检测 | — |
| url | 路由、slug、自动 301；根路径 302；全部语言带前缀 | 页面层级变化时重算子树路径 | 重定向导入、前缀与正则重定向、404 日志 | — |
| media | 上传、图片变换、引用追踪 | 文件夹 | alt / 标题作为可翻译资源；私有文件（留资文档） | — |
| taxonomy | 词汇表、分类项、指派、分类落地页 | — | 分类名作为可翻译资源 | — |
| form | 表单定义、提交、Turnstile、反垃圾、附件 | — | 下载留资；表单文本作为可翻译资源 | — |
| notification | 发件箱、重试、询盘通知与自动回复 | 审核通知 | 翻译失败与过期通知；Webhook；客户邮件模板翻译 | — |
| translation | —（数据模型字段由 content 承担） | — | 全部：翻译记忆、术语表、风格指南、引擎、翻译编辑器、批量翻译、用量；询盘翻译 | — |
| product | 基础字段与属性、媒体、文档 | — | 停产与替代（S8）；属性与选项作为可翻译资源 | Excel 导入导出；型号表 |
| article | — | 文章、案例、FAQ、作者 | — | — |
| page | `product-detail` 模板（模板元数据与 `TemplateProvider`） | 页面、模板与插槽、页面编辑器、全局区块 | 法律页面（`legal` 模板） | — |
| inquiry | 询盘创建、归因、通知 | — | 分配规则、去重、SLA、报表、询价篮、询盘翻译 | 数据范围完善 |
| navigation | — | 菜单、面包屑 | 菜单标签作为可翻译资源 | — |
| delivery | `resolve`、站点引导数据、预览 | Cache Tag + CDN 清除 | 多语言（hreflang、未公开语言 404）；翻译完成后的失效 | — |
| seo | Meta 自动生成与覆盖、canonical、Product / Breadcrumb / Organization JSON-LD | — | Sitemap（含图片）、robots、IndexNow、链接索引、健康检查、hreflang | — |
| graph | — | — | 关系、评分、相关内容 | — |
| search | — | — | — | 站内搜索与分面筛选 |
| tracking | —（M1 的来源归因由前台插件采集） | — | 统计配置、同意管理、GPC、同意记录、转化事件 | — |

- 前端框架部分（Admin 外壳、渲染框架、共享包）属于 M0；模块的 Admin 页面与主题组件随模块切片交付。M2 的 Design Token 与“≥ 20 个组件”由 P 组与各组件所属模块的负责人共同完成。
- 某模块的切片为“—”只表示该里程碑没有集成验收要求，不表示不能开发（13.3）。

**集成看板**：每个模块 × 每个里程碑一格，状态依次为“文档已评审 → 开发中 → 契约测试通过 → 已在 application 中装配（替身退场）→ 场景通过”，在每周集成例会上更新（2.5）。

### 13.6 里程碑验收对应关系

| 里程碑 | 验收场景（系统设计 1.3） | 参与集成的模块 | 集成测试重点 |
|---|---|---|---|
| M1 | S1（不含英语部分）、S2、S4（不含 GA4） | core、content、url、media、taxonomy、product、page、seo、delivery、form、inquiry、notification | 发布事务的强一致（系统设计 4.4）；路由与自动 301；事件至少一次投递与幂等；询盘链路（表单 → 询盘 → 邮件）；SSR 输出完整 HTML 与 JSON-LD |
| M2 | S3、S10 | page、navigation、article、delivery、content、core | 编辑器 iframe 协议（系统设计 10.8）；模块停用行为（系统设计 3.8）；Cache Tag 失效在 60 秒内生效 |
| M3 | S1（英语部分）、S5、S7、S8、S9、S11、S12、S13；Lighthouse 预算 | translation、seo、graph、url、tracking、inquiry、form、media、notification、delivery、content、product | 翻译异步链路与翻译记忆复用；人工修订不被覆盖；hreflang 与多语言 sitemap；下线与替代；同意与 GPC |
| M4 | S6；安全扫描无高危；备份恢复演练 | product、search、core 及全部模块 | 批量导入的性能与逐行报告；搜索降级到 MySQL 投影；数据主体请求覆盖全部 `PersonalDataProvider` |

每个里程碑的验收由架构负责人主持、业主确认，结果记入验收报告；未通过的场景转为下一波次的阻塞项。

### 13.7 集成风险与对策

| 风险 | 表现 | 可能性 | 影响 | 对策 |
|---|---|---|---|---|
| 契约遗漏或设计不当 | 开发中频繁提出契约变更，或模块在内部绕行 | 中 | 高 | M0 契约草案用三条主链路走查（13.1）；模块文档在 M0 内完成评审，提前暴露缺口；契约变更请求 2 个工作日内答复（2.3） |
| 替身与真实实现行为不一致 | 模块测试全部通过，集成时失败 | 中 | 高 | 替身与真实实现通过同一套契约测试；异常、幂等、排序、分页、站点隔离等语义写入契约测试；不一致按框架缺陷优先修复 |
| 内容内核返工 | 快照或投影模型调整，波及全部内容类型模块 | 中 | 高 | M1 先用 PRODUCT 验证（系统设计 30、31）；content 与 product 配对开发；`PublishContributor` 等契约在 M1 中期复审 |
| 事件重复投递与乱序 | 集成后出现重复处理、旧数据覆盖新数据 | 中 | 中 | 所有监听器测试重复投递与乱序（2.4）；按 `revisionNo` 丢弃过期事件（ADR-003） |
| 跨模块调用的性能 | 逐条调用服务接口导致 Delivery 超出 P95 目标 | 中 | 中 | 契约提供批量查询方法（第 4 章）；M1 末用 k6 验证 Delivery 性能预算（系统设计 1.5、28） |
| 共享文件合并冲突 | 聚合 POM、`application` 依赖、CODEOWNERS 反复冲突 | 高 | 低 | M0 末一次性生成全部模块骨架（3.8）；之后各模块只改自己的目录 |
| 组件 Schema、主题与后端不同步 | 编辑器中可配置，前台不渲染或校验失败 | 中 | 中 | 三方使用同一份 JSON（系统设计 10.4）；Schema 校验、组件画廊视觉回归（第 8、10 章） |
| platform-api 评审成为瓶颈 | 契约变更排队，模块等待 | 中 | 中 | 兼容新增使用简写 ADR；框架组第二人起草；等待期间允许模块内部防腐接口（2.3） |
| 站点隔离遗漏 | 新增后台接口可以越权访问其他站点 | 中 | 高 | 站点隔离测试遍历全部后台接口（系统设计 28）；切片级 DoD 第 5 条（2.8） |
| AI 生成代码侵蚀边界 | 跨模块 import、硬编码文案、颜色字面量 | 高 | 中 | Enforcer、ArchUnit、Modulith、ESLint、Stylelint 自动拦截（第 10 章）；`CLAUDE.md`；每个会话只聚焦一个模块 |
| 问题集中在里程碑末暴露 | 验收前大量集成缺陷 | 中 | 高 | 主干持续集成、每日部署 dev、每周演示；鼓励尽早替身退场 |
| 前后端进度不一致 | 接口已合入而界面滞后，或相反 | 中 | 中 | 同一模块负责人负责四个位置；前端基于生成类型与 Mock 先行开发 |
| 人手不足 | 2–6 人负责 17 个模块，切片串行 | 高 | 中 | 按 13.4 分组；按关键路径排序（13.3）；多个 AI 会话并行，人负责评审与集成 |
| 外部服务难以在本地与 CI 测试 | 邮件、人机验证、AI 引擎、CDN 相关缺陷到 staging 才暴露 | 中 | 中 | 端口假实现与本地替代服务（Mailpit、MinIO 等，第 11 章）；外部服务在 staging 联调（系统设计 27.2） |

---

## 附录 A：module.json Schema

### A.1 字段说明

`module.json` 位于每个模块的 `src/main/resources/`（3.4；core 的位于 platform-core）。Schema 文件为 `backend/platform/platform-api/src/main/resources/schema/module.schema.json`（3.3），以下三处使用同一份：模块管理器的启动校验（系统设计 3.6 启动流程第 2 步）、CI 的 `module.json` 一致性检查（`tools/checks/`，第 10 章）、脚手架（3.8）。Schema 只做单文件的结构校验；跨文件、跨模块的规则见 A.3。

**顶层字段**（除 `$schema` 与 `x-*` 外全部必填；空值显式写 `{}`、`[]` 或 `null`）：

| 字段 | 类型 | 说明 | 约束 | 平台如何使用 |
|---|---|---|---|---|
| `id` | string | 模块 ID | `^[a-z][a-z0-9]{1,31}$`（3.7）；core 为 `core` | `sys_module` 的主键；扩展点、设置键、事件元数据、指标名中的模块 ID |
| `name` | string | 中文名称 | 1–32 个字符，至少含一个汉字；不得是按语言区分的对象（`CLAUDE.md` 语言规则 1） | 后台模块管理、停用影响报告、审计 |
| `version` | string | 模块自身的版本，与 Maven 版本相互独立（3.2） | `MAJOR.MINOR.PATCH`，不带预发布标识 | 与 `sys_module` 中的已安装版本比较，触发 `onInstall` / `onUpgrade`（系统设计 3.6） |
| `layer` | string | 所属层 | `L0`（只有 core）、`L2`、`L3`、`L4`；L1（platform-api）没有描述符 | 依赖方向校验（A.3） |
| `platform` | string | 兼容的平台版本（即 `platform-api` 版本）范围 | 版本范围（A.2 的 `versionRange`） | 启动时与当前 `platform-api` 版本比较（2.3） |
| `requires` | object | 必需依赖：模块 ID → 版本范围 | 不含 `core` 与自身；可以为 `{}` | 启动校验与拓扑排序；站点启用 / 停用校验（A.3） |
| `optional` | object | 可选依赖：存在时增强，缺失时降级（系统设计 3.5） | 同上；与 `requires` 不重叠 | 拓扑排序；服务接口经 `ObjectProvider` 注入（4.1.3） |
| `disableable` | boolean | 站点能否停用本模块 | 与系统设计 3.2 一致 | `sys_site_module`；停用流程（系统设计 3.8） |
| `tablePrefix` | string \| null | 表前缀 | `^[a-z]{2,4}_$`；没有表的delivery为 `null`；search按ADR-021提案为sch_ | 表前缀扫描（第 10 章）；`onPurge` 的范围 |
| `apiPrefix` | string \| null | REST 路径中的模块段、权限码第一段、Admin 路由前缀与目录名（3.6） | 非 core 等于 `id`，core 为 `system`；没有任何 HTTP 接口时为 `null` | 停用时该前缀下的接口不可用（系统设计 3.8）；菜单路由校验 |
| `permissions` | array | 权限声明 | 见下表 | 启动时幂等同步到 `sys_permission`（系统设计 3.6 第 6 步） |
| `menus` | array | 后台菜单 | 见下表 | 同步到 `sys_menu`；`/api/admin/v1/system/bootstrap` 按权限与启用状态过滤后返回（系统设计 22.2） |
| `settings` | string \| null | 站点设置的 JSON Schema 文件 | 与 `module.json` 同目录的 `*.schema.json`；没有设置时为 `null` | Schema 驱动的设置页；`SettingsApi` 的默认值与校验（4.4.3） |
| `components` | array | 本模块拥有的前台组件类型 | 小写连字符（3.7），不重复；没有组件时为 `[]` | 停用时编辑器面板隐藏、前台不渲染（系统设计 3.8） |
| `events` | object | 发布与订阅的领域事件 | `publishes`、`subscribes` 均必填；元素为 `com.b2bweb.api.event` 中的简单类名 | 文档生成与依赖分析；CI 与代码比对（系统设计 3.5） |
| `$schema` | string | 可选，供编辑器提示 | — | 忽略 |
| `x-*` | 任意 | 可选的扩展字段 | 只允许 `x-` 前缀 | 保留在 `ModuleDescriptor.raw`（4.5.22），平台不解释 |

**`permissions[]` 的元素**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `code` | string | 是 | 权限码 `{apiPrefix}:{资源}:{动作}`（3.7）；内容生命周期权限的例外见 A.3 第 11 条 |
| `name` | string | 是 | 中文名称，显示在角色编辑器中 |
| `description` | string | 否 | 中文说明，≤ 200 个字符 |
| `dataScopes` | array | 否 | 支持的数据范围：`ALL` / `GROUP` / `OWN` 的非空子集；省略表示不区分数据范围（`PermissionChecker.dataScope` 返回 `ALL`，4.4.7） |
| `sensitive` | boolean | 否（默认 `false`） | 高危权限（导出个人数据、管理用户与角色、处理数据主体请求等）：角色编辑器中突出显示，授予与撤销的审计记录标记为敏感；对应接口通常同时要求二次验证（`@RequiresPermission(reauthenticate = true)`，4.4.7） |
| `localeScoped` | boolean | 否（默认 `false`） | 受角色的语言范围限制（`PermissionChecker.localeScope`，4.4.7），如翻译审校（系统设计 5.8） |

**`menus[]` 的元素**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `key` | string | 是 | 全局唯一，小写连字符（3.7） |
| `name` | string | 是 | 中文菜单名 |
| `parent` | string | 否 | 父菜单 key；省略表示一级菜单（一级分组只能由 core 声明，A.3 第 14 条） |
| `route` | string | 否 | Admin 路由 `/{apiPrefix}/{页面}`（3.7）；省略表示分组，分组没有可见的子菜单时隐藏 |
| `permission` | string | 有 `route` 时必填 | 显示该菜单需要的权限码 |
| `order` | integer | 否（默认 100） | 同级排序，升序，0–9999 |
| `icon` | string | 否 | `@element-plus/icons-vue` 的图标组件名，只用于一级分组 |

**与系统设计 3.5 的差异**（框架细化，不改变 3.5 的含义）：

- 权限与菜单增加必填的中文 `name`（角色编辑器与菜单显示需要），权限增加 `description`、`localeScoped`，菜单增加 `icon`。系统设计 3.5 的示例是节选，省略了这些字段，按本 Schema 校验时以 A.4 的完整示例为准。
- 顶层字段全部必填，空值显式写出，评审时一眼可见模块“没有什么”。
- 只允许 `x-` 前缀的扩展字段，其他未定义字段视为错误，避免拼写错误被静默忽略。

### A.2 JSON Schema（draft 2020-12）

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:b2bweb:schema:module-descriptor:1",
  "title": "模块描述符（module.json）",
  "description": "平台框架设计附录 A；系统设计 3.5。跨文件与跨模块的规则见附录 A.3。",
  "type": "object",
  "required": [
    "id", "name", "version", "layer", "platform", "requires", "optional", "disableable",
    "tablePrefix", "apiPrefix", "permissions", "menus", "settings", "components", "events"
  ],
  "properties": {
    "$schema": { "type": "string" },
    "id": { "$ref": "#/$defs/moduleId" },
    "name": { "$ref": "#/$defs/chineseName" },
    "version": { "$ref": "#/$defs/semver" },
    "layer": { "enum": ["L0", "L2", "L3", "L4"] },
    "platform": { "$ref": "#/$defs/versionRange" },
    "requires": { "$ref": "#/$defs/dependencies" },
    "optional": { "$ref": "#/$defs/dependencies" },
    "disableable": { "type": "boolean" },
    "tablePrefix": {
      "type": ["string", "null"],
      "pattern": "^[a-z]{2,4}_$"
    },
    "apiPrefix": {
      "type": ["string", "null"],
      "pattern": "^[a-z][a-z0-9]{1,31}$"
    },
    "permissions": {
      "type": "array",
      "items": { "$ref": "#/$defs/permission" }
    },
    "menus": {
      "type": "array",
      "items": { "$ref": "#/$defs/menu" }
    },
    "settings": {
      "type": ["string", "null"],
      "pattern": "^[a-z0-9][a-z0-9.-]*\\.schema\\.json$"
    },
    "components": {
      "type": "array",
      "items": { "$ref": "#/$defs/kebabKey" },
      "uniqueItems": true
    },
    "events": {
      "type": "object",
      "required": ["publishes", "subscribes"],
      "properties": {
        "publishes": { "$ref": "#/$defs/eventList" },
        "subscribes": { "$ref": "#/$defs/eventList" }
      },
      "additionalProperties": false
    }
  },
  "patternProperties": {
    "^x-": true
  },
  "additionalProperties": false,
  "allOf": [
    {
      "if": { "properties": { "layer": { "const": "L0" } } },
      "then": {
        "properties": {
          "id": { "const": "core" },
          "apiPrefix": { "const": "system" },
          "tablePrefix": { "const": "sys_" },
          "disableable": { "const": false },
          "requires": { "maxProperties": 0 },
          "optional": { "maxProperties": 0 }
        }
      },
      "else": {
        "properties": {
          "id": { "not": { "const": "core" } },
          "apiPrefix": { "not": { "const": "system" } },
          "tablePrefix": { "not": { "const": "sys_" } }
        }
      }
    }
  ],
  "$defs": {
    "moduleId": {
      "type": "string",
      "pattern": "^[a-z][a-z0-9]{1,31}$"
    },
    "chineseName": {
      "description": "中文字符串，至少包含一个汉字；不得是按语言区分的对象",
      "type": "string",
      "minLength": 1,
      "maxLength": 32,
      "pattern": "[一-鿿]"
    },
    "semver": {
      "type": "string",
      "pattern": "^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$"
    },
    "versionRange": {
      "description": "^1.0、~1.2、1.2.3 或 >=1.0.0 <2.0.0；语义同 npm semver；不支持 ||、*、x",
      "type": "string",
      "pattern": "^(?:[~^]?[0-9]+(?:\\.[0-9]+){0,2}|(?:>=|>|=)[0-9]+\\.[0-9]+\\.[0-9]+(?: (?:<|<=)[0-9]+\\.[0-9]+\\.[0-9]+)?)$"
    },
    "dependencies": {
      "type": "object",
      "propertyNames": { "$ref": "#/$defs/moduleId" },
      "additionalProperties": { "$ref": "#/$defs/versionRange" },
      "not": { "required": ["core"] }
    },
    "kebabKey": {
      "type": "string",
      "maxLength": 64,
      "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
    },
    "permissionCode": {
      "type": "string",
      "maxLength": 128,
      "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*:[a-z][a-z0-9]*(?:-[a-z0-9]+)*:[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
    },
    "permission": {
      "type": "object",
      "required": ["code", "name"],
      "properties": {
        "code": { "$ref": "#/$defs/permissionCode" },
        "name": { "$ref": "#/$defs/chineseName" },
        "description": { "type": "string", "maxLength": 200 },
        "dataScopes": {
          "type": "array",
          "items": { "enum": ["ALL", "GROUP", "OWN"] },
          "minItems": 1,
          "uniqueItems": true
        },
        "sensitive": { "type": "boolean", "default": false },
        "localeScoped": { "type": "boolean", "default": false }
      },
      "additionalProperties": false
    },
    "menu": {
      "type": "object",
      "required": ["key", "name"],
      "properties": {
        "key": { "$ref": "#/$defs/kebabKey" },
        "name": { "$ref": "#/$defs/chineseName" },
        "parent": { "$ref": "#/$defs/kebabKey" },
        "route": {
          "type": "string",
          "pattern": "^/[a-z][a-z0-9]*(?:/[a-z0-9]+(?:-[a-z0-9]+)*)+$"
        },
        "permission": { "$ref": "#/$defs/permissionCode" },
        "order": { "type": "integer", "minimum": 0, "maximum": 9999, "default": 100 },
        "icon": { "type": "string", "pattern": "^[A-Z][A-Za-z0-9]*$" }
      },
      "dependentRequired": { "route": ["permission"] },
      "additionalProperties": false
    },
    "eventList": {
      "type": "array",
      "items": { "type": "string", "pattern": "^[A-Z][A-Za-z0-9]*Event$" },
      "uniqueItems": true
    }
  }
}
```

说明：

- `chineseName` 的 `pattern` 中 `一-鿿` 是 JSON 字符串转义，解析后为汉字区间，各语言的正则引擎均可使用；名称中可以包含 `SEO` 等通用缩写，但不能只有英文。
- `if` / `then` / `else` 保证只有 core 是 L0，且 core 的 `apiPrefix`、`tablePrefix`、`disableable` 固定；“`apiPrefix` 等于 `id`”这类字段间相等关系 JSON Schema 无法表达，放在 A.3。
- Java 端（模块管理器）与 Node 端（`tools/checks/`、脚手架）都使用支持 draft 2020-12 的校验库（3.2 的 BOM）；校验失败时输出中文信息，并给出文件路径与 JSON Pointer。

### A.3 Schema 之外的校验规则

检查方：“CI”指 `tools/checks/` 的 `module.json` 一致性检查与 testkit 中的 ArchUnit 规则（第 10 章）；“启动”指模块管理器的启动校验（系统设计 3.6 第 2 步），违反时拒绝启动；“运行时”指站点启用 / 停用模块的接口。

| # | 规则 | 内容 | 检查方 |
|---|---|---|---|
| 1 | 全局唯一 | `id`、`apiPrefix`、非空的 `tablePrefix`、权限码、菜单 key、组件类型在全部描述符中唯一 | CI、启动 |
| 2 | 身份一致 | `id` 等于 `backend/modules/<id>/` 目录名、根包 `com.b2bweb.<id>`、Flyway 位置 `db/migration/<id>/`（3.4、3.7）；core 的描述符位于 platform-core | CI |
| 3 | API 前缀 | 非 core 模块的 `apiPrefix` 等于 `id`，core 为 `system`（3.6、3.7）；`apiPrefix` 为 `null` 的模块不得有控制器 | CI |
| 4 | 与模块清单一致 | `layer`、`tablePrefix`、`disableable` 与系统设计 3.2 一致（清单以常量形式保存在 testkit 的架构规则中）；delivery 的 `tablePrefix` 为 `null`；search按ADR-021提案为 `sch_`；新增模块先写 ADR 并更新清单（系统设计 29.5） | CI |
| 5 | 依赖方向 | `requires` 只能指向 L2 模块：L2 模块的 `requires` 只能是 4.1.3 登记的 L2 → L2 方向，L3 / L4 可以 `requires` 任何 L2 模块（系统设计 3.3 R2）。`optional` 不得指向更高层的模块；指向同层模块时只表示通过扩展点或事件获得增强，不调用其服务（如 inquiry → product，系统设计 3.5） | CI、启动 |
| 6 | 依赖不自指、不重叠 | `requires`、`optional` 不含自身，两者不重叠（`core` 已由 Schema 排除，所有模块隐含依赖 core） | CI、启动 |
| 7 | 无环 | `requires` ∪ `optional` 构成有向无环图；模块管理器按拓扑序执行迁移与 `onInstall` / `onUpgrade`（系统设计 3.6） | CI、启动 |
| 8 | 版本满足 | `platform` 范围包含当前 `platform-api` 版本；`requires` 中的模块必须在构建中且版本满足范围；`optional` 中的模块缺失时降级，存在但版本不满足时拒绝启动 | 启动 |
| 9 | 版本单调 | `version` 不得低于 `sys_module` 中的已安装版本（不支持降级）；新增迁移脚本或 `onUpgrade` 数据修正时提升 `version`；不兼容的数据模型变更提升主版本并先写 ADR（`CLAUDE.md` 文档维护） | 启动；CI（与 main 比较） |
| 10 | 停用一致 | `disableable = false` 的模块不得 `requires` 可停用的模块；站点停用某模块时，若该站点有已启用的模块 `requires` 它，则拒绝并列出依赖方；启用模块时，其 `requires` 中的模块必须已在该站点启用 | CI；运行时 |
| 11 | 权限码归属 | 权限码第一段等于本模块的 `apiPrefix`。例外：内容生命周期权限 `content:{内容类型的小写连字符形式}:{edit\|review\|publish\|delete}` 由注册该内容类型（`ContentTypeProvider`，4.5.2）的模块声明，四个动作齐全（3.7；系统设计 6.3、26.2），如 product 声明 `content:product:*`（A.4） | CI |
| 12 | 权限码被使用 | 代码中的 `@RequiresPermission`、菜单的 `permission`、Admin 的 `registerContentTab` 与 `v-perm` 使用的权限码都已在某个描述符中声明（未声明则构建失败）；声明而未使用的权限码报警告 | CI |
| 13 | 权限属性 | `dataScopes` 只用于有归属人或归属组的资源，`GROUP` 的含义由本模块解释并写入模块文档 6.4（4.4.7）；`localeScoped` 只用于按语言操作的权限 | 评审（12.4 第 9 条） |
| 14 | 菜单 | `key` 等于模块 ID 或以 `<模块 ID>-` 开头（core 用 `system` 代替 `core`）；一级分组只由 core 声明（A.5），其他模块不得声明没有 `parent` 的菜单；`parent` 只能是 core 的一级分组或本模块的菜单；`route` 以 `/{apiPrefix}/` 开头，并存在于本模块 `defineAdminModule` 的路由中；层级不超过 3 级 | CI |
| 15 | 设置 Schema | `settings` 指向的文件存在，是合法的 draft 2020-12 Schema；顶层 `type: object`、`additionalProperties: false`；每个属性有中文 `title`，非 `x-secret` 属性有 `default`；敏感字段标注 `x-secret`（4.4.3）；顶层属性名为 camelCase，对应设置键 `{id}.{属性名}`（3.7） | CI、启动 |
| 16 | 组件 | `components` 与 `component-schemas/components/<id>/` 中 `module` 等于本模块 ID 的组件定义一一对应（3.6）；主题中缺少对应实现时报警告 | CI |
| 17 | 事件 | 事件类都存在于 `com.b2bweb.api.event`；`publishes` 与代码中实际发布的事件一致，`subscribes` 与 `@ApplicationModuleListener` 的参数类型一致（系统设计 3.5）；`publishes` 与 4.2.4 登记的发布方一致 | CI |

### A.4 完整示例

以系统设计 3.5 的 inquiry 为基础补全（权限、菜单与设置的最终内容以 `docs/modules/inquiry.md` 为准）：

```json
{
  "id": "inquiry",
  "name": "询盘",
  "version": "1.0.0",
  "layer": "L3",
  "platform": ">=1.0.0 <2.0.0",
  "requires": { "content": "^1.0", "taxonomy": "^1.0", "form": "^1.0", "notification": "^1.0" },
  "optional": { "product": "^1.0", "translation": "^1.0" },
  "disableable": true,
  "tablePrefix": "inq_",
  "apiPrefix": "inquiry",
  "permissions": [
    { "code": "inquiry:inquiry:view", "name": "查看询盘", "dataScopes": ["ALL", "GROUP", "OWN"] },
    { "code": "inquiry:inquiry:edit", "name": "跟进询盘", "description": "修改状态、添加跟进记录与备注", "dataScopes": ["ALL", "GROUP", "OWN"] },
    { "code": "inquiry:inquiry:assign", "name": "分配询盘" },
    { "code": "inquiry:inquiry:export", "name": "导出询盘", "sensitive": true },
    { "code": "inquiry:report:view", "name": "查看询盘报表" },
    { "code": "inquiry:rule:manage", "name": "管理分配规则" }
  ],
  "menus": [
    { "key": "inquiry", "name": "询盘", "parent": "sales", "route": "/inquiry/inquiries", "permission": "inquiry:inquiry:view", "order": 10 },
    { "key": "inquiry-reports", "name": "询盘报表", "parent": "sales", "route": "/inquiry/reports", "permission": "inquiry:report:view", "order": 20 },
    { "key": "inquiry-rules", "name": "询盘分配规则", "parent": "settings", "route": "/inquiry/rules", "permission": "inquiry:rule:manage", "order": 40 }
  ],
  "settings": "settings.schema.json",
  "components": ["inquiry-form", "add-to-inquiry-button", "inquiry-cart"],
  "events": {
    "publishes": ["InquiryCreatedEvent", "InquiryAssignedEvent", "InquiryStatusChangedEvent"],
    "subscribes": ["FormSubmittedEvent", "UserDeactivatedEvent"]
  }
}
```

| 要点 | 说明 |
|---|---|
| 依赖 | `requires` 全部为 L2；`optional` 中的 product 是同层模块，只通过 `ContentVariantProvider` 获取型号（系统设计 3.5）；translation 存在且已启用时，询盘翻译（S13）经 `TranslationApi` 调用，否则隐藏“翻译”按钮 |
| 数据范围 | `view` 与 `edit` 支持 ALL / GROUP / OWN（系统设计 26.2）；`GROUP` 为 inquiry 的销售组 |
| 高危权限 | 导出包含个人数据，标记 `sensitive`，接口同时要求二次验证 |
| 菜单 | 主入口 key 为模块 ID；挂在 core 的一级分组 `sales`、`settings` 下（A.5） |
| 事件 | 订阅 `UserDeactivatedEvent`，在用户停用后重新分配其负责的询盘（4.2.4；系统设计 17.7）；`InquiryCreatedEvent` 等在 V1 没有跨模块订阅方，仍须声明 |

内容类型模块的权限片段（product，说明 A.3 第 11 条的例外）：

```json
"permissions": [
  { "code": "content:product:edit", "name": "编辑产品" },
  { "code": "content:product:review", "name": "审核产品" },
  { "code": "content:product:publish", "name": "发布产品" },
  { "code": "content:product:delete", "name": "删除产品" },
  { "code": "product:attribute:manage", "name": "管理产品属性" },
  { "code": "product:product:import", "name": "导入产品" },
  { "code": "product:product:export", "name": "导出产品" }
]
```

前四项由 product 声明、由 content 的应用服务按内容类型校验（系统设计 6.3）；product 自己的资源使用 `product:` 前缀。

### A.5 core 的描述符片段

core 的描述符位于 `backend/platform/platform-core/src/main/resources/module.json`。除自身的权限与菜单外，core 还声明后台的**一级菜单分组**，供各模块作为 `parent` 引用。以下权限与菜单为节选，完整清单随 platform-core 的实现维护（第 5 章）：

```json
{
  "id": "core",
  "name": "平台核心",
  "version": "1.0.0",
  "layer": "L0",
  "platform": ">=1.0.0 <2.0.0",
  "requires": {},
  "optional": {},
  "disableable": false,
  "tablePrefix": "sys_",
  "apiPrefix": "system",
  "permissions": [
    { "code": "system:site:manage", "name": "管理站点与语言", "sensitive": true },
    { "code": "system:module:manage", "name": "启用与停用模块", "sensitive": true },
    { "code": "system:user:manage", "name": "管理用户", "sensitive": true },
    { "code": "system:role:manage", "name": "管理角色与权限", "sensitive": true },
    { "code": "system:setting:edit", "name": "修改站点设置" },
    { "code": "system:profile:edit", "name": "编辑站点资料" },
    { "code": "system:ui-text:edit", "name": "编辑界面文案" },
    { "code": "system:dict:manage", "name": "管理字典" },
    { "code": "system:job:view", "name": "查看任务" },
    { "code": "system:audit:view", "name": "查看审计日志" },
    { "code": "system:dsr:manage", "name": "处理数据主体请求", "sensitive": true }
  ],
  "menus": [
    { "key": "editorial", "name": "内容", "order": 10, "icon": "Document" },
    { "key": "marketing", "name": "营销", "order": 20, "icon": "Promotion" },
    { "key": "sales", "name": "销售", "order": 30, "icon": "ChatDotRound" },
    { "key": "localization", "name": "翻译", "order": 40, "icon": "Switch" },
    { "key": "settings", "name": "设置", "order": 80, "icon": "Setting" },
    { "key": "system", "name": "系统", "order": 90, "icon": "Tools" },
    { "key": "system-profile", "name": "站点资料", "parent": "settings", "route": "/system/profile", "permission": "system:profile:edit", "order": 10 },
    { "key": "system-settings", "name": "站点设置", "parent": "settings", "route": "/system/settings", "permission": "system:setting:edit", "order": 20 },
    { "key": "system-ui-texts", "name": "界面文案", "parent": "settings", "route": "/system/ui-texts", "permission": "system:ui-text:edit", "order": 30 },
    { "key": "system-sites", "name": "站点与语言", "parent": "system", "route": "/system/sites", "permission": "system:site:manage", "order": 10 },
    { "key": "system-modules", "name": "模块管理", "parent": "system", "route": "/system/modules", "permission": "system:module:manage", "order": 20 },
    { "key": "system-users", "name": "用户", "parent": "system", "route": "/system/users", "permission": "system:user:manage", "order": 30 },
    { "key": "system-roles", "name": "角色与权限", "parent": "system", "route": "/system/roles", "permission": "system:role:manage", "order": 40 },
    { "key": "system-dicts", "name": "字典", "parent": "system", "route": "/system/dicts", "permission": "system:dict:manage", "order": 50 },
    { "key": "system-jobs", "name": "任务", "parent": "system", "route": "/system/jobs", "permission": "system:job:view", "order": 60 },
    { "key": "system-audit-logs", "name": "审计日志", "parent": "system", "route": "/system/audit-logs", "permission": "system:audit:view", "order": 70 },
    { "key": "system-dsr", "name": "数据主体请求", "parent": "system", "route": "/system/dsr", "permission": "system:dsr:manage", "order": 80 }
  ],
  "settings": "settings.schema.json",
  "components": [],
  "events": {
    "publishes": [
      "SiteSettingsChangedEvent", "ModuleEnabledEvent", "ModuleDisabledEvent",
      "UserDeactivatedEvent", "TranslatableResourceChangedEvent"
    ],
    "subscribes": []
  }
}
```

**一级菜单分组**（保留 key，只由 core 声明；新增分组修改 core 的描述符，由框架组审核）：

| key | 名称 | 建议挂载的菜单 |
|---|---|---|
| `editorial` | 内容 | content（内容总览、审核、回收站）、product、article、page、media、taxonomy、navigation、graph |
| `marketing` | 营销 | form、seo、tracking |
| `sales` | 销售 | inquiry |
| `localization` | 翻译 | translation（翻译中心、翻译记忆、术语表、引擎与用量） |
| `settings` | 设置 | core 的站点资料、站点设置、界面文案；各模块的规则与配置页（如 `inquiry-rules`） |
| `system` | 系统 | core 的站点与语言、模块管理、用户、角色、字典、任务、审计日志、数据主体请求 |

core 描述符的特殊之处（均由 A.2 的 `if` / `then` 与 A.3 保证）：

| 项 | 取值 | 原因 |
|---|---|---|
| `layer` | `L0` | 唯一的 L0 模块；没有 `requires` / `optional`，所有模块隐含依赖它 |
| `apiPrefix` | `system` | 唯一与模块 ID 不同的前缀（3.6、3.7）；Admin 目录为 `modules/system/`（系统设计 22.1） |
| `disableable` | `false` | 核心模块不能停用（系统设计 3.6） |
| `components` | `[]` | core 没有前台组件；站点资料、语言列表与界面文案由 core 的 `SiteBootstrapContributor`（`site`）经 delivery 提供给前台（4.5.8） |
| `events.subscribes` | `[]` | L0 不订阅上层事件，需要上层能力时收集扩展点（4.1.3） |
| 菜单 | 一级分组 + `system-*` | 一级分组是 A.3 第 14 条的唯一例外 |
