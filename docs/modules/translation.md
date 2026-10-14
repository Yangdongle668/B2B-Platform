# AI 翻译（translation）模块设计

| 项 | 内容 |
|---|---|
| 模块 ID | `translation` |
| 名称 | AI 翻译 |
| 层 | L2 |
| 表前缀 | `trn_` |
| API 前缀 | `translation` |
| 可停用 | 是 |
| 模块版本 | 1.0.0（module.json 的 version） |
| 平台版本 | `>=1.0.0 <2.0.0`（module.json 的 platform） |
| 里程碑切片 | M1：<…>；M2：<…>；M3：<…>；M4：<…>（详见第 13 节） |
| 模块负责人 | 待定 |
| 文档版本 | V1.0 |
| 状态 | 草案 |
| 更新日期 | 2026-10-14 |
| 依据 | 系统设计 V1.4 第 <…> 章；平台框架设计 V1.<x> 第 4 章 <…> 节；ADR-<…> |

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
| 聚合根 | Engine（`trn_engine`） | 全局的 OpenAI 兼容引擎配置：base URL、加密的 API Key、模型、温度、单次请求上限、超时、速率上限、结构化输出方式、单价；可配置多个，站点选择主引擎与备用引擎 |
| 聚合根 | MemoryEntry（`trn_memory`） | 翻译记忆条目：一个（站点，源语言，目标语言，原文哈希，变体）的有效译文；来源 `MACHINE` / `REVIEWED` / `HUMAN`；内容与站点资源共用 |
| 聚合根 | GlossaryTerm（`trn_glossary_term`） | 站点术语：中文术语、类别、是否禁止翻译、变更序号 |
| 实体 | GlossaryTranslation（`trn_glossary_translation`） | 术语在某目标语言的规定译法（随术语整体保存） |
| 聚合根 | StyleGuide（`trn_style_guide`） | 站点 × 目标语言的风格指南：称呼、语气、数字格式、其他要求 |
| 聚合根 | Job（`trn_job`） | 翻译任务：实时 / 资源 / 批量 / 重译 / 询盘；可拆为按目标语言的子任务（只有一层）；进度、单元统计、token 与费用 |
| 实体 | JobItem（`trn_job_item`） | 任务项：一个内容本地化、一个站点资源、一条翻译记忆或一段按需文本 × 目标语言；单元级统计与未通过校验的单元（`issues`） |
| 聚合根 | UsageDaily（`trn_usage_daily`） | 按（站点，日期，引擎，模型，任务类型）汇总的请求数、token 与费用 |
| 值对象 | TranslationUnit | 翻译单元：`key`（字段路径）、`text`、`format`（`TEXT` / `RICH_INLINE`）、`maxLength`、`context`；由扩展点在内存中产生，不落表（系统设计 3.7） |
| 值对象 | SourceHash | SHA-256(规范化原文)；规范化 = Unicode NFC、去除首尾空白、合并连续空白（系统设计 5.6） |
| 值对象 | Variant | `default`，或长度受限时的 `max{N}`（如 `max60`） |
| 值对象 | UnitIssue | 未通过校验的单元：字段路径、原文哈希、变体、失败的校验项、候选译文、说明（`trn_job_item.issues` 的元素） |
| 值对象 | TokenUsage | 请求数、输入 / 缓存命中输入 / 输出 token、耗时、估算费用 |
| 值对象 | EncryptedSecret | 与 core 共用的加密信封 `v1:{keyId}:{Base64(nonce‖密文‖tag)}`（AES-256-GCM，见 平台框架设计 6.1） |
| 值对象 | TranslationSiteConfig | 站点级翻译设置，保存在 `sys_setting`（键 `translation.config`，见下） |

### 3.2 不变量

| # | 规则 | 保证方式 |
|---|---|---|
| 1 | 同一（站点，源语言，目标语言，原文哈希，变体）最多一条 `ACTIVE` 记忆；作废（`OBSOLETE`）的条目保留备查，不再命中 | 数据库：`uk_mem_key`（含生成列 `active_flag`） |
| 2 | **机器译文永远不覆盖 `HUMAN` / `REVIEWED` 条目**：机器写入使用 `INSERT … ON DUPLICATE KEY UPDATE`，更新子句只在现有行 `origin = 'MACHINE'` 时替换译文；重译只选择 `origin = 'MACHINE'` 的条目 | 应用层（SQL 条件更新）+ 用假引擎的模块集成测试（系统设计 28） |
| 3 | 来源只升不降：`MACHINE → REVIEWED → HUMAN`、`MACHINE → HUMAN`；`REVIEWED` / `HUMAN` 条目必须有 `reviewed_at`、`reviewed_by` | 应用层；数据库 `ck_trn_mem_review`（`reviewed_at` 非空） |
| 4 | 只有通过全部自动校验（系统设计 5.8）的译文才写入翻译记忆；未通过的单元只记入 `trn_job_item.issues` | 应用层 |
| 5 | 询盘留言及其译文不写入翻译记忆；`TEXT` 类任务项的 `issues`、`error_message` 不含原文与译文 | 应用层 + 模块集成测试 |
| 6 | 源语言与目标语言不同 | 数据库：`ck_trn_mem_locale`、`ck_trn_job_locale` |
| 7 | 父任务的 `target_locale` 为空、没有任务项；子任务与普通任务的 `target_locale` 非空；只有一层父子关系 | 数据库：`ck_trn_job_locale`（子任务非空）；其余应用层 |
| 8 | 同一对象 × 目标语言最多一个“当前”任务项：创建新任务项时，在同一事务中把该对象 × 语言的旧项中 `PENDING` / `RUNNING` / `NEEDS_REVIEW` / `FAILED` 一律置为 `SUPERSEDED`（以新修订为准，系统设计 5.7）。因此 `NEEDS_REVIEW` / `FAILED` 的任务项总是该对象当前的问题记录 | 应用层（`idx_item_content` / `idx_item_resource` 上 `SELECT … FOR UPDATE`） |
| 9 | 任务项的类别与引用字段一致：`CONTENT` 只填内容字段，`RESOURCE` / `TEXT` 只填资源字段，`MEMORY` 只填 `memory_id` | 数据库：`ck_trn_item_kind` |
| 10 | 事件重复投递不重复建任务 | 数据库：`uk_job_dedup`（`dedup_key`） |
| 11 | `status = 'PAUSED'` ⇔ `pause_reason` 非空 | 数据库：`ck_trn_job_pause` |
| 12 | 任务的计数（任务项、单元、token、费用）等于其任务项之和；任务项结束时在同一事务中累加 | 应用层（父任务的进度在读取时由子任务汇总） |
| 13 | 翻译只在事务提交后异步执行，不在发布事务中调用引擎；AI 引擎故障不影响中文内容发布 | 应用层（`@ApplicationModuleListener`，ADR-003） |
| 14 | 法律页面与 `legal` 模板的机器译文不自动发布：translation 在 `TranslationCompletedEvent` 中给出站点的自动发布设置，内容内核再与 `cnt_content.translation_review_required` 合并判断 | 应用层（content 与 translation） |
| 15 | API Key 只以加密信封保存，接口与日志只出现 `api_key_hint`；连接测试结果不含密钥与响应正文 | 应用层（系统设计 26.6） |
| 16 | 被任一站点设为主引擎或备用引擎的引擎不能删除；停用（`enabled = 0`）后站点自动改用备用引擎 | 应用层（经 `SettingsApi` 查询各站点的 `translation.config`） |
| 17 | 达到月度预算时暂停站点的 `BATCH` / `RETRANSLATE` 任务（`pause_reason = 'BUDGET'`）；实时任务按 `realtimeOverBudget` 决定是否继续 | 应用层（系统设计 5.10） |
| 18 | 术语的任何变更（含其译法、删除）都取新的 TSID 写入 `change_seq`；站点术语表版本 = `MAX(change_seq)` | 应用层（术语聚合保存时） |
| 19 | 禁止翻译的术语不含中日韩文字（否则译文必然“残留中文”），且没有译法行；中文品牌名应配置各语言的固定译法 | 应用层 |
| 20 | 提示词中的单元 ID 由系统生成（`u1…un`），响应中的 ID 必须与请求一一对应；原文、译文与询盘留言一律按纯文本处理，不拼入指令部分 | 应用层（系统设计 5.9、26.3） |

**补充与取舍**（系统设计之外的补充及理由）：

| 项 | 内容与理由 |
|---|---|
| 站点级翻译设置用 `sys_setting` | 不建 `trn_site_config` 表：主 / 备引擎、自动发布、月度预算等是每站点一份的设置，沿用 core 的 `settings.schema.json` + Schema 表单（系统设计 3.5、22.4）与 4.4.16 的约定（模块业务设置统一放 `sys_setting`）；引擎引用由本模块监听器校验（见 4.6） |
| 新增 `trn_glossary_translation` | 系统设计 5.8 的“中文术语 → 各语言译法”是一对多；拆为子表才能按（站点，目标语言）一次加载术语并保证每种语言一条译法。不命名为 `*_localization`：译法由人工录入、不经过翻译流程，因此不带 `translation_state` / `source_fingerprint` |
| 术语表版本 = `MAX(change_seq)` | 系统设计 5.6 要求记忆条目记录“术语表版本”。用 TSID 作变更序号，无需单独的版本表；`trn_memory.glossary_version < 术语.change_seq` 即“在该术语变化前生成的机器译文”，一键重译据此筛选 |
| `trn_memory.status` + `active_flag` | “作废”后条目保留（审计、对比），下次遇到相同原文重新翻译并新建 `ACTIVE` 行；唯一约束只作用于 `ACTIVE` 行，避免“覆盖 `HUMAN` 条目”的歧义 |
| `trn_memory.machine_text`、`context`、`reviewed_*` | 人工修订时保留原机器译文，用于对比与抽检统计（系统设计 5.8）；`context` 在翻译中心显示单元用途；`reviewed_*` 记录确认或修订人 |
| `trn_engine` 的补充列 | `protocol`（为将来的其他接口预留，V1 只有 OpenAI 兼容接口）、`api_key_hint`（后台掩码显示）、`max_output_tokens`、`cached_input_price`（服务商自动前缀缓存的折扣价，系统设计 5.9 的稳定前缀）、`price_currency`、`extra_options`（兼容服务的附加头与参数）、连接测试结果 |
| 任务类型细分 | `job_type` 在“实时 / 批量”之外区分 `RESOURCE`、`RETRANSLATE`、`INQUIRY`，便于翻译中心筛选、按类型统计费用（`trn_usage_daily.job_type`）以及预算暂停只作用于批量类任务 |
| 父子任务 | 一次操作涉及多个目标语言时（开放多种语言、术语变化后重译），父任务只做汇总，每个目标语言一个子任务，可分别暂停、重试 |
| 任务项承载单元级状态 | 不建“每个单元一行”的表：全量批量翻译一次就有约 1 万产品 × 30 个单元 × 9 种语言 ≈ 270 万单元。单元级状态以计数列（命中、去重、机器、失败）+ `issues`（只记未通过校验或出错的单元）保存；通过校验的单元，其来源（`MACHINE` / `REVIEWED` / `HUMAN`）可直接查翻译记忆。任务项失败后重试代价很低：已写入记忆的单元直接命中 |
| `trn_job_item.resolved_*` | 人工在翻译编辑器中处理完全部问题单元后，把该项从 `NEEDS_REVIEW` 改为 `SUCCEEDED` 并记录处理人，翻译中心的“待审校”列表据此清除 |
| 术语变化后的“重新应用” | 不建“记忆条目被哪些内容使用”的反向索引（数量 ≈ 单元数 × 语言数）。`RETRANSLATE` 任务先重译相关的 `MACHINE` 条目（`MEMORY` 项），再按 `params.reapply` 创建 `BATCH` 子任务对该语言的 `AUTO` 对象重新提取与回写：全部命中记忆、不调用 AI，草稿未变化的对象不会产生新的发布 |
| `dedup_key`、`priority`、认领字段 | 事件至少投递一次，需要幂等；实时任务优先于批量任务；多实例按任务项认领（`worker_id`、`heartbeat_at`），心跳超时的项重新排队 |

**站点级翻译设置**（`sys_setting`，键 `translation.config`，`module_id = 'translation'`；没有行时取 Schema 默认值）：

```json
{
  "schemaVersion": 1,
  "primaryEngineId": "7201893453912345",
  "fallbackEngineId": "7201893453912399",
  "autoPublish": true,
  "inquiryTranslation": true,
  "monthlyBudget": 300.00,
  "budgetCurrency": "USD",
  "budgetAlertPercent": 80,
  "realtimeOverBudget": true,
  "batchConcurrency": 2,
  "failedItemAlertThreshold": 20,
  "jobRetentionDays": 180
}
```

| 键 | 默认 | 说明 |
|---|---|---|
| `primaryEngineId` / `fallbackEngineId` | 空 | 主引擎 / 备用引擎（`trn_engine.id`）；未设置主引擎时不创建自动翻译任务。主引擎连续失败（429 / 5xx 退避重试 3 次后仍失败）时切换备用引擎（系统设计 5.9）；二者 `price_currency` 必须与 `budgetCurrency` 相同 |
| `autoPublish` | `true` | 目标语言译文 `TRANSLATED` 后自动发布；`false` 时进入审核（系统设计 5.7、6.7）。法律页面一律审核 |
| `inquiryTranslation` | `true` | 询盘留言“翻译为中文”是否可用（系统设计 17.10） |
| `monthlyBudget` / `budgetCurrency` | 空 / `USD` | 月度预算上限（按 UTC 自然月汇总 `trn_usage_daily.cost`）；空表示不限 |
| `budgetAlertPercent` | 80 | 达到该比例时告警（系统设计 27.4） |
| `realtimeOverBudget` | `true` | 超出预算后实时任务（`REALTIME` / `RESOURCE` / `INQUIRY`）是否继续；批量类任务一律暂停 |
| `batchConcurrency` | 2 | 本站点同时执行的批量类任务项数上限（引擎的 RPM / TPM 上限在全部站点之间共享） |
| `failedItemAlertThreshold` | 20 | `FAILED` 任务项积压告警阈值（系统设计 27.4） |
| `jobRetentionDays` | 180 | 任务与任务项的保留天数（见第 22 章） |

### 3.3 状态机

<<FILL: 状态与转换；没有状态机时写“不适用：<原因>”>>

## 4. 数据模型

> 遵守平台框架设计第 7 章的数据库通用约定。DDL 已在 MySQL 8.4 上执行通过；表结构以本节为准，与迁移脚本保持一致。

### 4.1 表清单

| 表 | 说明 | 站点级 | 软删除 | 预估规模（单站，按系统设计假设 A4） |
|---|---|---|---|---|
| `trn_engine` | 翻译引擎配置 | 否（全局） | 是 | ≤ 10（全局） |
| `trn_memory` | 翻译记忆 | 是 | 否（`OBSOLETE` 作废，不设过期） | ≈ 40 万个不同原文 × 9 种目标语言 ≈ 300–400 万行，约 2 GB |
| `trn_glossary_term` | 术语 | 是 | 是 | ≤ 2 千 |
| `trn_glossary_translation` | 术语的各语言译法 | 是 | 否（随术语） | ≤ 2 万 |
| `trn_style_guide` | 风格指南 | 是 | 否（删除即恢复默认） | ≤ 10 |
| `trn_job` | 翻译任务 | 是 | 否（保留 180 天） | 实时任务 ≈ 每次源语言发布 × 自动翻译语言数，约 500–2 千 / 天；180 天 ≈ 10–30 万 |
| `trn_job_item` | 任务项 | 是 | 否（保留 180 天，见 4.3.7） | 实时 ≈ 500–2 千 / 天；开放一种新语言 ≈ 2 万项；180 天 ≈ 30–50 万 |
| `trn_usage_daily` | 用量日汇总 | 是 | 否（保留 3 年） | ≈ 10 行 / 天 |

**站点过滤**（行级拦截器，系统设计 25.7）：

| 表 | 拦截器 | 访问方式 |
|---|---|---|
| `trn_engine` | 豁免（全局表） | 只有系统管理员可写（权限 `translation:engine:manage`）；站点管理员在翻译设置中只能读取已启用引擎的名称与模型 |
| `trn_job`、`trn_job_item` | 不豁免 | 例外：调度器跨站点轮询可执行的任务与心跳超时的任务项，使用标注 `@InterceptorIgnore` 的专用查询 |
| 其余站点级表 | 不豁免 | — |

### 4.2 ER 图

```mermaid
erDiagram
    trn_engine ||--o{ trn_memory : "generates"
    trn_engine ||--o{ trn_job_item : "executes"
    trn_engine ||--o{ trn_usage_daily : "meters"
    trn_glossary_term ||--o{ trn_glossary_translation : "translated as"
    trn_job |o--o{ trn_job : "splits into"
    trn_job ||--o{ trn_job_item : "contains"
    trn_memory |o--o{ trn_job_item : "retranslated by"
```

`trn_style_guide` 与其他表无外键关系，未画出；术语表版本与记忆条目之间是按 `change_seq` / `glossary_version` 比较的逻辑关系，也未画出。

### 4.3 表结构

类型列中的 `bin` 表示 `COLLATE utf8mb4_0900_bin`；通用字段的定义见第 2.4 节。表按外键依赖顺序排列。

#### 4.3.1 `trn_engine`：翻译引擎

全局表（系统设计 5.9）：可配置多个 OpenAI 兼容服务（OpenAI、DeepSeek、通义千问、Kimi，或自部署的 vLLM、Ollama），由 infrastructure 中的适配器按本行配置调用 `POST {base_url}/chat/completions`；切换服务商只改配置。速率上限由适配器在 Redis 中按引擎 ID 实现令牌桶（全部站点、全部实例共享）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `name` | VARCHAR(100) | 否 | — | 引擎名称（中文，后台显示，如“DeepSeek 主引擎”） |
| `protocol` | VARCHAR(16) | 否 | `'OPENAI_CHAT'` | 接口协议；V1 只有 `OPENAI_CHAT`（OpenAI 兼容 Chat Completions） |
| `base_url` | VARCHAR(500) | 否 | — | 服务地址（不含 `/chat/completions`），如 `https://api.example.com/v1` |
| `api_key_encrypted` | VARCHAR(1000) | 是 | — | API Key 密文（EncryptedSecret 信封，AES-256-GCM，系统设计 26.6）；自部署服务无需鉴权时为空 |
| `api_key_hint` | VARCHAR(16) | 是 | — | API Key 掩码（只含末 4 位，如 `****a1b2`），后台显示用 |
| `model` | VARCHAR(128) | 否 | — | 模型名（由服务商决定） |
| `temperature` | DECIMAL(3,2) | 否 | 0.20 | 温度（0–2） |
| `max_units_per_request` | INT | 否 | 50 | 单次请求最大翻译单元数 |
| `max_request_tokens` | INT | 否 | 6000 | 单次请求输入 token 上限（按估算值分批：提示词 + 待翻译单元） |
| `max_output_tokens` | INT | 是 | — | 单次请求输出 token 上限（请求参数 `max_tokens`；为空不传） |
| `timeout_ms` | INT | 否 | 60000 | 单次请求超时（毫秒） |
| `rpm_limit` | INT | 是 | — | 每分钟请求数上限（全部站点合计；为空不限） |
| `tpm_limit` | INT | 是 | — | 每分钟 token 数上限（全部站点合计；为空不限） |
| `response_format` | VARCHAR(16) | 否 | `'JSON_SCHEMA'` | 结构化输出方式：`JSON_SCHEMA` / `JSON_OBJECT` / `NONE` |
| `input_price` | DECIMAL(12,6) | 否 | 0 | 输入 token 单价（每百万 token） |
| `cached_input_price` | DECIMAL(12,6) | 是 | — | 命中服务商前缀缓存的输入 token 单价（每百万 token；为空按 `input_price` 计） |
| `output_price` | DECIMAL(12,6) | 否 | 0 | 输出 token 单价（每百万 token） |
| `price_currency` | CHAR(3) | 否 | `'USD'` | 单价币种（ISO 4217） |
| `extra_options` | JSON | 是 | — | 附加选项（不得含密钥）：额外请求头与请求体参数，如 `{"headers":{"X-Region":"eu"},"body":{"seed":7}}` |
| `enabled` | TINYINT(1) | 否 | 1 | 启用；停用后站点不能选择，已选择它作主引擎的站点改用备用引擎 |
| `last_checked_at` | DATETIME(3) | 是 | — | 最近一次连接测试时间 |
| `last_check_ok` | TINYINT(1) | 是 | — | 最近一次连接测试是否成功 |
| `last_check_message` | VARCHAR(500) | 是 | — | 最近一次连接测试结果说明（错误摘要，不含密钥与响应正文） |
| `note` | VARCHAR(500) | 是 | — | 备注（服务商、数据区域、DPA 签署情况等，系统设计 26.8） |
| `deleted_at` | DATETIME(3) | 是 | — | 删除时间（软删除） |
| `alive` | TINYINT（生成列） | 是 | — | 未删除标记（唯一索引用） |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_eng_name (name, alive)`：未删除的引擎名称唯一（引擎列表 ≤ 10 行，无其他查询索引）
- `ck_trn_eng_temperature`：温度在 0–2 之间
- `ck_trn_eng_limits`：单元数、token 上限、超时为正数

```sql
CREATE TABLE trn_engine (
  id                     BIGINT         NOT NULL COMMENT '主键（TSID）',
  name                   VARCHAR(100)   NOT NULL COMMENT '引擎名称（中文，后台显示，如“DeepSeek 主引擎”）',
  protocol               VARCHAR(16)    NOT NULL DEFAULT 'OPENAI_CHAT' COMMENT '接口协议：OPENAI_CHAT（OpenAI 兼容 Chat Completions，POST {base_url}/chat/completions）',
  base_url               VARCHAR(500)   NOT NULL COMMENT '服务地址（不含 /chat/completions），如 https://api.example.com/v1',
  api_key_encrypted      VARCHAR(1000)  NULL COMMENT 'API Key 密文：加密信封 v1:{keyId}:{Base64(nonce‖密文‖tag)}（AES-256-GCM）；自部署服务无需鉴权时为空',
  api_key_hint           VARCHAR(16)    NULL COMMENT 'API Key 掩码（只含末 4 位，如 ****a1b2），后台显示用',
  model                  VARCHAR(128)   NOT NULL COMMENT '模型名（由服务商决定）',
  temperature            DECIMAL(3,2)   NOT NULL DEFAULT 0.20 COMMENT '温度（0–2，默认 0.2）',
  max_units_per_request  INT            NOT NULL DEFAULT 50 COMMENT '单次请求最大翻译单元数',
  max_request_tokens     INT            NOT NULL DEFAULT 6000 COMMENT '单次请求输入 token 上限（估算值：提示词 + 待翻译单元）',
  max_output_tokens      INT            NULL COMMENT '单次请求输出 token 上限（请求参数 max_tokens；为空不传）',
  timeout_ms             INT            NOT NULL DEFAULT 60000 COMMENT '单次请求超时（毫秒）',
  rpm_limit              INT            NULL COMMENT '每分钟请求数上限（RPM，全部站点合计；为空不限）',
  tpm_limit              INT            NULL COMMENT '每分钟 token 数上限（TPM，全部站点合计；为空不限）',
  response_format        VARCHAR(16)    NOT NULL DEFAULT 'JSON_SCHEMA' COMMENT '结构化输出方式：JSON_SCHEMA（json_schema 严格模式）/ JSON_OBJECT / NONE（不支持 response_format，靠提示词约束 + 严格解析 + 校验重试）',
  input_price            DECIMAL(12,6)  NOT NULL DEFAULT 0 COMMENT '输入 token 单价（每百万 token，币种见 price_currency）',
  cached_input_price     DECIMAL(12,6)  NULL COMMENT '命中服务商前缀缓存的输入 token 单价（每百万 token；为空按 input_price 计）',
  output_price           DECIMAL(12,6)  NOT NULL DEFAULT 0 COMMENT '输出 token 单价（每百万 token）',
  price_currency         CHAR(3)        NOT NULL DEFAULT 'USD' COMMENT '单价币种（ISO 4217，如 USD、CNY）',
  extra_options          JSON           NULL COMMENT '附加选项（不得含密钥）：额外请求头与请求体参数，如 {"headers":{"X-Region":"eu"},"body":{"seed":7}}',
  enabled                TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '启用：0 时站点不能选择，已选择它作主引擎的站点改用备用引擎',
  last_checked_at        DATETIME(3)    NULL COMMENT '最近一次连接测试时间',
  last_check_ok          TINYINT(1)     NULL COMMENT '最近一次连接测试是否成功',
  last_check_message     VARCHAR(500)   NULL COMMENT '最近一次连接测试结果说明（错误摘要，不含密钥与响应正文）',
  note                   VARCHAR(500)   NULL COMMENT '备注（服务商、数据区域、DPA 签署情况等）',
  deleted_at             DATETIME(3)    NULL COMMENT '删除时间（软删除）',
  alive                  TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  version                INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by             BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by             BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_eng_name (name, alive),
  CONSTRAINT ck_trn_eng_temperature CHECK (temperature >= 0 AND temperature <= 2),
  CONSTRAINT ck_trn_eng_limits CHECK (max_units_per_request > 0 AND max_request_tokens > 0 AND timeout_ms > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '翻译引擎（OpenAI 兼容接口，全局）';
```

#### 4.3.2 `trn_memory`：翻译记忆

站点内内容与站点资源共用（系统设计 5.6）。查找与写入规则：

- **查找**：对一批单元按（`site_id`，`source_locale`，`target_locale`，`source_hash IN (…)`，`active_flag = 1`）一次查询。单元有长度上限 N 时，先取 `default`，长度不超过 N 则使用；否则取 `max{N}`；都没有则送引擎。
- **写入**：引擎译文通过校验后写入。不受长度限制、或未超长的译文写 `default`；超长后带长度约束重译得到的译文写 `max{N}`。写入使用 `INSERT … ON DUPLICATE KEY UPDATE`，只在现有行 `origin = 'MACHINE'` 时替换 `target_text`、`engine_id`、`model`、`glossary_version`（不变量 2）。
- **人工修订**（翻译编辑器）：就地更新为 `origin = 'HUMAN'`，首次由机器译文改为人工时把原译文存入 `machine_text`；“确认”只把 `MACHINE` 改为 `REVIEWED`。修订后对当前对象重新执行回写（全部命中记忆）。
- **作废**：`status` 改为 `OBSOLETE`，不再命中；之后遇到相同原文重新翻译并新建 `ACTIVE` 行。
- **命中计数**：每个任务项结束时对本项命中的条目批量执行 `usage_count = usage_count + 1, last_used_at = …`（同一条目在一项中多次命中只加 1）。
- **重译**（术语变化后）：在 `idx_mem_list` 的（站点，目标语言，`MACHINE`）范围内按 `source_text LIKE '%术语%'` 与 `glossary_version < 术语.change_seq` 筛选；每个术语通常只涉及数百条，异步批量执行，可接受范围扫描。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `source_locale` | VARCHAR(16) | 否 | `'zh-CN'` | 源语言（V1 固定 `zh-CN`） |
| `target_locale` | VARCHAR(16) | 否 | — | 目标语言（BCP 47） |
| `source_hash` | CHAR(64) bin | 否 | — | 原文哈希：SHA-256(规范化原文) |
| `variant` | VARCHAR(16) bin | 否 | `'default'` | 变体：`default`，或长度受限的 `max{N}` |
| `format` | VARCHAR(16) | 否 | `'TEXT'` | 单元格式：`TEXT` / `RICH_INLINE` |
| `source_text` | TEXT | 否 | — | 规范化原文（富文本行内标记已转为编号占位标签 `<1>…</1>`） |
| `target_text` | TEXT | 否 | — | 译文（占位标签与原文一致） |
| `origin` | VARCHAR(10) | 否 | — | 来源：`MACHINE` / `REVIEWED` / `HUMAN` |
| `status` | VARCHAR(10) | 否 | `'ACTIVE'` | 状态：`ACTIVE` / `OBSOLETE` |
| `active_flag` | TINYINT（生成列） | 是 | — | `status = 'ACTIVE'` 时为 1，否则 NULL（唯一索引用） |
| `context` | VARCHAR(200) | 是 | — | 首次生成时的上下文说明（如“产品名称”“按钮文字”） |
| `engine_id` | BIGINT | 是 | — | 生成机器译文的引擎（`trn_engine.id`）；人工新建的条目为空 |
| `model` | VARCHAR(128) | 是 | — | 生成机器译文的模型名 |
| `glossary_version` | BIGINT | 是 | — | 生成机器译文时的站点术语表版本（当时的 `MAX(trn_glossary_term.change_seq)`） |
| `machine_text` | TEXT | 是 | — | 人工修订前的机器译文（首次改为 `HUMAN` 时保留） |
| `reviewed_by` | BIGINT | 是 | — | 确认或修订人（`REVIEWED` / `HUMAN`） |
| `reviewed_at` | DATETIME(3) | 是 | — | 确认或修订时间 |
| `usage_count` | INT | 否 | 0 | 命中复用次数（不含首次生成） |
| `last_used_at` | DATETIME(3) | 是 | — | 最后一次命中时间 |
| `version` | INT | 否 | 0 | 乐观锁（人工修订与机器写入并发） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人（机器译文为 0） |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_mem_key (site_id, source_locale, target_locale, source_hash, variant, active_flag)`：有效条目唯一；同时服务翻译时的批量查找（前缀 + `source_hash IN (…)`）
- `idx_mem_list (site_id, target_locale, origin, updated_at)`：翻译中心按语言、来源浏览（按更新时间倒序）；术语变化后筛选 `MACHINE` 条目
- `idx_mem_engine (engine_id)`：按引擎统计、引擎删除前检查；外键索引
- `fk_trn_mem_engine`：→ `trn_engine.id`（RESTRICT；引擎只软删除）
- `ck_trn_mem_locale`：源语言与目标语言不同
- `ck_trn_mem_review`：`REVIEWED` / `HUMAN` 必须有确认时间

```sql
CREATE TABLE trn_memory (
  id                BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id           BIGINT        NOT NULL COMMENT '站点',
  source_locale     VARCHAR(16)   NOT NULL DEFAULT 'zh-CN' COMMENT '源语言（V1 固定 zh-CN）',
  target_locale     VARCHAR(16)   NOT NULL COMMENT '目标语言（BCP 47，如 en、de）',
  source_hash       CHAR(64)      COLLATE utf8mb4_0900_bin NOT NULL COMMENT '原文哈希：SHA-256(规范化原文)；规范化 = Unicode NFC、去首尾空白、合并连续空白',
  variant           VARCHAR(16)   COLLATE utf8mb4_0900_bin NOT NULL DEFAULT 'default' COMMENT '变体：default，或长度受限且默认译文超长时的 max{N}（如 max60）',
  format            VARCHAR(16)   NOT NULL DEFAULT 'TEXT' COMMENT '单元格式：TEXT / RICH_INLINE（含编号占位标签 <1>…</1>）',
  source_text       TEXT          NOT NULL COMMENT '规范化原文（富文本行内标记已转为编号占位标签）',
  target_text       TEXT          NOT NULL COMMENT '译文（占位标签与原文一致）',
  origin            VARCHAR(10)   NOT NULL COMMENT '来源：MACHINE（机器译文）/ REVIEWED（人工确认的机器译文）/ HUMAN（人工修订）；优先级 HUMAN > REVIEWED > MACHINE',
  status            VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE（可复用）/ OBSOLETE（已作废，不再命中，保留备查）',
  active_flag       TINYINT AS (IF(status = 'ACTIVE', 1, NULL)) STORED COMMENT '有效标记：ACTIVE 为 1，否则 NULL（唯一索引用）',
  context           VARCHAR(200)  NULL COMMENT '首次生成时的上下文说明（如“产品名称”“按钮文字”）',
  engine_id         BIGINT        NULL COMMENT '生成机器译文的引擎（trn_engine.id）；人工新建的条目为空',
  model             VARCHAR(128)  NULL COMMENT '生成机器译文的模型名',
  glossary_version  BIGINT        NULL COMMENT '生成机器译文时的站点术语表版本（当时的 MAX(trn_glossary_term.change_seq)）',
  machine_text      TEXT          NULL COMMENT '人工修订前的机器译文（首次由 MACHINE / REVIEWED 改为 HUMAN 时保留，用于对比与抽检统计）',
  reviewed_by       BIGINT        NULL COMMENT '确认或修订人（origin 为 REVIEWED / HUMAN 时）',
  reviewed_at       DATETIME(3)   NULL COMMENT '确认或修订时间',
  usage_count       INT           NOT NULL DEFAULT 0 COMMENT '命中复用次数（不含首次生成）',
  last_used_at      DATETIME(3)   NULL COMMENT '最后一次命中时间',
  version           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（人工修订与机器写入并发）',
  created_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by        BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统，机器译文）',
  updated_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by        BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mem_key (site_id, source_locale, target_locale, source_hash, variant, active_flag),
  KEY idx_mem_list (site_id, target_locale, origin, updated_at),
  KEY idx_mem_engine (engine_id),
  CONSTRAINT fk_trn_mem_engine FOREIGN KEY (engine_id) REFERENCES trn_engine (id),
  CONSTRAINT ck_trn_mem_locale CHECK (source_locale <> target_locale),
  CONSTRAINT ck_trn_mem_review CHECK (origin = 'MACHINE' OR reviewed_at IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '翻译记忆（同站点内容与站点资源共用；同一原文只翻译一次）';
```

#### 4.3.3 `trn_glossary_term`：术语

站点级术语表（系统设计 5.8）：翻译时按（站点，目标语言）把术语及译法加载到缓存（以术语表版本为缓存键），对每批原文做多模式匹配（如 Aho-Corasick），只把出现在本批原文中的条目放入提示词；校验“禁止翻译的术语保留原样”“术语表译法被遵守”。法规规定的固定文字（如 “Do Not Sell or Share My Personal Information”）也通过术语表固定译法（系统设计 20.2）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `term` | VARCHAR(200) | 否 | — | 术语原文（中文；按 NFC 规范化后在原文中匹配） |
| `category` | VARCHAR(16) | 否 | `'GENERAL'` | 类别：`BRAND` / `SERIES` / `MODEL` / `STANDARD` / `LEGAL` / `GENERAL` |
| `do_not_translate` | TINYINT(1) | 否 | 0 | 禁止翻译：1 = 各语言保留原样，不需要译法行 |
| `note` | VARCHAR(500) | 是 | — | 说明（含义、用法；随术语放入提示词） |
| `change_seq` | BIGINT | 否 | — | 变更序号：新建、修改本条或其译法、删除时取新的 TSID |
| `deleted_at` | DATETIME(3) | 是 | — | 删除时间（软删除；删除也更新 `change_seq`） |
| `alive` | TINYINT（生成列） | 是 | — | 未删除标记（唯一索引用） |
| `version` | INT | 否 | 0 | 乐观锁（术语与其译法整体保存） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_gls_term (site_id, term, alive)`：站点内未删除的术语唯一（默认排序规则，大小写不同视为重复）；后台按术语查找
- `idx_gls_change (site_id, change_seq)`：读取站点术语表版本（`MAX(change_seq)`，索引末端一次读取）；列出某版本之后变化的术语（触发重译）

```sql
CREATE TABLE trn_glossary_term (
  id                BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id           BIGINT        NOT NULL COMMENT '站点',
  term              VARCHAR(200)  NOT NULL COMMENT '术语原文（中文；按 NFC 规范化后在原文中匹配）',
  category          VARCHAR(16)   NOT NULL DEFAULT 'GENERAL' COMMENT '类别：BRAND（品牌）/ SERIES（产品系列）/ MODEL（型号）/ STANDARD（标准号）/ LEGAL（法规固定文字）/ GENERAL（一般术语）',
  do_not_translate  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '禁止翻译：1 = 各语言保留原样（不得含中日韩文字），不需要译法行',
  note              VARCHAR(500)  NULL COMMENT '说明（含义、用法；随术语放入提示词）',
  change_seq        BIGINT        NOT NULL COMMENT '变更序号：新建、修改本条或其译法、删除时取新的 TSID；站点术语表版本 = MAX(change_seq)（含已删除行）',
  deleted_at        DATETIME(3)   NULL COMMENT '删除时间（软删除）',
  alive             TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  version           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（术语与其译法整体保存）',
  created_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by        BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by        BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gls_term (site_id, term, alive),
  KEY idx_gls_change (site_id, change_seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '术语（站点级；可标记禁止翻译）';
```

#### 4.3.4 `trn_glossary_translation`：术语译法

术语在各目标语言的规定译法，人工录入，随术语整体保存（同一事务内更新术语的 `change_seq` 与 `version`）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `term_id` | BIGINT | 否 | — | 术语（`trn_glossary_term.id`） |
| `target_locale` | VARCHAR(16) | 否 | — | 目标语言 |
| `translation` | VARCHAR(300) | 否 | — | 规定译法（翻译时必须使用） |
| `note` | VARCHAR(500) | 是 | — | 该语言的用法说明（如德语名词的性与复数） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_glt_locale (term_id, target_locale)`：每个术语每种语言一条译法；兼作外键索引
- `idx_glt_load (site_id, target_locale)`：翻译时加载（站点，目标语言）的全部译法
- `fk_trn_glt_term`：→ `trn_glossary_term.id`，`ON DELETE CASCADE`（术语只软删除；物理删除只发生在站点或模块数据清除时）

```sql
CREATE TABLE trn_glossary_translation (
  id             BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id        BIGINT        NOT NULL COMMENT '站点',
  term_id        BIGINT        NOT NULL COMMENT '术语（trn_glossary_term.id）',
  target_locale  VARCHAR(16)   NOT NULL COMMENT '目标语言（BCP 47）',
  translation    VARCHAR(300)  NOT NULL COMMENT '规定译法（翻译时必须使用；自动校验“术语表译法被遵守”）',
  note           VARCHAR(500)  NULL COMMENT '该语言的用法说明（如德语名词的性与复数）',
  created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by     BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by     BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_glt_locale (term_id, target_locale),
  KEY idx_glt_load (site_id, target_locale),
  CONSTRAINT fk_trn_glt_term FOREIGN KEY (term_id) REFERENCES trn_glossary_term (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '术语的各语言规定译法';
```

#### 4.3.5 `trn_style_guide`：风格指南

站点 × 目标语言一条（系统设计 5.8），作为系统提示词的一部分（固定前缀，便于服务商的前缀缓存）；没有行时使用该语言的内置默认。数字分隔符同时用于“数字与原文一致”的校验（按风格指南调整小数点与千位分隔符）。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `target_locale` | VARCHAR(16) | 否 | — | 目标语言 |
| `formality` | VARCHAR(16) | 否 | `'FORMAL'` | 称呼：`FORMAL` / `INFORMAL` / `NEUTRAL` |
| `tone` | VARCHAR(200) | 是 | — | 语气（如“专业、简洁，面向工程采购人员”） |
| `decimal_separator` | VARCHAR(4) | 是 | — | 小数点（如 `,`）；为空按该语言的 CLDR 默认 |
| `group_separator` | VARCHAR(4) | 是 | — | 千位分隔符（如 `.`、窄不换行空格 U+202F）；为空按 CLDR 默认 |
| `instructions` | TEXT | 是 | — | 其他要求（数字与单位格式、品牌写法、禁用表达等）；应用层限制 ≤ 4000 个字符 |
| `version` | INT | 否 | 0 | 乐观锁 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 创建人 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_style_locale (site_id, target_locale)`：每站点每种目标语言一条；翻译时按此读取（结果缓存）

```sql
CREATE TABLE trn_style_guide (
  id                 BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id            BIGINT        NOT NULL COMMENT '站点',
  target_locale      VARCHAR(16)   NOT NULL COMMENT '目标语言（BCP 47）',
  formality          VARCHAR(16)   NOT NULL DEFAULT 'FORMAL' COMMENT '称呼：FORMAL（敬称，如德语 Sie、法语 vous）/ INFORMAL（如德语 du）/ NEUTRAL（不区分或不限定）',
  tone               VARCHAR(200)  NULL COMMENT '语气（如“专业、简洁，面向工程采购人员”）',
  decimal_separator  VARCHAR(4)    NULL COMMENT '小数点（如 , 或 .）；为空按该语言的 CLDR 默认',
  group_separator    VARCHAR(4)    NULL COMMENT '千位分隔符（如 . 、, 或窄不换行空格 U+202F）；为空按 CLDR 默认',
  instructions       TEXT          NULL COMMENT '其他要求（数字与单位格式、品牌写法、禁用表达等），作为系统提示词的一部分；应用层限制 ≤ 4000 个字符',
  version            INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统）',
  updated_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_style_locale (site_id, target_locale)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风格指南（站点 × 目标语言）';
```

#### 4.3.6 `trn_job`：翻译任务

| 任务类型 | 触发 | 任务项 | 说明 |
|---|---|---|---|
| `REALTIME` | 源语言内容发布（`ContentPublishedEvent`，`locale = zh-CN`） | 1 个 `CONTENT` | 每个启用自动翻译的目标语言一个任务（系统设计 5.7）；`dedup_key = pub:{localizationId}:{revisionNo}:{targetLocale}` |
| `RESOURCE` | `TranslatableResourceChangedEvent` | 1 个 `RESOURCE` | 每个目标语言一个任务；`dedup_key = res:{resourceType}:{resourceId}:{fingerprint}:{targetLocale}` |
| `BATCH` | 开放新语言（`SiteSettingsChangedEvent`）、批量导入后、手动批量、重译后的重新应用 | 多个 `CONTENT` / `RESOURCE` | 按内容逐条排队，限制并发与速率；可暂停、取消、重试失败项（系统设计 5.7） |
| `RETRANSLATE` | 术语表变化后一键重译、翻译编辑器的单段重译 | 多个 `MEMORY` | 只重译 `MACHINE` 条目；单段重译 `HUMAN` / `REVIEWED` 条目时，结果作为候选放入 `issues`，由人工采纳（采纳即 `HUMAN`），不覆盖原条目 |
| `INQUIRY` | `TranslationApi.translateText`（销售点击“翻译为中文”） | 1 个 `TEXT` | 同步执行；只记录用量与结果状态，不保存留言与译文，不写翻译记忆（系统设计 17.10） |

状态流转：`PENDING → RUNNING → SUCCEEDED / FAILED`；`PENDING` / `RUNNING ⇄ PAUSED`；`PENDING` / `RUNNING` / `PAUSED → CANCELED`；`FAILED → RUNNING`（重试失败项：把 `FAILED` 任务项重置为 `PENDING`）。全部任务项结束且没有 `FAILED` 项为 `SUCCEEDED`（可有 `NEEDS_REVIEW` 项），有 `FAILED` 项为 `FAILED`。父任务的状态与进度在读取时由子任务汇总。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `parent_job_id` | BIGINT | 是 | — | 父任务（`trn_job.id`）；只有一层 |
| `job_type` | VARCHAR(16) | 否 | — | 任务类型：`REALTIME` / `RESOURCE` / `BATCH` / `RETRANSLATE` / `INQUIRY` |
| `trigger_type` | VARCHAR(24) | 否 | — | 触发来源：`CONTENT_PUBLISHED` / `RESOURCE_CHANGED` / `LOCALE_ENABLED` / `IMPORT` / `GLOSSARY_CHANGED` / `MANUAL` / `API` |
| `title` | VARCHAR(200) | 否 | — | 任务名称（中文，翻译中心列表显示） |
| `source_locale` | VARCHAR(16) | 否 | `'zh-CN'` | 源语言（`INQUIRY` 为询盘语言） |
| `target_locale` | VARCHAR(16) | 是 | — | 目标语言；只有父任务为空（`INQUIRY` 为 `zh-CN`） |
| `status` | VARCHAR(16) | 否 | `'PENDING'` | 状态：`PENDING` / `RUNNING` / `PAUSED` / `SUCCEEDED` / `FAILED` / `CANCELED` |
| `pause_reason` | VARCHAR(16) | 是 | — | 暂停原因：`MANUAL` / `BUDGET` / `ENGINE`；仅 `PAUSED` 时有值 |
| `priority` | INT | 否 | 0 | 优先级（大的先执行）：实时类默认 100，批量类默认 0 |
| `dedup_key` | VARCHAR(200) bin | 是 | — | 去重键（事件重复投递时幂等） |
| `params` | JSON | 是 | — | 任务参数：批量范围（内容类型、资源类型、内容 ID 列表）、重译的术语 ID、`reapply`（重译后是否重新应用）等 |
| `item_total` | INT | 否 | 0 | 任务项总数 |
| `item_succeeded` | INT | 否 | 0 | 成功的任务项数 |
| `item_needs_review` | INT | 否 | 0 | 待审校的任务项数 |
| `item_failed` | INT | 否 | 0 | 失败的任务项数 |
| `item_skipped` | INT | 否 | 0 | 跳过、被新修订取代或取消的任务项数 |
| `unit_total` | INT | 否 | 0 | 提取的翻译单元总数 |
| `unit_memory_hit` | INT | 否 | 0 | 翻译记忆命中数 |
| `unit_machine` | INT | 否 | 0 | 调用引擎翻译并通过校验的单元数 |
| `unit_failed` | INT | 否 | 0 | 未通过校验的单元数 |
| `request_count` | INT | 否 | 0 | 引擎请求次数（含重试） |
| `input_tokens` | BIGINT | 否 | 0 | 输入 token 数 |
| `cached_input_tokens` | BIGINT | 否 | 0 | 其中命中服务商前缀缓存的输入 token 数 |
| `output_tokens` | BIGINT | 否 | 0 | 输出 token 数 |
| `cost` | DECIMAL(14,6) | 否 | 0 | 估算费用 |
| `currency` | CHAR(3) | 否 | `'USD'` | 费用币种（站点预算币种） |
| `error_code` | VARCHAR(64) | 是 | — | 任务级失败错误码（`TRANSLATION_{REASON}`） |
| `error_message` | VARCHAR(1000) | 是 | — | 任务级失败说明（中文） |
| `started_at` | DATETIME(3) | 是 | — | 开始执行时间 |
| `finished_at` | DATETIME(3) | 是 | — | 结束时间 |
| `version` | INT | 否 | 0 | 乐观锁（暂停、取消、重试与执行器并发） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `created_by` | BIGINT | 否 | 0 | 发起人（事件触发为 0） |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |
| `updated_by` | BIGINT | 否 | 0 | 更新人 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_job_dedup (site_id, dedup_key)`：事件幂等（NULL 不参与比较）
- `idx_job_poll (status, priority, created_at)`：调度器跨站点选取 `PENDING` / `RUNNING` 任务（按优先级、先后）（专用豁免查询）
- `idx_job_type (site_id, job_type, created_at)`：翻译中心按类型列出任务；预算暂停时查找站点的批量类任务
- `idx_job_list (site_id, created_at)`：翻译中心全部任务按时间倒序；保留期清理
- `idx_job_parent (parent_job_id)`：汇总子任务；外键索引
- `fk_trn_job_parent`：→ `trn_job.id`，`ON DELETE CASCADE`（子任务随父任务清理）
- `ck_trn_job_locale`：子任务必须有目标语言；目标语言与源语言不同
- `ck_trn_job_pause`：`PAUSED` ⇔ 有暂停原因

```sql
CREATE TABLE trn_job (
  id                   BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id              BIGINT         NOT NULL COMMENT '站点',
  parent_job_id        BIGINT         NULL COMMENT '父任务（trn_job.id）：涉及多个目标语言时按语言拆分子任务；只有一层',
  job_type             VARCHAR(16)    NOT NULL COMMENT '任务类型：REALTIME（源语言内容发布）/ RESOURCE（站点资源变化）/ BATCH（批量）/ RETRANSLATE（重译记忆条目）/ INQUIRY（询盘留言翻译为中文）',
  trigger_type         VARCHAR(24)    NOT NULL COMMENT '触发来源：CONTENT_PUBLISHED / RESOURCE_CHANGED / LOCALE_ENABLED / IMPORT / GLOSSARY_CHANGED / MANUAL / API',
  title                VARCHAR(200)   NOT NULL COMMENT '任务名称（中文，翻译中心列表显示，如“开放英语：翻译全部已发布内容”）',
  source_locale        VARCHAR(16)    NOT NULL DEFAULT 'zh-CN' COMMENT '源语言（INQUIRY 为询盘语言）',
  target_locale        VARCHAR(16)    NULL COMMENT '目标语言；只有父任务为空（INQUIRY 为 zh-CN）',
  status               VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / RUNNING / PAUSED / SUCCEEDED / FAILED / CANCELED',
  pause_reason         VARCHAR(16)    NULL COMMENT '暂停原因：MANUAL（人工）/ BUDGET（达到月度预算）/ ENGINE（主备引擎均不可用）；仅 PAUSED 时有值',
  priority             INT            NOT NULL DEFAULT 0 COMMENT '优先级（大的先执行）：REALTIME、RESOURCE、INQUIRY 默认 100，BATCH、RETRANSLATE 默认 0',
  dedup_key            VARCHAR(200)   COLLATE utf8mb4_0900_bin NULL COMMENT '去重键（事件重复投递时幂等），如 pub:{localizationId}:{revisionNo}:{targetLocale}',
  params               JSON           NULL COMMENT '任务参数：批量范围（内容类型、资源类型、内容 ID 列表）、重译的术语 ID、reapply（重译后是否重新应用）等',
  item_total           INT            NOT NULL DEFAULT 0 COMMENT '任务项总数',
  item_succeeded       INT            NOT NULL DEFAULT 0 COMMENT '成功的任务项数',
  item_needs_review    INT            NOT NULL DEFAULT 0 COMMENT '待审校的任务项数',
  item_failed          INT            NOT NULL DEFAULT 0 COMMENT '失败的任务项数',
  item_skipped         INT            NOT NULL DEFAULT 0 COMMENT '跳过、被新修订取代或取消的任务项数',
  unit_total           INT            NOT NULL DEFAULT 0 COMMENT '提取的翻译单元总数',
  unit_memory_hit      INT            NOT NULL DEFAULT 0 COMMENT '翻译记忆命中数（未调用 AI）',
  unit_machine         INT            NOT NULL DEFAULT 0 COMMENT '调用引擎翻译并通过校验的单元数',
  unit_failed          INT            NOT NULL DEFAULT 0 COMMENT '重试后仍未通过校验的单元数',
  request_count        INT            NOT NULL DEFAULT 0 COMMENT '引擎请求次数（含重试）',
  input_tokens         BIGINT         NOT NULL DEFAULT 0 COMMENT '输入 token 数',
  cached_input_tokens  BIGINT         NOT NULL DEFAULT 0 COMMENT '其中命中服务商前缀缓存的输入 token 数',
  output_tokens        BIGINT         NOT NULL DEFAULT 0 COMMENT '输出 token 数',
  cost                 DECIMAL(14,6)  NOT NULL DEFAULT 0 COMMENT '估算费用（按调用时引擎单价）',
  currency             CHAR(3)        NOT NULL DEFAULT 'USD' COMMENT '费用币种（站点预算币种，ISO 4217）',
  error_code           VARCHAR(64)    NULL COMMENT '任务级失败错误码（TRANSLATION_{REASON}）',
  error_message        VARCHAR(1000)  NULL COMMENT '任务级失败说明（中文）',
  started_at           DATETIME(3)    NULL COMMENT '开始执行时间',
  finished_at          DATETIME(3)    NULL COMMENT '结束时间（SUCCEEDED / FAILED / CANCELED）',
  version              INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（暂停、取消、重试与执行器并发）',
  created_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by           BIGINT         NOT NULL DEFAULT 0 COMMENT '发起人（0 = 系统，事件触发）',
  updated_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by           BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_dedup (site_id, dedup_key),
  KEY idx_job_poll (status, priority, created_at),
  KEY idx_job_type (site_id, job_type, created_at),
  KEY idx_job_list (site_id, created_at),
  KEY idx_job_parent (parent_job_id),
  CONSTRAINT fk_trn_job_parent FOREIGN KEY (parent_job_id) REFERENCES trn_job (id) ON DELETE CASCADE,
  CONSTRAINT ck_trn_job_locale CHECK ((parent_job_id IS NULL OR target_locale IS NOT NULL) AND (target_locale IS NULL OR target_locale <> source_locale)),
  CONSTRAINT ck_trn_job_pause CHECK ((status = 'PAUSED') = (pause_reason IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '翻译任务（实时、资源、批量、重译、询盘；可拆为按目标语言的子任务）';
```

#### 4.3.7 `trn_job_item`：任务项

一个任务项 = 一个翻译对象 × 一个目标语言，是认领、执行、重试的单位。执行步骤（系统设计 5.7）：提取单元 → 查翻译记忆 → 去重后按引擎上限分批调用 → 校验（失败自动重试一次）→ 写入记忆 → 回写（`apply()`）→ 经 `ContentApi` 或资源提供方更新翻译状态 → 发布 `TranslationCompletedEvent` / `TranslationFailedEvent`。

| 类别 | 引用字段 | 结束状态 |
|---|---|---|
| `CONTENT` | `content_id`、`content_type`、`source_localization_id`、`source_revision_no`、`target_localization_id` | 全部单元通过 → `SUCCEEDED`（本地化 `TRANSLATED`）；有单元未通过 → `NEEDS_REVIEW`（已通过的单元照常回写，本地化 `NEEDS_REVIEW`，不自动发布）；引擎错误且重试耗尽 → `FAILED`（本地化 `FAILED`） |
| `RESOURCE` | `resource_type`、`resource_id`、`source_fingerprint` | 同上（状态写入资源本地化行的 `translation_state`） |
| `MEMORY` | `memory_id` | 重译通过 → `SUCCEEDED`；条目已不是 `MACHINE` → `SKIPPED` |
| `TEXT` | `resource_type`（调用方用途，如 `inquiry.message`）、`resource_id`（调用方对象 ID，只用于追踪） | 同步返回译文给调用方；只记录状态与用量 |

执行时目标对象已被删除、目标本地化已改为 `MANUAL`、或源语言已下线时置为 `SKIPPED`。重试：任务项级失败（实例失联、引擎持续不可用）按 `attempts` 退避，最多 3 次；“重试失败项”把 `FAILED` 项重置为 `PENDING`。人工在翻译编辑器中处理完全部问题单元后，项改为 `SUCCEEDED`（记录 `resolved_*`），并在同一事务中调整所属任务的计数。

保留期（第 22 章）：先删除结束超过 `jobRetentionDays` 且状态不是 `NEEDS_REVIEW` / `FAILED` 的任务项，再删除结束超过保留期且已没有任务项的任务。`NEEDS_REVIEW` / `FAILED` 项总是对象当前的问题记录（不变量 8），保留到被新任务项取代或人工处理完毕。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `job_id` | BIGINT | 否 | — | 所属任务（`trn_job.id`） |
| `item_kind` | VARCHAR(10) | 否 | — | 类别：`CONTENT` / `RESOURCE` / `MEMORY` / `TEXT` |
| `target_locale` | VARCHAR(16) | 否 | — | 目标语言（冗余 `trn_job.target_locale`） |
| `content_id` | BIGINT | 是 | — | `CONTENT`：内容（`cnt_content.id`） |
| `content_type` | VARCHAR(32) | 是 | — | `CONTENT`：内容类型（选择 `TranslatableContentProvider`） |
| `source_localization_id` | BIGINT | 是 | — | `CONTENT`：源语言本地化（`cnt_localization.id`） |
| `source_revision_no` | INT | 是 | — | `CONTENT`：依据的源语言修订号 |
| `target_localization_id` | BIGINT | 是 | — | `CONTENT`：目标语言本地化（`cnt_localization.id`，创建任务时由内容内核创建或返回） |
| `resource_type` | VARCHAR(64) bin | 是 | — | `RESOURCE`：资源类型（`{moduleId}.{资源}`，如 `taxonomy.term`）；`TEXT`：调用方用途（如 `inquiry.message`） |
| `resource_id` | BIGINT | 是 | — | `RESOURCE`：资源 ID；`TEXT`：调用方对象 ID |
| `source_fingerprint` | CHAR(64) bin | 是 | — | `RESOURCE`：提取时的源语言指纹（回写目标语言行的 `source_fingerprint`） |
| `memory_id` | BIGINT | 是 | — | `MEMORY`：被重译的记忆条目（`trn_memory.id`） |
| `status` | VARCHAR(16) | 否 | `'PENDING'` | 状态：`PENDING` / `RUNNING` / `SUCCEEDED` / `NEEDS_REVIEW` / `FAILED` / `SKIPPED` / `SUPERSEDED` / `CANCELED` |
| `attempts` | INT | 否 | 0 | 已执行次数 |
| `next_attempt_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 最早执行时间（重试退避） |
| `worker_id` | VARCHAR(64) | 是 | — | 执行实例标识 |
| `heartbeat_at` | DATETIME(3) | 是 | — | 执行实例最近心跳；超时视为失联，重新排队 |
| `engine_id` | BIGINT | 是 | — | 最后使用的引擎（主引擎或切换后的备用引擎） |
| `unit_total` | INT | 否 | 0 | 提取的翻译单元数 |
| `unit_memory_hit` | INT | 否 | 0 | 翻译记忆命中数 |
| `unit_deduped` | INT | 否 | 0 | 本项内重复原文数（只翻译一次） |
| `unit_machine` | INT | 否 | 0 | 调用引擎翻译并通过校验的单元数 |
| `unit_failed` | INT | 否 | 0 | 重试后仍未通过校验的单元数 |
| `request_count` | INT | 否 | 0 | 引擎请求次数（含重试） |
| `input_tokens` | INT | 否 | 0 | 输入 token 数 |
| `cached_input_tokens` | INT | 否 | 0 | 其中命中前缀缓存的输入 token 数 |
| `output_tokens` | INT | 否 | 0 | 输出 token 数 |
| `cost` | DECIMAL(14,6) | 否 | 0 | 估算费用 |
| `engine_ms` | INT | 否 | 0 | 引擎调用累计耗时（毫秒） |
| `issues` | JSON | 是 | — | 未通过校验或出错的单元（见下）；`TEXT` 项不含原文与译文 |
| `error_code` | VARCHAR(64) | 是 | — | 失败错误码（如 `TRANSLATION_ENGINE_UNAVAILABLE`） |
| `error_message` | VARCHAR(1000) | 是 | — | 失败说明（中文） |
| `resolved_by` | BIGINT | 是 | — | 人工处理完全部问题单元的用户 |
| `resolved_at` | DATETIME(3) | 是 | — | 人工处理完毕时间（`NEEDS_REVIEW` → `SUCCEEDED`） |
| `started_at` | DATETIME(3) | 是 | — | 开始执行时间 |
| `finished_at` | DATETIME(3) | 是 | — | 结束时间 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |

`issues` 示例（翻译编辑器据此标出问题段落，翻译中心“待审校”据此列出单元）：

```json
[
  { "key": "body[3]", "sourceHash": "9f2c…", "variant": "default",
    "checks": ["PLACEHOLDER", "NUMBER"], "candidate": "This product is <1>CE</1> certified…",
    "message": "占位标签 <2> 缺失；数字 3.7 与原文不一致" }
]
```

索引与约束：
- `PRIMARY KEY (id)`
- `idx_item_claim (job_id, status, next_attempt_at)`：执行器在选中的任务内认领到期的 `PENDING` 项（`FOR UPDATE SKIP LOCKED`）、查找心跳超时的 `RUNNING` 项；兼作外键索引
- `idx_item_content (site_id, content_id, target_locale, created_at)`：同一内容 × 目标语言的进行中项（作废旧项）；翻译编辑器读取最近一项的 `issues`；内容彻底删除时清理
- `idx_item_resource (site_id, resource_type, resource_id, target_locale, created_at)`：站点资源的同类查询
- `idx_item_review (site_id, status, target_locale)`：翻译中心“待审校”（`NEEDS_REVIEW` / `FAILED`）按语言列出；失败积压告警计数
- `idx_item_memory (memory_id)`：外键索引
- `idx_item_engine (engine_id)`：外键索引
- `fk_trn_item_job`：→ `trn_job.id`，`ON DELETE CASCADE`
- `fk_trn_item_memory`：→ `trn_memory.id`（RESTRICT；记忆条目不物理删除）
- `fk_trn_item_engine`：→ `trn_engine.id`（RESTRICT）
- `ck_trn_item_kind`：类别与引用字段一致（不变量 9）

```sql
CREATE TABLE trn_job_item (
  id                      BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id                 BIGINT         NOT NULL COMMENT '站点',
  job_id                  BIGINT         NOT NULL COMMENT '所属任务（trn_job.id）',
  item_kind               VARCHAR(10)    NOT NULL COMMENT '类别：CONTENT（内容本地化）/ RESOURCE（站点资源）/ MEMORY（记忆条目重译）/ TEXT（按需文本，如询盘留言）',
  target_locale           VARCHAR(16)    NOT NULL COMMENT '目标语言（冗余 trn_job.target_locale，供按对象查找进行中的任务项）',
  content_id              BIGINT         NULL COMMENT 'CONTENT：内容 ID（cnt_content.id）',
  content_type            VARCHAR(32)    NULL COMMENT 'CONTENT：内容类型（选择 TranslatableContentProvider）：PAGE / PRODUCT / ARTICLE / CASE_STUDY / FAQ / TERM_PAGE / GLOBAL_BLOCK',
  source_localization_id  BIGINT         NULL COMMENT 'CONTENT：源语言本地化（cnt_localization.id）',
  source_revision_no      INT            NULL COMMENT 'CONTENT：依据的源语言修订号；出现更新的修订时本项作废（SUPERSEDED）',
  target_localization_id  BIGINT         NULL COMMENT 'CONTENT：目标语言本地化（cnt_localization.id；创建任务时由内容内核创建或返回）',
  resource_type           VARCHAR(64)    COLLATE utf8mb4_0900_bin NULL COMMENT 'RESOURCE：资源类型（{moduleId}.{资源}，如 taxonomy.term）；TEXT：调用方用途（如 inquiry.message）',
  resource_id             BIGINT         NULL COMMENT 'RESOURCE：资源 ID；TEXT：调用方对象 ID（如询盘 ID，只用于追踪）',
  source_fingerprint      CHAR(64)       COLLATE utf8mb4_0900_bin NULL COMMENT 'RESOURCE：提取时的源语言指纹（回写目标语言行的 source_fingerprint）',
  memory_id               BIGINT         NULL COMMENT 'MEMORY：被重译的翻译记忆条目（trn_memory.id）',
  status                  VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / RUNNING / SUCCEEDED / NEEDS_REVIEW / FAILED / SKIPPED / SUPERSEDED / CANCELED',
  attempts                INT            NOT NULL DEFAULT 0 COMMENT '已执行次数',
  next_attempt_at         DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最早执行时间（重试退避）',
  worker_id               VARCHAR(64)    NULL COMMENT '执行实例标识（主机名 + 进程）',
  heartbeat_at            DATETIME(3)    NULL COMMENT '执行实例最近心跳；超时视为失联，重新排队',
  engine_id               BIGINT         NULL COMMENT '最后使用的引擎（trn_engine.id；主引擎或切换后的备用引擎）',
  unit_total              INT            NOT NULL DEFAULT 0 COMMENT '提取的翻译单元数',
  unit_memory_hit         INT            NOT NULL DEFAULT 0 COMMENT '翻译记忆命中数（未调用 AI）',
  unit_deduped            INT            NOT NULL DEFAULT 0 COMMENT '本项内重复原文数（同一原文只翻译一次）',
  unit_machine            INT            NOT NULL DEFAULT 0 COMMENT '调用引擎翻译并通过校验的单元数',
  unit_failed             INT            NOT NULL DEFAULT 0 COMMENT '重试后仍未通过校验的单元数（NEEDS_REVIEW）',
  request_count           INT            NOT NULL DEFAULT 0 COMMENT '引擎请求次数（含重试）',
  input_tokens            INT            NOT NULL DEFAULT 0 COMMENT '输入 token 数',
  cached_input_tokens     INT            NOT NULL DEFAULT 0 COMMENT '其中命中服务商前缀缓存的输入 token 数',
  output_tokens           INT            NOT NULL DEFAULT 0 COMMENT '输出 token 数',
  cost                    DECIMAL(14,6)  NOT NULL DEFAULT 0 COMMENT '估算费用（按调用时引擎单价，站点预算币种）',
  engine_ms               INT            NOT NULL DEFAULT 0 COMMENT '引擎调用累计耗时（毫秒）',
  issues                  JSON           NULL COMMENT '未通过校验或出错的单元：[{"key","sourceHash","variant","checks":[…],"candidate","message"}]；TEXT 项不含原文与译文',
  error_code              VARCHAR(64)    NULL COMMENT '失败错误码（如 TRANSLATION_ENGINE_UNAVAILABLE）',
  error_message           VARCHAR(1000)  NULL COMMENT '失败说明（中文；不含原文与译文）',
  resolved_by             BIGINT         NULL COMMENT '人工处理完全部问题单元的用户',
  resolved_at             DATETIME(3)    NULL COMMENT '人工处理完毕时间（NEEDS_REVIEW → SUCCEEDED）',
  started_at              DATETIME(3)    NULL COMMENT '开始执行时间',
  finished_at             DATETIME(3)    NULL COMMENT '结束时间',
  created_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at              DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_item_claim (job_id, status, next_attempt_at),
  KEY idx_item_content (site_id, content_id, target_locale, created_at),
  KEY idx_item_resource (site_id, resource_type, resource_id, target_locale, created_at),
  KEY idx_item_review (site_id, status, target_locale),
  KEY idx_item_memory (memory_id),
  KEY idx_item_engine (engine_id),
  CONSTRAINT fk_trn_item_job FOREIGN KEY (job_id) REFERENCES trn_job (id) ON DELETE CASCADE,
  CONSTRAINT fk_trn_item_memory FOREIGN KEY (memory_id) REFERENCES trn_memory (id),
  CONSTRAINT fk_trn_item_engine FOREIGN KEY (engine_id) REFERENCES trn_engine (id),
  CONSTRAINT ck_trn_item_kind CHECK (
    (item_kind = 'CONTENT' AND content_id IS NOT NULL AND content_type IS NOT NULL AND source_revision_no IS NOT NULL AND resource_type IS NULL AND memory_id IS NULL)
    OR (item_kind IN ('RESOURCE', 'TEXT') AND resource_type IS NOT NULL AND content_id IS NULL AND memory_id IS NULL)
    OR (item_kind = 'MEMORY' AND memory_id IS NOT NULL AND content_id IS NULL AND resource_type IS NULL)
  )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '翻译任务项（一个对象 × 一个目标语言；含单元级统计与问题单元）';
```

#### 4.3.8 `trn_usage_daily`：用量日汇总

每次引擎调用后按增量写入（`INSERT … ON DUPLICATE KEY UPDATE x = x + 增量`；也可在内存中按分钟聚合后批量写入）。日期按 UTC；月度预算 = 当月各行 `cost` 之和（同一站点的主、备引擎币种与预算币种一致）。翻译记忆命中率等运行指标同时输出到 Prometheus（系统设计 27.4），本表只记录引擎调用。

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 |
| `usage_date` | DATE | 否 | — | 日期（UTC） |
| `engine_id` | BIGINT | 否 | — | 引擎（`trn_engine.id`） |
| `model` | VARCHAR(128) | 否 | — | 模型名（引擎改模型后分行统计） |
| `job_type` | VARCHAR(16) | 否 | — | 任务类型：`REALTIME` / `RESOURCE` / `BATCH` / `RETRANSLATE` / `INQUIRY` |
| `request_count` | INT | 否 | 0 | 请求次数（含失败与重试） |
| `failed_request_count` | INT | 否 | 0 | 失败的请求次数（超时、429、5xx、响应无法解析） |
| `unit_count` | INT | 否 | 0 | 提交引擎翻译的单元数 |
| `input_tokens` | BIGINT | 否 | 0 | 输入 token 数 |
| `cached_input_tokens` | BIGINT | 否 | 0 | 其中命中前缀缓存的输入 token 数 |
| `output_tokens` | BIGINT | 否 | 0 | 输出 token 数 |
| `cost` | DECIMAL(14,6) | 否 | 0 | 估算费用（按调用时单价） |
| `currency` | CHAR(3) | 否 | — | 币种（引擎的 `price_currency`） |
| `latency_ms_total` | BIGINT | 否 | 0 | 请求耗时合计（毫秒；平均延迟 = 合计 / 请求次数） |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3)，更新时自动刷新 | 更新时间 |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_usage_day (site_id, usage_date, engine_id, model, job_type)`：增量写入的目标行唯一；按站点、日期范围出报表与计算月度已用预算
- `idx_usage_engine (engine_id, usage_date)`：系统管理员按引擎查看全部站点的用量；外键索引
- `fk_trn_usage_engine`：→ `trn_engine.id`（RESTRICT）

```sql
CREATE TABLE trn_usage_daily (
  id                    BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id               BIGINT         NOT NULL COMMENT '站点',
  usage_date            DATE           NOT NULL COMMENT '日期（UTC）',
  engine_id             BIGINT         NOT NULL COMMENT '引擎（trn_engine.id）',
  model                 VARCHAR(128)   NOT NULL COMMENT '模型名（引擎改模型后分行统计）',
  job_type              VARCHAR(16)    NOT NULL COMMENT '任务类型：REALTIME / RESOURCE / BATCH / RETRANSLATE / INQUIRY',
  request_count         INT            NOT NULL DEFAULT 0 COMMENT '请求次数（含失败与重试）',
  failed_request_count  INT            NOT NULL DEFAULT 0 COMMENT '失败的请求次数（超时、429、5xx、响应无法解析）',
  unit_count            INT            NOT NULL DEFAULT 0 COMMENT '提交引擎翻译的单元数',
  input_tokens          BIGINT         NOT NULL DEFAULT 0 COMMENT '输入 token 数（取自响应 usage）',
  cached_input_tokens   BIGINT         NOT NULL DEFAULT 0 COMMENT '其中命中服务商前缀缓存的输入 token 数',
  output_tokens         BIGINT         NOT NULL DEFAULT 0 COMMENT '输出 token 数（取自响应 usage）',
  cost                  DECIMAL(14,6)  NOT NULL DEFAULT 0 COMMENT '估算费用（按调用时引擎单价）',
  currency              CHAR(3)        NOT NULL COMMENT '币种（引擎的 price_currency，ISO 4217）',
  latency_ms_total      BIGINT         NOT NULL DEFAULT 0 COMMENT '请求耗时合计（毫秒；平均延迟 = 合计 / 请求次数）',
  created_at            DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at            DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_usage_day (site_id, usage_date, engine_id, model, job_type),
  KEY idx_usage_engine (engine_id, usage_date),
  CONSTRAINT fk_trn_usage_engine FOREIGN KEY (engine_id) REFERENCES trn_engine (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '翻译用量日汇总（token 与费用）';
```

### 4.4 枚举

| 字段 | 取值 | 说明 |
|---|---|---|
| `trn_engine.protocol` | `OPENAI_CHAT` | OpenAI 兼容 Chat Completions（`POST {base_url}/chat/completions`）；V1 唯一取值 |
| `trn_engine.response_format` | `JSON_SCHEMA` | `response_format` 使用 `json_schema` 严格模式（默认） |
| | `JSON_OBJECT` | `response_format` 使用 `json_object` |
| | `NONE` | 不支持 `response_format`：依靠提示词约束 + 严格解析 + 校验重试 |
| `trn_memory.origin` | `MACHINE` / `REVIEWED` / `HUMAN` | 共享枚举：机器译文 / 人工确认的机器译文 / 人工修订；优先级 `HUMAN` > `REVIEWED` > `MACHINE` |
| `trn_memory.status` | `ACTIVE` | 有效，可命中复用（默认） |
| | `OBSOLETE` | 已作废：不再命中，保留备查 |
| `trn_memory.format` | `TEXT` / `RICH_INLINE` | 纯文本 / 含编号占位标签的富文本行内内容（系统设计 5.6） |
| `trn_memory.variant` | `default` / `max{N}` | 默认译文 / 长度上限为 N 时的变体（如 `max60`） |
| `trn_glossary_term.category` | `BRAND` / `SERIES` / `MODEL` / `STANDARD` / `LEGAL` / `GENERAL` | 品牌 / 产品系列 / 型号 / 标准号 / 法规固定文字 / 一般术语（默认） |
| `trn_style_guide.formality` | `FORMAL` / `INFORMAL` / `NEUTRAL` | 敬称（默认，如德语 Sie）/ 非正式称呼（如德语 du）/ 不区分 |
| `trn_job.job_type`、`trn_usage_daily.job_type` | `REALTIME` / `RESOURCE` / `BATCH` / `RETRANSLATE` / `INQUIRY` | 见 4.3.6 |
| `trn_job.trigger_type` | `CONTENT_PUBLISHED` | 源语言内容发布 |
| | `RESOURCE_CHANGED` | 站点资源变化 |
| | `LOCALE_ENABLED` | 站点语言开启自动翻译（开放新语言） |
| | `IMPORT` | 批量导入后 |
| | `GLOSSARY_CHANGED` | 术语表变化后重译 |
| | `MANUAL` | 后台手动创建（批量翻译、单段重译、重试） |
| | `API` | 其他模块经 `TranslationApi` 调用（询盘翻译） |
| `trn_job.status` | `PENDING` / `RUNNING` / `PAUSED` / `SUCCEEDED` / `FAILED` / `CANCELED` | 通用任务状态（共享枚举）+ `PAUSED`（批量翻译可暂停，系统设计 5.7、5.10） |
| `trn_job.pause_reason` | `MANUAL` / `BUDGET` / `ENGINE` | 人工暂停 / 达到月度预算 / 主备引擎均不可用 |
| `trn_job_item.item_kind` | `CONTENT` / `RESOURCE` / `MEMORY` / `TEXT` | 内容本地化 / 站点资源 / 记忆条目重译 / 按需文本 |
| `trn_job_item.status` | `PENDING` | 排队中 |
| | `RUNNING` | 执行中 |
| | `SUCCEEDED` | 全部单元已翻译并通过校验，已回写（或人工处理完毕） |
| | `NEEDS_REVIEW` | 有单元重试后仍未通过校验；已通过的单元已回写，不自动发布 |
| | `FAILED` | 引擎错误且重试耗尽 |
| | `SKIPPED` | 无需执行（对象已删除、目标改为 `MANUAL`、源语言已下线、记忆条目已不是 `MACHINE`） |
| | `SUPERSEDED` | 被同一对象 × 目标语言的新任务项取代（源语言发布了新修订、资源再次变化或重新翻译） |
| | `CANCELED` | 任务被取消 |
| `trn_job_item.issues[].checks[]` | `PLACEHOLDER` / `NUMBER` / `DO_NOT_TRANSLATE` / `GLOSSARY` / `LENGTH` / `RESIDUAL_CJK` / `FORMAT` | 自动校验项（系统设计 5.8）：占位标签 / 数字 / 禁止翻译的术语 / 术语表译法 / 长度上限 / 残留中文 / 输出格式（合法 JSON 且单元 ID 一一对应） |
| `cnt_localization.translation_state` 等 | `NONE` / `QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED` | 共享枚举，由各模块保存；本模块经 `ContentApi` 与资源提供方更新 |

### 4.5 站点过滤

<<FILL: | 表 | 行级拦截器（不豁免 / 豁免） | 访问方式与理由 |>>

### 4.6 跨模块引用与清理

**本模块引用其他模块的对象**：

| 本模块字段 | 引用对象（模块.表） | 被引用对象删除 / 下线时的处理 | 触发事件 |
|---|---|---|---|
| 各表 `site_id` | core.`sys_site` | V1 不删除站点；站点归档时停止调度其任务 | — |
| `trn_memory.target_locale`、`trn_job.target_locale`、`trn_style_guide.target_locale`、`trn_glossary_translation.target_locale` | core.`sys_site_locale` | 开启自动翻译：创建 `BATCH` 任务（`LOCALE_ENABLED`）翻译全部已发布内容与站点资源；关闭自动翻译：取消该语言 `PENDING` 的实时与批量任务项；记忆、术语译法、风格指南保留 | `SiteSettingsChangedEvent`（需携带变化的语言与字段） |
| `sys_setting`（`translation.config`）中的 `primaryEngineId`、`fallbackEngineId` | 本模块 `trn_engine`（保存在 core 的表中） | 引擎被引用时拒绝删除；引擎停用时站点改用备用引擎，主备都不可用时暂停任务（`ENGINE`）并告警 | — |
| `trn_job_item.content_id`、`source_localization_id`、`target_localization_id` | content.`cnt_content`、`cnt_localization` | 内容彻底删除：删除其全部任务项（`idx_item_content`）；源语言下线或进回收站：取消该内容 `PENDING` / `RUNNING` 项（`AUTO` 目标语言由内核跟随下线）；目标本地化删除：删除对应任务项 | `ContentPurgedEvent`、`ContentUnpublishedEvent`、`ContentTrashedEvent`、`LocalizationDeletedEvent` |
| `trn_job_item.content_type` | 内容类型（代码注册） | 所属模块停用：该类型的待执行项置为 `SKIPPED`；重新启用后可重试 | `ModuleDisabledEvent` |
| `trn_job_item.resource_type`、`resource_id`（`RESOURCE`） | 各模块的站点资源（navigation、form、media、product、taxonomy、notification、core、tracking） | 资源删除：取消其 `PENDING` 项，历史项保留到保留期满 | `TranslatableResourceChangedEvent`（需携带变化类型 `DELETED`，见下） |
| `trn_job_item.resource_id`（`TEXT`，`inquiry.message`） | inquiry.`inq_inquiry` | 不清理：本表不保存留言、译文与联系人信息，只有询盘 ID 与用量；保留期满物理删除 | — |
| `created_by`、`updated_by`、`reviewed_by`、`resolved_by` | core.`sys_user` | 不清理（用户只停用或软删除） | — |

**本模块订阅与发布的事件**：

| 方向 | 事件 | 处理 |
|---|---|---|
| 订阅 | `ContentPublishedEvent`（`locale = zh-CN`） | 为每个启用自动翻译的目标语言创建 `REALTIME` 任务；同一内容 × 语言的旧项置为 `SUPERSEDED` |
| 订阅 | `TranslatableResourceChangedEvent` | 创建 `RESOURCE` 任务；`DELETED` 时取消待执行项 |
| 订阅 | `SiteSettingsChangedEvent` | 语言开启 / 关闭自动翻译时创建或取消任务（见上表） |
| 订阅 | `ContentUnpublishedEvent`、`ContentTrashedEvent`、`ContentPurgedEvent`、`LocalizationDeletedEvent`、`ModuleDisabledEvent` | 见上表 |
| 发布 | `TranslationCompletedEvent` | 任务项 `SUCCEEDED` / `NEEDS_REVIEW` 后发布；content 据此自动发布或进入审核，delivery 按缓存标签失效 |
| 发布 | `TranslationFailedEvent` | 任务项 `FAILED` 后发布；notification 通知翻译审校人员 |

**提供给其他模块的服务**（platform-api `TranslationApi`）：`translateText`（询盘按需翻译，结果由 inquiry 保存在 `inq_inquiry.message_translated`，见系统设计 17.10）；按本地化查询未经人工审校的机器译文段数（供 SEO 规则“关键页面的机器译文未经审校”使用，见 seo 模块设计 3）。

本模块停用（站点级）时：不再创建新任务，调度器跳过该站点的任务（保持原状态，重新启用后继续）；翻译记忆、术语表、风格指南与已生成的译文全部保留（系统设计 3.8、5.5）。

---

### 4.7 迁移脚本

| 文件 | 内容 | 里程碑 |
|---|---|---|
| `db/migration/translation/V001__init.sql` | 4.3 中的全部表（8 张：`trn_engine`、`trn_memory`、`trn_glossary_term`、`trn_glossary_translation`、`trn_style_guide`、`trn_job`、`trn_job_item`、`trn_usage_daily`），按本节顺序创建 | <见第 13 节> |

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
| V1.0 | 2026-10-14 | 初稿 |
