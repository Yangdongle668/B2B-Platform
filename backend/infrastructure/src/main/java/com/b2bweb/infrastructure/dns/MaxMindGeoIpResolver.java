package com.b2bweb.infrastructure.dns;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.http.SsrfGuard;
import com.maxmind.geoip2.DatabaseReader;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/**
 * Local database, replaced atomically by operations; reload opens the new file before closing the
 * old.
 */
public final class MaxMindGeoIpResolver implements GeoIpResolver, AutoCloseable {
  private final Path path;
  private DatabaseReader reader;
  private java.nio.file.attribute.FileTime modified;

  public MaxMindGeoIpResolver(Path path) throws java.io.IOException {
    this.path = path;
    reload();
  }

  private synchronized void reload() throws java.io.IOException {
    var nextModified = Files.getLastModifiedTime(path);
    if (!nextModified.equals(modified)) {
      var next = new DatabaseReader.Builder(path.toFile()).build();
      var old = reader;
      reader = next;
      modified = nextModified;
      if (old != null) old.close();
    }
  }

  public synchronized Optional<GeoLocation> resolve(String ip) {
    if (ip == null || !ip.matches("[0-9a-fA-F:.]+")) return Optional.empty();
    try {
      var address = InetAddress.getByName(ip);
      if (!SsrfGuard.publicAddress(address)) return Optional.empty();
      reload();
      var result = reader.tryCity(address);
      if (result.isEmpty()) return Optional.empty();
      var city = result.get();
      String country = city.getCountry().getIsoCode();
      if (country == null) return Optional.empty();
      String region = city.getMostSpecificSubdivision().getIsoCode();
      return Optional.of(
          new GeoLocation(
              country,
              region == null ? null : country + "-" + region,
              city.getContinent().getCode()));
    } catch (java.io.IOException | com.maxmind.geoip2.exception.GeoIp2Exception e) {
      return Optional.empty();
    }
  }

  public synchronized String databaseVersion() {
    try {
      reload();
      return reader.getMetadata().getBuildDate().toInstant().toString();
    } catch (java.io.IOException e) {
      return "unavailable";
    }
  }

  public synchronized void close() throws java.io.IOException {
    reader.close();
  }
}
