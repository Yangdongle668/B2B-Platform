package com.b2bplatform.compat.infra;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ProbePersistence {
    private static final ThreadLocal<Long> SITE = ThreadLocal.withInitial(() -> 1L);
    public static void site(long siteId) { SITE.set(siteId); }
    public static void clearSite() { SITE.remove(); }
    @Bean MybatisPlusInterceptor siteInterceptor() {
        var plugin = new MybatisPlusInterceptor();
        plugin.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            public Expression getTenantId() { return new LongValue(SITE.get()); }
            public String getTenantIdColumn() { return "site_id"; }
        }));
        return plugin;
    }
}
