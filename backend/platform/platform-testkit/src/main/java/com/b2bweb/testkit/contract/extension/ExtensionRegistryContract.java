package com.b2bweb.testkit.contract.extension;
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
public abstract class ExtensionRegistryContract {
 protected abstract com.b2bweb.api.extension.ExtensionRegistry api();
 protected abstract void site(Long id);
 protected abstract void enabled(boolean value);
 @Test public void sampleExtensionIsFilteredAtEachSiteAndUnknownKeyFails() {
  site(1L);assertThat(api().all(com.b2bweb.api.content.PublishContributor.class)).isNotEmpty();enabled(false);assertThat(api().all(com.b2bweb.api.content.PublishContributor.class)).isEmpty();assertThat(api().allRegardlessOfSite(com.b2bweb.api.content.PublishContributor.class)).isNotEmpty();site(2L);assertThat(api().all(com.b2bweb.api.content.PublishContributor.class)).isNotEmpty();
  assertThat(api().byKey(JobHandler.class,"missing")).isEmpty();assertThatThrownBy(()->api().requireByKey(JobHandler.class,"missing")).isInstanceOf(NotFoundException.class);site(null);assertThatThrownBy(()->api().all(JobHandler.class)).isInstanceOf(NoSiteContextException.class);
 }

}
