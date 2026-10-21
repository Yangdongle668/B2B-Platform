package com.b2bweb.application;
import com.b2bweb.testkit.contract.DomainEventContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
@SpringBootTest
@ActiveProfiles("test")
class DomainEventsRealContractTest extends DomainEventContract {
 @Autowired JsonMapper mapper;
 protected JsonMapper json(){return mapper;}
}
