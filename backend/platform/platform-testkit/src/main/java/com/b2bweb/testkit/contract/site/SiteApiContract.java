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
public abstract class SiteApiContract {
 protected abstract SiteApi api();
 protected abstract void site(Long id);
 protected abstract void enabled(String module,boolean value);
 @Test public void languageVisibilityAndModuleSwitchAreExplicit() {
  site(1L);assertThat(api().current().siteId()).isEqualTo(1);assertThat(api().find(Long.MAX_VALUE)).isEmpty();assertThat(api().locale("missing")).isEmpty();assertThat(api().localeByUrlPrefix("zh").orElseThrow().locale()).isEqualTo("zh-CN");assertThat(api().isPublicLocale("zh-CN")).isTrue();assertThat(api().isPublicLocale("missing")).isFalse();assertThat(api().autoTranslateLocales()).doesNotContain("zh-CN");
  enabled("sample",false);assertThat(api().isModuleEnabled("sample")).isFalse();site(2L);assertThat(api().isModuleEnabled("sample")).isTrue();site(null);assertThatThrownBy(()->api().current()).isInstanceOf(NoSiteContextException.class);
 }

}
