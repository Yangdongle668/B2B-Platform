package com.b2bweb.infrastructure;
import com.b2bweb.api.port.*;import com.b2bweb.api.common.Secret;import com.b2bweb.infrastructure.mail.SmtpMailTransport;import com.b2bweb.infrastructure.search.MeilisearchClient;
import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.beans.factory.annotation.Value;import java.net.URI;
@Configuration(proxyBeanMethods=false)
public class InfrastructurePorts {
 @Bean MailTransport mailTransport(org.springframework.core.env.Environment env,java.time.Clock clock){boolean local=env.matchesProfiles("local","test");String host=local?env.getProperty("b2bweb.infra.mail.host","mailpit"):env.getRequiredProperty("b2bweb.infra.mail.host");String password=env.getProperty("B2BWEB_SMTP_PASSWORD");var defaults=new MailServerConfig(host,env.getProperty("b2bweb.infra.mail.port",Integer.class,local?1025:587),env.getProperty("b2bweb.infra.mail.username"),password==null?null:Secret.of(password),SmtpSecurity.valueOf(env.getProperty("b2bweb.infra.mail.security",local?"NONE":"STARTTLS")));return new SmtpMailTransport(defaults,clock,local);}
 @Bean @ConditionalOnProperty(name="B2BWEB_MEILI_URL") SearchEngineClient searchEngineClient(@Value("${B2BWEB_MEILI_URL}") URI base,@Value("${B2BWEB_MEILI_MASTER_KEY}") String key){return new MeilisearchClient(base,Secret.of(key));}
}
