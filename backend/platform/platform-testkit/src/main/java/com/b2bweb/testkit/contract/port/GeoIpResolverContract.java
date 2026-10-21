package com.b2bweb.testkit.contract.port;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class GeoIpResolverContract {
 protected abstract GeoIpResolver api();
 protected abstract void seed(String ip,GeoLocation location);
 @Test public void knownPublicAddressAndNonPublicRejection() {
  var location=new GeoLocation("US",null,"NA");seed("8.8.8.8",location);seed("127.0.0.1",location);
  assertThat(api().resolve("8.8.8.8")).contains(location);assertThat(api().databaseVersion()).isNotBlank();
  for(String input:List.of("127.0.0.1","::1","10.1.2.3","169.254.169.254","localhost","invalid"))assertThat(api().resolve(input)).isEmpty();
  assertThat(api().resolve("1.1.1.1")).isEmpty();
 }

}
