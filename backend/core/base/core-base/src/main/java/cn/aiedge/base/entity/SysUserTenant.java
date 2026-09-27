package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户-租户关联实体
 * 支持一个用户关联多个租户，实现跨租户访问
 *
 * @author AI-Ready Team
 * @since 1.1.8
 */
@Data
@Accessors(chain = true)
@TableName("sys_user_tenant")
public class SysUserTenant {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 是否默认租户
     */
    private Boolean isDefault;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;

    /**
     * 上次登录该租户的时间（登录成功/切换企业时更新）
     * <p>多企业用户登录选企业时，据此把上次登录的企业排在第一位。</p>
     */
    private LocalDateTime lastLoginTime;

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
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
