package com.b2bweb.product;
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_product")
public class ProductModule implements com.b2bweb.api.extension.PlatformModule {
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public ProductModule(@org.springframework.beans.factory.annotation.Qualifier("productModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){this.descriptor=descriptor;}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){return descriptor;}
}
