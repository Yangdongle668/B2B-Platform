# 模块化 B2B 企业网站平台 系统设计说明书

| 项 | 内容 |
|---|---|
| 文档版本 | V1.3（在 V1.2 基础上修订；V1.2 基于 V1.1 修订，V1.1 基于 V1.0《系统架构设计说明书》修订） |
| 状态 | 草案（Draft）：产品形态、目标市场、自研、语言策略与 AI 翻译已确认，其余待评审 |
| 更新日期 | 2026-10-06 |
| 项目代号 | B2B Web Platform |
| 定位 | 模块化、可扩展、SEO 原生、设计系统驱动的企业网站与询盘获客平台 |

> 阅读提示：
> - 标注 **【V1】** 的内容属于第一版交付范围；标注 **【V2+】** 的为预留设计，V1 只保证数据结构和接口不阻碍其实现。
> - 第 0.4 节列出了本文依赖的关键假设，第 0.5 节记录了 V1.2、V1.3 已确认的决策，第 32 章列出了待业务确认的问题。假设不成立时，需回到对应章节调整。

## 目录

- [0. 文档说明](#0-文档说明)
- [1. 需求与范围](#1-需求与范围)
- [2. 架构总览](#2-架构总览)
- [3. 模块化设计](#3-模块化设计)
- [4. 领域事件](#4-领域事件)
- [5. 站点与语言](#5-站点与语言)
- [6. 内容内核（Content Kernel）](#6-内容内核content-kernel)
- [7. 分类体系（Taxonomy）](#7-分类体系taxonomy)
- [8. 产品模块（Product）](#8-产品模块product)
- [9. 文章模块（Article）](#9-文章模块article)
- [10. 页面、模板与组件](#10-页面模板与组件)
- [11. 设计系统与主题](#11-设计系统与主题)
- [12. 媒体模块（Media）](#12-媒体模块media)
- [13. URL 引擎](#13-url-引擎)
- [14. SEO 引擎](#14-seo-引擎)
- [15. 内容关系图谱（Content Graph）](#15-内容关系图谱content-graph)
- [16. 导航与站点资料](#16-导航与站点资料)
- [17. 表单与询盘](#17-表单与询盘)
- [18. 通知（Notification）](#18-通知notification)
- [19. 站内搜索（Search）](#19-站内搜索search)
- [20. 统计追踪与隐私同意](#20-统计追踪与隐私同意)
- [21. 前台网站（Nuxt）](#21-前台网站nuxt)
- [22. 管理后台（Admin）](#22-管理后台admin)
- [23. 缓存架构](#23-缓存架构)
- [24. API 设计规范](#24-api-设计规范)
- [25. 数据库设计规范](#25-数据库设计规范)
- [26. 安全设计](#26-安全设计)
- [27. 部署与运维](#27-部署与运维)
- [28. 测试策略](#28-测试策略)
- [29. 工程规范与架构治理](#29-工程规范与架构治理)
- [30. 实施路线图](#30-实施路线图)
- [31. 风险与对策](#31-风险与对策)
- [32. 待确认问题](#32-待确认问题)
- [附录 A：CLAUDE.md 草案](#附录-aclaudemd-草案)
- [附录 B：ADR 索引](#附录-badr-索引)
- [附录 C：后续详细设计文档](#附录-c后续详细设计文档)

---

## 0. 文档说明

### 0.1 目的与读者

本文档是平台的顶层系统设计，用作：

- 需求范围与验收标准的依据；
- 后续详细设计（数据库、Page Schema、扩展点、SEO 引擎等）的上位约束；
- 开发人员与 AI 编码助手（Claude Code）的架构边界依据。

读者：产品负责人、架构师、后端/前端开发、测试、运维。

### 0.2 相对 V1.0 的主要变更

| # | 变更 | 原因 |
|---|---|---|
| 1 | 新增第 1 章“需求与范围”：定位、角色、场景、V1 边界与非功能指标 | V1.0 缺少需求层，无法界定 MVP 与验收标准 |
| 2 | 模块形态明确为**编译期模块 + 运行时启用开关**，取消运行时热插拔 | Nuxt 与 Admin 无法在运行时加载新组件；热插拔复杂度与收益不匹配 |
| 3 | 引入 `platform-api` **扩展点（Extension Point）**，与领域事件并列为解耦手段 | 修正 V1.0 中 Product → SEO 的反向依赖 |
| 4 | Content 拆为 `Content`（语言无关）+ `ContentLocalization`（每种语言一条）；多语言提前到 M1 | 消除 V1.0 中三套多语言模型的冲突 |
| 5 | 新增“发布快照 + 发布投影”模型，统一草稿、发布、回滚 | V1.0 只有 PageVersion，产品等内容没有版本机制 |
| 6 | SEO 数据统一归 SEO 模块所有，每个字段区分“自动值 / 人工覆盖值” | V1.0 中 SEO 数据分散在 4 处 |
| 7 | 新增模块：taxonomy、url、form、navigation、notification、search、tracking、delivery | 补齐获客闭环与 Content Graph 的基础 |
| 8 | 原 `module-cms` 拆为 `content`（内核）、`article`（编辑型内容）、`page`（页面搭建）；原 `module-i18n` 并入 platform-core（语言配置、界面文案）与 content（内容本地化） | 原模块边界不清 |
| 9 | Inquiry 收窄为“线索捕获 + 分配 + 跟进”；报价、谈判、成交移交未来的 CRM / Quotation 模块 | 避免与 CRM 状态重叠 |
| 10 | 事件补全为成对的生命周期（发布/下线/删除/路径变化…）；事件可靠性采用 Outbox | V1.0 没有下线事件，也没有可靠性设计 |
| 11 | 缓存失效改为 **Cache Tag** 精确失效 | V1.0 的失效粒度过粗 |
| 12 | 编辑器画布采用 **iframe 嵌入 Nuxt 预览**，组件 Schema 在前后端之间共享 | V1.0 没有设计画布渲染 |
| 13 | 新增站点 → 语言两级上下文（自用，不设租户层），隔离覆盖 DB、Redis、存储、搜索、CDN | V1.0 只预留了 tenant_id，未设计站点与语言；确认自用后不再需要租户层 |
| 14 | 开发顺序改为垂直切片（M0–M5） | V1.0 中前台与多语言排得过晚 |
| 15 | 新增安全、运维、测试、治理章节；架构规则全部配套自动化检查 | V1.0 的规则只停留在文字层面 |

### 0.3 术语表

| 术语 | 含义 |
|---|---|
| Site（站点） | 一个对外网站（一个主域名）。企业可以有多个站点（品牌站、国家站），每个站点可以有多种语言 |
| Locale（语言） | 站点启用的语言，如 `en`、`de`、`es`，采用 BCP 47 代码 |
| Content（内容） | 所有可被路由、被关联、被 SEO 的对象的语言无关抽象 |
| Localization（本地化） | 内容在某一语言下的版本，拥有独立的 slug、标题、SEO 与发布状态 |
| 源语言 | 编辑录入内容与站点资源所用的语言，为简体中文（`zh-CN`），即 `cnt_content.primary_locale`；其他语言的本地化由 AI 翻译从源语言生成 |
| 翻译单元（Segment） | 最小翻译单位：纯文本字段一个字段一个单元，富文本按块级节点拆分（见 5.6） |
| 翻译记忆（TM） | 已翻译单元的“原文 → 译文”存储；相同原文命中即复用，不再调用 AI；人工修订优先于机器译文（见 5.6） |
| 术语表（Glossary） | 站点级的中文术语及其各语言译法，可标记“禁止翻译”（品牌名、型号等），翻译时必须遵守（见 5.8） |
| Revision（修订） | 某个本地化的一次发布，不可变 |
| Snapshot（快照） | 发布时冻结的完整渲染数据（JSON），存放在修订中 |
| Projection（投影） | 发布时从快照中提取、用于列表、筛选、路由的只读数据 |
| Template（模板） | 某类内容的页面骨架，定义固定区域与可编辑插槽 |
| Component（组件） | 页面中可复用的区块，带有编辑 Schema |
| Variant（变体） | 同一组件的不同视觉形态，由主题提供 |
| Section / Slot | 页面中的一个组件实例 / 模板中允许放置组件的位置 |
| Theme（主题） | 视觉层：Token、字体、组件变体、Header/Footer 样式 |
| Design Token | 设计变量（颜色、间距、字号等），是前台样式的唯一来源 |
| Extension Point（扩展点） | 定义在 `platform-api` 中、由模块实现、由平台或其他模块收集调用的接口 |
| Delivery API | 面向前台网站的内容交付聚合接口 |
| Taxonomy / Term | 分类体系（词汇表）/ 分类项，如产品分类、应用、行业、标签 |
| Content Graph | 内容之间的关系网络，驱动相关推荐与内链 |
| Cache Tag | 缓存条目上的标签，用于按内容精确失效 |
| Lead / Inquiry | 线索 / 询盘，访客通过表单提交的商机 |

### 0.4 关键假设

| 编号 | 假设 | 影响章节 |
|---|---|---|
| A1 | **已确认**：自用。一家企业，可以有多个品牌站或国家站；不做 SaaS，不为其他企业交付 | 3、5、25 |
| A2 | **已确认**：目标市场为欧盟、英国与北美；内容以简体中文录入（源语言）；前台 V1 先只开放中文，其他语言（建议 en、de、fr、es、it）由 AI 翻译生成后按需开放 | 5、13、14 |
| A3 | 服务器部署在欧盟（默认法兰克福）+ 全球 CDN，云厂商、CDN 的选择与部署由业主自行负责（见 32 章 Q3）；后台用户主要在中国大陆，后台界面只用简体中文 | 22、27 |
| A4 | 单站规模：≤ 1 万产品、≤ 5 千篇文章、≤ 10 种语言、询盘 ≤ 1 千条/天 | 1.5、19、25 |
| A5 | 不展示价格、不在线交易；转化目标是询盘、索样、资料下载 | 8、17 |
| A6 | 团队 2–6 人，Java + Vue 技术栈，使用 Claude Code 辅助开发 | 29、附录 A |
| A7 | 多数客户已有旧站，需要迁移内容与 URL | 8.6、13.5 |

### 0.5 修订记录

**V1.2（2026-10-06）**

V1.2 确认了以下三项决策，并据此修订相关章节；其余内容沿用 V1.1。

| 确认事项 | 主要影响 | 涉及章节 |
|---|---|---|
| 自用 | 去掉租户层，站点为最高隔离单位；用户全局唯一，角色按站点分配；单套生产环境；未来多企业采用一企业一实例 | 0、1、2.4、2.5、3、4、5、6、12、19、22、25、26、27、28、30、32、附录 A、附录 B |
| 欧美为主 | 首批语种建议；欧盟部署与数据驻留；GDPR / UK GDPR / ePrivacy / CCPA 合规基线；询盘改为隐私告知 + 可选的营销同意；法律页面；双单位显示；德语 slug 音译；Google 与 Bing | 0、1、2、3、5、8、10、11、12、13、14、16、17、18、20、22、24、25、26、27、30、31、32、附录 B |
| 自研 | ADR-000 确认（Accepted） | 2.5、32、附录 B |

**V1.3（2026-10-06）**

V1.3 确认了以下事项，并据此修订相关章节；其余内容沿用 V1.2。

| 确认事项 | 主要影响 | 涉及章节 |
|---|---|---|
| 后台只用简体中文 | 后台界面只提供简体中文，不做语言切换，不引入 vue-i18n，界面文案集中在常量 / 字典文件中；后台错误信息使用中文；`module.json`、组件与模板定义中的名称、标签只写中文字符串 | 0、1、2.4、3.5、10、15、18、22、24、附录 A |
| 中文为源语言、前台先开放中文、URL 全部带前缀 | 内容与站点资源只用简体中文录入；前台 V1 先只开放中文，其他语言由 AI 翻译生成后按语言开放；全部语言 URL 带前缀，根路径 302 到默认对外语言；hreflang 与 x-default 调整；中文 slug 用拼音音译、译文 slug 首次发布后固定；中文使用系统字体栈；搜索使用中文分词 | 0、1、2.5、5、6、11、13、14、16、19、21、30、32、附录 B |
| AI 翻译与翻译记忆（OpenAI 兼容接口） | 新增 translation 模块（L2）：翻译单元、翻译记忆（同一原文只翻译一次）、术语表、风格指南、翻译编辑器（人工修订优先）、批量翻译、用量与成本统计；引擎默认使用 OpenAI 兼容的 Chat Completions 接口；法律页面人工审校后发布；询盘留言一键翻译为中文；ADR-017 | 0、1、2、3、4、5、6、8、9、10、12、13、14、16、17、18、20、21、22、24、25、26、27、28、30、31、32、附录 A、附录 B |
| 部署与法务事项由业主负责 | Q3（云厂商与 CDN）、Q13、Q14、Q15 由业主自行负责，不在本文范围；第 27 章给出参考拓扑与要求 | 0、16、27、32 |

---

## 1. 需求与范围

### 1.1 产品定位

一句话：**让 B2B 制造与贸易企业用“搭积木”的方式运营一个设计专业、SEO 自动维护、能持续获取海外询盘的官网。**

与传统 CMS 的差异：

1. **SEO 原生**：URL、Meta、Schema、Sitemap、hreflang、内链由系统自动维护，人工只做覆盖。
2. **内容关系驱动**：新增内容自动与已有的产品、应用、案例、文章建立关联，并自动生成内链。
3. **设计系统驱动**：运营只能在设计系统的约束内选择组件与变体，前台始终保持设计质量。
4. **获客闭环**：从访问来源到询盘、分配、通知、跟进，全链路可追踪。
5. **模块化**：CRM、报价、WhatsApp、AI 等能力以模块形式增加，不改动内核。

### 1.2 用户角色

| 角色 | 主要职责 | 典型权限 |
|---|---|---|
| 系统管理员 | 站点、模块、系统设置 | 全部（所有站点） |
| 站点管理员 | 站点设置、主题、导航、用户与角色 | 站点内全部 |
| 内容编辑 | 创建、编辑产品、文章、页面 | 编辑、提交审核 |
| 审核 / 发布者 | 审核并发布内容 | 审核、发布、下线 |
| SEO 专员 | SEO 覆盖、重定向、SEO 健康、内链 | SEO 相关全部 |
| 翻译审校（可选） | 审校与修订 AI 译文、维护术语表 | 翻译中心、指定语言的译文修订 |
| 销售 | 处理分配给自己的询盘 | 询盘（数据范围：本人） |
| 销售主管 | 分配询盘，查看团队询盘与报表 | 询盘（数据范围：团队或全部） |
| 开发者 / 实施 | 统计代码、自定义脚本、技术设置 | 技术设置（高危权限，强审计） |
| 访客 / 采购商 | 浏览、搜索、下载资料、提交询盘 | 前台公开 |
| 搜索引擎 / AI 爬虫 | 抓取页面 | 前台公开 |

### 1.3 核心用户场景（验收用）

| 编号 | 场景 | 验收要点 |
|---|---|---|
| S1 | 编辑用中文新建一个产品，选择分类、应用、行业，上传图片与 Datasheet，提交审核并发布 | 中文页面可访问；SSR 输出完整 HTML；Meta、canonical、Product 与 BreadcrumbList JSON-LD 自动生成；sitemap 自动更新；相关产品与文章自动出现；若已开放英语，英语版本在 5 分钟内自动生成并发布 |
| S2 | 编辑修改已发布产品的 slug | 旧 URL 自动 301 到新 URL；不产生重定向链；内链与 sitemap 自动更新 |
| S3 | 运营不写代码，用页面编辑器搭建 Landing Page（Hero + 优势 + 产品网格 + 案例 + FAQ + 询盘表单），并在三种设备尺寸下预览 | 只能使用设计系统内的组件与变体；草稿不影响线上；可以分享预览链接 |
| S4 | 海外采购商从 Google 广告进入落地页，浏览两个产品后提交询盘（附图纸） | 询盘记录首次与末次来源、UTM、gclid、落地页、浏览过的产品；通过反垃圾校验；按国家与产品自动分配；销售 1 分钟内收到邮件；客户收到对应语言的自动回复；GA4 按访客的同意状态（Consent Mode v2）收到转化事件 |
| S5 | 采购商下载需要留资的产品目录 PDF | 填写表单后获得下载链接；生成一条 DOWNLOAD 类型线索 |
| S6 | 编辑从 Excel 批量导入 300 个产品 | 异步执行；先校验预览再提交；逐行报告错误；可重复导入（按型号更新） |
| S7 | 从旧站迁移：导入旧 URL → 新 URL 映射表 | 批量生成 301；检测循环与链；上线后旧 URL 全部可以跳转 |
| S8 | 某产品停产 | 可选择下线为 410，或 301 到替代产品/分类；所有引用处自动移除该产品卡片；sitemap 自动移除 |
| S9 | SEO 专员查看站点 SEO 健康报告 | 列出孤立页面、重复标题、缺失 alt、hreflang 不完整等问题，并可跳转修复 |
| S10 | 站点管理员停用“文章”模块 | 后台菜单、API、组件面板中不再出现；系统提示受影响的已发布内容；数据全部保留 |
| S11 | 站点管理员开放英语 | 系统用 AI 批量翻译全部已发布内容与站点资源（菜单、表单、界面文案、属性名等）；完成后英语页面、hreflang、sitemap 自动生效；翻译记忆中已有的原文不调用 AI；可在翻译中心查看进度与费用 |
| S12 | 编辑修改已发布产品的一段描述后重新发布 | 只有被修改的段落调用 AI 重新翻译，其余段落直接复用翻译记忆；人工修订过的译文保持不变 |
| S13 | 销售查看一条德语询盘，一键翻译为中文 | 译文保存在询盘上，再次查看不重复调用 AI |

### 1.4 V1 范围

**V1 包含（In Scope）**

- **平台**：站点、语言上下文；用户、角色、权限（含数据范围）、2FA；模块管理（启用/停用）；设置、审计、任务、字典；数据主体请求工具与保留期任务；管理后台界面为简体中文。
- **内容**：内容内核（多语言、修订、发布、定时发布、预览、回滚、回收站、单级审核）；分类体系；产品；文章（新闻、博客、案例、FAQ）；页面；产品属性双单位显示（公制 + 英制）。
- **AI 翻译**：翻译记忆（同一原文只翻译一次）、术语表、风格指南、翻译编辑器（人工修订）、批量翻译、用量与成本统计；引擎默认使用 OpenAI 兼容接口。
- **呈现**：Design Token；1 套主题（≥ 20 个组件，每个 2–3 个变体）；模板；页面编辑器（Section 级）；全局区块；导航；站点资料；法律页面（隐私政策、Cookie 政策、条款、Imprint）。
- **媒体**：上传、文件夹、多语言 alt、图片变换（WebP/AVIF/响应式）、引用追踪、私有文件。
- **SEO**：URL、slug、重定向；Meta 自动生成与覆盖；canonical、robots、hreflang、Schema.org、Sitemap、IndexNow；内链（导航、面包屑、相关内容、正文引用）；SEO 健康检查。
- **关系**：Content Graph（人工关系 + 基于分类的规则评分）。
- **获客**：表单引擎；询盘（分配、去重、状态、备注、附件、导出）；反垃圾；来源归因；资料下载留资；询价篮。
- **通知**：邮件（询盘通知、自动回复、SLA 提醒）、Webhook。
- **搜索**：站内搜索、产品分面筛选。
- **追踪**：GA4 / GTM / Pixel 配置（可切换为欧盟托管的统计工具）、Consent Mode v2、Cookie 事先同意与同意记录、GPC、转化事件。
- **工具**：产品 Excel 导入导出、重定向导入。
- **工程**：CI/CD、监控、备份、自动化架构守护。

**V1 不包含（Out of Scope，V2+）**

- 运行时热插拔模块、第三方模块市场；
- 多企业 SaaS（不在规划中；如未来需要，采用一企业一实例部署）；
- CRM、报价单、WhatsApp / 企业微信集成、Newsletter、客户门户；
- AI 写作、SEO 建议、语义相似度、垃圾识别模型（AI 翻译已纳入 V1）；
- 后台界面多语言；
- 自建访问统计（V1 依赖 GA4 或欧盟托管的统计工具，如 Matomo、Plausible，只提供询盘来源报表）；
- 自由布局编辑（像素级拖拽）、多主题市场；
- 多级审批流、实时协同编辑；
- 产品价格、购物车、在线支付。

### 1.5 非功能需求

| 类别 | 指标 | 目标 |
|---|---|---|
| 前台性能 | LCP（移动端，真实用户 75 分位） | ≤ 2.5 s |
| | INP / CLS（75 分位） | ≤ 200 ms / ≤ 0.1 |
| | HTML TTFB（CDN 命中 / 未命中） | ≤ 200 ms / ≤ 800 ms |
| | 首屏 JS（gzip，单页） | ≤ 150 KB |
| | Lighthouse（移动端，典型产品页） | Performance ≥ 90，SEO = 100，Accessibility ≥ 95 |
| 后端性能 | Delivery API P95（缓存命中 / 未命中） | ≤ 30 ms / ≤ 300 ms |
| | Admin API P95 | ≤ 500 ms |
| | 发布后前台可见（含缓存失效） | ≤ 60 s |
| AI 翻译 | 单条内容发布到目标语言生成 | ≤ 5 分钟（不含人工审校）；翻译记忆命中的原文不调用 AI |
| 规模 | 单站内容 | 1 万产品 × 10 种语言、5 千篇文章；Content Graph 关系 ≤ 50 万条 |
| | 并发 | 前台 200 RPS（CDN 未命中），后台 50 个并发用户 |
| 可用性 | 前台（含 CDN） | 99.9%；后端故障时 CDN 继续提供过期缓存（stale-if-error） |
| | 后台 | 99.5% |
| 数据 | 备份 | RPO ≤ 15 分钟，RTO ≤ 4 小时，每季度做一次恢复演练 |
| 安全 | 标准 | OWASP ASVS Level 2；后台高权限账号强制 2FA |
| 合规 | 隐私 | GDPR、UK GDPR、ePrivacy（Cookie 事先同意；英国为 PECR）；CCPA/CPRA 等美国州隐私法（GPC、退出“出售 / 共享”）；加拿大（PIPEDA、魁北克第 25 号法律、CASL）的要求由法务确认；数据驻留欧盟（设计决策：数据库、对象存储、备份存放在欧盟，见 ADR-015；经全球 CDN 的传输、美国服务商的处理以及中国大陆员工的访问是否构成跨境传输、需要何种传输机制，由法务确认，见 32 章 Q13）；数据主体请求按法定时限处理 |
| 无障碍 | 前台 | WCAG 2.2 AA；提供无障碍声明页。欧洲无障碍法案（EAA）自 2025-06-28 起适用，主要针对面向消费者的产品与服务，B2B 站点是否适用由法务确认；美国存在基于 ADA 的网站无障碍诉讼风险 |
| 兼容性 | 前台 | 主流浏览器最新两个版本、iOS Safari 16+；后台：Chrome / Edge / Safari 最新两个版本 |
| SEO | 抓取 | 所有可索引页面无需执行 JS 即可获得主要内容、链接与结构化数据 |
| 可维护性 | 架构 | 模块边界、依赖方向、Design Token 的使用均由 CI 自动检查 |

---

## 2. 架构总览

### 2.1 架构风格与原则

**模块化单体（Modular Monolith）+ 编译期模块 + 运行时启用开关 + 扩展点 + 领域事件。**

- 所有后端模块运行在同一个 Spring Boot 进程中，通过多实例水平扩展。
- 模块在编译期装配（Maven 模块），每个站点可以在运行时启用或停用。
- 模块之间只有三种交互方式：**公开 API（同步调用）**、**领域事件（异步通知）**、**扩展点（能力注册）**。
- 前台网站（Nuxt SSR）与管理后台（Vue SPA）分开部署，都只通过 HTTP API 访问后端。
- 不采用微服务。V1 不引入消息队列；需要时可以通过事件外部化平滑演进（见 4.1）。

八条架构原则（贯穿全文，均配有自动化检查，见 29.3）：

| # | 原则 |
|---|---|
| P1 | Core 不依赖任何业务模块；依赖方向只能自上而下（见 3.3） |
| P2 | 模块只访问自己的表；跨模块数据通过公开 API、事件或扩展点获取 |
| P3 | “A 发生后 B 要做事”用事件；“A 需要 B 的能力”用扩展点或公开 API |
| P4 | 前台只读取发布态数据（快照与投影），永远不读取草稿 |
| P5 | 页面与组件不得硬编码业务字段；组件通过 Schema 声明属性，通过数据源绑定数据 |
| P6 | SEO 由系统自动维护，人工只做覆盖；任何内容变化都不得静默丢失 URL |
| P7 | 前台样式只能来自 Design Token 与主题变体 |
| P8 | 未经 ADR 不得修改核心架构或破坏模块边界 |

### 2.2 系统上下文

```text
        采购商 / 访客              搜索引擎 / AI 爬虫             企业员工（编辑、SEO、销售）
              │                          │                                │
              └────────────┬─────────────┘                                │
                           ▼                                              ▼
                ┌─────────────────────┐                        ┌─────────────────────┐
                │   前台网站（Nuxt）    │                        │  管理后台（Vue SPA）  │
                └──────────┬──────────┘                        └──────────┬──────────┘
                           │ Public / Delivery API                         │ Admin API
                           └──────────────────────┬────────────────────────┘
                                                  ▼
                                 ┌─────────────────────────────────┐
                                 │  B2B Web Platform（模块化单体）   │
                                 └───┬──────────┬──────────┬───────┘
                                     │          │          │
               ┌─────────────────────┘          │          └──────────────────────┐
               ▼                                ▼                                 ▼
   邮件服务（SMTP / API）           Cloudflare Turnstile                CDN 清除 API、IndexNow
   GA4 / GTM / 广告平台（前台直连）   GeoIP 数据库（本地）                Webhook 接收方（企业微信、飞书等）
   AI 翻译引擎（OpenAI 兼容接口）
```

### 2.3 运行时视图

```text
                    ┌──────────────────── CDN / WAF ────────────────────┐
                    │  HTML、静态资源、图片缓存；按 Cache-Tag 清除          │
                    └──────┬──────────────────────┬──────────────┬───────┘
                           │                      │              │
                           ▼                      ▼              ▼
                  ┌────────────────┐     ┌───────────────┐  ┌──────────────┐
                  │ Nuxt SSR × N   │     │ imgproxy × N  │  │ Admin 静态文件 │
                  │（前台网站）      │     │（图片变换）     │  │（SPA）         │
                  └───────┬────────┘     └───────┬───────┘  └──────┬───────┘
                          │ 内网：Delivery API     │ 读原图          │ /api/admin（同源）
                          ▼                       │                 ▼
          ┌──────────────────────────────────────────────────────────────────┐
          │          反向代理（Nginx / Caddy）：TLS 终止、路由、真实 IP           │
          └──────────────────────────────┬───────────────────────────────────┘
                                         ▼
          ┌──────────────────────────────────────────────────────────────────┐
          │        Spring Boot 模块化单体 × N（无状态，会话在 Redis）            │
          └──────┬──────────────┬──────────────┬──────────────┬──────────────┘
                 ▼              ▼              ▼              ▼
             MySQL 8.4       Redis 7       Meilisearch     对象存储（S3 兼容）
           （托管，主从）    （缓存、会话、  （搜索读模型，   （公开桶、私有桶）
                             限流、锁）      可重建）
```

说明：

- 浏览器访问 `www` 域名时由 CDN 回源到 Nuxt；浏览器发起的表单提交、站内搜索请求 `/api/public/*` 经反向代理直达后端。
- Nuxt 服务端渲染时通过内网调用 Delivery API，并携带站点域名与内部令牌（见 26.7）。
- 后台 SPA 与 Admin API 同源部署（`admin` 域名下 `/api/admin` 反向代理到后端），避免 CORS，Cookie 可以使用 SameSite。
- 原 V1.0 中的“API Gateway”在本设计中由反向代理承担，不引入独立网关产品。
- 部署区域：CDN 以下的全部组件（反向代理、后端、Nuxt、imgproxy、MySQL、Redis、Meilisearch、对象存储）及备份都位于欧盟区域（默认法兰克福）；全球 CDN 覆盖欧美访客（见 ADR-015、27.1）。

### 2.4 技术选型

| 层 | 选型 | 说明 |
|---|---|---|
| 后端运行时 | JDK 25 LTS（最低 21） | 使用虚拟线程处理 I/O 密集任务（数据源并行解析、通知发送） |
| 后端框架 | Spring Boot 4.x + Spring Modulith 2.x | Modulith 负责模块边界验证、事件发布登记（Outbox）、模块集成测试 |
| 持久层 | MyBatis-Plus | SQL 可控，团队熟悉；行级拦截器（`TenantLineInnerInterceptor`，列名配置为 `site_id`）用于站点过滤（见 25.7） |
| 数据库 | MySQL 8.4 LTS | JSON 列保存快照、布局文档与设置；生产使用欧盟区域的托管服务 |
| 数据库迁移 | Flyway（每个模块独立的 history 表） | 由模块管理器按依赖顺序执行（见 3.6） |
| 缓存 / 会话 / 限流 | Redis 7（或兼容的 Valkey）+ Spring Session + Bucket4j | |
| 搜索 | Meilisearch | 多语言、分面筛选、容错；作为可重建的读模型 |
| 对象存储 | S3 兼容 API（AWS S3、Cloudflare R2、阿里云 OSS、MinIO） | 生产存储桶与备份位于欧盟区域；本地开发使用 MinIO |
| 图片处理 | imgproxy（签名 URL）+ CDN | |
| 任务 | Spring Scheduling + ShedLock；Core 提供持久化异步任务框架 | |
| 安全 | Spring Security；Argon2id 密码哈希；TOTP 2FA | |
| API 文档 | springdoc-openapi → openapi-typescript | 前端类型自动生成 |
| HTML 清洗 / 文件识别 | jsoup Safelist；Apache Tika | |
| 前台网站 | Nuxt 4 + Vue 3.5 + TypeScript；@nuxt/image（自定义 imgproxy provider） | |
| 管理后台 | Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router | 界面只提供简体中文，不引入 vue-i18n；界面文案集中放在常量 / 字典文件中，便于以后扩展 |
| 富文本 | Tiptap（以 ProseMirror JSON 存储） | 不存原始 HTML |
| Design Token | DTCG 格式 JSON + Style Dictionary | 生成 CSS 变量与 TS 常量 |
| 前端工程 | pnpm workspace；ESLint、Stylelint、Vitest、Playwright | |
| 人机验证 | Cloudflare Turnstile | |
| 邮件 | SMTP / API（兼容 SES 欧盟区域、Mailgun EU、Brevo 等服务）；开发环境用 Mailpit | 优先选择提供欧盟数据区域的服务商 |
| AI 翻译 | OpenAI 兼容的 Chat Completions 接口（可配置 base URL、模型、密钥） | 后端用 Spring HTTP 客户端直接调用，不绑定厂商 SDK；可接 OpenAI、DeepSeek、通义千问、Kimi、自部署 vLLM / Ollama 等 |
| 可观测性 | Micrometer + Prometheus + Grafana；OpenTelemetry；Loki；Sentry（欧盟数据区域） | 自建组件部署在欧盟区域；第三方托管服务选择欧盟数据区域 |
| 部署 | Docker；V1 生产使用 Docker Compose（欧盟单区域、多实例，见 27.1），V2 可迁移到 Kubernetes | 单机 Docker Compose 只用于本地开发与演示 |
| CDN | 支持按 Cache-Tag / Surrogate-Key 清除的 CDN（如 Cloudflare、Fastly） | 全球节点覆盖欧美访客（见 ADR-015） |
| CI/CD | GitHub Actions | |

> 版本说明：以上为选型方向。M0 阶段需要对 Spring Boot、Spring Modulith、MyBatis-Plus、Flyway、Nuxt 的具体版本做一次兼容性验证，锁定组合后写入 ADR-011。

### 2.5 关键架构决策（ADR 摘要）

完整列表与文件名见附录 B。

| ADR | 主题 | 决策 |
|---|---|---|
| 000 | 自研还是基于现有 Headless CMS | **自研（已确认，Accepted）**。SEO 引擎、内容图谱、模块扩展、Java 技术栈统一是核心竞争力，现有 Headless CMS 难以深度定制且技术栈不一致。代价是内容内核与页面编辑器工作量大，靠严格的 V1 范围控制 |
| 001 | 模块形态 | 编译期模块 + 站点级运行时启用开关；不做热插拔 |
| 002 | 模块通信 | 只允许公开 API、领域事件、扩展点三种方式；所有跨模块契约放在 `platform-api` |
| 003 | 事件可靠性 | 事务提交后投递 + Spring Modulith Event Publication Registry（数据库 Outbox）；V1 不引入 MQ |
| 004 | 内容模型 | Content + Localization + Revision；发布时生成不可变快照与只读投影 |
| 005 | 语言策略与 URL | **已确认（Accepted）**。后台界面只用中文；简体中文为源语言；前台先开放中文，其他语言由 AI 翻译生成后按语言开放；全部语言 URL 带前缀，根路径 302 到默认对外语言；内容不跨语言回退 |
| 006 | 渲染方式 | 系统内容使用“模板 + 插槽”，营销页使用页面编辑器；前台只读快照 |
| 007 | 编辑器画布 | Admin 中通过 iframe 嵌入 Nuxt 预览路由，用 postMessage 通信；组件 Schema 放在共享包中 |
| 008 | SEO 数据归属 | SEO 模块拥有 SEO 数据；自动值与覆盖值分离；发布时写入快照 |
| 009 | 站点与语言 | **已确认（Accepted）**。单企业、多站点，不设租户层（Site → Locale）；站点级表带 site_id，由拦截器自动追加过滤条件；未来多企业采用一企业一实例 |
| 010 | 认证 | 后台使用 Session + HttpOnly Cookie + CSRF + TOTP；前台没有登录 |
| 011 | 技术版本 | M0 验证兼容组合后锁定 |
| 012 | 缓存 | CDN + 后端 Redis 两级，按 Cache Tag 精确失效；Nuxt 层不缓存 HTML |
| 013 | 搜索 | Meilisearch 作为发布态读模型，负责搜索与分面；可以从数据库全量重建 |
| 014 | ID | 64 位 TSID（时间有序），在 JSON 中序列化为字符串 |
| 015 | 部署区域与数据驻留 | 源站与数据（数据库、对象存储、备份）在欧盟（默认法兰克福），全球 CDN 覆盖欧美访客；第三方服务优先选择欧盟数据区域 |
| 016 | 隐私合规基线 | GDPR / UK GDPR / ePrivacy（英国为 PECR）+ CCPA/CPRA：非必要 Cookie 事先同意并保存同意记录；识别 GPC；询盘的合法性基础（订立合同前的步骤或正当利益）由法务确认，不强制勾选同意，营销同意单独勾选；数据主体请求按法定时限处理 |
| 017 | AI 翻译与翻译记忆 | **已确认（Accepted）**。默认 OpenAI 兼容接口；段级翻译记忆，同一原文只翻译一次；人工修订优先；法律页面人工审校 |

---

## 3. 模块化设计

### 3.1 分层

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ application         启动装配、全局配置、部署 Profile                            │
├──────────────────────────────────────────────────────────────────────────────┤
│ L4 聚合与横切模块    delivery · seo · graph · search · tracking                │
├──────────────────────────────────────────────────────────────────────────────┤
│ L3 业务模块          product · article · page · inquiry · navigation           │
├──────────────────────────────────────────────────────────────────────────────┤
│ L2 基础能力模块      content · url · media · taxonomy · form · notification ·  │
│                      translation                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│ L1 platform-api      跨模块契约：服务接口、扩展点接口、事件、共享值对象            │
├──────────────────────────────────────────────────────────────────────────────┤
│ L0 platform-core     站点、身份、权限、模块管理、设置、审计、事件基础、任务、       │
│                      字典、界面文案、站点资料                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│ infrastructure       技术适配器：对象存储、邮件传输、搜索客户端、缓存、CDN 清除     │
│                      AI 翻译引擎；（实现 L0 / L1 中定义的端口接口）               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 模块清单

| 模块 | 层 | 职责 | 表前缀 | 可停用 | 范围 |
|---|---|---|---|---|---|
| platform-core | L0 | 站点、语言、用户、角色、权限、数据范围、模块注册与生命周期、设置、站点资料、审计、数据主体请求、任务、字典、界面文案 | `sys_` | 否 | V1 |
| platform-api | L1 | 跨模块契约（无实现） | — | — | V1 |
| content | L2 | 内容内核：类型注册、本地化、修订、发布状态机、快照、投影、预览、回收站、审核 | `cnt_` | 否 | V1 |
| url | L2 | 路由表、slug、URL 规则、重定向、规范化、404 日志 | `url_` | 否 | V1 |
| media | L2 | 媒体资产、文件夹、图片变换、引用追踪、存储适配 | `mda_` | 否 | V1 |
| taxonomy | L2 | 词汇表、分类项、内容分类指派、分类落地页 | `tx_` | 否 | V1 |
| form | L2 | 表单定义、提交、反垃圾、附件 | `frm_` | 是 | V1 |
| notification | L2 | 通知模板、发送、重试、Webhook | `ntf_` | 否 | V1 |
| translation | L2 | AI 翻译：翻译单元提取与回写的编排、翻译记忆、术语表、风格指南、引擎适配、翻译任务（实时与批量）、用量与成本统计、翻译中心（后台）；停用后不再自动翻译，已生成的译文保留 | `trn_` | 是 | V1 |
| product | L3 | 产品、属性体系、型号表、产品文档、导入导出 | `prd_` | 是 | V1 |
| article | L3 | 文章（新闻、博客）、案例、FAQ、作者 | `art_` | 是 | V1 |
| page | L3 | 页面、模板与组件元数据、布局文档、全局区块 | `pg_` | 否 | V1 |
| inquiry | L3 | 询盘/线索、分配规则、跟进、询价篮、报表 | `inq_` | 是 | V1 |
| navigation | L3 | 菜单、面包屑 | `nav_` | 否 | V1 |
| delivery | L4 | 前台交付聚合：解析路由、组装渲染数据、解析数据源、生成缓存标签 | 无表 | 否 | V1 |
| seo | L4 | SEO 元数据、生成规则、Schema.org、Sitemap、robots、链接索引、健康检查、IndexNow | `seo_` | 否 | V1 |
| graph | L4 | 内容关系、关系类型、评分计算、相关推荐 | `grh_` | 否 | V1 |
| search | L4 | 搜索索引、站内搜索、分面筛选 | 索引在 Meilisearch | 是 | V1 |
| tracking | L4 | 统计代码、同意管理（含同意记录、GPC）、转化事件配置 | `trk_` | 是 | V1 |
| crm、quotation、whatsapp、newsletter、ai、analytics、portal | L3/L4 | 见 1.4 | 各自前缀 | 是 | V2+ |

### 3.3 依赖规则

```text
                               application
                                    │ 装配全部模块
     ┌──────────────────────────────┼──────────────────────────────┐
     │  L4  delivery   seo   graph   search   tracking               │
     │  L3  product   article   page   inquiry   navigation          │
     │  L2  content   url   media   taxonomy   form   notification   │
     │      translation                                              │
     └──────────────────────────────┬──────────────────────────────┘
                                    │ 编译依赖：只允许依赖下面两层
                                    ▼
                     L1 platform-api（契约） ──▶ L0 platform-core
```

| 规则 | 内容 |
|---|---|
| R1 编译依赖 | 任何模块只能依赖 `platform-core`、`platform-api` 以及 infrastructure 定义的端口接口；**不得依赖任何其他模块的 Maven 构件，也不得 import 其他模块的包** |
| R2 调用方向 | L3 / L4 可以调用 L2 与 L0 的服务接口；L2 不得调用 L3 / L4（只能通过扩展点回调）；同为 L3 或同为 L4 的模块之间不直接调用，只通过事件或扩展点协作 |
| R3 契约位置 | 跨模块的服务接口、扩展点、事件、DTO 只存在于 `platform-api`，变更需要 CODEOWNERS 中的架构负责人审核 |
| R4 运行时依赖 | 某模块工作所需的其他模块在 `module.json` 的 `requires` 中声明，启动时校验；禁止循环依赖 |
| R5 数据访问 | 模块只访问自己前缀的表；不允许跨模块外键与跨模块 JOIN |

由于所有跨模块契约都是 `platform-api` 中的接口，模块之间在编译期互相不可见，V1.0 中“Product 依赖 SEO”的问题从结构上被消除：Product 实现 `SchemaOrgProvider` 等扩展点，SEO 模块收集这些实现，两者互不知道对方存在。

### 3.4 模块内部结构

```text
backend/modules/product/
├── pom.xml                                  # 只依赖 platform-core、platform-api
└── src/main/
    ├── java/com/example/b2b/product/        # 包名待定
    │   ├── ProductModule.java               # PlatformModule 实现
    │   ├── domain/                          # 实体、值对象、领域服务（不依赖框架）
    │   ├── application/                     # 应用服务：事务边界、权限校验、发布事件
    │   ├── infrastructure/persistence/      # MyBatis Mapper、持久化对象
    │   ├── web/admin/                       # /api/admin/v1/product/**
    │   ├── web/publicapi/                   # /api/public/v1/product/**（如有）
    │   └── extension/                       # 扩展点实现
    └── resources/
        ├── module.json                      # 模块描述符
        ├── db/migration/product/            # Flyway 迁移脚本
        └── messages.properties              # 后台错误信息等（只有中文）
```

一个模块是一个**纵向切片**，横跨四个位置：后端 Maven 模块、Admin 中的 `modules/<id>/`、前台主题中的组件、共享 Schema 包中的组件与模板定义。四者使用同一个模块 ID。

### 3.5 模块描述符

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
    { "code": "inquiry:inquiry:view",   "dataScopes": ["ALL", "GROUP", "OWN"] },
    { "code": "inquiry:inquiry:assign" },
    { "code": "inquiry:inquiry:export", "sensitive": true },
    { "code": "inquiry:rule:manage" }
  ],
  "menus": [
    { "key": "inquiry", "parent": "sales", "route": "/inquiry/inquiries", "permission": "inquiry:inquiry:view", "order": 10 },
    { "key": "inquiry-rules", "parent": "settings", "route": "/inquiry/rules", "permission": "inquiry:rule:manage" }
  ],
  "settings": "settings.schema.json",
  "components": ["inquiry-form", "add-to-inquiry-button", "inquiry-cart"],
  "events": {
    "publishes": ["InquiryCreatedEvent", "InquiryAssignedEvent", "InquiryStatusChangedEvent"],
    "subscribes": ["FormSubmittedEvent"]
  }
}
```

- `requires` / `optional` 使用语义化版本范围；`optional` 表示存在时增强功能，不存在时降级。例如 product 启用时，询盘明细可以精确到型号（通过 `ContentVariantProvider` 扩展点获取型号信息，而不是依赖 product 模块的代码）；product 停用时，明细只记录内容。
- `name` 只写中文字符串（后台界面只用简体中文，见 2.4），不使用按语言区分的对象。
- 权限、菜单、设置 Schema、组件清单采用声明式注册，启动时由平台幂等同步（见 3.6）。
- `events` 用于文档生成与依赖分析，CI 会检查它与代码中实际发布、订阅的事件一致。

### 3.6 模块生命周期

**平台级状态**（与代码版本相关）：

```text
（构建中出现新模块）──▶ INSTALLING ──成功──▶ INSTALLED(vX)
                              │                  │ 代码版本升高
                              └─失败─▶ FAILED      ▼
                                              UPGRADING ──成功──▶ INSTALLED(vY)
                                                   └─失败─▶ FAILED（阻止启动，保留现场）
（构建中移除了模块）──▶ MISSING：数据保留，相关引用降级处理
（系统管理员显式执行，且所有站点均已停用）──▶ PURGED：删除模块数据（需二次确认 + 先备份）
```

**站点级状态**：`ENABLED` / `DISABLED`，保存在 `sys_site_module`。核心模块（`disableable = false`）不能停用。

**启动流程**：

```text
1. 扫描 classpath 中的 module.json 与 PlatformModule Bean
2. 校验：ID 唯一、platform 版本范围、requires 版本范围、无循环依赖（拓扑排序）
3. 获取数据库锁（sys_lock）
4. 按拓扑顺序执行各模块的 Flyway 迁移（history 表：flyway_<module>_history）
5. 与 sys_module 中记录的版本比较：新模块执行 onInstall，版本升高执行 onUpgrade
6. 幂等同步声明式资源：权限、菜单、设置 Schema、内容类型、关系类型、通知模板、组件与模板元数据
7. 构建扩展点注册表
8. 释放锁；readiness 变为 UP，开始接收流量
```

> 生产环境的迁移由独立的 migrate 任务执行（同一镜像，以 `--mode=migrate` 启动），在滚动更新之前完成；应用启动时的第 3–4 步只作为兜底。迁移脚本必须遵循 expand / contract 模式，保证新旧版本应用可以同时运行（见 25.6）。

**SPI 接口**：

```java
public interface PlatformModule {

    /** 读取 module.json 得到的描述 */
    ModuleDescriptor descriptor();

    /** 首次安装：迁移已由平台执行，此处初始化默认数据（如默认表单、通知模板） */
    default void onInstall(ModuleContext ctx) {}

    /** 版本升级：执行数据修正（结构迁移由 Flyway 完成） */
    default void onUpgrade(ModuleContext ctx, SemVer fromVersion) {}

    /** 站点启用 / 停用 */
    default void onEnable(SiteModuleContext ctx) {}
    default void onDisable(SiteModuleContext ctx) {}

    /** 停用前的影响分析，例如“该站点有 120 篇已发布文章将不可访问” */
    default ImpactReport analyzeDisableImpact(SiteModuleContext ctx) { return ImpactReport.none(); }

    /** 彻底删除模块数据（仅 CLI / 系统管理员，需二次确认） */
    default void onPurge(ModuleContext ctx) {}
}
```

**V1.0 中“卸载模块”与“删除模块数据”的区分**在本设计中对应为：站点停用（数据保留）、从构建中移除（MISSING，数据保留）、PURGED（显式删除数据）。

### 3.7 扩展点

扩展点是 `platform-api` 中的接口。模块用 `@Extension` 注解声明实现，平台的 `ExtensionRegistry` 负责收集，并**按当前站点的模块启用状态过滤**。

```java
@Extension(module = "product", contentTypes = {"PRODUCT"}, order = 100)
public class ProductSchemaOrgProvider implements SchemaOrgProvider { ... }

// 调用方
List<SchemaOrgProvider> providers = extensions.forContentType(SchemaOrgProvider.class, "PRODUCT");
```

V1 扩展点清单：

| 扩展点 | 用途 | V1 实现方 | 调用方 |
|---|---|---|---|
| `ContentTypeProvider` | 注册内容类型（是否可路由、默认 URL 规则、可用模板、适用词汇表等） | page、product、article、taxonomy | content、url、Admin |
| `PublishContributor` | 发布前校验；向快照贡献本模块的数据；回滚时恢复草稿 | product、article、page、taxonomy、seo | content |
| `ContentImpactProvider` | 下线、删除、停用前的影响分析（被哪些页面引用、导航是否指向它等） | seo、navigation、graph | content、core |
| `TemplateProvider` | 提供模板定义（区域、插槽、固定区域的数据源） | page | delivery、Admin |
| `DataSourceProvider` | 组件数据绑定（按条件查询内容、相关内容、子分类等） | content、graph、taxonomy、article、search | delivery |
| `DeliveryContributor` | 向前台交付结果贡献一部分数据（如 SEO 头信息） | seo | delivery |
| `SiteBootstrapContributor` | 向前台站点引导数据贡献内容（导航、追踪配置等） | navigation、tracking、core | delivery |
| `BreadcrumbProvider` | 生成面包屑路径 | page、product、article、taxonomy | delivery、seo |
| `SchemaOrgProvider` | 生成 JSON-LD | product、article、page、core | seo |
| `SeoVariableProvider` | 提供 SEO 模板变量（如 `{modelNo}`） | product、article、taxonomy | seo |
| `SeoRule` | SEO 健康检查规则 | seo、product | seo |
| `SitemapProvider` | 非内容类 URL 进入 sitemap | （预留） | seo |
| `SearchDocumentProvider` | 构建搜索文档（字段、分面） | product、article、page | search |
| `RelationTypeProvider` | 注册关系类型 | graph、product | graph |
| `MediaReferenceCollector` | 收集内容引用的媒体，用于引用追踪 | product、article、page、seo、core | media |
| `ContentVariantProvider` | 提供内容下可选择的子项（如产品型号），用于询价篮与询盘明细 | product | inquiry |
| `NotificationTemplateProvider` | 注册默认通知模板 | inquiry、content | notification |
| `PersonalDataProvider` | 数据主体请求：按邮箱查找、导出、更正、匿名化本模块保存的个人数据 | inquiry、form、notification | core（数据主体请求工具） |
| `TranslatableContentProvider` | 从源语言草稿提取翻译单元；把译文写回目标语言草稿（非文本数据从源语言复制） | product、article、page、taxonomy、seo | translation |
| `TranslatableResourceProvider` | 提取与回写站点资源中的可翻译文本 | navigation、form、media、product（属性与选项）、taxonomy（分类名）、notification（客户邮件模板）、core（站点资料、界面文案）、tracking（Cookie 横幅文案） | translation |

翻译相关扩展点示意（翻译流程见 5.6、5.7）：

```java
public interface TranslatableContentProvider {
    boolean supports(String contentType);
    List<TranslationUnit> extract(TranslationContext ctx);                // 从源语言草稿提取翻译单元
    void apply(TranslationContext ctx, Map<String, String> translations); // key → 译文；写回目标语言草稿，非文本数据从源语言复制
}

public interface TranslatableResourceProvider {
    boolean supports(String resourceType);                                // 如菜单、表单、媒体、属性、邮件模板
    List<TranslationUnit> extract(TranslationContext ctx);                // 从站点资源提取可翻译文本
    void apply(TranslationContext ctx, Map<String, String> translations); // 写回目标语言的资源文本
}

public record TranslationUnit(
        String  key,        // 字段路径，如 "title"、"seo.title"、"body[3]"
        String  text,       // 原文；富文本行内标记已转为编号占位标签
        String  format,     // TEXT / RICH_INLINE
        Integer maxLength,  // 可选的长度上限，如 SEO 标题 60 个字符
        String  context     // 上下文说明，如“产品名称”“按钮文字”
) {}
```

`TranslationContext` 携带站点、内容或资源 ID、源语言与目标语言。

### 3.8 模块停用时的行为

| 对象 | 停用后的行为 |
|---|---|
| 后台菜单与路由 | 隐藏；直接访问返回 404 |
| Admin / Public API | `ModuleGuardFilter` 按 `apiPrefix` 返回 404 |
| 事件监听 | 跳过执行（监听器包装器检查站点启用状态）；事件本身照常记录 |
| 扩展点 | 注册表不再返回该模块的实现 |
| 前台组件 | 编辑器组件面板中隐藏；已放置的实例在前台不渲染，在编辑器中显示“模块已停用”占位 |
| 该模块的内容类型 | 停用前展示影响报告并要求确认；停用后对应路由返回 404（可选：批量 301 到指定页面）；sitemap、搜索、图谱自动排除 |
| 定时任务 | 不执行 |
| 数据 | 全部保留；重新启用后恢复 |

---

## 4. 领域事件

### 4.1 机制

| 方面 | 设计 |
|---|---|
| 发布 | 应用服务在业务事务内调用 `ApplicationEventPublisher.publishEvent(event)` |
| 投递 | 监听器使用 Spring Modulith 的 `@ApplicationModuleListener`：事务提交后异步执行，并在新事务中运行 |
| 可靠性 | Event Publication Registry 在发布事务中把事件写入 `event_publication` 表；监听器成功后标记完成；未完成的事件在启动时与定时任务中重新投递（至少一次） |
| 幂等 | 所有监听器必须幂等：按 `eventId` 去重，或采用天然幂等的 upsert |
| 顺序 | 不保证全局顺序；同一内容的事件携带 `revisionNo` 与 `occurredAt`，监听器丢弃过期事件 |
| 外部化 | V1 不引入 MQ；V2 需要时通过 Modulith 事件外部化到 Kafka / RabbitMQ，业务代码不变 |
| 监控 | 未完成事件的数量与最早时间纳入告警（见 27.4） |

### 4.2 事件规范

```java
public interface DomainEvent {
    String  eventId();        // TSID
    int     schemaVersion();  // 事件结构版本
    Instant occurredAt();
    Long    siteId();         // 可空：系统级事件为空
    String  actor();          // 用户 ID，或 "system"
}

public record ContentPublishedEvent(
        String eventId, int schemaVersion, Instant occurredAt,
        Long siteId, String actor,
        long contentId, String contentType, String locale,
        long localizationId, long revisionId, int revisionNo,
        String path, String previousPath, boolean firstPublish, boolean contentChanged
) implements DomainEvent {}
```

- 命名：`<聚合><过去分词>Event`，如 `ContentPublishedEvent`、`InquiryAssignedEvent`。
- 事件只携带 ID 与必要的事实，不携带完整实体；订阅方需要更多数据时调用对应的服务接口。
- 事件是契约：只允许兼容地增加字段；不兼容的变更必须提升 `schemaVersion`，并在过渡期内同时支持新旧版本。
- V1.0 中的 `ProductPublishedEvent`、`PagePublishedEvent`、`ArticlePublishedEvent` 统一为 `ContentPublishedEvent(contentType = …)`，避免订阅方依赖具体的业务模块。

### 4.3 V1 事件目录

| 事件 | 发布者 | 主要订阅者 |
|---|---|---|
| `ContentCreatedEvent` | content | graph |
| `ContentPublishedEvent` | content | seo（sitemap、IndexNow、链接索引、健康检查）、graph（增量计算）、search（索引）、media（引用）、delivery（缓存失效）、translation（源语言发布时创建翻译任务，见 5.7） |
| `ContentUnpublishedEvent` | content | seo、graph、search、media、delivery |
| `ContentTrashedEvent` / `ContentRestoredEvent` | content | graph、search、delivery |
| `ContentPurgedEvent` | content | 所有持有该内容数据的模块（清理各自的表）、graph、media |
| `LocalizationCreatedEvent` / `LocalizationDeletedEvent` | content | seo（hreflang）、delivery |
| `TranslationOutdatedEvent` | content | notification（MANUAL 模式的本地化：通知翻译审校人员） |
| `TranslatableResourceChangedEvent`（资源类型 + ID） | 各模块：navigation、form、media、product、taxonomy、notification、core、tracking | translation（翻译站点资源，见 5.7） |
| `TranslationCompletedEvent` | translation | content（自动发布）、delivery |
| `TranslationFailedEvent` | translation | notification |
| `ReviewRequestedEvent` / `ReviewCompletedEvent` | content | notification |
| `ContentPathChangedEvent` | url | seo（IndexNow 推送新旧 URL）、delivery |
| `RedirectChangedEvent` | url | delivery |
| `TermChangedEvent`（新建、更名、移动、删除） | taxonomy | graph、search、delivery |
| `MediaUploadedEvent` / `MediaUpdatedEvent` / `MediaDeletedEvent` | media | delivery、seo |
| `NavigationChangedEvent` | navigation | delivery |
| `SiteSettingsChangedEvent` | core | delivery、seo |
| `RelationChangedEvent` | graph | delivery |
| `FormSubmittedEvent` | form | inquiry |
| `InquiryCreatedEvent` / `InquiryAssignedEvent` / `InquiryStatusChangedEvent` | inquiry | V2：crm、analytics |
| `ModuleEnabledEvent` / `ModuleDisabledEvent` | core | delivery、seo、search、graph |

### 4.4 一致性边界：以“发布”为例

```text
编辑点击“发布”
   │
   ▼
content.publish(localizationId, expectedVersion)        ──── 同一个数据库事务 ────┐
   1. 校验状态、权限、乐观锁                                                    │
   2. url.reservePath()：校验路径唯一                                           │
   3. 依次调用 PublishContributor.validate()，任一 ERROR 则中止                  │
   4. 依次调用 PublishContributor.contribute()，组装快照                         │
   5. 写入 cnt_revision（快照），更新 cnt_localization 与 cnt_published_term     │
   6. url.applyRoute()：更新路由；路径变化时自动生成 301                          │
   7. 发布 ContentPublishedEvent（写入 event_publication）                      │
提交事务 ─────────────────────────────────────────────────────────────────────┘
   │ 事务提交后，异步、各自独立重试
   ├─ delivery：按 Cache Tag 清除 Redis 与 CDN
   ├─ seo：更新 sitemap 缓存、推送 IndexNow、重建链接索引、执行健康检查
   ├─ graph：增量重算关系
   ├─ search：更新索引
   ├─ media：更新引用记录
   └─ translation：源语言发布时，为启用自动翻译的目标语言创建翻译任务（见 5.7）
```

- **强一致**：内容状态、快照、投影、路由与重定向在同一个事务中完成，保证“发布成功即可被路由”。
- **最终一致**：缓存、sitemap、图谱、搜索、引用在 60 秒内完成（见 1.5）；目标语言的译文在 5 分钟内生成（不含人工审校，见 1.5、5.7），AI 引擎故障不影响源语言的发布。

---

## 5. 站点与语言

### 5.1 模型

系统自用：一次部署 = 一家企业，不设租户层。**站点（Site）是最高隔离单位**：企业可以有多个品牌站或国家站（如 `brand-a.com`、`brand-b.de`），每个站点可以有多种语言。

```text
User（用户：全局唯一，不属于某个站点）
Site（站点：主域名、主题、默认对外语言、URL 策略、时区）
 ├── SiteDomain（主域名 / 别名域名 / V2：语言独立域名）
 ├── SiteLocale（站点语言：URL 前缀、hreflang 值、是否对外公开、是否自动翻译、排序）
 ├── SiteModule（站点启用的模块）
 ├── SiteProfile（站点资料：公司信息、Logo、联系方式，见 16.3）
 └── UserSiteRole（用户在该站点的角色）
```

| 表 | 关键字段 |
|---|---|
| `sys_site` | code、name、theme_key、default_locale（默认对外语言）、url_strategy、trailing_slash、timezone、status |
| `sys_site_domain` | site_id、domain、type（PRIMARY / ALIAS / LOCALE）、locale |
| `sys_site_locale` | site_id、locale、url_prefix、hreflang、public（对外公开）、auto_translate（自动翻译）、sort |
| `sys_site_module` | site_id、module_id、enabled、config（JSON） |
| `sys_user_site_role` | user_id、site_id、role_id |

- **站点语言**：源语言固定为简体中文 `zh-CN`（见 5.4）。
  - `public`（对外公开）：该语言是否在前台可访问、是否出现在语言切换器、hreflang 与 sitemap 中；未对外公开的语言只能在后台与预览中查看。V1 默认只有 `zh-CN` 对外公开。
  - `auto_translate`（自动翻译）：源语言内容发布、站点资源变化时，是否自动翻译到该语言（见 5.7）；对源语言本身不适用。
  - `default_locale`（默认对外语言）：根路径跳转与 `x-default` 的目标（见 5.4），必须是对外公开的语言。
  - `url_strategy`：V1 固定为子目录（全部语言带前缀，见 5.4）；V2 可选语言独立域名。
- 用户全局唯一；角色按站点分配（`sys_user_site_role`）。
- **系统管理员**管理站点、模块与系统设置，拥有所有站点的权限。

### 5.2 请求上下文解析

| 场景 | 解析方式 |
|---|---|
| 前台（Nuxt → Delivery） | Nuxt 把请求的 Host 放入 `X-Site-Host`，连同内部令牌发送给后端；`SiteResolver` 根据域名找到站点（结果缓存）；路径前缀决定语言；访问别名域名或 http 时 301 到主域名 https |
| 前台（浏览器 → Public API） | 反向代理透传 Host 与真实 IP；后端用同样的方式解析站点 |
| 后台 | 用户登录后在界面上选择当前站点，请求头带 `X-Site-Id`；后端校验用户在该站点有角色 |
| 异步任务与事件 | 事件与任务显式携带 siteId（系统级为空）；执行器在运行前设置上下文 |

上下文保存在 `RequestContext` 中（ScopedValue / ThreadLocal），通过 `TaskDecorator` 传递给异步线程。

### 5.3 站点隔离

| 资源 | 隔离方式 |
|---|---|
| MySQL | 站点级表带 site_id；MyBatis-Plus 行级拦截器（`TenantLineInnerInterceptor`，列名配置为 `site_id`）自动追加 `site_id` 条件；全局表（`sys_user`、`sys_user_mfa`、`sys_role`、`sys_permission`、`sys_module`、系统级设置等）没有 site_id，列入豁免清单（见 25.7）；`sys_audit_log` 带可空的 site_id；跨站点查询只允许系统管理接口 |
| Redis | Key 前缀 `s{siteId}:…`；系统级数据 `sys:…` |
| 对象存储 | 路径 `{siteId}/{yyyy}/{mm}/{id}-{slug化的原文件名}.{ext}`（见 12.2）；公开桶与私有桶分离 |
| Meilisearch | 每个站点的每种语言一个索引：`s{site}_{locale}_content` |
| CDN Cache Tag | 标签带站点前缀 `s{siteId}:…` |
| 日志、指标与链路追踪 | 带 site 维度 |
| 后台会话 | 会话绑定用户；切换站点需要校验用户在该站点有角色 |

CI 中有专门的站点隔离测试：准备两个站点的数据，以及只在其中一个站点拥有角色的用户，遍历所有后台接口，验证不能越权读写另一个站点的数据（见 28）。

未来如果需要为其他企业提供本系统，采用“一企业一实例”独立部署（独立数据库与存储），不在一套部署中共享多家企业的数据（见 ADR-009）。

### 5.4 语言与 URL 策略

- **源语言**（编辑录入语言）：简体中文，语言代码 `zh-CN`。所有内容、站点资源只需用中文录入；`cnt_content.primary_locale` 即源语言。管理后台界面只提供简体中文，与站点的内容语言无关（见 22.5）。
- **对外语言**：前台 V1 先只开放中文（`zh-CN` 对外公开）。其他语言（建议首批 **en、de、fr、es、it**；按需扩展 nl、pl、pt）由 AI 翻译生成（见 5.5–5.10）；站点管理员在站点语言设置中把某语言设为“对外公开”即可上线，不需要改代码（见 32 章 Q2）。目标市场仍是欧盟、英国与北美（见 0.4 A2）。
- **开放新语言**：在站点语言设置中添加该语言并启用自动翻译 → translation 创建批量翻译任务，翻译全部已发布内容与站点资源（菜单、表单、界面文案、属性名等，见 5.7）→ 设为“对外公开”。建议在批量任务完成后再对外公开；对外公开后，该语言已发布的页面、hreflang、sitemap 与语言切换器自动生效（见 1.3 S11）。
- **URL 全部语言都带前缀**（子目录策略）：`/zh/…`、`/en/…`、`/de/…`；前缀取自站点语言的 `url_prefix`，内容类型的 URL 规则本身不含语言前缀（见 6.2）。V2 支持每种语言使用独立域名（`example.de`）。
- **根路径**：`/` 以 **302** 跳转到站点“默认对外语言”（`sys_site.default_locale`）的首页；固定跳转，**不按 Accept-Language**（对爬虫不友好），可以按浏览器语言显示语言建议条，但不自动跳转。使用 302 而不是 301，是因为默认对外语言以后可以调整。这样以后开放英语，或把默认对外语言从中文改为英语时，所有内容 URL 都不变。
- **语言代码与 hreflang**：语言代码使用 BCP 47（`zh-CN`、`en`、`de`、`pt-BR`）；hreflang 值可以单独配置，**默认只用语言代码**：中文为 `zh`（可配置为 `zh-Hans`），英语 `en`、德语 `de`、法语 `fr`…；英语只做一个版本，同时服务美国与英国，以后需要区分 `en-US` / `en-GB` 时再增加区域变体。只有已发布且“对外公开”的语言输出 hreflang（见 14.4）。
- **x-default**：首页指向 `/`（会跳转到默认对外语言，符合 Google 对“自动跳转首页”的建议）；其他页面指向默认对外语言的版本（若已发布）。
- **内容不跨语言回退**：某内容没有 `de` 本地化（包括译文尚未生成或尚未发布）时，`de` 下就不存在该页面，列表与推荐中也不出现。
- **界面文案**（按钮、表单标签等）：主题以文案键提供默认中文文案，站点可以覆盖；查找顺序为站点覆盖 → 主题默认。二者都以中文为源，作为站点资源翻译到各语言（见 5.5、21.7）；译文翻译一次并存储，前台渲染时不调用 AI。
- **slug、字体与搜索**：中文 slug 默认用拼音音译，其他语言的 slug 由译文标题生成、首次发布后固定（见 13.3）；中文使用系统字体栈（见 11.6）；中文索引使用 Meilisearch 内置的中文分词（见 19）。
- RTL 语言不在 V1 范围；前台样式仍全部使用 CSS 逻辑属性（`margin-inline-start` 等），成本低，保留扩展性；`<html dir>` 由语言配置决定。

### 5.5 AI 翻译概述

AI 翻译由 `translation` 模块实现：L2 基础能力模块，表前缀 `trn_`，可停用（停用后不再自动翻译，已生成的译文保留），V1 范围（见 3.2）。职责：翻译单元提取与回写的编排、翻译记忆、术语表、风格指南、引擎适配、翻译任务（实时与批量）、用量与成本统计、翻译中心（后台）。

- **目标**：中文录入一次，其他语言由 AI 生成；**一次翻译、持久存储、相同原文不再翻译**；人工修订优先于机器翻译。
- **翻译对象**：

| 类别 | 范围 | 提取与回写 |
|---|---|---|
| 内容 | 产品、文章、案例、FAQ、页面、分类落地页、全局区块 | `TranslatableContentProvider`（见 3.7），源语言发布时触发 |
| 站点资源 | 菜单、表单、界面文案、产品属性名与选项、分类名、媒体 alt / 标题、通知邮件模板、站点资料中的文本、Cookie 横幅文案 | `TranslatableResourceProvider`（见 3.7），资源保存时触发 |

- **不翻译**：非文本数据（图片、数值、型号、分类指派、关联关系、布局结构），目标语言直接沿用源语言。
- **翻译模式**（每个本地化一个，`cnt_localization.translation_mode`，见 6.1）：

| 模式 | 说明 |
|---|---|
| `SOURCE` | 源语言本地化本身 |
| `AUTO`（目标语言默认） | 由源语言 + 翻译记忆生成；布局结构与源语言自动保持同步；人工只能在翻译编辑器中按段修订，修订结果写回翻译记忆 |
| `MANUAL` | 脱离自动翻译，独立编辑（如为某个市场单独写的页面）；源语言更新后只标记 `OUTDATED`（翻译面板列出变化的字段），不自动覆盖 |

- **翻译状态**（`translation_state`）：

| 状态 | 含义 |
|---|---|
| `NONE` | 源语言本身 |
| `QUEUED` | 排队或翻译中 |
| `TRANSLATED` | 全部段落已生成并通过校验 |
| `NEEDS_REVIEW` | 有段落校验未通过，或该内容要求人工审校 |
| `FAILED` | 引擎错误且重试耗尽 |
| `OUTDATED` | MANUAL 模式下源语言已更新 |

- **法律页面**（隐私政策、Cookie 政策、条款、Imprint）与 `legal` 模板：机器译文**不自动发布**，必须人工审校后发布。

### 5.6 翻译单元与翻译记忆

**翻译单元（Segment）**是最小翻译单位：

- 纯文本字段：一个字段一个单元（标题、摘要、SEO 标题与描述、属性名、选项名、分类名、菜单名、表单标签、alt、界面文案等）。
- 富文本：按块级节点拆分（段落、标题、列表项、表格单元格、引用）；行内标记（加粗、链接、`content://` 引用等）转为编号占位标签（如 `<1>…</1>`），翻译后按编号还原（见 9）。
- 每个单元带：字段路径、格式（`TEXT` / `RICH_INLINE`）、可选的长度上限（如 SEO 标题 60 个字符）、上下文说明（如“产品名称”“按钮文字”）。

富文本段落的处理示意：

```text
原文段落：  本产品通过 [加粗]CE[/加粗] 认证，详见 [链接 content://7201893453912345]安装手册[/链接]。
翻译单元：  本产品通过 <1>CE</1> 认证，详见 <2>安装手册</2>。          （格式 RICH_INLINE）
译文（en）：This product is <1>CE</1> certified. See the <2>installation manual</2>.
还原：      按编号把 <1>、<2> 还原为加粗与链接；链接目标 content://7201893453912345 不变
```

**翻译记忆**（Translation Memory，`trn_memory`）：

- 键：`(source_locale, target_locale, source_hash, variant)`；`source_hash` = SHA-256(规范化原文)，规范化：Unicode NFC、去除首尾空白、合并连续空白；`variant` 默认 `default`，有长度上限且默认译文超长时，另存 `max{N}` 变体（如 `max60`）。
- **命中即复用，不调用 AI**；同一批次中重复的原文只翻译一次。
- 来源与优先级：`HUMAN`（人工修订）> `REVIEWED`（人工确认的机器译文）> `MACHINE`（机器译文）。人工修订写回翻译记忆，以后相同原文复用人工版本；**机器翻译永远不会覆盖 `HUMAN` / `REVIEWED` 条目**。
- 每个条目记录引擎、模型、术语表版本、创建时间、使用次数、最后使用时间。
- 不设过期；可以按条目在翻译中心中查看、修订、作废。
- 站点资源与内容共用同一个翻译记忆（同站点内），因此“询价”“立即联系”这类常用词全站译法一致。

### 5.7 翻译流程

```text
中文内容发布（ContentPublishedEvent，locale = 源语言）
  │ translation 监听（事务提交后，异步）
  ▼
1. 对每个“启用自动翻译”的目标语言创建翻译任务 trn_job
2. 调用该内容类型的 TranslatableContentProvider.extract()，得到翻译单元（字段路径 + 原文 + 格式 + 长度上限 + 上下文）
3. 逐个查翻译记忆：命中 → 直接使用；未命中 → 去重后进入待翻译队列
4. 待翻译单元按批次调用 AI 引擎（每批不超过约 50 个单元或配置的 token 上限）
5. 校验译文（见 5.8）→ 写入翻译记忆
6. 调用 TranslatableContentProvider.apply()：按字段路径写回目标语言草稿；非文本数据从源语言复制
7. 按站点配置：自动发布（默认）或进入审核；法律页面一律进入审核
8. 发布 TranslationCompletedEvent / TranslationFailedEvent
```

- **任务与状态**：`trn_job` 记录翻译任务（实时 / 批量、目标语言、状态、进度、费用），`trn_job_item` 记录任务中每条内容或站点资源的执行结果（含未通过校验的单元）。创建任务时，目标语言本地化（不存在时由内容内核创建，`translation_mode = AUTO`）的 `translation_state` 置为 `QUEUED`；完成后为 `TRANSLATED`，有单元校验未通过时为 `NEEDS_REVIEW`，引擎错误且重试耗尽时为 `FAILED`；`source_revision_no` 记录所依据的源语言修订号。源语言在任务执行期间再次发布时，以新修订为准，旧任务作废（见 4.1 的顺序规则）。
- **增量翻译**：源语言再次发布时重复上述流程。未修改的段落原文不变，直接命中翻译记忆（包括人工修订过的译文）；只有被修改的段落调用 AI（见 1.3 S12）。
- **自动发布**：第 7 步的自动发布由内容内核在收到 `TranslationCompletedEvent` 后执行，与人工发布使用同一套校验与发布流程（见 4.4、6.3），操作人记为 `system`。目标语言的 slug 由译文标题生成，首次发布后固定（见 13.3）。
- **站点资源**：各模块在资源变化时发布 `TranslatableResourceChangedEvent`（资源类型 + ID），translation 通过 `TranslatableResourceProvider` 提取与回写，流程同上（资源保存即生效，不走内容发布流程）；delivery 收到 `TranslationCompletedEvent` 后按缓存标签失效。
- **批量翻译**：开放新语言、批量导入产品、术语表变化后重译时，创建批量任务，按内容逐条排队执行，限制并发与速率；进度、费用在翻译中心可见；可暂停、取消、重试失败项。
- **源语言下线或进回收站**：`AUTO` 模式的目标语言本地化跟随下线；`MANUAL` 模式不受影响（见 6.3）。
- **实时性**：单条内容通常在 1–5 分钟内完成（取决于引擎）；翻译是异步的，**AI 引擎故障不影响中文内容的发布**。
- **询盘翻译（按需）**：销售在询盘详情页点击“翻译为中文”，inquiry 调用 translation 在 platform-api 中的公开服务接口（`TranslationApi.translateText`）翻译留言，再由 inquiry 把译文保存在询盘上（见 17.10；translation 是 L2，不直接写询盘表）；再次查看不重复调用（见 1.3 S13）。站点可关闭此功能。询盘译文不写入翻译记忆（属于个人数据，不复用）。

### 5.8 质量保障

- **术语表**（`trn_glossary_term`，站点级）：中文术语 → 各语言译法；可标记“禁止翻译”（品牌名、产品系列名、型号、标准号等）。翻译时把与本批原文相关的术语条目放入提示词。术语表变化后，可一键重译包含该术语的 `MACHINE` 条目（不影响 `HUMAN` / `REVIEWED`）。
- **风格指南**（`trn_style_guide`，站点 × 目标语言）：语气（专业、简洁）、称呼（如德语使用 Sie）、数字与单位格式、品牌写法等，作为系统提示词的一部分。
- **占位保护**：型号、数字与单位、URL、邮箱、`content://` 引用、编号占位标签在译文中必须原样保留。
- **自动校验**（每个译文）：

| 校验项 | 规则 |
|---|---|
| 占位标签 | 完整，且编号与原文一致 |
| 数字 | 与原文一致（按数值比较，小数点与千位分隔符可以按风格指南调整） |
| 禁止翻译的术语 | 保留原样 |
| 术语表译法 | 被遵守 |
| 长度 | 不超过上限 |
| 残留中文 | 译文中没有残留中文 |
| 输出格式 | 合法 JSON，且单元数量与 ID 一一对应 |

- **校验失败**：自动重试一次（附带失败原因）；仍失败则该单元标记 `NEEDS_REVIEW`，所在本地化不自动发布，在翻译中心待处理列表中出现。
- **翻译编辑器**（后台，见 22.3）：源语言与目标语言按段并排显示；可修订（写回翻译记忆，来源 `HUMAN`）、标记已确认（`REVIEWED`）、单段重译；显示每段来源（记忆命中 / 机器 / 人工）。翻译审校角色可以被限定在指定语言（数据范围）。
- **SEO**：Google 允许使用自动翻译，但要求对用户有帮助；建议对首页、主要产品与分类页安排母语人员抽检；SEO 健康检查新增规则“关键页面的机器译文未经审校”（INFO，见 14.9）。

### 5.9 引擎接入（OpenAI 兼容接口）

- 后端定义端口 `TranslationEngine`（infrastructure 提供适配器，做法与 12.5 的存储适配相同）；translation 只依赖该端口，不接触具体服务商：

```java
public interface TranslationEngine {
    /** 翻译一批单元：返回每个单元的译文与本次调用的 token 用量 */
    TranslationResult translate(TranslationRequest request);
}
```

- **默认适配器使用 OpenAI 兼容的 Chat Completions 接口**（`POST {baseUrl}/chat/completions`），可配置项见下表。因此可接入任何兼容该接口的服务（如 OpenAI、DeepSeek、通义千问、Kimi，或自部署的 vLLM、Ollama），切换服务商只改配置。
- 后端用 Spring 的 HTTP 客户端直接调用，**不绑定任何厂商 SDK**。
- 引擎配置项：

| 配置项 | 说明 |
|---|---|
| base URL | 服务地址，如 OpenAI、其他兼容服务或自部署服务的地址 |
| API Key | 加密存储（见 26.6） |
| 模型名 | 由服务商决定 |
| 温度 | 默认 0.2 |
| 单次请求上限 | 最大单元数与 token 上限 |
| 超时 | 单次请求的超时时间 |
| 速率上限 | 每分钟请求数与 token 数 |
| 结构化输出 | 是否支持 `response_format`（`json_schema` 严格模式 / `json_object` / 不支持） |
| 单价 | 输入、输出 token 单价，用于估算费用（见 5.10） |

- **输出格式**：要求模型返回 JSON（`{"items":[{"id":"…","text":"…"}]}`）；服务商支持时使用 `response_format`（`json_schema` 严格模式，或 `json_object`），不支持时依靠提示词约束 + 严格解析 + 校验重试。是否支持由引擎配置项声明。单元 ID 由系统生成，响应中的 ID 必须与请求一一对应（见 26.3）。
- **提示词结构**：固定的系统提示词（角色、规则、输出格式）+ 风格指南 + 本批相关术语在前，待翻译单元在后——前缀稳定，便于支持自动前缀缓存的服务商降低费用。

请求示例（`POST {baseUrl}/chat/completions`）：

```json
{
  "model": "<引擎配置中的模型名>",
  "temperature": 0.2,
  "response_format": { "type": "json_schema", "json_schema": { "name": "translations", "strict": true, "schema": { } } },
  "messages": [
    { "role": "system", "content": "固定的系统提示词（角色、规则、输出格式）+ 风格指南 + 本批相关术语" },
    { "role": "user",   "content": "{\"source\":\"zh-CN\",\"target\":\"de\",\"items\":[{\"id\":\"u1\",\"text\":\"…\",\"maxLength\":60,\"context\":\"产品名称\"}]}" }
  ]
}
```

- **重试**：429 与 5xx 按退避重试（尊重 `Retry-After`），最多 3 次；可配置**备用引擎**（另一个 OpenAI 兼容服务），主引擎持续失败时切换。
- 引擎配置保存在 `trn_engine`（API Key 加密存储，见 26.6），可配置多个，站点选择主引擎与备用引擎。
- 待翻译文本与译文一律按数据处理，不作为指令执行（提示词注入防护见 26.3）。

### 5.10 用量、成本与运维

- 每次调用记录输入、输出 token（取自响应的 `usage`）与耗时；按引擎配置的单价估算费用；按天汇总到 `trn_usage_daily`。
- 指标：翻译记忆命中率、引擎错误率与延迟、待处理单元数、每日费用（见 27.4）。
- **月度预算上限**（站点配置）：达到 80% 告警；达到 100% 暂停批量任务（实时任务可配置是否继续）。
- **翻译中心**（后台）：

| 功能 | 说明 |
|---|---|
| 任务 | 任务列表与进度；暂停、取消、重试失败项 |
| 待审校 | `NEEDS_REVIEW` / `FAILED` 的本地化与单元；法律页面的待审校译文 |
| 翻译记忆 | 浏览、修订、作废 |
| 术语表 / 风格指南 | 维护；术语变化后一键重译相关的 `MACHINE` 条目 |
| 引擎配置 | 多个 OpenAI 兼容引擎；站点选择主引擎与备用引擎 |
| 用量与费用 | 按天汇总的 token 用量与费用报表；月度预算 |

---

## 6. 内容内核（Content Kernel）

### 6.1 模型

```text
cnt_content（语言无关）
 ├── id、site_id、type、owner_module、routable、primary_locale、trashed_at
 │
 ├── cnt_localization（每种语言一条）
 │     ├── locale、slug、draft_title、draft_state、draft_hash
 │     ├── publish_state、published_revision_id、first_published_at、published_at
 │     ├── scheduled_publish_at、scheduled_unpublish_at、unpublish_strategy
 │     ├── translation_mode、translation_state、source_locale、source_revision_no
 │     ├── 投影字段：pub_title、pub_summary、pub_cover_media_id、pub_path、pub_hash、content_modified_at
 │     └── version（乐观锁）
 │
 ├── cnt_revision（不可变的发布快照）
 │     └── localization_id、revision_no、payload（JSON）、payload_hash、comment、published_by、published_at
 │
 ├── cnt_published_term（投影：发布态的分类指派，用于列表与图谱）
 └── cnt_review（审核记录：提交人、审核人、结论、意见）
```

**内容与业务数据分离**（以产品为例）：

```text
cnt_content (id=1001, type=PRODUCT)          ──1:1──  prd_product (content_id=1001, model_no, brand, …)
cnt_localization (content_id=1001, locale=en) ──1:1──  prd_product_localization (product_id=1001, locale=en, name, description, …)
```

- 内容内核拥有：身份、类型、语言版本、slug、发布状态、修订、快照、投影。
- 业务模块拥有：本类型的业务字段（草稿态），保存在自己的表中，以 `content_id` 关联。
- 业务模块创建内容时，在自己的事务中调用 `ContentApi.create(type, locale)` 获得 `content_id`；保存草稿时调用 `ContentApi.updateDraftMeta()` 同步后台列表需要的标题。

**翻译相关字段**：

- `cnt_content.primary_locale` 即源语言（`zh-CN`，见 5.4）。
- `translation_mode`：`SOURCE`（源语言本地化）/ `AUTO`（由 AI 翻译生成，目标语言默认）/ `MANUAL`（独立编辑），见 5.5。
- `translation_state`：`NONE` / `QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED`，含义见 5.5。
- `source_locale`、`source_revision_no`：生成译文时依据的源语言及其修订号。源语言发布新修订后，`AUTO` 模式据此重新翻译（见 5.7），`MANUAL` 模式据此标记 `OUTDATED`。

### 6.2 内容类型

内容类型通过 `ContentTypeProvider` 注册：

```java
public record ContentTypeDefinition(
        String type,                    // "PRODUCT"
        String module,                  // "product"
        boolean routable,               // 是否有独立 URL
        String defaultUrlPattern,       // "/products/{slug}"
        List<String> templates,         // ["product-detail", "product-detail-compact"]
        Set<String> vocabularies,       // ["product_category", "application", "industry", "tag", "certification"]
        boolean layoutSlots,            // 是否允许在模板插槽中放置组件
        boolean reviewable              // 是否参与审核流
) {}
```

V1 内容类型：

| 类型 | 所属模块 | 可路由 | 默认 URL 规则 | 说明 |
|---|---|---|---|---|
| `PAGE` | page | 是 | `/{parentPath}/{slug}` | 普通页面与落地页，可以有父页面；落地页 = PAGE + landing 模板 |
| `PRODUCT` | product | 是 | `/products/{slug}` | 产品 |
| `ARTICLE` | article | 是 | `/blog/{slug}` | 新闻、博客（通过文章栏目区分） |
| `CASE_STUDY` | article | 是 | `/case-studies/{slug}` | 案例 |
| `FAQ` | article | 否 | — | 可复用的问答条目，通过组件或关系展示 |
| `TERM_PAGE` | taxonomy | 是 | `/product-category/{slug}`、`/applications/{slug}`、`/industries/{slug}` | 可路由分类项（产品分类、应用、行业）的落地页 |
| `GLOBAL_BLOCK` | page | 否 | — | 全局复用区块（见 10.6） |

默认 URL 规则不含语言前缀，语言前缀由站点语言配置添加（如 `/zh/products/{slug}`、`/en/products/{slug}`，见 5.4）。

V1.0 中的 `CATEGORY`、`APPLICATION` 统一为 `TERM_PAGE`；`LANDING_PAGE` 统一为 `PAGE` + landing 模板。

### 6.3 状态机

本地化有两个**正交**的状态：草稿状态（`draft_state`）与发布状态（`publish_state`）。这样可以表达“已发布，同时有尚未发布的修改”。

```text
draft_state：   EDITING ──提交审核──▶ IN_REVIEW ──通过──▶ APPROVED
                   ▲                     │ 驳回（附意见）        │ 再次修改
                   └─────────────────────┴─────────────────────┘

publish_state： NEVER ──发布──▶ PUBLISHED ──下线──▶ UNPUBLISHED ──再次发布──▶ PUBLISHED
                  │                 ▲
                  └──定时发布──▶ SCHEDULED ──到达时间──┘

内容级：         正常 ──移入回收站──▶ TRASHED（30 天后可彻底删除） ──恢复──▶ 正常
```

| 操作 | 前置条件 | 结果 | 权限 |
|---|---|---|---|
| 保存草稿 | — | draft_state = EDITING；version + 1 | `content:<type>:edit` |
| 提交审核 | 站点对该类型启用了审核 | IN_REVIEW；通知审核人 | `content:<type>:edit` |
| 通过 / 驳回 | IN_REVIEW | APPROVED / EDITING（附意见） | `content:<type>:review` |
| 发布 | 不需要审核，或已 APPROVED；校验通过 | 生成新修订；PUBLISHED；草稿与线上一致 | `content:<type>:publish` |
| 定时发布 / 定时下线 | 同发布 | SCHEDULED；每分钟的任务到时执行 | `content:<type>:publish` |
| 下线 | PUBLISHED | UNPUBLISHED；路由按下线策略返回 410 或 301（见 13.6）；源语言下线时，AUTO 模式的目标语言跟随下线 | `content:<type>:publish` |
| 回滚 | 存在历史修订 | 把历史快照恢复为草稿，需要再次发布 | `content:<type>:publish` |
| 移入回收站 | 未发布（已发布的需先下线） | trashed_at 有值 | `content:<type>:delete` |
| 恢复 / 彻底删除 | 在回收站中 | 恢复 / 物理删除并发布 `ContentPurgedEvent` | `content:<type>:delete` |

“有未发布的修改”通过比较 `draft_hash` 与 `pub_hash` 判断，在后台列表中显示徽标。

**AI 翻译生成的目标语言本地化**（`translation_mode = AUTO`）：

- 草稿由 translation 写回（见 5.7），不需要人工编辑。翻译完成（`TRANSLATED`）后，按站点配置**自动发布**（默认），或进入审核（`IN_REVIEW`）；法律页面（`legal` 模板）一律进入审核，人工审校后发布；有 `NEEDS_REVIEW` 或 `FAILED` 时不自动发布。
- 自动发布由内容内核在收到 `TranslationCompletedEvent` 后执行，与人工发布使用同一套发布校验与流程（见 4.4），操作人记为 `system`。
- 源语言本地化下线或内容进回收站时，AUTO 模式的目标语言本地化跟随下线；MANUAL 模式的本地化不受影响。由于移入回收站是内容级操作，仍有已发布的 MANUAL 本地化时，需先将其下线。

**发布校验**（`PublishContributor.validate()`）包括：必填字段、slug 唯一、引用的媒体存在、布局文档符合 Schema、引用的内容已发布（未发布时给出警告）。

### 6.4 发布快照与发布投影

**快照**是发布时冻结的完整渲染数据，前台只读取快照（原则 P4）。快照由各模块通过 `PublishContributor` 共同组装：

```java
public interface PublishContributor {
    String partKey();                                      // "product" / "layout" / "seo" / "terms"
    boolean supports(String contentType);
    void validate(PublishContext ctx, ValidationResult result);
    JsonNode contribute(PublishContext ctx);              // 产出本模块的快照部分
    void restore(PublishContext ctx, JsonNode part);      // 回滚时把这部分恢复为草稿
}
```

快照结构：

```json
{
  "schemaVersion": 1,
  "content": { "id": "7201893453912345", "type": "PRODUCT", "locale": "en", "revisionNo": 12 },
  "card": { "title": "Li-ion Pack X200", "summary": "…", "coverMediaId": "7201893453918208" },
  "terms": [
    { "vocabulary": "product_category", "termId": "7201893453900001", "primary": true },
    { "vocabulary": "application", "termId": "7201893453900107" }
  ],
  "parts": {
    "product": { "modelNo": "X200", "attributes": [], "models": [], "documents": [], "gallery": [] },
    "layout":  { "template": "product-detail", "slots": { "afterSpecs": [], "bottom": [] } },
    "seo":     { "title": "…", "description": "…", "robots": "index,follow", "canonical": null, "og": {}, "overridden": ["title"] }
  }
}
```

规则：

- **快照只包含本内容自身的数据**。对其他内容、媒体、分类只保存 ID（如 `$content`、`$media`），在读取时解析。这样被引用对象的变化（如图片 alt、另一个产品的标题）不需要重新发布引用方。
- 内容内核在发布时从快照中提取**投影**：`cnt_localization` 的 `pub_*` 字段、`cnt_published_term`、`url_route`。列表、筛选、路由、sitemap 都基于投影，永远不读取草稿表。
- `content_modified_at` 只在 `payload_hash` 变化时更新，用作 sitemap 的 `lastmod`，避免“只是重新发布”造成虚假的更新时间。
- 修订保留策略：90 天内全部保留；之后每个本地化至少保留最近 20 个修订。快照大小上限 1 MB。

这一模型的好处：前台按 ID 读取一行即可渲染，速度快且结果一致；草稿永远不会泄漏到线上；回滚与审计天然支持。

### 6.5 预览

- 后台点击“预览”时生成预览令牌（签名，包含 site、localizationId、模式 `draft` 或 `revision:n`；有效期 1 小时；分享链接最长 7 天）。
- Nuxt 路由 `/__preview/{token}` 调用 `GET /api/public/v1/delivery/preview`；Delivery 调用各 `PublishContributor.contribute()` 从草稿**临时**组装快照（与发布同一套代码，但不持久化）后渲染。
- 预览响应带 `X-Robots-Tag: noindex, nofollow` 与 `Cache-Control: no-store`。
- 页面编辑器中的实时预览通过 postMessage 传递未保存的文档（见 10.8）。

### 6.6 并发控制

- 所有可编辑实体使用乐观锁：请求携带 `version`（或 `If-Match`），冲突时返回 409，后台展示差异并提示重新加载。
- 【V1 可选】编辑占用提示：通过心跳显示“某某正在编辑”，不强制加锁。

### 6.7 审核

- 每个站点可以按内容类型配置是否需要审核（`review.required`）。
- AI 翻译生成的目标语言本地化是否需要审核由站点单独配置（默认自动发布；法律页面一律审核，见 6.3）。
- V1 只支持单级审核：拥有 `review` 权限的人通过或驳回。
- 提交审核与审核结果通过通知模块发送给相关人员。
- 【V2+】多级审批与按条件路由。

---

## 7. 分类体系（Taxonomy）

分类体系是 Content Graph 自动关联、产品筛选、面包屑、分类落地页的共同基础。

### 7.1 模型

| 表 | 关键字段 |
|---|---|
| `tx_vocabulary` | key、hierarchical、multiple、routable、required、applicable_types |
| `tx_term` | vocabulary_id、parent_id、path（物化路径 `/1/5/9/`）、depth、sort、content_id（可路由时关联 TERM_PAGE） |
| `tx_term_localization` | term_id、locale、name、description |
| `tx_assignment` | content_id、term_id、is_primary、sort（草稿态） |

发布时，taxonomy 的 `PublishContributor` 把指派写入快照的 `terms` 部分，内容内核据此更新 `cnt_published_term`（发布态）。

### 7.2 内置词汇表

| key | 名称 | 层级 | 可路由 | 适用内容 |
|---|---|---|---|---|
| `product_category` | 产品分类 | 是 | 是 | PRODUCT |
| `application` | 应用场景 | 否（可配置） | 是 | PRODUCT、ARTICLE、CASE_STUDY、PAGE |
| `industry` | 行业 | 否 | 是 | PRODUCT、ARTICLE、CASE_STUDY |
| `article_category` | 文章栏目 | 是 | 是 | ARTICLE |
| `tag` | 标签 | 否 | 否 | 全部 |
| `certification` | 认证（CE、UL、ISO…） | 否 | 否 | PRODUCT |

站点可以自定义词汇表。

### 7.3 规则

- 可路由的分类项自动拥有一个 `TERM_PAGE` 内容：分类项某语言的名称首次填写时创建对应本地化；落地页展示分类描述、插槽组件与该分类下的内容列表（数据源）。
- 产品必须有一个**主分类**（`is_primary`），用于面包屑与结构化数据。
- 删除仍有内容指派的分类项时，要求先转移指派；删除可路由分类项时，其落地页下线并 301 到上级分类页。
- 分类项更名或移动发布 `TermChangedEvent`，按 `t:{termId}` 标签失效缓存。
- **翻译**：分类名以中文录入，作为站点资源翻译（经翻译记忆，全站译法一致，见 5.5）；分类描述随分类落地页（TERM_PAGE）作为内容翻译。
- 【V2+】分类项合并工具。

---

## 8. 产品模块（Product）

### 8.1 模型

| 表 | 说明 |
|---|---|
| `prd_product` | content_id（主键）、model_no、brand、primary_category_term_id、flags（新品、热销、停产）、replacement_content_id、sort |
| `prd_product_localization` | 名称、副标题、简介、详情（富文本 JSON）、卖点列表、包装信息、交期文本、MOQ 文本 |
| `prd_attribute_group` + 本地化 | 属性分组（如“电气参数”） |
| `prd_attribute` + 本地化 | key、group_id、data_type、unit、precision、dual_unit（双单位显示开关）、secondary_unit（第二单位，如 `in`、`lb`）、filterable、searchable、comparable、show_on_card、sort |
| `prd_attribute_option` + 本地化 | 枚举选项 |
| `prd_attribute_set` / `prd_attribute_set_item` | 属性集（哪些属性、是否必填、顺序） |
| `prd_category_attribute_set` | 产品分类绑定属性集（子分类继承） |
| `prd_attribute_value` | product_id、attribute_id、model_id（可空）、locale（仅可翻译文本）、value_text、value_number、value_number_max、option_ids（JSON）、value_bool |
| `prd_model` | 型号表的行：product_id、model_no、sort |
| `prd_product_media` | product_id、media_id、role（COVER / GALLERY / DIAGRAM）、sort |
| `prd_product_document` | product_id、media_id、doc_type（DATASHEET / MANUAL / CERTIFICATE / CAD）、locale（可空）、gated_form_key（可空）、sort |

属性数据类型：`TEXT`（可翻译）、`NUMBER`、`RANGE`（最小–最大）、`BOOLEAN`、`ENUM`、`MULTI_ENUM`。数值统一以标准单位存储。

**双单位显示**：数值类属性（`NUMBER`、`RANGE`）可以开启“公制 + 英制”双单位显示（`dual_unit`），并指定第二单位（`secondary_unit`），前台显示为 `100 mm (3.94 in)`、`5 kg (11 lb)`。换算在发布时计算并写入快照（见 8.2），前台不做换算。

**翻译**（见 5.5–5.7）：

- 属性分组名、属性名、枚举选项名属于**站点资源**：以中文录入，保存时发布 `TranslatableResourceChangedEvent`，经翻译记忆翻译到各语言，保证同一属性名、选项名全站译法一致。
- 产品名称、副标题、简介、详情、卖点等本地化字段与 `TEXT` 类型的属性值属于**内容**，随源语言发布由 AI 翻译；型号、数值、单位、分类指派、媒体与型号表结构不翻译，目标语言直接沿用源语言。

### 8.2 属性的存储与查询

- 编辑态使用 EAV 结构（`prd_attribute_value`），满足不同分类的属性差异。
- 发布时，产品的 `PublishContributor` 把属性展开为快照中的结构化规格：`[{group, items: [{key, label, value, unit, secondaryValue, secondaryUnit}]}]`；开启 `dual_unit` 的属性在此时按 `secondary_unit` 换算出 `secondaryValue`，未开启时这两个字段为空。
- 同时，`SearchDocumentProvider` 把可筛选属性写入搜索索引的分面字段（如 `attr.voltage`、`attr.color`）。
- **前台流量永远不查询 EAV 表**：详情页读快照，列表与筛选走搜索索引（见 19）。

### 8.3 型号表

B2B 产品常以“系列 + 多个型号”的形式出现。一个产品可以有多行型号，每行有自己的可比较属性值，前台渲染为规格对比表；询盘可以精确到型号。

### 8.4 产品文档与下载留资

- **公开文档**：媒体为公开文件，直接通过 CDN 下载。
- **留资文档**：媒体存放在私有桶；点击下载时弹出指定表单；提交通过后，后端签发短期有效的下载链接（页面直接返回，同时邮件发送），并生成一条 `DOWNLOAD` 类型线索（见 17）。
- 留资文档没有可直接访问的 URL，不会被索引。

### 8.5 停产与替代

- 标记停产：页面显示“停产”标识与替代产品链接，继续可索引。
- 下线：选择 410，或 301 到替代产品 / 主分类页（见 13.6）；引用该产品的卡片、推荐、导航自动移除。

### 8.6 导入与导出

| 项 | 设计 |
|---|---|
| 模板 | 按属性集生成 Excel 模板：型号、名称（中文）、分类、属性列、图片文件名、旧站 URL |
| 流程 | 上传 xlsx（可附图片 zip）→ 异步任务解析 → 校验并生成预览报告（新建 N、更新 M、错误逐行列出）→ 用户确认 → 分批执行（每个产品独立事务）→ 结果报告可下载 |
| 匹配键 | 站点内的 `model_no`，支持重复导入（更新） |
| 图片 | 来自 zip；或来自 URL（域名白名单 + SSRF 防护，见 26.3） |
| 发布 | 默认导入为草稿；有发布权限的用户可以选择“导入后直接发布”；发布后由 translation 创建批量翻译任务，翻译到启用自动翻译的语言（见 5.7） |
| 导出 | 按筛选条件导出为同一模板，支持“导出 → 修改 → 导入”的批量编辑 |
| 旧站迁移 | “旧站 URL”列在发布后自动生成 301（见 13.5） |

### 8.7 后台功能

- 列表：按分类、状态、翻译状态、SEO 分数筛选；批量指派分类、批量发布/下线。
- 编辑页标签：基本信息 / 属性 / 型号 / 媒体 / 文档 / 分类 / 关联（graph 提供）/ 页面插槽（page 提供）/ SEO（seo 提供）/ 翻译（translation 提供，见 22.3）/ 历史修订。

---

## 9. 文章模块（Article）

| 类型 | 字段 |
|---|---|
| `ARTICLE` | 标题、摘要、封面、正文（富文本 JSON）、作者、文章栏目、标签、应用、行业 |
| `CASE_STUDY` | 标题、摘要、封面、客户（可匿名）、行业、应用、挑战、方案、成果（指标列表）、使用的产品（关系 `USES`） |
| `FAQ` | 问题、答案（富文本 JSON）；通过分类指派或关系与产品、分类、页面关联 |

- **作者**（`art_author`）：姓名、头像、简介、社交链接。用于 Article 结构化数据中的 author，体现 E-E-A-T。
- **富文本**：使用 Tiptap，以 ProseMirror JSON 存储，节点白名单渲染（标题、段落、列表、表格、图片、视频、引用、提示框、产品卡片、CTA）。
  - 正文从 H2 开始，H1 保留给页面标题；长文自动生成目录。
  - 内部链接存为 `content://{id}`，渲染时解析为当前 URL；目标下线时降级为纯文本，并在 SEO 健康中报警。
  - 外部链接自动添加 `rel="noopener"`，可选 `nofollow` / `sponsored`。
  - **翻译**：按块级节点（段落、标题、列表项、表格单元格、引用）拆分为翻译单元；行内标记（加粗、链接、`content://` 引用等）转为编号占位标签，翻译后按编号还原（见 5.6）。图片、视频、产品卡片等非文本节点不翻译，目标语言直接沿用源语言；内部链接在目标语言中解析为被引用内容同语言版本的 URL，该语言没有对应版本时同样降级为纯文本。
- 阅读时长自动计算；前台展示发布日期与更新日期。

---

## 10. 页面、模板与组件

### 10.1 两种渲染模式

| 模式 | 适用 | 布局来源 | 编辑方式 |
|---|---|---|---|
| 模板渲染 | PRODUCT、ARTICLE、CASE_STUDY、TERM_PAGE | 模板的固定区域 + 可编辑插槽 | 表单编辑业务字段；在插槽中放置组件 |
| 页面搭建 | PAGE（首页、关于我们、落地页等） | 完整的布局文档 | 页面编辑器 |

系统内容使用模板，保证上万个产品页结构一致、可批量升级；营销页使用页面编辑器，保证灵活。两者使用同一套组件与布局文档格式。

**法律页面**（每个站点必备）：隐私政策（Privacy Policy）、Cookie 政策（Cookie Policy）、使用条款（Terms of Use）、Imprint（德语 Impressum；面向德国、奥地利时通常属于法律要求，适用范围与必填字段由法务确认，见 32 章 Q14）、无障碍声明（Accessibility Statement，可选，见 11.5）。实现方式：PAGE + `legal` 模板（以正文为主的简洁版式）；Imprint 的正文由站点资料中的法定信息字段自动生成（见 16.3），不手工维护。法律页面的机器译文不自动发布，必须人工审校后发布（见 5.5）。

### 10.2 模板定义

模板 = 主题中的 Nuxt 组件 + 共享 Schema 包中的模板元数据：

```json
{
  "key": "product-detail",
  "version": 1,
  "contentTypes": ["PRODUCT"],
  "label": "产品详情（标准）",
  "regions": [
    { "key": "hero",       "fixed": true, "component": "product-hero" },
    { "key": "specs",      "fixed": true, "component": "product-specification" },
    { "key": "afterSpecs", "slot": true,  "allowed": ["rich-text", "feature-list", "video", "faq", "download-list", "cta"], "max": 6 },
    { "key": "related",    "fixed": true, "component": "related-content",
      "source": { "provider": "related", "params": { "mix": { "PRODUCT": 4, "CASE_STUDY": 2, "ARTICLE": 3 } } } },
    { "key": "bottom",     "slot": true,  "allowed": ["cta", "inquiry-form", "global-block"], "max": 2,
      "default": [ { "component": "global-block", "props": { "key": "product-cta" } } ] }
  ]
}
```

- `label` 只写中文字符串（后台界面只用简体中文，见 2.4），不使用按语言区分的对象。
- 固定区域由模板渲染，读取快照中的业务数据；固定区域的数据源在读取时解析，因此修改模板配置（如推荐配比）**不需要重新发布内容**。
- 插槽中的组件保存在该本地化的布局文档中（快照的 `layout` 部分），由 page 模块的 `PublishContributor` 负责；目标语言的插槽布局按翻译模式生成（见 10.3）。
- Delivery 通过 `TemplateProvider` 扩展点读取模板定义。

### 10.3 布局文档

```json
{
  "schemaVersion": 1,
  "template": "landing",
  "sections": [
    {
      "id": "sec_01J9X4K2",
      "component": "hero",
      "componentVersion": 2,
      "variant": "hero-split",
      "hidden": false,
      "visibility": { "desktop": true, "tablet": true, "mobile": true },
      "style": { "background": "surface-brand", "paddingY": "xl", "container": "wide" },
      "props": {
        "eyebrow": "工业电池",
        "title": "可穿戴设备定制锂电池组",
        "headingLevel": "h1",
        "description": { "type": "doc", "content": [] },
        "image": { "$media": "7201893453918208" },
        "primaryCta": { "label": "获取报价", "link": { "$content": "7201893453912345" } }
      }
    },
    {
      "id": "sec_01J9X4K3",
      "component": "product-grid",
      "componentVersion": 1,
      "variant": "grid-4",
      "props": {
        "title": "推荐产品",
        "items": {
          "$source": {
            "provider": "content-query",
            "params": { "contentType": "PRODUCT", "terms": ["7201893453900001"], "sort": "-publishedAt", "limit": 8 }
          }
        }
      }
    },
    {
      "id": "sec_01J9X4K4",
      "component": "columns",
      "componentVersion": 1,
      "variant": "2-1",
      "children": {
        "main":  [ { "id": "sec_01J9X4K5", "component": "rich-text", "componentVersion": 1, "props": {} } ],
        "aside": [ { "id": "sec_01J9X4K6", "component": "inquiry-form", "componentVersion": 1, "props": { "formKey": "default-inquiry" } } ]
      }
    }
  ]
}
```

规则：

| 规则 | 说明 |
|---|---|
| 引用对象化 | 媒体、内容、分类、数据源一律使用 `$media`、`$content`、`$term`、`$source` 对象，**不保存 URL**。URL 在渲染时解析，永远不会失效，缓存标签也可以据此推导 |
| 样式只用 Token | `style` 中的值只能是 Token 枚举（如 `paddingY: "xl"`），不允许任意 CSS |
| 有限嵌套 | 只有布局类组件（`columns`、`tabs`、`accordion-group`）可以通过具名 `children` 嵌套，最大深度 2 |
| 按翻译模式生成 | 源语言（中文）本地化的布局文档由编辑维护（上例即源语言布局）。`AUTO` 模式下，目标语言布局 = 源语言布局 + 译文：结构（Section 及其 `id`、组件、变体、样式、可见性、引用与数据源）与源语言自动保持同步，只有可翻译属性（`x-translatable`，见 10.4）替换为译文，由 page 模块的 `TranslatableContentProvider` 提取与回写（见 3.7、5.7）；`MANUAL` 模式下独立编辑，可以用“从其他语言复制结构”操作作为起点，源语言更新后只标记 `OUTDATED`（见 5.5） |
| 稳定 ID | Section 的 `id` 不变，用于编辑器选中、锚点与统计 |
| 隐藏与可见性 | `hidden` 不输出；按设备的 `visibility` 通过 CSS 实现（内容仍在 HTML 中） |

### 10.4 组件定义

每个组件在共享包 `packages/component-schemas` 中有一份定义：

```json
{
  "type": "hero",
  "version": 2,
  "module": "page",
  "category": "marketing",
  "label": "首屏横幅",
  "variants": ["hero-centered", "hero-split", "hero-video"],
  "propsSchema": {
    "type": "object",
    "required": ["title"],
    "properties": {
      "eyebrow":      { "type": "string", "maxLength": 60, "x-editor": "text" },
      "title":        { "type": "string", "maxLength": 120, "x-editor": "text", "x-seo": "heading" },
      "headingLevel": { "enum": ["h1", "h2"], "default": "h2", "x-translatable": false },
      "description":  { "$ref": "#/$defs/richTextInline", "x-editor": "richtext-inline" },
      "image":        { "$ref": "#/$defs/mediaRef", "x-editor": "media", "x-media": { "kinds": ["IMAGE"], "requireAlt": true } },
      "primaryCta":   { "$ref": "#/$defs/cta" }
    }
  },
  "styleOptions": ["background", "paddingY", "container"],
  "seo": { "lcpCandidate": true },
  "allowedIn": ["page", "slot"],
  "migrations": "./migrations/hero.ts"
}
```

- `label` 只写中文字符串（见 10.2）。
- `x-editor`：后台据此自动生成编辑表单（见 22.4）。
- `x-translatable`：该属性是否作为翻译单元提取（见 5.6）。字符串与富文本默认 `true`（如上例的 `eyebrow`、`title`、`description`）；型号、代码、URL 等字符串以及枚举设为 `false`（如上例的 `headingLevel`）；媒体、内容、分类与数据源引用属于非文本数据，不翻译。`$defs` 中的复合类型在内部字段上标注（如 `cta` 的按钮文字可翻译，链接不翻译）。page 模块的 `TranslatableContentProvider` 据此从布局文档中提取翻译单元并写回目标语言（见 3.7、10.3）；属性的 `maxLength` 同时作为翻译单元的长度上限。
- `x-seo` 与 `seo`：供 SEO 健康检查使用（标题层级、H1 数量）；`lcpCandidate` 表示该组件位于首屏时，前台为其图片设置 `fetchpriority="high"` 并预加载。
- 同一份 JSON 被三方使用：Admin（生成表单、校验）、Nuxt（迁移与渲染）、后端（保存时用 JSON Schema 校验、按 `x-translatable` 提取翻译单元，构建时从共享包复制到 classpath）。

V1 组件清单（≥ 20 个，每个 2–3 个变体）：

| 分类 | 组件 |
|---|---|
| 布局 | `columns`、`tabs`、`accordion-group` |
| 营销 | `hero`、`feature-list`、`image-text`、`stats`、`logo-wall`、`testimonial`、`process-steps`、`timeline`、`cta`、`video`、`gallery` |
| 内容 | `rich-text`、`faq`、`article-list`、`case-list`、`download-list` |
| 产品 | `product-grid`、`product-category-grid`、`product-hero`、`product-specification`、`product-model-table`、`product-gallery` |
| 获客 | `inquiry-form`、`add-to-inquiry-button`、`inquiry-cart`、`contact-info`、`contact-buttons`、`map`（静态图 + 外链） |
| 系统 | `related-content`、`breadcrumb`、`global-block`、`embed`（白名单服务商）、`custom-html`（仅开发者角色，见 26.3） |

### 10.5 数据源（数据绑定）

组件不硬编码业务数据，而是通过数据源获取（原则 P5）：

```java
public interface DataSourceProvider {
    String key();                                      // "content-query"
    JsonNode paramsSchema();                           // 后台据此生成参数表单
    DataSourceResult resolve(DataSourceRequest req);   // 返回卡片数据与缓存标签
}
```

| 数据源 | 实现模块 | 说明 |
|---|---|---|
| `content-query` | content | 按类型、分类、手选列表、排序、数量查询已发布内容（基于投影） |
| `related` | graph | 当前内容的相关内容，支持按类型配比 |
| `term-children` | taxonomy | 子分类列表（分类导航网格） |
| `faq-by-context` | article | 与当前内容或分类关联的 FAQ |
| `search-query` | search | 带分面的产品列表（分类落地页） |

- 参数中可以使用上下文变量，如 `{ "relatedTo": "$current" }`。
- 限制：每个数据源最多 24 条结果；每页最多 10 个数据源；单个解析超时 500 ms，失败时该组件渲染为空并记录日志。
- Delivery 使用虚拟线程并行解析页面中的全部 `$source`，结果按 Section ID 放在响应的 `dataSources` 中（见 24.4）。

### 10.6 全局区块

- 全局区块是不可路由的 `GLOBAL_BLOCK` 内容，复用内容内核的修订与发布机制。
- 通过 `global-block` 组件按 key 引用（如页脚上方的 CTA、信任背书条）；全局区块内部不能再引用全局区块。
- 发布后按 `c:{blockContentId}` 标签失效所有使用它的页面。

### 10.7 Header 与 Footer

由主题实现，读取导航菜单（见 16）与站点资料。站点管理员在“主题设置”中选择变体与选项（是否吸顶、是否显示语言切换、右上角 CTA 等）。V1 不允许用页面编辑器编辑 Header / Footer，以保证全站的设计质量。

页脚由主题固定提供“Cookie 设置”入口，并可配置“Your Privacy Choices”链接（见 20.2）；法律页面（见 10.1）的链接通过页脚菜单配置。

### 10.8 页面编辑器

```text
┌─────────────────────────────────── Admin（Vue + Element Plus）───────────────────────────────────┐
│ 顶栏：页面名 · 语言切换 · 设备切换（1440 / 768 / 390）· 撤销 / 重做 · 保存 · 预览 · 提交 / 发布 · 历史  │
├────────────────┬────────────────────────────────────────────────────────┬───────────────────────┤
│ 组件面板         │  <iframe src="https://www.example.com/__editor?token=…">  │ 属性面板（Schema 生成）   │
│ （按模板插槽与     │                                                        │ 内容 / 样式 / 数据源 /    │
│  模块启用状态过滤） │      Nuxt 渲染：真实的主题、组件与数据                       │ 可见性 / 高级             │
│                │      选中框、悬停框、插入点由 iframe 内的覆盖层绘制            │                       │
│ 大纲树           │                                                        │ 校验错误就地提示          │
│ （拖拽排序、嵌套）  │                                                        │                       │
└────────────────┴────────────────────────────────────────────────────────┴───────────────────────┘
```

**为什么用 iframe**：Admin 使用 Element Plus，前台组件在 Nuxt 主题中，两者无法直接复用。iframe 直接加载前台的真实渲染，保证“所见即所得”，并且样式完全隔离。

**通信协议**（双方都校验 `origin`）：

| 方向 | 消息 | 说明 |
|---|---|---|
| Admin → 画布 | `editor:init {document, locale, device}` | 初始化 |
| Admin → 画布 | `editor:patch {ops}` | 以 JSON Patch 增量更新文档 |
| Admin → 画布 | `editor:select {sectionId}`、`editor:scrollTo {sectionId}` | 同步选中 |
| 画布 → Admin | `preview:ready`、`preview:select {sectionId}`、`preview:hover {sectionId}` | 画布内点击与悬停 |
| 画布 → Admin | `preview:insert {parentId, slot, index}` | 点击画布中的插入点 |
| 画布 → Admin | `preview:error {sectionId, message}` | 渲染错误 |

画布中的数据源由 Nuxt 使用编辑器令牌直接调用 Delivery 预览接口解析。

**交互取舍**：跨 iframe 的拖放实现复杂且不稳定，V1 采用“大纲树拖拽排序 + 画布内插入点按钮 + 组件面板点击插入”的方式。

**其他**：

- 撤销 / 重做：在 Admin 中保存 JSON Patch 历史（最多 100 步）。
- 自动保存：每 30 秒保存一次草稿（带乐观锁）；本地同时保存一份，防止意外丢失。
- 发布前在侧栏展示校验错误与 SEO 健康提示。
- 多语言：页面编辑器编辑源语言（中文）与 `MANUAL` 模式的布局文档；切换到 `AUTO` 模式的目标语言时只能预览，文字在翻译编辑器中按段修订（见 5.8、22.3），结构在源语言中修改后自动同步（见 10.3）。

### 10.9 组件版本与迁移

- 组件属性发生破坏性变化时，`version + 1`，并在共享包中编写迁移函数 `(props, fromVersion) => props`（TypeScript）。
- Admin 打开草稿时执行迁移，因此保存的总是最新版本；Nuxt 渲染旧快照时即时迁移，**旧内容无需重新发布**。
- 后端保存时用最新的 JSON Schema 校验。
- 删除组件：先标记为 deprecated（组件面板中隐藏），保留渲染器，直到 `pg_component_usage`（发布时维护的使用索引）显示已无已发布内容使用它。
- CI 检查：组件 Schema 发生破坏性变化时，必须同时提升版本并提供迁移函数（见 29.3）。

---

## 11. 设计系统与主题

### 11.1 Design Token 分层

```text
Primitive（原子值）     color.blue.600 = #1F5FD1      space.4 = 16px      font.size.300 = 1.125rem
        │ 被引用
        ▼
Semantic（语义）        color.brand.primary → {color.blue.600}
                       color.text.default、color.surface.alt、space.section.md、radius.card
        │ 被引用
        ▼
Component（组件级）      button.primary.bg → {color.brand.primary}
```

- 类别：color、typography（字体、字号、字重、行高、字距；使用 `clamp()` 的流式字号）、spacing、radius、shadow、border、container、breakpoint、motion（时长、缓动、减弱动画）、z-index、opacity。
- 格式：DTCG（W3C Design Tokens 社区组）JSON，放在 `packages/design-tokens`。
- 构建：Style Dictionary 生成 CSS 自定义属性（`--color-brand-primary`）、TS 常量（编辑器样式选项的枚举）与文档页面。
- **后台界面不使用前台的 Design System**：Admin 使用 Element Plus 自己的主题变量；前台 Token 只用于主题设置预览。

### 11.2 主题结构

```text
frontend/packages/themes/industrial/
├── theme.json        # key、名称、支持的组件变体、默认设置、可调整的 Token 白名单
├── tokens/           # 覆盖 semantic / component 层 Token
├── fonts/            # 自托管 woff2 子集（拉丁字体；中文使用系统字体，见 11.6）
├── layouts/          # default、landing（无导航）、blank
├── header/  footer/  # 多个变体
├── templates/        # product-detail、article-detail、case-detail、term-page、legal …
├── components/       # 组件变体实现：hero/HeroSplit.vue …
└── index.ts          # defineTheme({...})：注册组件变体与模板（异步组件，按需加载）
```

- 所有主题在构建时打包进 Nuxt 应用，运行时按站点配置选择。
- **变体回退**：主题缺少某变体 → 使用该主题中该组件的默认变体 → 使用基础主题的实现 → 不渲染并记录日志。
- V1 只交付 1 套主题，把 Token 与变体机制做扎实；【V2+】多主题。

### 11.3 品牌定制

站点管理员在“主题设置”中只能调整白名单内的项目：

| 可调整项 | 实现 |
|---|---|
| 品牌色（主色、辅色、强调色） | 写入站点设置 → 站点引导数据 → SSR 在 `<head>` 中注入 `:root{--color-brand-primary:…}`；悬停、浅色背景等派生色用 `color-mix(in oklch, …)` 计算 |
| 标题与正文字体 | 只能从主题内置的字体列表中选择（拉丁字体；中文始终使用系统字体栈，见 11.6） |
| 圆角风格 | none / sm / md / lg，映射到 radius Token |
| 按钮风格、Header / Footer 变体 | 主题提供的选项 |
| Logo | 媒体引用 |

保存时检查对比度：品牌色上的文字对比度必须 ≥ 4.5:1，否则阻止保存并给出建议色。

### 11.4 约束手段

- **Stylelint**：`declaration-strict-value` 规则要求颜色、间距、字号、圆角、阴影、z-index 只能使用 CSS 变量；除 tokens 包外禁止十六进制与 rgb 颜色字面量。
- **ESLint**：主题组件中禁止带字面量的内联 `style`；禁止 `v-html`（富文本渲染器除外）。
- **编辑器**：样式选项只能是 Token 枚举，不允许输入任意 CSS。
- **视觉回归**：`/__gallery` 路由（非生产环境）展示组件 × 变体 × 断点，用 Playwright 截图对比（见 28）。

### 11.5 无障碍

目标 WCAG 2.2 AA：语义化标签与地标、可见的焦点样式、跳转到主内容链接、颜色对比度、`prefers-reduced-motion`、表单标签与错误提示、内容图片必须有 alt、标题层级正确（由 SEO 健康检查辅助）。

- **法规背景**：欧洲无障碍法案（EAA）自 2025-06-28 起适用，主要针对面向消费者的产品与服务，B2B 站点是否适用由法务确认；美国存在基于 ADA 的网站无障碍诉讼风险。无论是否适用，前台都按 WCAG 2.2 AA 实现。
- **无障碍声明**：提供无障碍声明页（Accessibility Statement，PAGE + `legal` 模板，见 10.1），说明符合程度、已知问题与反馈渠道。

### 11.6 字体与静态资源

- 拉丁文字体自托管（woff2，子集为 Latin + Latin Extended，覆盖德、法、西、意、波兰等语言；不需要西里尔），`font-display: swap`，预加载主字重；系统字体回退时使用 `size-adjust` 减少布局偏移。
- **中文使用系统字体栈**，不下载中文 Web 字体（中文字体文件体积大）：中文页面的 `font-family` 为主题的拉丁字体 + `"PingFang SC", "Microsoft YaHei", "Noto Sans SC"` 等系统中文字体 + `sans-serif`，拉丁字母与数字使用主题字体，中文字符由系统字体显示；按 `<html lang>` 切换字体栈。
- 不使用 Google Fonts CDN（欧盟隐私判例风险，以及中国大陆后台预览时的访问问题）。

---

## 12. 媒体模块（Media）

### 12.1 模型

| 表 | 关键字段 |
|---|---|
| `mda_media` | kind（IMAGE / VIDEO / DOCUMENT / ARCHIVE / OTHER）、visibility（PUBLIC / PRIVATE）、storage_key、original_name、mime、size、sha256、width、height、duration、focal_x、focal_y、blurhash、dominant_color、folder_id、status（PROCESSING / READY / QUARANTINED） |
| `mda_media_localization` | media_id、locale、alt、title、caption |
| `mda_folder` | parent_id、name、path |
| `mda_usage` | media_id、content_id、locale、part、field_path（引用追踪，由 `MediaReferenceCollector` 在保存与发布时维护） |

- alt、标题等文本以中文录入，作为站点资源翻译到各语言（经翻译记忆，见 5.5）；媒体文件本身不翻译，各语言共用。

### 12.2 上传流程

```text
Admin 上传（V1 经后端流式上传，单文件 ≤ 50 MB；【V2+】大文件使用预签名直传）
  → 校验扩展名白名单与大小
  → Apache Tika 根据文件头识别真实 MIME，与扩展名不符则拒绝
  → 计算 sha256：发现重复时提示复用已有文件
  → 图片：读取尺寸，生成 blurhash 与主色
     SVG：净化（移除 script、事件属性、外部引用）；非管理员角色禁止上传 SVG
  → 文档与压缩包：病毒扫描（ClamAV，异步）→ READY / QUARANTINED
  → 存储键：{siteId}/{yyyy}/{mm}/{id}-{slug化的原文件名}.{ext}
  → 发布 MediaUploadedEvent
```

### 12.3 图片交付

- 原图保存在对象存储中；前台通过 imgproxy 按需变换：`https://img.example.com/{签名}/rs:fill:800:600/g:fp:0.30:0.60/plain/{storage_key}@webp`，结果由 CDN 缓存。
- 使用 `<picture>` 输出 AVIF、WebP 与 JPEG/PNG 回退，**在 URL 中显式指定格式**，不依赖 `Accept` 头协商，避免 CDN 缓存需要按 `Vary: Accept` 区分。
- 响应式宽度：320、640、960、1280、1920；预设：thumb、card、content、hero。
- 必须输出 `width` 与 `height`（防止 CLS）；非首屏图片 `loading="lazy"`；LCP 图片 `fetchpriority="high"` 并预加载。
- 输出时去除 EXIF 等元数据（保护隐私）；原图保留。

### 12.4 视频

- 推荐使用 YouTube / Vimeo，并采用“外观占位”方式：缩略图自托管（存入媒体库），页面加载时不请求第三方；点击后才加载 iframe。明示同意地区在点击前提示“将加载第三方内容并向其传输数据”，或要求访客已同意对应类别，具体由法务确认。YouTube 仍使用 nocookie 域名，但它只能减少播放前的 Cookie，不能代替同意。`embed` 组件同样适用。
- 自托管 MP4（≤ 50 MB）只用于静音循环的背景视频，必须提供封面图。
- 视频组件输出 `VideoObject` 结构化数据。

### 12.5 存储适配

```java
public interface ObjectStorage {
    void put(StorageKey key, InputStream in, long size, String contentType, Visibility visibility);
    InputStream get(StorageKey key);
    void delete(StorageKey key);
    URI publicUrl(StorageKey key);
    URI presignedGet(StorageKey key, Duration ttl);
}
```

实现：`S3CompatibleStorage`（覆盖 AWS S3、Cloudflare R2、阿里云 OSS、MinIO）、`LocalFileStorage`（仅开发）。业务模块只依赖 `MediaApi`，不接触具体存储。

### 12.6 删除与清理

- `mda_usage` 中仍有引用的媒体不允许删除，界面列出引用位置。
- 提供“未使用媒体”报告，供定期清理。

---

## 13. URL 引擎

### 13.1 模型

| 表 | 关键字段 |
|---|---|
| `url_route` | site_id、locale、path、content_id、localization_id；唯一键 (site_id, path)、(localization_id) |
| `url_redirect` | site_id、source_path、match_type（EXACT / PREFIX / REGEX）、target_kind（LOCALIZATION / PATH / EXTERNAL / GONE）、target_localization_id、target_path、status_code（301 / 302 / 308 / 410）、origin（AUTO / MANUAL / IMPORT）、hits、last_hit_at、note |
| `url_pattern` | site_id、locale、content_type 或 vocabulary、pattern（不含语言前缀，如德语 `/produkte/{slug}`；语言前缀由站点语言配置添加，完整路径为 `/de/produkte/{slug}`，见 5.4） |
| `url_not_found` | site_id、path、hits、first_seen_at、last_seen_at、last_referrer（按天聚合） |

### 13.2 请求解析顺序

```text
1. 规范化（不符合时 301 到规范形式）
   a. 非主域名或 http → 主域名 https
   b. 尾斜杠策略（站点配置：统一去掉或统一保留）
   c. 合并重复斜杠
   d. 路径包含大写字母且存在对应的小写路由 → 301 到小写
2. 根路径 /                  → 302 到默认对外语言的首页（固定跳转，不按 Accept-Language，见 5.4）
3. 精确匹配 url_route        → 200，返回内容（路由所属语言须对外公开，否则按 404 处理）
4. 匹配 url_redirect（EXACT → PREFIX → REGEX，按顺序）→ 301 / 308 / 410
5. 系统路由（sitemap、robots、搜索页等）由 Nuxt 或 Delivery 处理
6. 都不匹配 → 404，并计入 url_not_found（先在 Redis 中计数，定期批量写库）
```

### 13.3 slug 规则

- 首次创建时根据标题生成：ICU 音译（`Any-Latin; Latin-ASCII; Lower`），只保留 `[a-z0-9-]`，最长 80 个字符。
- 音译规则按语言配置：
  - 中文（源语言）：默认用拼音音译（ICU `Han-Latin; Latin-ASCII`，不带声调），如 `锂电池组` → `li-dian-chi-zu`；编辑可以手工修改（如多音字音译不准时）；
  - 德语：先替换 `ä→ae、ö→oe、ü→ue、ß→ss`（大写同理），再执行 `Latin-ASCII`，如 `Größe für Häuser` → `groesse-fuer-haeuser`；
  - 法语、西语、意大利语等：去掉重音（ICU `Latin-ASCII`），如 `Câble résistant` → `cable-resistant`。
- **目标语言**（AI 翻译生成）：slug 由译文标题按该语言的音译规则生成，**首次发布后固定**；之后译文变化（源语言修改后重译、术语表变化后重译、人工修订）不自动修改 slug，避免产生重定向。需要时可以手工修改（见本节最后一条）。
- 按语言配置，可以保留本地文字的 slug（如日语）：以 Unicode NFC 存储，在 HTML 与 sitemap 中输出百分号编码形式。
- 保留字：`api`、`admin`、`__preview`、`__editor`、`sitemap.xml`、`robots.txt` 等。
- 在站点内按完整路径唯一；冲突时建议追加 `-2`。
- 发布后仍可修改 slug（界面给出提示）；发布时自动生成重定向。

### 13.4 自动重定向（关键设计）

- 当本地化 L 的发布路径从 `P_old` 变为 `P_new` 时，写入 `url_redirect(source = P_old, target_kind = LOCALIZATION, target = L, 301)`。
- **重定向指向本地化，而不是路径**：L 以后再改路径，所有历史路径仍然直接跳到它的当前路径，**从结构上避免重定向链**。
- L 下线时，指向它的重定向遵循 L 的下线策略（410，或 301 到替代内容）。
- 新内容占用某条历史路径时，删除该重定向（路由优先），并提示用户“此 URL 以前属于 X”。
- 手工重定向（路径 → 路径）保存时：若目标路径本身是某个重定向的源，则压缩为最终目标；做环路检测，有环则拒绝；若目标路径是某个本地化的路由，则自动转存为 `LOCALIZATION` 类型。
- 页面层级变化（移动父页面）时，由任务重新计算子树路径，并为每个变化的路径生成重定向。
- 产品 URL 默认不包含分类路径，因此调整分类不会引发批量重定向；层级信息由面包屑与结构化数据表达。

### 13.5 旧站迁移

- 支持 CSV 批量导入重定向（source、target、code），提供校验报告：目标不存在、环路、链、与现有路由冲突。
- 支持有限的前缀与正则重定向（如 `/old-blog/(.*)` → `/en/blog/$1`，目标路径包含语言前缀），在精确匹配之后按顺序执行，每站最多 200 条。
- 上线后提供“未命中的旧 URL”报告（来自 `url_not_found`），SEO 专员可以一键创建重定向。

### 13.6 下线策略

| 策略 | 适用 |
|---|---|
| `GONE`（410） | 内容永久删除且没有替代（默认） |
| `REDIRECT_CONTENT`（301 到指定内容） | 停产产品有替代型号；文章被合并 |
| `REDIRECT_PARENT`（301 到主分类页或父页面） | 没有直接替代，但有上级页面 |

---

## 14. SEO 引擎

### 14.1 数据归属

- **`seo_meta`**（每个本地化一条，草稿态）：title_override、description_override、canonical_override、robots_override、og_title_override、og_description_override、og_image_media_id、schema_extra（手工补充的 JSON-LD，仅开发者角色）、focus_keyword。
- **`seo_template`**（站点 × 语言 × 内容类型）：标题与描述模板，例如标题 `{title} | {siteName}`，描述取摘要并截断到 155 个字符。模板变量由 `SeoVariableProvider` 提供（产品可以提供 `{modelNo}`、`{primaryCategory}`、`{brand}`）。
- 发布时，SEO 的 `PublishContributor` 计算最终值 = 覆盖值 ?? 自动值，把最终值、自动值以及被覆盖的字段列表一起写入快照的 `seo` 部分。
- **自动值永远不会覆盖人工值**；后台可以“恢复为自动”。
- **翻译**：源语言的覆盖值（标题、描述、OG 标题与描述）作为翻译单元随内容翻译到目标语言（seo 模块实现 `TranslatableContentProvider`，见 3.7、5.6）；没有覆盖值的字段在目标语言中同样由该语言的模板自动生成。
- 后台 SEO 面板：搜索结果预览（按像素宽度估算桌面端与移动端截断）、字数统计、自动值占位提示。

### 14.2 页面头部输出

由 seo 模块的 `DeliveryContributor` 生成，Nuxt 直接输出：

- `<title>`、meta description、`<html lang dir>`；
- canonical：绝对 URL，默认指向自身，去除查询参数（分页参数除外）；
- robots meta；
- hreflang alternate 链接；
- Open Graph 与 Twitter Card；
- JSON-LD（`@graph` 形式，见 14.5）。

### 14.3 索引控制规则

| 场景 | 规则 |
|---|---|
| 预发布 / 测试环境 | 全站 `noindex` + Basic Auth + robots.txt `Disallow: /` |
| 预览与编辑器 | `noindex, nofollow` + `Cache-Control: no-store` |
| 站内搜索结果页 | `noindex, follow`（不在 robots.txt 中屏蔽，以便爬虫读到 noindex） |
| 列表分页 `?page=2` | canonical 指向自身，可以索引（不 canonical 到第 1 页） |
| 带单个筛选参数的列表 | `noindex, follow`，canonical 指向自身（保留筛选参数；不同时使用 noindex 与指向其他 URL 的 canonical，Google 视为相互矛盾的信号）；有价值的筛选组合应建成 TERM_PAGE 或 PAGE |
| 带多个筛选或排序参数 | `noindex, follow`，canonical 同样指向自身；筛选链接加 `rel="nofollow"`，避免爬虫陷阱 |
| 询盘成功页、下载门控页 | `noindex` |
| 公开的 PDF 附件 | 可以索引；留资文档没有直接 URL |
| 未翻译的语言（译文尚未生成或尚未发布） | 页面不存在（404），不输出该语言的 hreflang |
| 未对外公开的语言 | 前台 404（只能在后台与预览中查看）；不输出 hreflang，不进入 sitemap 与语言切换器（见 5.1） |

**robots.txt**：每个站点由模板生成，并允许站点管理员追加规则。默认内容：

```text
User-agent: *
Disallow: /api/
Disallow: /__preview/
Disallow: /__editor

Sitemap: https://www.example.com/sitemap.xml
```

站点可以按爬虫配置 AI 爬虫策略（如 GPTBot、ClaudeBot、Google-Extended、PerplexityBot 等），选择允许或禁止。

### 14.4 hreflang

- 由同一内容所有**已发布、且站点语言“对外公开”**的本地化生成，包含自身；因为来自同一集合，**相互引用天然成立**。已由 AI 翻译生成但所属语言尚未对外公开的本地化不输出 hreflang。
- hreflang 值默认只用语言代码：中文为 `zh`（可配置为 `zh-Hans`），英语 `en`、德语 `de`、法语 `fr`…；英语只做一个版本，同时服务美国与英国（见 5.4）。
- `x-default`：首页指向根路径 `/`（302 跳转到默认对外语言，符合 Google 对“自动跳转首页”的建议）；其他页面指向默认对外语言（`sys_site.default_locale`）的版本（若已发布）。
- canonical 必须指向本语言自身，禁止跨语言 canonical。
- 同时输出到 HTML 与 sitemap，两者来自同一数据源，保持一致。

### 14.5 Schema.org（JSON-LD）

由各模块实现 `SchemaOrgProvider`，SEO 模块组装为一个 `@graph`，节点之间用 `@id` 互相引用。

| 页面 | 输出 |
|---|---|
| 全站 | `Organization`（来自站点资料：名称、Logo、地址、联系方式、sameAs）、`WebSite` |
| 所有页面 | `WebPage`（或 `CollectionPage`、`AboutPage`、`ContactPage`）、`BreadcrumbList` |
| PRODUCT | `Product`：name、image、description、sku / mpn（型号）、brand、category、additionalProperty（规格）。没有价格时不输出 `Offer`，B2B 场景不追求价格富结果 |
| ARTICLE | `Article` / `BlogPosting`：headline、image、datePublished、dateModified、author（Person） |
| CASE_STUDY | `Article` |
| FAQ 组件 | `FAQPage`（注意：Google 已把 FAQ 富结果限定为权威的政府与医疗网站，输出仍有助于语义理解，但不应期待富结果） |
| 视频组件 | `VideoObject` |
| 联系页 | `ContactPage` + `Organization.contactPoint` |

- `WebSite` 不输出 `SearchAction`（Google 已于 2024 年停用站内搜索框富结果）。
- CI 中用 JSON Schema 校验各类型输出的 JSON-LD；SEO 健康检查必需属性是否齐全。

### 14.6 Sitemap

- `/sitemap.xml` 是 sitemap 索引，指向 `/sitemaps/{type}-{locale}-{n}.xml`，每个文件 ≤ 50,000 条且 ≤ 50 MB。
- 收录条件：已发布、可路由、所属语言对外公开、robots 为 index、canonical 指向自身。
- `lastmod` 取 `content_modified_at`（只在内容实际变化时更新，见 6.4）。
- 包含 `xhtml:link` hreflang 与 `image:image`（封面图、产品图集）。
- 生成方式：基于投影按需生成，缓存在 Redis，标签 `s{site}:sitemap`，发布事件触发失效；内容量很大的站点由任务预生成到对象存储。
- `SitemapProvider` 扩展点允许非内容类 URL 进入 sitemap。

### 14.7 搜索引擎推送

搜索引擎以 **Google 为主、Bing 为辅**。

- **Google**：依靠 sitemap 与 Search Console（Google Indexing API 只适用于招聘与直播类页面，不适用于普通页面）。
- **IndexNow**（主要用于 Bing 等支持 IndexNow 的搜索引擎）：发布、下线、路径变化时，按分钟批量推送（只推送对外公开语言的 URL）；密钥文件由 Nuxt 在 `/{key}.txt` 提供。
- **站点验证**：站点设置支持 Google Search Console 与 Bing Webmaster Tools 的验证 meta 标签字段（输出到首页 `<head>`）；也可以使用 DNS 验证（在运维手册中说明）。
- 【V2+】接入 Search Console API，把收录与表现数据拉回 SEO 面板。

### 14.8 内链引擎

| 来源 | 机制 | 模式 |
|---|---|---|
| 导航 | 菜单配置（见 16） | 手动 |
| 面包屑 | `BreadcrumbProvider` 自动生成（产品：首页 › 产品 › 主分类路径 › 产品） | 自动 |
| 相关内容 | Content Graph 推荐（模板的 related 区域、`related-content` 组件） | 每个本地化可选 `AUTO` / `MANUAL` / `DISABLED` |
| 正文链接 | 富文本中的 `content://{id}` 引用，渲染时解析为当前 URL；目标下线则降级为纯文本 | 手动 |
| 分类与应用落地页 | 自动列出该分类下的内容 | 自动 |
| 关键词自动锚文本 | 为指定关键词自动加链接（每页上限、每个目标只链一次、排除标题与已有链接） | 【V2+】 |

模式说明：`AUTO` = 人工关系优先，其余由 Graph 按评分补足；`MANUAL` = 只显示人工关系；`DISABLED` = 不显示相关内容。

**链接索引**：`seo_link`（source_localization_id、target_content_id、kind：NAV / BREADCRUMB / RELATED / BODY / COMPONENT），在发布后重建。用途：统计入站链接、发现孤立页面、展示“哪些页面链接到这里”、下线前的影响分析（`ContentImpactProvider`）。

### 14.9 SEO 健康检查

规则通过 `SeoRule` 扩展点注册：

```java
public interface SeoRule {
    String id();                         // "seo.title.length"
    Severity severity();                 // ERROR / WARNING / INFO
    Set<String> contentTypes();
    List<SeoIssue> check(SeoAuditContext ctx);   // 上下文包含快照、头部输出、链接索引
}
```

V1 规则：

| 规则 | 级别 | 说明 |
|---|---|---|
| 标题长度 | WARNING | 建议 30–60 个字符（按像素宽度估算 ≤ 580 px） |
| 描述长度 | WARNING | 建议 70–160 个字符 |
| 标题或描述重复 | ERROR / WARNING | 同站点、同语言内重复 |
| H1 数量 | ERROR | 恰好一个（模板保证；页面搭建时检查组件的 headingLevel） |
| 标题层级 | INFO | 不跳级 |
| 图片 alt | WARNING | 内容图片缺少当前语言的 alt |
| canonical | ERROR | 指向不可访问、重定向或 noindex 的 URL |
| 结构化数据 | ERROR | 缺少必需属性 |
| 孤立页面 | WARNING | 入站内链为 0（不计 sitemap） |
| 相关内容过少 | INFO | 相关内容少于 2 条 |
| 正文字数 | INFO | 文章少于 300 词（产品页不检查） |
| slug 质量 | INFO | 过长、包含停用词 |
| 引用已下线内容 | WARNING | 正文或组件引用了已下线的内容 |
| 翻译过期 | INFO | 本地化基于源语言的旧修订（如 `MANUAL` 模式的 `OUTDATED`，见 5.5） |
| 关键页面的机器译文未经审校 | INFO | 首页、主要产品与分类页等关键页面的目标语言译文中，仍有未经人工修订或确认的机器译文段落（来源为 `MACHINE`，见 5.6、5.8） |

- 评分 = 100 − Σ 扣分（ERROR 15、WARNING 5、INFO 1），最低为 0。
- 发布后异步检查单个页面；每晚做一次全站检查（重复检测需要全站数据）。结果保存在 `seo_audit`。
- 后台提供站点 SEO 看板：分数分布、问题排行、按分数筛选内容列表；编辑器发布前在侧栏展示问题（V1 不阻止发布）。

### 14.10 面向 AI 搜索【V2+】

`llms.txt`、更完整的实体结构化数据、AI 爬虫访问报表。V1 通过“语义化 HTML + 完整的 JSON-LD + 可配置的 AI 爬虫策略”打好基础。

---

## 15. 内容关系图谱（Content Graph）

### 15.1 模型

| 表 | 关键字段 |
|---|---|
| `grh_relation` | site_id、source_content_id、target_content_id、relation_type、origin（MANUAL / AUTO）、score、rank、pinned、reason（JSON）、computed_at；唯一键 (site_id, source, target, relation_type) |
| `grh_relation_type` | key、symmetric、inverse_key、allowed_source_types、allowed_target_types、label（中文）、module |
| `grh_config` | site_id、content_type、weights（JSON）、threshold、top_k |

- 关系建立在**语言无关的内容**上。渲染某种语言时，只显示在该语言已发布的目标；数量不足时按排名继续补足。
- 对称关系与互逆关系在同一事务中写入两个方向（互逆关系写入反向类型），查询时只需按 source 查询。
- 归属关系（属于某分类、适用于某应用）由分类体系表达，**不在图谱中重复存储**。

### 15.2 关系类型

| 类型 | 对称 | 反向类型 | 说明 |
|---|---|---|---|
| `RELATED` | 是 | — | 一般相关 |
| `RECOMMENDED` | 否 | — | 单向推荐，排序优先 |
| `USES` | 否 | `USED_IN` | 案例或文章使用了某产品 |
| `CASE_OF` | 否 | `HAS_CASE` | 案例属于某产品或应用 |
| `ACCESSORY_OF` | 否 | `HAS_ACCESSORY` | 配件 |
| `REPLACED_BY` | 否 | `REPLACES` | 替代型号 |
| `EXCLUDED` | 是 | — | 人工排除，阻止自动关联 |

V1.0 类型的对应：`BELONGS_TO`、`SUITABLE_FOR` → 分类体系；`PARENT`、`CHILD` → 页面层级与分类层级；`USED_IN`、`CASE_OF`、`RELATED`、`RECOMMENDED` 保留。模块可以通过 `RelationTypeProvider` 注册新类型。

### 15.3 V1 评分算法（可解释的规则评分）

**候选集**：与源内容至少共享一个发布态分类项的内容（通过 `cnt_published_term` 索引），同站点、可路由、未进回收站，排除自身与 `EXCLUDED`。按分类项稀有度截取，最多 2,000 个候选。

**评分**（默认权重，按站点、按内容类型可配置）：

| 因子 | 分值 | 上限 |
|---|---|---|
| 主产品分类相同 | 30 | 30 |
| 非主分类相同，或同属一个父分类 | 15 | 15 |
| 每个相同的应用 | 25 | 50 |
| 每个相同的行业 | 20 | 40 |
| 每个相同的标签 | 10 | 30 |
| 每个相同的认证 | 5 | 10 |
| 类型亲和（如 CASE_STUDY → PRODUCT） | 0–10 | 按配置 |
| 新鲜度（仅文章，一年内线性衰减） | 0–10 | 10 |

- 默认阈值 30；每个源内容按目标类型各保留前 20 条自动关系。
- **人工关系不参与评分**：始终置顶，按人工排序展示。
- `reason` 记录得分原因（如 `{"application": ["可穿戴设备"], "tag": ["低温"]}`），后台可以解释“为什么推荐”。
- 【V2+】通过 `SimilarityProvider` 扩展点增加关键词相似度（搜索引擎的相似文档能力）与语义相似度（向量检索）因子。

### 15.4 计算方式

- **触发**：`ContentPublishedEvent`（分类指派随发布生效）、`ContentUnpublishedEvent`、`ContentTrashedEvent`、`TermChangedEvent`（分类移动会影响“同属一个父分类”）、人工关系变化、配置变化。
- **增量**：同一内容的触发合并去抖 30 秒（应对批量导入）；重算源内容的候选与评分；分类重叠分是对称的，同时更新对方的列表（只更新排名可能受影响的候选）。
- **全量**：每晚低峰期按站点重建，计算完成后整体替换，并与增量结果比对作为校验。
- **性能目标**：1 万产品 + 5 千文章的站点全量重建 ≤ 10 分钟。

### 15.5 读取

```java
List<RelatedItem> related(long contentId, String locale, Map<String, Integer> mix, RelatedMode mode);
// mix 例：{"PRODUCT": 4, "CASE_STUDY": 2, "ARTICLE": 3}
```

人工关系置顶 → 自动关系按排名 → 过滤出该语言已发布的内容 → 从投影取卡片数据 → 返回结果，并附带缓存标签（每个卡片的 `c:{id}` 与源内容的 `g:{id}`）。

### 15.6 后台

内容编辑页的“关联”标签：手动添加、删除、排序；排除某条自动推荐；查看自动推荐列表及其得分与原因；设置模式（AUTO / MANUAL / DISABLED）。【V2+】图谱可视化。

---

## 16. 导航与站点资料

### 16.1 菜单

| 表 | 关键字段 |
|---|---|
| `nav_menu` | site_id、key（`header`、`footer-1`、`footer-2`…） |
| `nav_item` | menu_id、parent_id、sort、link_type（CONTENT / TERM / URL）、link_ref、open_in_new、mega 配置（列、推荐图片，取决于 Header 变体） |
| `nav_item_localization` | item_id、locale、label、visible |

- 菜单结构在各语言之间共享，标签与可见性按语言设置。
- **翻译**：菜单标签以中文录入，作为站点资源由 AI 翻译到各语言（经翻译记忆，全站译法一致，见 5.5）；目标语言的标签与可见性自动生成，可以人工修订（修订写回翻译记忆，见 5.6）。
- 链接到内容时，若目标在该语言未发布，该菜单项自动隐藏。
- V1 菜单保存即生效（不走发布流程），变更进入审计日志，并发布 `NavigationChangedEvent` 与 `TranslatableResourceChangedEvent`（触发翻译，见 5.7）。

### 16.2 面包屑

由 `BreadcrumbProvider` 按内容类型生成：

| 类型 | 路径 |
|---|---|
| PRODUCT | 首页 › 产品（站点配置的列表页）› 主分类的祖先路径 › 产品 |
| ARTICLE | 首页 › 博客 › 文章栏目 › 文章 |
| PAGE | 首页 › 父页面链 › 页面 |
| TERM_PAGE | 首页 › 分类祖先路径 › 分类 |

面包屑同时用于前台展示与 `BreadcrumbList` 结构化数据。

### 16.3 站点资料

由 platform-core 管理（每个站点一份）：

| 分组 | 字段 |
|---|---|
| 基本信息 | 公司名称、Logo（浅色 / 深色）、favicon 与应用图标、地址（可多个）、电话、邮箱、WhatsApp、社交账号、营业时间、成立年份 |
| 法定信息 | 法定名称（含法律形式，如 GmbH / Ltd.）、注册地址（可送达地址）、授权代表人、登记机关（如商业登记法院）与注册号、增值税号（VAT ID）、联系方式（可快速电子联系的邮箱，以及电话等第二联系渠道）、监管机关（经营活动需要许可时）、内容负责人（站点含新闻、博客等编辑内容时） |

- 文本类字段以中文录入，作为站点资源翻译到各语言（见 5.5），可以按语言人工修订。
- 法定信息用于自动生成 Imprint 页面（见 10.1）；具体内容由业主自行确认（见 32 章 Q14）。

使用方：`Organization` 结构化数据、Header / Footer、联系组件、通知邮件模板、Imprint 页面。

### 16.4 语言切换器

只列出站点中“对外公开”的语言（见 5.1）；只有一种对外公开的语言时（如 V1 默认只开放中文）不显示。链接到当前内容在这些语言中的已发布版本；某语言没有已发布的本地化时（包括译文尚未生成或尚未发布），链接到该语言的首页并加以标注，而不是链接到 404。

---

## 17. 表单与询盘

表单（form，L2）负责“收集并验证提交”；询盘（inquiry，L3）负责“把提交变成线索并跟进”。两者通过 `FormSubmittedEvent` 解耦。

### 17.1 表单定义

| 表 | 关键字段 |
|---|---|
| `frm_form` | site_id、key、purpose（INQUIRY / SAMPLE / DOWNLOAD / CONTACT / CUSTOM；【V2+】NEWSLETTER）、status、fields（JSON）、settings（JSON：反垃圾、附件、成功动作、是否显示营销同意框、常用国家、Turnstile） |
| `frm_form_localization` | form_id、locale、labels（JSON）、success_message、privacy_notice_text（简短告知，含隐私政策链接）、marketing_consent_text、privacy_notice_version（两段文本任一修改时递增）；每种语言一行，源语言（中文）由编辑录入，其他语言由翻译生成（见下） |
| `frm_form_notice_version` | form_id、locale、version、privacy_notice_text、marketing_consent_text、created_at；每个版本一条（含当前版本），告知或营销同意文本修改时追加，只追加不修改，用于追溯同意文本 |
| `frm_submission` | site_id、form_id、locale、payload（JSON）、attachment_ids、attribution（JSON）、privacy_notice_version（展示的告知版本）、marketing_consent、marketing_consent_at（可空）、gpc、opt_out_sale_share、spam_score、spam_verdict（ACCEPT / QUARANTINE / REJECT）、ip、ip_country、user_agent、created_at |

- 字段类型：文本、邮箱、电话（带国家区号，用 libphonenumber 校验，按 E.164 格式保存，如 `+4930123456`）、多行文本、单选、多选、国家（ISO 列表，按语言显示）、数字、勾选、附件、隐藏字段（上下文）、产品选择（由询价篮或当前产品自动填充）。
- 标准字段 key（映射到询盘）：`name`、`email`、`phone`、`company`、`country`、`job_title`、`message`、`quantity`、`website`。
- **国家与电话**：国家字段默认按访客 IP 所在国家预选（页面 HTML 有 CDN 缓存，国家随 formToken 一并返回，见 17.2）；电话按所选国家预填区号；国家列表可配置常用国家置顶。
- **隐私告知与营销同意**：表单只显示简短告知与隐私政策链接，不设强制勾选的同意框；营销同意为单独的、默认不勾选的可选框（表单设置决定是否显示）。依据见 17.11。
- 成功动作：显示提示语，或跳转到感谢页（`noindex`）。
- **多语言**：表单标签、选项、提示语、成功提示语、隐私告知与营销同意文本只用中文录入，作为站点资源由 translation 翻译成各目标语言（form 实现 `TranslatableResourceProvider`，见 3.7、5.7），译文写入对应语言的 `frm_form_localization`，可在翻译编辑器中人工修订（见 5.8）；告知或营销同意文本的译文变化同样递增该语言的 `privacy_notice_version`，并追加 `frm_form_notice_version`。

### 17.2 提交流程

```text
访客在 Nuxt 中打开带表单的页面
  1. 表单组件获取 formToken（签名：formKey、渲染时间、站点），用于最短填写时间校验与防重放；
     同时返回访客 IP 所在国家，用于预选国家字段
  2. 提交：字段（含可选的营销同意）+ 展示的告知版本 + GPC 与退出“出售 / 共享”状态 + 附件 ID + 归因数据 + Turnstile 令牌 + Idempotency-Key
       │ POST /api/public/v1/form/forms/{key}/submissions
       ▼
form 模块（同步）
  3. 限流（IP、邮箱、站点总量）→ 超限返回 429
  4. 校验 Turnstile（服务端 siteverify）、formToken、honeypot、最短填写时间（≥ 3 秒）
  5. 按字段定义校验；校验附件属于本次会话
  6. 反垃圾评分 → ACCEPT / QUARANTINE / REJECT（REJECT 也返回“成功”，不暴露判定结果）
  7. 保存 frm_submission，并在同一事务中发布 FormSubmittedEvent
  8. 返回 201 与成功动作；DOWNLOAD 类型（非 REJECT）同时返回签名下载链接
  9. 前端向 dataLayer 推送 generate_lead 事件
异步
 10. inquiry 监听 FormSubmittedEvent：创建线索（从提交记录复制隐私字段到 inq_inquiry）→ 去重 → 分配
 11. inquiry 调用 NotificationApi：通知负责人（中文）；按询盘的 locale 向客户发送对应语言的自动回复（仅 ACCEPT，含预计回复时间，见 17.7、18）
```

### 17.3 附件

- 预上传接口 `POST /api/public/v1/form/attachments`：需要有效的 formToken，单独限流。
- 存入私有桶，状态为临时；24 小时内未关联到提交则自动清除。
- 类型白名单：pdf、jpg、png、dwg、dxf、step / stp、igs、zip；单个 ≤ 20 MB，最多 5 个。
- 异步病毒扫描，扫描通过前员工无法下载；员工通过预签名链接下载，并记录审计日志。

### 17.4 反垃圾

| 层 | 手段 |
|---|---|
| 网络 | 限流（Bucket4j + Redis）：同 IP 5 次 / 10 分钟、同邮箱 10 次 / 天；站点提交量突增时告警 |
| 人机 | Turnstile（站点可关闭）；honeypot 隐藏字段；最短填写时间；formToken 防重放 |
| 内容 | 链接数量、垃圾词库、一次性邮箱域名库、邮箱 MX 记录（异步） |
| 信誉 | IP / 邮箱 / 域名黑名单（后台维护）；可选的国家屏蔽 |
| 判定 | 规则加权得分：< 30 ACCEPT；30–70 QUARANTINE（进入“待审”，不发通知）；> 70 REJECT（保留 30 天后清除） |
| 【V2+】 | AI 分类器作为额外的评分因子 |

### 17.5 询盘模型

```text
inq_inquiry
├── id、site_id、no（如 INQ-20261006-0001）、type（INQUIRY / SAMPLE / DOWNLOAD / CONTACT）
├── status、priority、owner_user_id、assigned_at、first_contacted_at、closed_at、close_reason
├── 联系人：name、email、phone、whatsapp、company、job_title、country、website
├── message、quantity_text、locale（客户提交时的语言，即提交表单所在页面的语言；自动回复使用该语言）
├── 留言译文：message_translated（留言的中文译文，可空）、message_translated_at（翻译时间，可空），见 17.10
├── submission_id、duplicate_of_id、spam_verdict、is_test
├── 归因：landing_url、referrer、utm_source / medium / campaign / term / content、gclid / msclkid / fbclid、
│        first_touch（JSON）、last_touch（JSON）、viewed_content_ids、ip_country、device
└── 隐私：privacy_notice_version（展示的告知版本）、marketing_consent、marketing_consent_at（可空）、gpc、opt_out_sale_share

inq_inquiry_item     inquiry_id、content_id、model_id、title_snapshot、model_no_snapshot、url_snapshot、quantity
inq_activity         inquiry_id、kind（NOTE / STATUS / ASSIGN / EMAIL_SENT / CALL）、content、created_by、created_at
inq_attachment       inquiry_id、media_id
inq_assignment_rule  site_id、priority、name、conditions（JSON）、assignee_type（USER / GROUP_ROUND_ROBIN）、assignee_ref、enabled
inq_sales_group      id、name；inq_sales_group_member：group_id、user_id、active
```

询盘明细保存产品标题、型号、URL 的**快照**，因为产品之后可能改名或下线。

### 17.6 状态机

```text
NEW ──分配──▶ ASSIGNED ──首次联系──▶ CONTACTED ──确认有效──▶ QUALIFIED ──【V2】转为 CRM 商机──▶ CONVERTED
 │               │                     │                     │
 └───────────────┴─────────────────────┴─────────────────────┴──▶ CLOSED（原因：无效 / 垃圾 / 重复 / 无回复 / 其他）

CLOSED ──重新打开──▶ ASSIGNED
```

报价、谈判、成交、丢单属于 CRM 商机的状态，不放在询盘中。`CONVERTED` 为 V2 的 CRM 模块预留。

### 17.7 分配、去重与 SLA

- **分配规则**按优先级依次匹配。条件：国家或地区、询盘类型、产品分类（通过 `TaxonomyApi` 查询询盘明细中内容的分类）、语言、来源（utm_source）。负责人：指定用户，或销售组内轮询（跳过停用的成员）。都不匹配时使用站点的默认负责人。支持手动改派。
- **去重**：同一邮箱（规范化后）7 天内再次提交时，仍创建新询盘，但通过 `duplicate_of_id` 关联，并分配给同一负责人，在详情页中合并展示时间线。
- **SLA**：销售在中国时区，客户在欧美，因此 SLA 按工作时间计算：站点配置工作时区（如 `Asia/Shanghai`）、工作日与节假日（含调休补班日）；分配后超过 X 个工作小时（站点配置，默认 8 个工作小时，即 1 个工作日）未进入 CONTACTED，提醒负责人与主管。
- **预计回复时间**：客户自动回复中告知预计回复时间（如“1 个工作日内”），文案写在自动回复模板中，以中文编写，随模板翻译成客户语言（见 18）。

### 17.8 询价篮

- 访客可以把多个产品或型号加入询价篮（保存在浏览器 localStorage，无需登录）。
- 询盘表单中的“产品选择”字段自动带入询价篮内容，提交后成为询盘明细。

### 17.9 来源归因

- Nuxt 插件在首次访问时记录首次触达：落地页、referrer、UTM、点击 ID（gclid / msclkid / fbclid）、时间；每次会话记录末次触达；会话内记录最近浏览的 10 个内容。
- **按地区遵循同意**（同意模式见 20.2；未获同意时的具体做法由法务确认）：
  - 明示同意地区：访客同意相应类别之前，不在 Cookie、localStorage、sessionStorage 中写入归因数据（ePrivacy 对这些存储方式同样适用）；只在当前页面的内存中保留本页的落地页、referrer 与 UTM，随表单提交；访客同意分析类后，才写入首次触达（第一方 Cookie，90 天）以及末次触达与浏览记录（会话内）。
  - “告知 + 可退出”地区：默认写入，访客退出分析类后删除。
  - 点击 ID（gclid / msclkid / fbclid）属于广告标识，归入营销类：明示同意地区只在访客同意营销类后保存；“告知 + 可退出”地区在访客退出营销类或收到 GPC 信号时删除。
- 服务端校验并截断（URL ≤ 2048 个字符，UTM ≤ 200 个字符）。
- IP 通过本地 GeoIP 数据库解析为国家；原始 IP 保留 30 天（用于反欺诈）后置空；应用日志中的 IP 截断记录（IPv4 最后一段置零，IPv6 保留前 48 位，见 27.4）。

### 17.10 后台功能

- 列表：按状态、负责人、类型、国家、来源、日期、产品筛选；批量分配、批量关闭。
- 详情：联系人、明细、归因、附件、时间线、备注、状态操作；“复制邮箱 / mailto”并记录跟进活动（系统内直接发邮件属于 V2 的 CRM）。
- **询盘留言一键翻译为中文**（见 1.3 S13、5.7）：详情页提供“翻译为中文”按钮；inquiry 调用 translation 在 platform-api 中的公开服务接口（`TranslationApi.translateText`）翻译留言，由 inquiry 把译文保存到 `inq_inquiry.message_translated`，并记录 `message_translated_at`（translation 是 L2，不直接写询盘表）；再次查看直接显示已保存的译文，不重复调用 AI。
  - 站点可关闭此功能；关闭时，或 translation 模块停用时，不显示该按钮。
  - 询盘译文不写入翻译记忆（属于个人数据，不复用）；留言按纯文本处理，不作为指令（提示词注入防护见 26.3；发送给 AI 服务商的数据范围见 26.8）。
  - 留言译文与留言同属询盘的个人数据，保留期、导出与删除与留言一致（见 17.11）。
- 导出 xlsx：需要 `inquiry:inquiry:export` 权限，记录审计日志。
- 报表：按来源 / 媒介 / 活动 / 落地页 / 国家 / 产品统计询盘数；首次响应时间（按工作时间计算，见 17.7）。

### 17.11 隐私

- **合法性基础**（由法务确认，见 32 章 Q13）：询价人本人为合同一方时，回复询盘通常可依据 GDPR 第 6 条第 1 款 (b) 项（应数据主体要求在订立合同前采取的步骤）；询价人代表所在企业时，通常依据第 6 条第 1 款 (f) 项（正当利益）。下载留资（DOWNLOAD）与一般联系（CONTACT）线索的后续跟进通常不属于订立合同前的步骤，其依据由法务确认。无论采用哪种依据，都**不需要强制勾选同意**；向线索主动发送推广邮件须遵守 ePrivacy 各国转化法（各国对企业间推广邮件的要求不同，如德国同样要求事先同意），以 `marketing_consent` 为准。表单采用“隐私告知 + 可选的营销同意”：
  - 隐私告知：只显示简短告知与隐私政策链接（指向站点的 Privacy Policy 页面，见 10.1）；告知文本有版本号，询盘保存 `privacy_notice_version`；
  - 营销同意：单独的、默认不勾选的同意框；勾选时保存 `marketing_consent` 与 `marketing_consent_at`，同意文本的版本通过 `privacy_notice_version` 追溯（见 17.1）；
  - 【V2+】营销同意按“站点 + 规范化邮箱”维护当前状态与变更历史；撤回或反对时写入退订名单，任何营销发送前都要检查该名单；询盘表单中勾选的营销同意，在用于推广邮件前先通过确认邮件完成双重确认（与 Newsletter 共用同一机制，见 18）。V1 不在系统内发送营销邮件。
- 保留期：REJECT 与垃圾询盘 30 天；已关闭的询盘按站点配置保留（默认 3 年）后匿名化；附件同步删除；`frm_submission` 的保留期与询盘一致；`ntf_message` 发送成功后按较短期限（如 90 天）清理。
- **数据主体请求**：支持访问、更正、删除、可携带、反对、限制处理，以及 CCPA 的退出“出售 / 共享”。答复时限：GDPR / UK GDPR 自收到请求起 1 个月内，必要时可延长两个月（须在 1 个月内告知理由）；CCPA 的访问、删除、更正请求 45 个日历日内答复，必要时可再延长 45 天（须在首个 45 天内告知），并在 10 个工作日内确认收到；退出“出售 / 共享”请求 15 个工作日内处理。具体时限以法务确认为准。后台工具由 platform-core 提供，通过 `PersonalDataProvider` 扩展点收集各模块保存的个人数据（见 3.7）；属于系统管理接口（跨站点），需要 `system:dsr:manage` 权限（高危权限，强审计）：
  - 按邮箱跨站点查找；导出 JSON（访问、可携带）；更正联系字段；匿名化（删除：替换联系字段，保留统计；覆盖范围见下）；反对：撤销营销同意；
  - 删除的覆盖范围：同一邮箱在所有站点的 `inq_inquiry` 联系字段、`inq_activity` 的备注内容、`frm_submission`（payload、ip、user_agent）、表单与询盘附件、`ntf_message`（收件人与 payload）；邮件服务商的发送记录通过其接口删除，或按其保留期自动过期（写入处理者清单）。备份不做单条删除：记录删除台账，从备份恢复后重新执行台账中的删除；
  - `sys_dsr_request`（系统级）记录请求、核验、处理与答复时间：type（ACCESS / RECTIFY / ERASE / PORTABILITY / OBJECT / RESTRICT / OPT_OUT_SALE_SHARE）、jurisdiction（GDPR / UK_GDPR / CCPA）、subject_email、received_at、acknowledged_at、due_at（按法域与延期计算）、extended_until、extension_reason、verified_at、verification_method、handled_by、completed_at、replied_at、status；临近 due_at 时提醒处理人。
- **受理与身份核验**：Privacy Policy 中公布受理渠道（专用邮箱；是否另设请求表单，以及美国是否需要免费电话等其他渠道，由法务确认）。核验默认向记录中的邮箱发送一次性确认链接，核验通过前不导出、不删除；导出文件只通过短期有效的私有下载链接发送到记录中的邮箱，不发送到请求中新提供的地址。CCPA 授权代理人代为提交时，需要本人的书面授权并核验本人身份。无法核验时按法定方式答复，并记录原因。
- 日志中脱敏 PII；按数据范围控制访问；导出操作记录审计日志。
- 处理活动记录（ROPA）、处理者清单与 DPA 见 26.8；待法务确认的事项（GDPR 第 27 条欧盟代表与英国代表、询盘与下载留资的合法性基础、中国大陆销售人员访问欧盟个人数据及其他跨境传输的合规安排、CCPA 是否适用、加拿大的要求、同意记录保留期限）见 32 章 Q13。

---

## 18. 通知（Notification）

| 表 | 关键字段 |
|---|---|
| `ntf_template` | site_id、key（如 `inquiry.assigned.staff`、`inquiry.autoreply.customer`、`inquiry.sla.reminder`、`content.review.requested`、`translation.outdated`、`translation.failed`、`download.link`）、channel、locale（员工通知只有 `zh-CN`；客户邮件模板的源语言为 `zh-CN`，其他语言为译文）、subject、body |
| `ntf_message` | 发件箱：channel、recipient、template_key、locale、payload、status（PENDING / SENT / FAILED）、attempts、next_attempt_at、provider_message_id、error |
| `ntf_webhook` | site_id、url、events、secret、enabled |

- **调用**：`NotificationApi.send(request)` 在调用方的事务中写入发件箱，由后台任务投递，确保“询盘保存成功就一定会发通知”。
- **重试**：指数退避，最多 6 次；最终失败时告警（见 27.4）。
- **模板**：使用会自动转义 HTML 的模板引擎；邮件外层布局带站点品牌；模块通过 `NotificationTemplateProvider` 注册默认模板（中文），站点可以修改。
- **语言**：
  - 发给员工的通知（分配、SLA 提醒、审核请求、翻译过期与翻译失败等）使用中文（后台用户只用简体中文，见 22.5）；
  - 发给客户的邮件模板（自动回复、下载链接等）以中文编写，作为站点资源由 translation 翻译成各语言（notification 实现 `TranslatableResourceProvider`，见 3.7、5.7），可在翻译编辑器中人工修订（见 5.8）；发送时按询盘或提交记录的 `locale`（客户提交时的语言）选择对应语言的模板。
- **邮件**：V1 使用 SMTP 或服务商 API（兼容主流邮件服务）；优先选择提供欧盟数据区域的服务商（如 SES 的欧盟区域、Mailgun EU、Brevo，见 2.4、ADR-015）；发件域名必须配置 SPF、DKIM、DMARC（写入运维手册）；客户自动回复的 Reply-To 设为负责人邮箱。
- **Webhook**：按站点配置订阅的事件；请求带 HMAC 签名头并支持重试；可通过中间服务对接企业微信、飞书、Slack 等。载荷默认只含事件类型、询盘编号与后台链接，不含联系人信息；需要包含联系人信息时（尤其是接收方在欧盟以外），是否允许由法务确认（见 32 章 Q13）；Webhook 接收方纳入处理者清单（见 26.8）。
- 【V2+】通过 `NotificationChannel` 扩展点增加 WhatsApp、企业微信、短信等渠道；退信处理。
- 【V2+】营销邮件（Newsletter）：以订阅者事先同意为前提（欧盟为 ePrivacy 各国转化法，英国为 PECR），采用双重确认（double opt-in，德国用于证明同意的通行做法）；加拿大需符合 CASL；美国需符合 CAN-SPAM（发件信息与主题不得误导、标明广告性质、提供退订方式并在 10 个工作日内生效、带发件人有效的实际邮寄地址）；大批量发送还需满足 Gmail、Yahoo 等对批量发件人的要求（如一键退订 `List-Unsubscribe-Post`）。具体要求由法务确认。

---

## 19. 站内搜索（Search）

- **索引**：每个站点的每种语言一个 Meilisearch 索引，索引名为 `s{site}_{locale}_content`（如 `s1_de_content`）。文档由各类型的 `SearchDocumentProvider` 在发布时根据快照构建：`id`、`type`、`title`、`summary`、`body_text`（去除标记，≤ 10,000 个字符）、`terms`（ID 与名称）、`attr.*`（分面字段）、`model_nos`、`url`、`cover`、`published_at`。
- **配置**：
  - 搜索字段优先级：title > model_nos > summary > terms > body_text；
  - 型号字段关闭拼写容错；
  - **中文分词**：中文索引使用 Meilisearch 内置的中文分词，不另装分词插件；索引设置中通过 `localizedAttributes` 显式声明中文（`cmn`），不依赖自动语言识别；
  - 按语言配置同义词；
  - 可筛选字段：type、terms、`attr.*`；可排序字段：published_at、sort。
- **用途**：
  - 站内搜索页（`noindex`）；
  - 分类落地页的产品筛选：SSR 首屏输出无筛选的默认列表，筛选在客户端通过公开搜索接口完成，URL 同步查询参数（索引规则见 14.3）。
- **一致性**：事件驱动的增量更新（按 ID + 修订号幂等 upsert）；每晚做一次数量与哈希比对；提供全量重建命令。**Meilisearch 是可重建的读模型，不需要备份**。
- **降级**：搜索服务不可用时，分类列表降级为基于 MySQL 投影的无分面列表；搜索页显示友好的错误提示。
- **安全**：前台查询经后端代理，不暴露主密钥；【V2+】使用 Meilisearch 签发的受限搜索令牌（限定索引与过滤条件、短期有效）让浏览器直连。

---

## 20. 统计追踪与隐私同意

### 20.1 统计配置

`trk_setting`（按站点）：统计服务商（GA4，或欧盟托管的 Matomo、Plausible 等）及其 Measurement ID 或站点地址、GTM 容器 ID、Google Ads 转化 ID、Meta Pixel ID、LinkedIn Partner ID、自定义脚本（head / body，按同意类别加载）。

- **统计服务商可切换**：GA4 在欧盟的使用以访客事先同意为前提；跨境传输目前依靠 Google 的 EU-US Data Privacy Framework（DPF）认证（英国数据适用 DPF 的英国扩展）及标准合同条款，由法务确认。DPF 存在被欧盟法院推翻的风险，因此本模块通过配置支持切换到欧盟托管的统计工具（如 Matomo、Plausible），不写死 GA4；标准转化事件（见 20.3）按所选服务商的接口上报。
- 使用 GTM 时，其他标签建议在 GTM 内管理，避免重复加载；本模块只负责注入 GTM、同意默认值与 dataLayer 事件。
- 自定义脚本需要 `tracking:script:edit` 权限（开发者角色），修改时记录差异审计；CSP 白名单按已配置的服务商自动更新。

### 20.2 同意管理

- Cookie 横幅组件（主题提供变体），分类：必要、偏好、分析、营销。
- **横幅文案**（标题、说明、按钮、类别说明、“Cookie 设置”与退出链接文字）只用中文录入，作为站点资源由 translation 翻译成各语言（tracking 实现 `TranslatableResourceProvider`，见 3.7、5.7），可在翻译编辑器中人工修订；法规规定的固定文字（如“Do Not Sell or Share My Personal Information”“Your Privacy Choices”）通过术语表固定译法（见 5.8）。
- **按访客地区确定同意模式**：

| 地区 | 模式 | 要求 |
|---|---|---|
| 欧洲经济区（GDPR、ePrivacy 指令的各国转化法）、英国（UK GDPR、PECR）、瑞士（瑞士联邦数据保护法 FADP、电信法） | 明示同意（opt-in） | 非必要 Cookie 必须事先同意后才加载；“全部拒绝”与“全部接受”同样显眼；类别不预先勾选；可随时撤回（页脚“Cookie 设置”） |
| 美国（CCPA/CPRA 及其他州隐私法） | 告知 + 可退出（站点可配置为与欧盟相同的明示同意模式） | 横幅告知并提供退出入口；识别并遵守 Global Privacy Control（GPC）信号（浏览器端读取 `navigator.globalPrivacyControl`），视为退出营销类 Cookie 与“出售 / 共享”；页脚可配置“Do Not Sell or Share My Personal Information”或“Your Privacy Choices”链接（后者须附加法规规定的退出图标），点击后进入退出“出售 / 共享”的设置：立即停止营销类标签并记录退出状态（Cookie 横幅本身不能代替退出“出售 / 共享”的方式）。退出状态（含 GPC）同样适用于同一浏览器关联的线索：表单提交时把 `gpc` 与 `opt_out_sale_share` 一并保存到询盘（见 17.5）；向广告平台回传数据（如【V2+】离线转化导入、服务端 GTM 与 Measurement Protocol）时排除已退出的访客。分析类工具是否构成“出售 / 共享”（如 GA4 开启 Google 信号或关联 Google Ads）由法务确认 |
| 加拿大（PIPEDA、魁北克第 25 号法律） | 由法务确认；确认前按明示同意处理 | 魁北克对用于识别、定位或画像的技术有专门的告知与启用要求 |
| 其他地区 | 可配置 | 选择明示同意或告知 + 可退出 |

是否达到 CCPA 适用门槛由法务确认（见 32 章 Q13）；即使不适用，也按上表实现（B2B 联系人数据自 2023 年起不再豁免）。

瑞士法律对 Cookie 总体采用告知 + 可拒绝的模式，此处按明示同意处理，属于从严的统一实现；英国 PECR 已由《2025 年数据（使用与访问）法》修订，部分统计类 Cookie 的同意豁免是否适用、何时生效，由法务确认。

- **访客地区判定**：内容页 HTML 有 CDN 缓存（见 21.3），访客地区不写入缓存的 HTML，也不按地区区分缓存；浏览器端在页面加载时通过不缓存的轻量请求获取地区（由 CDN 的地理位置信息提供，如 Cloudflare 的 `CF-IPCountry` 请求头，可由边缘函数直接返回），据此决定横幅模式与 Consent Mode 默认值，判定完成前不加载非必要脚本。地区未知、判定失败或超时时，一律按明示同意处理。

- **同意记录**（作为同意证明）：访客每次选择（接受、拒绝、修改、撤回）都通过公开接口（见 24.4）写入 `trk_consent_log`：site_id、consent_id（随机生成的同意 ID，与同意 Cookie 中的 ID 一致；属于假名标识，仍按个人数据管理）、action（ACCEPT_ALL / REJECT_ALL / CUSTOM / WITHDRAW / OPT_OUT_SALE_SHARE / GPC）、source（横幅 / Cookie 设置 / 退出链接 / GPC 自动）、text_version（同意文本版本）、locale（展示的语言）、ui_version（横幅版式版本）、categories（每个类别的选择结果，含拒绝项）、region（访客地区）、gpc（是否收到 GPC 信号）、created_at、expires_at。保留期限由法务确认（默认 3 年，见 32 章 Q13）。
- 支持 Google Consent Mode v2（`ad_storage`、`analytics_storage`、`ad_user_data`、`ad_personalization`）：`default` 命令必须在任何 Google 标签之前执行。欧洲经济区、英国、瑞士默认全部 `denied`，并采用基本模式（获得同意前不加载 Google 标签，也不发送无 Cookie 请求）；是否改用高级模式、GTM 容器能否在同意前加载，由法务确认。“告知 + 可退出”地区：页面加载时已检测到 GPC 的，直接在 `default` 中把 `ad_storage`、`ad_user_data`、`ad_personalization` 设为 `denied`；访客之后退出时用 `update` 更新为 `denied`。
- 同意结果保存在第一方 Cookie 中（带同意 ID 与版本号，有效期 6–12 个月）；隐私政策或同意文本版本变化时重新征求；页脚提供“Cookie 设置”入口。
- 也可以关闭内置横幅，改用第三方 CMP（如 Cookiebot、OneTrust）；此时同意记录与 GPC 由所选 CMP 负责，需确认其满足上述要求。

### 20.3 标准转化事件

| 事件 | 触发 | 参数 |
|---|---|---|
| `generate_lead` | 表单提交成功 | form_key、form_purpose、content_type、content_id |
| `file_download` | 下载文档 | file_name、content_id、gated |
| `contact_click` | 点击邮箱、电话、WhatsApp | channel |
| `add_to_inquiry` | 加入询价篮 | content_id |
| `search` | 站内搜索 | search_term |
| `view_item` | 打开产品详情页（可选） | content_id、category |

- 第三方脚本全部异步、延后加载，并且只在获得对应类别的同意后加载（“告知 + 可退出”地区：访客未退出该类别时加载；收到 GPC 信号时不加载营销类）。
- 【V2+】服务端 GTM，以及通过 GA4 Measurement Protocol 上报服务端转化。

---

## 21. 前台网站（Nuxt）

### 21.1 目录结构

```text
frontend/apps/website/
├── app/
│   ├── app.vue
│   ├── pages/
│   │   ├── [...path].vue          # 唯一的内容路由：调用 Delivery resolve
│   │   ├── __preview/[token].vue  # 预览
│   │   ├── __editor.vue           # 编辑器画布（只允许被 Admin 的 iframe 嵌入）
│   │   └── __gallery.vue          # 组件画廊（非生产环境）
│   ├── renderer/                  # LayoutRenderer、SectionRenderer、RichTextRenderer、TemplateResolver
│   ├── composables/               # useSite、useDelivery、useConsent、useAttribution、useInquiryCart
│   ├── plugins/                   # 归因采集、同意管理、追踪
│   └── error.vue                  # 404 / 410 / 503 页面
├── server/
│   ├── routes/                    # sitemap.xml、sitemaps/[file]、robots.txt、IndexNow 密钥文件
│   └── middleware/                # 安全头、请求 ID
└── nuxt.config.ts
```

主题、组件 Schema、Token、API 客户端等共享代码在 `frontend/packages/` 中（见 29.1）。

### 21.2 请求处理流程

```text
GET https://www.example.com/de/produkte/li-ion-pack-x200
 → CDN 命中：直接返回 HTML
 → 未命中：Nuxt 服务端
     1. 中间件：安全头、请求 ID
     2. [...path].vue 调用 GET /api/public/v1/delivery/resolve?path=/de/produkte/li-ion-pack-x200
        （请求头：X-Site-Host、X-Internal-Token；Delivery 结果在后端 Redis 中缓存）
     3. 获取站点引导数据（导航、设置、Token 覆盖、界面文案、同意与追踪配置；Nuxt 进程内缓存 ≤ 60 秒）
     4. kind = REDIRECT → 服务端 301 / 308；GONE → 410 页面；NOT_FOUND → 404 页面
     5. kind = CONTENT → 选择主题 → 模板 → 渲染布局（组件变体按需异步加载）
     6. 输出 HTML，响应头带 Cache-Control 与 Cache-Tag（来自 Delivery 的 cache.tags）
```

### 21.3 渲染策略

| 页面 | 策略 | 缓存 |
|---|---|---|
| 内容页（产品、文章、页面、分类） | SSR | CDN：`s-maxage=86400, stale-while-revalidate=604800, stale-if-error=604800`；按 Cache-Tag 清除 |
| 带筛选参数的分类页 | SSR 首屏 + 客户端筛选 | 不同查询参数分别缓存；`noindex` |
| 站内搜索 | SSR 外壳 + 客户端查询 | `s-maxage=60` |
| sitemap、robots | 服务端路由 | `s-maxage=3600` + Cache-Tag |
| 预览、编辑器 | SSR / CSR | `no-store` |
| 404 | SSR | `s-maxage=60` |
| 5xx | SSR | 不缓存 |

- 浏览器端：HTML 使用 `max-age=0, must-revalidate`（CDN 使用 `s-maxage`）；带哈希的静态资源使用 `max-age=31536000, immutable`。
- **不使用 SSG**：发布事件 + 按标签清除 CDN 已经提供了类似 ISR 的新鲜度，无需构建时生成页面。

### 21.4 性能

- **按需水合**：利用 Vue 3.5 / Nuxt 的懒水合，静态区块不水合或进入视口时再水合；只有表单、标签页、图集、同意横幅等交互组件水合。
- 预加载 LCP 图片与主字体；第三方脚本不阻塞渲染。
- 内部链接的预取改为悬停时触发，避免可视区预取造成的大量请求。
- 在 CI 中用 Lighthouse CI 对代表性页面执行性能预算（见 1.5、28）。

### 21.5 故障处理

- Delivery 不可用时：CDN 通过 `stale-if-error` 继续提供旧页面；没有缓存时返回 **503 + Retry-After** 维护页，**绝不返回 404**，以免页面被搜索引擎移出索引。
- 单个数据源失败：该组件渲染为空，页面其余部分正常。

### 21.6 安全头

CSP（基于 nonce）、HSTS、`X-Content-Type-Options: nosniff`、`Referrer-Policy: strict-origin-when-cross-origin`、`Permissions-Policy`；`frame-ancestors 'self'`，其中 `/__editor` 只允许后台域名嵌入。

### 21.7 多语言

- **界面文案**来自站点引导数据（已按当前语言翻译，见 5.4、5.7）；前台渲染时不调用 AI。
- 主题中的固定文案（按钮、提示语、无结果提示等）只能写成文案键（如 `form.submit`），由主题提供默认中文文案，站点可以覆盖；**不得在组件中硬编码中文或英文**。
- 设置 `<html lang dir>`；日期、数字使用 `Intl` 按语言格式化。

---

## 22. 管理后台（Admin）

### 22.1 目录结构

```text
frontend/apps/admin/src/
├── core/        # HTTP（openapi-fetch + CSRF + 统一错误处理）、认证、布局、路由、权限指令 v-perm、站点切换
├── modules/     # 与后端模块一一对应
│   ├── content/ product/ article/ page/ media/ taxonomy/ seo/ graph/
│   ├── inquiry/ form/ navigation/ tracking/ translation/ system/
│   └── product/
│       ├── index.ts      # defineAdminModule({ id: 'product', routes, contentTabs, widgets })
│       ├── views/  components/  api/  stores/
├── shared/      # SchemaForm、MediaPicker、ContentPicker、TermPicker、RichTextEditor、
│                # LocaleTabs、RevisionHistory、SeoPanel、RelationPanel
└── editor/      # 页面编辑器
```

### 22.2 启动与动态菜单

- `GET /api/admin/v1/system/bootstrap` 返回：当前用户、权限、可访问的站点、当前站点启用的模块、菜单（来自各模块 `module.json`，已按权限与启用状态过滤）、功能开关。
- 所有模块的代码都编译在 Admin 中（按模块分包、懒加载）；路由器只注册已启用且有权限的模块路由。
- 安装 CRM 等新模块后，菜单由模块描述符声明，**Core 不需要修改菜单代码**。

### 22.3 内容编辑框架

所有内容类型共用一个编辑外壳：

- 顶栏：状态徽标、语言切换（每种语言显示翻译模式与翻译状态：`QUEUED` / `TRANSLATED` / `NEEDS_REVIEW` / `FAILED` / `OUTDATED`，见 5.5）、保存、预览、提交审核、发布 / 定时发布、历史修订。
- `AUTO` 模式的目标语言本地化中，文本只能在“翻译”标签中按段修订（见 5.5）。
- 标签页由各模块注册（与后端扩展点对应）：

```ts
registerContentTab({
  module: 'seo',
  key: 'seo',
  contentTypes: '*',
  permission: 'seo:meta:edit',
  component: () => import('./SeoTab.vue'),
})
```

例如：SEO 标签由 seo 模块提供，关联标签由 graph 模块提供，页面插槽标签由 page 模块提供，“翻译”标签由 translation 模块提供。

**“翻译”标签**（即翻译编辑器，见 5.8）：按段并排显示源语言与目标语言，并显示每段来源（记忆命中 / 机器 / 人工）与校验结果；可修订（写回翻译记忆，来源 `HUMAN`）、确认（`REVIEWED`）、单段重译；`MANUAL` 模式的本地化列出源语言变化的字段。

### 22.4 Schema 驱动的表单

`SchemaForm` 根据 JSON Schema 中的 `x-editor` 渲染控件：`text`、`textarea`、`richtext`、`richtext-inline`、`media`、`content-ref`、`term-ref`、`select`、`color-token`、`spacing-token`、`link`、`repeater`（数组）、`group`（对象）、`data-source`。组件属性、模块设置、数据源参数都使用它。

### 22.5 体验细节

自动保存草稿、离开前提示未保存的修改、常用快捷键、批量操作、列表保存筛选条件、后台界面只提供简体中文（不做语言切换，界面文案集中在常量 / 字典文件中，见 2.4）。

### 22.6 网络访问

后台与 Admin API 同源（`admin.example.com`，`/api/admin` 由反向代理转发），Cookie 使用 SameSite，无需 CORS。源站在欧盟（见 ADR-015），后台用户主要在中国大陆，访问延迟较高：后台静态资源走 CDN，接口响应尽量精简，必要时使用加速线路（见 27.1；风险见 31）。

---

## 23. 缓存架构

### 23.1 缓存层

| 层 | 内容 | 失效方式 |
|---|---|---|
| 浏览器 | 静态资源（文件名带哈希，缓存 1 年） | 文件名变化 |
| CDN | HTML、图片、sitemap | Cache-Tag 清除 + TTL + stale-while-revalidate |
| Nuxt | 站点引导数据（进程内，≤ 60 秒） | TTL |
| Redis | Delivery 结果、站点引导数据、路由、权限、设置 | 标签索引清除 |
| 应用内存 | 扩展点注册表、Schema、域名到站点的映射 | Redis Pub/Sub 广播 |

Nuxt 层不缓存 HTML：HTML 缓存交给 CDN，Nuxt 保持无状态，便于扩展与排查问题。

### 23.2 缓存标签

| 标签 | 含义 | 由谁添加 |
|---|---|---|
| `s{site}` | 站点全部 | 所有响应 |
| `s{site}:c:{contentId}` | 某内容（作为页面主体，或以卡片、链接形式出现） | delivery（主体 + 所有卡片与引用） |
| `s{site}:t:{termId}` | 分类项（名称、列表） | delivery（面包屑、列表数据源） |
| `s{site}:m:{mediaId}` | 媒体（alt、尺寸） | delivery |
| `s{site}:g:{contentId}` | 某内容的关系列表 | graph 数据源 |
| `s{site}:list:{type}` | 某类型的列表（有新内容加入时失效） | content-query 数据源 |
| `s{site}:nav` | 导航 | 所有 HTML |
| `s{site}:settings` | 站点设置与主题 | 所有 HTML |
| `s{site}:sitemap` | sitemap | sitemap 响应 |

单个响应的标签超过 200 个时，把卡片标签合并为粗粒度的 `list:{type}`，避免响应头过大。

### 23.3 失效流程

```text
ContentPublishedEvent(contentId = 42)
 → delivery.CacheInvalidator（事务提交后，异步执行）
     1. 计算标签：s1:c:42；首次发布再加 s1:list:PRODUCT；路径变化再加旧路径对应的条目
     2. Redis：通过标签索引（tag → 缓存键集合）删除 Delivery 结果
     3. CDN：调用按标签清除的接口（2 秒去抖合并；失败重试）
```

- 导航或站点设置变化会清除 `nav` / `settings` 标签，相当于清除整站 HTML；这类变化频率低，可以接受。
- **防击穿**：CDN 使用 stale-while-revalidate；Delivery 对同一个缓存键使用 Redis 锁合并并发回源。

---

## 24. API 设计规范

### 24.1 路径

```text
/api/admin/v1/{module}/{resources}      后台：Session + CSRF + 权限校验
/api/public/v1/{module}/{resources}     前台公开：无会话；限流；只返回发布态数据
/api/internal/v1/{module}/{action}      内部：只允许内网 + 服务令牌（缓存清除、运维）
```

| 示例 | 说明 |
|---|---|
| `GET /api/admin/v1/product/products?category=…&status=…` | 产品列表 |
| `POST /api/admin/v1/content/localizations/{id}/publish` | 发布 |
| `POST /api/admin/v1/content/localizations/{id}/revisions/{no}/restore` | 回滚为草稿 |
| `POST /api/admin/v1/translation/jobs` | 创建批量翻译任务 |
| `GET /api/public/v1/delivery/resolve?path=…` | 前台路由解析与渲染数据 |
| `POST /api/public/v1/form/forms/{key}/submissions` | 提交表单 |

### 24.2 约定

| 项 | 约定 |
|---|---|
| 风格 | REST；资源用复数名词；路径使用 kebab-case；JSON 字段使用 camelCase |
| 动作 | 状态变化用子资源动词：`POST …/publish`、`POST …/unpublish` |
| 成功响应 | 直接返回资源；列表返回 `{ "items": [], "page": 1, "size": 20, "total": 135 }` |
| 错误响应 | RFC 9457 `application/problem+json`（见下） |
| ID | 64 位 TSID，JSON 中为字符串（避免 JavaScript 精度丢失） |
| 时间 | ISO-8601 UTC，带 `Z` |
| 语言 | 内容语言用显式参数 `locale`；后台错误信息只使用中文（见下） |
| 并发 | 请求体带 `version`，或使用 `ETag` / `If-Match`；冲突返回 409 / 412 |
| 幂等 | 公开表单提交与后台导入支持 `Idempotency-Key`（保存 24 小时） |
| 分页 | 后台使用 page / size（size ≤ 100）；导出与同步使用游标 |
| 版本 | 主版本放在 URL 中；只做兼容的新增；破坏性变更需要新版本路径，并通过 `Sunset` 响应头宣布旧版本下线时间 |

错误示例：

```json
{
  "type": "https://docs.example.com/errors/content-slug-conflict",
  "title": "slug 已被占用",
  "status": 409,
  "code": "CONTENT_SLUG_CONFLICT",
  "detail": "路径 /zh/products/x200 已被内容 7201893453912345 使用",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "errors": [ { "field": "slug", "code": "CONFLICT", "message": "该 slug 已被其他内容使用" } ]
}
```

错误码命名为 `{MODULE}_{REASON}`。后台错误信息（`title`、`detail`、`errors[].message`）使用中文（后台界面只用简体中文，见 22.5）。前台公开接口的错误由前端按 `code`（及 `errors[].code`）映射为文案键显示（见 21.7），不直接展示 `detail`。

### 24.3 OpenAPI 与类型生成

- springdoc 按 admin / public 分组生成 OpenAPI 文档；
- `openapi-typescript` 生成前端类型到 `packages/api-client`；生成结果与代码不一致时 CI 失败；
- 使用 oasdiff 在 PR 中检测破坏性变更。

### 24.4 Delivery API

| 接口 | 说明 |
|---|---|
| `GET /api/public/v1/delivery/resolve?path=` | 解析路由并返回完整渲染数据 |
| `GET /api/public/v1/delivery/site` | 站点引导数据：导航、站点资料、主题设置、界面文案、同意与追踪配置 |
| `GET /api/public/v1/delivery/preview?token=` | 预览（草稿或指定修订） |
| `GET /api/public/v1/search?q=&type=&filters=&page=` | 站内搜索与分面筛选 |
| `GET /api/public/v1/form/forms/{key}` | 表单定义（通常已内嵌在 resolve 结果中） |
| `POST /api/public/v1/form/forms/{key}/submissions` | 提交表单 |
| `POST /api/public/v1/form/attachments` | 上传表单附件 |
| `POST /api/public/v1/tracking/consents` | 写入同意记录（见 20.2） |

`resolve` 的响应示例：

```json
{
  "kind": "CONTENT",
  "locale": "de",
  "route": {
    "path": "/de/produkte/li-ion-pack-x200",
    "canonical": "https://www.example.com/de/produkte/li-ion-pack-x200"
  },
  "content": {
    "id": "7201893453912345",
    "type": "PRODUCT",
    "revisionNo": 12,
    "template": "product-detail",
    "data": { "…": "快照中的 product 部分" },
    "layout": { "slots": { "afterSpecs": [], "bottom": [] } }
  },
  "refs": {
    "media":   { "7201893453918208": { "url": "…", "width": 1600, "height": 1200, "alt": "…", "blurhash": "…" } },
    "content": { "7201893453912399": { "type": "PRODUCT", "title": "…", "url": "/de/produkte/…" } },
    "terms":   { "7201893453900001": { "name": "Lithium-Akkus", "url": "/de/produktkategorie/…" } }
  },
  "dataSources": {
    "related": { "PRODUCT": [], "CASE_STUDY": [], "ARTICLE": [] },
    "sec_01J9X4K3": { "items": [] }
  },
  "breadcrumbs": [ { "title": "Startseite", "url": "/de" } ],
  "seo": {
    "title": "…",
    "meta": [ { "name": "description", "content": "…" }, { "name": "robots", "content": "index,follow" } ],
    "alternates": [ { "hreflang": "zh", "href": "…/zh/products/…" }, { "hreflang": "en", "href": "…/en/products/…" }, { "hreflang": "de", "href": "…/de/produkte/…" }, { "hreflang": "x-default", "href": "…（默认对外语言版本）" } ],
    "jsonLd": { "@context": "https://schema.org", "@graph": [] }
  },
  "cache": { "tags": ["s1", "s1:c:7201893453912345", "s1:t:7201893453900001", "s1:nav", "s1:settings"], "maxAge": 86400 }
}
```

其他 `kind`：`REDIRECT`（`{ "status": 301, "location": "…" }`）、`GONE`、`NOT_FOUND`。

---

## 25. 数据库设计规范

### 25.1 基础设定

- MySQL 8.4 LTS，InnoDB，默认字符集 `utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。
- 路径、slug、key 类字段使用 `utf8mb4_0900_bin`：精确匹配，避免 `café` 与 `cafe` 这类不区分重音的冲突。
- 时间统一以 UTC 存储（`DATETIME(3)`），应用时区为 UTC，展示时按用户或站点时区转换。
- 快照、布局文档、设置等使用 JSON 列，结构由应用层校验。

### 25.2 命名

| 对象 | 规则 | 示例 |
|---|---|---|
| 表 | `{模块前缀}_{名词单数}`，snake_case | `prd_product`、`cnt_localization` |
| 主键 | `id` | |
| 外键字段 | `{实体}_id` | `content_id` |
| 普通索引 | `idx_{表}_{字段}` | `idx_cnt_loc_list` |
| 唯一索引 | `uk_{表}_{字段}` | `uk_url_route_path` |

### 25.3 通用字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | TSID（时间有序的 64 位 ID），由应用生成 |
| `site_id` | BIGINT | 站点级数据（绝大多数表）；全局表没有（见 25.7） |
| `created_at` / `updated_at` | DATETIME(3) | UTC |
| `created_by` / `updated_by` | BIGINT | 用户 ID；系统操作为 0 |
| `version` | INT | 乐观锁（可编辑实体） |
| `deleted_at` | DATETIME(3) NULL | 软删除（只用于配置类实体；内容使用回收站状态） |

关联表与日志表按需取舍，不强制包含全部字段。

### 25.4 软删除与唯一索引

使用生成列，让唯一约束只在未删除的行之间生效（唯一索引中 NULL 互不相等）：

```sql
ALTER TABLE frm_form
  ADD COLUMN alive TINYINT AS (IF(deleted_at IS NULL, 1, NULL)) STORED,
  ADD UNIQUE KEY uk_frm_form_key (site_id, `key`, alive);
```

### 25.5 跨模块引用

- 不建跨模块外键，不做跨模块 JOIN；模块内部可以使用外键。
- 跨模块引用只保存 ID；被引用对象删除时，由事件（如 `ContentPurgedEvent`）驱动各模块清理自己的数据。

### 25.6 迁移

- 每个模块有自己的 Flyway 位置 `classpath:db/migration/{module}` 与 history 表 `flyway_{module}_history`，由模块管理器按依赖顺序执行（见 3.6）。
- 文件命名：`V{三位序号}__{描述}.sql`，例如 `V001__init.sql`。
- 已合入主干的迁移脚本禁止修改（CI 校验文件哈希）。
- 使用 expand / contract 模式支持滚动发布：先加可空列并回填 → 下个版本再加约束或删除旧列。
- 破坏性的数据变更需要 ADR。

### 25.7 站点过滤

- 使用 MyBatis-Plus 行级拦截器（`TenantLineInnerInterceptor`，列名配置为 `site_id`），对站点级表自动追加 `site_id = ?`（取自请求上下文中的当前站点，见 5.2）。
- 全局表（`sys_user`、`sys_user_mfa`、`sys_role`、`sys_permission`、`sys_module`、`sys_dsr_request`、系统级设置等）没有 site_id，在豁免清单中；`sys_audit_log` 带可空的 site_id，同样豁免；跨站点查询只允许系统管理接口。
- 由站点隔离测试保证（见 28）。

### 25.8 V1 主要表

| 模块 | 主要表 |
|---|---|
| core | `sys_site`、`sys_site_domain`、`sys_site_locale`、`sys_site_module`、`sys_site_profile`、`sys_user`、`sys_user_mfa`、`sys_role`、`sys_role_permission`、`sys_user_site_role`、`sys_permission`、`sys_menu`、`sys_module`、`sys_setting`、`sys_ui_text`（前台界面文案的站点覆盖与译文）、`sys_dict`、`sys_audit_log`、`sys_dsr_request`、`sys_job`、`sys_lock`、`event_publication` |
| content | `cnt_content`、`cnt_localization`、`cnt_revision`、`cnt_published_term`、`cnt_review`、`cnt_preview_token` |
| url | `url_route`、`url_redirect`、`url_pattern`、`url_not_found` |
| media | `mda_media`、`mda_media_localization`、`mda_folder`、`mda_usage` |
| taxonomy | `tx_vocabulary`、`tx_term`、`tx_term_localization`、`tx_assignment` |
| form | `frm_form`、`frm_form_localization`、`frm_form_notice_version`、`frm_submission`、`frm_attachment`、`frm_blocklist` |
| notification | `ntf_template`、`ntf_message`、`ntf_webhook` |
| translation | `trn_engine`、`trn_memory`、`trn_glossary_term`、`trn_style_guide`、`trn_job`、`trn_job_item`、`trn_usage_daily` |
| product | `prd_product`、`prd_product_localization`、`prd_attribute_group`、`prd_attribute`、`prd_attribute_option`、`prd_attribute_set`、`prd_attribute_set_item`、`prd_category_attribute_set`、`prd_attribute_value`、`prd_model`、`prd_product_media`、`prd_product_document` |
| article | `art_article`、`art_article_localization`、`art_case_study`、`art_faq`、`art_author` |
| page | `pg_page`、`pg_layout`（各内容类型的草稿布局文档）、`pg_component_usage` |
| inquiry | `inq_inquiry`、`inq_inquiry_item`、`inq_activity`、`inq_attachment`、`inq_assignment_rule`、`inq_sales_group`、`inq_sales_group_member` |
| navigation | `nav_menu`、`nav_item`、`nav_item_localization` |
| seo | `seo_meta`、`seo_template`、`seo_link`、`seo_audit`、`seo_robots_rule` |
| graph | `grh_relation`、`grh_relation_type`、`grh_config` |
| tracking | `trk_setting`、`trk_script`、`trk_consent_log` |

完整字段在《领域模型与数据库设计》中给出（附录 C）。

### 25.9 关键表 DDL 示例

```sql
CREATE TABLE cnt_localization (
  id                      BIGINT        NOT NULL,
  site_id                 BIGINT        NOT NULL,
  content_id              BIGINT        NOT NULL,
  locale                  VARCHAR(16)   NOT NULL,
  slug                    VARCHAR(160)  COLLATE utf8mb4_0900_bin NULL,
  draft_title             VARCHAR(300)  NOT NULL,
  draft_state             VARCHAR(20)   NOT NULL COMMENT 'EDITING / IN_REVIEW / APPROVED',
  draft_hash              CHAR(64)      NULL,
  publish_state           VARCHAR(20)   NOT NULL COMMENT 'NEVER / SCHEDULED / PUBLISHED / UNPUBLISHED',
  published_revision_id   BIGINT        NULL,
  first_published_at      DATETIME(3)   NULL,
  published_at            DATETIME(3)   NULL,
  scheduled_publish_at    DATETIME(3)   NULL,
  scheduled_unpublish_at  DATETIME(3)   NULL,
  unpublish_strategy      JSON          NULL COMMENT 'GONE / REDIRECT_CONTENT / REDIRECT_PARENT',
  translation_mode        VARCHAR(10)   NOT NULL COMMENT 'SOURCE / AUTO / MANUAL',
  translation_state       VARCHAR(20)   NOT NULL COMMENT 'NONE / QUEUED / TRANSLATED / NEEDS_REVIEW / FAILED / OUTDATED',
  source_locale           VARCHAR(16)   NULL,
  source_revision_no      INT           NULL,
  pub_title               VARCHAR(300)  NULL,
  pub_summary             VARCHAR(1000) NULL,
  pub_cover_media_id      BIGINT        NULL,
  pub_path                VARCHAR(500)  COLLATE utf8mb4_0900_bin NULL,
  pub_hash                CHAR(64)      NULL,
  content_modified_at     DATETIME(3)   NULL COMMENT '发布内容实际变化的时间，用作 sitemap lastmod',
  version                 INT           NOT NULL DEFAULT 0,
  created_at              DATETIME(3)   NOT NULL,
  created_by              BIGINT        NOT NULL,
  updated_at              DATETIME(3)   NOT NULL,
  updated_by              BIGINT        NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cnt_loc_content_locale (content_id, locale),
  KEY idx_cnt_loc_list (site_id, locale, publish_state, published_at),
  KEY idx_cnt_loc_schedule (scheduled_publish_at),
  KEY idx_cnt_loc_unschedule (scheduled_unpublish_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cnt_revision (
  id               BIGINT        NOT NULL,
  site_id          BIGINT        NOT NULL,
  localization_id  BIGINT        NOT NULL,
  revision_no      INT           NOT NULL,
  payload          JSON          NOT NULL,
  payload_hash     CHAR(64)      NOT NULL,
  comment          VARCHAR(500)  NULL,
  published_by     BIGINT        NOT NULL,
  published_at     DATETIME(3)   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cnt_rev_loc_no (localization_id, revision_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE url_redirect (
  id                      BIGINT         NOT NULL,
  site_id                 BIGINT         NOT NULL,
  source_path             VARCHAR(500)   COLLATE utf8mb4_0900_bin NOT NULL,
  match_type              VARCHAR(10)    NOT NULL DEFAULT 'EXACT' COMMENT 'EXACT / PREFIX / REGEX',
  target_kind             VARCHAR(16)    NOT NULL COMMENT 'LOCALIZATION / PATH / EXTERNAL / GONE',
  target_localization_id  BIGINT         NULL,
  target_path             VARCHAR(1000)  NULL,
  status_code             SMALLINT       NOT NULL COMMENT '301 / 302 / 308 / 410',
  origin                  VARCHAR(10)    NOT NULL COMMENT 'AUTO / MANUAL / IMPORT',
  hits                    BIGINT         NOT NULL DEFAULT 0,
  last_hit_at             DATETIME(3)    NULL,
  note                    VARCHAR(500)   NULL,
  created_at              DATETIME(3)    NOT NULL,
  created_by              BIGINT         NOT NULL,
  updated_at              DATETIME(3)    NOT NULL,
  updated_by              BIGINT         NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_url_redirect_source (site_id, source_path, match_type),
  KEY idx_url_redirect_target (target_localization_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE grh_relation (
  id                 BIGINT         NOT NULL,
  site_id            BIGINT         NOT NULL,
  source_content_id  BIGINT         NOT NULL,
  target_content_id  BIGINT         NOT NULL,
  relation_type      VARCHAR(40)    NOT NULL,
  origin             VARCHAR(10)    NOT NULL COMMENT 'MANUAL / AUTO',
  score              DECIMAL(7, 2)  NULL,
  rank_no            INT            NOT NULL DEFAULT 0,
  pinned             TINYINT(1)     NOT NULL DEFAULT 0,
  reason             JSON           NULL,
  computed_at        DATETIME(3)    NULL,
  created_at         DATETIME(3)    NOT NULL,
  created_by         BIGINT         NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_grh_relation (site_id, source_content_id, target_content_id, relation_type),
  KEY idx_grh_relation_source (site_id, source_content_id, origin, rank_no),
  KEY idx_grh_relation_target (site_id, target_content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
```

---

## 26. 安全设计

### 26.1 认证（后台）

| 项 | 设计 |
|---|---|
| 登录 | 邮箱 + 密码；Argon2id 哈希；密码 ≥ 12 位；【可选】对接泄露密码库检查 |
| 2FA | TOTP；拥有发布、管理、开发者权限的角色强制开启；提供恢复码 |
| 会话 | Spring Session（Redis）；Cookie `__Host-SESSION; Secure; HttpOnly; SameSite=Lax`；空闲 2 小时、绝对 12 小时过期 |
| 二次验证 | 修改密码、重置 2FA、导出数据、修改脚本等敏感操作需要重新验证 |
| 防暴力破解 | 同账号 + IP 15 分钟内失败 5 次后递增延迟；异常时告警 |
| CSRF | Spring Security CSRF，Cookie 转 Header（`XSRF-TOKEN` → `X-XSRF-TOKEN`） |
| SSO | 【V2+】OIDC（Google Workspace、Microsoft Entra 等） |

### 26.2 授权

- 权限码格式：`{module}:{resource}:{action}`。内容生命周期的权限归内容内核，resource 为内容类型，如 `content:product:publish`；业务模块自己的资源如 `product:attribute:manage`、`inquiry:inquiry:export`、`tracking:script:edit`。
- 角色按站点分配；内置角色对应 1.2 中的角色表，也支持自定义角色。
- **数据范围**：询盘支持 ALL / GROUP / OWN（ALL 指当前站点内全部；角色按站点分配，跨站点由站点过滤保证）；翻译审校人员可以限定语言（见 5.8）。
- **执行位置**：应用服务层的方法注解（`@RequiresPermission`）+ 查询层的数据范围过滤。前端隐藏按钮只是体验优化，不是安全措施。
- Public API 只返回已发布数据，不能通过遍历 ID 访问未发布的内容。

### 26.3 Web 安全

| 威胁 | 措施 |
|---|---|
| XSS | Vue 默认转义；富文本以 JSON 存储、按白名单渲染；禁止 `v-html`（富文本渲染器除外）；SVG 净化；基于 nonce 的 CSP |
| 自定义 HTML | `custom-html` 组件只对开发者角色开放，内容经 jsoup 白名单清洗（允许 HTML 与 CSS，不允许脚本）；脚本只能通过追踪模块的脚本管理注入，并全量审计 |
| CSRF | 后台 CSRF Token + SameSite Cookie；公开表单无会话，使用 formToken + Turnstile |
| SQL 注入 | MyBatis 只允许 `#{}`；`${}` 由 CI 检查禁止（排序字段使用白名单映射） |
| SSRF | 服务端不访问用户提供的 URL；导入图片 URL 时使用域名白名单，DNS 解析后校验不是内网地址，并限制大小与超时 |
| 提示词注入 | 待翻译文本与询盘留言可能包含指令；译文一律按纯文本处理（转义输出、不执行、不作为后续提示词的指令）；单元 ID 由系统生成并校验，响应中的 ID 必须与请求一一对应（见 5.9、17.10） |
| 文件上传 | 扩展名白名单 + Tika 魔数校验 + 大小限制 + 私有桶 + 病毒扫描 + 随机化存储键 + `Content-Disposition` |
| 点击劫持 | `frame-ancestors`：前台为 `'self'`（编辑器路由允许后台域名）；后台为 `'none'` |
| 传输 | 全站 HTTPS + HSTS；TLS 1.2 及以上 |
| 依赖漏洞 | Renovate / Dependabot、OWASP Dependency-Check、`pnpm audit`、镜像扫描（Trivy） |
| 恶意流量 | 限流 + Turnstile + CDN WAF |
| 信息泄露 | 错误响应不暴露堆栈；actuator 只在内网开放；日志脱敏 |

### 26.4 限流

| 接口 | 维度 | 限制 |
|---|---|---|
| 登录 | 账号 + IP | 5 次 / 15 分钟 |
| 表单提交 | IP | 5 次 / 10 分钟 |
| 表单提交 | 邮箱 | 10 次 / 天 |
| 附件上传 | IP | 10 次 / 10 分钟 |
| 同意记录写入 | IP | 30 次 / 10 分钟 |
| 站内搜索 | IP | 60 次 / 分钟 |
| Delivery（经 Nuxt） | 内部令牌 | 不限（由 CDN 与 WAF 保护） |
| 后台 API | 用户 | 600 次 / 分钟 |

同意记录接口只接受固定结构的请求体（字段见 20.2）；超出限流时静默丢弃，不影响前台横幅的行为。

### 26.5 审计

- `sys_audit_log` 只追加：操作人、时间、站点（系统级操作为空）、IP、UA、模块、动作、目标类型与 ID、摘要、变更差异（敏感字段脱敏）。
- 覆盖：所有后台写操作、登录登出与失败、角色与权限变更、数据导出、脚本修改、模块启停、设置修改。
- 默认保留 1 年（可配置）；后台提供筛选查看；应用内不能修改或删除。

### 26.6 密钥与配置

- 密钥通过环境变量或密钥管理服务注入，永远不进入代码仓库（CI 中运行 gitleaks）。
- 生产环境的应用数据库账号没有 DDL 权限；迁移任务使用单独的账号。
- 设置中的敏感值（SMTP 密码、第三方 API Key）在数据库中用 AES-GCM 加密，密钥来自环境变量。
- AI 翻译引擎的 API Key（保存在 `trn_engine`，见 5.9）同样用 AES-GCM 加密存储。

### 26.7 内部调用

- Nuxt → 后端只走内网，并携带可轮换的 `X-Internal-Token`；只有带令牌的请求，后端才信任 `X-Site-Host` 与客户端 IP 头。
- `/api/internal/*` 只在内网开放，并要求服务令牌。

### 26.8 隐私

合规基线见 ADR-016（GDPR、UK GDPR、ePrivacy；CCPA/CPRA 等美国州隐私法）；数据驻留欧盟见 ADR-015。

- **个人信息清单**：询盘联系人、IP、附件、后台用户、同意记录；数据最小化；保留期任务（询盘见 17.11；原始 IP 保留 30 天后置空，应用日志中 IP 截断，见 27.4）。
- **同意记录**：Cookie 同意的证明保存在 `trk_consent_log`：同意 ID（假名标识）、时间、动作、同意文本版本、展示的语言、选择的类别、访客地区等（字段见 20.2）；保留期限由法务确认（默认 3 年）；同意管理与 GPC 见 20.2。
- **数据主体请求**：支持访问、更正、删除、可携带、反对、限制处理，以及 CCPA 的退出“出售 / 共享”；GDPR / UK GDPR 1 个月内答复（必要时可延长两个月），CCPA 45 个日历日内答复（必要时可再延长 45 天），具体时限以法务确认为准（见 17.11）；`sys_dsr_request` 记录请求、身份核验、处理与答复时间；受理渠道、身份核验与后台工具见 17.11。
- **AI 翻译**：自动翻译发送给 AI 引擎的只有公开的内容与站点资源；询盘翻译为按需功能（由销售在询盘详情页手动触发，会把询盘留言发送给 AI 引擎，见 5.7、17.10），站点可关闭；询盘译文不写入翻译记忆；AI 服务商纳入处理者清单（见下表）。
- **上线前合规清单**：

| 项 | 内容 |
|---|---|
| 处理活动记录（ROPA） | 处理目的、数据类别、合法性基础、保留期、接收方 |
| 处理者清单 | 云厂商、CDN / WAF、人机验证（Cloudflare Turnstile）、邮件服务商、Sentry、GA4 等以处理者身份提供服务的统计工具、Webhook 接收方（如企业微信、飞书、Slack）、AI 翻译引擎服务商（询盘翻译会发送询盘留言）等；记录数据区域与跨境传输依据 |
| DPA | 与清单中的处理者签署数据处理协议（GDPR 第 28 条） |
| 广告与社交平台 | Google Ads、Meta Pixel、LinkedIn Insight Tag 等通常为独立控制者或共同控制者，适用其控制者条款或共同控制协议；角色划分由法务确认 |
| 法律页面 | 每个站点的隐私政策、Cookie 政策、使用条款、Imprint（见 10.1、16.3） |
| 数据泄露响应 | 响应流程、联系人、泄露记录表（见 27.5） |
| 法务待确认 | GDPR 第 27 条欧盟代表与英国代表；询盘与下载留资的合法性基础；中国大陆销售人员访问欧盟个人数据及其他跨境传输（全球 CDN、美国服务商、Webhook 接收方）的合规安排；CCPA 是否适用；加拿大的要求（PIPEDA、魁北克第 25 号法律、CASL）；同意记录保留期限。以上事项由业主自行确认（见 32 章 Q13），系统设计中不下结论 |

---

## 27. 部署与运维

部署由业主自行负责（云厂商与 CDN 的选择见 32 章 Q3）；本章给出参考拓扑与要求。

### 27.1 部署拓扑

自用，一次部署 = 一家企业，只有一套生产环境；另有 dev（集成）、staging（验收）与本地开发环境（见 27.2）。生产环境以 Docker Compose 部署在欧盟单区域的多台主机上（多实例），V2 可迁移到 Kubernetes（见 2.4）；单机 Docker Compose 只用于本地开发与演示。

生产拓扑：

| 组件 | 配置 |
|---|---|
| 后端 | × 2+（无状态，会话在 Redis） |
| Nuxt | × 2+ |
| imgproxy | × 1–2 |
| MySQL | 托管服务（主从 + 自动备份） |
| Redis | 托管服务 |
| Meilisearch | 单节点 + 快照（可从数据库重建） |
| 对象存储 | S3 兼容（如 S3、R2），公开桶与私有桶分离 |
| 反向代理 | Nginx / Caddy（TLS 终止、路由、真实 IP，见 2.3） |
| Admin 静态文件 | SPA 静态文件，经 CDN 分发（见 22.6） |
| 边缘 | 全球 CDN + WAF |

- 区域：源站部署在**欧盟**（默认法兰克福，备选阿姆斯特丹、巴黎）；数据库、对象存储、备份全部在欧盟区域；全球 CDN（如 Cloudflare）覆盖欧美访客，美国访客由 CDN 边缘节点提供 HTML 与图片（见 ADR-015）。若美国流量远大于欧洲，可以评估美东源站，但需要处理欧盟 → 美国的数据传输（可依靠服务商的 EU-US Data Privacy Framework 认证或标准合同条款，由法务确认）；是否需要美国源站由业主自行决定（见 32 章 Q15）。
- 第三方服务：优先选择提供欧盟数据区域的服务，如邮件服务（SES 的欧盟区域、Mailgun EU、Brevo）、Sentry（欧盟数据区域）、欧盟区域的托管数据库。
- 后台访问：后台用户主要在中国大陆，访问欧盟源站延迟较高；后台静态资源走 CDN，API 响应精简；必要时使用加速线路（其运营方纳入处理者清单评估，见 26.8；风险见 31）。
- 未来如果需要为其他企业提供系统，采用一企业一实例独立部署（独立数据库与存储），见 ADR-009。

### 27.2 环境

| 环境 | 用途 | 特点 |
|---|---|---|
| local | 开发 | Docker Compose：MySQL、Redis、Meilisearch、MinIO、Mailpit、imgproxy |
| dev | 集成 | 主干自动部署 |
| staging | 验收 | 与生产一致；`noindex` + Basic Auth；使用脱敏数据；发布候选版本 |
| prod | 生产 | 人工批准发布 |

### 27.3 CI/CD

```text
PR：
  后端：编译 → 单元测试 → 架构测试（Modulith verify + ArchUnit）→ 集成测试（Testcontainers）
       → OpenAPI 生成 + 破坏性变更检查
  前端：ESLint / Stylelint → 类型检查 → 单元测试 → 构建 → 组件视觉回归（改动涉及主题时）
  通用：gitleaks、依赖漏洞扫描、迁移脚本不可修改检查
main：构建镜像 → 部署 dev → E2E 冒烟测试
发布标签：部署 staging → E2E 全量 + Lighthouse CI + SEO 爬取检查 + 安全扫描 → 人工批准 → 生产：
  1. 运行 migrate 任务（独立容器，--mode=migrate）
  2. 依次滚动更新 backend → website → admin
  3. 冒烟测试（含一条测试询盘），失败则自动回滚到上一个镜像
```

### 27.4 可观测性

| 方面 | 设计 |
|---|---|
| 日志 | JSON 结构化；字段包含 traceId、spanId、siteId、userId、module；PII 脱敏；IP 截断（IPv4 最后一段置零，IPv6 保留前 48 位）；汇总到 Loki 或 ELK（部署在欧盟区域）；保留 30 天 |
| 指标 | Micrometer → Prometheus：HTTP 请求量、错误率、延迟；JVM；连接池；缓存命中率；未完成事件；任务队列；通知失败；表单提交量与垃圾率；Delivery 延迟；Nuxt 渲染耗时；AI 翻译：翻译记忆命中率、引擎错误率与延迟、待处理单元数、每日费用（见 5.10） |
| 链路追踪 | OpenTelemetry：Nuxt（Node）→ 后端，传递 `traceparent` |
| 错误 | Sentry（欧盟数据区域）：后端、Nuxt 服务端与客户端、Admin |
| 健康检查 | 后端 `/actuator/health/liveness`、`/readiness`（readiness 包含 DB、Redis；不包含 Meilisearch，其故障只算降级）；Nuxt `/__health` |

告警：

| 告警 | 条件 | 级别 |
|---|---|---|
| 前台 5xx 比例 | > 1%，持续 5 分钟 | P1 |
| Delivery P95 | > 500 ms，持续 10 分钟 | P2 |
| 未完成事件 | 数量 > 100，或最早一条超过 10 分钟 | P2 |
| 邮件发送失败 | 连续失败 > 5 次 | P1（影响询盘通知） |
| 询盘量骤降 | 工作日比 7 日均值下降 80% | P2（可能是表单故障） |
| 证书到期 | < 14 天 | P2 |
| 备份失败 | 任意一次 | P1 |
| 磁盘 / 连接池 | 使用率 > 85% | P2 |
| 翻译失败积压 | `FAILED`（引擎错误且重试耗尽）的翻译任务项数量超过阈值（可配置） | P2（不影响中文内容发布） |
| 翻译费用 | 达到月度预算 80%（达到 100% 时暂停批量任务，见 5.10） | P2 |

**合成监控**：每 15 分钟检查首页、一个产品页与 sitemap；每天提交一条测试询盘（标记为 `is_test`，不计入报表，通知发到运维邮箱），验证整条获客链路。

### 27.5 备份与恢复

| 数据 | 方式 | 保留 |
|---|---|---|
| MySQL | 每日全量 + binlog 时间点恢复（RPO ≤ 15 分钟）；使用欧盟区域的托管服务 | 30 天；每月快照保留 12 个月 |
| 对象存储 | 开启版本控制；生产环境复制到另一个欧盟区域 | 旧版本保留 30 天 |
| Redis | 不备份（缓存与会话可以丢失，会话丢失只需重新登录） | — |
| Meilisearch | 不备份，从数据库重建 | — |
| 密钥与配置 | 密钥管理服务 | — |

所有备份与副本都存放在欧盟区域（见 ADR-015）。每季度在 staging 上做一次恢复演练。运维手册（`docs/runbooks/`）覆盖：数据库恢复、重建搜索索引、清除 CDN、轮换密钥、迁移失败的回滚、个人数据泄露响应（评估、记录；按 GDPR / UK GDPR 第 33 条在知悉后 72 小时内通知监管机构，高风险时按第 34 条通知数据主体；美国各州的通知要求由法务确认）。

### 27.6 容量基线（单站，按假设 A4）

| 组件 | 规格 |
|---|---|
| 后端 | 2 × (2 vCPU, 4 GB) |
| Nuxt | 2 × (1 vCPU, 1 GB) |
| MySQL | 2 vCPU, 8 GB, 100 GB SSD |
| Redis | 1 GB |
| Meilisearch | 2 GB 内存 |
| imgproxy | 1 vCPU, 1 GB |

---

## 28. 测试策略

| 层级 | 工具 | 范围 | 门禁 |
|---|---|---|---|
| 单元 | JUnit 5 + AssertJ；Vitest | 领域逻辑、评分算法、URL 规范化、组件迁移函数 | 领域层覆盖率 ≥ 80% |
| 架构 | Spring Modulith `verify()`、ArchUnit；前端 dependency-cruiser | 依赖方向、跨模块访问、包结构 | 必须通过 |
| 模块集成 | `@ApplicationModuleTest` + Testcontainers（MySQL、Redis、Meilisearch） | 单个模块 + 事件场景（Scenario API） | 必须通过 |
| AI 翻译 | `@ApplicationModuleTest` + 假引擎（`TranslationEngine` 的测试实现，不调用真实服务商） | 用假引擎测试翻译流程（提取 → 查翻译记忆 → 调用引擎 → 校验 → 回写）；翻译记忆复用（同一原文不重复调用引擎）；占位标签与数字校验；`HUMAN` 条目不被机器译文覆盖 | 必须通过 |
| 契约 | OpenAPI 快照 + oasdiff；组件 Schema 校验 | API 与 Schema 不被无意破坏 | 必须通过 |
| 站点隔离 | 专用测试套件 | 准备两个站点的数据，以及只在其中一个站点拥有角色的用户，遍历后台接口，验证不能越权读写另一个站点的数据 | 必须通过 |
| 组件视觉 | Playwright 截图 `/__gallery` | 组件 × 变体 × 断点 × 主题 | 差异需人工确认 |
| E2E | Playwright | 场景 S1–S13 | staging 必须通过 |
| SEO | 自研爬取检查脚本（`tools/seo-check`） | 全站状态码、canonical、hreflang 互链、JSON-LD 校验、不执行 JS 时的内容、重定向链 | staging 必须通过 |
| 性能 | Lighthouse CI；k6 | 页面性能预算；Delivery API 200 RPS | 超出预算则阻断 |
| 安全 | OWASP ZAP baseline、依赖扫描、gitleaks | staging | 高危则阻断 |
| 无障碍 | axe-core（集成在 Playwright 中） | 主要模板 | 严重问题则阻断 |

测试数据：维护一套演示数据集（多分类、多语言的电池类产品、文章、案例），供 E2E、组件画廊与演示使用。

---

## 29. 工程规范与架构治理

### 29.1 仓库结构（Monorepo）

```text
b2b-platform/
├── CLAUDE.md
├── backend/                         # Maven 多模块
│   ├── pom.xml
│   ├── platform/
│   │   ├── platform-core/
│   │   └── platform-api/
│   ├── modules/
│   │   ├── content/  url/  media/  taxonomy/  form/  notification/  translation/    # L2
│   │   ├── product/  article/  page/  inquiry/  navigation/                         # L3
│   │   └── delivery/  seo/  graph/  search/  tracking/                              # L4
│   ├── infrastructure/              # 存储、邮件、搜索客户端、缓存、CDN 清除、AI 翻译引擎等适配器
│   └── application/                 # Spring Boot 启动与装配
├── frontend/                        # pnpm workspace
│   ├── apps/
│   │   ├── website/                 # Nuxt
│   │   └── admin/                   # Vue + Vite
│   └── packages/
│       ├── design-tokens/
│       ├── component-schemas/       # 组件、模板 Schema 与迁移函数（后端构建时复制到 classpath）
│       ├── themes/industrial/       # V1 主题
│       ├── api-client/              # OpenAPI 生成的类型与客户端
│       └── shared/                  # 富文本渲染、工具函数
├── docs/
│   ├── architecture/                # 本文档
│   ├── adr/                         # 架构决策记录
│   ├── modules/                     # 每个模块一份文档
│   ├── api/
│   └── runbooks/                    # 运维手册
├── deploy/                          # Dockerfile、Compose、（V2）Kubernetes 清单
└── tools/                           # seo-check、数据导入脚本、代码检查脚本
```

### 29.2 新模块接入清单

1. 在 `platform-api` 中定义需要的契约（服务接口、事件、DTO），并通过架构评审。
2. 按模板创建后端模块（3.4）与 `module.json`（3.5）。
3. 新增内容类型时，实现必需的扩展点：`ContentTypeProvider`、`PublishContributor`、`SchemaOrgProvider`、`SearchDocumentProvider`、`BreadcrumbProvider`、`MediaReferenceCollector`、`SeoVariableProvider`、`TranslatableContentProvider`；模块中有站点资源的可翻译文本时，实现 `TranslatableResourceProvider`（见 3.7、5.5）。
4. 在 Admin 中创建 `modules/<id>/` 并用 `defineAdminModule` 注册。
5. 前台组件：在 `component-schemas` 中添加定义，在主题中实现变体，补充组件画廊示例。
6. 在 `docs/modules/<id>.md` 中写明职责、数据模型、事件、扩展点、API、配置。

### 29.3 架构守护（自动化）

| 规则 | 检查手段 |
|---|---|
| Core 不依赖业务模块；模块之间不互相 import | Maven 依赖（编译期隔离）+ ArchUnit + Spring Modulith `verify()` |
| 模块只访问自己的表 | ArchUnit：Mapper 只能在本模块的包中使用；脚本扫描 SQL 中的表前缀 |
| MyBatis 不使用 `${}` | 自定义检查脚本（Mapper XML 与注解） |
| 跨模块契约只在 platform-api | CODEOWNERS：platform-api、platform-core 的变更需要架构负责人审核 |
| 前端模块之间不互相 import | ESLint `no-restricted-imports` / dependency-cruiser |
| 前台样式只用 Design Token | Stylelint `declaration-strict-value`；tokens 包之外禁止颜色字面量 |
| 禁止 `v-html` | ESLint `vue/no-v-html`（富文本渲染器例外） |
| 已合入的迁移脚本不可修改 | CI 校验迁移文件哈希 |
| API 破坏性变更 | oasdiff |
| 组件 Schema 破坏性变更必须提升版本并附迁移函数 | CI 中的 Schema 比对脚本 |
| `module.json` 中声明的事件与代码一致 | CI 脚本 |
| 性能预算 | Lighthouse CI |
| 密钥泄露 | gitleaks |

### 29.4 分支与提交

- 主干开发；短生命周期的功能分支；必须通过 PR 合入（至少 1 人审核；platform-core / platform-api 需要 2 人，其中包括架构负责人）。
- 提交信息遵循 Conventional Commits；使用 squash 合并。
- 平台与各模块使用语义化版本；自动生成 CHANGELOG。

### 29.5 ADR 流程

- 文件：`docs/adr/NNNN-title.md`（MADR 模板：背景、决策、备选方案、后果、状态）。
- 以下情况必须写 ADR：新增模块；跨模块契约变更；引入新的基础设施依赖；不兼容的数据模型变更；偏离第 2.1 节的架构原则。

### 29.6 完成定义（Definition of Done）

- 代码、测试、文档在同一个 PR 中；
- 全部 CI 门禁通过；
- 新的后台功能有对应的权限点与审计；
- 新的前台组件有 Schema、变体截图、无障碍检查；固定文案使用文案键，不硬编码（见 21.7）；
- 新增的可翻译字段已在 `TranslatableContentProvider` / `TranslatableResourceProvider` 中登记；
- 新的内容类型实现了 29.2 第 3 步中的全部扩展点；
- 数据迁移遵循 expand / contract。

### 29.7 AI 辅助开发

- 项目根目录的 `CLAUDE.md`（草案见附录 A）是 Claude Code 的架构边界。
- 每个会话聚焦一个模块或一个垂直切片；开始前阅读相关模块文档与 ADR。
- 提交前必须在本地跑通 29.3 中的检查；未经 ADR 不修改架构。

---

## 30. 实施路线图

采用垂直切片：每个里程碑结束时都有一条可演示、可验收的完整链路。V1 发布 = M1–M4 全部完成。

| 里程碑 | 目标 | 主要交付 | 验收 |
|---|---|---|---|
| **M0 工程底座** | 可持续开发的骨架 | Monorepo；Maven 多模块与 platform-api 骨架；模块管理器（描述符、依赖校验、迁移、启用开关）；扩展点注册表；事件（Modulith）；站点上下文；OpenAPI → TS；Nuxt 与 Admin 骨架；Docker Compose；CI 全部门禁；版本兼容验证（ADR-011） | 示例模块违反边界时 CI 失败；按模板新建一个模块并接入 ≤ 1 小时 |
| **M1 中文单语闭环** | 以中文站点打通“内容 → SEO → 询盘” | 用户、角色、2FA、审计；内容内核（本地化、修订、发布、预览）；多语言数据模型就绪（源语言、翻译模式与翻译状态字段；URL 全部带语言前缀，根路径 302 到默认对外语言）；URL（路由、自动 301）；媒体（上传、图片变换）；分类体系；产品（基础字段与属性）；SEO（Meta 自动生成与覆盖、canonical、Product / Breadcrumb / Organization JSON-LD）；Delivery；Nuxt SSR + 基础主题（产品模板）；表单 + 询盘 + Turnstile + 邮件通知 + 归因 | S1（不含英语部分）、S2、S4（不含 GA4） |
| **M2 页面与设计系统** | 运营可以自主搭建页面 | Design Token；主题（≥ 20 个组件）；模板与插槽；页面编辑器（iframe 画布）；全局区块；导航；站点资料；文章、案例、FAQ；审核；定时发布；回滚；Cache Tag + CDN 清除 | S3、S10 |
| **M3 SEO 与获客完善** | SEO 引擎完整，转化可追踪；AI 翻译上线并开放英语 | Sitemap（含图片）；robots；IndexNow；链接索引；Content Graph（人工 + 规则评分）；相关内容；SEO 健康；重定向导入；404 日志；追踪与同意（含 GPC、同意记录、法律页面）；询盘分配规则、去重、SLA、报表；下载留资；询价篮；AI 翻译（翻译记忆、术语表、风格指南、翻译编辑器、批量翻译、用量统计）；开放英语；多语言 SEO（hreflang、多语言 sitemap） | S5、S7、S8、S9、S11、S12、S13；Lighthouse 预算达标 |
| **M4 规模化运营** | 支撑大量内容与上线 | 产品 Excel 导入导出；型号表；站内搜索与分面筛选；完善数据范围；隐私工具（数据主体请求、保留期）；性能与安全加固；上线运维手册 | S6；安全扫描无高危；备份恢复演练通过 |
| **M5（V2）** | 扩展能力 | AI（写作、SEO 建议、语义相似、垃圾识别；AI 翻译已纳入 V1，见 M3）；CRM；报价；WhatsApp；Newsletter；Analytics；多主题 | 按各模块的需求文档 |

关键调整（相对 V1.0）：**多语言数据模型、Nuxt 前台、URL 引擎全部提前到 M1；插件系统降级为编译期模块 + 启用开关；先在 M1 用 PRODUCT 一种类型验证“快照 + 投影”模型，M2 再扩展到其他类型**。

V1.3 调整：M1 为中文单语闭环（前台只开放中文，多语言数据模型就绪，URL 全部带语言前缀）；AI 翻译、开放英语与多语言 SEO 放在 M3。

每个里程碑结束时做一次演示与复盘，必要时调整后续范围。

---

## 31. 风险与对策

| 风险 | 可能性 | 影响 | 对策 |
|---|---|---|---|
| 页面编辑器复杂度失控 | 高 | 高 | V1 只做 Section 级编辑与预设变体，不做自由布局；先做模板，再做编辑器；跨 iframe 拖放改为大纲树排序 |
| 自研工作量超出预期 | 中 | 高 | 垂直切片，每个里程碑都可演示；严守 V1 范围；必要时把 M4 中的部分功能移到 V2 |
| 内容内核设计错误导致大面积返工 | 中 | 高 | M1 先用产品类型验证快照与投影，再推广到其他类型；关键设计必须经 ADR 评审 |
| 模块边界逐步被侵蚀（包括 AI 生成的代码） | 高 | 中 | 29.3 中的自动化守护；CODEOWNERS；`CLAUDE.md` |
| SEO 倒退（上线或迁移后出现大量 404） | 中 | 高 | 自动 301；404 日志与一键重定向；上线前跑 SEO 爬取检查；迁移映射报告 |
| 多语言内容维护成本高 | 高 | 中 | AI 翻译 + 翻译记忆（同一原文只翻译一次，见 5.5–5.10）；翻译状态与过期检测（`MANUAL` 模式） |
| AI 译文质量（术语错误、法律页面） | 中 | 高 | 术语表、风格指南、自动校验；法律页面人工审校后发布；关键页面母语人员抽检（见 5.8） |
| AI 费用失控 | 低 | 中 | 翻译记忆（命中不调用 AI）；月度预算上限与告警；用量报表（见 5.10） |
| AI 引擎不可用 | 中 | 低 | 异步翻译，不影响中文内容发布；退避重试；备用引擎（见 5.9） |
| 询盘通知失败导致丢单 | 低 | 高 | 通知发件箱 + 重试 + 告警；每日合成测试询盘 |
| 垃圾询盘泛滥 | 中 | 中 | 多层反垃圾 + 待审区 + 可调整的规则 |
| 源站在欧盟，国内访问后台慢 | 中 | 中 | 后台静态资源走 CDN；接口精简；必要时使用加速线路 |
| Meilisearch 故障 | 低 | 中 | 可以重建；列表降级到 MySQL |
| 第三方依赖版本不兼容 | 中 | 中 | M0 做版本验证并锁定；Renovate 渐进升级 |
| 隐私合规风险 | 中 | 高 | 同意管理、同意记录、GPC、保留期、数据主体请求工具；法务评审（业主自行负责，见 32 章 Q13）；上线前的合规检查清单（见 26.8） |

---

## 32. 待确认问题

已确认或由业主自行负责的问题保留在表中，并写明结论。

| # | 问题 | 影响 |
|---|---|---|
| Q1 | 产品形态（对应假设 A1）？**已确认：自用**。一家企业，可以有多个品牌站或国家站；不做 SaaS，不为其他企业交付；站点为最高隔离单位（见 ADR-009） | 3、5、25、27 |
| Q2 | 首批语种清单？**已确认**：前台 V1 先开放中文；其他语言由 AI 翻译生成后按需开放（建议 en、de、fr、es、it） | 5、11 |
| Q3 | 云厂商与 CDN 的选择（区域已定为欧盟）？**业主自行负责**（不在本文范围） | 27 |
| Q4 | 首批站点的产品规模与属性复杂度？是否需要型号表？ | 8 |
| Q5 | 是否有旧站需要迁移？旧站使用什么平台（如 WordPress）？ | 8.6、13.5 |
| Q6 | V1 是否需要在系统内直接给客户发邮件？ | 17 |
| Q7 | 销售团队结构与分配规则？ | 17.7 |
| Q8 | 哪些站点、哪些内容类型需要审核？ | 6.7 |
| Q9 | V1 唯一主题的设计方向与设计稿由谁提供？ | 11 |
| Q10 | 邮件服务商？是否已有 GA4 / GTM 账号？ | 18、20 |
| Q11 | 团队规模与技能（是否有专职前端与设计师）？ | 30 |
| Q12 | 是否确认“自研”（ADR-000）？**已确认：自研**（ADR-000 为 Accepted） | 全局 |
| Q13 | 法务确认：是否需要 GDPR 第 27 条欧盟代表与英国代表？询盘与下载留资的合法性基础？中国大陆销售人员访问欧盟个人数据，以及经全球 CDN、美国服务商、Webhook 接收方的跨境传输的合规安排？CCPA 是否适用？加拿大访客的同意模式与 CASL？同意记录保留期限？**业主自行负责**（不在本文范围） | 17、18、20、26 |
| Q14 | Imprint 所需的公司法定信息（法定名称与法律形式、登记机关与注册号、增值税号、代表人、监管机关、内容负责人）？**业主自行负责**（不在本文范围） | 16.3 |
| Q15 | 欧洲与美国的流量占比？是否需要美国源站？**业主自行负责**（不在本文范围） | 27、ADR-015 |

---

## 附录 A：CLAUDE.md 草案

```markdown
# CLAUDE.md

本项目是 Modular B2B Website Platform。系统设计见 docs/architecture/system-design.md。

## 核心架构
- 模块化单体：编译期模块 + 站点级启用开关（不做热插拔）
- 站点是最高隔离单位：站点级数据必须带 site_id 过滤（行级拦截器自动追加；全局表列入豁免清单）
- 模块通信只有三种：platform-api 中的服务接口、领域事件、扩展点
- 内容内核：Content + Localization + Revision；发布生成不可变快照与只读投影
- 前台只读发布态数据（快照与投影），永远不读草稿
- Design System：前台样式只能使用 Design Token 与主题变体
- 渲染：系统内容用“模板 + 插槽”，营销页用页面编辑器；组件 Schema 在 frontend/packages/component-schemas

## 语言规则
- 管理后台只用简体中文：不做语言切换，不引入 vue-i18n，界面文案集中在常量 / 字典文件中；module.json、组件与模板定义中的名称、标签只写中文字符串
- 内容与站点资源只用简体中文（源语言 zh-CN）录入；其他语言由 translation 模块经 AI 翻译生成
- 前台所有用户可见文字必须经过文案键或翻译流程；组件中不得硬编码文案（中文或英文）
- 新增可翻译字段时，必须在 TranslatableContentProvider / TranslatableResourceProvider 中登记；组件属性用 `x-translatable` 标注是否需要翻译（见 10.4）

## 严格禁止
1. platform-core 依赖任何业务模块；模块 import 其他模块的包
2. 访问其他模块的表、跨模块 JOIN、跨模块外键
3. 在 platform-api 之外定义跨模块契约
4. 页面或组件硬编码业务字段；在布局文档中保存 URL（必须使用 $content / $media / $term / $source）
5. 重复实现已有组件；在前台写颜色、间距等字面量或新建 Token（Token 只能在 design-tokens 包中修改）
6. SEO 逻辑散落在业务模块中（业务模块只能实现 SchemaOrgProvider、SeoVariableProvider 等扩展点）
7. 修改已合入的数据库迁移脚本；在 MyBatis 中使用 ${}
8. 使用 v-html（富文本渲染器除外）
9. 为实现单个需求破坏模块边界；未经 ADR 修改核心架构

## 新功能先判断
- 属于现有模块，还是应该成为新模块？
- 应该是组件、模板、数据源，还是内容关系？
- 应该通过事件实现，还是通过扩展点实现？
- 是否需要新的跨模块契约（需要则先写 ADR）？

## 开发前必须
1. 阅读 docs/modules/<模块>.md 与相关 ADR
2. 确认模块边界与数据模型
3. 再开始编码

## 提交前必须通过
- 后端：./mvnw verify（含架构测试与集成测试）
- 前端：pnpm lint && pnpm typecheck && pnpm test
- 涉及 API：重新生成 api-client 并提交
- 涉及组件 Schema：破坏性变更必须提升版本并附迁移函数

任何跨模块修改都必须在 PR 中说明原因。
```

---

## 附录 B：ADR 索引

M0 阶段把 2.5 节的摘要整理为正式的 ADR 文件。ADR-000、ADR-005、ADR-009、ADR-017 为 Accepted，其余为 Proposed，评审通过后改为 Accepted。

| 编号 | 文件 | 主题 |
|---|---|---|
| ADR-000 | `docs/adr/0000-build-vs-buy.md` | 自研还是基于现有 Headless CMS（Accepted：自研） |
| ADR-001 | `docs/adr/0001-compile-time-modules.md` | 编译期模块 + 运行时启用开关 |
| ADR-002 | `docs/adr/0002-module-communication.md` | 公开 API、事件、扩展点；契约集中在 platform-api |
| ADR-003 | `docs/adr/0003-event-reliability.md` | 事务后投递 + Event Publication Registry |
| ADR-004 | `docs/adr/0004-content-kernel.md` | Content / Localization / Revision；快照与投影 |
| ADR-005 | `docs/adr/0005-i18n-and-url-strategy.md` | 语言策略与 URL |
| ADR-006 | `docs/adr/0006-template-and-page-builder.md` | 模板 + 插槽与页面搭建两种模式 |
| ADR-007 | `docs/adr/0007-editor-canvas-iframe.md` | 编辑器画布使用 iframe |
| ADR-008 | `docs/adr/0008-seo-data-ownership.md` | SEO 数据归属与自动值 / 覆盖值 |
| ADR-009 | `docs/adr/0009-site-and-locale.md` | 站点与语言模型（不设租户层） |
| ADR-010 | `docs/adr/0010-admin-authentication.md` | 后台认证方案 |
| ADR-011 | `docs/adr/0011-technology-versions.md` | 技术版本锁定 |
| ADR-012 | `docs/adr/0012-cache-tags.md` | 两级缓存与 Cache Tag |
| ADR-013 | `docs/adr/0013-search-read-model.md` | Meilisearch 作为搜索读模型 |
| ADR-014 | `docs/adr/0014-id-strategy.md` | TSID 与字符串序列化 |
| ADR-015 | `docs/adr/0015-hosting-region-and-data-residency.md` | 部署区域与数据驻留：源站与数据在欧盟，全球 CDN |
| ADR-016 | `docs/adr/0016-privacy-compliance-baseline.md` | 隐私合规基线：GDPR / UK GDPR / ePrivacy + CCPA/CPRA |
| ADR-017 | `docs/adr/0017-ai-translation-and-translation-memory.md` | AI 翻译与翻译记忆 |

---

## 附录 C：后续详细设计文档

按以下顺序编写；每份文档完成并评审后，再开始对应模块的编码。

| 顺序 | 文档 | 依据章节 | 需要在何时完成 |
|---|---|---|---|
| 1 | 《领域模型与数据库设计》：完整 ER 图与 DDL | 5–20、25 | M0 结束前（M1 涉及的部分） |
| 2 | 《Module SPI 与扩展点详细设计》：接口签名、注册与过滤、生命周期 | 3 | M0 |
| 3 | 《内容发布、快照与投影详细设计》 | 4.4、6 | M1 开始前 |
| 4 | 《URL 与 SEO 引擎详细设计》 | 13、14 | M1 开始前 |
| 5 | 《Page / Component / Template JSON Schema 规范》：含数据绑定、嵌套、多语言、版本迁移 | 10 | M2 开始前 |
| 6 | 《Design System 规范与主题开发指南》 | 11 | M2 开始前 |
| 7 | 《REST API 规范与 Delivery API 定义》 | 24 | M1 开始前 |
| 8 | 《询盘与反垃圾详细设计》 | 17 | M1 开始前 |
| 9 | 《Content Graph 详细设计》 | 15 | M3 开始前 |
| 10 | 《部署、运维与运维手册》 | 27 | M4 |

