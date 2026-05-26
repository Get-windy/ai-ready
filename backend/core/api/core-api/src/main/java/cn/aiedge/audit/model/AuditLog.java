package cn.aiedge.audit.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_audit_log")
public class AuditLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 审计类型：LOGIN, LOGOUT, CREATE, UPDATE, DELETE, EXPORT, IMPORT, ACCESS
     */
    private String auditType;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作对象类型
     */
    private String targetType;

    /**
     * 操作对象ID
     */
    private String targetId;

    /**
     * 操作对象名称
     */
    private String targetName;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作IP
     */
    private String operIp;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * 操作时间
     */
    private LocalDateTime operTime;

    /**
     * 操作结果：SUCCESS, FAILURE
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseData;

    /**
     * 耗时（毫秒）
     */
    private Long duration;

    /**
     * 业务数据快照（JSON）
     */
    private String dataSnapshot;

    /**
     * 变更前数据（JSON）
     */
    private String beforeData;

    /**
     * 变更后数据（JSON）
     */
    private String afterData;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 审计类型枚举
     */
    public enum AuditType {
        /**
         * 登录
         */
        LOGIN("login", "登录"),

        /**
         * 登出
         */
        LOGOUT("logout", "登出"),

        /**
         * 创建
         */
        CREATE("create", "创建"),

        /**
         * 更新
         */
        UPDATE("update", "更新"),

        /**
         * 删除
         */
        DELETE("delete", "删除"),

        /**
         * 导出
         */
        EXPORT("export", "导出"),

        /**
         * 导入
         */
        IMPORT("import", "导入"),

        /**
         * 访问
         */
        ACCESS("access", "访问"),

        /**
         * 授权
         */
        GRANT("grant", "授权"),

        /**
         * 其他
         */
        OTHER("other", "其他");

        private final String code;
        private final String desc;

        AuditType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() { return code; }
        public String getDesc() { return desc; }
    }
}
