package cn.aiedge.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于标注方法需要进行数据权限过滤
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    
    /**
     * 数据权限字段（默认为 create_by）
     */
    String field() default "create_by";
    
    /**
     * 数据权限类型
     */
    DataScopeType scope() default DataScopeType.AUTO;
    
    /**
     * 数据权限类型枚举
     */
    enum DataScopeType {
        /** 自动根据用户角色判断 */
        AUTO,
        /** 全部数据 */
        ALL,
        /** 本部门数据 */
        DEPT,
        /** 本部门及下级数据 */
        DEPT_AND_CHILD,
        /** 仅本人数据 */
        SELF
    }
}
