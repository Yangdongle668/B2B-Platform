# 11 项实现前设计问题的统一处理说明

> 后续正式评审发现部分统一方案仍有字段归属、生命周期和失败语义冲突。本文保留提案形成时的记录；当前开工结论与整改项以[正式评审FR-2026-10-20](formal-design-review-2026-10-20.md)为准，不能把本文“方案已补齐”当成冻结批准。

| 项 | 当前结果 |
|---|---|
| 日期 | 2026-10-20 |
| 基线 | [模块复审记录](module-design-review-2026-10-20.md)提出的11项具体问题；不含M0工程骨架前提 |
| 本轮交付 | 已为11项补齐具体方案、契约归属、数据结构、事务顺序、重试/恢复与故障验收要求 |
| 权威定义 | [平台框架4.10](../framework/platform-framework.md)；core表由框架6.7定义，graph/search表由各模块定义 |
| 评审状态 | 7份ADR为Proposed；通知权限名称修正与图谱集合版本已明确，其余关联新增契约待架构评审冻结 |
| 实现状态 | 仅文档，无应用骨架、迁移代码或运行测试结果；不能把方案补齐当作已上线修复 |

## 1. 逐项处理

| 原问题 | 统一方案与设计收益 | 契约/数据模型归属 | 最早必需阶段 | 评审与实施责任 |
|---|---|---|---|---|
| [MEDIA-01](../modules/media.md) | 内容拥有者事务内提供完整引用，media取消对ContentApi的反向读取 | 框架4.10.1；ReferenceWriteApi/MediaReferenceSink；[ADR-019](../adr/0019-transactional-media-references.md) | M1 | 架构、core/content/media负责人 |
| [MEDIA-02](../modules/media.md) | 媒体共享行锁保护新引用，删除持排他行锁并当前读引用；二者不能同时提交悬空引用 | 框架4.10.1；沿用mda_usage，无跨表访问 | M1 | media及所有源保存入口负责人 |
| [ARTICLE-01](../modules/article.md) | 登记article.author；头像引用同步登记，非文本资源变化使用独立生命周期事件，翻译只处理文本 | 框架4.10.1–2；[ADR-025](../adr/0025-site-resource-lifecycle.md) | M2头像；M3翻译 | article/media/translation/delivery负责人 |
| [URL-02](../modules/url.md) | 重算任务归content；事务互斥/generation保护当前树读取；重启/二次移动重新取得routeHints，父先子后更新 | 框架4.10.5、6.7；sys_transaction_mutex；[ADR-022](../adr/0022-current-route-recalculation.md) | M2 | core/content/page/url负责人 |
| [DELIVERY-02](../modules/delivery.md) | 站点代次阻止失效后旧回源写入；Redis与CDN均成功才完成持久投递，缓冲不承担唯一可靠性 | 框架4.10.3；TaggedCache新增fence方法；[ADR-020](../adr/0020-cache-invalidation-fences.md) | M2 | infrastructure/delivery负责人 |
| [content #1](../modules/content.md) | 可信内容拥有者同事务持久化审校策略，目标语言初始化继承；完成事件携带目标草稿版本，发布时复核当前策略 | 框架4.10.7；ContentApi/LocalizationInfo/TranslationCompletedEvent；[ADR-024](../adr/0024-translation-review-guard.md) | M3 | content/page/translation负责人 |
| [FORM-01](../modules/form.md) | 明确form→notification单向服务依赖，下载提交/门控票据/发件箱同事务；失败回滚、不反向回查 | 框架4.10.6；form.requires(notification)；[ADR-023](../adr/0023-form-notification-direction.md) | M3 | form/notification负责人 |
| [NOTIFICATION-02](../modules/notification.md) | 使用已有translation:memory:review，按目标语言、站点和用户状态筛选；不新增权限 | 框架4.6.2、4.10.6；既有UserDirectory/权限描述符 | M3 | notification/translation/core权限负责人 |
| [GRAPH-01](../modules/graph.md) | 集合有持久单调version；空集合保留状态，所有相关写入排序锁定受影响源集合并递增 | graph4.3.6；grh_relation_set；框架4.10.8 | M3 | graph负责人 |
| [TRACKING-01](../modules/tracking.md) | 原设置/脚本保存事务发布SiteResourceChangedEvent，delivery可靠清settings；不借翻译事件驱动缓存 | 框架4.10.2–3；ADR-025/020 | M3 | tracking/delivery负责人 |
| [SEARCH-01](../modules/search.md) | 源事务可靠捕获变更序号，私有代次构建并追平后CAS切换活跃指针，旧worker无权改活跃代次 | 框架4.10.4；search4.3.1；3张sch_控制表；[ADR-021](../adr/0021-immutable-search-generations.md) | M4 | core/search/infrastructure及事实发布方负责人 |

角色尚未登记到真实人员，M0仍需分配。保留各模块第14章原编号，状态从“缺少方案”改为“方案已补齐，待评审冻结/实现验证”，便于跟踪；不凭本次文档修改把ADR标为Accepted。

## 2. 关键时序与故障验收

下列是后续必须实现的测试要求，当前未创建应用测试或声称运行通过。testkit必须与真实提供方遵守同一协议，不能只模拟最终成功状态。

| 编号 | 故障/并发时序 | 必须观察到的结果 | 阶段 |
|---|---|---|---|
| D01 | 新引用事务取得媒体共享锁，删除并发执行 | 删除等待后看到已提交引用并拒绝；不能删对象后回滚数据库 | M1 |
| D02 | 删除先提交，旧编辑器随后保存指向该媒体 | 源保存失败并回滚业务与引用，无悬空行 | M1 |
| D03 | 草稿/发布/资源写入后人为抛错；另站使用同媒体ID | 原站业务与引用一起回滚；跨站媒体不可引用 | M1 |
| D04 | 发布引用与草稿不同、共享字段多语言变更、旧异步事件重放 | 两阶段分别保护，语言不相互覆盖；旧事件不重写权威引用 | M1 |
| D05 | 作者只改头像、不改职务；再删除作者 | 引用与作者同事务更新；发资源事件，不触发无效翻译；文章快照引用仍保留 | M2 |
| D06 | loader读旧值后失效，loader最后返回；同样时序插入Redis重启 | 旧代次结果不写缓存；重启后的新incarnation不接受旧token | M2 |
| D07 | Redis失效成功、CDN清除失败；事件重复投递 | 未完成投递仍存在；重试安全；两者成功才完成 | M2 |
| D08 | BufferStore pollDue后杀进程、Redis断开、Pub/Sub漏消息 | 缓冲丢失不丢持久失效义务；读绕过缓存、写拒绝旧代次；本地硬TTL兜底 | M2 |
| D09 | 已发布产品改标题/分类；多层缓存连续命中 | list和content依赖都失效；命中不重置总龄，正常依赖下满足60秒预算 | M2 |
| D10 | 移动父页后任务执行一批，再次移动/改前缀并重启 | generation变化废弃旧游标，按当前树父先子后重算，旧路径正确跳转 | M2 |
| D11 | 子树重算撞路径或中途取消 | 冲突批次整体回滚且任务可见暂停；先前批次仍可解释，重试无重定向链膨胀 | M2 |
| D12 | 创建legal及目标语言、翻译回写与模板更改并发 | 初始化即带可信审校策略；共同版本冲突不覆盖新草稿，legal不自动发布 | M3 |
| D13 | 翻译完成事件延迟到人工改稿/批准旧稿之后 | targetVersion/源修订/当前策略复核失败，旧事件和旧批准不能发布新版本 | M3 |
| D14 | DOWNLOAD事务写入票据后抛错；同幂等请求网络重试 | 提交/票据/发件箱一致回滚或复用同一结果，不重复线索或通知 | M3 |
| D15 | DOWNLOAD处于QUARANTINE/REJECT/测试模式；邮件模板配置错误 | 前三者不发邮件；配置错误不报告“提交成功但永远无邮件” | M3 |
| D16 | 审校人只授权de语言、用户停用或无接收人 | 不接收en/其他站通知；无接收人可观察告警，不广播降级 | M3 |
| D17 | 两窗口同ETag改关系；空→有→空后旧ETag再写 | 仅一笔成功；412；空集合version不复位 | M3 |
| D18 | 双向关系修改与AUTO重算竞争，制造死锁/计算过期 | 所有集合升序加锁，失败完整回滚；旧计算重验失败，不覆盖人工结果 | M3 |
| D19 | 只切换脚本开关/类别、无文本变化；CDN失败后重试 | 有独立资源事件，settings可靠失效；不触发翻译或重复脚本执行 | M3 |
| D20 | 内容发布源事务回滚；扫描前后分别插入/删除/改路径 | 回滚无sch_change；已提交变化有序重放，CAS不越过变更水位 | M4 |
| D21 | 旧builder失联后新attempt成功，旧worker恢复并发送写入 | 旧worker只污染自己的废弃代次，无法CAS发布或修改活跃代次 | M4 |
| D22 | 指针切换前/提交后杀进程，切换响应丢失 | 恢复读指针和构建状态，无双swap；旧代次一直可查到确认切换 | M4 |
| D23 | 引擎任务长期PROCESSING/FAILED；日志GC与长构建并发 | 不提前发布；日志保留到安全水位，活跃/在途代次不被GC | M4 |
| D24 | 持续写入使追平失败；1万产品/5千文章/10语言压测 | 保留旧可用指针、积压可观察；给出O(N)复制代价和吞吐报告，不强制跳过CAS | M4 |
| D25 | 引擎受理响应超时且请求迟到；复制基础代次有未处理(C,H0]变更并遇GC | 未知受理使整个候选代次废弃；复制从C重放，不漏存量增量；base_index仍被存活构建引用时不删除 | M4 |

## 3. 契约、数据和迁移范围

- core补ReferenceWriteApi、ProjectionChangeApi、TransactionMutexApi；新增协调SPI只通过platform-api定义，core不依赖业务实现。
- TaggedCache补beginLoad/putIfUnchanged、带freshUntil的读取接口与CacheWriteFence；SearchEngineClient补scanDocuments；ContentApi补路由任务/审校标记方法，读取模型和翻译完成事件补策略/目标version。
- 新增5张表：core.sys_transaction_mutex；graph.grh_relation_set；search.sch_index_state、sch_change、sch_build。原有SQL代码块保留，所有新增DDL均为草案，需MySQL8.4实测。
- form描述符增加notification依赖；search描述符tablePrefix改sch_。其余模块ID、层级、现有权限和组件类型不变。框架主L2方向表在评审前显式列出form→notification提案，不把新方向隐藏在模块实现里。
- 旧异步媒体引用写入、无fence回填、固定_rebuild/swap、url自行读取树的路径都必须在实现前移除；不能同时实现两套相互覆盖的协议。

仓库尚未冻结1.0.0契约，当前是M0草案增补，不存在已发布二进制迁移。日后冻结后如再变更，必须按框架4.9做版本、旧事件反序列化和testkit迁移；不能用本次“尚无代码”作为未来跳过评审的理由。

## 4. 文档检查与实施门槛

本轮检查内容：新增文件与引用、原SQL保留、新表归属、描述符Schema、L2依赖DAG、权限/组件唯一性、问题编号与方案映射、文档版本和ADR状态。当前静态检查通过：17份描述符符合框架Schema；原118个SQL块保留、新增5个；9项契约方案待评审、2项局部设计明确；链接/章节/表归属与L2 DAG检查无新增错误。详细数字以本轮最终检查为准；运行环境未建立应用，不执行或编造MySQL、Redis、Meilisearch、SMTP、浏览器或故障注入结果。

正式实施前按[ADR流程](../adr/README.md)和框架4.9评审新增契约，并同步真实实现、适配器、testkit与契约测试。M0工程骨架、真实负责人、原模块中其他非阻塞文档清理项仍需跟踪；这11项的方案补齐不等于整个项目已可上线。
