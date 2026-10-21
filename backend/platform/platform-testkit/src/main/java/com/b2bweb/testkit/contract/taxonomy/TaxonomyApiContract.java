package com.b2bweb.testkit.contract.taxonomy;
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
public abstract class TaxonomyApiContract {
 protected abstract com.b2bweb.api.taxonomy.TaxonomyApi api();
 protected abstract void site(long id);
 protected abstract void seed();
 @Test public void ancestorsBatchAndMissingTranslationAreSiteScoped() {
  site(1);seed();assertThat(api().ancestors(2)).extracting(com.b2bweb.api.taxonomy.TermInfo::termId).containsExactly(1L);assertThat(api().isSameOrDescendant(2,1)).isTrue();assertThat(api().isSameOrDescendant(1,2)).isFalse();
  assertThat(api().findTerms(List.of())).isEmpty();assertThat(api().findTerms(List.of(1L,999L))).containsOnlyKeys(1L);assertThat(api().termViews(List.of(1L),"en").get(1L).name()).isNull();
  site(2);assertThat(api().findTerm(1)).isEmpty();
 }

}
