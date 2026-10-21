package com.b2bweb.core.support.persistence;

import java.sql.*;
import org.apache.ibatis.type.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

@MappedTypes(JsonNode.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public final class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {
  private static volatile JsonMapper mapper;

  public static void configure(JsonMapper value) {
    mapper = java.util.Objects.requireNonNull(value);
  }

  private static JsonMapper json() {
    var result = mapper;
    if (result == null) throw new IllegalStateException("平台JSON映射器尚未就绪");
    return result;
  }

  public void setNonNullParameter(PreparedStatement ps, int i, JsonNode value, JdbcType type)
      throws SQLException {
    ps.setString(i, json().writeValueAsString(value));
  }

  private JsonNode parse(String value) {
    return value == null ? null : json().readTree(value);
  }

  public JsonNode getNullableResult(ResultSet rs, String column) throws SQLException {
    return parse(rs.getString(column));
  }

  public JsonNode getNullableResult(ResultSet rs, int column) throws SQLException {
    return parse(rs.getString(column));
  }

  public JsonNode getNullableResult(CallableStatement cs, int column) throws SQLException {
    return parse(cs.getString(column));
  }
}
