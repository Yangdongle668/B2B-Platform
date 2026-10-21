package com.b2bweb.testkit;
import com.b2bweb.api.port.ObjectStorage;
import com.b2bweb.testkit.contract.port.ObjectStorageContract;
class StorageDoubleContractTest extends ObjectStorageContract {
 final TestPlatform platform=TestPlatform.standard();protected ObjectStorage api(){return platform.objectStorage();}
}
