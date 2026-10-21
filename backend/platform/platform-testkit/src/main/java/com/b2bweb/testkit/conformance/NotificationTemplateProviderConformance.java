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

public abstract class NotificationTemplateProviderConformance {
  protected abstract NotificationTemplateProvider provider();

  @Test
  public void templatesDeclareEveryVariableAndHaveCanonicalKeys() {
    var templates = provider().templates();
    assertThat(templates).isNotEmpty();
    assertThat(templates).extracting(NotificationTemplateDefinition::key).doesNotHaveDuplicates();
    for (var t : templates) {
      assertThat(t.key()).matches("[a-z]+\\.[a-z0-9_]+\\.(staff|customer)");
      assertThat(t.version()).isPositive();
      var variables = t.variables().stream().map(TemplateVariable::name).toList();
      assertThat(variables).doesNotHaveDuplicates();
      var matcher =
          java.util.regex.Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_.]*)\\s*}}")
              .matcher(t.subject() + t.body());
      while (matcher.find()) assertThat(variables).contains(matcher.group(1));
      if (t.audience() == NotificationAudience.STAFF) {
        assertThat(t.subject()).containsPattern("[㐀-鿿]");
        assertThat(t.body()).containsPattern("[㐀-鿿]");
      }
    }
  }
}
