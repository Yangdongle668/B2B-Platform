package com.b2bweb.testkit.conformance;

import static org.assertj.core.api.Assertions.*;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.seo.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

public abstract class PersonalDataProviderConformance {
  protected abstract PersonalDataProvider provider();

  protected abstract PersonalDataRequest request(long site);

  protected abstract void seedTwoSites();

  protected abstract int exportedRecords(JsonNode value);

  @Test
  public void exportAndFindAgreeEraseIsSiteScopedAndIdempotent() {
    seedTwoSites();
    var p = provider();
    var first = request(1);
    var second = request(2);
    int other = p.find(second).recordCount();
    assertThat(other).isPositive();
    int count = p.find(first).recordCount();
    assertThat(count).isPositive().isEqualTo(exportedRecords(p.export(first)));
    assertThat(p.erase(first)).isEqualTo(count);
    assertThat(p.find(first).recordCount()).isZero();
    assertThat(p.erase(first)).isZero();
    assertThat(p.find(second).recordCount()).isEqualTo(other);
  }
}
