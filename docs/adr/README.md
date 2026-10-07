# 架构决策记录（ADR）

本目录保存平台的架构决策记录（Architecture Decision Record，ADR）。系统设计说明书（[`docs/architecture/system-design.md`](../architecture/system-design.md)，以 V1.3 为准）第 2.5 节给出全部决策的摘要，附录 B 给出文件索引；本目录中的文件是每项决策的正式记录。

## ADR 是什么

ADR 是一份简短的文档，记录一项架构决策：当时要解决什么问题、受哪些约束、做了什么决定、考虑过哪些备选方案、带来哪些后果，以及开发时必须遵守的规则。它回答“为什么这样设计”，供后来的开发者与 AI 编码助手（Claude Code）在修改代码之前了解边界与理由（见系统设计 29.7）。

- 一个 ADR 只记录一项决策；
- 格式沿用 MADR 模板（背景、决策、备选方案、后果、状态），本目录统一使用 [template.md](template.md)；
- 文件名为 `NNNN-title.md`：四位编号 + 英文短标题（kebab-case），如 `0003-event-reliability.md`；
- 正文用中文书写；引用系统设计说明书时写明章节编号（以当前版本为准）。

## 何时必须写 ADR

按系统设计 29.5，以下情况必须先写 ADR：

| 情况 | 说明与例子 |
|---|---|
| 新增模块 | 如 V1.3 新增的 translation 模块（ADR-017）；V2+ 的 crm、quotation 等模块 |
| 跨模块契约变更 | `platform-api` 中的服务接口、扩展点、事件、DTO 的新增或变更（见 3.3 R3） |
| 引入新的基础设施依赖 | 如引入消息队列（V1 不引入，见 ADR-003）、新的存储或外部服务 |
| 不兼容的数据模型变更 | 破坏性的数据变更（见 25.6），如删除列、改变字段含义 |
| 偏离架构原则 | 偏离第 2.1 节的原则 P1–P8；P8 规定“未经 ADR 不得修改核心架构或破坏模块边界” |

## 状态

| 状态 | 含义 |
|---|---|
| Proposed（待评审） | 已整理为正式记录，等待评审；评审通过后改为 Accepted |
| Accepted（已确认） | 已通过评审或已由业主确认，开发必须遵守；改变决策需要新的 ADR |
| Superseded（已被取代） | 已被后续的 ADR 取代。文件保留，状态写为“Superseded（被 ADR-NNN 取代）”；新 ADR 的“相关 ADR”中注明取代了哪一个 |
| Deprecated（已废弃） | 决策不再适用（如对应的功能或模块已移除），且没有新的 ADR 取代。文件保留，在变更记录中写明原因 |

## 流程

1. **起草**：复制 [template.md](template.md)，取下一个未使用的编号（编号不复用），状态填 Proposed；背景中引用系统设计说明书的具体章节与结论。
2. **评审**：通过 PR 提交，按 29.4 审核：至少 1 人；涉及 platform-core / platform-api 的决策需要 2 人，其中包括架构负责人。需要业主确认的事项（如产品形态、语言策略）由业主确认。
3. **确认**：评审通过后把状态改为 Accepted，并在“变更记录”中写明日期。
4. **同步**：同时更新本文件的索引，以及系统设计说明书第 2.5 节的摘要与附录 B。
5. **修改**：Accepted 的 ADR 不改写决策内容。需要改变决策时新写一个 ADR，并把旧 ADR 的状态改为 Superseded；只做文字澄清、补充链接等不改变决策的修改时，可以直接修改，并记入变更记录。
6. **使用**：开发前阅读相关模块文档与 ADR（29.7）；PR 中涉及 ADR 规定的规则时引用 ADR 编号。

## 索引

ADR-000、ADR-005、ADR-009、ADR-017 为 Accepted，其余为 Proposed（附录 B）。部署（云厂商与 CDN、是否需要美国源站）由业主自行负责，ADR-015 只作为建议；ADR-016 中需要法务确认的事项由业主自行负责（32 章 Q3、Q13、Q14、Q15）。

| 编号 | 标题 | 状态 | 文件 |
|---|---|---|---|
| ADR-000 | 自研，而不是基于现有 Headless CMS | Accepted | [0000-build-vs-buy.md](0000-build-vs-buy.md) |
| ADR-001 | 编译期模块 + 站点级启用开关 | Proposed | [0001-compile-time-modules.md](0001-compile-time-modules.md) |
| ADR-002 | 模块通信：公开 API、领域事件、扩展点 | Proposed | [0002-module-communication.md](0002-module-communication.md) |
| ADR-003 | 事件可靠性：事务后投递 + Event Publication Registry | Proposed | [0003-event-reliability.md](0003-event-reliability.md) |
| ADR-004 | 内容内核：Content / Localization / Revision，发布快照与投影 | Proposed | [0004-content-kernel.md](0004-content-kernel.md) |
| ADR-005 | 语言策略与 URL | Accepted | [0005-i18n-and-url-strategy.md](0005-i18n-and-url-strategy.md) |
| ADR-006 | 模板 + 插槽与页面搭建两种渲染模式 | Proposed | [0006-template-and-page-builder.md](0006-template-and-page-builder.md) |
| ADR-007 | 编辑器画布使用 iframe 嵌入 Nuxt 预览 | Proposed | [0007-editor-canvas-iframe.md](0007-editor-canvas-iframe.md) |
| ADR-008 | SEO 数据归属：自动值与覆盖值 | Proposed | [0008-seo-data-ownership.md](0008-seo-data-ownership.md) |
| ADR-009 | 站点与语言模型（不设租户层） | Accepted | [0009-site-and-locale.md](0009-site-and-locale.md) |
| ADR-010 | 后台认证：Session + HttpOnly Cookie + CSRF + TOTP | Proposed | [0010-admin-authentication.md](0010-admin-authentication.md) |
| ADR-011 | 技术版本锁定 | Proposed | [0011-technology-versions.md](0011-technology-versions.md) |
| ADR-012 | 两级缓存与 Cache Tag 精确失效 | Proposed | [0012-cache-tags.md](0012-cache-tags.md) |
| ADR-013 | Meilisearch 作为搜索读模型 | Proposed | [0013-search-read-model.md](0013-search-read-model.md) |
| ADR-014 | ID 策略：TSID，JSON 中序列化为字符串 | Proposed | [0014-id-strategy.md](0014-id-strategy.md) |
| ADR-015 | 部署区域与数据驻留 | Proposed（部署由业主负责，本 ADR 为建议） | [0015-hosting-region-and-data-residency.md](0015-hosting-region-and-data-residency.md) |
| ADR-016 | 隐私合规基线 | Proposed（法务事项由业主负责） | [0016-privacy-compliance-baseline.md](0016-privacy-compliance-baseline.md) |
| ADR-017 | AI 翻译与翻译记忆 | Accepted | [0017-ai-translation-and-translation-memory.md](0017-ai-translation-and-translation-memory.md) |
