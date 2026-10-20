package com.b2bweb.core;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.extension.*;
import com.b2bweb.core.context.SiteContexts;
import com.b2bweb.core.module.Extensions;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

class ExtensionsTest {
  interface Sample extends KeyedExtension, ContentTypeScoped {}

  @Extension(
      module = "product",
      contentTypes = {"PRODUCT", "ARTICLE"},
      order = 20)
  static class Product implements Sample {
    public String key() {
      return "product";
    }

    public boolean supports(String type) {
      return type.equals("PRODUCT");
    }
  }

  @Test
  void proxiedInheritedInterfacesAreResolvedAndDynamicScopeIsHonoured() {
    var proxy = new ProxyFactory(new Product());
    proxy.setProxyTargetClass(true);
    var contexts =
        new SiteContexts(
            id -> {
              throw new AssertionError();
            });
    var registry = new Extensions(List.of(proxy.getProxy()), contexts, (site, module) -> site == 1);
    assertThat(registry.forContentTypeForSite(1, Sample.class, "PRODUCT")).hasSize(1);
    assertThat(registry.forContentTypeForSite(1, Sample.class, "ARTICLE")).isEmpty();
    assertThat(registry.allForSite(2, Sample.class)).isEmpty();
    assertThat(registry.allRegardlessOfSite(Sample.class)).hasSize(1);
    assertThatThrownBy(() -> registry.all(Sample.class))
        .isInstanceOf(com.b2bweb.api.common.NoSiteContextException.class);
  }

  @Test
  void duplicateKeysAreRejectedEvenThroughProxies() {
    var proxy = new ProxyFactory(new Product());
    proxy.setProxyTargetClass(true);
    assertThatThrownBy(
            () ->
                new Extensions(
                    List.of(new Product(), proxy.getProxy()),
                    new SiteContexts(
                        id -> {
                          throw new AssertionError();
                        }),
                    (site, module) -> true))
        .isInstanceOf(IllegalStateException.class);
  }
}
