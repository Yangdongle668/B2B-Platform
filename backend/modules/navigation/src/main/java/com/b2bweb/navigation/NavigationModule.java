package com.b2bweb.navigation;
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_navigation")
public class NavigationModule implements com.b2bweb.api.extension.PlatformModule {
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public NavigationModule(@org.springframework.beans.factory.annotation.Qualifier("navigationModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){this.descriptor=descriptor;}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){return descriptor;}
}
