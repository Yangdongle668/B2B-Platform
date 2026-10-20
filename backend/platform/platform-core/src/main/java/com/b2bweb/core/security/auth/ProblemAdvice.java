package com.b2bweb.core.security.auth;

import com.b2bweb.api.common.PlatformException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ProblemAdvice {
  @ExceptionHandler(PlatformException.class)
  void platform(PlatformException e, HttpServletResponse response) throws IOException {
    Problems.write(response, e.errorCode().status(), e.errorCode().code(), e.errorCode().title());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  void invalid(Exception e, HttpServletResponse response) throws IOException {
    Problems.write(response, 400, "CORE_VALIDATION_FAILED", "输入校验失败");
  }
}
