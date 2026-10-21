package com.b2bweb.core.support.web;

import java.lang.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@RestController
public @interface AdminController {}
