package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.util.*;

public final class FakeGeoIpResolver implements GeoIpResolver {
  private final Map<String, GeoLocation> entries = new HashMap<>();

  public void seed(String ip, GeoLocation location) {
    entries.put(ip, location);
  }

  public Optional<GeoLocation> resolve(String ip) {
    if (!PublicAddresses.isPublicLiteral(ip)) return Optional.empty();
    return Optional.ofNullable(entries.get(ip));
  }

  public String databaseVersion() {
    return "testkit-fixtures";
  }
}
