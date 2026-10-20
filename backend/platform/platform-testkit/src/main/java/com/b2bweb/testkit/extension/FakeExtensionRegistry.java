package com.b2bweb.testkit.extension;

import com.b2bweb.api.common.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.site.SiteContextProvider;
import java.util.*;
import java.util.function.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

public final class FakeExtensionRegistry implements ExtensionRegistry {
  private final List<Object> values;
  private final SiteContextProvider contexts;
  private final BiPredicate<Long, String> enabled;

  public FakeExtensionRegistry(
      Collection<?> values, SiteContextProvider contexts, BiPredicate<Long, String> enabled) {
    this.contexts = contexts;
    this.enabled = enabled;
    var copy = new ArrayList<Object>(values);
    for (Object value : copy)
      if (annotation(value) == null) throw new IllegalArgumentException("扩展缺少注解");
    copy.sort(
        Comparator.comparingInt((Object v) -> annotation(v).order())
            .thenComparing(v -> annotation(v).module())
            .thenComparing(v -> AopUtils.getTargetClass(v).getName()));
    Set<String> keys = new HashSet<>();
    for (Object v : copy)
      if (v instanceof KeyedExtension k) {
        for (Class<?> point : ClassUtils.getAllInterfacesForClassAsSet(AopUtils.getTargetClass(v)))
          if (point != KeyedExtension.class
              && KeyedExtension.class.isAssignableFrom(point)
              && !keys.add(point.getName() + ":" + k.key()))
            throw new IllegalStateException("重复扩展键: " + k.key());
      }
    this.values = List.copyOf(copy);
  }

  private static Extension annotation(Object v) {
    return AnnotatedElementUtils.findMergedAnnotation(AopUtils.getTargetClass(v), Extension.class);
  }

  public <T> List<T> allRegardlessOfSite(Class<T> point) {
    return values.stream().filter(point::isInstance).map(point::cast).toList();
  }

  public <T> List<T> allForSite(long site, Class<T> point) {
    return allRegardlessOfSite(point).stream()
        .filter(v -> enabled.test(site, moduleOf(v)))
        .toList();
  }

  public <T> List<T> all(Class<T> point) {
    return allForSite(contexts.requireSiteId(), point);
  }

  public <T> List<T> forContentTypeForSite(long site, Class<T> point, String type) {
    return allForSite(site, point).stream()
        .filter(
            v -> {
              var types = annotation(v).contentTypes();
              return (types.length == 0 || Arrays.asList(types).contains(type))
                  && (!(v instanceof ContentTypeScoped scoped) || scoped.supports(type));
            })
        .toList();
  }

  public <T> List<T> forContentType(Class<T> point, String type) {
    return forContentTypeForSite(contexts.requireSiteId(), point, type);
  }

  public <T extends KeyedExtension> Optional<T> byKey(Class<T> point, String key) {
    return all(point).stream().filter(v -> v.key().equals(key)).findFirst();
  }

  public <T extends KeyedExtension> T requireByKey(Class<T> point, String key) {
    return byKey(point, key)
        .orElseThrow(() -> new NotFoundException(CoreErrors.EXTENSION_KEY_UNKNOWN, "扩展未注册或已停用"));
  }

  public String moduleOf(Object extension) {
    return annotation(extension).module();
  }

  public List<ExtensionInfo> describe(Class<?> point) {
    return allRegardlessOfSite(point).stream()
        .map(
            v ->
                new ExtensionInfo(
                    point.getName(),
                    AopUtils.getTargetClass(v).getName(),
                    moduleOf(v),
                    Set.of(annotation(v).contentTypes()),
                    annotation(v).order(),
                    v instanceof KeyedExtension k ? k.key() : null,
                    contexts
                        .current()
                        .map(s -> enabled.test(s.siteId(), moduleOf(v)))
                        .orElse(false)))
        .toList();
  }
}
