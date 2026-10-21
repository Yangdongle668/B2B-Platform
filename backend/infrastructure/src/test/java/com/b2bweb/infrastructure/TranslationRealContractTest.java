package com.b2bweb.infrastructure;
import com.b2bweb.api.common.Secret;
import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.translation.OpenAiTranslationEngine;
import com.b2bweb.testkit.contract.port.TranslationEngineContract;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
class TranslationRealContractTest extends TranslationEngineContract {
 HttpServer server;final Map<String,String> outputs=new HashMap<>();final JsonMapper json=JsonMapper.builder().build();
 @BeforeEach void start() throws Exception {
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",exchange->{
   var output=json.createObjectNode();var items=output.putArray("items");for(String id:List.of("u1","u2"))items.addObject().put("id",id).put("text",outputs.get(id));
   var response=json.createObjectNode().put("model","contract-http");response.putArray("choices").addObject().put("finish_reason","stop").putObject("message").put("content",json.writeValueAsString(output));response.putObject("usage").put("prompt_tokens",12).put("completion_tokens",6);
   byte[] bytes=json.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
 }
 @AfterEach void stop(){server.stop(0);}
 protected TranslationEngine api(){return new OpenAiTranslationEngine(ignored->{});}
 protected EngineConfig config(){return new EngineConfig(1,"本地契约",URI.create("http://127.0.0.1:"+server.getAddress().getPort()),Secret.of(""),"contract",0,null,Duration.ofSeconds(2),StructuredOutputMode.JSON_OBJECT,Map.of());}
 protected void output(String id,String text){outputs.put(id,text);}
}
