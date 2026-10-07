# ADR-002：模块通信：公开 API、领域事件、扩展点

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-06 |
| 决策者 | 业主、架构负责人 |
| 相关章节 | 0.2（第 3 项）、2.1（P1、P2、P3、P8）、3.1、3.3、3.5、3.7、4.2、4.3、25.5、29.2、29.3、29.4、附录 A |
| 相关 ADR | ADR-001、ADR-003、ADR-004、ADR-008、ADR-017 |

## 背景

- 所有后端模块运行在同一个进程中（ADR-001），模块之间直接调用、直接查表非常容易，边界会被逐步侵蚀，AI 生成的代码同样如此（31）。
- V1.0 中 Product 依赖 SEO，形成反向依赖（0.2 第 3 项）。
- 分层为 L0 platform-core、L1 platform-api、L2 基础能力、L3 业务、L4 聚合与横切（3.1）。下层经常需要上层的能力，例如 content（L2）发布时需要 product（L3）贡献快照、seo（L4）需要 product 提供 JSON-LD、translation（L2）需要各模块提取可翻译文本。
- 模块可以按站点停用（ADR-001），协作方式必须能自动适应“对方模块不存在”。

## 决策

模块之间**只允许三种交互方式**，所有跨模块契约（服务接口、扩展点、事件、DTO）**只放在 `platform-api`**：

| 方式 | 何时使用（P3） | 形式 | 例子 |
|---|---|---|---|
| 公开 API（同步调用） | A 需要 B 的数据或能力，且调用方向允许（R2） | `platform-api` 中的服务接口，由 B 实现 | 业务模块调用 `ContentApi.create(type, locale)` 获得 `content_id`（6.1） |
| 领域事件（异步通知） | “A 发生后 B 要做事” | `DomainEvent`，事务提交后投递（ADR-003） | `ContentPublishedEvent` → seo、graph、search、media、delivery、translation |
| 扩展点（能力注册） | “A 需要 B 的能力”，但 A 不能（或不应）依赖 B，尤其是下层需要上层的能力 | `platform-api` 中的接口；模块用 `@Extension` 声明实现，`ExtensionRegistry` 收集并**按当前站点的模块启用状态过滤** | product 实现 `SchemaOrgProvider`，由 seo 收集；各模块实现 `TranslatableContentProvider`，由 translation 调用 |

```java
@Extension(module = "product", contentTypes = {"PRODUCT"}, order = 100)
public class ProductSchemaOrgProvider implements SchemaOrgProvider { ... }

List<SchemaOrgProvider> providers = extensions.forContentType(SchemaOrgProvider.class, "PRODUCT");
```

依赖规则（3.3）：

| 规则 | 内容 |
|---|---|
| R1 编译依赖 | 只能依赖 `platform-core`、`platform-api` 与 infrastructure 定义的端口接口；不得依赖其他模块的 Maven 构件，不得 import 其他模块的包 |
| R2 调用方向 | L3 / L4 可以调用 L2 与 L0 的服务接口；L2 不得调用 L3 / L4（只能通过扩展点回调）；同为 L3 或同为 L4 的模块之间不直接调用，只通过事件或扩展点协作 |
| R3 契约位置 | 跨模块契约只在 `platform-api`；变更需要 CODEOWNERS 中的架构负责人审核 |
| R4 运行时依赖 | 在 `module.json` 的 `requires` 中声明，启动时校验；禁止循环依赖；可选依赖用 `optional` 并在缺失时降级 |
| R5 数据访问 | 只访问本模块前缀的表；不允许跨模块外键与跨模块 JOIN；跨模块引用只保存 ID，被引用对象删除时由事件（如 `ContentPurgedEvent`）驱动各模块清理（25.5） |

事件与 DTO 是契约：事件只携带 ID 与必要的事实；只允许兼容地增加字段，不兼容的变更必须提升 `schemaVersion` 并在过渡期内同时支持新旧版本（4.2）。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 允许模块依赖其他模块的构件，直接调用 | 编码直接 | 产生 V1.0 的 Product → SEO 反向依赖与循环依赖；对方模块停用时调用方出错；边界无法自动检查 | 不采用 |
| 只用事件，所有协作都异步 | 解耦最彻底 | 发布校验、快照组装、SEO 头信息等必须在同一请求或事务内同步完成（4.4），无法用事件表达 | 不采用 |
| 共享表，跨模块 JOIN | 查询方便 | 数据所有权不清；表结构变更波及多个模块；无法按模块停用与清理 | 不采用 |
| 三种方式 + 契约集中在 platform-api | 编译期互不可见；停用模块自动退出协作；契约集中评审 | 契约设计与评审成本 | 采用 |

## 后果

### 正面

- 模块之间在编译期互相不可见，“Product 依赖 SEO”这类问题从结构上被消除（3.3）。
- 扩展点注册表按站点启用状态过滤，停用的模块自动退出协作，调用方无需判断。
- 跨模块契约集中在一处，便于评审、文档生成与 Spring Modulith 验证。

### 负面与代价

- `platform-api` 成为瓶颈：每个新的跨模块需求都要先设计契约并经架构负责人审核。
- 不能跨模块 JOIN，列表与筛选需要依靠投影与读模型（`cnt_published_term`、Meilisearch，见 ADR-004、ADR-013），或分两步查询再组装。
- 间接调用增加了阅读代码的难度；V1 已有约 20 个扩展点（3.7），需要在《Module SPI 与扩展点详细设计》（附录 C）中维护清单与签名。

### 需要遵守的规则

| 规则 | 检查方式 |
|---|---|
| Core 不依赖业务模块；模块之间不互相 import | Maven 依赖（编译期隔离）+ ArchUnit + Spring Modulith `verify()`（29.3） |
| 模块只访问自己的表 | ArchUnit：Mapper 只能在本模块的包中使用；脚本扫描 SQL 中的表前缀（29.3） |
| 跨模块契约只在 `platform-api` | CODEOWNERS；platform-api、platform-core 的 PR 需要 2 人审核，其中包括架构负责人（29.4） |
| 前端模块之间不互相 import | ESLint `no-restricted-imports` / dependency-cruiser（29.3） |
| `module.json` 中声明的事件与代码一致 | CI 脚本（29.3） |
| 选择方式时遵循 P3：“A 发生后 B 要做事”用事件，“A 需要 B 的能力”用扩展点或公开 API | PR 评审 |
| 新增或变更跨模块契约先写 ADR；跨模块修改在 PR 中说明原因 | 29.5；附录 A |
| 新内容类型实现 29.2 第 3 步列出的全部扩展点 | 完成定义（29.6）；PR 评审 |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-10-06 | 创建 |
