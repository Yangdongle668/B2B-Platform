package com.b2bweb.testkit.contract.site;
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
public abstract class UiTextApiContract {
 protected abstract UiTextApi api();
 protected abstract void site(long id);
 protected abstract void seed(String key,String locale,String text);
 @Test public void interpolationAndNoCrossLanguageOrCrossSiteFallback() {
  site(1);seed("contract.greeting","zh-CN","你好 {name}");assertThat(api().text("contract.greeting","zh-CN")).contains("你好 {name}");assertThat(api().format("contract.greeting","zh-CN",Map.of("name","读者"))).contains("你好 读者");
  assertThat(api().text("contract.greeting","en")).isEmpty();assertThat(api().texts(List.of("contract.greeting","contract.missing"),"zh-CN")).containsOnlyKeys("contract.greeting");assertThat(api().texts(List.of(),"zh-CN")).isEmpty();
  site(2);assertThat(api().text("contract.greeting","zh-CN")).isEmpty();
 }

}
