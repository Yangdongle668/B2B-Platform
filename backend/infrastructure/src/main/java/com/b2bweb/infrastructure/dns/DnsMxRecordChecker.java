package com.b2bweb.infrastructure.dns;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.http.RemoteCalls;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.naming.*;
import javax.naming.directory.*;

public final class DnsMxRecordChecker implements MxRecordChecker {
  @FunctionalInterface
  public interface Lookup {
    Map<String, List<String>> query(String domain) throws NamingException;
  }

  private final Clock clock;
  private final Duration ttl;
  private final Lookup lookup;
  private final ConcurrentHashMap<String, MxCheckResult> cache = new ConcurrentHashMap<>();

  public DnsMxRecordChecker(Clock clock, Duration timeout, Duration ttl) {
    this(
        clock,
        ttl,
        domain -> {
          var environment = new Hashtable<String, String>();
          environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
          environment.put("com.sun.jndi.dns.timeout.initial", Long.toString(timeout.toMillis()));
          environment.put("com.sun.jndi.dns.timeout.retries", "1");
          var context = new InitialDirContext(environment);
          try {
            var attributes = context.getAttributes(domain, new String[] {"MX", "A", "AAAA"});
            var result = new HashMap<String, List<String>>();
            for (String type : List.of("MX", "A", "AAAA")) {
              var a = attributes.get(type);
              if (a != null) {
                var list = new ArrayList<String>();
                for (int i = 0; i < a.size(); i++) list.add(a.get(i).toString());
                result.put(type, list);
              }
            }
            return result;
          } finally {
            context.close();
          }
        });
  }

  public DnsMxRecordChecker(Clock clock, Duration ttl, Lookup lookup) {
    this.clock = clock;
    this.ttl = ttl;
    this.lookup = lookup;
  }

  public MxCheckResult check(String domain) {
    RemoteCalls.outsideTransaction();
    if (domain == null
        || domain.length() > 253
        || !domain.equals(domain.toLowerCase(Locale.ROOT))
        || !domain.equals(java.net.IDN.toASCII(domain))
        || !domain.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")
        || domain.contains("..")) throw new IllegalArgumentException("邮箱域名格式无效");
    var saved = cache.get(domain);
    if (saved != null && saved.checkedAt().plus(ttl).isAfter(clock.instant())) return saved;
    MxCheckResult result;
    try {
      var records = lookup.query(domain);
      var mx =
          records.getOrDefault("MX", List.of()).stream()
              .map(v -> v.replaceFirst("^\\d+\\s+", ""))
              .map(v -> v.endsWith(".") ? v.substring(0, v.length() - 1) : v)
              .sorted()
              .toList();
      boolean nullMx =
          records.getOrDefault("MX", List.of()).stream().anyMatch(v -> v.matches("0\\s+\\."));
      MxStatus status =
          nullMx
              ? MxStatus.NONE
              : !mx.isEmpty()
                  ? MxStatus.HAS_MX
                  : records.containsKey("A") || records.containsKey("AAAA")
                      ? MxStatus.IMPLICIT_A
                      : MxStatus.NONE;
      result = new MxCheckResult(domain, status, nullMx ? List.of() : mx, clock.instant());
    } catch (NameNotFoundException e) {
      result = new MxCheckResult(domain, MxStatus.NONE, List.of(), clock.instant());
    } catch (NamingException e) {
      return new MxCheckResult(domain, MxStatus.UNKNOWN, List.of(), clock.instant());
    }
    if (cache.size() > 10000)
      cache.entrySet().removeIf(e -> !e.getValue().checkedAt().plus(ttl).isAfter(clock.instant()));
    if (cache.size() < 10000) cache.put(domain, result);
    return result;
  }
}
