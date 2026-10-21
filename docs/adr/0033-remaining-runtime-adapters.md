# ADR-033：补齐运行适配器与请求边界

状态：Proposed（实现候选，尚待架构评审）

日期：2026-10-21

依据框架4.7、5.7、5.13及评审R1–R5，补齐缺失端口、持久化支撑与执行门禁。不修改跨模块API及已有迁移。

安全下载和Webhook必须连接已经检查的DNS地址。JDK HttpClient没有公开的自定义DNS接口，因此这两个适配器候选使用Spring RestClient同族的Apache HttpClient 5传输，关闭自动重试、重定向和解压，绑定解析结果并保留原始Host/TLS主机验证。其余供应商HTTP使用JDK HttpClient。新增MaxMind本地数据库读取器，不提供虚假的国家推断；未配置数据库时不注册该端口。

Redis限流候选使用单次Lua原子令牌桶，替代框架中指定的Bucket4j。这样无需额外的Redis客户端/序列化层；容量与周期、故障策略保持4.7.15，生产时钟来自Redis TIME。此实现选择须评审采纳后同步框架规范；不能因候选实现存在而标记ADR已接受。

所有远程端口统一拒绝持有实际数据库事务时调用，并通过低基数端口/操作/结果指标记录。安全HTTP客户端限制超时、响应体与下载体大小；临时文件失败立即删除。MX查询失败为UNKNOWN，不缓存瞬时故障。

替身、HTTP/clamd协议夹具及真实Redis运行同一消费者语义。供应商凭据、真实GeoIP数据库及云服务端验收仍需部署环境提供，不能由本地协议夹具签收。

候选依赖：Apache HttpClient 5与OpenTelemetry starter由Spring Boot 4.0.8 BOM管理；MaxMind geoip2 4.4.0；Sentry Java 8.61.0；JaCoCo 0.8.14（检查domain包80%行覆盖率）。新增依赖仍经过安全扫描，不因版本可下载而认定无漏洞。
