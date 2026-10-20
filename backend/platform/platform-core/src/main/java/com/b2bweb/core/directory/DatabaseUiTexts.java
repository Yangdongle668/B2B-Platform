package com.b2bweb.core.directory;

import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class DatabaseUiTexts implements UiTextApi {
  final JdbcTemplate jdbc;
  final SiteContextProvider sites;
  final SiteApi siteApi;

  public DatabaseUiTexts(JdbcTemplate jdbc, SiteContextProvider sites, SiteApi siteApi) {
    this.jdbc = jdbc;
    this.sites = sites;
    this.siteApi = siteApi;
  }

  public Optional<String> text(String key, String locale) {
    return Optional.ofNullable(allTexts(locale).get(key));
  }

  public Map<String, String> texts(Collection<String> keys, String locale) {
    var all = allTexts(locale);
    Map<String, String> out = new LinkedHashMap<>();
    for (String key : keys) if (all.containsKey(key)) out.put(key, all.get(key));
    return Map.copyOf(out);
  }

  public Map<String, String> allTexts(String locale) {
    long site = sites.requireSiteId();
    Map<String, String> out = new LinkedHashMap<>();
    if (locale.equals("zh-CN")) {
      String theme = siteApi.current().themeKey();
      if (!theme.matches("[a-z][a-z0-9-]{1,63}")) throw new IllegalStateException("主题标识非法");
      try (var input =
          getClass().getResourceAsStream("/META-INF/b2bweb/texts/" + theme + "/zh-CN.json")) {
        if (input == null) throw new IllegalStateException("主题文案未打包");
        var json = JsonMapper.builder().build().readTree(input);
        for (var item : json.properties()) out.put(item.getKey(), item.getValue().asString());
      } catch (java.io.IOException e) {
        throw new IllegalStateException("主题文案读取失败", e);
      }
    }
    for (var row :
        jdbc.queryForList(
            "SELECT text_key,text_value FROM sys_ui_text WHERE site_id=? AND locale=?",
            site,
            locale)) out.put((String) row.get("text_key"), (String) row.get("text_value"));
    return Map.copyOf(out);
  }

  public Optional<String> format(String key, String locale, Map<String, String> values) {
    return text(key, locale)
        .map(
            text ->
                java.util.regex.Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9]*)}")
                    .matcher(text)
                    .replaceAll(
                        m ->
                            java.util.regex.Matcher.quoteReplacement(
                                values.getOrDefault(m.group(1), m.group()))));
  }
}
