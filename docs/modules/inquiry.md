# 询盘（inquiry）模块设计

| 项 | 内容 |
|---|---|
| 模块 ID | `inquiry` |
| 名称 | 询盘 |
| 层 | L3 |
| 表前缀 | `inq_` |
| API 前缀 | `inquiry` |
| 可停用 | 是 |
| 模块版本 | 1.0.0（module.json 的 version） |
| 平台版本 | `>=1.0.0 <2.0.0`（module.json 的 platform） |
| 里程碑切片 | M1：<…>；M2：<…>；M3：<…>；M4：<…>（详见第 13 节） |
| 模块负责人 | 待定 |
| 文档版本 | V1.0 |
| 状态 | 草案 |
| 更新日期 | 2026-10-14 |
| 依据 | 系统设计 V1.4 第 <…> 章；平台框架设计 V1.<x> 第 4 章 <…> 节；ADR-<…> |

> 本文只引用《平台框架设计》第 4 章中的契约，不定义新的跨模块契约；需要新契约时按平台框架设计 2.3 提出契约变更请求。

## 1. 职责与边界

### 1.1 职责

> 一段概述 + 要点列表；与系统设计 3.2 的职责描述一致，并细化到本模块的功能清单。

### 1.2 不负责的事

| 事项 | 归属模块 | 说明 |
|---|---|---|

### 1.3 协作概览

> 用图或表说明本模块与哪些模块协作、通过哪种方式（服务接口 / 事件 / 扩展点）。只画契约，不画实现。

### 1.4 适用的架构约束

> 列出与本模块直接相关的原则（P1–P8）、ADR 规则与 CLAUDE.md 禁止项，以及本模块如何满足。

## 2. 依赖与契约

### 2.1 module.json

```json
{
  "id": "<id>",
  "name": "<中文名称>",
  "version": "1.0.0",
  "layer": "L3",
  "platform": ">=1.0.0 <2.0.0",
  "requires": {},
  "optional": {},
  "disableable": true,
  "tablePrefix": "<前缀>_",
  "apiPrefix": "<id>",
  "permissions": [],
  "menus": [],
  "settings": null,
  "components": [],
  "events": { "publishes": [], "subscribes": [] }
}
```

> 完整描述符，须通过平台框架设计附录 A 的 Schema 与 A.3 的校验规则。

### 2.2 使用的服务接口

| 接口（平台框架设计小节） | 方法 | 用途 | 调用位置与时机（事务内 / 外） | 测试替身 |
|---|---|---|---|---|

### 2.3 提供的服务接口

| 接口（平台框架设计小节） | 方法 | 实现类 | 契约测试 | 说明（幂等、权限、性能目标） |
|---|---|---|---|---|

### 2.4 实现的扩展点

| 扩展点（平台框架设计小节） | 实现类 | 适用范围（内容类型 / 资源类型） | order | 收集方 |
|---|---|---|---|---|

### 2.5 收集的扩展点

| 扩展点 | 调用时机 | 过滤（站点启用状态、内容类型等） | 实现缺失或抛出异常时的行为 | 测试用示例实现 |
|---|---|---|---|---|

### 2.6 发布与订阅的事件

发布：

| 事件 | 触发时机（所在事务） | 关键字段 | 主要订阅方 |
|---|---|---|---|

订阅：

| 事件 | 处理 | 幂等方式 | 过期判断 | 失败与重试 |
|---|---|---|---|---|

### 2.7 使用的端口

| 端口 | 用途 | 测试替身 |
|---|---|---|

## 3. 领域模型

### 3.1 聚合、实体与值对象

| 类型 | 名称 | 说明 |
|---|---|---|
| 聚合根 | Inquiry（`inq_inquiry`） | 线索：编号、类型、状态、优先级、负责人、联系人、留言与中文译文、语言、来源归因、隐私字段、SLA 时间、去重关联 |
| 实体 | InquiryItem（`inq_inquiry_item`） | 询盘明细：内容、型号、数量，以及标题、型号、URL 的快照（产品之后可能改名或下线） |
| 实体 | Activity（`inq_activity`） | 时间线：备注、状态变更、分配、邮件跟进、电话 |
| 实体 | Attachment（`inq_attachment`） | 员工在后台上传的附件（媒体中 `owner_module = 'inquiry'` 的私有文件）；访客提交的附件仍属于 form |
| 聚合根 | AssignmentRule（`inq_assignment_rule`） | 分配规则：优先级、条件、负责人（指定用户或销售组轮询） |
| 聚合根 | SalesGroup（`inq_sales_group`） | 销售组；成员（`inq_sales_group_member`）含是否参与轮询、是否组长、最近分配时间 |
| 实体 | NoSequence（`inq_no_sequence`） | 询盘编号的按站点按日序号 |
| 聚合根 | WorkCalendar（`inq_work_calendar`） | SLA 工作日历：工作时区、工作日、工作时间、SLA 工作小时数；特殊日期（`inq_calendar_day`）：节假日与调休补班日 |
| 值对象 | Contact | `name`、`email`（+ `email_normalized`）、`phone`、`whatsapp`、`company`、`job_title`、`country`、`website` |
| 值对象 | Attribution | `landing_url`、`referrer`、`utm_*`、`gclid` / `msclkid` / `fbclid`、`first_touch`、`last_touch`、`viewed_content_ids`、`ip_country`、`device`（系统设计 17.9） |
| 值对象 | PrivacyRecord | `privacy_notice_version`、`marketing_consent`、`marketing_consent_at`、`gpc`、`opt_out_sale_share`（字段名与 `frm_submission` 一致） |
| 值对象 | RuleConditions | 国家或地区、询盘类型、产品分类、语言、来源（utm_source） |

在系统设计之外补充的字段：

| 补充 | 理由 |
|---|---|
| `inq_inquiry.no` 改名 `inquiry_no` | `NO` 是 MySQL 关键字（本文 2.2） |
| `inq_inquiry.email_normalized` | 同一邮箱 7 天内去重、数据主体请求按邮箱跨站点查找（本文 2.11） |
| `inq_inquiry.form_id`、`download_media_id` | 告知文本追溯（与 `locale`、`privacy_notice_version` 一起指向 `frm_form_notice_version`）；DOWNLOAD 线索显示请求的文档 |
| `inq_inquiry.spam_reviewed_by`、`spam_reviewed_at` | QUARANTINE 询盘经人工复核为有效后才分配与通知，记录复核人与时间 |
| `inq_inquiry.priority` 取值、`close_note` | 系统设计未给出优先级取值，本文定为 LOW / NORMAL / HIGH；关闭时的补充说明 |
| `inq_inquiry.sla_due_at`、`sla_reminded_at`、`first_response_minutes` | SLA 提醒任务按到期时间扫描且只提醒一次；报表“首次响应时间（按工作时间）”不必实时计算 |
| `inq_inquiry.marketing_consent_withdrawn_at` | 数据主体“反对”时撤销营销同意（系统设计 17.11），保留原同意时间并记录撤销时间 |
| `inq_inquiry.anonymized_at` | 保留期满或删除请求后匿名化，统计字段保留 |
| `inq_sales_group_member.is_leader`、`sort_order`、`last_assigned_at` | SLA 超时提醒“负责人与主管”：主管为负责人所在销售组的组长；轮询选择最久未分配的在岗成员 |
| `inq_assignment_rule.assignee_group_id`（生成列） | `assignee_ref` 按 `assignee_type` 指向用户或销售组；生成列只在组轮询时取值，用于外键 |
| `inq_activity.detail` | 状态与分配变更的结构化信息（原值、新值、命中规则），供时间线与审计使用 |
| `inq_no_sequence` | 编号 `INQ-20261006-0001` 需要按站点按日连续的序号（系统设计 17.5） |
| `inq_work_calendar`、`inq_calendar_day` | 系统设计 17.7 的“工作时区、工作日与节假日（含调休补班日）”；SLA 工作小时数（默认 8）与日历同表 |

站点级参数放在 `sys_setting`（第 4 章）：`inquiry.assignment`（默认负责人、负责人不在任何销售组时的 SLA 升级对象）、`inquiry.message_translation`（留言翻译开关，系统设计 17.10）、`inquiry.retention`（已关闭询盘的保留期，默认 3 年）。

### 3.2 不变量

| # | 规则 | 保证方式 |
|---|---|---|
| 1 | 每个表单提交最多创建一个询盘（事件重复投递时幂等）；用途为 `CUSTOM` 的表单与判定为 `REJECT` 的提交不创建询盘 | 数据库：`uk_inq_submission`；应用层 |
| 2 | 站点内 `inquiry_no` 唯一；格式 `INQ-{yyyyMMdd}-{序号}`，日期取站点工作时区的当天，序号按日从 1 递增，至少 4 位 | 数据库：`uk_inq_no`、`inq_no_sequence` 主键；应用层 |
| 3 | 状态只能按状态机迁移：NEW → ASSIGNED → CONTACTED → QUALIFIED → CONVERTED（V2 预留）；CLOSED 以外的状态都可以 → CLOSED；CLOSED → ASSIGNED（重新打开） | 应用层 |
| 4 | NEW 没有负责人；ASSIGNED / CONTACTED / QUALIFIED / CONVERTED 必须有负责人与分配时间 | 数据库：`ck_inq_inquiry_owner` |
| 5 | CLOSED ⇔ `closed_at`、`close_reason` 非空；重新打开时清空 | 数据库：`ck_inq_inquiry_closed` |
| 6 | QUARANTINE 询盘保持 NEW：不分配、不通知负责人、不发客户自动回复，直到人工复核为 ACCEPT（或以 SPAM 关闭） | 应用层 |
| 7 | 营销同意：勾选时 `marketing_consent_at` 非空；未勾选时为空；撤销后 `marketing_consent = 0` 且保留原同意时间与撤销时间 | 数据库：`ck_inq_inquiry_consent` |
| 8 | `message_translated` 与 `message_translated_at` 同时为空或同时非空；译文不写入翻译记忆；留言更正或匿名化时同时清空译文 | 数据库：`ck_inq_inquiry_translated`；应用层 |
| 9 | 去重：同站点同一 `email_normalized` 在 7 天内已有询盘（未以 SPAM 关闭）时，新询盘的 `duplicate_of_id` 指向最早一条（根询盘，不形成链），并分配给根询盘的负责人；不能指向自身 | 应用层（MySQL 不允许 CHECK 引用带 SET NULL 动作的外键列） |
| 10 | 联系字段、归因与隐私字段从 `frm_submission` 复制，之后只能经数据主体请求更正或匿名化 | 应用层 |
| 11 | 匿名化后联系字段、留言与译文、点击 ID、触达记录为空，`anonymized_at` 非空；类型、国家、来源、明细、时间等统计字段保留 | 应用层 |
| 12 | 规则 `assignee_type = USER` 时 `assignee_ref` 为用户 ID；`GROUP_ROUND_ROBIN` 时为存在且未删除的销售组 ID | 数据库：`fk_inq_rule_group`（存在性）；应用层 |
| 13 | 每个销售组每个用户一条成员记录；轮询只选择 `active = 1` 且账号未停用的成员 | 数据库：主键；应用层 |
| 14 | 每个站点一个工作日历；工作开始早于结束，午休在工作时间内；每个日期最多一条特殊日期 | 数据库：`uk_cal_site`、`ck_inq_cal_hours`、`uk_cday_date` |
| 15 | `sla_due_at` = `assigned_at` + SLA 工作小时（按工作日历）；改派、重新打开时重算并清空 `sla_reminded_at`；进入 CONTACTED 后不再提醒 | 应用层 |
| 16 | 数据范围：OWN 只看 `owner_user_id` = 本人；GROUP 看负责人与本人同属某个销售组的询盘；ALL 看站点全部；未分配的询盘只对 ALL 可见 | 应用层（查询条件） |
| 17 | 子表的 `site_id` 与询盘或父表一致 | 应用层 |

### 3.3 状态机

<<FILL: 状态与转换；没有状态机时写“不适用：<原因>”>>

## 4. 数据模型

> 遵守平台框架设计第 7 章的数据库通用约定。DDL 已在 MySQL 8.4 上执行通过；表结构以本节为准，与迁移脚本保持一致。

### 4.1 表清单

| 表 | 说明 | 站点级 | 软删除 | 预估规模（单站，按系统设计假设 A4） |
|---|---|---|---|---|
| `inq_sales_group` | 销售组 | 是 | 是（`deleted_at`） | 1–20 行 |
| `inq_sales_group_member` | 销售组成员 | 是 | 否 | 10–100 行 |
| `inq_assignment_rule` | 分配规则 | 是 | 是（`deleted_at`） | 10–50 行 |
| `inq_inquiry` | 询盘 | 是 | 否（保留期满匿名化；垃圾询盘 30 天后物理删除） | ≤ 110 万行（≤ 1 千条 / 天 × 3 年） |
| `inq_inquiry_item` | 询盘明细 | 是 | 否（随询盘） | ≤ 200 万行（平均 1–2 条） |
| `inq_activity` | 跟进活动 | 是 | 否（随询盘） | ≤ 550 万行（平均 5 条） |
| `inq_attachment` | 员工上传的询盘附件 | 是 | 否（随询盘） | < 1 万行 / 年 |
| `inq_no_sequence` | 编号按日序号 | 是 | 否 | 365 行 / 年 |
| `inq_work_calendar` | SLA 工作日历 | 是 | 否 | 1 行 |
| `inq_calendar_day` | 节假日与调休补班日 | 是 | 否 | 20–40 行 / 年 |

### 4.2 ER 图

```mermaid
erDiagram
    inq_inquiry ||--o{ inq_inquiry_item : "contains"
    inq_inquiry ||--o{ inq_activity : "has"
    inq_inquiry ||--o{ inq_attachment : "has"
    inq_inquiry |o--o{ inq_inquiry : "is duplicated by"
    inq_sales_group ||--o{ inq_sales_group_member : "has"
    inq_sales_group |o--o{ inq_assignment_rule : "is assignee of"
    inq_work_calendar ||--o{ inq_calendar_day : "has"
```

`inq_no_sequence` 与其他表没有关系，未画出。

### 4.3 表结构

#### 4.3.1 `inq_sales_group`：销售组

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `name` | VARCHAR(100) | 否 | — | 组名（中文，如“欧洲销售组”） |
| `description` | VARCHAR(500) | 是 | — | 说明（负责的市场或产品线） |
| `deleted_at` | DATETIME(3) | 是 | — | 删除时间（软删除） |
| `alive` | TINYINT | 是 | 生成列 `IF(deleted_at IS NULL, 1, NULL)` | 未删除标记（唯一索引用） |
| `version` | INT | 否 | `0` | 乐观锁版本号 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_sg_name (site_id, name, alive)`：未删除的组名唯一；站点的销售组列表

```sql
CREATE TABLE inq_sales_group (
  id           BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id      BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  name         VARCHAR(100)  NOT NULL COMMENT '组名（中文，如“欧洲销售组”）',
  description  VARCHAR(500)  NULL COMMENT '说明（负责的市场或产品线）',
  deleted_at   DATETIME(3)   NULL COMMENT '删除时间（软删除）',
  alive        TINYINT       AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  version      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sg_name (site_id, name, alive)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售组（分配规则轮询对象；数据范围 GROUP 的依据）';
```

要点：被未删除的分配规则引用的销售组不能删除（应用层，先改规则）；软删除时保留成员记录，供历史数据范围判断。

#### 4.3.2 `inq_sales_group_member`：销售组成员

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `group_id` | BIGINT | 否 | — | 销售组 ID（inq_sales_group.id） |
| `user_id` | BIGINT | 否 | — | 用户 ID（sys_user.id） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `active` | TINYINT(1) | 否 | `1` | 是否参与轮询（休假、暂停接单时置 0；仍属于该组，数据范围不变） |
| `is_leader` | TINYINT(1) | 否 | `0` | 是否组长（SLA 超时提醒的主管） |
| `sort_order` | INT | 否 | `0` | 轮询次序（最近分配时间相同时按此排序） |
| `last_assigned_at` | DATETIME(3) | 是 | — | 最近一次经轮询分配到询盘的时间；轮询选择最早者，空值优先 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 加入时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (group_id, user_id)`：组成员列表；轮询时按组读取在岗成员
- `idx_sgm_user (site_id, user_id)`：数据范围 GROUP——查找本人所在的组及同组成员；SLA 提醒查找负责人所在组的组长
- `fk_inq_sgm_group`：组合关系，`ON DELETE CASCADE`

```sql
CREATE TABLE inq_sales_group_member (
  group_id          BIGINT       NOT NULL COMMENT '销售组 ID（inq_sales_group.id）',
  user_id           BIGINT       NOT NULL COMMENT '用户 ID（sys_user.id）',
  site_id           BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  active            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否参与轮询（休假、暂停接单时置 0；仍属于该组，数据范围不变）',
  is_leader         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否组长（SLA 超时提醒的主管）',
  sort_order        INT          NOT NULL DEFAULT 0 COMMENT '轮询次序（最近分配时间相同时按此排序）',
  last_assigned_at  DATETIME(3)  NULL COMMENT '最近一次经轮询分配到询盘的时间；轮询选择最早者，空值优先',
  created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间（UTC）',
  created_by        BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by        BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (group_id, user_id),
  KEY idx_sgm_user (site_id, user_id),
  CONSTRAINT fk_inq_sgm_group FOREIGN KEY (group_id) REFERENCES inq_sales_group (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售组成员（是否参与轮询、是否组长、最近分配时间）';
```

要点：轮询在事务中先锁定销售组行（`SELECT … FOR UPDATE`），选出 `active = 1`、账号未停用、`last_assigned_at` 最早的成员并更新其 `last_assigned_at`，保证并发分配时依次轮换；一个用户可以属于多个组。

#### 4.3.3 `inq_assignment_rule`：分配规则

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `name` | VARCHAR(100) | 否 | — | 规则名称（中文） |
| `priority` | INT | 否 | `100` | 优先级：数值越小越先匹配；相同时按 id |
| `conditions` | JSON | 否 | `(JSON_OBJECT())` | 匹配条件（条件之间为“且”，同一条件的多个取值为“或”，未设置的条件不限制）：{"countries":["DE","AT"],"types":["INQUIRY"],"categoryTermIds":["…"],"locales":["de"],"utmSources":["google"]} |
| `assignee_type` | VARCHAR(24) | 否 | — | 负责人类型：USER（指定用户）/ GROUP_ROUND_ROBIN（销售组内轮询） |
| `assignee_ref` | BIGINT | 否 | — | 负责人引用：USER 为用户 ID（sys_user.id）；GROUP_ROUND_ROBIN 为销售组 ID（inq_sales_group.id） |
| `assignee_group_id` | BIGINT | 是 | 生成列 `IF(assignee_type = 'GROUP_ROUND_ROBIN', assignee_ref, NULL)` | 销售组 ID（生成列：组轮询时等于 assignee_ref，用于外键） |
| `enabled` | TINYINT(1) | 否 | `1` | 是否启用 |
| `deleted_at` | DATETIME(3) | 是 | — | 删除时间（软删除） |
| `alive` | TINYINT | 是 | 生成列 `IF(deleted_at IS NULL, 1, NULL)` | 未删除标记（唯一索引用） |
| `version` | INT | 否 | `0` | 乐观锁版本号 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_rule_name (site_id, name, alive)`：未删除的规则名称唯一
- `idx_rule_match (site_id, enabled, priority)`：分配时按优先级读取启用的规则
- `idx_rule_group (assignee_group_id)`：`fk_inq_rule_group` 的索引；删除销售组前检查引用
- `fk_inq_rule_group`：引用关系（RESTRICT）
- `ck_inq_rule_assignee`：负责人类型取值

```sql
CREATE TABLE inq_assignment_rule (
  id                 BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id            BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  name               VARCHAR(100)  NOT NULL COMMENT '规则名称（中文）',
  priority           INT           NOT NULL DEFAULT 100 COMMENT '优先级：数值越小越先匹配；相同时按 id',
  conditions         JSON          NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '匹配条件（条件之间为“且”，同一条件的多个取值为“或”，未设置的条件不限制）：{"countries":["DE","AT"],"types":["INQUIRY"],"categoryTermIds":["…"],"locales":["de"],"utmSources":["google"]}',
  assignee_type      VARCHAR(24)   NOT NULL COMMENT '负责人类型：USER（指定用户）/ GROUP_ROUND_ROBIN（销售组内轮询）',
  assignee_ref       BIGINT        NOT NULL COMMENT '负责人引用：USER 为用户 ID（sys_user.id）；GROUP_ROUND_ROBIN 为销售组 ID（inq_sales_group.id）',
  assignee_group_id  BIGINT        AS (IF(assignee_type = 'GROUP_ROUND_ROBIN', assignee_ref, NULL)) STORED COMMENT '销售组 ID（生成列：组轮询时等于 assignee_ref，用于外键）',
  enabled            TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用',
  deleted_at         DATETIME(3)   NULL COMMENT '删除时间（软删除）',
  alive              TINYINT       AS (IF(deleted_at IS NULL, 1, NULL)) STORED COMMENT '未删除标记（唯一索引用）',
  version            INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_name (site_id, name, alive),
  KEY idx_rule_match (site_id, enabled, priority),
  KEY idx_rule_group (assignee_group_id),
  CONSTRAINT fk_inq_rule_group FOREIGN KEY (assignee_group_id) REFERENCES inq_sales_group (id),
  CONSTRAINT ck_inq_rule_assignee CHECK (assignee_type IN ('USER', 'GROUP_ROUND_ROBIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘分配规则（按优先级匹配；负责人为指定用户或销售组轮询）';
```

要点：
- 分配（系统设计 17.7）：询盘为 ACCEPT（或复核为 ACCEPT）且不是重复询盘时，按 `priority` 依次匹配启用的规则；产品分类条件经 `TaxonomyApi` 查询明细中内容的分类（含祖先分类）；都不匹配时使用 `inquiry.assignment` 中的默认负责人。重复询盘直接分配给根询盘的负责人。命中的规则 ID 记入 `inq_activity.detail`。
- 规则指向的用户停用或销售组没有可用成员时，跳过该规则继续匹配，并在后台提示。

#### 4.3.4 `inq_inquiry`：询盘

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `inquiry_no` | VARCHAR(32) bin | 否 | — | 询盘编号，如 INQ-20261006-0001（日期取站点工作时区） |
| `type` | VARCHAR(16) | 否 | — | 询盘类型：INQUIRY / SAMPLE / DOWNLOAD / CONTACT（取自表单用途） |
| `status` | VARCHAR(16) | 否 | `'NEW'` | 状态：NEW / ASSIGNED / CONTACTED / QUALIFIED / CONVERTED（V2 预留）/ CLOSED |
| `priority` | VARCHAR(8) | 否 | `'NORMAL'` | 优先级：LOW / NORMAL / HIGH |
| `owner_user_id` | BIGINT | 是 | — | 负责人用户 ID（sys_user.id）；未分配为空 |
| `assigned_at` | DATETIME(3) | 是 | — | 最近一次分配时间（改派、重新打开时更新） |
| `first_contacted_at` | DATETIME(3) | 是 | — | 首次联系时间（首次进入 CONTACTED） |
| `first_response_minutes` | INT | 是 | — | 首次响应时长：创建到首次联系的工作分钟数（按工作日历计算，报表用） |
| `sla_due_at` | DATETIME(3) | 是 | — | SLA 到期时间：分配时间 + SLA 工作小时（按工作日历换算为 UTC） |
| `sla_reminded_at` | DATETIME(3) | 是 | — | 已发送 SLA 超时提醒的时间（每次分配只提醒一次） |
| `closed_at` | DATETIME(3) | 是 | — | 关闭时间 |
| `close_reason` | VARCHAR(32) | 是 | — | 关闭原因（字典 inquiry.close_reason）：INVALID / SPAM / DUPLICATE / NO_RESPONSE / OTHER |
| `close_note` | VARCHAR(500) | 是 | — | 关闭说明 |
| `name` | VARCHAR(200) | 是 | — | 联系人姓名 |
| `email` | VARCHAR(254) | 是 | — | 邮箱（原样） |
| `email_normalized` | VARCHAR(254) | 是 | — | 邮箱的小写规范化值（7 天去重、数据主体请求查找） |
| `phone` | VARCHAR(32) | 是 | — | 电话（E.164） |
| `whatsapp` | VARCHAR(32) | 是 | — | WhatsApp 号码（E.164） |
| `company` | VARCHAR(200) | 是 | — | 公司 |
| `job_title` | VARCHAR(200) | 是 | — | 职位 |
| `country` | CHAR(2) | 是 | — | 国家（访客填写，ISO 3166-1 alpha-2） |
| `website` | VARCHAR(2048) | 是 | — | 公司网站 |
| `message` | TEXT | 是 | — | 留言（纯文本，原语言；不作为指令处理） |
| `quantity_text` | VARCHAR(200) | 是 | — | 数量（访客填写的文本） |
| `locale` | VARCHAR(16) | 否 | — | 客户提交时的语言（提交表单所在页面的语言；自动回复使用该语言） |
| `message_translated` | TEXT | 是 | — | 留言的中文译文（经 TranslationApi.translateText 按需翻译；不写入翻译记忆） |
| `message_translated_at` | DATETIME(3) | 是 | — | 翻译时间 |
| `form_id` | BIGINT | 否 | — | 表单 ID（frm_form.id） |
| `submission_id` | BIGINT | 否 | — | 表单提交 ID（frm_submission.id；站点内唯一，事件幂等） |
| `download_media_id` | BIGINT | 是 | — | DOWNLOAD 线索请求的文档（mda_media.id） |
| `duplicate_of_id` | BIGINT | 是 | — | 重复询盘指向的根询盘 ID（inq_inquiry.id；同一邮箱 7 天内） |
| `spam_verdict` | VARCHAR(16) | 否 | — | 垃圾判定：ACCEPT / QUARANTINE（REJECT 的提交不创建询盘）；QUARANTINE 复核为有效后改为 ACCEPT |
| `spam_reviewed_by` | BIGINT | 是 | — | 复核 QUARANTINE 询盘的用户 ID |
| `spam_reviewed_at` | DATETIME(3) | 是 | — | 复核时间 |
| `is_test` | TINYINT(1) | 否 | `0` | 是否测试询盘（合成监控；不计入报表，通知发到运维邮箱） |
| `landing_url` | VARCHAR(2048) | 是 | — | 落地页 URL |
| `referrer` | VARCHAR(2048) | 是 | — | 来源页 URL（referrer） |
| `utm_source` | VARCHAR(200) | 是 | — | UTM 来源 |
| `utm_medium` | VARCHAR(200) | 是 | — | UTM 媒介 |
| `utm_campaign` | VARCHAR(200) | 是 | — | UTM 活动 |
| `utm_term` | VARCHAR(200) | 是 | — | UTM 关键词 |
| `utm_content` | VARCHAR(200) | 是 | — | UTM 内容 |
| `gclid` | VARCHAR(255) | 是 | — | Google Ads 点击 ID（营销类，按同意保存） |
| `msclkid` | VARCHAR(255) | 是 | — | Microsoft Ads 点击 ID（营销类，按同意保存） |
| `fbclid` | VARCHAR(255) | 是 | — | Meta 点击 ID（营销类，按同意保存） |
| `first_touch` | JSON | 是 | — | 首次触达：{landingUrl, referrer, utm, clickIds, at} |
| `last_touch` | JSON | 是 | — | 末次触达：{landingUrl, referrer, utm, clickIds, at} |
| `viewed_content_ids` | JSON | 否 | `(JSON_ARRAY())` | 会话内最近浏览的内容 ID 数组（最多 10 个，字符串） |
| `ip_country` | CHAR(2) | 是 | — | IP 所在国家（本地 GeoIP 解析，ISO 3166-1 alpha-2） |
| `device` | VARCHAR(16) | 是 | — | 设备类型：DESKTOP / TABLET / MOBILE |
| `privacy_notice_version` | INT | 是 | — | 提交时展示的告知版本（与 form_id、locale 一起指向 frm_form_notice_version）；表单没有告知文本时为空 |
| `marketing_consent` | TINYINT(1) | 否 | `0` | 是否同意营销（默认不勾选；撤销后为 0） |
| `marketing_consent_at` | DATETIME(3) | 是 | — | 营销同意时间（勾选时为服务器接收时间；撤销后保留） |
| `marketing_consent_withdrawn_at` | DATETIME(3) | 是 | — | 营销同意撤销时间（数据主体反对） |
| `gpc` | TINYINT(1) | 否 | `0` | 提交时浏览器是否发出 Global Privacy Control 信号 |
| `opt_out_sale_share` | TINYINT(1) | 否 | `0` | 是否已退出“出售 / 共享”（含 GPC 与数据主体请求） |
| `anonymized_at` | DATETIME(3) | 是 | — | 匿名化时间（保留期满或数据主体删除请求） |
| `version` | INT | 否 | `0` | 乐观锁版本号 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC；收到 FormSubmittedEvent 后创建） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_inq_no (site_id, inquiry_no)`：编号唯一；按编号查找（邮件中的编号、后台搜索）
- `uk_inq_submission (submission_id)`：每个提交只创建一个询盘（事件幂等）
- `idx_inq_list (site_id, status, created_at)`：后台列表按状态筛选、按时间倒序；SLA 任务扫描 ASSIGNED 询盘；保留期任务按状态与时间查找已关闭询盘
- `idx_inq_owner (site_id, owner_user_id, status, created_at)`：“我的询盘”与数据范围 OWN / GROUP；负责人停用时查找未关闭询盘
- `idx_inq_email (email_normalized, site_id, created_at)`：7 天去重；数据主体请求按邮箱跨站点查找（系统管理接口）
- `idx_inq_dup (duplicate_of_id)`：详情页合并时间线；兼作 `fk_inq_inquiry_dup` 的索引
- `fk_inq_inquiry_dup`：引用关系，`ON DELETE SET NULL`（垃圾询盘物理删除时解除关联）
- `ck_inq_inquiry_owner`、`ck_inq_inquiry_closed`、`ck_inq_inquiry_consent`、`ck_inq_inquiry_translated`：见 3.2 不变量 4、5、7、8

```sql
CREATE TABLE inq_inquiry (
  id                              BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id                         BIGINT         NOT NULL COMMENT '站点 ID（sys_site.id）',
  inquiry_no                      VARCHAR(32)    COLLATE utf8mb4_0900_bin NOT NULL COMMENT '询盘编号，如 INQ-20261006-0001（日期取站点工作时区）',
  type                            VARCHAR(16)    NOT NULL COMMENT '询盘类型：INQUIRY / SAMPLE / DOWNLOAD / CONTACT（取自表单用途）',
  status                          VARCHAR(16)    NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW / ASSIGNED / CONTACTED / QUALIFIED / CONVERTED（V2 预留）/ CLOSED',
  priority                        VARCHAR(8)     NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW / NORMAL / HIGH',
  owner_user_id                   BIGINT         NULL COMMENT '负责人用户 ID（sys_user.id）；未分配为空',
  assigned_at                     DATETIME(3)    NULL COMMENT '最近一次分配时间（改派、重新打开时更新）',
  first_contacted_at              DATETIME(3)    NULL COMMENT '首次联系时间（首次进入 CONTACTED）',
  first_response_minutes          INT            NULL COMMENT '首次响应时长：创建到首次联系的工作分钟数（按工作日历计算，报表用）',
  sla_due_at                      DATETIME(3)    NULL COMMENT 'SLA 到期时间：分配时间 + SLA 工作小时（按工作日历换算为 UTC）',
  sla_reminded_at                 DATETIME(3)    NULL COMMENT '已发送 SLA 超时提醒的时间（每次分配只提醒一次）',
  closed_at                       DATETIME(3)    NULL COMMENT '关闭时间',
  close_reason                    VARCHAR(32)    NULL COMMENT '关闭原因（字典 inquiry.close_reason）：INVALID / SPAM / DUPLICATE / NO_RESPONSE / OTHER',
  close_note                      VARCHAR(500)   NULL COMMENT '关闭说明',
  name                            VARCHAR(200)   NULL COMMENT '联系人姓名',
  email                           VARCHAR(254)   NULL COMMENT '邮箱（原样）',
  email_normalized                VARCHAR(254)   NULL COMMENT '邮箱的小写规范化值（7 天去重、数据主体请求查找）',
  phone                           VARCHAR(32)    NULL COMMENT '电话（E.164）',
  whatsapp                        VARCHAR(32)    NULL COMMENT 'WhatsApp 号码（E.164）',
  company                         VARCHAR(200)   NULL COMMENT '公司',
  job_title                       VARCHAR(200)   NULL COMMENT '职位',
  country                         CHAR(2)        NULL COMMENT '国家（访客填写，ISO 3166-1 alpha-2）',
  website                         VARCHAR(2048)  NULL COMMENT '公司网站',
  message                         TEXT           NULL COMMENT '留言（纯文本，原语言；不作为指令处理）',
  quantity_text                   VARCHAR(200)   NULL COMMENT '数量（访客填写的文本）',
  locale                          VARCHAR(16)    NOT NULL COMMENT '客户提交时的语言（提交表单所在页面的语言；自动回复使用该语言）',
  message_translated              TEXT           NULL COMMENT '留言的中文译文（经 TranslationApi.translateText 按需翻译；不写入翻译记忆）',
  message_translated_at           DATETIME(3)    NULL COMMENT '翻译时间',
  form_id                         BIGINT         NOT NULL COMMENT '表单 ID（frm_form.id）',
  submission_id                   BIGINT         NOT NULL COMMENT '表单提交 ID（frm_submission.id；事件幂等）',
  download_media_id               BIGINT         NULL COMMENT 'DOWNLOAD 线索请求的文档（mda_media.id）',
  duplicate_of_id                 BIGINT         NULL COMMENT '重复询盘指向的根询盘 ID（inq_inquiry.id；同一邮箱 7 天内）',
  spam_verdict                    VARCHAR(16)    NOT NULL COMMENT '垃圾判定：ACCEPT / QUARANTINE（REJECT 的提交不创建询盘）；QUARANTINE 复核为有效后改为 ACCEPT',
  spam_reviewed_by                BIGINT         NULL COMMENT '复核 QUARANTINE 询盘的用户 ID',
  spam_reviewed_at                DATETIME(3)    NULL COMMENT '复核时间',
  is_test                         TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否测试询盘（合成监控；不计入报表，通知发到运维邮箱）',
  landing_url                     VARCHAR(2048)  NULL COMMENT '落地页 URL',
  referrer                        VARCHAR(2048)  NULL COMMENT '来源页 URL（referrer）',
  utm_source                      VARCHAR(200)   NULL COMMENT 'UTM 来源',
  utm_medium                      VARCHAR(200)   NULL COMMENT 'UTM 媒介',
  utm_campaign                    VARCHAR(200)   NULL COMMENT 'UTM 活动',
  utm_term                        VARCHAR(200)   NULL COMMENT 'UTM 关键词',
  utm_content                     VARCHAR(200)   NULL COMMENT 'UTM 内容',
  gclid                           VARCHAR(255)   NULL COMMENT 'Google Ads 点击 ID（营销类，按同意保存）',
  msclkid                         VARCHAR(255)   NULL COMMENT 'Microsoft Ads 点击 ID（营销类，按同意保存）',
  fbclid                          VARCHAR(255)   NULL COMMENT 'Meta 点击 ID（营销类，按同意保存）',
  first_touch                     JSON           NULL COMMENT '首次触达：{landingUrl, referrer, utm, clickIds, at}',
  last_touch                      JSON           NULL COMMENT '末次触达：{landingUrl, referrer, utm, clickIds, at}',
  viewed_content_ids              JSON           NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '会话内最近浏览的内容 ID 数组（最多 10 个，字符串）',
  ip_country                      CHAR(2)        NULL COMMENT 'IP 所在国家（本地 GeoIP 解析，ISO 3166-1 alpha-2）',
  device                          VARCHAR(16)    NULL COMMENT '设备类型：DESKTOP / TABLET / MOBILE',
  privacy_notice_version          INT            NULL COMMENT '提交时展示的告知版本（与 form_id、locale 一起指向 frm_form_notice_version）；表单没有告知文本时为空',
  marketing_consent               TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否同意营销（默认不勾选；撤销后为 0）',
  marketing_consent_at            DATETIME(3)    NULL COMMENT '营销同意时间（勾选时为服务器接收时间；撤销后保留）',
  marketing_consent_withdrawn_at  DATETIME(3)    NULL COMMENT '营销同意撤销时间（数据主体反对）',
  gpc                             TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '提交时浏览器是否发出 Global Privacy Control 信号',
  opt_out_sale_share              TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已退出“出售 / 共享”（含 GPC 与数据主体请求）',
  anonymized_at                   DATETIME(3)    NULL COMMENT '匿名化时间（保留期满或数据主体删除请求）',
  version                         INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at                      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC；收到 FormSubmittedEvent 后创建）',
  created_by                      BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at                      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by                      BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_inq_no (site_id, inquiry_no),
  UNIQUE KEY uk_inq_submission (submission_id),
  KEY idx_inq_list (site_id, status, created_at),
  KEY idx_inq_owner (site_id, owner_user_id, status, created_at),
  KEY idx_inq_email (email_normalized, site_id, created_at),
  KEY idx_inq_dup (duplicate_of_id),
  CONSTRAINT fk_inq_inquiry_dup FOREIGN KEY (duplicate_of_id) REFERENCES inq_inquiry (id) ON DELETE SET NULL,
  CONSTRAINT ck_inq_inquiry_owner CHECK ((status = 'NEW' AND owner_user_id IS NULL) OR (status IN ('ASSIGNED', 'CONTACTED', 'QUALIFIED', 'CONVERTED') AND owner_user_id IS NOT NULL AND assigned_at IS NOT NULL) OR status = 'CLOSED'),
  CONSTRAINT ck_inq_inquiry_closed CHECK ((status = 'CLOSED' AND closed_at IS NOT NULL AND close_reason IS NOT NULL) OR (status <> 'CLOSED' AND closed_at IS NULL AND close_reason IS NULL)),
  CONSTRAINT ck_inq_inquiry_consent CHECK ((marketing_consent = 1 AND marketing_consent_at IS NOT NULL AND marketing_consent_withdrawn_at IS NULL) OR (marketing_consent = 0 AND marketing_consent_at IS NULL AND marketing_consent_withdrawn_at IS NULL) OR (marketing_consent = 0 AND marketing_consent_at IS NOT NULL AND marketing_consent_withdrawn_at IS NOT NULL)),
  CONSTRAINT ck_inq_inquiry_translated CHECK ((message_translated IS NULL AND message_translated_at IS NULL) OR (message_translated IS NOT NULL AND message_translated_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘（线索：联系人、留言与译文、来源归因、隐私字段、分配与 SLA）';
```

要点：
- 创建（系统设计 17.2）：收到 `FormSubmittedEvent` 后经 `FormApi` 读取提交，按标准字段 key（`name`、`email`、`phone`、`company`、`country`、`job_title`、`message`、`quantity`、`website`）映射联系字段，复制归因与隐私字段（`privacy_notice_version`、`marketing_consent`、`marketing_consent_at`、`gpc`、`opt_out_sale_share`、`ip_country`、`is_test`），产品选择字段生成明细；同一事务中取编号、去重、分配，经 `NotificationApi` 写入负责人通知与客户自动回复（仅 ACCEPT，按 `locale` 选择模板语言），事务提交后发布 `InquiryCreatedEvent`、`InquiryAssignedEvent`。
- 留言翻译（系统设计 17.10）：详情页“翻译为中文”经 `TranslationApi.translateText` 同步翻译，译文与时间写入本表；已有译文时直接显示，不重复调用；功能关闭或 translation 停用时不显示按钮。
- SLA（系统设计 17.7）：分配时按 `inq_work_calendar` 计算 `sla_due_at`；定时任务查找 `status = 'ASSIGNED'`、`sla_due_at` 已到且 `sla_reminded_at` 为空的询盘，经 `NotificationApi` 发送 `inquiry.sla.reminder` 给负责人与其所在组的组长（不在任何组时发给 `inquiry.assignment` 中的升级对象），写入 `sla_reminded_at`。
- 保留期（系统设计 17.11）：以 SPAM 关闭的询盘在关闭 30 天后物理删除（明细、活动、附件级联；附件文件先经 `MediaApi` 删除）；其他已关闭询盘在关闭满保留期（默认 3 年）后匿名化：联系字段、`email_normalized`、留言与译文、点击 ID、`first_touch`、`last_touch`、`landing_url`、`referrer` 置空，活动备注替换为占位文字，附件删除，记录 `anonymized_at`。
- 数据主体请求：inquiry 实现 `PersonalDataProvider`，按 `email_normalized` 跨站点查找、导出（含译文与活动备注）、更正联系字段（同时清空译文）、匿名化（同上）、反对（撤销营销同意）、退出“出售 / 共享”（置 `opt_out_sale_share = 1`）。

#### 4.3.5 `inq_inquiry_item`：询盘明细

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `inquiry_id` | BIGINT | 否 | — | 询盘 ID（inq_inquiry.id） |
| `content_id` | BIGINT | 否 | — | 内容 ID（cnt_content.id，通常为 PRODUCT） |
| `model_id` | BIGINT | 是 | — | 型号 ID（prd_model.id，经 ContentVariantProvider 获取）；未精确到型号或 product 停用时为空 |
| `title_snapshot` | VARCHAR(300) | 否 | — | 提交时的标题快照（询盘语言的发布标题） |
| `model_no_snapshot` | VARCHAR(100) | 是 | — | 提交时的型号快照 |
| `url_snapshot` | VARCHAR(2048) | 是 | — | 提交时的页面 URL 快照（含语言前缀） |
| `quantity` | INT | 是 | — | 数量（询价篮或表单中填写） |
| `sort_order` | INT | 否 | `0` | 显示顺序（询价篮中的顺序） |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC） |

索引与约束：
- `PRIMARY KEY (id)`
- `idx_item_inquiry (inquiry_id, sort_order)`：询盘详情的明细列表；兼作 `fk_inq_item_inquiry` 的索引
- `idx_item_content (site_id, content_id)`：报表按产品统计询盘数；列表按产品筛选
- `fk_inq_item_inquiry`：组合关系，`ON DELETE CASCADE`
- `ck_inq_item_qty`：数量为正数

```sql
CREATE TABLE inq_inquiry_item (
  id                 BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id            BIGINT         NOT NULL COMMENT '站点 ID（sys_site.id）',
  inquiry_id         BIGINT         NOT NULL COMMENT '询盘 ID（inq_inquiry.id）',
  content_id         BIGINT         NOT NULL COMMENT '内容 ID（cnt_content.id，通常为 PRODUCT）',
  model_id           BIGINT         NULL COMMENT '型号 ID（prd_model.id，经 ContentVariantProvider 获取）；未精确到型号或 product 停用时为空',
  title_snapshot     VARCHAR(300)   NOT NULL COMMENT '提交时的标题快照（询盘语言的发布标题）',
  model_no_snapshot  VARCHAR(100)   NULL COMMENT '提交时的型号快照',
  url_snapshot       VARCHAR(2048)  NULL COMMENT '提交时的页面 URL 快照（含语言前缀）',
  quantity           INT            NULL COMMENT '数量（询价篮或表单中填写）',
  sort_order         INT            NOT NULL DEFAULT 0 COMMENT '显示顺序（询价篮中的顺序）',
  created_at         DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  PRIMARY KEY (id),
  KEY idx_item_inquiry (inquiry_id, sort_order),
  KEY idx_item_content (site_id, content_id),
  CONSTRAINT fk_inq_item_inquiry FOREIGN KEY (inquiry_id) REFERENCES inq_inquiry (id) ON DELETE CASCADE,
  CONSTRAINT ck_inq_item_qty CHECK (quantity IS NULL OR quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘明细（产品与型号，含标题、型号、URL 快照）';
```

要点：明细在询盘创建时写入，之后不修改；快照取自内容在询盘语言中的发布投影（`pub_title`、`pub_path`）与 `ContentVariantProvider` 返回的型号号码。后台同时经 `ContentApi` 显示内容的当前中文标题，内容已删除时只显示快照。

#### 4.3.6 `inq_activity`：跟进活动

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `inquiry_id` | BIGINT | 否 | — | 询盘 ID（inq_inquiry.id） |
| `kind` | VARCHAR(16) | 否 | — | 类型：NOTE（备注）/ STATUS（状态变更）/ ASSIGN（分配）/ EMAIL_SENT（邮件跟进）/ CALL（电话） |
| `content` | TEXT | 是 | — | 文本：备注内容、电话摘要、邮件摘要（个人数据，匿名化时替换） |
| `detail` | JSON | 是 | — | 结构化信息：STATUS 为 {from, to, closeReason}；ASSIGN 为 {fromUserId, toUserId, mode: RULE / MANUAL / DEFAULT / DUPLICATE, ruleId} |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 发生时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 操作人用户 ID（0 = 系统，如自动分配） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC；只在匿名化时更新） |

索引与约束：
- `PRIMARY KEY (id)`
- `idx_act_inquiry (inquiry_id, created_at)`：详情页时间线（含重复询盘合并展示：`inquiry_id IN (根询盘, 重复询盘…)`）；兼作 `fk_inq_act_inquiry` 的索引
- `fk_inq_act_inquiry`：组合关系，`ON DELETE CASCADE`
- `ck_inq_act_note`：备注必须有内容

```sql
CREATE TABLE inq_activity (
  id          BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id     BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  inquiry_id  BIGINT        NOT NULL COMMENT '询盘 ID（inq_inquiry.id）',
  kind        VARCHAR(16)   NOT NULL COMMENT '类型：NOTE（备注）/ STATUS（状态变更）/ ASSIGN（分配）/ EMAIL_SENT（邮件跟进）/ CALL（电话）',
  content     TEXT          NULL COMMENT '文本：备注内容、电话摘要、邮件摘要（个人数据，匿名化时替换）',
  detail      JSON          NULL COMMENT '结构化信息：STATUS 为 {from, to, closeReason}；ASSIGN 为 {fromUserId, toUserId, mode: RULE / MANUAL / DEFAULT / DUPLICATE, ruleId}',
  created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间（UTC）',
  created_by  BIGINT        NOT NULL DEFAULT 0 COMMENT '操作人用户 ID（0 = 系统，如自动分配）',
  updated_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC；只在匿名化时更新）',
  PRIMARY KEY (id),
  KEY idx_act_inquiry (inquiry_id, created_at),
  CONSTRAINT fk_inq_act_inquiry FOREIGN KEY (inquiry_id) REFERENCES inq_inquiry (id) ON DELETE CASCADE,
  CONSTRAINT ck_inq_act_note CHECK (kind <> 'NOTE' OR content IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘跟进活动（时间线：备注、状态、分配、邮件、电话；只追加）';
```

要点：只追加，员工不能修改或删除已记录的活动；状态变更与分配由系统在同一事务中写入。“复制邮箱 / mailto”后记录 `EMAIL_SENT`，首次 `EMAIL_SENT` 或 `CALL` 时提示把状态改为 CONTACTED（系统设计 17.10）。

#### 4.3.7 `inq_attachment`：询盘附件

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `inquiry_id` | BIGINT | 否 | — | 询盘 ID（inq_inquiry.id） |
| `media_id` | BIGINT | 否 | — | 媒体 ID（mda_media.id；owner_module = inquiry 的私有文件） |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 上传时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 上传人用户 ID |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_iatt_media (inquiry_id, media_id)`：同一文件在询盘中只出现一次；详情页附件列表；兼作 `fk_inq_att_inquiry` 的索引
- `fk_inq_att_inquiry`：组合关系，`ON DELETE CASCADE`（应用层先经 `MediaApi` 删除文件）

```sql
CREATE TABLE inq_attachment (
  id          BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id     BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  inquiry_id  BIGINT       NOT NULL COMMENT '询盘 ID（inq_inquiry.id）',
  media_id    BIGINT       NOT NULL COMMENT '媒体 ID（mda_media.id；owner_module = inquiry 的私有文件）',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上传时间（UTC）',
  created_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '上传人用户 ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iatt_media (inquiry_id, media_id),
  CONSTRAINT fk_inq_att_inquiry FOREIGN KEY (inquiry_id) REFERENCES inq_inquiry (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘附件（员工在后台上传；文件为媒体模块中 inquiry 管理的私有文件）';
```

要点：员工上传经 `MediaApi` 写入私有桶（`owner_module = 'inquiry'`、`visibility = 'PRIVATE'`），不出现在媒体库；下载走预签名链接并记审计日志，按询盘的数据范围鉴权。访客提交的附件在 `frm_attachment`，详情页经 `FormApi` 按 `submission_id` 读取清单与下载链接，两者合并显示。

#### 4.3.8 `inq_no_sequence`：询盘编号按日序号

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `seq_date` | DATE | 否 | — | 编号日期（站点工作时区的日期） |
| `last_no` | INT | 否 | `0` | 当日已分配的最大序号 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC，当日首个询盘） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |

索引与约束：
- `PRIMARY KEY (site_id, seq_date)`：按站点按日取号

```sql
CREATE TABLE inq_no_sequence (
  site_id     BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  seq_date    DATE         NOT NULL COMMENT '编号日期（站点工作时区的日期）',
  last_no     INT          NOT NULL DEFAULT 0 COMMENT '当日已分配的最大序号',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC，当日首个询盘）',
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (site_id, seq_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '询盘编号的按日序号（INQ-yyyyMMdd-NNNN）';
```

要点：在创建询盘的事务中执行 `INSERT INTO inq_no_sequence (site_id, seq_date, last_no) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE last_no = last_no + 1`，再在同一事务中读取 `last_no` 作为序号（行锁保证并发唯一，同站点同日的取号串行，≤ 1 千条 / 天无压力；事务回滚时序号一并回滚，不产生空号）。序号超过 9999 时自动扩展位数。列名不用 `last_value`（MySQL 8 的窗口函数保留字）。

#### 4.3.9 `inq_work_calendar`：SLA 工作日历

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `time_zone` | VARCHAR(64) | 否 | `'Asia/Shanghai'` | 工作时区（IANA 时区名） |
| `work_days` | JSON | 否 | `(JSON_ARRAY(1, 2, 3, 4, 5))` | 每周工作日（ISO 星期：1 = 周一 … 7 = 周日） |
| `work_start` | TIME | 否 | `'09:00:00'` | 每日上班时间（工作时区） |
| `work_end` | TIME | 否 | `'18:00:00'` | 每日下班时间（工作时区） |
| `break_start` | TIME | 是 | `'12:00:00'` | 午休开始（不计入工作时间）；无午休为空 |
| `break_end` | TIME | 是 | `'13:00:00'` | 午休结束 |
| `sla_hours` | SMALLINT | 否 | `8` | 首次联系 SLA（工作小时；默认 8，即 1 个工作日） |
| `version` | INT | 否 | `0` | 乐观锁版本号 |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_cal_site (site_id)`：每站一个日历
- `ck_inq_cal_hours`：上班早于下班；午休两端同时为空或同时非空且在工作时间内；SLA 1–240 个工作小时

```sql
CREATE TABLE inq_work_calendar (
  id           BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id      BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  time_zone    VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '工作时区（IANA 时区名）',
  work_days    JSON         NOT NULL DEFAULT (JSON_ARRAY(1, 2, 3, 4, 5)) COMMENT '每周工作日（ISO 星期：1 = 周一 … 7 = 周日）',
  work_start   TIME         NOT NULL DEFAULT '09:00:00' COMMENT '每日上班时间（工作时区）',
  work_end     TIME         NOT NULL DEFAULT '18:00:00' COMMENT '每日下班时间（工作时区）',
  break_start  TIME         NULL DEFAULT '12:00:00' COMMENT '午休开始（不计入工作时间）；无午休为空',
  break_end    TIME         NULL DEFAULT '13:00:00' COMMENT '午休结束',
  sla_hours    SMALLINT     NOT NULL DEFAULT 8 COMMENT '首次联系 SLA（工作小时；默认 8，即 1 个工作日）',
  version      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by   BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cal_site (site_id),
  CONSTRAINT ck_inq_cal_hours CHECK (work_start < work_end AND ((break_start IS NULL AND break_end IS NULL) OR (break_start IS NOT NULL AND break_end IS NOT NULL AND work_start < break_start AND break_start < break_end AND break_end < work_end)) AND sla_hours BETWEEN 1 AND 240)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SLA 工作日历（工作时区、工作日、工作时间、SLA 工作小时）';
```

要点：
- 工作时间计算：某日是工作日 ⇔（`work_days` 包含该日星期且不是 `HOLIDAY`）或该日是 `WORKDAY`（调休补班日）；每个工作日的工作时段为 `work_start`–`work_end` 扣除午休。按 `time_zone` 换算为 UTC 后得到 `sla_due_at`。
- 日历修改只影响之后计算的到期时间，已有询盘的 `sla_due_at` 不重算。数据主体请求的时限日历属于 core（`core.dsr`，系统级），与本表无关。
- inquiry 安装或站点创建时写入默认日历（第 23 章）。

#### 4.3.10 `inq_calendar_day`：节假日与调休补班日

| 字段 | 类型 | 可空 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 否 | — | 主键（TSID） |
| `site_id` | BIGINT | 否 | — | 站点 ID（sys_site.id） |
| `calendar_id` | BIGINT | 否 | — | 工作日历 ID（inq_work_calendar.id） |
| `cal_date` | DATE | 否 | — | 日期（工作时区） |
| `day_type` | VARCHAR(16) | 否 | — | 类型：HOLIDAY（节假日，不上班）/ WORKDAY（调休补班日，按工作日计算） |
| `name` | VARCHAR(100) | 是 | — | 名称，如“国庆节”“国庆调休补班” |
| `created_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3)` | 创建时间（UTC） |
| `created_by` | BIGINT | 否 | `0` | 创建人用户 ID（0 = 系统，如导入） |
| `updated_at` | DATETIME(3) | 否 | `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` | 更新时间（UTC） |
| `updated_by` | BIGINT | 否 | `0` | 更新人用户 ID（0 = 系统） |

索引与约束：
- `PRIMARY KEY (id)`
- `uk_cday_date (calendar_id, cal_date)`：每个日期最多一条；计算时按日期范围读取；兼作 `fk_inq_cday_calendar` 的索引
- `fk_inq_cday_calendar`：组合关系，`ON DELETE CASCADE`
- `ck_inq_cday_type`：类型取值

```sql
CREATE TABLE inq_calendar_day (
  id           BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id      BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  calendar_id  BIGINT        NOT NULL COMMENT '工作日历 ID（inq_work_calendar.id）',
  cal_date     DATE          NOT NULL COMMENT '日期（工作时区）',
  day_type     VARCHAR(16)   NOT NULL COMMENT '类型：HOLIDAY（节假日，不上班）/ WORKDAY（调休补班日，按工作日计算）',
  name         VARCHAR(100)  NULL COMMENT '名称，如“国庆节”“国庆调休补班”',
  created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统，如导入）',
  updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cday_date (calendar_id, cal_date),
  CONSTRAINT fk_inq_cday_calendar FOREIGN KEY (calendar_id) REFERENCES inq_work_calendar (id) ON DELETE CASCADE,
  CONSTRAINT ck_inq_cday_type CHECK (day_type IN ('HOLIDAY', 'WORKDAY'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SLA 工作日历的特殊日期（节假日与调休补班日）';
```

要点：每年由管理员按国务院公布的节假日安排录入或导入（CSV：日期、类型、名称）；未录入下一年数据时后台提示。

### 4.4 枚举

| 字段 | 取值 | 说明 |
|---|---|---|
| `inq_inquiry.type` | `INQUIRY` / `SAMPLE` / `DOWNLOAD` / `CONTACT` | 共享询盘类型枚举：询价 / 索样 / 资料下载留资 / 一般联系（取自表单 `purpose`；`CUSTOM` 表单不创建询盘） |
| `inq_inquiry.status` | `NEW` | 新建，未分配（含待复核的 QUARANTINE） |
| | `ASSIGNED` | 已分配，等待首次联系（SLA 计时） |
| | `CONTACTED` | 已联系 |
| | `QUALIFIED` | 确认有效 |
| | `CONVERTED` | 已转为 CRM 商机（V2 预留） |
| | `CLOSED` | 已关闭（可重新打开为 ASSIGNED） |
| `inq_inquiry.priority` | `LOW` / `NORMAL` / `HIGH` | 低 / 普通 / 高 |
| `inq_inquiry.close_reason`（字典 `inquiry.close_reason`） | `INVALID` / `SPAM` / `DUPLICATE` / `NO_RESPONSE` / `OTHER` | 无效 / 垃圾（30 天后物理删除）/ 重复 / 无回复 / 其他 |
| `inq_inquiry.spam_verdict` | `ACCEPT` / `QUARANTINE` | 共享垃圾判定枚举（`REJECT` 的提交不创建询盘） |
| `inq_inquiry.device` | `DESKTOP` / `TABLET` / `MOBILE` | 设备类型 |
| `inq_activity.kind` | `NOTE` / `STATUS` / `ASSIGN` / `EMAIL_SENT` / `CALL` | 备注 / 状态变更 / 分配 / 邮件跟进 / 电话 |
| `inq_activity.detail.mode`（ASSIGN） | `RULE` / `MANUAL` / `DEFAULT` / `DUPLICATE` | 规则分配 / 手动改派 / 默认负责人 / 跟随根询盘 |
| `inq_assignment_rule.assignee_type` | `USER` / `GROUP_ROUND_ROBIN` | 指定用户 / 销售组内轮询 |
| `inq_calendar_day.day_type` | `HOLIDAY` / `WORKDAY` | 节假日 / 调休补班日 |

### 4.5 站点过滤

<<FILL: | 表 | 行级拦截器（不豁免 / 豁免） | 访问方式与理由 |>>

### 4.6 跨模块引用与清理

| 本模块字段 | 引用对象（模块.表） | 被引用对象删除 / 下线时的处理 | 触发事件 |
|---|---|---|---|
| `inq_inquiry.submission_id`、`form_id` | form.`frm_submission`、`frm_form` | 创建时经 `FormApi` 读取并复制字段；提交的保留期与询盘一致，匿名化由各自的 `PersonalDataProvider` 执行；ID 不清理 | `FormSubmittedEvent` |
| `inq_inquiry.privacy_notice_version` | form.`frm_form_notice_version` | 告知版本只追加不修改，无需清理 | — |
| `inq_inquiry.owner_user_id`、`spam_reviewed_by`、`inq_sales_group_member.user_id`、`inq_assignment_rule.assignee_ref`（USER） | core.`sys_user` | 用户停用：名下未关闭的询盘按规则或默认负责人改派，轮询跳过该成员，指向该用户的规则在后台提示 | 建议新增 `UserDeactivatedEvent`（见第 4 章） |
| `inq_inquiry_item.content_id`、`model_id`、`inq_inquiry.viewed_content_ids` | content.`cnt_content`、product.`prd_model` | 不清理：明细保存快照，后台链接在目标不存在时降级为纯文本 | — |
| `inq_inquiry.download_media_id` | media.`mda_media` | 不清理：后台显示“文档已删除” | — |
| `inq_attachment.media_id` | media.`mda_media`（`owner_module = 'inquiry'`） | inquiry 经 `MediaApi` 上传与删除；删除附件、匿名化、数据主体删除请求时先删除文件 | — |
| `inq_inquiry.message_translated` | translation（`TranslationApi.translateText`） | 译文由 inquiry 保存；translation 只在 `trn_job_item` 记录状态与用量（资源类型 `inquiry.message`），不写本表 | — |
| `inq_assignment_rule.conditions.categoryTermIds` | taxonomy.`tx_term` | 分类项删除：匹配时忽略不存在的分类项，后台提示更新规则 | `TermChangedEvent`（DELETED） |
| `inq_inquiry.close_reason` | core.`sys_dict_item`（`inquiry.close_reason`） | 内置字典项不可删除；自定义项停用后历史值照常显示 | — |
| `inq_inquiry.locale`、`conditions.locales` | core.`sys_site_locale` | V1 不删除站点语言，无需清理 | — |
| `created_by`、`updated_by` | core.`sys_user` | 不清理 | — |

本模块发布的事件：`InquiryCreatedEvent`、`InquiryAssignedEvent`、`InquiryStatusChangedEvent`（V2 的 crm、analytics 订阅；Webhook 由 inquiry 经 `NotificationApi` 提交）。订阅：`FormSubmittedEvent`。实现的扩展点：`NotificationTemplateProvider`（`inquiry.assigned.staff`、`inquiry.autoreply.customer`、`inquiry.sla.reminder`）、`PersonalDataProvider`。调用：`FormApi`、`TaxonomyApi`、`TranslationApi`、`NotificationApi`、`MediaApi`、`ContentApi`，以及 product 实现的 `ContentVariantProvider`（product 停用时明细只记录内容）。

其他模块以询盘 ID 引用本模块（`ntf_message.source_id`、`trn_job_item.resource_id`），只用于追踪，不需要本模块通知清理。

---

### 4.7 迁移脚本

| 文件 | 内容 | 里程碑 |
|---|---|---|
| `db/migration/inquiry/V001__init.sql` | 4.3 中的全部表（10 张：`inq_sales_group`、`inq_sales_group_member`、`inq_assignment_rule`、`inq_inquiry`、`inq_inquiry_item`、`inq_activity`、`inq_attachment`、`inq_no_sequence`、`inq_work_calendar`、`inq_calendar_day`），按本节顺序创建 | <见第 13 节> |

## 5. 业务流程

### 5.1 <流程名称>

```text
<步骤；标出事务边界、调用的服务接口、发布的事件、异步部分>
```

| 步骤 | 说明 | 失败处理 |
|---|---|---|

## 6. 接口

### 6.1 Admin 端点

| 方法 | 路径（/api/admin/v1/<apiPrefix>/…） | 说明 | 权限码 | 并发控制 / 幂等 | 审计 |
|---|---|---|---|---|---|

### 6.2 Public 端点

| 方法 | 路径（/api/public/v1/<apiPrefix>/…） | 说明 | 限流 | 缓存 |
|---|---|---|---|---|

### 6.3 Internal 端点

| 方法 | 路径（/api/internal/v1/<apiPrefix>/…） | 说明 | 调用方 |
|---|---|---|---|

### 6.4 权限码

| 权限码 | 名称 | 数据范围 | 高危（sensitive） | 受语言范围限制（localeScoped） |
|---|---|---|---|---|

### 6.5 错误码

| 错误码 | HTTP 状态 | 后台中文信息 | 前台文案键（仅公开接口） |
|---|---|---|---|

## 7. 后台界面

### 7.1 菜单与路由

| 菜单 key | 名称 | 父菜单 | 路由 | 权限码 |
|---|---|---|---|---|

### 7.2 页面

| 页面 | 路由 | 主要功能 | 使用的 admin-kit 组件 |
|---|---|---|---|

### 7.3 注册到内容编辑外壳的标签

| key | 适用内容类型 | 权限码 | 组件 |
|---|---|---|---|

### 7.4 界面文案

> texts.ts 中的分组与关键文案；后台只用简体中文。

## 8. 前台组件与模板

> 没有前台组件时写“不适用”。

### 8.1 组件

| 组件类型 | 名称（label） | 变体 | 分类 | allowedIn | 数据源 | 是否水合 |
|---|---|---|---|---|---|---|

### 8.2 属性与翻译

| 组件 | 属性 | 类型 / x-editor | x-translatable | maxLength |
|---|---|---|---|---|

### 8.3 模板

| 模板 key | 内容类型 | 固定区域与插槽 |
|---|---|---|

### 8.4 数据源

| key | 参数 | 返回 | 缓存标签 |
|---|---|---|---|

### 8.5 文案键

| 文案键 | 默认中文文案 | 使用位置 |
|---|---|---|

## 9. 配置项

### 9.1 站点设置（settings.schema.json）

| 键 | 类型 | 默认值 | 说明 | 敏感（x-secret） |
|---|---|---|---|---|

### 9.2 部署配置（application.yml，前缀 b2bweb.<id>）

| 属性 | 默认值 | 说明 |
|---|---|---|

## 10. 定时与异步任务

> 没有时写“不适用”。定时任务实现 `ScheduledTask`，通用异步任务实现 `JobHandler`（平台框架设计 4.4.12）；锁名与任务类型按平台框架设计 3.7 命名。事件监听器已在 2.6 中列出，这里只补充耗时长、需要单独调度或重试的处理。`PER_SITE` 任务由 core 在站点上下文中逐站点执行，停用了本模块的站点自动跳过（系统设计 3.8）。外部服务（AI 引擎、邮件、CDN）只在事务提交后的异步处理中调用，失败不回滚已提交的业务数据（ADR-003、ADR-017）。

| 任务 | 类型（定时 / sys_job / 事件监听） | 触发 | 锁名 | 幂等与重试 | 模块停用时 |
|---|---|---|---|---|---|

> 每个任务另写：执行粒度（`PER_SITE` / 系统级）、单次处理量与分批方式、超时、能否取消、失败后的可见结果（`sys_job` 的失败报告、指标 `b2bweb.<id>.*` 或告警）。

## 11. 测试要点

> 测试层级、替身与契约测试的写法见平台框架设计第 9 章，门禁与覆盖率见第 10 章。“至少”后的用例为必测项；不适用的行写“不适用：<原因>”，不得删除。

| 层级 | 覆盖内容 | 关键用例 |
|---|---|---|
| 单元 | <领域对象、不变量（3.2）、状态机（3.3）、扩展点实现（2.4）> | 至少：3.2 每条不变量的违反用例；3.3 每个非法状态转换被拒绝 |
| 模块集成（Testcontainers + 替身） | <应用服务 + 本模块的表与迁移> | 至少：迁移脚本在空库（MySQL 8.4）上执行通过；6.1 中声明的并发控制与幂等；唯一约束冲突转为 6.5 的错误码 |
| 契约（作为提供方） | <2.3 中本模块提供的服务接口> | 继承 testkit 中的 `<接口名>ContractTest`，用真实实现运行；不提供服务接口时写“不适用” |
| 事件（Scenario API，含重复投递） | <2.6 中发布与订阅的事件> | 至少：事件在业务事务内发布、事务回滚时不发布；每个监听器重复投递只生效一次、过期事件被丢弃（ADR-003） |
| 停用行为（可停用模块） | <系统设计 3.8 的各项> | 至少：停用后写操作与公开接口返回 `CORE_MODULE_DISABLED`；扩展点实现不再被收集；定时任务跳过；`analyzeDisableImpact` 的结果正确；重新启用后数据完整（S10） |
| 站点隔离 | <6.1 的全部后台接口与 4.5 的全部站点级表> | 至少：站点 A 的用户不能读写站点 B 的数据（遍历全部后台端点，系统设计 28）；豁免表只在 4.5 列出的位置访问 |
| 翻译（如有可翻译字段） | <`TranslatableContentProvider` / `TranslatableResourceProvider` 实现> | 至少：提取 → 回写往返一致；`x-translatable = false` 的字段不被提取；`HUMAN` / `REVIEWED` 段不被机器译文覆盖；资源变化时发布 `TranslatableResourceChangedEvent` |
| 前端（组件、Admin） | <主题组件、Admin 页面> | 至少：组件 Schema 校验与画廊示例（视觉回归）；前台文字只来自文案键、后台文字只来自 `texts.ts`；无权限时操作不可见 |
| E2E（场景） | <13.2 中验证方式为 E2E 的验收项> | Playwright 用例名包含 13.2 的验收项编号 |

## 12. 独立开发说明

### 12.1 依赖的测试替身

> 2.2 使用的每个服务接口、2.7 使用的每个端口、2.5 收集的每个扩展点，都要在此列出 testkit 中对应的替身或示例实现（平台框架设计 2.4、第 9 章）。testkit 中缺少的替身向框架组提出需求（DoR 第 3 条，平台框架设计 2.7），不在本模块中另建一套代替其他模块的模拟实现；单元测试可以用 Mockito 模拟个别方法，但对其他模块行为的假设以替身与契约测试为准。

| 替身（platform-testkit） | 代替的模块 | 用途 | 需要预置的数据 |
|---|---|---|---|

### 12.2 本模块提供的替身

> 只有提供服务接口的模块填写（core、content、url、media、taxonomy、form、notification、translation，见平台框架设计 4.2）；其他模块写“不适用：不提供服务接口”。替身由框架组编写并维护，与真实实现通过同一套契约测试；本模块改变服务接口的行为时，在同一个 PR 中同步修改契约测试，并通知框架组修改替身（平台框架设计 2.3）。无法消除的差异（如不做病毒扫描、不生成真实缩略图）列入下表，并确认不影响使用方的测试结论。

| 服务接口 | testkit 中的替身 | 与真实实现的差异（应为零，或已在契约测试中声明） |
|---|---|---|

### 12.3 本地运行

> 启动方式、依赖的本地服务（平台框架设计第 11 章）、需要的配置。

### 12.4 演示数据

> tools/demo-data 中本模块的数据及其用途。

## 13. 验收标准

### 13.1 里程碑切片

> 与平台框架设计 13.5 中本模块的一行一致；调整切片按平台框架设计 12.5 处理。该里程碑没有切片时写“—”（不表示不能提前开发）。

| 里程碑 | 交付功能 | 验收场景 |
|---|---|---|
| M1 | <…> | <S1、S2、S4 中与本模块相关的部分，或“模块自身”> |
| M2 | <…> | <…> |
| M3 | <…> | <…> |
| M4 | <…> | <…> |

### 13.2 验收项

> 每条验收项写明输入、期望结果与度量（如“5 分钟内”“P95 ≤ 200 ms”）。场景验收项只写本模块负责的部分（如 S2 中 url 负责“旧 URL 301 到新 URL、不产生重定向链”），不复述整个场景；各里程碑的场景见平台框架设计 13.6。验证方式为单元 / 集成 / E2E 时写出测试类或用例名；人工检查写明检查人与方法。

| # | 验收项 | 对应场景（S1–S13）或模块自身 | 验证方式（单元 / 集成 / E2E / 人工） | 里程碑 |
|---|---|---|---|---|
| A1 | <…> | <S? / 模块自身> | <集成：`<测试类>`> | M? |

> 每个模块都要包含以下通用验收项（不适用时注明原因）：架构测试与 Modulith 验证通过，无跨模块 import 与表访问；站点隔离测试覆盖全部后台接口；后台写操作都有权限码与审计记录；可停用模块满足 S10；可翻译字段与资源已登记并通过往返测试（S11、S12）；系统设计 1.5 中与本模块相关的非功能指标；文档与代码一致（DoD，平台框架设计 2.8）。

## 14. 待定问题

> 记录评审与开发中尚未决定的事项。“阻塞开发”的问题必须在评审通过（DoR）前关闭；需要新契约或修改上层文档的问题，写明契约变更请求或 Issue 编号。已关闭的问题保留在表中，填写结论。

| # | 问题 | 影响 | 是否阻塞开发 | 负责人 | 最晚解决 | 结论 |
|---|---|---|---|---|---|---|

## 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| V1.0 | 2026-10-14 | 初稿 |
