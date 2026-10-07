# ADR-007：编辑器画布使用 iframe 嵌入 Nuxt 预览

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-06 |
| 决策者 | 业主、架构负责人 |
| 相关章节 | 0.2（第 12 项）、1.3（S3）、2.1、2.3、6.5、10.3、10.4、10.8、10.9、11.1、13.3、14.3、21.1、21.3、21.6、22.6、24.3、26.3、28、31 |
| 相关 ADR | ADR-004、ADR-006、ADR-010、ADR-017 |

## 背景

页面编辑器（ADR-006）要让运营不写代码搭建页面，并在三种设备尺寸下预览；只能使用设计系统内的组件与变体，草稿不影响线上（1.3 S3）。V1.0 没有设计画布如何渲染（0.2 第 12 项）。

约束与现状：

- 管理后台是 Vue 3 + Element Plus 的 SPA，前台组件实现在 Nuxt 主题中，两者无法直接复用组件（10.8）；后台也不使用前台的 Design System（11.1）。
- 前台与后台分开部署（2.1）：前台在 `www` 域名，后台与 Admin API 同源部署在 `admin` 域名（2.3、22.6）。
- 预览链路已经存在：Delivery 用与发布相同的 `PublishContributor.contribute()` 从草稿临时组装快照（不持久化），由 Nuxt 渲染（6.5）。
- “页面编辑器复杂度失控”是可能性与影响都为“高”的风险，对策之一是“跨 iframe 拖放改为大纲树排序”（31）。
- 编辑器路由不得被搜索引擎收录，也不得被任意站点嵌入（14.3、21.6）。

## 决策

1. **画布是一个 iframe，加载前台站点的 Nuxt 编辑器路由** `/__editor?token=…`（`app/pages/__editor.vue`，21.1）：
   - Nuxt 用**真实的主题、组件与数据**渲染；选中框、悬停框、插入点由 iframe 内的覆盖层绘制。
   - 组件面板（按模板插槽与模块启用状态过滤）、大纲树、属性面板（由 Schema 生成）在 Admin 中；设备切换（1440 / 768 / 390）改变画布宽度。

2. **双方通过 postMessage 通信，收发都校验 `origin`**（10.8）：

| 方向 | 消息 |
|---|---|
| Admin → 画布 | `editor:init {document, locale, device}`；`editor:patch {ops}`（JSON Patch 增量）；`editor:select {sectionId}`、`editor:scrollTo {sectionId}` |
| 画布 → Admin | `preview:ready`；`preview:select {sectionId}`、`preview:hover {sectionId}`；`preview:insert {parentId, slot, index}`；`preview:error {sectionId, message}` |

   - 编辑中的布局文档由 Admin 持有；画布渲染尚未保存的文档（6.5），每次修改以 JSON Patch 增量下发。
   - 画布中的数据源由 Nuxt 使用编辑器令牌直接调用 Delivery 预览接口解析（10.8）；草稿只经这条预览链路渲染，不另写一套组装逻辑。

3. **组件 Schema 只有一份**，放在共享包 `frontend/packages/component-schemas`，三方使用同一份 JSON（10.4）：

| 使用方 | 用途 |
|---|---|
| Admin | 按 `x-editor` 生成属性表单；校验 |
| Nuxt | 组件迁移与渲染 |
| 后端 | 保存时用 JSON Schema 校验；按 `x-translatable` 提取翻译单元；构建时从共享包复制到 classpath |

4. **交互取舍**：V1 不做跨 iframe 拖放，采用“大纲树拖拽排序 + 画布内插入点按钮 + 组件面板点击插入”。撤销 / 重做在 Admin 中保存 JSON Patch 历史（最多 100 步）；每 30 秒自动保存草稿（带乐观锁），本地同时保存一份；发布前在侧栏展示校验错误与 SEO 健康提示（10.8）。

5. **多语言**（V1.3）：页面编辑器编辑源语言（中文）与 `MANUAL` 模式的布局文档；`AUTO` 模式的目标语言在画布中只能预览，文字在翻译编辑器中按段修订，结构在源语言中修改后自动同步（10.3、10.8、ADR-017）。

6. **安全与索引控制**：
   - `/__editor` 只允许后台域名嵌入（CSP `frame-ancestors`）；其他前台路由为 `'self'`，后台为 `'none'`（21.6、26.3）。
   - 编辑器与预览响应带 `noindex, nofollow` 与 `Cache-Control: no-store`；robots.txt 默认 `Disallow: /__editor`（14.3、21.3）。
   - `__editor`、`__preview` 是 slug 保留字（13.3）。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 在 Admin 页面中直接渲染前台组件 | 不需要跨窗口通信 | Element Plus 与主题样式互相干扰；Admin 要再打包一份主题与组件；结果可能与 Nuxt SSR 的实际输出不一致 | 不采用 |
| 为编辑器单独实现一套“编辑态组件” | 编辑交互可以定制 | 两套实现，所见不等于所得 | 不采用：违反“不重复实现已有组件” |
| 只用表单编辑，另开窗口预览 | 实现最简单 | 不能在页面上直接选中、插入 Section，搭建效率低 | 不采用：难以满足 S3 的搭建体验 |
| iframe 画布 + 跨 iframe 拖放 | 交互最直观 | 实现复杂且不稳定 | 不采用：V1 改为大纲树排序（10.8、31） |
| iframe 嵌入 Nuxt 编辑器路由 + postMessage + 共享组件 Schema | 所见即所得；样式完全隔离；组件只实现一次 | 需要维护消息协议；画布依赖 Nuxt 与 Delivery 预览接口 | 采用 |

## 后果

### 正面

- 画布与线上使用同一套主题、组件、渲染器与数据源，保证“所见即所得”。
- 样式完全隔离：Admin 的 Element Plus 与前台的 Design Token 互不影响。
- 复用 6.5 的预览链路，草稿不会写入快照，也不会泄漏到线上（P4）。
- 组件 Schema 只有一份，Admin 表单、Nuxt 渲染、后端校验与翻译提取始终一致。
- Admin 与 Nuxt 仍然分开部署，只通过消息协议与 HTTP API 交互。

### 负面与代价

- 消息协议是 Admin 与 Nuxt 之间的契约，两边必须同步修改。
- 画布依赖前台 Nuxt 与 Delivery 预览接口可用；后台用户主要在中国大陆，访问欧盟源站延迟较高（22.6），画布的响应速度会受影响。
- 不支持跨 iframe 拖放，交互不如自由拖拽直观。
- 编辑期间画布中的数据源通过 Delivery 预览接口实时解析，会增加该接口的请求量。
- `frame-ancestors` 需要按路由区分配置：配置错误会导致画布无法加载，或编辑器路由被其他站点嵌入。

### 需要遵守的规则

| 规则 | 检查方式 |
|---|---|
| postMessage 收发双方都校验 `origin`，只处理协议表中的消息类型；新增或修改消息时 Admin 与 Nuxt 在同一个 PR 中修改 | PR 评审 |
| 组件 Schema 只在 `component-schemas` 中定义，Admin、Nuxt、后端不得各自复制或改写 | 契约测试中的组件 Schema 校验（28）；PR 评审 |
| 画布只通过 Delivery 预览接口获取草稿渲染数据，不新增绕过 `PublishContributor` 的草稿读取接口 | PR 评审；API 变更由 oasdiff 提示（24.3） |
| `/__editor` 的 `frame-ancestors` 只允许后台域名；编辑器与预览响应带 `noindex, nofollow` 与 `no-store` | 安全扫描 OWASP ZAP baseline（28）；PR 评审 |
| 页面编辑器不允许编辑 `AUTO` 模式目标语言的布局与文字 | E2E；PR 评审 |
| 场景 S3（三种设备尺寸预览、草稿不影响线上、可分享预览链接）纳入 E2E | Playwright E2E（28） |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-10-06 | 创建 |
