package com.aiready.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("erp_replenishment_suggestion")
public class ReplenishmentSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = true)
    private Long tenantId;

    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;

    private BigDecimal currentQty;
    private BigDecimal safetyStock;
    private BigDecimal shortageQty;
    private BigDecimal avgDailySales;
    private Integer daysOfStock;
    private BigDecimal suggestedQty;

    private Integer priority;
    private Integer leadTime;
    private LocalDateTime estimatedArrival;
    private String reason;

    private String status;
    private Long purchaseOrderId;
    private LocalDateTime processTime;
    private String ignoreReason;

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