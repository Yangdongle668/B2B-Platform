package com.b2bweb.core.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.b2bweb.core.support.log.LogSafe;
import java.time.Instant;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** No request objects, exception messages, SQL or arbitrary MDC fields are serialized. */
public final class SafeLogLayout extends LayoutBase<ILoggingEvent> {
  private final JsonMapper json = JsonMapper.builder().build();
  private boolean structured = true;

  public void setStructured(boolean value) {
    structured = value;
  }

  public String doLayout(ILoggingEvent event) {
    String message = LogSafe.mask(event.getFormattedMessage());
    String logger = event.getLoggerName();
    String module =
        logger.startsWith("com.b2bweb.") ? logger.substring(11).split("\\.", 2)[0] : "framework";
    var fields = new LinkedHashMap<String, Object>();
    fields.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
    fields.put("level", event.getLevel().toString());
    fields.put("logger", logger);
    fields.put("module", module);
    fields.put("message", message);
    for (String key : List.of("traceId", "spanId", "siteId", "userId", "eventId", "jobId")) {
      String value = event.getMDCPropertyMap().get(key);
      if (value != null && value.matches("[a-zA-Z0-9_-]{1,80}")) fields.put(key, value);
    }
    if (event.getThrowableProxy() != null) {
      var error = event.getThrowableProxy();
      fields.put("exception", error.getClassName());
      fields.put(
          "frames",
          Arrays.stream(error.getStackTraceElementProxyArray())
              .limit(40)
              .map(Object::toString)
              .toList());
    }
    return structured
        ? json.writeValueAsString(fields) + "\n"
        : fields.get("timestamp")
            + " "
            + fields.get("level")
            + " "
            + logger
            + " "
            + message
            + (fields.containsKey("exception") ? " [" + fields.get("exception") + "]" : "")
            + "\n";
  }
}
