package com.b2bweb.application;
import com.b2bweb.api.common.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.core.context.*;
import com.b2bweb.core.security.AesGcmSecrets;
import com.b2bweb.testkit.contract.common.IdGeneratorContract;
import com.b2bweb.testkit.contract.security.*;
import com.b2bweb.testkit.contract.site.SiteContextProviderContract;
import com.b2bweb.testkit.contract.event.EventMetaFactoryContract;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties="spring.profiles.active=test")
class CoreSharedContractsTest {
 @Autowired IdGenerator generator;
 @Autowired EventMetaFactory meta;
 @Autowired SiteContexts sites;
 @Nested class Ids extends IdGeneratorContract {protected IdGenerator api(){return generator;}}
 @Nested class Cipher extends SecretCipherContract {final Map<String,byte[]> keys=Map.of("old",new byte[32],"current",java.util.HexFormat.of().parseHex("07".repeat(32)));final SecretCipher value=new AesGcmSecrets("current",keys);protected SecretCipher api(){return value;}protected SecretCipher oldKey(){return new AesGcmSecrets("old",keys);}}
 @Nested class Users extends CurrentUserProviderContract {final UserContexts context=new UserContexts();protected CurrentUserProvider api(){return context;}protected void current(CurrentUser u){context.open(u);}}
 @Nested class Contexts extends SiteContextProviderContract {final SiteContexts context=new SiteContexts(id->new SiteContext(id,"s"+id,"zh-CN","zh-CN",ZoneOffset.UTC,null,ContextOrigin.SYSTEM));protected SiteContextProvider api(){return context;}protected void site(Long id){if(id==null)context.withoutSite();else context.open(new SiteContext(id,"s"+id,"zh-CN","zh-CN",ZoneOffset.UTC,null,ContextOrigin.SYSTEM));}}
 @Nested class Metadata extends EventMetaFactoryContract {
  SiteContexts.Scope scope;
  protected EventMetaFactory api(){return meta;}
  protected void site(Long id){if(scope!=null)scope.close();scope=id==null?sites.withoutSite():sites.open(new SiteContext(id,"s"+id,"zh-CN","zh-CN",ZoneOffset.UTC,null,ContextOrigin.SYSTEM));}
  protected Instant now(){return Instant.now();}
  @AfterEach void clear(){if(scope!=null)scope.close();}
 }
}
