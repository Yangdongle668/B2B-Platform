package com.b2bweb.delivery;

/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_delivery")
public class DeliveryModule implements com.b2bweb.api.extension.PlatformModule {
  private final com.b2bweb.api.extension.ModuleDescriptor descriptor;

  public DeliveryModule(
      @org.springframework.beans.factory.annotation.Qualifier("deliveryModuleDescriptor")
          com.b2bweb.api.extension.ModuleDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor() {
    return descriptor;
  }
}
