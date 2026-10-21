package com.b2bweb.testkit;
import com.b2bweb.api.port.SearchEngineClient;
import com.b2bweb.testkit.contract.port.SearchEngineContract;
class SearchDoubleContractTest extends SearchEngineContract {
 final TestPlatform platform=TestPlatform.standard();protected SearchEngineClient api(){return platform.searchEngineClient();}
}
