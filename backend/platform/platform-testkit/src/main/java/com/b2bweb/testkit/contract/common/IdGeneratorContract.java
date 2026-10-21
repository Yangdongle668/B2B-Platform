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
public abstract class IdGeneratorContract {
 protected abstract IdGenerator api();
 @Test public void positiveUniqueAndOrderedWithExactDecimalStrings() {
  long last=0;var unique=new HashSet<Long>();
  for(int i=0;i<10000;i++){long id=api().nextId();assertThat(id).isGreaterThan(last);assertThat(unique.add(id)).isTrue();assertThat(api().timestampOf(id)).isNotNull();last=id;}
  String value=api().nextIdString();assertThat(value).matches("[1-9][0-9]{0,18}");assertThat(api().timestampOf(Long.parseLong(value))).isNotNull();
 }

}
