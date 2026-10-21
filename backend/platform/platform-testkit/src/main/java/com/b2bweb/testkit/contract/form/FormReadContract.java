package com.b2bweb.testkit.contract.form;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.form.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Implement seed through fixture setup; assertions depend only on the canonical read API. */
public abstract class FormReadContract {
 protected abstract FormApi api(); protected abstract void site(long id); protected abstract void seed(FormSummary form);
 @Test public void disabledDefinitionsCanBeReadButNeverSelectedForUse(){
  site(1);seed(new FormSummary(11,"enabled","启用表单",FormPurpose.INQUIRY,FormStatus.ACTIVE,Set.of("zh-CN")));
  seed(new FormSummary(12,"disabled","停用表单",FormPurpose.INQUIRY,FormStatus.DISABLED,Set.of("zh-CN")));
  seed(new FormSummary(13,"download","下载",FormPurpose.DOWNLOAD,FormStatus.ACTIVE,Set.of("zh-CN")));
  assertThat(api().findForm("disabled")).get().extracting(FormSummary::status).isEqualTo(FormStatus.DISABLED);
  assertThat(api().forms(FormPurpose.INQUIRY)).extracting(FormSummary::formId).containsExactly(11L);
  site(2);assertThat(api().findForm(11)).isEmpty();assertThat(api().findForm("enabled")).isEmpty();assertThat(api().forms(FormPurpose.INQUIRY)).isEmpty();
 }
}
