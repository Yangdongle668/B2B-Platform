package com.b2bweb.core.support.validation;

import jakarta.validation.*;
import java.lang.annotation.*;

@Constraint(validatedBy = PlainText.Validator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.RECORD_COMPONENT,
  ElementType.ANNOTATION_TYPE,
  ElementType.TYPE_USE
})
public @interface PlainText {
  String message() default "请输入纯文本";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  final class Validator implements ConstraintValidator<PlainText, CharSequence> {
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
      return value == null
          || !java.util.regex.Pattern.compile("[<][^>]+[>]|[\\p{Cntrl}&&[^\\n\\r\\t]]")
              .matcher(value)
              .find();
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
