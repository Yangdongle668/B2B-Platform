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
public abstract class SiteProfileApiContract {
 protected abstract SiteProfileApi api();
 protected abstract void site(long id);
 @Test public void untranslatedLanguageNeverFallsBackToChinese() {
  site(1);var view=api().get("en");assertThat(view.locale()).isEqualTo("en");assertThat(view.companyName()).isNull();
 }

}
