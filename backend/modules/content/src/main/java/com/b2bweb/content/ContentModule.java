package com.b2bweb.content;

/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_content")
public class ContentModule implements com.b2bweb.api.extension.PlatformModule {
  private final com.b2bweb.api.extension.ModuleDescriptor descriptor;

  public ContentModule(
      @org.springframework.beans.factory.annotation.Qualifier("contentModuleDescriptor")
          com.b2bweb.api.extension.ModuleDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor() {
    return descriptor;
  }
}
