# ADR-030：预览用途、编辑器签发人与覆盖权限

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-21 |
| 决策者 | 业主、架构负责人（待签审） |
| 相关章节 | 系统设计6.5、10.8、26.2；框架4.4.16、8.3.13；content 5.9 |
| 相关 ADR | ADR-007、ADR-010、ADR-018 |

## 背景

content文档和表已区分PREVIEW、SHARE、EDITOR，但Java PreviewTarget没有purpose或签发人，createPreviewToken也不能显式签发EDITOR；Delivery无法区分分享链接和允许提交未保存布局的编辑器令牌。仅凭请求中的DeliveryMode或TTL认定EDITOR会让持有分享链接的人进入编辑器覆盖链。

权限复查还发现Permissions.forContent将TERM_PAGE映射为term_page，而已声明权限为content:term-page:edit；CASE_STUDY、GLOBAL_BLOCK有相同不一致。预览签发/续用必须采用与描述符相同的权限码。

## 决策

- 在唯一规范来源框架第4章增加PreviewPurpose；签发增加显式purpose重载，旧三参数方法只产生PREVIEW或SHARE，绝不能推断EDITOR。PREVIEW最长1小时、SHARE最长7天、EDITOR最长12小时；空ttl默认分别1小时、1小时、12小时。EDITOR仅用于Draft模式。
- PreviewTarget保留用途与可空issuedBy。EDITOR必须绑定真实用户ID，签发和每次解析都确认账户有效、当前站点成员资格及内容类型edit权限/语言范围；停用、撤销角色或语言范围后不再有效。PREVIEW/SHARE仍按签名/签发行、站点、到期和撤销规则验证，不因签发人角色调整自动变为编辑器令牌。
- PreviewRequest显式携带purpose。旧三参数构造仅表示PREVIEW，不能携带覆盖；只有经Delivery验证的EDITOR/Draft可以提交partOverrides。V1覆盖仅允许声明的layout，须经对应Schema/贡献方校验，不能修改content/card等保留元数据；AUTO目标语言只能只读。临时组装不写草稿/修订/公共缓存、不发布领域事件。
- Permissions.forContent先检查内容类型格式，再将大写类型转为小写并把下划线改为连字符，与现有module.json一致；不接受两种权限别名。真实权限仍由core与描述符管理。
- 后续HTTP入口必须先按可信Host建立站点，再验证令牌用途和签发人；未通过不能进入Nuxt画布。此变更是M0候选契约修正，不代表iframe链路已交付。测试替身使用不透明随机令牌与显式夹具，不冒充真实HMAC/签发表提供方。

## 备选方案与影响

不采用根据TTL、草稿模式、请求参数或前端消息判断EDITOR。增加DTO字段与一个枚举，不增加模块、数据表或服务接口，不改已生成迁移。现有cnt_preview_token已有purpose及created_by；真实提供方应核验签名载荷与持久行一致。当前契约尚未冻结，生成代码、调用方、替身、共享反例与设计文档同批更新；正式采纳仍需人工架构评审。

旧数据或令牌缺少purpose/issuedBy时不得默认解释为EDITOR；编辑器令牌须重新签发并按新规则验证。当前工程尚无生产内容令牌提供方，不借此修改既有DDL。
