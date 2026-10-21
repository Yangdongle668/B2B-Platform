package com.b2bweb.infrastructure;
import com.b2bweb.api.port.SearchEngineClient;
import com.b2bweb.api.common.Secret;
import com.b2bweb.testkit.contract.port.SearchEngineContract;
import com.b2bweb.infrastructure.search.MeilisearchClient;
import java.net.URI;
class SearchRealContractTest extends SearchEngineContract {
 final SearchEngineClient engine=new MeilisearchClient(URI.create(LocalAdaptersTest.required("B2BWEB_MEILI_URL")),Secret.of(LocalAdaptersTest.required("B2BWEB_MEILI_MASTER_KEY")));
 protected SearchEngineClient api(){return engine;}
}
