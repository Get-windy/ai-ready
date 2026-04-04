package cn.aiedge.storage.permission;

import java.lang.annotation.*;

/**
 * 文件权限检查注解
 * 用于标注需要权限检查的方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFilePermission {

    /**
     * 需要的权限类型
     */
    FilePermission.PermissionType value() default FilePermission.PermissionType.READ;

    /**
     * 文件ID参数名（用于从方法参数中提取文件ID）
     */
    String fileIdParam() default "fileId";

    /**
     * 未授权时的提示信息
     */
    String message() default "您没有访问此文件的权限";
}
