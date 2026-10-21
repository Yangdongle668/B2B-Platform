package com.b2bweb.testkit.contract.media;
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
public abstract class MediaApiContract {
 protected abstract com.b2bweb.api.media.MediaApi api();
 protected abstract void site(long id);
 protected abstract long readyMedia();
 @Test public void deleteAndFindAreSiteScoped() {
  site(1);long id=readyMedia();assertThat(api().find(id)).isPresent();site(2);assertThat(api().find(id)).isEmpty();site(1);api().delete(id,false);assertThat(api().find(id)).isEmpty();api().delete(id,false);site(2);assertThat(api().find(id)).isEmpty();
 }

}
