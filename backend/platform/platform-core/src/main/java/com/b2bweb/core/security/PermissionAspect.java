package com.b2bweb.core.security;

import com.b2bweb.api.common.*;
import com.b2bweb.api.security.*;
import com.b2bweb.core.context.UserContexts;
import java.time.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class PermissionAspect {
  private final PermissionChecker permissions;
  private final UserContexts users;
  private final Clock clock;
  private final Duration window;

  public PermissionAspect(
      PermissionChecker permissions, UserContexts users, Clock clock, Duration window) {
    this.permissions = permissions;
    this.users = users;
    this.clock = clock;
    this.window = window;
  }

  @Around("@annotation(rule)")
  public Object method(ProceedingJoinPoint call, RequiresPermission rule) throws Throwable {
    return invoke(call, rule);
  }

  @Around("@within(rule) && !@annotation(com.b2bweb.api.security.RequiresPermission)")
  public Object type(ProceedingJoinPoint call, RequiresPermission rule) throws Throwable {
    return invoke(call, rule);
  }

  private Object invoke(ProceedingJoinPoint call, RequiresPermission rule) throws Throwable {
    boolean allowed =
        rule.logical() == RequiresPermission.Logical.ANY
            ? permissions.hasAny(rule.value())
            : java.util.Arrays.stream(rule.value()).allMatch(permissions::has);
    if (!allowed) throw new ForbiddenException(CoreErrors.FORBIDDEN, "没有操作权限");
    if (rule.reauthenticate()
        && users.current().isPresent()
        && users
                .lastReauthentication()
                .map(at -> at.plus(window).isAfter(clock.instant()))
                .orElse(false)
            == false)
      throw new BusinessException(ErrorCode.of("CORE_REAUTH_REQUIRED", 403, "需要再次验证身份"), "请重新验证身份");
    return call.proceed();
  }
}
