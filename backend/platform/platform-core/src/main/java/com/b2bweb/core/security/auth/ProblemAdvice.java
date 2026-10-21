package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ProblemAdvice {
  @ExceptionHandler(PlatformException.class)
  void platform(PlatformException e, HttpServletResponse response) throws IOException {
    Problems.write(response, e);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  void fields(MethodArgumentNotValidException e, HttpServletResponse response) throws IOException {
    var errors =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                f ->
                    new FieldError(
                        f.getField(),
                        Objects.requireNonNullElse(f.getCode(), "INVALID"),
                        switch (Objects.requireNonNullElse(f.getCode(), "")) {
                          case "NotBlank", "NotNull", "NotEmpty" -> "字段不能为空";
                          case "Email" -> "邮箱格式不合法";
                          case "Size", "Min", "Max", "Positive" -> "字段超出允许范围";
                          default -> "字段格式不合法";
                        }))
            .toList();
    Problems.write(response, 400, "CORE_VALIDATION_FAILED", "输入校验失败", "请检查输入字段", errors, Map.of());
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  void invalid(Exception e, HttpServletResponse response) throws IOException {
    Problems.write(response, 400, "CORE_VALIDATION_FAILED", "输入校验失败");
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  void missing(Exception e, HttpServletResponse response) throws IOException {
    Problems.write(response, 404, "CORE_RESOURCE_NOT_FOUND", "资源不存在");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  void method(Exception e, HttpServletResponse response) throws IOException {
    Problems.write(response, 405, "CORE_METHOD_NOT_ALLOWED", "请求方法不支持");
  }

  @ExceptionHandler(Exception.class)
  void unexpected(Exception e, HttpServletResponse response) throws IOException {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("系统请求处理失败", e);
    Problems.write(response, 500, "CORE_INTERNAL_ERROR", "系统暂时无法处理请求");
  }
}
