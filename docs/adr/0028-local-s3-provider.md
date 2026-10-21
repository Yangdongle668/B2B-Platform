# ADR-028：M0本地S3兼容服务候选

| 项 | 内容 |
|---|---|
| 状态 | Proposed（待评审） |
| 日期 | 2026-10-20 |
| 决策者 | 架构负责人（待签审） |

## 背景与候选决策

MinIO官方Docker Hub及Quay固定版本在当前环境返回访问拒绝，官方二进制下载域名尚未在运行环境允许列表中；从固定官方源代码构建也受依赖下载限制。因此不能记录MinIO冒烟已通过。

M0本地可复现环境暂用adobe/s3mock:4.11.0，固定镜像摘要cd49108c0094bc3f420b24bff354073032a9ec1a6f4cc89f8e8f483a308cf99e；生产端口仍为ObjectStorage，实现仍使用AWS SDK v2。S3Mock是HTTP S3兼容测试服务，不是内存ObjectStorage替身，也不代表真实云厂商或MinIO的完整兼容性。

已验证桶隔离、对象长度校验、读写/元数据/幂等删除、私有预签名下载及Content-Disposition；未证明S3Mock校验签名、生产IAM、公开桶策略、Multipart、CDN缓存或MinIO特有行为。默认不暴露公网端口，开发栈仅绑定回环地址。MinIO补测在环境下载权限就绪后执行，报告须保留实际provider名称。G0第4项采用此候选的决定仍需评审，不自动改变ADR状态。

## 图片代理链验证配置（2026-10-21）

本地与验证栈增加框架既定的imgproxy，候选版本v3.30.1，固定摘要`sha256:3b709e4a0e5e8e0e959b556b7031229202b4b8e7e7d955c517ea7abed68ee34d`。只开放回环端口；随机生成HMAC key/salt，不接受未签名请求；`IMGPROXY_BASE_URL=s3://public-assets/`固定公开桶，不能由请求指定私有桶或任意URL。服务从内部storage:9090读取公开对象，不开放远程HTTP源。自动集成测试必须验证真实输出格式/尺寸、FIT/FILL、伪造签名拒绝和私有对象不可读；这不替代生产IAM或MinIO兼容性验收。
