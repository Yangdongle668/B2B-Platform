package com.b2bweb.testkit.contract.notification;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.notification.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
public abstract class NotificationOutboxContract {
 protected abstract NotificationApi api(); protected abstract void site(long id); protected abstract void template(String key,String locale);
 protected abstract void staff(long id,boolean active); protected abstract void rollback(Runnable action);
 NotificationRequest request(List<Recipient> recipients,String locale,String key){return new NotificationRequest("contract.notice",recipients,locale,null,JsonMapper.builder().build().readTree("{\"title\":\"<untrusted>\"}"),null,key,null,null,null);}
 @Test public void deduplicationAndStatusAreSiteScoped(){
  site(1);template("contract.notice","zh-CN");var request=request(List.of(new Recipient.Customer("customer@example.test",null)),"zh-CN","same");
  var ids=api().send(request);assertThat(ids).hasSize(1);assertThat(api().send(request)).isEqualTo(ids);assertThat(api().status(ids.getFirst()).orElseThrow().status()).isEqualTo(MessageStatus.PENDING);
  site(2);assertThat(api().status(ids.getFirst())).isEmpty();assertThat(api().templateAvailable("contract.notice","zh-CN")).isFalse();
 }
 @Test public void inactiveStaffAreSkippedAndActiveStaffAlwaysUseChineseTemplate(){
  site(1);staff(11,true);staff(12,false);template("contract.notice","zh-CN");
  assertThat(api().send(request(List.of(new Recipient.StaffUser(11),new Recipient.StaffUser(12)),"en","staff"))).hasSize(1);
 }
 @Test public void outboxJoinsSourceTransactionAndUnknownTemplatesFail(){
  site(1);var request=request(List.of(new Recipient.Customer("customer@example.test",null)),"zh-CN","rollback");
  assertThatThrownBy(()->api().send(request)).isInstanceOf(BusinessException.class);template("contract.notice","zh-CN");
  long[] id={0};rollback(()->id[0]=api().send(request).getFirst());assertThat(api().status(id[0])).isEmpty();
 }
}
