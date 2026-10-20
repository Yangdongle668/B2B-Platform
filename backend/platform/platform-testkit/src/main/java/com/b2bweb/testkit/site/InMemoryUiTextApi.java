package com.b2bweb.testkit.site;

import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemoryUiTextApi implements UiTextApi {
  final MemoryStore store;
  final SiteContextProvider sites;
  final Map<String, String> defaults = new HashMap<>();

  public InMemoryUiTextApi(MemoryStore store, SiteContextProvider sites) {
    this.store = store;
    this.sites = sites;
  }

  public void defaults(Map<String, String> values) {
    defaults.clear();
    defaults.putAll(values);
  }

  public void override(String locale, String key, String value) {
    String path = sites.requireSiteId() + ":texts:" + locale + ":" + key;
    store.atomic(
        rows -> {
          rows.put(path, value);
          return null;
        });
  }

  public Optional<String> text(String key, String locale) {
    return Optional.ofNullable(allTexts(locale).get(key));
  }

  public Map<String, String> texts(Collection<String> keys, String locale) {
    var values = allTexts(locale);
    Map<String, String> out = new LinkedHashMap<>();
    for (String key : keys) if (values.containsKey(key)) out.put(key, values.get(key));
    return Map.copyOf(out);
  }

  public Map<String, String> allTexts(String locale) {
    String prefix = sites.requireSiteId() + ":texts:" + locale + ":";
    return store.atomic(
        rows -> {
          Map<String, String> result = new LinkedHashMap<>();
          if (locale.equals("zh-CN")) result.putAll(defaults);
          rows.forEach(
              (key, value) -> {
                if (key.startsWith(prefix))
                  result.put(key.substring(prefix.length()), (String) value);
              });
          return Map.copyOf(result);
        });
  }

  public Optional<String> format(String key, String locale, Map<String, String> vars) {
    return text(key, locale)
        .map(
            value -> {
              var matcher =
                  java.util.regex.Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9]*)}").matcher(value);
              return matcher.replaceAll(
                  m ->
                      java.util.regex.Matcher.quoteReplacement(
                          vars.getOrDefault(m.group(1), m.group())));
            });
  }
}
