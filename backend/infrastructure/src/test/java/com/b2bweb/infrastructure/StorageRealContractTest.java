package com.b2bweb.infrastructure;
import com.b2bweb.api.port.ObjectStorage;
import com.b2bweb.testkit.contract.port.ObjectStorageContract;
import org.junit.jupiter.api.*;
class StorageRealContractTest extends ObjectStorageContract {
 @BeforeAll static void setup(){LocalAdaptersTest.connect();}
 @AfterAll static void cleanup(){LocalAdaptersTest.close();}
 protected ObjectStorage api(){return LocalAdaptersTest.storage;}
}
