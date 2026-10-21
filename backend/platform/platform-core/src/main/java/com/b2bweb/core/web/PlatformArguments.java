package com.b2bweb.core.web;

import com.b2bweb.api.common.*;
import com.b2bweb.core.support.web.*;
import java.util.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class PlatformArguments implements WebMvcConfigurer {
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new Arguments());
  }

  public static final class Arguments implements HandlerMethodArgumentResolver {
    public boolean supportsParameter(MethodParameter p) {
      return p.getParameterType() == PageRequest.class
          || p.hasParameterAnnotation(IfMatchVersion.class)
          || p.hasParameterAnnotation(IdempotencyKey.class);
    }

    public Object resolveArgument(
        MethodParameter p,
        ModelAndViewContainer container,
        NativeWebRequest request,
        org.springframework.web.bind.support.WebDataBinderFactory factory) {
      if (p.hasParameterAnnotation(IfMatchVersion.class)) {
        String value = request.getHeader("If-Match");
        if (value == null) {
          if (p.getParameterAnnotation(IfMatchVersion.class).required())
            throw new BusinessException(
                ErrorCode.of("CORE_PRECONDITION_REQUIRED", 428, "缺少版本条件"), "请提供If-Match版本条件");
          return null;
        }
        return ETags.parse(value);
      }
      if (p.hasParameterAnnotation(IdempotencyKey.class)) {
        String value = request.getHeader("Idempotency-Key");
        if (value == null && !p.getParameterAnnotation(IdempotencyKey.class).required())
          return null;
        if (value == null || !value.matches("[A-Za-z0-9_-]{8,64}"))
          throw new IllegalArgumentException("幂等键格式无效");
        return value;
      }
      PageDefault defaults = p.getParameterAnnotation(PageDefault.class);
      Sortable allowed = p.getParameterAnnotation(Sortable.class);
      String order = request.getParameter("sort");
      if (order == null && defaults != null) order = defaults.sort();
      var sorts = new ArrayList<SortOrder>();
      if (order != null && !order.isEmpty()) {
        if (order.length() > 500) throw new IllegalArgumentException("排序字段过多");
        for (String expr : order.split(",", -1)) {
          var sort = SortOrder.parse(expr);
          if (allowed == null || !Arrays.asList(allowed.value()).contains(sort.field()))
            throw new IllegalArgumentException("排序字段不允许");
          if (sorts.stream().anyMatch(s -> s.field().equals(sort.field())))
            throw new IllegalArgumentException("排序字段重复");
          sorts.add(sort);
        }
      }
      String page = request.getParameter("page"), size = request.getParameter("size");
      return new PageRequest(
          page == null ? 1 : Integer.parseInt(page),
          size == null ? (defaults == null ? 20 : defaults.size()) : Integer.parseInt(size),
          sorts);
    }
  }
}
