// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.port;
import java.util.*;
import java.time.*;
import java.net.*;
import java.io.*;
import java.util.function.*;
import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.url.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.form.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.seo.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;

/**
 * 索引设置（系统设计 19）。
 *
 * @param searchableAttributes            按优先级排序：title > model_nos > summary > terms > body_text
 * @param typoToleranceDisabledAttributes 关闭拼写容错的字段（model_nos）
 * @param localizedLanguage               localizedAttributes 声明的语言（ISO 639-3，如中文 "cmn"、德语 "deu"）；为空表示自动识别
 * @param synonyms                        同义词：词 → 同义词
 */
public record SearchIndexSettings(String primaryKey, List<String> searchableAttributes,
                                  List<String> filterableAttributes, List<String> sortableAttributes,
                                  List<String> typoToleranceDisabledAttributes, @Nullable String localizedLanguage,
                                  Map<String, List<String>> synonyms, Set<String> stopWords) {
    public SearchIndexSettings {
        searchableAttributes = List.copyOf(searchableAttributes);
        filterableAttributes = List.copyOf(filterableAttributes);
        sortableAttributes = List.copyOf(sortableAttributes);
        typoToleranceDisabledAttributes = List.copyOf(typoToleranceDisabledAttributes);
        synonyms = Map.copyOf(synonyms);
        stopWords = Set.copyOf(stopWords);
    }
}
