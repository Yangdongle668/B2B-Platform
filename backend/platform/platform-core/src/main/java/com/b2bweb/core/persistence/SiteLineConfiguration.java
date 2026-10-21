package com.b2bweb.core.persistence;

import com.b2bweb.api.site.SiteContextProvider;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import java.util.*;
import net.sf.jsqlparser.expression.*;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class SiteLineConfiguration {
  // Schema-owned global tables only. Missing context is never silently turned into site 0.
  static final Set<String> GLOBAL =
      Set.of(
          "sys_lock",
          "sys_module",
          "sys_role",
          "sys_site",
          "sys_user",
          "sys_dict_type",
          "sys_dsr_request",
          "sys_menu",
          "sys_permission",
          "sys_user_mfa",
          "sys_dict_item",
          "sys_role_permission",
          "sys_audit_log",
          "sys_processed_event",
          "sys_cache_recovery",
          "sys_cache_recovery_publication",
          "EVENT_PUBLICATION");

  @Bean
  MybatisPlusInterceptor siteRows(SiteContextProvider sites) {
    var result = new MybatisPlusInterceptor();
    result.addInnerInterceptor(
        new TenantLineInnerInterceptor(
            new TenantLineHandler() {
              public Expression getTenantId() {
                return new LongValue(sites.requireSiteId());
              }

              public String getTenantIdColumn() {
                return "site_id";
              }

              public boolean ignoreTable(String name) {
                return GLOBAL.contains(name);
              }
            }));
    return result;
  }
}
