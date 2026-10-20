package com.b2bweb.application;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
  @Bean
  JsonMapperBuilderCustomizer decimalLongs() {
    var schema = new io.swagger.v3.oas.models.media.StringSchema().pattern("^-?[0-9]+$");
    org.springdoc.core.utils.SpringDocUtils.getConfig()
        .replaceWithSchema(Long.class, schema)
        .replaceWithSchema(Long.TYPE, schema);
    return builder -> {
      var module = new SimpleModule("decimal-long-ids");
      module.addSerializer(Long.class, ToStringSerializer.instance);
      module.addSerializer(Long.TYPE, ToStringSerializer.instance);
      builder.addModule(module);
    };
  }
}
