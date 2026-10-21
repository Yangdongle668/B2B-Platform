package com.b2bweb.testkit.contract.url;
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
public abstract class UrlApiContract {
 protected abstract com.b2bweb.api.url.UrlApi api();
 protected abstract void site(long id);
 @Test public void routeHistoryIsFlattenedAndDoesNotCrossSites() {
  site(1);for(String slug:List.of("first","second","third"))api().applyRoute(new com.b2bweb.api.url.ApplyRouteCommand(1,11,"SAMPLE","zh-CN","/zh/"+slug,null));
  assertThat(api().resolve("/zh/first")).isEqualTo(new com.b2bweb.api.url.RouteResolution.Redirect(301,"/zh/third",com.b2bweb.api.url.RedirectReason.REDIRECT_RULE));
  site(2);assertThat(api().pathOf(11)).isEmpty();
 }
 @Test public void retirementAndLanguageVisibilityAreExplicit() {
  site(1);api().applyRoute(new com.b2bweb.api.url.ApplyRouteCommand(1,11,"SAMPLE","zh-CN","/zh/first",null));api().retireRoute(new com.b2bweb.api.url.RetireRouteCommand(11,com.b2bweb.api.url.RetireMode.GONE,null));assertThat(api().resolve("/zh/first")).isInstanceOf(com.b2bweb.api.url.RouteResolution.Gone.class);
  api().applyRoute(new com.b2bweb.api.url.ApplyRouteCommand(2,22,"SAMPLE","en","/en/second",null));assertThat(api().resolve("/en/second")).isInstanceOf(com.b2bweb.api.url.RouteResolution.NotFound.class);
 }

}
