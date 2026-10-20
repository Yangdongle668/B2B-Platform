package com.b2bweb.testkit.common;

import java.lang.reflect.*;
import java.util.*;

/**
 * Typed record updates preserve every component; unknown names fail rather than defaulting fields.
 */
public final class Records {
  private Records() {}

  public static <T extends Record> T copy(T value, Map<String, ?> changes) {
    Object[] pairs = new Object[changes.size() * 2];
    int i = 0;
    for (var change : changes.entrySet()) {
      pairs[i++] = change.getKey();
      pairs[i++] = change.getValue();
    }
    return copy(value, pairs);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Record> T copy(T value, Object... changes) {
    if (changes.length % 2 != 0) throw new IllegalArgumentException();
    Map<String, Object> patch = new HashMap<>();
    for (int i = 0; i < changes.length; i += 2) patch.put((String) changes[i], changes[i + 1]);
    try {
      var fields = value.getClass().getRecordComponents();
      Class<?>[] types = new Class<?>[fields.length];
      Object[] args = new Object[fields.length];
      for (int i = 0; i < fields.length; i++) {
        types[i] = fields[i].getType();
        args[i] =
            patch.containsKey(fields[i].getName())
                ? patch.remove(fields[i].getName())
                : fields[i].getAccessor().invoke(value);
      }
      if (!patch.isEmpty())
        throw new IllegalArgumentException("Unknown record components: " + patch.keySet());
      return (T) value.getClass().getDeclaredConstructor(types).newInstance(args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtime) throw runtime;
      throw new IllegalStateException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
