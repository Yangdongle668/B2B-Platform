package com.b2bweb.inquiry.fixtures;

/** Deliberately invalid compiled fixture. Never a Spring component or production source. */
public class IllegalBoundaryExample {
  private com.b2bweb.product.ProductModule forbiddenBusinessDependency;
  private com.b2bweb.core.context.SiteContexts forbiddenCoreImplementation;
}
