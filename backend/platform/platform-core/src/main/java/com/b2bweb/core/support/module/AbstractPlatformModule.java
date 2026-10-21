package com.b2bweb.core.support.module;

import com.b2bweb.api.extension.*;

/** Descriptor-backed lifecycle base; hooks retain PlatformModule's idempotency contract. */
public abstract class AbstractPlatformModule implements PlatformModule {
  private final ModuleDescriptor descriptor;

  protected AbstractPlatformModule(ModuleDescriptor descriptor) {
    this.descriptor = java.util.Objects.requireNonNull(descriptor);
  }

  public final ModuleDescriptor descriptor() {
    return descriptor;
  }
}
