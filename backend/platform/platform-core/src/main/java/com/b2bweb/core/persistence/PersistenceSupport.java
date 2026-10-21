package com.b2bweb.core.persistence;

import com.b2bweb.api.common.IdGenerator;
import com.b2bweb.api.security.CurrentUserProvider;
import com.b2bweb.api.site.SiteContextProvider;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import java.time.*;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class PersistenceSupport {
  @Bean
  IdentifierGenerator platformIdentifiers(IdGenerator ids) {
    return new IdentifierGenerator() {
      public Number nextId(Object entity) {
        return ids.nextId();
      }
    };
  }

  @Bean
  MetaObjectHandler auditFields(SiteContextProvider sites, CurrentUserProvider users, Clock clock) {
    return new MetaObjectHandler() {
      private void absent(MetaObject object, String field, Object value) {
        if (object.hasSetter(field) && getFieldValByName(field, object) == null)
          setFieldValByName(field, value, object);
      }

      public void insertFill(MetaObject object) {
        Instant now = clock.instant();
        long actor = users.actor().userId();
        if (object.hasSetter("siteId")) {
          Long supplied = (Long) getFieldValByName("siteId", object);
          long site = sites.requireSiteId();
          if (supplied != null && supplied != site) throw new IllegalArgumentException("不能写入其他站点");
          absent(object, "siteId", site);
        }
        absent(object, "createdAt", now);
        absent(object, "updatedAt", now);
        absent(object, "createdBy", actor);
        absent(object, "updatedBy", actor);
      }

      public void updateFill(MetaObject object) {
        if (object.hasSetter("updatedAt")) setFieldValByName("updatedAt", clock.instant(), object);
        if (object.hasSetter("updatedBy"))
          setFieldValByName("updatedBy", users.actor().userId(), object);
      }
    };
  }

  @Bean
  org.springframework.beans.factory.InitializingBean jsonTypeHandlers(
      tools.jackson.databind.json.JsonMapper json) {
    return () -> com.b2bweb.core.support.persistence.JsonNodeTypeHandler.configure(json);
  }
}
