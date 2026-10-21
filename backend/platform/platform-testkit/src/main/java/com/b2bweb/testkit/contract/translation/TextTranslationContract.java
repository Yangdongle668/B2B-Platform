package com.b2bweb.testkit.contract.translation;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.translation.*;
import org.junit.jupiter.api.Test;
public abstract class TextTranslationContract {
 protected abstract TranslationApi api();protected abstract void site(long id);protected abstract void feature(boolean enabled);protected abstract void module(boolean enabled);
 @Test public void featureAndModuleAvailabilityAreScopedToCurrentSite(){
  site(1);feature(false);assertThat(api().isTextTranslationAvailable()).isFalse();
  assertThatThrownBy(()->api().translateText(new TextTranslationRequest("原文","en","zh-CN",TextPurpose.INQUIRY_MESSAGE,null))).isInstanceOf(BusinessException.class);
  site(2);assertThat(api().isTextTranslationAvailable()).isTrue();
  site(1);feature(true);module(false);assertThat(api().isTextTranslationAvailable()).isFalse();
  assertThatThrownBy(()->api().translateText(new TextTranslationRequest("原文","en","zh-CN",TextPurpose.INQUIRY_MESSAGE,null))).isInstanceOf(ModuleDisabledException.class);
 }
 @Test public void sameLanguageIsUnchangedAndLongInputsAreRejected(){
  site(1);var same=api().translateText(new TextTranslationRequest("<1>100</1> W","zh-CN","zh-CN",TextPurpose.INQUIRY_MESSAGE,null));
  assertThat(same.sameLanguage()).isTrue();assertThat(same.translatedText()).isEqualTo("<1>100</1> W");assertThat(same.inputTokens()+same.outputTokens()).isZero();
  assertThatThrownBy(()->api().translateText(new TextTranslationRequest("x".repeat(5001),"en","zh-CN",TextPurpose.INQUIRY_MESSAGE,null))).isInstanceOf(BusinessException.class);
 }
}
