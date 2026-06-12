package cn.aiedge.base.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        "sys_tenant",             // 租户表本身
        "sys_project_config",     // 项目配置可能跨租户
        "sys_menu",               // 菜单定义系统级共享
        "sys_role_menu",          // 角色菜单分配系统级
        "sys_permission",         // 权限定义系统级
        "sys_role_permission",    // 角色权限分配系统级
        "sys_permission_template", // 权限模板系统级
        "sys_user_tenant",        // 用户租户关联表（登录时需要无租户过滤查询）
        "sys_user",               // 用户表（登录时需要无租户过滤查询）
        "sys_login_log",          // 登录日志表
        "flyway_schema_history"   // Flyway迁移历史表
    ));

    /** 临时租户ID（ThreadLocal）- 用于登录等未认证场景 */
    private static final ThreadLocal<Long> TEMP_TENANT_ID = new ThreadLocal<>();

    /**
     * 设置临时租户ID（用于登录流程等未认证场景）
     */
    public static void setTempTenantId(Long tenantId) {
        TEMP_TENANT_ID.set(tenantId);
    }

    /**
     * 清除临时租户ID
     */
    public static void clearTempTenantId() {
        TEMP_TENANT_ID.remove();
    }

    /**
     * 获取当前租户ID
     */
    public static Long getCurrentTenantIdValue() {
        // 1. 优先使用临时租户ID（用于登录等未认证场景）
        Long tempTenantId = TEMP_TENANT_ID.get();
        if (tempTenantId != null) {
            return tempTenantId;
        }
        // 2. 从 Sa-Token Session 获取（登录时存入）
        try {
            if (StpUtil.isLogin()) {
                Object sessionTenantId = StpUtil.getSession().get("tenantId");
                if (sessionTenantId != null) {
                    return Long.parseLong(sessionTenantId.toString());
                }
            }
        } catch (Exception ignored) {
            // session 不可用时忽略
        }
        // 3. 未登录时返回 null（不注入租户条件）
        return null;
    }

    /**
     * 分页插件 + 租户隔离插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        // 乐观锁插件（支持 @Version 注解）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 全局租户隔离插件 — 自动为 SELECT/INSERT/UPDATE/DELETE 注入 tenant_id 过滤
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = getCurrentTenantIdValue();
                // 未登录或无法获取时不注入租户条件（返回null让MyBatis-Plus跳过）
                if (tenantId == null) {
                    return null;
                }
                return new LongValue(tenantId);
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
                // 自动填充 tenantId（从当前租户ID获取）
                if (metaObject.hasSetter("tenantId")) {
                    Long tenantId = getCurrentTenantIdValue();
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
