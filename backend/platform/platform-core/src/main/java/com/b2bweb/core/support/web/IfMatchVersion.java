package com.b2bweb.core.support.web;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface IfMatchVersion {
  boolean required() default true;
}
