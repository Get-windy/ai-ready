package cn.aiedge.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_customer_pool")
public class CustomerPool {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long customerId;

    private String customerCode;

    private String customerName;

    private Integer poolType;

    private String poolTypeDesc;

    private Integer poolReason;

    private String poolReasonDesc;

    private Long originalSalesPersonId;

    private String originalSalesPersonName;

    private Long originalDepartmentId;

    private String originalDepartmentName;

    private LocalDateTime poolTime;

    private Integer poolDays;

    private LocalDateTime expireTime;

    private Integer status;

    private String statusDesc;

    private Long claimSalesPersonId;

    private String claimSalesPersonName;

    private Long claimDepartmentId;

    private String claimDepartmentName;

    private LocalDateTime claimTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}