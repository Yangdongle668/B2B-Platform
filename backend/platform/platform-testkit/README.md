# 契约测试使用方式

`ContractSuites.all()`登记25个服务和17个端口的可复用行为套件。套件仅调用platform-api，提供方测试继承套件并实现受保护的API/夹具控制方法；数据库提供方使用真实事务管理器，内存替身用`ContractTransactions`。不要在真实提供方测试中把`api()`返回成替身。

`SharedServicesTest`、`SharedPortsTest`及原有专用测试执行替身套件；正式提供方的示例见application的`CoreSharedContractsTest`、`PermissionsIntegrationTest`、`JobsIntegrationTest`和infrastructure的`StorageRealContractTest`、`SearchRealContractTest`、`ImagesRealContractTest`、`TranslationRealContractTest`。`ContractCatalogTest`检查唯一目录与套件/具体测试继承关系；每个套件必须有实际JUnit行为场景。该门禁证明基础套件存在且运行，不把它等同为全部方法/异常/并发语义已签收。

`EventSamples.all()`显式注册框架4.2.4的34个事件，不能用反射自动生成新事件的样例。`DomainEventContract`扫描全部事件并验证record/元数据/不可变集合/个人数据字段启发式，分别用testkit与真实应用Jackson配置往返序列化，检查字符串ID和schemaVersion。新增事件须先修改唯一契约，然后补样例及消费者。

`conformance/`现有内容翻译、资源翻译及搜索投影的消费者行为套件：翻译保持业务字段和引用，资源游标隔离站点，搜索只用发布快照。其他SPI的完整消费者套件、服务全部方法的语义矩阵和尚无生产提供方的配对测试仍属于G0未完成项；不能用示例类型存在或总测试数代替。
