package com.b2bweb.core.support.validation;

import jakarta.validation.*;
import java.lang.annotation.*;

@Constraint(validatedBy = LocaleCode.Validator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.RECORD_COMPONENT,
  ElementType.ANNOTATION_TYPE,
  ElementType.TYPE_USE
})
public @interface LocaleCode {
  String message() default "语言代码格式无效";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  final class Validator implements ConstraintValidator<LocaleCode, CharSequence> {
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
      return value == null || com.b2bweb.api.common.Locales.isValid(value.toString());
    }

    private boolean http(String value) {
      try {
        var uri = java.net.URI.create(value);
        return java.util.Set.of("http", "https").contains(uri.getScheme())
            && uri.getHost() != null
            && uri.getUserInfo() == null;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
  }
}
