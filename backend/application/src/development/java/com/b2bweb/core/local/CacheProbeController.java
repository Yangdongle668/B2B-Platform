package com.b2bweb.core.local;
import com.b2bweb.api.common.CacheTag;
import com.b2bweb.api.port.*;
import com.b2bweb.api.site.SiteContextProvider;
import com.b2bweb.core.cache.*;
import java.time.Duration;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
/** Internal-token + socket-CIDR protected, local/development-only verification transport. */
@Profile("local")
@io.swagger.v3.oas.annotations.Hidden
@RestController
public class CacheProbeController {
 final RedisTaggedCache cache;final CacheRecovery recovery;final SiteContextProvider sites;
 public CacheProbeController(RedisTaggedCache cache,CacheRecovery recovery,SiteContextProvider sites){this.cache=cache;this.recovery=recovery;this.sites=sites;}
 public record Input(String operation,String incarnation,Long counter,String value){}
 @PostMapping("/api/internal/v1/system/m0/cache-probe")
 public Map<String,?> probe(@RequestBody Input input){
  String ns="delivery.m0probe",key="restart-proof";
  return switch(input.operation()){
   case "state" -> Map.of("state",cache.recoveryState());
   case "recover" -> Map.of("ready",recovery.recover());
   case "fence" -> Map.of("fence",cache.beginLoad());
   case "put" -> Map.of("stored",cache.putIfUnchanged(ns,key,Objects.requireNonNull(input.value()),Set.of(CacheTag.site(sites.requireSiteId())),Duration.ofSeconds(30),new CacheWriteFence(Objects.requireNonNull(input.incarnation()),Objects.requireNonNull(input.counter()))));
   case "get" -> {var value=cache.get(ns,key,String.class);yield Map.of("found",value.isPresent(),"value",value.orElse(""));}
   case "invalidate" -> Map.of("removed",cache.invalidate(Set.of(CacheTag.site(sites.requireSiteId()))));
   default -> throw new IllegalArgumentException("未知缓存验证操作");
  };
 }
}
