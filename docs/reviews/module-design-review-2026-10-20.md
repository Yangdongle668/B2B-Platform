# 模块需求设计复审总结

| 项 | 结果 |
|---|---|
| 日期 | 2026-10-20 |
| 审查范围 | docs/modules 下全部 17 份模块设计及索引；对照系统设计、平台框架和相关 ADR |
| 文档版本 | 17 份模块文档统一为 V1.2，继续保留“草案”状态 |
| 总体判断 | 已修正文档中可由现有约束确定的问题，补足接口与异常验收闭环；仍有 11 个具体契约/一致性问题需要在对应切片开工前统一（不含 M0 骨架前提） |
| 修改边界 | 仅修改文档；未引入代码骨架、运行依赖或新跨模块签名；原有 SQL 代码块全部保留 |

## 1. 主要发现与修正

| 编号 | 原问题 | 本轮处理与实际收益 |
|---|---|---|
| R01 | navigation.item、product.attribute_option 不在框架资源常量中，聚合 ID 也不匹配 | 导航统一 navigation.menu/menuId；选项按 product.attribute/attributeId 聚合，使用稳定 item/option ID 定位单元。单项删除不再误发整个资源的 DELETED。 |
| R02 | 普通编辑器保存混用无版本的 markDraftChanged；内容创建流程先写业务行但尚未取得 ID | 普通保存同事务使用 updateDraftMeta(expectedVersion)，共享字段明确 sharedDataChanged；先取身份再写业务行并重算完整哈希，失败整体回滚。 |
| R03 | 只按“第一个非空字段”安排验收，导致 translation/tracking 的 M3 功能和 page 的 M2 编辑器被写成 M1 | 逐功能核对框架 13.5，修正 A1–A10 里程碑和 13.1 场景；跨切片通用项明确只验收当前已交付端点。 |
| R04 | 多个管理对象只有 PUT，没有创建、详情、删除/恢复或任务项读取入口 | 补充 56 条后台 HTTP 端点设计，复用现有权限；delivery 的数据源目录增加独立查看权限。它们是接口需求，不是已实现 API。 |
| R05 | 草稿引用和发布引用混淆，部分文档调用了 MediaApi 不存在的引用写方法 | DRAFT 经现有草稿事件 + collectDraft，PUBLISHED 经 ReferenceSet.scan；关闭已可由既有通道解决的“新增写方法”问题，作者独立资源及删除竞争继续单独跟踪。 |
| R06 | 大量事件表只有“见框架/失败重试”，清理时可能因父对象已删除而跳过 | 列出真实 payload 字段和订阅方处理，区别源资源变化、翻译回写、模块停用和物理删除；明确停用期间的 purge 仍执行、恢复后补偿当前状态。 |
| R07 | 上传、验证码重试、发件箱及取消任务缺少部分失败定义 | 补对象/数据库补偿、一次性验证码之前的幂等命中、有效语言固定、租约校验、未知发送结果、暂停/取消后已发生费用等要求。 |
| R08 | 缓存失效、旧事件与重建切换只有笼统的“按版本幂等” | 添加故障注入时序，指出 getOrLoad 回填竞争、搜索 upsert 无 CAS、全量扫描不是增量屏障及 swap 不能盲目重放；未把未冻结协议写成已解决。 |
| R09 | 导出/报表和批量翻译的权限范围不够明确；同意撤回存在过度承诺 | 导出/报表声明数据范围并在执行/下载时重验；翻译操作逐目标语言检查；GPC 覆盖相关分析传输，撤回不声称可擦除第三方域 Cookie。 |
| R10 | Graph 的规模估算超过系统设计，404 缓冲却被描述成无丢失计数 | Graph 验收回到单站≤50万行，topK 按总量预算调整；404 使用 BufferStore 的实际 drain-and-clear 语义，明确故障下统计不确定性。 |
| R11 | 存在指向本模块并不存在的第22/23章及旧数据库章节引用，还有未经证实的“DDL已验证”字样 | 改为实际框架/本模块章节；删除无运行证据的验证结论，保留 DDL 文本并要求后续 MySQL 8.4 实测。 |
| R12 | 统一写“4xx、回滚、重试”和通用性能目标，难以形成测试 | 每模块新增4条专项行为与故障验收，补公开错误映射、独立任务名称及适用性能指标，避免用单一模块锁阻塞全部任务。 |

## 2. 逐模块改进记录

| 模块 | 改进重点 | 本轮新增后台端点设计 | 当前验收项 |
|---|---|---|---|
| [content](../modules/content.md) | 修正先写业务表再取得 ID 的创建顺序；区分普通编辑的版本检查与系统 markDraftChanged；收紧预览与修订保留。 | 0 | 14 |
| [url](../modules/url.md) | 补齐规则创建/删除与预检结果读取；纠正 RouteHints 调用方向，明确 404 计数是尽力统计而非持久审计。 | 4 | 14 |
| [media](../modules/media.md) | 区分 collectDraft 和发布快照扫描；补偿对象存储部分失败，明确引用事件空窗与删除竞争。 | 2 | 14 |
| [taxonomy](../modules/taxonomy.md) | 补齐分类详情；明确树移动原子性、草稿指派/发布投影和词汇表 ID/分类项 ID 的区别。 | 1 | 14 |
| [form](../modules/form.md) | 补齐重新启用、删除与授权附件下载；说明验证码的幂等重放、告知历史、附件绑定及门控重试。 | 3 | 14 |
| [notification](../modules/notification.md) | 补齐发送/配置详情；锁定 effectiveLocale，明确发件箱租约、未知投递结果、退避与 Webhook 保护。 | 2 | 14 |
| [translation](../modules/translation.md) | 补齐任务项、术语、审校确认等入口；收紧源修订、租约、部分回写、人工优先、预算币种和取消计费。 | 9 | 14 |
| [product](../modules/product.md) | 补齐属性组、选项、属性集、分类绑定和导入预检入口；选项翻译并入属性聚合，补导入/导出安全与共享版本。 | 13 | 14 |
| [article](../modules/article.md) | 补齐案例/FAQ/作者详情；普通内容引用使用已有 Collector，作者独立资源通道单独跟踪，明确 FAQ 与 JSON-LD 责任。 | 3 | 14 |
| [page](../modules/page.md) | 补齐页面/区块详情和组件目录；布局保存用内容共同版本，修正 M2 编辑器/M3 法律页验收，保留稳定节点 ID。 | 4 | 14 |
| [inquiry](../modules/inquiry.md) | 补齐规则、销售组、日历、附件、批量跟进入口；导出/报表受数据范围限制，加入 SLA、DSR 与翻译竞态验证。 | 10 | 14 |
| [navigation](../modules/navigation.md) | 翻译以 navigation.menu/menuId 为聚合；补人工可见性入口，纠正项删除/菜单删除事件语义与 URL 安全检查。 | 1 | 14 |
| [delivery](../modules/delivery.md) | 补齐框架已要求的后台数据源目录；明确最多10个源/每源24项、列表更新失效、回填竞态和可靠失效边界。 | 1 | 14 |
| [seo](../modules/seo.md) | 补齐模板创建/删除；明确共同版本、公开 hreflang 集合、lastmod、双阈值 sitemap、JSON-LD 转义及扫描失败呈现。 | 2 | 14 |
| [graph](../modules/graph.md) | 修正超过顶层规模的估算，以单站≤50万关系行为验收；定义脏队列版本检查，暴露人工集合版本来源缺口。 | 0 | 14 |
| [search](../modules/search.md) | 区分受理与成功，明确 PROCESSING/FAILED、重建增量屏障和 swap 崩溃恢复；搜索页失败不伪装为空结果。 | 0 | 14 |
| [tracking](../modules/tracking.md) | 修正全部专项验收到 M3；补脚本详情，明确地区查询失败、GPC/分析出售共享、撤回边界及静默限流。 | 1 | 14 |

## 3. 尚需统一的具体设计问题

以下是文档层面的实现前提，当前没有可运行应用，不代表已证实存在运行中的安全漏洞。问题编号均为仓库文档内编号，不冒充外部 Issue；每个模块第14章记录了负责人角色、处理要求和截止阶段。

| 模块 / 编号 | 问题 | 最晚处理阶段 |
|---|---|---|
| [article / ARTICLE-01](../modules/article.md) | 作者的 TranslatableResourceProvider 与资源事件已在 4.6 要求，但框架 4.2.3/4.2.4 未登记 article | M2 作者头像通道；M3 作者翻译 |
| [content / #1](../modules/content.md) | translation_review_required 的写入途径未在 ContentApi 明确 | M3 开工前 |
| [delivery / DELIVERY-02](../modules/delivery.md) | TaggedCache.invalidate 与 in-flight getOrLoad 回填竞态，以及 BufferStore 临时合并丢失后的失效重试未形成闭环 | M2 缓存失效开工前 |
| [form / FORM-01](../modules/form.md) | 框架 NotificationApi 将 form 列为使用方，但 L2 调用白名单只允许 form→media；下载链接邮件未有一致调用方向 | M3 留资邮件开工前 |
| [graph / GRAPH-01](../modules/graph.md) | grh_relation 没有集合聚合 version，PUT relations 声称 If-Match 原子替换但现有数据模型未定义版本来源 | M3 人工关系开工前 |
| [media / MEDIA-01](../modules/media.md) | 框架 4.4.16 将 media 列为 ContentApi 读取方，但 4.1.3 明确禁止 media→任何 L2；发布态扫描与最新版本读取的调用方向不一致 | M1 引用追踪开工前 |
| [media / MEDIA-02](../modules/media.md) | mda_usage 异步更新与媒体删除存在空窗，现有空引用查询不是完整安全证明 | M1 引用追踪开工前 |
| [notification / NOTIFICATION-02](../modules/notification.md) | 框架事件接收人选择用 translation:review，但附录 A 权限码要求三段且模块声明 translation:memory:review | M3 翻译通知开工前 |
| [search / SEARCH-01](../modules/search.md) | SearchEngineClient.upsert 没有 compare-and-set，扫描+swap 未定义增量屏障；多实例重建与旧事件可相互覆盖 | M4 索引同步开工前 |
| [tracking / TRACKING-01](../modules/tracking.md) | trk_setting 与脚本保存后的 settings 标签失效缺少已登记的事件通道 | M3 追踪配置开工前 |
| [url / URL-02](../modules/url.md) | 父页面移动/语言前缀变化任务缺少允许方向内的 RouteHints 输入和重启后重新取当前树的协议 | M2 子树移动开工前 |

M0 仍需建立工程骨架、冻结 platform-api、交付 testkit，并登记真实模块负责人。语义冲突集中在框架层：media 的快照读取、form 的下载邮件、作者资源注册、翻译通知接收人权限；不能在模块里各自增加跨模块方法或绕过表边界。`content` 的预览覆盖问题、内容/导航的部分媒体上报疑问，以及 url 的非法反向查询，已根据现有契约澄清，未继续作为重复阻塞项。

建议处理顺序：M1 先确定媒体输入方向与删除/引用窗口；M2 统一作者头像通道、子树重算输入、缓存回填与可靠失效；M3 统一作者翻译资源/法律审校标记、留资邮件、通知权限、图谱集合并发及 tracking 变更通道；M4 统一搜索索引更新与切换协议。涉及真正跨模块契约变更时，按现有 ADR/框架4.9流程形成具体提案并同步 testkit。

## 4. 验证结果与限制

| 检查 | 本轮结果 |
|---|---|
| 17份描述符 | 全部通过平台框架附录A的 draft 2020-12 JSON Schema |
| 结构与编号 | 每模块1–14章齐全；每模块A1–A14连续，共238条；无未填模板标记 |
| 依赖与全局唯一性 | 依赖无环；L2方向符合当前白名单；96个权限码、36个组件类型和菜单key无重复 |
| 接口与权限 | 当前234条后台端点设计（审查前178条），无同模块 method/path 重复，使用权限均有声明，写操作都有审计要求 |
| 授权一致性 | 描述符、权限表的 dataScopes/localeScoped 一致；服务方法引用可在框架找到 |
| 里程碑 | 不再有验收早于模块首个有效切片；页面编辑器、翻译与追踪等专项进一步按功能切片人工核对 |
| 资源与引用 | 已移除废弃资源类型；Markdown链接、ADR引用与已有章节可解析；article.author 的缺口显式跟踪 |
| SQL保护 | 92个SQL代码块与本轮复审前逐字相同；未改变既有表结构 |
| Git差异 | git diff --check 通过；检查的是累计文档改动，未覆盖或丢弃上一轮工作 |
| 未执行 | 应用构建、单元/集成/E2E、MySQL DDL执行、浏览器与性能测试；仓库尚无代码骨架 |

静态检查脚本在本任务临时目录执行，结果记录于本报告；没有把校验脚本或新的构建系统加入文档仓库。静态检查能发现结构/声明不一致，不能证明业务并发协议、外部传输、DDL或性能正确。最终开发就绪仍以 DoR、契约确认及相应运行验证为准。
