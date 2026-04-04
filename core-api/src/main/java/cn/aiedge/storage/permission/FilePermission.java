package cn.aiedge.storage.permission;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件权限实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_file_permission")
public class FilePermission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 权限类型：READ, WRITE, DELETE, SHARE, ADMIN
     */
    private String permissionType;

    /**
     * 授权对象类型：USER, ROLE, DEPT, ALL
     */
    private String principalType;

    /**
     * 授权对象ID（用户ID/角色ID/部门ID）
     */
    private Long principalId;

    /**
     * 是否可继承
     */
    private Boolean inheritable;

    /**
     * 过期时间（null表示永不过期）
     */
    private LocalDateTime expireTime;

    /**
     * 授权人ID
     */
    private Long grantedBy;

    /**
     * 授权时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        /**
         * 只读权限
         */
        READ("read", "只读"),
        
        /**
         * 写入权限（包含读）
         */
        WRITE("write", "写入"),
        
        /**
         * 删除权限
         */
        DELETE("delete", "删除"),
        
        /**
         * 分享权限
         */
        SHARE("share", "分享"),
        
        /**
         * 管理权限（全部权限）
         */
        ADMIN("admin", "管理");

        private final String code;
        private final String desc;

        PermissionType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        /**
         * 检查是否包含指定权限
         */
        public boolean includes(PermissionType other) {
            if (this == ADMIN) return true;
            if (this == WRITE && other == READ) return true;
            return this == other;
        }
    }

    /**
     * 授权对象类型枚举
     */
    public enum PrincipalType {
        /**
         * 用户
         */
        USER("user", "用户"),
        
        /**
         * 角色
         */
        ROLE("role", "角色"),
        
        /**
         * 部门
         */
        DEPT("dept", "部门"),
        
        /**
         * 所有人
         */
        ALL("all", "所有人");

        private final String code;
        private final String desc;

        PrincipalType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
