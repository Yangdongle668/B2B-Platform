package com.b2bweb.core.coordination;

import com.b2bweb.api.extension.SearchChangeCapture;
import com.b2bweb.api.media.MediaReferenceSink;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class CoordinatorBindings implements ApplicationRunner, Ordered {
  private final ObjectProvider<MediaReferenceSink> media;
  private final ObjectProvider<SearchChangeCapture> search;

  public CoordinatorBindings(
      ObjectProvider<MediaReferenceSink> media, ObjectProvider<SearchChangeCapture> search) {
    this.media = media;
    this.search = search;
  }

  public int getOrder() {
    return 1;
  }

  public void run(ApplicationArguments args) {
    if (media.stream().count() != 1 || search.stream().count() != 1)
      throw new IllegalStateException("媒体引用与搜索捕获接收器必须各有且只有一个");
  }
}
