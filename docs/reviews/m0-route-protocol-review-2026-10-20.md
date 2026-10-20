# M0 路由互斥与 generation 协议复审

| 项 | 记录 |
|---|---|
| 编号 / 日期 | M0-2026-10-20-03 / 2026-10-20 |
| 承接 | [事务协议复审](m0-transaction-protocol-review-2026-10-20.md)、开发计划M0-07B |
| Git基线 | 已先提交并推送7184981568590c128d7c43dc7e540cfca1a14747至claude/brave-heisenberg-bpy3dh；本批在该基线上继续 |
| 设计基线 | 系统V1.7、框架V1.5；content/url/page及ADR-022同步实施规则，ADR仍Proposed |
| 实测结果 | **65项全部通过**，失败/错误/跳过均0：此前46项回归+新增19项路由原型 |
| 证据 / 复现 | [机器可读结果](evidence/m0-route-protocol-2026-10-20.json)、[验证说明](../../tools/verification/m0-compatibility/README.md) |
| 准入结论 | M0-07B路由协议原型切片完成；下一项M0-07C真实Redis/CDN，整体M0/契约冻结仍未通过 |

## 1. 已运行的路由时序

新增RouteProtocolModel与RouteProtocolTest，使用文档中的sys_transaction_mutex/sys_job、cnt_/pg_/url_和sch_真实表；按适配器分开SQL归属，任务由content编排，url不反向读取其他模块。

| 范围 | 已验证行为 |
|---|---|
| 互斥与隔离 | 同键首次建行等待后只有一行；无事务/非法命名空间拒绝；不同站点已建记录的行锁互不阻塞；冷建行可等待但generation独立；输入回滚时generation及任务一起回滚；输入不变不推进代次 |
| 当前树与旧任务 | 显式RC事务，等待互斥前读到旧slug、等待结束后按最新输入重算；generation变化拒绝旧游标，替代任务按当前父关系重算；RR执行入口直接拒绝；环形父关系整笔回滚 |
| 分批与检查点 | 父先子后时父ID可大于子ID；检查点按拓扑序定位后继；语言集合规范化去重，语言+contentId标识位置；损坏游标和批量0/101拒绝 |
| 原子写入 | 路由、AUTO重定向、content路径投影、sys_job检查点及sch_change/change_seq在同事务回滚；第二项路径冲突不能留下第一项成功；保留占用方，解除冲突并重新认领后从原检查点恢复 |
| 恢复与认领 | 批次提交后丢失响应，依据持久检查点继续；完成任务重跑不重复重定向/搜索日志；取消保留已提交前缀，下一代任务从当前事实修复；同worker_id被复用也不能凭旧认领attempt继续提交；FAILED任务不能直接继续 |
| 重定向 | 多次改路径后，历史路径直接指向本地化，再解析其当前路由，不制造PATH跳转链 |

每种受控锁竞争先查询performance_schema.data_lock_waits确认阻塞连接，再释放先手事务。19是实际执行方法数；前述表格归纳了同一方法内的多个断言。实验只有5个页面、2种语言，属于协议正确性验证，不能作为吞吐、全站响应时间或容量结论。

## 2. 发现并修正的实现细节

### 互斥初始化不能先拿共享锁再升级

负对照在已存在的互斥记录上先执行INSERT IGNORE，再请求FOR UPDATE，稳定复现MySQL1213死锁：两个事务各持共享锁并等待升级。原型改为INSERT ... ON DUPLICATE KEY UPDATE id=id取得排他锁，再读取generation；同时要求调用方已有事务。

这不代表所有InnoDB物理争用消失。首轮“两个新站点零等待”的断言实际触发锁超时；进一步记录到uk_tx_mutex上的supremum pseudo-record：阻塞方X锁，等待方X,INSERT_INTENTION。这是唯一索引冷建行的间隙争用，记录建立后的站点行锁独立测试通过，generation始终分别为1和0，没有跨站写入。

因此实施要求为：站点初始化宜用短事务预建已注册互斥行；动态缺行仍支持排他upsert；1213/1205按整个调用方事务回滚并由外层限次重试，不在已经部分写源数据的事务里仅重试锁语句。不得将站点隔离描述成没有任何底层索引争用。

### 稳定 ID 不是按 ID 大小翻页

测试树的ROOT=930、CHILD=910、GRANDCHILD=900。确定拓扑序为930→910→900→920→940，批次结束在930后，使用contentId>930会漏掉子孙。当前检查点携带语言、最后完成contentId及完成位置；相同generation下从确定序列定位后继，generation变化则重新开始。无效检查点必须拒绝。

### 去重、租约与原子边界必须完整

同一generation下，en、de与{de,en}是三个不同范围；{en,de}与{de,en}则相同。任务键必须包含规范化语言集合。每批取得互斥后再读取租约/可运行状态；仅比较worker_id不足以阻止同身份复用后的旧worker。路径、投影、检查点及捕获日志同事务，正式事件登记也必须遵循该事务边界。

框架4.10.5/6.7与ADR-022已补充这些规则。url第10章旧url.recalculate_paths注册已移除，content第10章新增content.recalculate_routes，完整接口签名仍只在框架定义。119个CREATE和3个ALTER保持不变，不新增业务表/字段。

## 3. 验证边界与下一步

原型不等于真实ContentApi/UrlApi、JobApi调度器、ContentTypeProvider/RouteHints实现或真实连接池。租约认领/暂停/恢复通过受控数据库夹具模拟；没有执行进程kill、TCP断连、完整站点生命周期、所有URL模式/语言前缀修改入口、URL墓碑/手工规则全矩阵或批量100项容量验证。路由的sch_捕获只证明当前事务日志原子性，没有验证Meilisearch构建/切换。ContentPathChangedEvent与真实监听器链仍待正式服务集成。

| 后续工作 | 完成条件 |
|---|---|
| **下一项：M0-07C缓存** | 真实Redis代次fence、旧loader、重启、CDN失败持久重试、freshUntil端到端预算 |
| M0-07/core正式实现 | 连接失联后的事务回滚/恢复、完整限次重试、站点预建、真实租约、事件及所有源入口接入；将本批模型场景移植到真实服务/testkit |
| 相似初始化路径复查 | graph的INSERT IGNORE初始化集合后FOR UPDATE、通用JobApi幂等读取等路径，在M0冻结前复用本批死锁/旧快照检查；本批不据此宣布这些实现已经验证 |
| M0-08搜索 | 明确容量/延迟阈值，实测代次复制、源行锁成本、迟到worker、CAS及GC；DR-05仍阻塞冻结 |
| M0完整G0 | Security/Session、独立模块迁移、正式工程/core/testkit/CI与人工签审全部达标 |

前批46项证据保留，本报告追加路由结果，不把协议原型改写成已实现的业务模块。视觉方向仍为Tesla参考，高保真页面及正式UI实现不在本批范围。

后续增量记录：见[M0-07C缓存协议复审](m0-cache-protocol-review-2026-10-20.md)，新增37项、后端累计102项通过；本文件历史结论与原证据保持其当时范围。
