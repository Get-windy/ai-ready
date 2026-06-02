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

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}