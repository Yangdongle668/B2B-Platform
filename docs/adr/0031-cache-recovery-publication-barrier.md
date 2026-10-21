# ADR-031：缓存恢复的持久边界与事件发布屏障

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待正式架构评审） |
| 日期 | 2026-10-21 |
| 决策者 | 业主、架构负责人（待签审） |
| 依据 | ADR-020、框架4.10.3、5.8、5.10 |

## 背景

隔离Lua原型证明了Redis run_id/epoch和回填代次，但恢复时仅查询一次pending=0无法证明跨MySQL/Redis边界。M0正式运行机制必须有持久边界，不能靠重启时清内存缓存放行。

## 候选决策

- Core追加全局技术表sys_cache_recovery和sys_cache_recovery_publication。源事务发布DomainEvent前取得单例恢复行共享锁，持有至事务结束；恢复事务取得同一行排他锁，记录本轮boot/epoch并快照全部未完成Event Publication ID。业务事务仍可并发持有共享锁；恢复不访问业务表。
- Redis的Lua在每次读取、capture、写入和失效时读取本次server run_id。run_id/epoch未匹配或ready未开启时读绕过、capture为空、写拒绝；失效不可用必须抛错保留重投。计数器按精确十进制字符串比较，溢出换incarnation，物理清理分批。
- 恢复者由独立连接MySQL命名锁串行化。持久边界内全部义务确认完成后，逐站清CDN；确认成功再以boot/epoch CAS放行，过期恢复者不能开启新进程。失败保留DRAINING状态并重试，禁止自动豁免。
- 没有CDN提供方或Redis不可用时继续绕过缓存，回源响应期限置为当前时刻以得到no-store，不伪造成功。回源期间代次变化同样不得向下游授予新缓存期限。
- RedisTaggedCache与恢复协调器置于Core内部的cache，避免Core依赖infrastructure或为内部恢复增加跨模块接口；替代5.10.1原建议的infrastructure.redis包位置。端口TaggedCache与模块依赖方向不变。
- 外部CDN清除在数据库事务外执行。纯失效监听器允许重复执行，不提前写完成标记；Redis与CDN均成功后由Modulith登记完成。

## 影响与限制

增加两个兼容的技术表和短共享锁，不修改已合入迁移。所有通过公开事件发布器发布的领域事件遵守屏障；绕过发布器、直接写业务表不是受支持的保存入口。Redis Cluster、提供商ACL及生产容量须在部署选型时复核；本地CDN故障夹具验证协议，不代表真实云CDN接入或人工采纳。
