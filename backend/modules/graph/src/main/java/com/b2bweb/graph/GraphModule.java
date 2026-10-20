package com.b2bweb.graph;

/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_graph")
public class GraphModule implements com.b2bweb.api.extension.PlatformModule {
  private final com.b2bweb.api.extension.ModuleDescriptor descriptor;

  public GraphModule(
      @org.springframework.beans.factory.annotation.Qualifier("graphModuleDescriptor")
          com.b2bweb.api.extension.ModuleDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  public com.b2bweb.api.extension.ModuleDescriptor descriptor() {
    return descriptor;
  }
}
