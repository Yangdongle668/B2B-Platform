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
public abstract class SettingsApiContract {
 protected abstract SettingsApi api();
 protected abstract void site(long id);
 @Test public void optimisticVersionAndSiteIsolation() {
  site(1);var value=api().getJson("inquiry.retention");assertThat(value.path("schemaVersion").asInt()).isEqualTo(1);assertThat(api().put("inquiry.retention",value,0)).isEqualTo(1);
  assertThatThrownBy(()->api().put("inquiry.retention",value,0)).isInstanceOf(VersionConflictException.class);
  site(2);assertThat(api().meta("inquiry.retention").version()).isZero();
 }

}
