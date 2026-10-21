package com.b2bweb.infrastructure.translation;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** OpenAI-compatible HTTP port. Untrusted text is only serialized into the user JSON message. */
public final class OpenAiTranslationEngine implements TranslationEngine {
  final JsonMapper json=JsonMapper.builder().build();
  final java.util.function.Consumer<Duration> sleeper;
  public OpenAiTranslationEngine(){this(duration->{try{Thread.sleep(duration.toMillis());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("翻译等待被中断",e);}});}
  public OpenAiTranslationEngine(java.util.function.Consumer<Duration> sleeper){this.sleeper=Objects.requireNonNull(sleeper);}
  static final class RetryFailure extends ExternalServiceException {
    final Duration delay;
    RetryFailure(Duration delay){super(InfraErrors.ENGINE_UNAVAILABLE,"翻译引擎暂时不可用",true,null);this.delay=delay;}
  }
  private ExternalServiceException failure(ErrorCode code,boolean retryable) {return new ExternalServiceException(code,code.title(),retryable,null);}
  private JsonNode format(StructuredOutputMode mode) {
    if(mode==StructuredOutputMode.NONE)return null;
    if(mode==StructuredOutputMode.JSON_OBJECT)return json.readTree("{\"type\":\"json_object\"}");
    return json.readTree("{\"type\":\"json_schema\",\"json_schema\":{\"name\":\"translations\",\"strict\":true,\"schema\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"detectedSource\",\"items\"],\"properties\":{\"detectedSource\":{\"type\":[\"string\",\"null\"]},\"items\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"id\",\"text\"],\"properties\":{\"id\":{\"type\":\"string\"},\"text\":{\"type\":\"string\"}}}}}}}}");
  }
  public TranslationResult translate(TranslationRequest request) {
    if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("翻译HTTP调用不能持有数据库事务");
    var engine=request.engine(); URI base=engine.baseUrl();
    if(base.getUserInfo()!=null || base.getQuery()!=null || base.getFragment()!=null || engine.timeout().isNegative() || engine.timeout().isZero())throw new IllegalArgumentException("引擎地址或超时无效");
    var data=json.createObjectNode(); if(request.sourceLocale()==null)data.putNull("source");else data.put("source",request.sourceLocale());data.put("target",request.targetLocale());
    data.set("items",json.valueToTree(request.items()));
    var body=json.createObjectNode().put("model",engine.model()).put("temperature",engine.temperature());
    var messages=body.putArray("messages");messages.addObject().put("role","system").put("content",request.systemPrompt());messages.addObject().put("role","user").put("content",json.writeValueAsString(data));
    if(engine.maxOutputTokens()!=null)body.put("max_tokens",engine.maxOutputTokens());
    var responseFormat=format(engine.structuredOutput());if(responseFormat!=null)body.set("response_format",responseFormat);
    long start=System.nanoTime();
    for(int attempt=1;attempt<=4;attempt++) {
      Duration delay=Duration.ofMillis((1L<<(attempt-1))*1000+java.util.concurrent.ThreadLocalRandom.current().nextInt(251));
      try(var client=HttpClient.newBuilder().connectTimeout(engine.timeout()).followRedirects(HttpClient.Redirect.NEVER).build()) {
        var factory=new JdkClientHttpRequestFactory(client);factory.setReadTimeout(engine.timeout());
        var call=RestClient.builder().requestFactory(factory).build().post().uri(URI.create(base.toString().replaceAll("/+$","")+"/chat/completions")).header("Content-Type","application/json");
        if(!engine.apiKey().value().isEmpty())call.header("Authorization","Bearer "+engine.apiKey().value());
        engine.extraHeaders().forEach((key,value)->{if(Set.of("host","content-length","content-type","connection").contains(key.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("引擎扩展头不允许覆盖传输头");call.header(key,value);});
        JsonNode response=call.body(json.writeValueAsString(body)).exchange((http,result)->{
          int status=result.getStatusCode().value();
          if(status==401 || status==403)throw failure(InfraErrors.ENGINE_AUTH_FAILED,false);
          if(status==429 || status>=500)throw new RetryFailure(retryAfter(result.getHeaders().getFirst("Retry-After")));
          if(status<200 || status>=300)throw failure(InfraErrors.ENGINE_BAD_REQUEST,false);
          byte[] bytes=result.getBody().readNBytes(2_097_153);if(bytes.length>2_097_152)throw failure(InfraErrors.ENGINE_OUTPUT_INVALID,false);
          try{return json.readTree(bytes);}catch(RuntimeException e){throw failure(InfraErrors.ENGINE_OUTPUT_INVALID,false);}
        });
        return decode(request,response,Duration.ofNanos(System.nanoTime()-start),attempt);
      } catch(ExternalServiceException e) {
        if(!e.retryable() || attempt==4)throw e;
        if(e instanceof RetryFailure retry && retry.delay!=null)delay=retry.delay;
      } catch(org.springframework.web.client.RestClientException e) {
        if(attempt==4)throw failure(InfraErrors.ENGINE_UNAVAILABLE,true);
      }
      try{sleeper.accept(delay);}catch(RuntimeException e){throw failure(InfraErrors.ENGINE_UNAVAILABLE,true);}
    }
    throw failure(InfraErrors.ENGINE_UNAVAILABLE,true);
  }
  private TranslationResult decode(TranslationRequest request,JsonNode response,Duration elapsed,int attempts) {
    try {
      if(response.path("choices").path(0).path("finish_reason").asString("").equals("length"))throw new IllegalArgumentException();
      var message=response.path("choices").path(0).path("message").path("content");if(!message.isString())throw new IllegalArgumentException();
      String output=message.asString().strip();
      if(request.engine().structuredOutput()==StructuredOutputMode.NONE && output.startsWith("```"))output=output.replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");
      var translated=json.readTree(output);if(!translated.isObject() || !translated.path("items").isArray())throw new IllegalArgumentException();
      Map<String,String> texts=new HashMap<>();
      for(var item:translated.path("items")) {
        if(!item.path("id").isString() || !item.path("text").isString() || texts.putIfAbsent(item.path("id").asString(),item.path("text").asString())!=null)throw new IllegalArgumentException();
      }
      if(!texts.keySet().equals(new HashSet<>(request.items().stream().map(EngineItem::id).toList())))throw new IllegalArgumentException();
      var items=request.items().stream().map(item->new EngineTranslation(item.id(),texts.get(item.id()))).toList();
      String detected=translated.path("detectedSource").isString()?translated.path("detectedSource").asString():null;
      if(detected!=null && !detected.matches("[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*"))throw new IllegalArgumentException();
      var usage=response.path("usage");int input=usage.path("prompt_tokens").asInt(0),outputTokens=usage.path("completion_tokens").asInt(0);if(input<0 || outputTokens<0)throw new IllegalArgumentException();
      Integer cached=usage.path("prompt_tokens_details").path("cached_tokens").isIntegralNumber()?usage.path("prompt_tokens_details").path("cached_tokens").asInt():null;
      if(cached!=null && (cached<0 || cached>input))throw new IllegalArgumentException();
      return new TranslationResult(items,detected,new EngineUsage(input,outputTokens,cached),response.path("model").asString(request.engine().model()),elapsed,attempts);
    } catch(RuntimeException e) {throw failure(InfraErrors.ENGINE_OUTPUT_INVALID,false);}
  }
  private Duration retryAfter(String value){
    if(value==null)return null;
    try{long seconds=Long.parseLong(value);return Duration.ofSeconds(Math.min(30,Math.max(0,seconds)));}
    catch(NumberFormatException e){try{Duration delay=Duration.between(Instant.now(),ZonedDateTime.parse(value,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());return delay.isNegative()?Duration.ZERO:delay.compareTo(Duration.ofSeconds(30))>0?Duration.ofSeconds(30):delay;}catch(java.time.DateTimeException invalid){return null;}}
  }
  public EngineProbeResult probe(EngineConfig engine) {
    long start=System.nanoTime();
    try {var result=translate(new TranslationRequest(engine,"Translate the supplied JSON data and return the required JSON structure.","zh-CN","en",List.of(new EngineItem("u1","连接测试",null,null)),"probe"));return new EngineProbeResult(true,result.model(),result.elapsed(),null,null);}
    catch(PlatformException e){return new EngineProbeResult(false,null,Duration.ofNanos(System.nanoTime()-start),e.errorCode().code(),"翻译引擎测试失败");}
    catch(RuntimeException e){return new EngineProbeResult(false,null,Duration.ofNanos(System.nanoTime()-start),"INFRA_ENGINE_UNAVAILABLE","翻译引擎测试失败");}
  }
}
