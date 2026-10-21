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
public abstract class RateLimiterContract {
 protected abstract RateLimiter api();
 protected abstract void site(long id);
 protected abstract void advance(Duration elapsed);
 protected abstract void available(boolean value);
 @Test public void tokenRefillAndSiteIsolation() {
  var rule=new RateLimitRule("form.submit",2,Duration.ofSeconds(10),true,true);
  site(1);assertThat(api().tryConsume(rule,"hash",2).allowed()).isTrue();assertThat(api().tryConsume(rule,"hash").allowed()).isFalse();
  site(2);assertThat(api().tryConsume(rule,"hash").allowed()).isTrue();
  site(1);advance(Duration.ofSeconds(5));assertThat(api().tryConsume(rule,"hash").allowed()).isTrue();assertThat(api().tryConsume(rule,"hash").allowed()).isFalse();
 }
 @Test public void outageUsesExplicitRulePolicy() {
  site(1);available(false);
  assertThat(api().tryConsume(new RateLimitRule("core.login",5,Duration.ofMinutes(15),false,true),"hash").allowed()).isFalse();
  assertThat(api().tryConsume(new RateLimitRule("tracking.view",5,Duration.ofMinutes(1),true,false),"hash").allowed()).isTrue();
 }

}
