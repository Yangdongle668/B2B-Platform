# M0 隔离验证

这是技术兼容性与设计DDL验证原型，不是业务应用骨架。候选版本与完整冻结条件见[ADR-011](../../../docs/adr/0011-technology-versions.md)，执行结果见[M0复审](../../../docs/reviews/m0-remediation-2026-10-20.md)。后续[M0事务协议复审](../../../docs/reviews/m0-transaction-protocol-review-2026-10-20.md)记录新增并发/故障验证。[路由协议复审](../../../docs/reviews/m0-route-protocol-review-2026-10-20.md)补充19项路由验证。[缓存协议复审](../../../docs/reviews/m0-cache-protocol-review-2026-10-20.md)补充37项缓存验证。前台只是SSR/图片provider/懒水合测试夹具，不是Tesla视觉稿。

## 环境与执行

需要Python 3.12+、本地Docker socket、Node 24.15+（本次24.19.0）。脚本固定Docker镜像digest、后端直接依赖与npm lockfile；Maven间接版本由Boot/Modulith BOM管理。后端原型不依赖宿主JDK/Maven。Maven仓库、npm registry和镜像仓库须在环境网络策略允许范围内。

在仓库根目录运行：

```bash
python -m pip install -r tools/verification/m0-compatibility/requirements.txt
python tools/verification/m0-compatibility/verify_design.py
python tools/verification/m0-compatibility/verify_backend.py
python tools/verification/m0-compatibility/verify_frontend.py
```

浏览器验证默认使用PATH中的`chromium`或Playwright自带Chromium，可用`CHROMIUM_PATH`指定可执行文件；缺少浏览器时运行`python -m playwright install chromium`（需网络策略允许）。前端脚本自行执行`npm ci`、类型检查、生产构建并启动临时loopback服务。

后端脚本直接连接本地Unix socket，忽略远程Docker选择变量，保留Docker凭据/网络代理配置。只创建带随机前缀的网络和容器，不映射MySQL/Redis主机端口；空密码仅用于此隔离临时数据库。脚本在finally清理自己的容器、匿名卷与网络，不触碰其他容器或数据库。Redis使用固定digest的7.4镜像（本次7.4.11），只为RDB恢复测试设置restart=always，SAVE/SHUTDOWN仅操作自己的临时实例，清理时rm -f -v停止并删除实例。遇到进程被强制杀死，可按控制台/`docker ps -a`中对应的随机前缀识别遗留资源后清理，不要批量清理Docker全局资源。

托管云环境有`CODEX_PROXY_CERT`时，Java在临时truststore中导入公开CA，前端使用`NODE_EXTRA_CA_CERTS`，保持TLS验证。Maven代理从容器配置读取，临时settings权限0600，结束自动移除；不要提交代理、凭据、CA或缓存。后端可用`--cache /tmp/你的缓存`与`--output /tmp/你的结果`复用下载缓存或指定报告位置；仅执行文档SQL可加`--schema-only`。后端项目只读挂载，编译和测试在容器临时目录执行。

## 实际检查与边界

| 检查 | 范围 |
|---|---|
| 设计静态检查 | 17模块JSON Schema、依赖DAG和L2方向、权限/组件唯一性、章节、文件链接、废弃规则、唯一契约存在性 |
| 文档SQL | 框架及模块的119个CREATE按外键顺序执行、3个ALTER追加执行，FK检查开启，核对新增列与策略归属；不等于已建立模块升级迁移 |
| 后端兼容7项测试 | 单库Flyway；MyBatis-Plus跨站读/写；事件提交/回滚；失败监听持久化与显式重投；Modulith verify；JDK虚拟线程 |
| 后端协议39项测试 | 真实文档表：媒体11、翻译12、下载16；受控双事务从performance_schema确认锁等待；本地文件端口及提交前故障注入；固定grant重放 |
| 路由协议19项测试 | 事务互斥初始化/死锁负对照、当前树和generation、拓扑检查点、语言去重、租约、路径冲突/批次回滚、同事务搜索日志；不含真实路由模式引擎 |
| 缓存协议37项测试 | 真实Redis代次/读写门闩/RDB恢复15项；真实Modulith持久事件+HTTP CDN故障端口8项；真实HTTP传输和可拨动时钟的freshUntil预算14项；不是正式CDN或Nuxt缓存集成 |
| 前端 | Nuxt类型检查和生产构建；无JS的SSR文本与图片URL；自定义图片provider；1440/768/390三个视口下可见前未水合、滚动后水合、两次点击；浏览器错误与水合警告 |

输出在忽略的`.results/`中。失败会返回非零退出码，不用旧报告推断成功。后端日志包含故意制造的监听/CDN失败与Redis重启重连告警，以及预期的INSERT IGNORE死锁负对照；冷建行诊断输出ROUTE_COLD_INIT_LOCKS只含夹具锁信息。以退出码与XML测试报告为准。

八个后端套件合计102项（此前65项+缓存37项）。`ProtocolModel`及其适配器全在src/test目录，是协议可执行模型，未成为生产业务服务。事务库由脚本创建，`PROBE_PROTOCOL_JDBC_URL`只能选择专用mysql:3306/remediation；不要手动指向现有数据库。源码只读挂载，旧报告不作为新执行成功证据。

没有验证：正式媒体/翻译/下载/路由/缓存业务实现、生产恢复协调器与真实CDN部署、Redis Cluster/ACL/容量、节点时钟偏差、真实imgproxy签名、搜索容量与源锁、Security/Session、HTTP适配器、独立模块Flyway history及升级、Scenario API、正式Monorepo/CI与完整视觉稿。通过本原型不能直接将ADR改为Accepted、冻结platform-api 1.0.0或跳过G0。
