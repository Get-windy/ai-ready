package com.aiready.erp.pricing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("erp_price_approval")
public class PriceApproval {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = true)
    private Long tenantId;

    private Long productId;
    private String productCode;
    private String productName;

    private Long customerId;
    private String customerName;

    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal priceChange;
    private String priceChangeType;

    private String approvalType;
    private String approvalReason;
    private String status;

    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;

    private Long approverId;
    private String approverName;
    private LocalDateTime approveTime;
    private String approveRemark;

    private Long effectiveStartTime;
    private Long effectiveEndTime;

    @TableField(fill = true)
    private Long createBy;

    @TableField(fill = true)
    private LocalDateTime createTime;

    @TableField(fill = true)
    private Long updateBy;

    @TableField(fill = true)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}