package com.b2bweb.core.support.validation;

import com.b2bweb.api.common.FieldError;
import com.networknt.schema.*;
import java.util.*;
import tools.jackson.databind.JsonNode;

public final class JsonSchemaValidator {
  public List<FieldError> validate(JsonNode schema, JsonNode value) {
    localOnly(schema);
    var compiled =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
    return compiled.validate(value).stream()
        .map(
            error ->
                new FieldError(
                    path(error.getInstanceLocation().toString()),
                    error.getKeyword().toUpperCase(Locale.ROOT),
                    "字段不符合定义约束"))
        .toList();
  }

  private void localOnly(JsonNode value) {
    if (value.isObject() && value.has("$ref") && !value.path("$ref").asString().startsWith("#"))
      throw new IllegalArgumentException("不允许远程Schema引用");
    if (value.isContainer()) value.forEach(this::localOnly);
  }

  private static String path(String pointer) {
    StringBuilder out = new StringBuilder();
    for (String token : pointer.replaceFirst("^/", "").split("/")) {
      if (token.isEmpty()) continue;
      token = token.replace("~1", "/").replace("~0", "~");
      if (token.matches("0|[1-9][0-9]*")) out.append('[').append(token).append(']');
      else {
        if (!out.isEmpty()) out.append('.');
        out.append(token);
      }
    }
    return out.toString();
  }
}
