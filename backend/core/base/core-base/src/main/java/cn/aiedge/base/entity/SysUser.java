package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体
 * 支持多租户架构
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    /**
     * 用户类型（0-系统用户 1-企业用户 2-代理用户）
     */
    private Integer userType;

    /**
     * 是否超级管理员（数据库唯一约束，最多一个 true）
     * 超级管理员不可删除、不可降级，拥有系统最高权限
     */
    private Boolean isSuperAdmin;

    /**
     * 是否租户管理员（每租户唯一约束，每租户最多一个 true）
     * 租户管理员不可删除、不可禁用，拥有该租户最高管理权限
     */
    private Boolean isTenantAdmin;

    /**
     * 状态（0-正常 1-禁用 2-锁定）
     */
    private Integer status;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 岗位ID
     */
    private Long postId;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 数据权限范围（ALL-全数据 DEPT-本部门 DEPT_CHILD-本部门及子部门 SELF-仅本人）
     * 若为空则继承角色 dataScope
     */
    private String dataScope;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 密码最后更新时间（用于密码过期校验）
     */
    private LocalDateTime passwordUpdateTime;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}