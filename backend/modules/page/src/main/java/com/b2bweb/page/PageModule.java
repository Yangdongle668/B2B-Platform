package com.b2bweb.page;

/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_page")
public class PageModule implements com.b2bweb.api.extension.PlatformModule {
  private final com.b2bweb.api.extension.ModuleDescriptor descriptor;

  public PageModule(
      @org.springframework.beans.factory.annotation.Qualifier("pageModuleDescriptor")
          com.b2bweb.api.extension.ModuleDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor() {
    return descriptor;
  }
}
