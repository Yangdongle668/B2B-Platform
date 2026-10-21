package com.b2bweb.testkit;
import com.b2bweb.api.form.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.security.*;
import com.b2bweb.testkit.contract.form.FormReadContract;
import com.b2bweb.testkit.contract.notification.NotificationOutboxContract;
import com.b2bweb.testkit.contract.translation.TextTranslationContract;
import java.util.*;
class FormDoubleContractTest extends FormReadContract {
 final TestPlatform p=TestPlatform.standard();
 protected FormApi api(){return p.implementation(FormApi.class);}protected void site(long id){p.useSite(id);}protected void seed(FormSummary f){p.formApi().addForm(f);}
}
class NotificationDoubleContractTest extends NotificationOutboxContract {
 final TestPlatform p=TestPlatform.standard();
 protected NotificationApi api(){return p.notificationApi();}protected void site(long id){p.useSite(id);}protected void template(String key,String locale){p.notificationApi().template(key,locale,Set.of("title"));}
 protected void staff(long id,boolean active){p.userDirectory().seed(new UserSummary(id,"员工","staff"+id+"@example.test",active),false,Set.of(1L));}
 protected void rollback(Runnable action){var manager=new org.springframework.transaction.support.AbstractPlatformTransactionManager(){protected Object doGetTransaction(){return new Object();}protected void doBegin(Object tx,org.springframework.transaction.TransactionDefinition definition){}protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status){}protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status){}};new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(status->{action.run();status.setRollbackOnly();});}
}
class TranslationDoubleContractTest extends TextTranslationContract {
 final TestPlatform p=TestPlatform.standard();
 protected TranslationApi api(){return p.translationApi();}protected void site(long id){p.useSite(id);}protected void feature(boolean enabled){p.translationApi().available(enabled);}protected void module(boolean enabled){p.siteApi().setModuleEnabled("translation",enabled);}
}
