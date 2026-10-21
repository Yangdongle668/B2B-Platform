package com.b2bweb.core.support.web;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface PageDefault {
  int size() default 20;

  String sort() default "-createdAt";
}
