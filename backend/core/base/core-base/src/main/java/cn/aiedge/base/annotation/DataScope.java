package cn.aiedge.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解
 * 用于标记需要进行行级数据权限过滤的方法
 *
 * 使用方式：
 * @DataScope(deptAlias = "d", userAlias = "u")
 * public List<User> selectUserList(User user) { ... }
 *
 * 该注解会根据用户的数据权限范围自动注入SQL条件：
 * - ALL: 无过滤
 * - DEPT: dept_id = currentDeptId
 * - DEPT_AND_CHILD: dept_id IN (childDeptIds)
 * - SELF: create_by = currentUserId
 * - CUSTOM_SQL: 执行自定义SQL条件
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * 部门表的别名（用于SQL拼接）
     * 默认为 "d"
     */
    String deptAlias() default "d";

    /**
     * 用户表的别名（用于SQL拼接）
     * 默认为 "u"
     */
    String userAlias() default "u";

    /**
     * 目标表名（为空时自动从Mapper推断）
     * 用于精确匹配 sys_data_scope.target_table
     */
    String tableName() default "";

    /**
     * 部门ID字段名（默认 dept_id）
     */
    String deptIdField() default "dept_id";

    /**
     * 创建人字段名（默认 create_by）
     */
    String createByField() default "create_by";
}