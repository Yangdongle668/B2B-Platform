# 站内搜索（search）模块设计

| 项 | 内容 |
|---|---|
| 模块 ID | `search` |
| 名称 | 站内搜索 |
| 层 | L4 |
| 表前缀 | `sch_`（ADR-021提案） |
| API 前缀 | `search` |
| 可停用 | 是 |
| 模块版本 | 1.0.0（module.json 的 version） |
| 平台版本 | `>=1.0.0 <2.0.0`（module.json 的 platform） |
| 里程碑切片 | M1：—；M2：—；M3：—；M4：站内搜索与分面筛选（详见第 13 节） |
| 模块负责人 | 待定 |
| 文档版本 | V1.4 |
| 状态 | 草案 |
| 更新日期 | 2026-10-20 |
| 依据 | 系统设计 V1.7 第 19 章及 3.2、3.8、24–29 章；平台框架设计 V1.4 第 4、7–13 章；ADR-002、ADR-003、ADR-005、ADR-009、ADR-018 |

> 本文只引用《平台框架设计》第 4 章中的契约，不定义新的跨模块契约；需要新契约时按平台框架设计 2.3 提出契约变更请求。

## 1. 职责与边界

### 1.1 职责

从内容发布快照构建按站点和语言隔离的 Meilisearch 索引；提供关键词搜索、分面筛选、排序与全量重建，并在引擎故障时降级到发布投影。

功能交付范围按第 13 章分为 M1–M4；表结构、约束与枚举以第 3、4 章为准。

### 1.2 不负责的事

| 事项 | 归属模块 | 边界 |
|---|---|---|
| 内容生命周期与路由 | content / url | 本模块只承担自身职责；不直接修改 cnt_、url_ 表 |
| 身份、权限、设置、任务及审计 | platform-core | 复用平台公共服务 |
| 第三方客户端与网络传输 | infrastructure | 仅经既有端口或 L2 服务使用，不在业务模块引入客户端 |

### 1.3 协作概览

| 协作方式 | 对象 | 本模块行为 |
|---|---|---|
| 服务接口 | ContentApi、UrlApi、MediaApi、TaxonomyApi | 方法与事务边界见 2.2；无跨模块表访问 |
| 扩展点 | DataSourceProvider、SearchDocumentProvider | 实现/收集清单见 2.4、2.5 |
| 领域事件 | 见 2.6 | 与本地事务一起登记，提交后异步投递；重复与过期处理独立验证 |

### 1.4 适用的架构约束

遵守 P2、P3 与 ADR-018：编译依赖只到 platform-core/platform-api，跨模块协作只用第 2 章登记契约；只访问自己的表，禁止跨模块 JOIN/外键。遵守 ADR-003：事务内登记、提交后投递、监听器幂等并检查过期。遵守 ADR-009：每次请求/任务明确站点上下文，站点表默认过滤。

公开读取只用当前语言发布态，不跨语言回退（ADR-005）；预览与公开缓存隔离。后台中文文案与错误集中管理，前台文案经 UiTextApi，样式只用 Design Token。远程端口在数据库事务外调用，密钥经 SecretCipher，不写日志。

## 2. 依赖与契约

### 2.1 module.json

```json
{
  "id": "search",
  "name": "站内搜索",
  "version": "1.0.0",
  "layer": "L4",
  "platform": ">=1.0.0 <2.0.0",
  "requires": {
    "content": "^1.0",
    "url": "^1.0",
    "media": "^1.0",
    "taxonomy": "^1.0"
  },
  "optional": {},
  "disableable": true,
  "tablePrefix": "sch_",
  "apiPrefix": "search",
  "permissions": [
    {
      "code": "search:index:view",
      "name": "查看索引"
    },
    {
      "code": "search:index:manage",
      "name": "管理索引"
    }
  ],
  "menus": [
    {
      "key": "search",
      "name": "站内搜索",
      "parent": "settings",
      "route": "/search/indexes",
      "permission": "search:index:view",
      "order": 100
    }
  ],
  "settings": null,
  "components": [],
  "events": {
    "publishes": [],
    "subscribes": [
      "ContentPublishedEvent",
      "ContentUnpublishedEvent",
      "ContentTrashedEvent",
      "ContentRestoredEvent",
      "TermChangedEvent",
      "SiteSettingsChangedEvent",
      "ModuleEnabledEvent",
      "ModuleDisabledEvent",
      "ContentPathChangedEvent",
      "LocalizationDeletedEvent"
    ]
  }
}
```

描述符按ADR-021提案声明sch_控制表；索引文档可重建，事务日志/指针/构建状态需要持久化和备份。

### 2.2 使用的服务接口

| 接口（平台框架设计小节） | 方法 | 用途 | 调用位置与时机 | 测试替身 |
|---|---|---|---|---|
| `ContentApi`（4.4.16） | findPublishedSnapshot、findPublishedCards、queryPublished、scanPublished | 读当前发布快照与卡片；内容类型所属模块创建身份并标记草稿变化 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryContentApi` |
| `UrlApi`（4.4.17） | pathsOf | 解析当前路径、生成 slug 或规范地址；不读取 url 表 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryUrlApi` |
| `MediaApi`（4.4.18） | findAll、views、imageSources | 媒体视图、图片源与授权下载；不读取媒体表 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryMediaApi` |
| `TaxonomyApi`（4.4.19） | findTerms、termViews、assignmentsOf | 词汇表、分类视图与内容指派；不读取分类表 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryTaxonomyApi` |
| `SiteApi`（4.4.1） | 按该契约声明的方法 | 语言、站点模块开关 | 业务事务内；站点切换由任务入口完成 | `InMemorySiteApi` |
| `SiteContextProvider`（4.4.2） | 按该契约声明的方法 | 当前站点；异步与任务由 core 恢复 | 业务事务内；站点切换由任务入口完成 | `FakeSiteContextProvider` |
| `PermissionChecker`（4.4.7） | 按该契约声明的方法 | 后台权限与数据范围 | 业务事务内；站点切换由任务入口完成 | `FakePermissionChecker` |
| `AuditApi`（4.4.9） | 按该契约声明的方法 | 同事务写审计，密钥与个人数据脱敏 | 业务事务内；站点切换由任务入口完成 | `InMemoryAuditApi` |
| `EventMetaFactory`（4.4.14） | 按该契约声明的方法 | 事件 ID、版本、站点与操作人 | 业务事务内；站点切换由任务入口完成 | `FakeEventMetaFactory` |
| `ExtensionRegistry`（4.4.15） | 按该契约声明的方法 | 按启用模块收集 2.5 的扩展点 | 业务事务内；站点切换由任务入口完成 | `InMemoryExtensionRegistry` |
| `JobApi`（4.4.12） | 按该契约声明的方法 | 耗时任务提交、取消、进度与结果 | 业务事务内；站点切换由任务入口完成 | `InMemoryJobApi` |

所有 L2/L3/L4 读写隐式使用当前站点；按 ID 查询单批最多 500。可选服务用 ObjectProvider 注入并检查当前站点启用状态，缺失时降级，不通过业务表绕过服务。

### 2.3 提供的服务接口

不适用：本模块不提供跨模块服务接口；其他模块通过 2.4 的既有扩展点或领域事件协作。

### 2.4 实现的扩展点

| 扩展点（平台框架设计小节） | 计划实现类 | 适用范围 | order | 收集方 |
|---|---|---|---|---|
| `DataSourceProvider`（4.5.6） | `SearchDataSourceProvider` | search-query | DEFAULT=1000 | delivery |
| `PlatformModule`（4.5.22） | `SearchModule` | 本模块安装/升级；可停用模块提供影响报告与停用后保留数据规则 | DEFAULT=1000 | core |
| `SearchChangeCapture`（4.10.4，Proposed） | `SearchChangeCaptureImpl` | 当前站点搜索变更日志 | 可选唯一实现 | core |

以上类名为待实现名称。PublishContributor 不调用调用方的写方法，不访问外部服务；翻译提供方只写本模块草稿/资源，先校验源指纹与共同 version。停用时 analyzeDisableImpact 返回本模块内容、组件及外部引用的影响，数据保留。


### 2.5 收集的扩展点

| 扩展点 | 调用时机 | 过滤 | 失败行为 | 测试示例 |
|---|---|---|---|---|
| `SearchDocumentProvider`（4.5.14） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleSearchDocumentProvider` |

### 2.6 发布与订阅的事件

发布：

不适用：没有本模块领域事件。

订阅：

| 事件 | 处理 | 幂等 | 过期与删除 | 失败与重试 |
|---|---|---|---|---|
| `ContentPublishedEvent` | 重读最新发布快照后构建文档；同对象写入与重建的串行化协议见第 14 章 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentUnpublishedEvent` | 移除该语言的公开投影/候选/缓存依赖，人工关系及历史快照按 4.6 保留 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentTrashedEvent` | 按当前回收站状态排除公开读取；不把进回收站等同物理删除 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentRestoredEvent` | 恢复回收站不等于重新发布；只在后续发布事件构建索引 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `TermChangedEvent` | 按 change/termId 更新相关草稿引用或重算候选；草稿指派不冒充发布态指派 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `SiteSettingsChangedEvent` | 按 scopes、settingKeys、localeChanges 筛选 4.6 的相关变化；不对无关设置做全量重建 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ModuleEnabledEvent` | 重新扫描该模块当前已发布内容，补回停用期间跳过的变化 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ModuleDisabledEvent` | 当前站点隐藏该模块的公开能力，保留自身数据；处理事件.moduleId 而非假定是接收方自身 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentPathChangedEvent` | 重读当前路径并刷新 URL，revision_no 可能不变，不能只按修订号忽略 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `LocalizationDeletedEvent` | 仅清理对应语言的本模块本地化/投影，不删除其他语言或共享业务字段 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 清理使用事件中的 ID；查不到父对象仍须清理 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |

监听器按框架 5.8 建立站点/system 上下文；`ContentPurgedEvent` 在模块停用时仍必须清理（deliverWhenModuleDisabled）。其他停用期间跳过并完成的事件，重新启用后通过扫描当前状态补偿，不能假定还会重投。数据库幂等标记与本模块数据在同一事务提交；外部发送、下载、搜索与 CDN 调用不得被数据库长事务包裹，使用已声明的任务/端口和业务幂等键。事件 payload 仅引用框架现有字段，本文不扩展签名。

### 2.7 使用的端口

| 端口（平台框架小节） | 用途 | 测试替身 |
|---|---|---|
| `SearchEngineClient`（4.7.6） | 第 5 章外部能力；只通过端口调用基础设施 | `InMemorySearchEngineClient` |
| `RateLimiter`（4.7.15） | 第 5 章外部能力；只通过端口调用基础设施 | `InMemoryRateLimiter` |

## 3. 领域模型

### 3.1 聚合、实体与值对象

| 类型 | 对象 | 职责 |
|---|---|---|
| 值对象 | SearchDocumentContext / SearchDocumentFields | 框架 4.5.14：从发布快照构建内容类型专属字段 |
| 读模型 | 搜索文档 | 本地化 ID 作为主键，带内容 ID、修订、当前语言路径及公共搜索字段 |
| 值对象 | SearchIndexSettings / SearchRequest / SearchResponse | 框架 4.7.6：索引设置、经过白名单约束的查询及结果 |
| 值对象 | SearchFacetDefinition | 由各类型提供，声明 attr 字段的类型、可筛选及可排序能力 |
| 任务 | SearchTaskStatus / sys_job | 引擎任务确认与全量重建进度；Meilisearch 读模型可删除重建 |

### 3.2 不变量

| # | 不变量 | 保证方式 |
|---|---|---|
| 1 | 一个站点一种语言一个索引；不存草稿、不跨语言回退 | SearchIndexNames.content；只读发布快照 |
| 2 | 文档 id 为 localizationId 字符串；revision_no 不得倒退 | 事务捕获+私有代次串行构建，逐任务await；活跃代次不可变 |
| 3 | noindex、未发布、非公开语言或停用内容不进入查询结果 | 构建返回空则删除；查询与交付再次校验公开状态 |
| 4 | 搜索文本来自快照，body_text 最长 10000 字符 | provider 输出纯文本，search 再截断 |
| 5 | filter/sort/facets 只能使用声明字段、正确类型和值 | SearchFilters 转义；禁止原始表达式 |
| 6 | 重建失败不能切换索引；切换前确认设置、批量写入及增量追平成功 | 逐项 await；单站点/语言只允许一个重建任务 |
| 7 | 搜索引擎是可重建读模型，不作为内容事实来源 | 仅从 ContentApi/扩展点全量重建，主密钥不下发浏览器 |

### 3.3 状态机

控制状态见4.3.1：BUILDING→READY→ACTIVE→RETIRED；失败或失去fence转FAILED/ABANDONED。引擎任务ENQUEUED→PROCESSING→SUCCEEDED/FAILED/CANCELED，taskId仅代表受理。取消只在数据库指针切换前生效，切换失败旧代次继续服务。

## 4. 数据模型

> 遵守平台框架设计第 7 章的数据库通用约定。

### 4.1 表清单

ADR-021（Proposed）补充 search 自有控制表，内容事实仍在 content，搜索文档仍在 Meilisearch。它们记录可靠捕获与切换状态，需要数据库备份，不能因“索引可重建”而丢弃。完整写入、日志保留与切换流程见框架4.10.4。

| 表 | 用途 | 站点级 | 软删除 |
|---|---|---|---|
| sch_index_state | 每语言的变更序号、活跃指针、版本与构建fence | 是 | 否 |
| sch_change | 与源事务一起提交的有序变更日志 | 是 | 否；按安全水位清理 |
| sch_build | 每次独立构建代次及最后引擎任务状态 | 是 | 否；按GC与审计策略保留 |

### 4.2 ER 图

sch_index_state按(site_id,locale)逻辑关联sch_change与sch_build，不设跨模块外键。构建任务由core.sys_job调度；search只经JobApi访问该任务。

### 4.3 表结构

逻辑名沿用 `SearchIndexNames.content(siteId, locale)`，物理名为 `{逻辑名}_g_{buildId}`。公开查询从sch_index_state读取活跃指针；每次尝试独立代次，已活跃代次不可写。旧的固定 `_rebuild` + swap 不再用于本提案。

| 字段 | 类型 | 来源与约束 |
|---|---|---|
| `id` | string | localizationId；主键 |
| `content_id`、`localization_id` | string | TSID，不转成 JS number |
| `type`、`locale`、`revision_no` | string/string/int | 已发布内容类型、当前语言、修订号 |
| `title`、`summary`、`body_text` | string | 当前语言纯文本；body_text ≤10000 字符 |
| `terms` | 发布态分类的 ID/名称集合 | 从发布快照与 TaxonomyApi 解析；过滤按 ID，不按可改名的文字 |
| `attr` | object | provider 声明的 STRING/NUMBER/BOOLEAN 值或数组 |
| `model_nos` | string[] | 型号；禁止拼写容错 |
| `url`、`cover` | 当前语言路径 / 媒体公开视图 | 只从服务接口与引用解析器取得 |
| `published_at`、`sort` | 数字时间戳 / integer | 公开文档排序，时间转换不受主机时区影响 |

搜索优先级：title > model_nos > summary > terms 的名称 > body_text。可筛选字段 type、terms 的 ID、以及 provider 明确声明的 attr 字段；可排序 published_at、sort 及声明 sortable 的属性。中文字段通过 localizedAttributes 声明 `cmn`，使用 Meilisearch 内置分词；按语言配置同义词，model_nos 关闭拼写容错。设置和修改都经 SearchEngineClient.ensureIndex 并 await 确认。嵌套 terms 映射、中文本地化字段配置与 SearchIndexSettings 的具体映射须在端口契约测试中验证，不能把框架未提供的 SDK 配置直接塞进业务模块。

#### 4.3.1 控制表 DDL（ADR-021，Proposed）

```sql
CREATE TABLE sch_index_state (
  id              BIGINT       NOT NULL COMMENT 'TSID',
  site_id         BIGINT       NOT NULL,
  locale          VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  change_seq      BIGINT       NOT NULL DEFAULT 0 COMMENT '源事务顺序，行锁保护',
  applied_seq     BIGINT       NOT NULL DEFAULT 0 COMMENT '活跃代次已追平水位',
  active_index    VARCHAR(200) COLLATE utf8mb4_0900_bin NULL COMMENT '不可变物理索引名',
  active_version  BIGINT       NOT NULL DEFAULT 0 COMMENT '指针CAS版本',
  build_fence     BIGINT       NOT NULL DEFAULT 0 COMMENT '构建认领代次',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by      BIGINT       NOT NULL DEFAULT 0,
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by      BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_state (site_id, locale),
  CONSTRAINT ck_search_seq CHECK (change_seq >= applied_seq AND applied_seq >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索活跃指针与事务水位';
```

```sql
CREATE TABLE sch_change (
  id                BIGINT       NOT NULL COMMENT 'TSID',
  site_id           BIGINT       NOT NULL,
  locale            VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  sequence_no       BIGINT       NOT NULL COMMENT '同语言提交序号',
  localization_ids  JSON         NOT NULL COMMENT '完整受影响ID集合；全量刷新时为空数组',
  full_refresh      TINYINT(1)   NOT NULL DEFAULT 0,
  created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by        BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_change (site_id, locale, sequence_no),
  CONSTRAINT ck_search_full CHECK (full_refresh IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索事务变更日志';
```

```sql
CREATE TABLE sch_build (
  id                       BIGINT       NOT NULL COMMENT 'buildId/TSID；每次尝试全新',
  site_id                  BIGINT       NOT NULL,
  locale                   VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  job_id                   BIGINT       NOT NULL COMMENT '经JobApi创建，逻辑引用',
  physical_index           VARCHAR(200) COLLATE utf8mb4_0900_bin NOT NULL,
  base_index               VARCHAR(200) COLLATE utf8mb4_0900_bin NULL COMMENT '复制基础代次；存活构建引用时禁止GC',
  expected_active_version  BIGINT       NOT NULL,
  build_fence              BIGINT       NOT NULL,
  start_seq                BIGINT       NOT NULL COMMENT '全量H0；复制从基础代次applied_seq起',
  applied_seq              BIGINT       NOT NULL,
  status                   VARCHAR(16)  NOT NULL DEFAULT 'BUILDING',
  last_engine_task_id      BIGINT       NULL COMMENT '仅单执行流顺序发出并逐个await',
  heartbeat_at             DATETIME(3)  NULL COMMENT '调度提示；不是发布资格依据',
  created_at               DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by               BIGINT       NOT NULL DEFAULT 0,
  updated_at               DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by               BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_physical (site_id, physical_index),
  KEY idx_search_build (site_id, locale, status),
  CONSTRAINT ck_search_build_state CHECK (status IN ('BUILDING','READY','ACTIVE','RETIRED','ABANDONED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索不可变索引代次';
```

所有表默认site过滤。控制行只由search写，源模块通过core协调的SearchChangeCapture追加，不能直接访问sch_表。日志至少保留到全部存活构建所需起点之后；无存活构建且活跃水位已覆盖才按批删除，失败构建与恢复策略同框架4.10.4。当前无代码迁移、MySQL执行或吞吐测试结果。

### 4.4 枚举

| 枚举 | 取值 | 说明 |
|---|---|---|
| FacetType | STRING / NUMBER / BOOLEAN | 与 provider 的属性定义一致 |
| SearchTaskState | ENQUEUED / PROCESSING / SUCCEEDED / FAILED / CANCELED | 以 await 返回的引擎任务状态为准 |
| 结果 degraded | true / false | 只在分类列表回退投影时为 true；搜索页故障显示提示 |

### 4.5 站点过滤

索引前缀强制取当前站点；请求不得直接提供 indexName 或 siteId，locale 必须是当前站点公开语言。重建、列举、删除只允许 SearchIndexNames.sitePrefix 的索引集合；多站点同 ID/型号不得串索引。浏览器不持有 Meilisearch 主密钥。

### 4.6 跨模块引用与清理

| 引用 | 触发 | 处理 |
|---|---|---|
| 内容/本地化 ID、修订与路径 | ContentPublished/Unpublished/Trashed/Restored/PurgedEvent、LocalizationDeletedEvent、ContentPathChangedEvent | 事务捕获已覆盖变化；事件仅唤醒下一代构建，公开查询复核当前状态 |
| 分类与分面 | TermChangedEvent、SiteSettingsChangedEvent | 重建受影响文档或当前语言索引配置；分类筛选只用发布态分类指派 |
| 模块与语言 | ModuleEnabledEvent、ModuleDisabledEvent、SiteSettingsChangedEvent | 停用或语言不公开时过滤并删除对应文档；重启用后重建，保留内容事实数据 |

异步删除与查询有窗口：返回卡片前仍经 ContentApi 批量校验当前发布状态、路径及修订，过滤陈旧命中后可继续读取下一页补足；估算总数来自引擎，不能声明为精确发布态数量。

### 4.7 迁移脚本

新增 `db/migration/search/V001__control_state.sql`，内容为4.3.1三张控制表；M4前建立并验证。不得改写其他模块或已合入迁移。

## 5. 业务流程

### 5.1 核心业务流程

| 步骤 | 事务及行为 | 失败处理 |
|---|---|---|
| 1 | 源事务在beforeCommit捕获sch_change；构建任务读取当前ContentApi发布快照，经SearchDocumentProvider生成文档并写私有代次，逐任务await成功。noindex/未发布/provider为空不纳入新代次。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |
| 2 | 搜索校验语言公开与模块启用，按 provider 声明白名单构造 SearchFilters，限制 query、分页、筛选和排序；不允许客户端传任意引擎表达式。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |
| 3 | 每个attempt独立代次，扫描并重放事务日志、await成功后，以水位/活跃版本/buildFence三条件CAS切换指针；失败保留旧代次，V1不调用swap。引擎故障时分类列表降级 ContentApi.queryPublished 的 MySQL 发布投影，分类列表响应标记降级、不伪造分面计数；完整站内搜索页显示友好不可用提示，不声称 MySQL 能替代全文相关性。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |

后台读写先校验权限与站点/语言范围；草稿写入和审计在同一事务。远程操作不能使用数据库长事务包裹。幂等重放返回既有结果；参数不同却复用同一幂等键时拒绝。

### 5.2 一致性、失败与恢复约束

| 场景 | 必须满足的行为 | 可验证结果 |
|---|---|---|
| 结构化筛选与纯公开输出 | 关键词/筛选/排序必须白名单和类型检查；只从该语言发布快照建索引，私有媒体不输出直链。中文分词显式 cmn，型号禁拼写容错。 | 恶意引擎表达式、未知 attr、错误数值类型均拒绝；中文词与完整型号可检索；正文≤10000字符。 |
| 异步成功和内容版本 | ensureIndex/upsert/delete 返回 taskId 只代表受理；await 返回仍 PROCESSING 要继续观察，FAILED 才记录失败。查询批量校验当前状态/路径/修订，过滤旧索引命中，不把估算 total 当精确数。 | 延迟处理任务不能被提前标记成功；r2 写入后重放 r1 不能让旧版本出现在公开结果；缺失索引项由修复任务补回。 |
| 重建切换和增量竞态 | 源事务记录提交序号，私有代次必须追平日志；change_seq/active_version/build_fence均匹配才切指针。旧worker不得写活跃代次；具体协议见框架4.10.4。 | 扫描后、指针切换前分别发布/下线一项，CAS不越过日志水位；提交前后宕机恢复读取数据库指针，旧worker不能改新活跃代次。 |
| 故障降级与分面可信度 | 只将分类列表降级为 ContentApi 发布投影无分面列表，不宣称支持原全文排序或属性筛选；不支持的筛选明确提示，完整搜索页显示友好不可用状态。 | 停 Meilisearch 后分类基础列表仍可读，facets 为空或显式不可用，degraded=true；搜索页不返回伪造全文结果，响应不缓存为正常空结果。 |

### 5.3 实现前统一方案与接入要求

依据[平台框架 4.10.4](../framework/platform-framework.md)及[设计问题处理说明](../reviews/design-issue-resolution-2026-10-20.md)。新增契约相关ADR仍为Proposed；下列方案已具体化，待评审冻结后同批实现，不能据此声明运行问题已验证。涉及本节的旧异步引用、原地索引写入或待定措辞按此方案替代。

- 控制表/物理索引结构见4.3.1，search实现SearchChangeCapture；源事务的beforeCommit捕获提交顺序，不用异步事件到达顺序当屏障。
- 每个attempt独立物理代次。初次全扫或复制不可变活跃代次，重放事务日志、await全部任务后，以change_seq/active_version/build_fence三条件CAS切换指针。
- 活跃代次不再写入；下一批增量构建下一代。旧worker只能写自己的废弃代次，不能覆盖活跃索引。V1不执行swap。
- 恢复先读数据库指针，未切换仍服务旧代次；GC保护活跃/构建/在途代次，日志水位不能跨过存活构建。O(N)复制成本须压测，查询继续校验当前公开状态。

## 6. 接口

### 6.1 Admin 端点

| 方法 | 路径（相对 /api/admin/v1/search） | 说明 | 权限码 | 并发控制 / 幂等 | 审计 |
|---|---|---|---|---|---|
| GET | `/indexes` | 站点各语言索引状态 | `search:index:view` | 只读；不写入业务状态 | 否 |
| POST | `/rebuild` | 按语言重建索引并返回 jobId | `search:index:manage` | Idempotency-Key；同键同请求返回既有任务/结果 | search.rebuild.post |
| GET | `/settings` | 可搜索与分面白名单 | `search:index:view` | 只读；不写入业务状态 | 否 |

ID 序列化为字符串，时间为 ISO-8601 UTC；列表 page/size（size ≤ 100），默认 size=20，排序字段白名单。版本化资源按框架 5.5.6 使用 ETag: "v{version}" 与 If-Match，不能用任意 JSON 摘要替代平台版本协议；无 version 的集合写入方式见本节补充约束。创建返回资源及标识；耗时 POST 返回 202 与 jobId；无跨站点 ID 访问。写操作审计不包含凭据、完整个人数据或任意脚本正文。

**请求与结果约束**：filters 使用结构化字段/操作符/值，不接收原始过滤字符串；数值与枚举类型匹配 provider 声明。返回 items、page、size、total（引擎估算）与 facets/degraded，不返回引擎凭据。重建指定当前站点已配置语言，禁止任意索引名；失败保留旧索引并可在后台查询 job 结果。

### 6.2 Public 端点

GET `/api/public/v1/search?q=&type=&filters=&page=&size=`。q 最长 200 字符，size 默认 20、最大 100；type、filters、facets、sort 均经 provider 白名单验证；结果含 items、total、facets、degraded，降级不伪造分面。按站点/IP 使用 RateLimiter；速率按系统设计 26.4：60 次/分钟/IP（含站点命名空间），429 返回 Retry-After；结果页 noindex,follow，缓存最长 60 秒。

降级分类列表只保留 PublishedQuery 支持的条件；不支持的 attr/分面筛选不应悄悄忽略后返回看似精确的结果。引擎故障时完整搜索接口返回 SEARCH_UNAVAILABLE（503），前台显示错误提示；不能将故障转换成 total=0 的正常缓存结果。

### 6.3 Internal 端点

不适用：不新增 internal HTTP 端点；跨模块使用进程内 platform-api 契约。Nuxt 所需公共读取使用 6.2 的既有入口及可信代理认证。

### 6.4 权限码

| 权限码 | 名称 | 数据范围 | 高危 | 语言范围 |
|---|---|---|---|---|
| `search:index:view` | 查看索引 | 当前站点 | 否 | 否 |
| `search:index:manage` | 管理索引 | 当前站点 | 否 | 否 |

### 6.5 错误码

| 错误码 | HTTP 状态 | 后台中文信息 | 前台文案键 |
|---|---|---|---|
| `SEARCH_VALIDATION_FAILED` | 422 | 输入不符合本模块规则 | errors.validation |
| `SEARCH_NOT_FOUND` | 404 | 请求的资源不存在 | errors.notFound |
| `CORE_MODULE_DISABLED` | 404 | 站点已停用该模块 | errors.moduleDisabled |
| `SEARCH_VERSION_CONFLICT`（模块内部错误码） | 409 / 412 | 资源已被修改，请刷新后重试 | — |
| `SEARCH_UNAVAILABLE` | 503 | 搜索服务暂时不可用 | search.errors.unavailable |
| `SEARCH_FILTER_INVALID` | 422 | 筛选字段、类型或条件不合法 | search.errors.filters |
| `SEARCH_RATE_LIMITED` | 429 | 搜索过于频繁 | search.errors.rateLimit |

跨模块错误使用框架已声明常量；本模块专用 HTTP 校验错误保存在本模块错误目录，不增加 platform-api 常量。公开接口不直接展示 detail；错误文案键统一登记在 UiTextApi。

**重点业务失败**：SEARCH_VALIDATION_FAILED、SEARCH_UNAVAILABLE（本模块内部）。模块内部错误不扩展 platform-api；HTTP 状态按本节与框架错误常量映射。

本模块内部错误补充：`SEARCH_VERSION_CONFLICT` 默认 409，经 If-Match 提交为 412；幂等键复用但规范化业务请求不同为 `SEARCH_IDEMPOTENCY_CONFLICT`（409）；状态操作不适用为 `SEARCH_STATE_INVALID`（409）。只有实际存在相应端点时才注册/使用，均不新增跨模块错误常量。跨站点或数据范围外 ID 返回不泄露对象存在性的结果，日志保留 traceId 和授权判定。

以上公开 HTTP 错误属于模块内部错误映射，不扩展 platform-api；跨模块透传同语义错误时保持既有错误码。

## 7. 后台界面

### 7.1 菜单与路由

| 菜单 key | 名称 | 父菜单 | 路由 | 权限码 |
|---|---|---|---|---|
| search | 站内搜索 | settings | /search/indexes | search:index:view |

### 7.2 页面

| 页面 | 路由 | 主要功能 | 共享组件 |
|---|---|---|---|
| 站内搜索 | /search/indexes | 按第 6 章端点完成列表、筛选、详情与授权操作；失败保留编辑上下文 | 复用框架 8.2 的 SchemaForm 与通用选择器 |

### 7.3 注册到内容编辑外壳的标签

不适用：不向内容编辑外壳注册业务标签；公共生命周期控件由 content 外壳负责。

### 7.4 界面文案

| texts.ts 分组 | 中文文案 | 使用场景 |
|---|---|---|
| search.title | 站内搜索 | 页面标题 |
| search.save | 保存成功 | 保存反馈 |
| search.conflict | 内容已被修改，请刷新后重试 | 并发冲突 |
| search.confirm | 确认执行此操作？ | 有副作用操作确认 |

字段名、枚举标签、按钮、空状态、错误与任务进度统一集中在本模块 texts.ts；后台不引入 vue-i18n。无权限时隐藏操作，服务端仍再次校验。

## 8. 前台组件与模板

> 没有前台组件时写“不适用”。

### 8.1 组件

不适用：不拥有可拖入布局的组件类型。运行时 UI 或主题固定区域见第 5、6 章。

### 8.2 属性与翻译

不适用：无本模块组件属性；可翻译内容及资源按第 3、4 章登记的提供方处理。

### 8.3 模板

不适用：模板元数据由 page 的 TemplateProvider 提供；本模块只提供所属组件或数据。

### 8.4 数据源

| key | 参数 | 返回 | 缓存标签 |
|---|---|---|---|
| search-query | q、type、filters、page、size | 命中卡片、分面、总量与 degraded | 列表标签及命中内容标签 |

参数必须按 paramsSchema 校验，批量读取避免 N+1；单个数据源预算遵守 DataSourceLimits（500ms），超时返回可诊断的空区域。

布局数据源每次最多 24 条（DataSourceRequest.limit），整体页面预算与并行度遵守框架 DataSourceLimits；HTTP 列表的 size≤100 不可直接用作布局数据源上限。

### 8.5 文案键

| 文案键 | 默认中文文案 | 位置 |
|---|---|---|
| search.empty | 暂无内容 | 列表/组件空态 |
| search.loading | 正在加载 | 交互等待 |
| search.retry | 重试 | 可恢复错误 |

| 公开错误文案键 | 默认中文文案 | 用途 |
|---|---|---|
| search.errors.unavailable | 搜索服务暂时不可用 | 公开错误提示 |
| search.errors.filters | 筛选字段、类型或条件不合法 | 公开错误提示 |
| search.errors.rateLimit | 搜索过于频繁 | 公开错误提示 |

### 8.6 视觉方向与组件呈现

遵循[前台视觉设计方向与 UI 基线](../design/visual-direction.md)，Token 集中在 design-tokens 包定义，变体由主题实现；后台仍使用 Element Plus。

M4 搜索界面以输入、当前条件、结果数量和稳定网格为核心，移动筛选面板有应用/清空/关闭及焦点返回。加载、无结果、服务降级和请求失败分别可辨认；M4 前页面可展示发布态基础列表，但不展示不可用的筛选入口。

视觉稿覆盖默认、加载、空、错误、成功及键盘焦点等适用状态；本节为呈现要求，未声称已有设计稿或组件实现。

## 9. 配置项

### 9.1 站点设置（settings.schema.json）

不新增 sys_setting 键；第 4 章业务配置表、模板或布局字段沿用各自定义及默认值，不能重复存放同一配置。没有独立站点设置 Schema 时，描述符 settings 为 null。

### 9.2 部署配置（application.yml）

本模块不另设连接地址或凭据前缀；数据库、Redis、对象存储及远程服务配置采用平台框架 5.12、5.14 的 `b2bweb.infra.*` 和环境变量。第 2.7 节列出的端口由 application 装配。部署地址、TLS 与密钥不写入仓库或响应。

执行批量任务时使用第 10 章的处理量与取消点；这些是初始运行预算，不能覆盖接口安全上限。任何新增可调参数先在本节明确类型、默认值及边界后再实现。

## 10. 定时与异步任务

| 任务 | 类型 | 触发 | 处理粒度与批量 | 锁与幂等 | 停用行为 |
|---|---|---|---|---|---|
| 搜索索引重建 | sys_job | 人工或修复 | 500 快照/批；await 引擎任务成功；可在数据库指针切换前取消 | `search.rebuild_index`；按站点和对象/版本认领，幂等完成 | 跳过并保留待处理数据 |
| 索引一致性核对 | 定时 | 每晚 | 每语言按游标比对发布数量与内容摘要，发现差异提交重建任务 | `search.reconcile_index`；按站点和对象/版本认领，幂等完成 | 停用站点跳过 |

普通任务为 PER_SITE；全局引擎或跨站点认领仅在 4.5 允许的位置执行。每批完成后更新进度与心跳，检查取消；短事务不得包裹外部调用。单次工作预算 60 秒，到预算即保存游标并续跑，外部端口超时用框架 4.7 的各端口上限。可重试失败指数退避；永久失败写 sys_job 报告或领域任务状态并输出 `b2bweb.search.task.errors` 指标，不记录敏感载荷。

上述标识为本模块任务/锁的稳定名称；定时任务仅该任务互斥，不用一个 search.task 串行阻塞全部维护。core 已负责站点命名空间，不重复手工添加 s{siteId}。sys_job 进度写入后在安全批次边界响应取消；任务超时/租约丢失须停止提交新结果。

## 11. 测试要点

| 层级 | 覆盖内容 | 必测用例 |
|---|---|---|
| 单元 | 第 3.2 全部不变量、3.3 状态与纯计算 | 逐条构造非法输入；合法及非法转换均断言；业务用例见 13.2 |
| 模块集成（Testcontainers + 替身） | 第 6.1 全部端点、唯一约束和版本控制 | 无本模块 SQL 迁移；验证真实 Redis/Meilisearch 交互、依赖故障及过期结果处理 |
| 契约（提供方） | 不适用：不提供服务接口 | 扩展点实现运行框架 4.8.5 中对应 Conformance 套件 |
| 事件（Scenario API） | 2.6 的发布与订阅 | 事务回滚无事件；重复投递不重复副作用；新修订后再投旧事件不回退 |
| 停用行为 | 系统设计 3.8 | 写与公开接口 404/CORE_MODULE_DISABLED；只读历史允许；扩展点过滤、任务跳过，重启用数据保留；验证影响报告 |
| 站点隔离 | 6.1 全部端点、4.5 全部站点表或缓存索引 | A/B 两站同构数据互不可见；全局豁免表只在显式系统权限处访问 |
| 翻译 | 可翻译内容/资源与人工保护 | 不适用：不持有翻译单元；读取不跨语言回退 |
| 前端 | Admin、Schema、主题及权限 | Vitest 校验字段/权限/错误；组件画廊视觉回归、Token 及文案键检查；未授权操作不可见 |
| E2E | 13.2 用例 | Playwright 用例名包含模块及 A 编号；不得把未实现测试写成已通过 |

本章是实现后的测试要求；当前仓库没有应用或测试工程，本次只验证文档及契约引用，未执行上述应用测试。

## 12. 独立开发说明

### 12.1 依赖的测试替身

| 替身或夹具（platform-testkit） | 替代能力 | 用途 | 预置数据 |
|---|---|---|---|
| InMemoryContentApi | ContentApi | 替代对应 L2 模块 | 站点 A/B、公开与未公开语言、发布/草稿/下线对象 |
| InMemoryUrlApi | UrlApi | 替代对应 L2 模块 | 站点 A/B、公开与未公开语言、发布/草稿/下线对象 |
| InMemoryMediaApi | MediaApi | 替代对应 L2 模块 | 站点 A/B、公开与未公开语言、发布/草稿/下线对象 |
| InMemoryTaxonomyApi | TaxonomyApi | 替代对应 L2 模块 | 站点 A/B、公开与未公开语言、发布/草稿/下线对象 |
| InMemorySearchEngineClient | SearchEngineClient | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| InMemoryRateLimiter | RateLimiter | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| SampleSearchDocumentProvider | SearchDocumentProvider | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
| TestPlatform / TestSites / TestUsers | core 公共服务与站点身份 | 按框架 9.2 装配替身、ManualClock | A/B 站、不同角色/语言范围、停用模块 |
| InMemorySiteApi | SiteApi | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| FakeSiteContextProvider | SiteContextProvider | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| FakePermissionChecker | PermissionChecker | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| InMemoryAuditApi | AuditApi | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| FakeEventMetaFactory | EventMetaFactory | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| InMemoryExtensionRegistry | ExtensionRegistry | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |
| InMemoryJobApi | JobApi | 替代 core 公共服务；实现以框架 9.2 为准 | 当前站点、不同角色及成功/拒绝分支 |

类名是平台框架已定义的 testkit 交付要求，仓库尚无实现。发现缺少夹具或行为时向框架组提出 DoR 阻塞，不在模块中建立另一套跨模块模拟。

### 12.2 本模块提供的替身

不适用：不提供服务接口。扩展点的 Sample 实现由 testkit 统一维护。

### 12.3 本地运行

当前仅文档，尚无可运行的 `search` 工程。M0 完成后，在仓库根目录按平台框架第 11 章启动 MySQL 8.4、Redis 及本模块 2.7 需要的本地服务；通过 application 的 dev/test profile 装配本模块及 TestPlatform，不能直接依赖其他业务模块的实现。

后端执行框架规定的 Maven 模块集成/契约测试，前端执行 pnpm lint、typecheck、test；演示环境再用 dev 装配执行第 13 章 E2E。具体命令以 M0 实际建立的 wrapper、脚本和锁文件为准，本文不虚构当前可运行命令。

### 12.4 演示数据

计划在框架规定的 `tools/demo-data` 中提供 `search` 的演示数据（该目录尚未建立）：两站相同业务 key、中文源内容、已发布英语与未公开语言、一个草稿/缺失语言对象、一个越权用户和一个故障服务场景。模块特有数据按 13.2 的四个输入构造；不得使用真实联系人或凭据。

加载器只经已授权后台接口录入中文源数据，不直接写数据库；目标语言通过翻译流程及假引擎生成。按自然键重复加载幂等，仅补缺；不实际建立此目录直到 M0 任务要求实现。

## 13. 验收标准

### 13.1 里程碑切片

| 里程碑 | 交付功能 | 验收场景 |
|---|---|---|
| M1 | — | — |
| M2 | — | — |
| M3 | — | — |
| M4 | 站内搜索与分面筛选 | S6；搜索与数据范围验收 |

切片逐字沿用平台框架 13.5；“—”表示没有该阶段集成要求，不表示不能提前实现。

### 13.2 验收项

| # | 输入与期望结果 | 场景 | 验证方式（计划用例名） | 里程碑 |
|---|---|---|---|---|
| A1 | 中文关键词及型号精确检索正确，model_nos 不做拼写容错 | 模块自身 | 集成：`SearchA1IT`；涉及界面时 E2E：`search-A1` | M4 |
| A2 | 下线、停用与非公开语言内容不出现在结果中 | 模块自身 | 集成：`SearchA2IT`；涉及界面时 E2E：`search-A2` | M4 |
| A3 | 旧发布事件重放不能覆盖新修订，异步索引失败不标记成功 | 模块自身 | 集成：`SearchA3IT`；涉及界面时 E2E：`search-A3` | M4 |
| A4 | 重建失败旧索引仍可查询，停引擎后分类列表按发布投影降级 | 模块自身 | 集成：`SearchA4IT`；涉及界面时 E2E：`search-A4` | M4 |
| A5 | ArchUnit 与 Modulith verify 通过；零业务模块编译依赖、跨表 JOIN/外键 | 模块自身 | 架构：`SearchArchitectureTest` | M4 |
| A6 | 第 6.1 当前切片已交付后台端点覆盖 A/B 站隔离、权限拒绝；全部成功写操作有脱敏审计 | 模块自身 | 集成：`SearchIsolationIT`、`SearchAuditIT` | M4 |
| A7 | 停用与重新启用满足 S10，影响报告可列出内容与组件引用 | S10 | 集成：`SearchModuleLifecycleIT` | M4 |
| A8 | 不适用：无翻译单元；验证公开读取不跨语言回退 | S11、S12 | 集成：`SearchLocaleIT` | M4 |
| A9 | 单站1万产品×10语言、5千文章的搜索基线；记录搜索延迟/重建耗时及引擎内存，60次/分钟/IP 限流生效；故障分类降级不伪造筛选/计数 | 模块自身 | 性能报告：`SearchPerformanceIT`；架构负责人核对适用指标 | M4 |
| A10 | 实现与本文件、描述符、OpenAPI、Schema 和迁移保持一致；第 14 章阻塞项在开工前关闭 | 模块自身 | 人工：模块负责人逐项按框架 12.4 检查并附评审记录 | M4 |
| A11 | 恶意引擎表达式、未知 attr、错误数值类型均拒绝；中文词与完整型号可检索；正文≤10000字符。 | 模块自身 / 5.2 | 集成：`SearchA11IT`；涉及界面 E2E：`search-A11` | M4 |
| A12 | 延迟处理任务不能被提前标记成功；r2 写入后重放 r1 不能让旧版本出现在公开结果；缺失索引项由修复任务补回。 | 模块自身 / 5.2 | 集成：`SearchA12IT`；涉及界面 E2E：`search-A12` | M4 |
| A13 | 扫描后、指针切换前分别发布/下线一项，CAS不越过日志水位；提交前后宕机恢复读取数据库指针，旧worker不能改新活跃代次。 | 模块自身 / 5.2 | 集成：`SearchA13IT`；涉及界面 E2E：`search-A13` | M4 |
| A14 | 停 Meilisearch 后分类基础列表仍可读，facets 为空或显式不可用，degraded=true；搜索页不返回伪造全文结果，响应不缓存为正常空结果。 | 模块自身 / 5.2 | 集成：`SearchA14IT`；涉及界面 E2E：`search-A14` | M4 |

用例名为后续实现要求，不表示已有测试。性能测试需记录数据规模、并发、冷/热缓存与结果分位数；不以单次请求代替验收。

## 14. 待定问题

| # | 问题 | 影响 | 是否阻塞开发 | 负责人 | 最晚解决 | 结论/处理要求 |
|---|---|---|---|---|---|---|
| SEARCH-M0 | 工程骨架、冻结契约与 testkit 尚未建立 | 所有运行和测试用例 | 是 | 框架负责人 | M0 完成 | 按 ADR-018 与框架 13.1 交付后才满足模块开工前提；文档补齐不等于 DoR 已通过。 |
| SEARCH-01 | SearchEngineClient.upsert 没有 compare-and-set，扫描+swap 未定义增量屏障；多实例重建与旧事件可相互覆盖 | 索引单调更新与无丢失切换 | 方案已补齐，待评审冻结 | 框架负责人 / 模块负责人 | M4 索引同步开工前 | 统一方案：事务变更日志、私有不可变代次和数据库CAS切换；无需重放swap；ADR-021。详见框架4.10.4及本模块第5章；保留原编号追踪评审与实施，不宣称已实现。 |

## 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| V1.0 | 2026-10-14 | 初稿 |
| V1.1 | 2026-10-20 | 依据现有系统设计、框架契约与数据模型补齐职责、描述符、流程、接口、界面、测试及验收；保留未解决的上层契约问题，未声称运行测试通过 |
| V1.2 | 2026-10-20 | 全量复审：统一资源聚合标识、并发/失败恢复、接口闭环与验收里程碑；补充模块专项约束和审查记录，保留既有 SQL |
| V1.3 | 2026-10-20 | 补充 Tesla 参考下的前台视觉、响应式与交互状态要求，引用统一视觉基线；业务模型、接口与组件类型保持不变 |
| V1.4 | 2026-10-20 | 补齐实现前统一方案与事务/恢复接入要求；关联ADR待评审，未执行应用测试 |
