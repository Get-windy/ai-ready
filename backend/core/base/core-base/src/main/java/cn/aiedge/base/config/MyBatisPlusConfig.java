package cn.aiedge.base.config;

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
        // 租户↔菜单授权表（系统 → 租户管理 → 模块授权，菜单 62004）：
        // ⚠️ 这张表的 `tenant_id` 是**数据归属**（「给哪个租户授了什么菜单」），不是「会话租户过滤条件」——
        // 平台侧必须跨租户读写它。不忽略时 `deleteByTenantId(2)` 会被注入 `AND tenant_id = 1`（会话租户）
        // → 删除 0 行、旧授权残留 → 再插入撞 `uk_tenant_menu(tenant_id, menu_id)` 唯一索引
        // （该索引**不含 deleted**，软删行仍占键值）→ 重复保存报 400
        //「请求数据不完整或存在冲突」（2026-09-18 实踩）。
        "sys_tenant_menu",
        "sys_project_config",     // 项目配置可能跨租户
        "sys_menu",               // 菜单定义系统级共享
        "sys_role_menu",          // 角色菜单分配系统级
        "sys_permission",         // 权限定义系统级
        "sys_role_permission",    // 角色权限分配系统级
        "sys_permission_template", // 权限模板系统级
        // 「模块 → 权限码前缀」映射（V11.454.0）：**平台级参考数据**，行上 tenant_id 恒为 0。
        // 不忽略时：模块 entitlement 门是在**租户会话**里读它的，会被注入
        // `AND tenant_id = <会话租户>` → 一行都读不到 → 前缀解析全空 →
        // 「模块未开通」拦不住任何人，即这道门**静默失效（fail-open）**。
        // 与 sys_menu / sys_permission / sys_role_permission 同性质、同处置。
        "sys_module_permission",
        "sys_user_tenant",        // 用户租户关联表（登录时按用户名跨租户找账号，需要无过滤）
        // ⚠️ `sys_user` **已于 2026-09-18 从本清单移出**（原注释：「用户表（登录时需要无租户过滤查询）」）。
        //    历史问题：整表不隔离 → 除登录之外的**所有** sys_user 查询都没有租户条件，
        //    于是 `selectUserPage` 的租户条件只能靠 HTTP 查询参数传入（不传＝全库可见），
        //    `getById/updateById` 等按 id 的操作也无归属校验 → 任意租户管理员可读/改/删其它租户账号。
        //    现在改为「**登录链路局部放开**」：登录发生在认证之前（无 Sa-Token 会话），
        //    而 `AiReadyTenantLineInnerInterceptor.shouldSkip()` 在「租户不可解析」时会整体跳过注入，
        //    因此登录用的 `selectByUsername` / `selectByPhone` / `selectByEmail` 与
        //    `TenantRegistrationService`（注册）照常跨租户可查；认证之后的常规查询则自动带上 tenant_id。
        //    **另**：平台超管（SUPER_ADMIN）经 `isTenantScopeExempt()` 整体豁免，保持全局视野（见该方法注释）。
        "sys_login_log",          // 登录日志表
        "sys_region",             // 行政区划（省/市/区县，全系统公共数据，无tenant_id列）
        "flyway_schema_history",  // Flyway迁移历史表
        "sys_print_chain_item",   // 打印链路项（无tenant_id列）
        "sys_screenshot_task",    // 截图任务（无tenant_id列）
        "dms_event_outbox",       // DMS事件发件箱（无tenant_id列）
        // 定时任务（开发工具 → 定时任务）：平台级调度配置，与租户无关。
        // 不忽略时：迁移/种子写入的行（tenant_id=0）在租户会话下读不到，表现为「页面空白、
        // 触发执行报参数非法」（2026-09-14 实踩）
        "scheduled_task",
        "scheduled_task_log",
        // 工作流四表：多租户拦截属 P1 未实现项（见 AGENTS.md 核心差距），
        // 现阶段由 WorkflowServiceImpl 按 X-Tenant-Id 显式过滤；自动注入会使
        // 启动种子判重（tenant_id = null 永不匹配）与空租户会话下的可见性失效
        "workflow_definition",
        "workflow_node",
        "workflow_instance",
        "workflow_task",
        // 商城装修模板（商城 → 商城设置 → 商城装修）：
        // ⚠️ `shop_template` **无 tenant_id 列**（真库 information_schema 已核；V6.4.0 建表即无）。
        // 该表按设计是**平台共享**（「模板库」14 个行业模板为全租户共用，见 V11.366.0 注释），
        // 不忽略时拦截器会注入 `AND tenant_id = 1` → SQL 直接报「字段 tenant_id 不存在」，
        // 表现为 /template/list 与 /template/library 双双 500（2026-09-14 实踩）。
        "shop_template",
        // 拼团参与记录（营销 → 商城营销 → 商城拼团 →「拼团订单」Tab）：
        // ⚠️ `erp_group_buy_participant` **无 tenant_id 列**（真库 information_schema 已核），
        // 且主表 `erp_group_buy_activity` 才是租户归属方。不忽略时拦截器会注入 `p.tenant_id = 1`
        // → SQL 报「字段 p.tenant_id 不存在」，表现为 /group-buy/order/page 整页 500（2026-09-18 实踩）。
        "erp_group_buy_participant",
        // 客户等级关系（往来单位 × 等级）：
        // ⚠️ `biz_party_grade_relation` **无 tenant_id 列**（真库 information_schema 已核；tenant 归属由 party_id 决定），
        // 不忽略时拦截器注入 `AND tenant_id = 1` → SQL 报「字段 tenant_id 不存在」。
        // 二阶级联后果（2026-09-18 实踩）：该查询发生在 `SaleOrderServiceImpl.getCustomerGradeCode` 的 try 里，
        // 异常被 catch 吞掉，但 **PostgreSQL 已把整个事务标记为 aborted**，其后所有语句都报
        // 「当前事务被终止，事务块结束之前的查询被忽略」→ **销售订单创建整体 500**（整单不可用）。
        "biz_party_grade_relation",
        // 同属资料域「往来单位子表」且**无 tenant_id 列**的其余 4 张表（真库已核，一次性收口同一类问题）：
        // 租户归属均由 party_id 指向的 biz_party 决定，故不注入租户条件；不登记则任一查询都会整事务中止。
        "biz_party_address",
        "biz_party_role",
        "biz_party_transaction",
        "biz_customer_grade_price",
        "biz_migration_log",
        // 开发模板表（系统 → 开发工具 → 模板管理，菜单 62402）：⚠️ **无 tenant_id 列**（真库已核；V6.17.0 建表即无），
        // 平台级共享数据（模板定义不按租户归属 + 代码生成遗留种子同表）。不忽略时拦截器注入 `AND tenant_id = 1`
        // → SQL 报「字段 tenant_id 不存在」→ /api/import-templates 13 个端点全 500（2026-09-19 落库改造引入该表读写）。
        "dev_template"
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
     * 当前会话是否**整体豁免**租户隔离（平台超级管理员）。
     *
     * <p>为什么需要它：MyBatis-Plus 的租户拦截器是**无条件**的 —— 它不认识「超管」这个概念，
     * 一旦给某张表开了自动注入，超管也会被收敛到自己的会话租户。而本库
     * `admin` 是 `is_super_admin = true` + 角色 `SUPER_ADMIN`（`tenant_id = 1` = 系统租户 = 平台自身），
     * 其预期行为是**全局视野 / 可切换租户**（RuoYi 上游亦为「只有超管支持切换租户」）。
     * 若不加豁免，「用 admin 验证开发」会当场失效，且「数据凭空少了」极难被识别成权限问题。
     *
     * <p>⚠️ **这里只能「读缓存」，绝不能实时算角色**：`StpUtil.hasRole(...)` 会去查用户的角色，
     * 那条 SQL 又会经过本租户拦截器 → 再次调用本方法 → **无限递归**（实踩：登录直接
     * `java.lang.StackOverflowError`，表现为「系统异常」。原注释「将租户ID存入 Sa-Token Session，
     * 避免多租户拦截器递归查询」说的就是这件事）。
     * 因此豁免标记由**登录时**写入 Session（那时算角色是安全的），拦截器只做一次 Session 读取。
     *
     * <p>副作用（须知悉）：改动前**已签发**的旧 token 没有这个标记，会被当作「不豁免」而按会话租户收敛，
     * 需**重新登录一次**即可恢复全局视野。
     *
     * <p>与 `SysUserServiceImpl.resolveScopedTenantId` / `SysUserController.assertSameTenant`
     * 的「超管豁免」口径一致（那两处在 HTTP 层，可安全实时判定角色）。
     */
    public static boolean isTenantScopeExempt() {
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            Object cached = StpUtil.getSession().get("tenantScopeExempt");
            return cached != null && Boolean.parseBoolean(cached.toString());
        } catch (Exception ignored) {
            return false;
        }
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

        // 注：core-base 原有的「@DataScope 注解式数据权限」（DataScopeAspect + DataScopeContextHolder
        // + DataScopeInterceptor 三件套）已于 2026-09-20 删除 —— 该注解全仓零业务引用，即整条链
        // 从未生效过；而其能力已由 core-api 的 @DataPermission 注解 + 表级自动模式
        // （见 PermissionConfig 的 dataPermissionInterceptorRegistrar）覆盖，
        // 两套并存只会让后来者以为 @DataScope 还能用。

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
                // 乐观锁版本号：BaseEntity 的 `version` 带 `@TableField(fill = FieldFill.INSERT)`，
                // 该注解会让 MyBatis-Plus **把 version 列写进 INSERT 语句**；而本处理器此前没有填充它
                // ⇒ 插入时显式为 NULL ⇒ 凡是「version NOT NULL」的表（如 set_print_config）一律
                // `null value in column "version" violates not-null constraint`（2026-09-18 实机踩到）。
                // 这里统一填 0（= 初始版本，与 @Version 语义一致；DB 默认值也是 0）。
                this.strictInsertFill(metaObject, "version", Integer.class, 0);
                // 自动填充 tenantId（从当前租户ID获取）。
                // 仅在实体**未显式指定**租户时填充：此前无条件 setValue 会覆盖调用方写入的 tenantId，
                // 造成「给租户 A 建数据却落到会话租户头上」这类跨租户写错位（2026-09-18 已在
                // SysTenantMenuMapper 实踩并绕过；sys_role_permission 亦有 16 行租户标记不一致的实证）。
                if (metaObject.hasSetter("tenantId") && metaObject.hasGetter("tenantId")
                        && metaObject.getValue("tenantId") == null) {
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
