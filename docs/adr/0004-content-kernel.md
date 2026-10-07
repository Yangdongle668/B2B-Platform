# ADR-004：内容内核：Content / Localization / Revision，发布快照与投影

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-06 |
| 决策者 | 业主、架构负责人 |
| 相关章节 | 0.2（第 4、5 项）、0.3、1.5、2.1（P4）、3.7、4.4、5.5、6.1–6.7、13.4、25.8、25.9、30、31 |
| 相关 ADR | ADR-002、ADR-003、ADR-005、ADR-006、ADR-008、ADR-012、ADR-013、ADR-017 |

## 背景

- V1.0 中存在三套相互冲突的多语言模型（0.2 第 4 项），并且只有页面有版本（PageVersion），产品等内容没有版本机制（0.2 第 5 项）。
- 需求：
  - 每种语言独立的 slug、标题、SEO 与发布状态（0.3）；V1.3 起简体中文为源语言，其他语言由 AI 翻译生成（ADR-005、ADR-017）；
  - 能表达“已发布，同时有尚未发布的修改”；支持定时发布、回滚、审核、回收站与审计（6.3）；
  - 前台只读取发布态数据，草稿永远不能泄漏到线上（P4）；预览必须与发布后的渲染一致（6.5）；
  - 前台性能：Delivery API P95 缓存命中 ≤ 30 ms、未命中 ≤ 300 ms（1.5）；列表、筛选、路由、sitemap 需要高效的发布态数据；sitemap 的 `lastmod` 要真实。
- 内容类型分属不同模块（page、product、article、taxonomy，6.2），各自有业务字段；内核不能依赖这些模块（ADR-002）。

## 决策

1. **三层模型**（6.1）：

```text
cnt_content（语言无关）：id、site_id、type、owner_module、routable、primary_locale（源语言）、trashed_at
 ├── cnt_localization（每种语言一条）：slug、草稿状态、发布状态、定时、下线策略、
 │                                    translation_mode、translation_state、source_locale、source_revision_no、
 │                                    投影字段 pub_*、content_modified_at、version
 ├── cnt_revision（不可变的发布快照）：revision_no、payload（JSON）、payload_hash、published_by、published_at
 ├── cnt_published_term（投影：发布态的分类指派）
 └── cnt_review（审核记录）
```

2. **内核与业务数据分离**：内核拥有身份、类型、语言版本、slug、发布状态、修订、快照、投影；业务模块拥有本类型的草稿态业务字段，存在自己的表中，以 `content_id` 关联。业务模块通过 `ContentApi.create(type, locale)` 创建内容，通过 `ContentApi.updateDraftMeta()` 同步后台列表所需的标题。
3. **两个正交状态**（6.3）：`draft_state`（EDITING / IN_REVIEW / APPROVED）与 `publish_state`（NEVER / SCHEDULED / PUBLISHED / UNPUBLISHED）；内容级另有回收站状态。“有未发布的修改”通过比较 `draft_hash` 与 `pub_hash` 判断。
4. **发布在一个事务中完成**（4.4）：校验状态、权限与乐观锁 → 预留路径 → 各模块 `PublishContributor.validate()` → `contribute()` 组装快照 → 写入 `cnt_revision` → 更新投影（`cnt_localization.pub_*`、`cnt_published_term`、`url_route`）→ 路径变化时生成 301 → 发布 `ContentPublishedEvent`。
5. **快照规则**（6.4）：快照只包含本内容自身的数据；对其他内容、媒体、分类只保存 ID（`$content`、`$media` 等），读取时解析；快照带 `schemaVersion`，大小上限 1 MB。
6. **投影规则**：列表、筛选、路由、sitemap 都基于投影，永远不读草稿表；`content_modified_at` 只在 `payload_hash` 变化时更新，用作 sitemap 的 `lastmod`。
7. **回滚**：把历史快照经 `PublishContributor.restore()` 恢复为草稿，再走发布流程。
8. **预览**：Delivery 调用同一套 `contribute()` 从草稿临时组装快照，不持久化（6.5）。
9. **修订保留**：90 天内全部保留；之后每个本地化至少保留最近 20 个修订。
10. **并发控制**：可编辑实体使用乐观锁（`version` 或 `If-Match`），冲突返回 409（6.6）。
11. **翻译相关**：`translation_mode` 为 SOURCE / AUTO / MANUAL，`translation_state` 的取值见 5.5；AUTO 模式的目标语言本地化在翻译完成后按站点配置自动发布，与人工发布走同一套校验与流程；源语言下线或进回收站时，AUTO 模式的目标语言跟随下线（6.3，详见 ADR-017）。
12. **验证顺序**：M1 先用 PRODUCT 一种类型验证“快照 + 投影”模型，M2 再扩展到其他类型（30）。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| V1.0 方式：只有页面有版本，其他内容直接读写业务表 | 简单 | 产品等没有修订与回滚；草稿与线上混在一起 | 不采用 |
| 各业务模块自行实现多语言与版本 | 模块自治 | 多套模型冲突（V1.0 的问题）；URL、SEO、图谱、搜索、翻译要分别适配每种模型 | 不采用 |
| 前台直接读取业务表中的发布态字段，不生成快照 | 不需要快照存储 | 渲染时跨模块拼装，速度慢且结果不稳定；草稿泄漏风险；回滚与审计困难 | 不采用 |
| 快照冻结被引用对象的完整数据 | 渲染时无需再查询 | 被引用对象（图片 alt、另一个产品的标题）变化后，所有引用方都要重新发布 | 不采用：只存 ID，读取时解析 |
| Content + Localization + Revision，发布生成快照与投影 | 生命周期统一；前台按 ID 读一行即可渲染；草稿不泄漏；回滚与审计天然支持 | 快照存储与双份数据；各模块要实现快照贡献 | 采用 |

## 后果

### 正面

- 所有内容类型共用同一套多语言、发布、定时、审核、回滚、回收站与预览机制；新增内容类型只需实现扩展点（29.2）。
- 前台渲染快且结果一致，草稿永远不会泄漏到线上；预览与发布使用同一套代码。
- 投影支撑列表、筛选、路由与 sitemap，`lastmod` 只反映真实变化。
- 翻译状态与源语言修订号记录在本地化上，AI 翻译与人工修订可以在同一模型中表达。

### 负面与代价

- 草稿数据（业务表）与发布数据（快照）是两份，每个参与发布的模块都要实现 `validate`、`contribute`、`restore`。
- 快照按修订累积，需要执行保留策略；快照结构的演进要依靠 `schemaVersion` 并保持兼容。
- 引用只存 ID，读取时解析，Delivery 结果的缓存需要按被引用对象的 Cache Tag 失效（ADR-012）。
- 内核设计错误会导致大面积返工（31），因此需要在 M1 先用 PRODUCT 验证。

### 需要遵守的规则

| 规则 | 检查方式 |
|---|---|
| 前台与 Delivery 只读取快照与投影，永远不读草稿（P4） | PR 评审；Delivery 只通过 content 等模块的服务接口取数（模块只访问自己的表，由 ArchUnit 与表前缀扫描检查，29.3） |
| 业务模块不直接读写 `cnt_*` 表，通过 `ContentApi` 操作内容 | ArchUnit；SQL 表前缀扫描（29.3） |
| 快照与布局文档中对其他对象只保存 ID，不保存 URL（使用 `$content` / `$media` / `$term` / `$source`） | 发布校验（布局文档符合 Schema，6.3）；PR 评审（附录 A） |
| 新内容类型实现 `ContentTypeProvider`、`PublishContributor` 等 29.2 第 3 步列出的扩展点；`restore()` 能把快照恢复为草稿 | 完成定义（29.6）；模块集成测试 |
| 快照不超过 1 MB；结构变更提升 `schemaVersion`，不兼容变更先写 ADR | 发布校验；29.5 |
| 可编辑实体的更新请求携带 `version`，或使用 `ETag` / `If-Match`，冲突返回 409 / 412（24.2） | PR 评审 |
| 发布、回滚、自动重定向的端到端行为 | E2E 场景 S1、S2、S8（28） |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-10-06 | 创建 |
