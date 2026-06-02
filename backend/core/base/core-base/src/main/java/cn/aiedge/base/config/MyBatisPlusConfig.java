package cn.aiedge.base.config;

import cn.aiedge.base.security.SecurityContext;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis-Plus 配置类
 * 包含全局租户隔离拦截器和分页插件
 */
@Configuration
public class MyBatisPlusConfig {

    /** 不需要租户隔离的表名（系统级表） */
    private static final Set<String> IGNORE_TENANT_TABLES = new HashSet<>(Arrays.asList(
        "sys_tenant",        // 租户表本身
        "sys_project_config" // 项目配置可能跨租户
    ));

    @Autowired(required = false)
    @Lazy
    private SecurityContext securityContext;

    /**
     * 分页插件 + 租户隔离插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        // 全局租户隔离插件 — 自动为 SELECT/INSERT/UPDATE/DELETE 注入 tenant_id 过滤
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从安全上下文获取当前租户ID
                if (securityContext != null) {
                    Long tenantId = securityContext.getCurrentTenantId();
                    if (tenantId != null) {
                        return new LongValue(tenantId);
                    }
                }
                // 未登录或无法获取时返回默认租户（防止全表扫描）
                return new LongValue(0);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 跳过系统表和非多租户表
                return IGNORE_TENANT_TABLES.contains(tableName)
                    || !tableName.contains("_");  // 简单判断：无下划线的表名跳过
            }

            @Override
            public boolean ignoreInsert(java.util.List<net.sf.jsqlparser.schema.Column> columns, String tenantIdColumn) {
                // INSERT 时如果已手动指定 tenant_id 则保留原值
                return columns != null && columns.stream()
                    .anyMatch(col -> tenantIdColumn.equalsIgnoreCase(col.getColumnName()));
            }
        }));

        return interceptor;
    }

    /**
     * 元数据填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                // 自动填充 tenantId（从安全上下文）
                if (metaObject.hasSetter("tenantId") && securityContext != null) {
                    Long tenantId = securityContext.getCurrentTenantId();
                    if (tenantId != null) {
                        metaObject.setValue("tenantId", tenantId);
                    }
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
