package com.b2bweb.core.support.log;

import org.slf4j.MDC;

public final class ContextMdc {
  private ContextMdc() {}

  public static AutoCloseable bind(String name, String value) {
    String previous = MDC.get(name);
    if (value == null) MDC.remove(name);
    else MDC.put(name, value);
    return () -> {
      if (previous == null) MDC.remove(name);
      else MDC.put(name, previous);
    };
  }
}
