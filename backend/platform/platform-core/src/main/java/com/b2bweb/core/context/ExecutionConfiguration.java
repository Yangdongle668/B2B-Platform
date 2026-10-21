package com.b2bweb.core.context;

import com.b2bweb.core.support.concurrent.PlatformExecutors;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class ExecutionConfiguration {
  @Bean(destroyMethod = "close")
  PlatformExecutors platformExecutors(PlatformTaskDecorator context) {
    return new PlatformExecutors(context);
  }

  @Bean(name = "applicationTaskExecutor")
  org.springframework.core.task.SimpleAsyncTaskExecutor applicationTaskExecutor(
      PlatformTaskDecorator context) {
    var executor = new org.springframework.core.task.SimpleAsyncTaskExecutor("platform-async-");
    executor.setVirtualThreads(true);
    executor.setTaskDecorator(context);
    executor.setTaskTerminationTimeout(30000);
    return executor;
  }

  @Bean
  com.b2bweb.core.support.validation.JsonSchemaValidator jsonSchemaValidator() {
    return new com.b2bweb.core.support.validation.JsonSchemaValidator();
  }
}
