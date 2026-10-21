package com.b2bweb.testkit;
import com.b2bweb.testkit.contract.DomainEventContract;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
class DomainEventsTest extends DomainEventContract {
 protected JsonMapper json(){var module=new SimpleModule("decimal-long-ids");module.addSerializer(Long.class,ToStringSerializer.instance);module.addSerializer(Long.TYPE,ToStringSerializer.instance);return JsonMapper.builder().addModule(module).build();}
}
