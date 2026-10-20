# ADR-027：M0设置值与模块启动回调统一

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-20 |
| 决策者 | 架构负责人（待签审） |

## 背景

框架4.4.3及sys_setting定义要求每个设置值是带schemaVersion的对象；content.reviewRequiredTypes却声明数组，inquiry.messageTranslation声明布尔值，其他默认对象也漏掉版本字段。框架5.11.2的InquiryRetention示例与inquiry模块的closedYears/spamDays不一致。5.2.4要求每次启动同步扩展声明，却没有对应生命周期回调。

## 候选决策

保留所有设置键。content.reviewRequiredTypes改为{schemaVersion:1,types:[]}，inquiry.messageTranslation改为{schemaVersion:1,enabled:false}；其他对象补schemaVersion:1。InquiryRetention采用模块定义的closedYears/spamDays。Schema文件与默认值一同验证，未声明键拒绝，不以空对象默认值掩盖缺项。

PlatformModule增加默认onStartup(ModuleContext)，每次启动在迁移及安装/升级完成后，按依赖拓扑顺序调用，必须幂等；不得依赖当前站点过滤。默认空实现保留现有提供方兼容性。此变更属于尚未发布1.0.0的RC修正，仍需正式评审，不代表G0通过。

Core设置候选的字段与默认值集中记录在框架5.11.2及版本化Schema：SMTP默认STARTTLS与空host（选择部署级默认）；保留期365/180/7天；theme空覆盖使用主题默认值；features初始无覆盖；DSR保持6.4.22的既有默认期限。它们不是法务签审或生产配置，生产明文SMTP仍被拒绝。
