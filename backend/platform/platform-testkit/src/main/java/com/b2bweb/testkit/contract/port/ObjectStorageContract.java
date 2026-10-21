package com.b2bweb.testkit.contract.port;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
public abstract class ObjectStorageContract {
 protected abstract ObjectStorage api();
 @Test public void visibilityAndCallerOwnedStreamsAndIdempotentDelete() throws Exception {
  var key=new StorageKey("1/contract/"+UUID.randomUUID()+".txt");
  var input=new ByteArrayInputStream(new byte[]{1,2}){public void close(){throw new AssertionError("caller owns input");}};
  try{
   api().put(key,input,2,"text/plain",Visibility.PRIVATE);assertThat(api().stat(key,Visibility.PUBLIC)).isEmpty();
   try(var result=api().get(key,Visibility.PRIVATE)){assertThat(result.readAllBytes()).containsExactly(1,2);}
   api().delete(key,Visibility.PRIVATE);api().delete(key,Visibility.PRIVATE);assertThatThrownBy(()->api().get(key,Visibility.PRIVATE)).isInstanceOf(NotFoundException.class);
  }finally{api().delete(key,Visibility.PRIVATE);}
 }
 @Test public void shortAndLongStreamsCannotLeavePartialObjects(){
  var key=new StorageKey("1/contract/"+UUID.randomUUID()+".txt");
  try{for(long size:List.of(1L,3L)){assertThatThrownBy(()->api().put(key,new ByteArrayInputStream(new byte[2]),size,"text/plain",Visibility.PUBLIC)).isInstanceOf(IllegalArgumentException.class);assertThat(api().stat(key,Visibility.PUBLIC)).isEmpty();}}
  finally{api().delete(key,Visibility.PUBLIC);}
 }
 @Test public void presignedLifetimeIsBoundedAndFilenameIsUtf8(){
  var key=new StorageKey("1/contract/file.pdf");
  assertThatThrownBy(()->api().presignedGet(key,Duration.ofSeconds(59),null)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->api().presignedGet(key,Duration.ofDays(8),null)).isInstanceOf(IllegalArgumentException.class);
  assertThat(java.net.URLDecoder.decode(api().presignedGet(key,Duration.ofMinutes(1),"中文 文件.pdf").getRawQuery(),java.nio.charset.StandardCharsets.UTF_8)).contains("%20");
 }
}
