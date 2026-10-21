package com.b2bweb.inquiry;
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_inquiry")
public class InquiryModule implements com.b2bweb.api.extension.PlatformModule {
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public InquiryModule(@org.springframework.beans.factory.annotation.Qualifier("inquiryModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){this.descriptor=descriptor;}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){return descriptor;}
}
