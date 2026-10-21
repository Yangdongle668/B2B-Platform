package com.b2bweb.testkit;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.Visibility;
import com.b2bweb.api.event.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.url.*;
import com.b2bweb.testkit.contract.common.*;
import com.b2bweb.testkit.contract.event.*;
import com.b2bweb.testkit.contract.extension.*;
import com.b2bweb.testkit.contract.security.*;
import com.b2bweb.testkit.contract.site.*;
import com.b2bweb.testkit.contract.media.*;
import com.b2bweb.testkit.contract.taxonomy.*;
import com.b2bweb.testkit.contract.url.*;
import com.b2bweb.testkit.fixture.ContractTransactions;
import com.b2bweb.testkit.security.FakeSecretCipher;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Nested;
class SharedServicesTest {
 static long ready(TestPlatform p){byte[] bytes="%PDF-1.7 contract".getBytes(java.nio.charset.StandardCharsets.UTF_8);long id=p.mediaApi().upload(new UploadCommand("file.pdf",null,bytes.length,Visibility.PRIVATE,UploadSource.FORM_ATTACHMENT,null,false,Set.of(),null),new java.io.ByteArrayInputStream(bytes)).media().mediaId();p.mediaApi().status(id,MediaStatus.READY);return id;}
 @Nested class Ids extends IdGeneratorContract {final TestPlatform p=TestPlatform.standard();protected IdGenerator api(){return p.ids();}}
 @Nested class Cipher extends SecretCipherContract {final Map<String,byte[]> keys=Map.of("old",new byte[32],"current",java.util.HexFormat.of().parseHex("07".repeat(32)));final SecretCipher value=new FakeSecretCipher("current",keys);protected SecretCipher api(){return value;}protected SecretCipher oldKey(){return new FakeSecretCipher("old",keys);}}
 @Nested class CurrentUser extends CurrentUserProviderContract {final TestPlatform p=TestPlatform.standard();protected CurrentUserProvider api(){return p.currentUserProvider();}protected void current(com.b2bweb.api.security.CurrentUser user){p.useUser(user);}}
 @Nested class SiteContext extends SiteContextProviderContract {final TestPlatform p=TestPlatform.standard();protected SiteContextProvider api(){return p.siteContextProvider();}protected void site(Long id){if(id==null)p.withoutSite();else p.useSite(id);}}
 @Nested class Meta extends EventMetaFactoryContract {final TestPlatform p=TestPlatform.standard();protected EventMetaFactory api(){return p.eventMetaFactory();}protected void site(Long id){if(id==null)p.withoutSite();else p.useSite(id);}protected Instant now(){return p.clock().instant();}}
 @Nested class Dicts extends DictApiContract {final TestPlatform p=TestPlatform.standard();protected DictApi api(){return p.dictApi();}protected void seed(String type,DictItem item){p.dictApi().seed(type,item);}}
 @Nested class Profiles extends SiteProfileApiContract {final TestPlatform p=TestPlatform.standard();protected SiteProfileApi api(){return p.siteProfileApi();}protected void site(long id){p.useSite(id);}}
 @Nested class Texts extends UiTextApiContract {final TestPlatform p=TestPlatform.standard();protected UiTextApi api(){return p.uiTextApi();}protected void site(long id){p.useSite(id);}protected void seed(String key,String locale,String text){p.uiTextApi().override(locale,key,text);}}
 @Nested class Settings extends SettingsApiContract {final TestPlatform p=TestPlatform.standard();protected SettingsApi api(){return p.settingsApi();}protected void site(long id){p.useSite(id);}}
 @Nested class Mutex extends TransactionMutexApiContract {final TestPlatform p=TestPlatform.standard();protected TransactionMutexApi api(){return p.transactionMutexApi();}protected void site(long id){p.useSite(id);}protected void transaction(Runnable action,boolean rollback){ContractTransactions.run(action,rollback);}}
 @Nested class Users extends UserDirectoryContract {final TestPlatform p=TestPlatform.standard();protected UserDirectory api(){return p.userDirectory();}protected void site(long id){p.useSite(id);}protected void seed(UserSummary u,boolean admin,Set<Long> sites){p.userDirectory().seed(u,admin,sites);}protected void grant(long user,String permission,DataScope data,LocaleScope locale){p.permissionChecker().grant(user,permission,data,locale);}}
 @Nested class Sites extends SiteApiContract {final TestPlatform p=TestPlatform.standard();protected SiteApi api(){return p.siteApi();}protected void site(Long id){if(id==null)p.withoutSite();else p.useSite(id);}protected void enabled(String module,boolean value){p.siteApi().setModuleEnabled(module,value);}}
 @Nested class Audit extends AuditApiContract {final TestPlatform p=TestPlatform.standard();protected AuditApi api(){return p.auditApi();}protected void site(long id){p.useSite(id);}protected void transaction(Runnable action,boolean rollback){ContractTransactions.run(action,rollback);}protected List<AuditEntry> records(){return p.auditApi().records().stream().map(com.b2bweb.testkit.security.RecordedAudit.Recorded::entry).toList();}}
 @Nested class Projections extends ProjectionChangeApiContract {final TestPlatform p=TestPlatform.standard();protected ProjectionChangeApi api(){return p.projectionChangeApi();}protected void site(long id){p.useSite(id);}protected void transaction(Runnable action,boolean rollback){ContractTransactions.run(action,rollback);}protected int capturedCount(){return p.projectionChangeApi().captures().size();}protected Set<Long> capturedIds(){return p.projectionChangeApi().captures().getLast().ids();}}
 @Nested class References extends ReferenceWriteApiContract {final TestPlatform p=TestPlatform.standard().useSite(1);protected ReferenceWriteApi api(){return p.referenceWriteApi();}protected void transaction(Runnable action,boolean rollback){ContractTransactions.run(action,rollback);}protected long readyMedia(){return ready(p);}protected int referenceCount(long id){return p.mediaApi().usages(id).size();}}
 @Nested class Urls extends UrlApiContract {final TestPlatform p=TestPlatform.standard();protected UrlApi api(){return p.urlApi();}protected void site(long id){p.useSite(id);}}
 @Nested class Media extends MediaApiContract {final TestPlatform p=TestPlatform.standard();protected MediaApi api(){return p.mediaApi();}protected void site(long id){p.useSite(id);}protected long readyMedia(){return ready(p);}}
 @Nested class Taxonomy extends TaxonomyApiContract {final TestPlatform p=TestPlatform.standard();protected TaxonomyApi api(){return p.taxonomyApi();}protected void site(long id){p.useSite(id);}protected void seed(){p.taxonomyApi().seed(new VocabularyInfo("category","分类",true,true,true,true,Set.of("SAMPLE"),true,0));p.taxonomyApi().seed(new TermInfo(1,"category",null,"/1/",0,0,null,"电池"),Map.of());p.taxonomyApi().seed(new TermInfo(2,"category",1L,"/1/2/",1,0,null,"锂电池"),Map.of("en","Lithium battery"));}}
 @Nested class Extensions extends ExtensionRegistryContract {final TestPlatform p=TestPlatform.standard();final ExtensionRegistry registry=p.extensionRegistry(new com.b2bweb.testkit.example.SampleExtensions(p).all().toArray());protected ExtensionRegistry api(){return registry;}protected void site(Long id){if(id==null)p.withoutSite();else p.useSite(id);}protected void enabled(boolean v){p.siteApi().setModuleEnabled("sample",v);}}
}
