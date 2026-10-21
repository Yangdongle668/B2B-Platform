package com.b2bweb.tracking;
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_tracking")
public class TrackingModule implements com.b2bweb.api.extension.PlatformModule {
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public TrackingModule(@org.springframework.beans.factory.annotation.Qualifier("trackingModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){this.descriptor=descriptor;}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){return descriptor;}
}
