# 内容交付（delivery）模块设计

| 项 | 内容 |
|---|---|
| 模块 ID | `delivery` |
| 名称 | 内容交付 |
| 层 | L4 |
| 表前缀 | 无 |
| API 前缀 | `delivery` |
| 可停用 | 否 |
| 模块版本 | 1.0.0（module.json 的 version） |
| 平台版本 | `>=1.0.0 <2.0.0`（module.json 的 platform） |
| 里程碑切片 | M1：`resolve`、站点引导数据、预览；M2：Cache Tag + CDN 清除；M3：多语言（hreflang、未公开语言 404）；翻译完成后的失效；M4：—（详见第 13 节） |
| 模块负责人 | 待定 |
| 文档版本 | V1.5 |
| 状态 | 草案 |
| 更新日期 | 2026-10-20 |
| 依据 | 系统设计 V1.8 第 21、23、24 章及 3.2、3.8、24–29 章；平台框架设计 V1.6 第 4、7–13 章；ADR-002、ADR-003、ADR-005、ADR-009、ADR-018 |

> 本文只引用《平台框架设计》第 4 章中的契约，不定义新的跨模块契约；需要新契约时按平台框架设计 2.3 提出契约变更请求。

## 1. 职责与边界

### 1.1 职责

聚合前台路由、发布快照、模板、面包屑、数据源与引用，返回完整渲染数据；负责站点引导、隔离预览、Cache Tag 与 CDN 失效。

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
| 扩展点 | TemplateProvider、DataSourceProvider、DeliveryContributor、SiteBootstrapContributor、BreadcrumbProvider | 实现/收集清单见 2.4、2.5 |
| 领域事件 | 见 2.6 | 与本地事务一起登记，提交后异步投递；重复与过期处理独立验证 |

### 1.4 适用的架构约束

遵守 P2、P3 与 ADR-018：编译依赖只到 platform-core/platform-api，跨模块协作只用第 2 章登记契约；只访问自己的表，禁止跨模块 JOIN/外键。遵守 ADR-003：事务内登记、提交后投递、监听器幂等并检查过期。遵守 ADR-009：每次请求/任务明确站点上下文，站点表默认过滤。

公开读取只用当前语言发布态，不跨语言回退（ADR-005）；预览与公开缓存隔离。后台中文文案与错误集中管理，前台文案经 UiTextApi，样式只用 Design Token。远程端口在数据库事务外调用，密钥经 SecretCipher，不写日志。

## 2. 依赖与契约

### 2.1 module.json

```json
{
  "id": "delivery",
  "name": "内容交付",
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
  "disableable": false,
  "tablePrefix": null,
  "apiPrefix": "delivery",
  "permissions": [
    {
      "code": "delivery:definition:view",
      "name": "后台读取已启用数据源的 key、label 与 paramsSc"
    }
  ],
  "menus": [],
  "settings": null,
  "components": [],
  "events": {
    "publishes": [],
    "subscribes": [
      "ContentPublishedEvent",
      "ContentUnpublishedEvent",
      "ContentTrashedEvent",
      "ContentRestoredEvent",
      "LocalizationCreatedEvent",
      "LocalizationDeletedEvent",
      "ContentPathChangedEvent",
      "RedirectChangedEvent",
      "TermChangedEvent",
      "MediaUploadedEvent",
      "MediaUpdatedEvent",
      "MediaDeletedEvent",
      "NavigationChangedEvent",
      "SiteSettingsChangedEvent",
      "ModuleEnabledEvent",
      "ModuleDisabledEvent",
      "RelationChangedEvent",
      "FormChangedEvent",
      "TranslationCompletedEvent",
      "ContentPurgedEvent",
      "SiteResourceChangedEvent"
    ]
  }
}
```

描述符为 M0 后的实现要求；无表模块的 tablePrefix 为 null，索引与缓存均可重建。

### 2.2 使用的服务接口

| 接口（平台框架设计小节） | 方法 | 用途 | 调用位置与时机 | 测试替身 |
|---|---|---|---|---|
| `ContentApi`（4.4.16） | findPublishedSnapshot、findPublishedCards、resolvePreviewToken、assemblePreview | 读当前发布快照与卡片；内容类型所属模块创建身份并标记草稿变化 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryContentApi` |
| `UrlApi`（4.4.17） | resolve、pathOf、absoluteUrl | 解析当前路径、生成 slug 或规范地址；不读取 url 表 | 读接口在查询阶段调用；写方法加入业务事务，远程文本翻译必须在事务外 | `InMemoryUrlApi` |
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
| `PlatformModule`（4.5.22） | `DeliveryModule` | 本模块安装/升级；可停用模块提供影响报告与停用后保留数据规则 | DEFAULT=1000 | core |

以上类名为待实现名称。PublishContributor 不调用调用方的写方法，不访问外部服务；翻译提供方只写本模块草稿/资源，先校验源指纹与共同 version。本模块不可站点级停用；对可选依赖缺失/停用仍须按框架约定降级。

### 2.5 收集的扩展点

| 扩展点 | 调用时机 | 过滤 | 失败行为 | 测试示例 |
|---|---|---|---|---|
| `TemplateProvider`（4.5.5） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleTemplateProvider` |
| `DataSourceProvider`（4.5.6） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleDataSourceProvider` |
| `DeliveryContributor`（4.5.7） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleDeliveryContributor` |
| `SiteBootstrapContributor`（4.5.8） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleSiteBootstrapContributor` |
| `BreadcrumbProvider`（4.5.9） | 见第 5 章流程 | ExtensionRegistry 按站点启用状态、内容类型或 key 过滤 | 事务内校验/发布异常则回滚；只读可选部分失败记指标并降级，核心部分失败返回 503 | `SampleBreadcrumbProvider` |

### 2.6 发布与订阅的事件

发布：

不适用：没有本模块领域事件。

订阅：

| 事件 | 处理 | 幂等 | 过期与删除 | 失败与重试 |
|---|---|---|---|---|
| `ContentPublishedEvent` | 失效主体 c 标签和所有相关 list:type；即便不是首次发布也处理排序/分类变化；清理新旧路径缓存 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentUnpublishedEvent` | 移除该语言的公开投影/候选/缓存依赖，人工关系及历史快照按 4.6 保留 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentTrashedEvent` | 按当前回收站状态排除公开读取；不把进回收站等同物理删除 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentRestoredEvent` | 清理回收站过滤缓存；仍未发布时不恢复线上内容 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `LocalizationCreatedEvent` | 按第 4.6 节处理事件所标识的对象；副作用只发生在本模块数据范围 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `LocalizationDeletedEvent` | 仅清理对应语言的本模块本地化/投影，不删除其他语言或共享业务字段 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 清理使用事件中的 ID；查不到父对象仍须清理 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentPathChangedEvent` | 失效旧路径、新路径与内容标签，不返回仍缓存的旧 canonical | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `RedirectChangedEvent` | 清理当前站点路由/交付命名空间，包含之前缓存的 NOT_FOUND | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `TermChangedEvent` | 按 change/termId 更新相关草稿引用或重算候选；草稿指派不冒充发布态指派 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `MediaUploadedEvent` | 仅处理被引用媒体：更新当前媒体视图或按 4.6 防御性清理；仍有有效引用的媒体不能被主动删除 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `MediaUpdatedEvent` | 仅处理被引用媒体：更新当前媒体视图或按 4.6 防御性清理；仍有有效引用的媒体不能被主动删除 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `MediaDeletedEvent` | 仅处理被引用媒体：更新当前媒体视图或按 4.6 防御性清理；仍有有效引用的媒体不能被主动删除 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 清理使用事件中的 ID；查不到父对象仍须清理 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `NavigationChangedEvent` | 按菜单整体变化重建导航派生结果或缓存，menuId 是聚合 ID | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `SiteSettingsChangedEvent` | 按 scopes、settingKeys、localeChanges 筛选 4.6 的相关变化；不对无关设置做全量重建 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ModuleEnabledEvent` | 检查被启用模块的当前数据，重建受影响读模型并补偿停用期间跳过的事件 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ModuleDisabledEvent` | 当前站点隐藏该模块的公开能力，保留自身数据；处理事件.moduleId 而非假定是接收方自身 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `RelationChangedEvent` | 失效该源内容的 g 标签，重读当前关系集合 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `FormChangedEvent` | 失效 f 标签及包含该表单的交付响应，按当前状态读取定义 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `TranslationCompletedEvent` | 仅资源翻译按框架 4.6.11 标签映射失效；内容翻译只改草稿，等待 ContentPublishedEvent | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 读取当前状态/指纹；只有匹配的当前结果才更新；不以 occurredAt 单独当全序 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |
| `ContentPurgedEvent` | 使用事件 ID 清除内容及引用响应，不能因查不到内容而跳过 | 本模块事务内的幂等标记与数据变更一起提交；缓存重复失效安全 | 清理使用事件中的 ID；查不到父对象仍须清理 | 失败抛出保留未完成投递；业务副作用转入已有任务/队列后重试 |

监听器按框架 5.8 建立站点/system 上下文；`ContentPurgedEvent` 在模块停用时仍必须清理（deliverWhenModuleDisabled）。其他停用期间跳过并完成的事件，重新启用后通过扫描当前状态补偿，不能假定还会重投。数据库幂等标记与本模块数据在同一事务提交；外部发送、下载、搜索与 CDN 调用不得被数据库长事务包裹，使用已声明的任务/端口和业务幂等键。事件 payload 仅引用框架现有字段，本文不扩展签名。

SiteResourceChangedEvent（框架4.10.2，Proposed）：订阅：article.author、tracking.setting/script清settings，url.pattern清整站。字段meta/resourceType/resourceId/change，原事务登记，重复处理安全。

### 2.7 使用的端口

| 端口（平台框架小节） | 用途 | 测试替身 |
|---|---|---|
| `TaggedCache`（4.7.14） | 第 5 章外部能力；只通过端口调用基础设施 | `InMemoryTaggedCache` |
| `CdnPurger`（4.7.7） | 第 5 章外部能力；只通过端口调用基础设施 | `RecordingCdnPurger` |
| `DistributedLock`（4.7.16） | 第 5 章外部能力；只通过端口调用基础设施 | `InMemoryDistributedLock` |
| `BufferStore`（4.7.17） | 第 5 章外部能力；只通过端口调用基础设施 | `InMemoryBufferStore` |

## 3. 领域模型

### 3.1 聚合、实体与值对象

| 类型 | 对象 | 职责 |
|---|---|---|
| 值对象 | RouteResolution | UrlApi 返回的 Content / Redirect / Gone / NotFound，不是持久化实体 |
| 值对象 | DeliveryContext / DeliveryOutput | 框架 4.5.7：当前发布或预览快照、当前语言、引用解析器和贡献方输出 |
| 值对象 | DeliveryResult | HTTP 响应内部模型：kind、route、content、refs、dataSources、breadcrumbs、cache 及贡献方 section |
| 值对象 | CacheEntry / CacheTag | 可重建缓存值与依赖标签；不是内容事实来源 |
| 值对象 | ReferenceSet / RefResolver | 批量解析快照及贡献方的对象化引用并收集 touched 标签 |

### 3.2 不变量

| # | 不变量 | 保证方式 |
|---|---|---|
| 1 | LIVE 只返回当前语言已发布快照；语言未公开或模块停用的路由不可访问 | SiteApi + ContentApi；不存在与依赖失败分开 |
| 2 | PREVIEW/EDITOR 不读写公共缓存，token 必须匹配站点和用途 | 先校验令牌，再 assemblePreview；no-store 与 noindex |
| 3 | 每个贡献方只能写自己的唯一 sectionKey，不能覆盖保留字段 | 启动时检查 key，输出容器隔离后合并 |
| 4 | 数据源上限、参数、并行度与 500ms 超时遵守 DataSourceLimits | 统一调度；失败区域为空并记录问题 |
| 5 | 缓存身份必须包含站点、语言、模式、路径及会影响输出的规范化查询参数 | 固定序列化后生成摘要；预览不进缓存 |
| 6 | 缓存标签由 CacheTag 工厂创建，最多 200 个；超量卡片标签合并为 list:type | 合并时保留主体及资源依赖 |
| 7 | 数据库/核心贡献方失败返回 503，不伪装成 NOT_FOUND | 统一异常映射；只用尚未过期缓存，否则503，禁用stale扩展 |

### 3.3 状态机

不适用：无持久化业务状态机。DeliveryMode 为 LIVE / PREVIEW / EDITOR；RouteResolution 的各结果是请求解析结果，不是状态转换。缓存从未命中到写入、到失效可重复执行，不能反向改变内容发布状态。

## 4. 数据模型

> 遵守平台框架设计第 7 章的数据库通用约定。

### 4.1 表清单

不适用：本模块没有数据库表。事实数据通过 ContentApi、UrlApi、MediaApi、TaxonomyApi 与既有扩展点获取，临时交付结果及标签索引保存在 Redis。

### 4.2 ER 图

不适用：本模块没有数据库表。

### 4.3 表结构

不适用：没有 SQL 表。使用框架 4.7.14 的 TaggedCache，不自行实现缓存协议。

| 用途 | namespace / key | 内容与期限 |
|---|---|---|
| 内容交付 | `delivery.resolve` / 规范化语言、路径、查询参数的摘要 | 当前修订、模板、refs、数据源及贡献输出；仅 LIVE；TTL 不超过各贡献方 capMaxAge 的最小值 |
| 站点引导 | `delivery.site` / 语言与主题相关版本标识 | site、navigation、tracking 的公开配置；不混入访客身份、地区或个人同意 |
| 标签索引 | 由 TaggedCache 管理 | `s{siteId}:tag:{标签值}` → 缓存键集合，TTL 不短于最长条目 |

实际缓存键由端口自动加 `s{siteId}:`，格式 `s{siteId}:{namespace}:{key}`；业务方不能再次加站点前缀。CacheTag 已包含站点前缀，通过工厂生成。Nuxt引导/SSR/CDN共享最早依赖freshUntil，剩余寿命至多55秒，不在命中时重计；404/重定向/robots同规则，无可信期限no-store，5xx/预览/私有授权响应no-store。Redis 不可用时回源，但失效失败必须保留重试。

### 4.4 枚举

| 字段 | 枚举 | 语义 |
|---|---|---|
| DeliveryMode | LIVE / PREVIEW / EDITOR | 当前发布 / 令牌预览 / 编辑器画布 |
| kind | CONTENT / REDIRECT / GONE / NOT_FOUND | SSR 页面 / 跳转 / 410 / 404 |
| AssemblyPurpose | 按框架 4.5.3 | 只将预览快照交给贡献方，不把 EDITOR 临时数据持久化 |

### 4.5 站点过滤

缓存命名空间通过 TaggedCache 的当前站点自动隔离，CacheTag 站点必须匹配上下文；同一路径在 A/B 站不共用缓存。公开语言取 SiteApi 校验，预览站点来自令牌且须与可信 Host 一致。访客地区和同意不放进共享站点引导缓存。

### 4.6 跨模块引用与清理

| 引用与标签 | 失效事件 | 处理 |
|---|---|---|
| 主体、卡片、全局区块：content 标签 | ContentPublished/Unpublished/Trashed/Restored/PurgedEvent、LocalizationCreated/DeletedEvent | 失效对应内容及 list:type；重新解析只读当前发布态 |
| 路由与重定向 | ContentPathChangedEvent、RedirectChangedEvent | 清理相关路径解析及交付缓存，避免保留旧跳转 |
| 分类、媒体、导航、关系、表单 | TermChangedEvent、MediaUploaded/Updated/DeletedEvent、NavigationChangedEvent、RelationChangedEvent、FormChangedEvent | 分别失效 term/media/nav/graph/form 工厂标签 |
| 站点设置、模块与翻译 | SiteSettingsChangedEvent、ModuleEnabled/DisabledEvent、TranslationCompletedEvent | 设置或站点级宽失效；按完成的目标内容/资源清理标签 |

所有事件名称以 2.6 中列出的完整类名为准。失效先 Redis 后 CDN；失败不能将事件标为处理完成。缓存中的 ID 不加数据库外键，清理后可完全重建。

### 4.7 迁移脚本

不适用：本模块没有数据库迁移脚本。

## 5. 业务流程

### 5.1 核心业务流程

| 步骤 | 事务及行为 | 失败处理 |
|---|---|---|
| 1 | 从可信 Host 解析站点与公开语言，UrlApi.resolve 得到内容、重定向、GONE 或 NOT_FOUND；CONTENT 再读取 ContentApi.findPublishedSnapshot；只有不存在才返回 404，依赖失败返回 503。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |
| 2 | 选择 TemplateProvider，收集 BreadcrumbProvider 与 DataSourceProvider，再并行调用 DeliveryContributor；每个贡献方使用独立 DeliveryOutput，汇总引用后批量解析 refs，最后合并标签并缓存。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |
| 3 | 预览先 resolvePreviewToken，核对站点、用途、有效期与权限；PREVIEW/EDITOR 使用预览快照，全链路 no-store。事件提交后先清 Redis，再直接按标签清 CDN；二者成功才完成持久投递，失败重试。 | 输入或状态非法返回 4xx；数据库事务回滚；提交后外部处理失败进入重试与可见失败记录 |

后台读写先校验权限与站点/语言范围；草稿写入和审计在同一事务。远程操作不能使用数据库长事务包裹。幂等重放返回既有结果；参数不同却复用同一幂等键时拒绝。

### 5.2 一致性、失败与恢复约束

| 场景 | 必须满足的行为 | 可验证结果 |
|---|---|---|
| 路由结果与异常状态 | 先判断可信站点、模块和公开语言，LIVE 只读发布快照。只有明确不存在返回 NOT_FOUND/404；核心依赖不可用返回 503+Retry-After，不能把异常降级成被索引删除的 404。 | 依次注入无路由、GONE、301、数据库不可用，SSR 分别保持 404/410/301/503，503 不进入公共缓存。 |
| 缓存键与标签完整性 | 缓存键包含所有影响输出的规范化查询/语言/主题因素；标签覆盖主体、卡片、分类、媒体、全局区块、表单及导航。内容已发布后的更新也失效列表排序/筛选，不限 firstPublish。 | 修改已有产品排序/主分类后 60s 内所有关联列表变化；A/B 站同路径及不同筛选不混缓存；标签不超过 200。 |
| 回源与失效竞争 | getOrLoad 回源期间发生内容/依赖失效时，旧计算不得在失效后覆盖新缓存；需验证 TaggedCache/适配器与版本防护的具体协议，见 DELIVERY-02。 | 阻塞旧回源、发布并失效、释放旧回源，缓存和页面在 60s 预算内保持最新结果；Redis/CDN 失败可重试且有指标。 |
| 公开头部与个体数据 | bootstrap 的共享输出不含地区/同意/联系人；预览、编辑器 no-store/noindex。每源最多 24 项/500ms，每页最多 10 个源，局部可选贡献失败只清空该部分，核心 SEO/路由语义不能伪造。 | 两个不同同意访客命中同份 HTML，不含对方状态；预览请求后 LIVE 缓存仍为发布版本；单源超时不使整页变 404。 |

### 5.3 实现前统一方案与接入要求

依据[平台框架 4.10.2、4.10.3](../framework/platform-framework.md)及[设计问题处理说明](../reviews/design-issue-resolution-2026-10-20.md)。新增契约相关ADR仍为Proposed；下列方案已具体化，待评审冻结后同批实现，不能据此声明运行问题已验证。涉及本节的旧异步引用、原地索引写入或待定措辞按此方案替代。

- getOrLoad回源前取得缓存代次，回填用同一原子操作校验；旧代次结果不写缓存。直接put回源结果改用beginLoad/putIfUnchanged。
- 监听器只有Redis失效与CDN purge均成功才返回；失败依靠Event Publication Registry重投，V1可靠路径不依赖BufferStore去抖。
- SiteResourceChangedEvent(article.author/tracking.setting/tracking.script)清settings；内容更新始终清list(type)，不只首次发布。
- 公开缓存按统一绝对过期时刻计龄，各层不重置TTL；故障、积压和失效失败可观察，不能以stale缓存突破预算。
- Redis恢复门闩覆盖命中、capture与回填；旧RDB中的ready不能恢复可用状态，过期恢复者不得开闸，细节统一见框架4.10.3。[M0缓存复审](../reviews/m0-cache-protocol-review-2026-10-20.md)仅验证协议，正式服务仍须接入恢复协调器和真实CDN。

## 6. 接口

### 6.1 Admin 端点

| 方法 | 路径（相对 /api/admin/v1/delivery） | 说明 | 权限码 | 并发控制 / 幂等 | 审计 |
|---|---|---|---|---|---|
| GET | `/data-sources` | 后台读取已启用数据源的 key、label 与 paramsSchema | `delivery:definition:view` | 只读 | 否 |

### 6.2 Public 端点

GET `/api/public/v1/delivery/resolve?path=`、`/site?locale=`、`/preview?token=`。resolve 返回 CONTENT / REDIRECT / GONE / NOT_FOUND 及对应数据；重定向与错误由 Nuxt 保持原 HTTP 语义。LIVE 检查语言公开状态；PREVIEW/EDITOR no-store、noindex,nofollow。请求须遵守框架可信 Host 与 Internal Token 校验，不接受外部伪造站点头。

POST `/api/public/v1/delivery/preview` 为框架 8.3.13 要求的编辑器画布解析入口：JSON 体含 token 与 partOverrides，仅接受 EDITOR 用途令牌及 layout 白名单覆盖；不接受 GET 查询参数传任意 JSON，不持久化。限制请求大小不超过内容快照 1MB 上限，响应 no-store；复用 ContentApi.resolvePreviewToken/assemblePreview，不增加跨模块契约。GET preview 不支持临时覆盖。

### 6.3 Internal 端点

不适用：不新增 internal HTTP 端点；跨模块使用进程内 platform-api 契约。Nuxt 所需公共读取使用 6.2 的既有入口及可信代理认证。

### 6.4 权限码

| 权限码 | 名称 | 数据范围 | 高危 | 语言范围 |
|---|---|---|---|---|
| `delivery:definition:view` | 后台读取已启用数据源的 key、label 与 paramsSc | 当前站点 | 否 | 否 |

### 6.5 错误码

| 错误码 | HTTP 状态 | 后台中文信息 | 前台文案键 |
|---|---|---|---|
| `DELIVERY_VALIDATION_FAILED` | 422 | 输入不符合本模块规则 | errors.validation |
| `DELIVERY_NOT_FOUND` | 404 | 请求的资源不存在 | errors.notFound |
| `CORE_MODULE_DISABLED` | 404 | 站点已停用该模块 | errors.moduleDisabled |
| `DELIVERY_VERSION_CONFLICT`（模块内部错误码） | 409 / 412 | 资源已被修改，请刷新后重试 | — |
| `DELIVERY_UNAVAILABLE` | 503 | 页面数据服务暂时不可用 | delivery.errors.unavailable |
| `DELIVERY_PREVIEW_INVALID` | 404 | 预览令牌无效或已过期 | delivery.errors.previewInvalid |

跨模块错误使用框架已声明常量；本模块专用 HTTP 校验错误保存在本模块错误目录，不增加 platform-api 常量。公开接口不直接展示 detail；错误文案键统一登记在 UiTextApi。

**重点业务失败**：DELIVERY_VALIDATION_FAILED、DELIVERY_UNAVAILABLE（本模块内部）；HTTP 503 与 Retry-After。模块内部错误不扩展 platform-api；HTTP 状态按本节与框架错误常量映射。

本模块内部错误补充：`DELIVERY_VERSION_CONFLICT` 默认 409，经 If-Match 提交为 412；幂等键复用但规范化业务请求不同为 `DELIVERY_IDEMPOTENCY_CONFLICT`（409）；状态操作不适用为 `DELIVERY_STATE_INVALID`（409）。只有实际存在相应端点时才注册/使用，均不新增跨模块错误常量。跨站点或数据范围外 ID 返回不泄露对象存在性的结果，日志保留 traceId 和授权判定。

以上公开 HTTP 错误属于模块内部错误映射，不扩展 platform-api；跨模块透传同语义错误时保持既有错误码。

## 7. 后台界面

### 7.1 菜单与路由

无独立后台菜单；数据源目录供 page 编辑器通过已生成 api-client 读取。

### 7.2 页面

无独立管理页面；组件参数选择器使用 GET /api/admin/v1/delivery/data-sources 展示已装配的数据源，不能伪造不存在的 provider key。

### 7.3 注册到内容编辑外壳的标签

不适用：不向内容编辑外壳注册业务标签；公共生命周期控件由 content 外壳负责。

### 7.4 界面文案

| texts.ts 分组 | 中文文案 | 使用场景 |
|---|---|---|
| delivery.title | 内容交付 | 页面标题 |
| delivery.save | 保存成功 | 保存反馈 |
| delivery.conflict | 内容已被修改，请刷新后重试 | 并发冲突 |
| delivery.confirm | 确认执行此操作？ | 有副作用操作确认 |

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

不适用：不提供 DataSourceProvider；使用发布快照或既有贡献接口。

### 8.5 文案键

公开错误由前台共享错误组件按下表映射，不直接展示服务器 detail。

| 文案键 | 默认中文文案 | 用途 |
|---|---|---|
| delivery.errors.unavailable | 页面数据服务暂时不可用 | 公开错误提示 |
| delivery.errors.previewInvalid | 预览令牌无效或已过期 | 公开错误提示 |

## 9. 配置项

### 9.1 站点设置（settings.schema.json）

不新增 sys_setting 键；第 4 章业务配置表、模板或布局字段沿用各自定义及默认值，不能重复存放同一配置。没有独立站点设置 Schema 时，描述符 settings 为 null。

### 9.2 部署配置（application.yml）

本模块不另设连接地址或凭据前缀；数据库、Redis、对象存储及远程服务配置采用平台框架 5.12、5.14 的 `b2bweb.infra.*` 和环境变量。第 2.7 节列出的端口由 application 装配。部署地址、TLS 与密钥不写入仓库或响应。

执行批量任务时使用第 10 章的处理量与取消点；这些是初始运行预算，不能覆盖接口安全上限。任何新增可调参数先在本节明确类型、默认值及边界后再实现。

## 10. 定时与异步任务

| 任务 | 类型 | 触发 | 处理粒度与批量 | 锁与幂等 | 停用行为 |
|---|---|---|---|---|---|
| 缓存可靠失效 | 持久事件监听及失败重投 | 源事务提交后 | 按服务商上限拆批；Redis后直接CdnPurger；不经可丢失BufferStore缓冲 | Event Publication Registry；两者成功才完成；重复清除安全，不注册delivery.flush_cache任务 | 不可停用 |

普通任务为 PER_SITE；全局引擎或跨站点认领仅在 4.5 允许的位置执行。每批完成后更新进度与心跳，检查取消；短事务不得包裹外部调用。单次工作预算 60 秒，到预算即保存游标并续跑，外部端口超时用框架 4.7 的各端口上限。可重试失败指数退避；永久失败写 sys_job 报告或领域任务状态并输出 `b2bweb.delivery.task.errors` 指标，不记录敏感载荷。

上述标识为本模块任务/锁的稳定名称；定时任务仅该任务互斥，不用一个 delivery.task 串行阻塞全部维护。core 已负责站点命名空间，不重复手工添加 s{siteId}。sys_job 进度写入后在安全批次边界响应取消；任务超时/租约丢失须停止提交新结果。

## 11. 测试要点

| 层级 | 覆盖内容 | 必测用例 |
|---|---|---|
| 单元 | 第 3.2 全部不变量、3.3 状态与纯计算 | 逐条构造非法输入；合法及非法转换均断言；业务用例见 13.2 |
| 模块集成（Testcontainers + 替身） | 第 6.1 全部端点、唯一约束和版本控制 | 无本模块 SQL 迁移；验证真实 Redis/Meilisearch 交互、依赖故障及过期结果处理 |
| 契约（提供方） | 不适用：不提供服务接口 | 扩展点实现运行框架 4.8.5 中对应 Conformance 套件 |
| 事件（Scenario API） | 2.6 的发布与订阅 | 事务回滚无事件；重复投递不重复副作用；新修订后再投旧事件不回退 |
| 停用行为 | 系统设计 3.8 | 不适用：不可停用；验证停用其可选依赖后仍能降级 |
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
| InMemoryTaggedCache | TaggedCache | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| RecordingCdnPurger | CdnPurger | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| InMemoryDistributedLock | DistributedLock | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| InMemoryBufferStore | BufferStore | 替代远程服务或共享基础设施 | 成功、超时、临时错误、永久拒绝与重复请求 |
| SampleTemplateProvider | TemplateProvider | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
| SampleDataSourceProvider | DataSourceProvider | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
| SampleDeliveryContributor | DeliveryContributor | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
| SampleSiteBootstrapContributor | SiteBootstrapContributor | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
| SampleBreadcrumbProvider | BreadcrumbProvider | 验证收集过滤及缺省行为 | sample 类型、key、空输出与抛错实现 |
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

当前仅文档，尚无可运行的 `delivery` 工程。M0 完成后，在仓库根目录按平台框架第 11 章启动 MySQL 8.4、Redis 及本模块 2.7 需要的本地服务；通过 application 的 dev/test profile 装配本模块及 TestPlatform，不能直接依赖其他业务模块的实现。

后端执行框架规定的 Maven 模块集成/契约测试，前端执行 pnpm lint、typecheck、test；演示环境再用 dev 装配执行第 13 章 E2E。具体命令以 M0 实际建立的 wrapper、脚本和锁文件为准，本文不虚构当前可运行命令。

### 12.4 演示数据

计划在框架规定的 `tools/demo-data` 中提供 `delivery` 的演示数据（该目录尚未建立）：两站相同业务 key、中文源内容、已发布英语与未公开语言、一个草稿/缺失语言对象、一个越权用户和一个故障服务场景。模块特有数据按 13.2 的四个输入构造；不得使用真实联系人或凭据。

加载器只经已授权后台接口录入中文源数据，不直接写数据库；目标语言通过翻译流程及假引擎生成。按自然键重复加载幂等，仅补缺；不实际建立此目录直到 M0 任务要求实现。

## 13. 验收标准

### 13.1 里程碑切片

| 里程碑 | 交付功能 | 验收场景 |
|---|---|---|
| M1 | `resolve`、站点引导数据、预览 | S1、S2、S4（不含翻译/GA4） |
| M2 | Cache Tag + CDN 清除 | S3、S10 |
| M3 | 多语言（hreflang、未公开语言 404）；翻译完成后的失效 | S5、S7–S9、S11–S13；S4 的同意/分配增强 |
| M4 | — | — |

切片逐字沿用平台框架 13.5；“—”表示没有该阶段集成要求，不表示不能提前实现。

### 13.2 验收项

| # | 输入与期望结果 | 场景 | 验证方式（计划用例名） | 里程碑 |
|---|---|---|---|---|
| A1 | resolve 返回可供 SSR 的内容、refs、数据源和 SEO，不读业务草稿 | S1 | 集成：`DeliveryA1IT`；涉及界面时 E2E：`delivery-A1` | M1 |
| A2 | 单个数据源 500ms 超时降级为空，页面其他区域仍输出 | S3 | 集成：`DeliveryA2IT`；涉及界面时 E2E：`delivery-A2` | M2 |
| A3 | 预览与编辑器无公共缓存、无索引，跨站令牌被拒绝 | S3（画布）；M1 普通预览另按 A1 验证 | 集成：`DeliveryA3IT`；涉及界面时 E2E：`delivery-A3` | M2 |
| A4 | 发布后 60 秒内引用页缓存失效，CDN 失败可观测并重试，依赖失败返回 503 | S2 | 集成：`DeliveryA4IT`；涉及界面时 E2E：`delivery-A4` | M2 |
| A5 | ArchUnit 与 Modulith verify 通过；零业务模块编译依赖、跨表 JOIN/外键 | 模块自身 | 架构：`DeliveryArchitectureTest` | M1 |
| A6 | 第 6.1 当前切片已交付后台端点覆盖 A/B 站隔离、权限拒绝；全部成功写操作有脱敏审计 | 模块自身 | 集成：`DeliveryIsolationIT`、`DeliveryAuditIT` | M1 |
| A7 | 自身停用被拒绝，可选依赖缺失不阻塞基础功能 | 模块自身 | 集成：`DeliveryModuleLifecycleIT` | M2 |
| A8 | 不适用：无翻译单元；验证公开读取不跨语言回退 | S11、S12 | 集成：`DeliveryLocaleIT` | M3 |
| A9 | 在系统设计 1.5 的前台 200 RPS（CDN 未命中）基线，Delivery P95 缓存命中≤30ms、未命中≤300ms，发布至缓存可见≤60s；故障用例另报不混入正常服务成功样本 | 模块自身 | 性能报告：`DeliveryPerformanceIT`；架构负责人核对适用指标 | M1 |
| A10 | 实现与本文件、描述符、OpenAPI、Schema 和迁移保持一致；第 14 章阻塞项在开工前关闭 | 模块自身 | 人工：模块负责人逐项按框架 12.4 检查并附评审记录 | M1 |
| A11 | 依次注入无路由、GONE、301、数据库不可用，SSR 分别保持 404/410/301/503，503 不进入公共缓存。 | 模块自身 / 5.2 | 集成：`DeliveryA11IT`；涉及界面 E2E：`delivery-A11` | M1 |
| A12 | 修改已有产品排序/主分类后 60s 内所有关联列表变化；A/B 站同路径及不同筛选不混缓存；标签不超过 200。 | 模块自身 / 5.2 | 集成：`DeliveryA12IT`；涉及界面 E2E：`delivery-A12` | M2 |
| A13 | 阻塞旧回源、发布并失效、释放旧回源，缓存和页面在 60s 预算内保持最新结果；Redis/CDN 失败可重试且有指标。 | 模块自身 / 5.2 | 集成：`DeliveryA13IT`；涉及界面 E2E：`delivery-A13` | M2 |
| A14 | 两个不同同意访客命中同份 HTML，不含对方状态；预览请求后 LIVE 缓存仍为发布版本；单源超时不使整页变 404。 | 模块自身 / 5.2 | 集成：`DeliveryA14IT`；涉及界面 E2E：`delivery-A14` | M3 |

用例名为后续实现要求，不表示已有测试。性能测试需记录数据规模、并发、冷/热缓存与结果分位数；不以单次请求代替验收。

## 14. 待定问题

| # | 问题 | 影响 | 是否阻塞开发 | 负责人 | 最晚解决 | 结论/处理要求 |
|---|---|---|---|---|---|---|
| DELIVERY-01 | CacheTag.form 与表单标签已在框架定义，系统设计 23.2 仍待同步 | 对应契约或缓存行为 | 否 | 框架负责人 / 本模块负责人 | 相关切片开工前 | 使用框架既有工厂方法，不手工发明新标签；由框架文档同步任务跟进。 |
| DELIVERY-M0 | 工程骨架、冻结契约与 testkit 尚未建立 | 所有运行和测试用例 | 是 | 框架负责人 | M0 完成 | 按 ADR-018 与框架 13.1 交付后才满足模块开工前提；文档补齐不等于 DoR 已通过。 |
| DELIVERY-02 | TaggedCache.invalidate 与 in-flight getOrLoad 回填竞态，以及 BufferStore 临时合并丢失后的失效重试未形成闭环 | M2 的60秒新鲜度保证 | 方案已补齐，待评审冻结 | 框架负责人 / 模块负责人 | M2 缓存失效开工前 | 统一方案：站点缓存代次保护回填，Redis/CDN均成功才完成持久投递；ADR-020。详见框架4.10.3及本模块第5章；保留原编号追踪评审与实施，不宣称已实现。 |

## 修订记录

| 版本 | 日期 | 变更 |
|---|---|---|
| V1.0 | 2026-10-14 | 初稿 |
| V1.1 | 2026-10-20 | 依据现有系统设计、框架契约与数据模型补齐职责、描述符、流程、接口、界面、测试及验收；保留未解决的上层契约问题，未声称运行测试通过 |
| V1.2 | 2026-10-20 | 全量复审：统一资源聚合标识、并发/失败恢复、接口闭环与验收里程碑；补充模块专项约束和审查记录，保留既有 SQL |
| V1.3 | 2026-10-20 | 补齐实现前统一方案与事务/恢复接入要求；关联ADR待评审，未执行应用测试 |
| V1.4 | 2026-10-20 | 整改正式评审DR-01～04的字段/锁/失败与缓存语义，同步框架V1.3；运行及ADR状态单独跟踪 |
| V1.5 | 2026-10-20 | M0-07C反馈：移除旧缓冲清除流程/任务，明确恢复门闩、可靠重投和原型边界；依据框架V1.6 |
