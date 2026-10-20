package com.b2bweb.core.directory;

import com.b2bweb.api.site.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The requested localization is read once. Missing text never falls back to the source locale. */
public final class DatabaseSiteProfiles implements SiteProfileApi {
  private final JdbcTemplate jdbc;
  private final SiteContextProvider sites;
  private final JsonMapper json = JsonMapper.builder().build();

  public DatabaseSiteProfiles(JdbcTemplate jdbc, SiteContextProvider sites) {
    this.jdbc = jdbc;
    this.sites = sites;
  }

  private JsonNode tree(Object value) {
    return value == null ? json.createObjectNode() : json.readTree(value.toString());
  }

  private static String text(Map<String, Object> row, String name) {
    return (String) row.get(name);
  }

  private static Long id(Map<String, Object> row, String name) {
    return row.get(name) instanceof Number n ? n.longValue() : null;
  }

  private static String localized(JsonNode node, String name) {
    return node.path(name).isString() ? node.path(name).asString() : null;
  }

  public SiteProfileView get(String locale) {
    long site = sites.requireSiteId();
    var rows = jdbc.queryForList("SELECT * FROM sys_site_profile WHERE site_id=?", site);
    Map<String, Object> profile = rows.isEmpty() ? Map.of() : rows.getFirst();
    var localizations =
        profile.isEmpty()
            ? List.<Map<String, Object>>of()
            : jdbc.queryForList(
                "SELECT * FROM sys_site_profile_localization WHERE site_id=? AND profile_id=? AND"
                    + " locale=?",
                site,
                profile.get("id"),
                locale);
    Map<String, Object> translated = localizations.isEmpty() ? Map.of() : localizations.getFirst();
    JsonNode addressText = tree(translated.get("addresses"));
    List<SiteProfileView.Address> addresses = new ArrayList<>();
    for (var address : tree(profile.get("addresses"))) {
      var label = addressText.path(address.path("id").asString());
      addresses.add(
          new SiteProfileView.Address(
              address.path("type").asString(),
              localized(label, "label"),
              label.path("street").asString(""),
              localized(address, "postalCode"),
              label.path("city").asString(""),
              localized(label, "region"),
              address.path("countryCode").asString(),
              localized(address, "phone")));
    }
    List<SiteProfileView.SocialLink> social = new ArrayList<>();
    for (var item : tree(profile.get("social_links")))
      social.add(
          new SiteProfileView.SocialLink(
              item.path("platform").asString(), item.path("url").asString()));
    String representatives = text(profile, "legal_representatives");
    var legal =
        new SiteProfileView.LegalInfo(
            text(profile, "legal_name"),
            text(profile, "registered_address"),
            representatives == null
                ? List.of()
                : representatives.lines().filter(v -> !v.isBlank()).toList(),
            text(profile, "register_authority"),
            text(profile, "register_number"),
            text(profile, "vat_id"),
            text(profile, "supervisory_authority"),
            text(profile, "content_responsible"),
            text(profile, "legal_email"),
            text(profile, "legal_phone"));
    return new SiteProfileView(
        locale,
        text(translated, "company_name"),
        text(translated, "description"),
        id(profile, "logo_media_id"),
        id(profile, "logo_inverse_media_id"),
        id(profile, "favicon_media_id"),
        id(profile, "app_icon_media_id"),
        text(profile, "email"),
        text(profile, "phone"),
        text(profile, "whatsapp"),
        addresses,
        social,
        text(translated, "opening_hours_note"),
        profile.get("founded_year") instanceof Number n ? n.intValue() : null,
        legal);
  }
}
