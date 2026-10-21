package com.b2bweb.core.observability;
import com.b2bweb.core.support.LeasedTsidIds;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.*;
import org.springframework.context.annotation.*;
/** Low cardinality runtime metrics and a fail-closed readiness contribution. */
@Configuration(proxyBeanMethods=false)
public class RuntimeObservability {
 @Bean HealthIndicator tsidLeaseHealthIndicator(LeasedTsidIds ids){return ()->ids.available()?Health.up().build():Health.down().withDetail("reason","编号租约不可用").build();}
 @Bean org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer<MeterRegistry> commonMetrics(@Value("${spring.application.name}") String application,@Value("${b2bweb.instance:local}") String instance,@Value("${b2bweb.environment:local}") String environment){return registry->registry.config().commonTags("application",application,"instance",instance,"env",environment);}
}
