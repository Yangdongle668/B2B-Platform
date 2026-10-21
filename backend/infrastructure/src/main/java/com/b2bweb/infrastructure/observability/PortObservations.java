package com.b2bweb.infrastructure.observability;

import io.micrometer.core.instrument.*;
import java.lang.reflect.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

/** Wraps infrastructure adapters only; keys/payloads never become metric tags. */
public final class PortObservations implements BeanPostProcessor {
  private final ObjectProvider<MeterRegistry> metrics;

  public PortObservations(ObjectProvider<MeterRegistry> metrics) {
    this.metrics = metrics;
  }

  public Object postProcessAfterInitialization(Object bean, String name) {
    if (!bean.getClass().getPackageName().startsWith("com.b2bweb.infrastructure.")) return bean;
    Class<?>[] ports =
        Arrays.stream(bean.getClass().getInterfaces())
            .filter(t -> t.getPackageName().equals("com.b2bweb.api.port"))
            .toArray(Class<?>[]::new);
    if (ports.length == 0) return bean;
    return Proxy.newProxyInstance(
        bean.getClass().getClassLoader(),
        bean.getClass().getInterfaces(),
        (proxy, method, args) -> {
          if (method.getDeclaringClass() == Object.class)
            return switch (method.getName()) {
              case "toString" -> "ObservedPort[" + ports[0].getSimpleName() + "]";
              case "hashCode" -> System.identityHashCode(proxy);
              case "equals" -> proxy == args[0];
              default -> throw new IllegalStateException();
            };
          var registry = metrics.getIfAvailable();
          String
              port =
                  ports[0]
                      .getSimpleName()
                      .replaceAll("([a-z])([A-Z])", "$1_$2")
                      .toLowerCase(Locale.ROOT),
              operation = method.getName();
          boolean pure =
              Set.of("ImageUrlSigner", "GeoIpResolver").contains(ports[0].getSimpleName())
                  || ports[0].getSimpleName().equals("ObjectStorage")
                      && Set.of("publicUrl", "presignedGet").contains(operation);
          if (!pure
              && org.springframework.transaction.support.TransactionSynchronizationManager
                  .isActualTransactionActive()) {
            if (registry != null)
              registry.counter("b2bweb.core.tx.remote_call", "port", port).increment();
            throw new IllegalStateException("远程端口不得持有数据库事务");
          }
          long started = System.nanoTime();
          String outcome = "success";
          try {
            Object result = method.invoke(bean, args);
            if (result instanceof com.b2bweb.api.port.RateLimitDecision decision
                && !decision.allowed()
                && registry != null)
              registry
                  .counter(
                      "b2bweb.core.ratelimit.rejected",
                      "rule",
                      ((com.b2bweb.api.port.RateLimitRule) args[0]).name())
                  .increment();
            return result;
          } catch (InvocationTargetException e) {
            outcome = "error";
            if (registry != null)
              registry
                  .counter(
                      "b2bweb.infra." + port + ".errors",
                      "operation",
                      operation,
                      "outcome",
                      outcome)
                  .increment();
            throw e.getCause();
          } finally {
            if (registry != null) {
              registry
                  .counter(
                      "b2bweb.infra." + port + ".calls", "operation", operation, "outcome", outcome)
                  .increment();
              registry
                  .timer(
                      "b2bweb.infra." + port + ".latency",
                      "operation",
                      operation,
                      "outcome",
                      outcome)
                  .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
          }
        });
  }
}
