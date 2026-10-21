package com.b2bweb.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.infrastructure.dns.MaxMindGeoIpResolver;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class GeoIpDatabaseTest {
  @Test
  void readsRealMmdbAndExcludesReservedAndUnknownAddresses() throws Exception {
    var path = Path.of(getClass().getResource("/geoip/GeoIP2-City-Test.mmdb").toURI());
    try (var resolver = new MaxMindGeoIpResolver(path)) {
      assertThat(resolver.resolve("81.2.69.142"))
          .hasValueSatisfying(v -> assertThat(v.countryCode()).isEqualTo("GB"));
      assertThat(resolver.databaseVersion()).isNotIn("unknown", "unavailable");
      assertThat(resolver.resolve("127.0.0.1")).isEmpty();
      assertThat(resolver.resolve("bad-host.invalid")).isEmpty();
    }
  }
}
