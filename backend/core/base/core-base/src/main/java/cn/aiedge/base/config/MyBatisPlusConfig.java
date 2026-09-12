package cn.aiedge.base.config;

import cn.aiedge.base.interceptor.DataScopeInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
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
 * 包含全局租户隔离拦截器、数据权限拦截器和分页插件
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
        "sys_region",             // 行政区划（省/市/区县，全系统公共数据，无tenant_id列）
        "flyway_schema_history",  // Flyway迁移历史表
        "sys_print_chain_item",   // 打印链路项（无tenant_id列）
        "sys_screenshot_task",    // 截图任务（无tenant_id列）
        "dms_event_outbox",       // DMS事件发件箱（无tenant_id列）
        // 工作流四表：多租户拦截属 P1 未实现项（见 AGENTS.md 核心差距），
        // 现阶段由 WorkflowServiceImpl 按 X-Tenant-Id 显式过滤；自动注入会使
        // 启动种子判重（tenant_id = null 永不匹配）与空租户会话下的可见性失效
        "workflow_definition",
        "workflow_node",
        "workflow_instance",
        "workflow_task"
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
     * 分页插件 + 租户隔离插件 + 数据权限插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 全局租户隔离插件 — 自动为 SELECT/INSERT/UPDATE/DELETE 注入 tenant_id 过滤
        // 使用增强版：租户不可解析（无会话线程）时跳过处理，不再注入字面量 tenant_id=null
        // 注意：必须放在分页插件之前（MyBatis-Plus 官方要求的多插件顺序），
        //      否则分页 count 语句不会注入 tenant_id，出现「total 含其它租户、records 只有本租户」的口径不一致
        interceptor.addInnerInterceptor(new AiReadyTenantLineInnerInterceptor(new TenantLineHandler() {
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

        // 分页插件（放在租户插件之后：count 语句同时带租户条件，total 与 records 口径一致）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        // 乐观锁插件（支持 @Version 注解）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 数据权限插件 — 根据 @DataScope 注解注入行级权限条件
        interceptor.addInnerInterceptor(new DataScopeInterceptor());

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
                        // 兼容 String 类型 tenantId 的实体（如 erp-finance 域），按字段类型赋值避免类型不匹配
                        Class<?> tenantIdType = metaObject.getGetterType("tenantId");
                        if (String.class.isAssignableFrom(tenantIdType)) {
                            metaObject.setValue("tenantId", String.valueOf(tenantId));
                        } else {
                            metaObject.setValue("tenantId", tenantId);
                        }
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
