package com.b2bweb.testkit.port;

import com.b2bweb.api.port.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit pseudo translations preserve numbers and numbered inline placeholders verbatim. */
public final class FakeTranslationEngine implements TranslationEngine {
  private final Map<String, String> outputs = new ConcurrentHashMap<>();
  private final List<TranslationRequest> requests = new CopyOnWriteArrayList<>();
  private RuntimeException failure;

  public void output(String unitId, String value) {
    outputs.put(unitId, value);
  }

  public void failWith(RuntimeException failure) {
    this.failure = failure;
  }

  public List<TranslationRequest> requests() {
    return List.copyOf(requests);
  }

  public TranslationResult translate(TranslationRequest request) {
    if (failure != null) throw failure;
    requests.add(request);
    var items =
        request.items().stream()
            .map(
                i ->
                    new EngineTranslation(
                        i.id(),
                        outputs.getOrDefault(
                            i.id(), "[" + request.targetLocale() + "] " + i.text())))
            .toList();
    int input =
        request.items().stream().mapToInt(i -> i.text().codePointCount(0, i.text().length())).sum();
    int output = items.stream().mapToInt(i -> i.text().codePointCount(0, i.text().length())).sum();
    return new TranslationResult(
        items,
        request.sourceLocale(),
        new EngineUsage(input, output, 0),
        "fake-translation",
        Duration.ZERO,
        1);
  }

  public EngineProbeResult probe(EngineConfig config) {
    return failure == null
        ? new EngineProbeResult(true, "fake-translation", Duration.ZERO, null, null)
        : new EngineProbeResult(false, null, Duration.ZERO, "INFRA_ENGINE_UNAVAILABLE", "测试引擎不可用");
  }

  public void reset() {
    outputs.clear();
    requests.clear();
    failure = null;
  }
}
