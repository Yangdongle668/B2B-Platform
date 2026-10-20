package com.b2bweb.form;

/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_form")
public class FormModule implements com.b2bweb.api.extension.PlatformModule {
  private final com.b2bweb.api.extension.ModuleDescriptor descriptor;

  public FormModule(
      @org.springframework.beans.factory.annotation.Qualifier("formModuleDescriptor")
          com.b2bweb.api.extension.ModuleDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor() {
    return descriptor;
  }
}
