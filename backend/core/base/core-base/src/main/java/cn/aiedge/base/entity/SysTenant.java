package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_tenant")
public class SysTenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantName;

    private String tenantCode;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String address;

    /**
     * 租户管理员用户ID
     */
    private Long adminUserId;

    /**
     * 租户等级（basic-基础版 professional-专业版 enterprise-企业版）
     */
    private String level;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}