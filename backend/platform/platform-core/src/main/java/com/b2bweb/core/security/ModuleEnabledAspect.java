package com.b2bweb.core.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class ModuleEnabledAspect {
  private final SiteApi sites;

  public ModuleEnabledAspect(SiteApi sites) {
    this.sites = sites;
  }

  private Object check(ProceedingJoinPoint call, RequiresModuleEnabled rule) throws Throwable {
    if (!sites.isModuleEnabled(rule.value())) throw new ModuleDisabledException(rule.value());
    return call.proceed();
  }

  @Around("@annotation(rule)")
  public Object method(ProceedingJoinPoint call, RequiresModuleEnabled rule) throws Throwable {
    return check(call, rule);
  }

  @Around("@within(rule) && !@annotation(com.b2bweb.api.common.RequiresModuleEnabled)")
  public Object type(ProceedingJoinPoint call, RequiresModuleEnabled rule) throws Throwable {
    return check(call, rule);
  }
}
