package com.b2bweb.application.fixtures;

import java.util.Map;
import org.apache.ibatis.annotations.*;

public interface SiteRowMapper {
  @Select("SELECT id,site_id,type FROM cnt_content WHERE id=#{id}")
  Map<String, Object> find(long id);

  @Update("UPDATE cnt_content SET version=version+1 WHERE id=#{id}")
  int update(long id);

  @Select("SELECT COUNT(*) FROM sys_user")
  int globalUsers();
}
