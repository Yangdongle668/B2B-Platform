package com.b2bweb.testkit.contract.common;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class DictApiContract {
 protected abstract DictApi api();
 protected abstract void seed(String type,DictItem item);
 @Test public void synchronizationIsIdempotentAndKeepsCustomDisabledHistory() {
  var definition=new DictTypeDefinition("core.contract","core","测试",null,true,List.of(new DictTypeDefinition.DictItemDefinition("closed","关闭",0)));
  api().ensureBuiltin(definition);api().ensureBuiltin(definition);assertThat(api().items("core.contract")).extracting(DictItem::value).containsExactly("closed");
  seed("core.contract",new DictItem("closed","关闭",0,false,true,null));seed("core.contract",new DictItem("custom","自定义",1,true,false,null));api().ensureBuiltin(definition);
  assertThat(api().items("core.contract")).extracting(DictItem::value).containsExactly("custom");assertThat(api().allItems("core.contract")).hasSize(2);assertThat(api().isValid("core.contract","closed")).isFalse();assertThat(api().label("core.contract","closed")).contains("关闭");assertThat(api().label("core.contract","missing")).isEmpty();
 }

}
