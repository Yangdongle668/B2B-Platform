# M0-07C 缓存协议复审与执行结果

| 项 | 记录 |
|---|---|
| 评审编号 / 日期 | M0-2026-10-20-04 / 2026-10-20 |
| 输入基线 | 已推送868c2b51237aa8842b372409533ef4adf31f7f76，工作分支claude/brave-heisenberg-bpy3dh |
| 对象 | 框架4.7.7/4.7.14/4.10.3/5.10、系统23章、delivery及ADR-012/020，隔离协议模型 |
| 结论 | **本批缓存协议原型通过；不等于正式业务实现、ADR采纳或完整G0通过** |
| 执行证据 | [机器记录](evidence/m0-cache-protocol-2026-10-20.json)、[复现说明](../../tools/verification/m0-compatibility/README.md) |
| 当前规范 | 系统V1.8、框架V1.6；完整跨模块签名仍只在框架定义，ADR-020保持Proposed |

## 1. 实际执行与覆盖

前批路由验证累计65项。本批新增37项，八个后端套件累计**102项通过，失败/错误/跳过均为0**。使用真实MySQL8.4.11、Redis7.4.11、JDK25.0.4.1、Boot4.0.8、Modulith2.0.8、Lettuce6.8.2.RELEASE。镜像digest与逐项方法名保存在机器记录。119个CREATE及3个ALTER在开启外键检查的空库重跑成功，并与输入提交逐块比较完全相同。

| 套件 | 实际测试数 | 覆盖及反例 |
|---|---:|---|
| CacheProtocolTest | 15 | 受控旧loader与失效交错、错误capture顺序负对照、标签索引丢失后的读保护、key/namespace失效、站点隔离、过期拒写、代次元数据丢失/TTL、2^53以上精确计数、long溢出换incarnation、恢复读写门闩、过期恢复者、断开连接、真实Redis进程重启及旧RDB恢复 |
| CacheInvalidationTest | 8 | 源事务回滚；提交后Redis和CDN均成功才完成；HTTP503、200但success=false、损坏JSON三种失败；执行purge后丢失确认；Redis断连及恢复门闩；关闭并重新创建Spring应用上下文后从持久记录重投 |
| FreshnessProtocolTest | 14 | 两个可信后端依赖取最早期限并扣渲染时间；真实HTTP跨后端/SSR/edge夹具；命中保留期限与Age；过期且源故障503/Retry-After；伪造浏览器头；错误/缺失期限；回源超过5秒；余寿命不足1秒；200/301/404/410共享期限；预览/私有no-store |

可靠事件使用真实Spring Modulith JDBC Event Publication Registry，不以自造内存队列替代。失败记录须为FAILED且未完成，再显式调用IncompleteEventPublications重投；完成条件同时包含Redis失效与CDN业务确认。事件使用独立UUID和精确EVENT_TYPE/JSON字段过滤；兼容套件原来的模糊LIKE查询也改为按事件类型及数字ID精确查询，防止新增事件串扰。

Redis重启用容器内SAVE及SHUTDOWN SAVE，由该临时容器restart策略重新启动；测试确认旧epoch、ready=1及值键确已从RDB恢复，实际run_id已变化，此时get/capture/旧token写入仍全部拒绝。不是仅重建Java客户端或清空Redis后宣称通过重启验证。所有临时容器、匿名卷及网络按随机前缀清理。

## 2. 发现并统一的规则

### 2.1 旧ready和代次也可能从快照恢复

只在Java启动时换一次UUID不足以覆盖Redis独立重启。原型每次Lua原子操作读取本次Redis进程run_id，并校验control.boot、epoch与ready；恢复出的旧ready不能自行开闸。beginRecovery关闭读与回填并轮换epoch，各站点惰性生成新的incarnation；只有当前boot/epoch对应的恢复者可finishRecovery。代次键不得先于条目过期；计数按十进制字符串精确比较，达到long上限后轮换incarnation，避免Lua浮点舍入及计数复用。

恢复原型的pending判断建立在静止源写入夹具上；**一次MySQL pending=0查询与Redis开闸之间不构成跨存储原子屏障**。正式协调器仍须持久记录恢复边界并保证该边界内义务全部重放；不能直接复制BooleanSupplier作为生产恢复协议。缺少证明时保持绕过缓存。恢复之后新提交事件按正常持久投递与freshUntil预算处理。

### 2.2 标签删除和旧回填必须分开证明

capture必须在loader计算之前发生，之后才capture会把旧值登记成新值，负对照已复现。invalidate/evict/evictNamespace先推进代次，put在同一Lua操作校验代次后写入；get也比较代次。标签集合丢失时，即使物理值还在，读仍拒绝。V1因此接受站点范围逻辑失效及额外回源成本；标签只是物理清理优化。

失效不能撤销已在途调用已经取得的旧结果。测试中的旧调用可以返回旧body，但不能覆盖新缓存；公开响应仍须服从自身原期限和回源时限。不得把此结果描述为即时线性一致的所有HTTP响应。

### 2.3 可靠失效不能先从可丢失缓冲取走义务

框架CdnPurger注释、关系/资源事件说明、BufferStore例子、RedisTaggedCache实现示例、delivery核心流程和定时任务仍残留2秒去抖路径。本批统一为监听器直接Redis失效后purge，二者成功才完成持久投递；不再注册delivery.flush_cache作为可靠清除任务。ADR-012同步指向ADR-020，不改变采纳状态。

HTTP200并不等于purge业务成功。业务失败、损坏响应或执行后确认丢失均保留投递；重投可以再次推进代次并重复清除。未来如引入合并任务，仍按框架4.10.3先持久移交义务，不能回到pollDue后内存承担完成责任。

### 2.4 多层缓存不能重置新鲜度时钟

父结果取各可信依赖最早freshUntil，扣除回源/渲染耗时。CDN TTL=floor(min(55秒, freshUntil-now))，<=0、缺少可信期限、预览/私有或超时均no-store；动态浏览器缓存max-age=0并重验，edge命中保留原期限且递增Age。源故障时不能用stale扩展突破期限，返回503及Retry-After120。浏览器请求头不能提供可信的freshUntil。

## 3. 实施边界与下一项

本批代码均为src/test中的协议模型或测试配置，不是正式RedisTaggedCache、Delivery/Nuxt中间件、CloudflareCdnPurger或恢复协调器。HTTP传输真实，后端/SSR/edge由loopback夹具模拟，时间由同一个可拨动时钟推进；未验证真实CDN配置、缓存标签头、网络重试调度、多个节点的时钟偏差或浏览器私有态集成。没有使用真实CDN凭据或向公众站点发送purge。

Redis使用单节点且允许脚本读取INFO；尚未证明Cluster键槽、受限ACL、代理/failover时的进程身份或AOF/主从切换。小集合Lua中的SMEMBERS/UNLINK及命名空间SCAN只用于正确性夹具，正式清理必须有界分批，并验证负载和超时；不把本批当吞吐或容量证据。Redis断连测试关闭真实客户端连接，进程重启测试则确实重启Redis，二者范围分开记录。

| 后续工作 | 完成条件 |
|---|---|
| **下一项：M0-07D相似SQL初始化路径复查** | graph集合状态INSERT IGNORE→FOR UPDATE和JobApi幂等读取，复用路由的死锁/当前读反例；修正完整事务重试与返回语义后再进入RC |
| M0-08搜索 | 实测不可变代次复制的容量阈值、源锁成本、迟到worker/CAS/GC；DR-05仍阻塞冻结 |
| M0正式缓存/恢复实现 | 生产协调器的持久恢复边界、所有回源入口fence接入、真实CDN与HTTP头、后台重投退避/告警/保留策略、丢进程/网络故障与testkit契约复跑 |
| 完整G0 | Security/Session、独立模块Flyway及升级、工程骨架/core/testkit/CI、负责人落实和正式签审；21份Proposed ADR仍未自动采纳 |

历史证据保留，新增结果不反向改写前批执行状态。Tesla仍是前台视觉参考；本批未新增视觉稿或UI成品。
